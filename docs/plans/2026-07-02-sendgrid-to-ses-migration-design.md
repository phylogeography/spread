# Migrate login email from SendGrid to AWS SES

**Date:** 2026-07-02
**Status:** Design — awaiting review before implementation
**Author:** Filip (with Claude)

## Problem

Magic-link login emails stopped delivering. CloudWatch (`/ecs/spread-api-prod`)
shows every send failing:

```
POST https://api.sendgrid.com/v3/mail/send → 401
{"errors":[{"message":"Maximum credits exceeded"}]}
```

The SendGrid account is on the free plan, which now allows 0 emails/month. The
API key authenticates fine; the account quota is the wall. Confirmed 1:1: every
`send-login-email` request (real users + a test send) is rejected. Decision:
migrate transactional email to **AWS SES**, which fits the existing AWS infra
(`us-east-2`) and avoids per-email plan ceilings.

## Constraints (verified via `kuleuven` AWS profile, us-east-2)

- **SES is in sandbox** (`ProductionAccessEnabled: false`, cap 200/day). In
  sandbox SES only delivers to *verified* recipients — magic-link login to
  arbitrary users (gmail, uwo.ca, …) will NOT work until production access is
  granted.
- **No verified identities** — `noreply@spreadviz.org` is not verified; nothing
  sends until the sender is verified.
- **No ECS TaskRole** (`serverless.yml:45` is commented out). The cognitect AWS
  clients authenticate with the IAM *user* access keys passed via env
  (`API_AWS_ACCESS_KEY_ID` / `API_AWS_SECRET_ACCESS_KEY`) — same as S3/SQS. So
  SES permission must be granted on that IAM user, not a task role.

## Ops prerequisites (must happen for delivery; largely outside the repo)

1. **Verify sender** `noreply@spreadviz.org` in SES `us-east-2` — prefer
   **domain verification** for `spreadviz.org` (DKIM DNS records) for
   deliverability and to allow any `@spreadviz.org` sender.
2. **Request SES production access** (exit sandbox) for `us-east-2`. AWS support
   request, ~24h. This is the critical-path item — start it early.
3. **Grant `ses:SendEmail` + `ses:SendRawEmail`** to the IAM user whose keys the
   API uses (`AKIA34OG2YU5O4LZRI5E`).

## Design decision

Use **cognitect aws-api (SES v1, `:api :email`)**, mirroring the existing
`aws.s3` / `aws.sqs` clients. Same client-creation shape, same `aws/invoke` +
`throw-on-error` idiom, same IAM creds/region from the `:aws` config. Email body
is built **inline as HTML** in Clojure (the SendGrid dynamic template is trivial:
a header, a line of body text, and a login button) — no SES templates (YAGNI).

Alternatives rejected: AWS Java SDK v2 (more interop boilerplate, inconsistent
with the cognitect clients); SES SMTP (needs separate SMTP credentials and a
mail library).

## Components

### New: `src/clj/aws/ses.clj`
```clojure
(ns aws.ses
  (:require [aws.utils :refer [throw-on-error]]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]))

(defn create-client
  "Creates an SES client, mirroring aws.s3/aws.sqs."
  [{:keys [access-key-id secret-access-key region]}]
  (aws/client (cond-> {:api :email
                       :credentials-provider
                       (credentials/basic-credentials-provider
                        {:access-key-id access-key-id
                         :secret-access-key secret-access-key})}
                region (assoc :region region))))

(defn build-email-request
  "Pure: builds the SES :SendEmail request map. Unit-tested."
  [{:keys [from to subject html]}]
  {:Source from
   :Destination {:ToAddresses [to]}
   :Message {:Subject {:Data subject :Charset "UTF-8"}
             :Body {:Html {:Data html :Charset "UTF-8"}}}})

(defn send-email [ses params]
  (-> (aws/invoke ses {:op :SendEmail :request (build-email-request params)})
      (throw-on-error {:api :ses :fn ::send-email})))
```

### New: `src/clj/api/emailer/login_email.clj` (or a fn in mutations)
Pure `login-email-html` builder producing the same content as the old template:
- header: "Login to Spread"
- body: "You requested a login link to Spread. Click the button below."
- button "Login" → `<redirect-uri>?auth=email&token=<token>`

Unit-tested: asserts the magic-link URL and token are present in the HTML.

### Changed: `src/clj/api/server.clj`
In `start`, create the SES client alongside s3/sqs and add to context:
```clojure
ses (aws-ses/create-client aws)
;; ... context: :ses ses
```

### Changed: `src/clj/api/mutations.clj` — `send-login-email`
Replace the `sendgrid/send-email` call with:
```clojure
(ses/send-email ses {:from    from-address
                     :to      email
                     :subject "Login to Spread"
                     :html    (login-email-html redirect-uri token)})
```
Destructure `:ses` (and a `:from-address`) from the resolver context instead of
`:sendgrid`. Keep the `banned-domains` check and error handling unchanged.

### Changed: `src/clj/api/config.clj`
- Add `:from-address "noreply@spreadviz.org"` (config value, overridable by env).
- Remove the `:sendgrid` block and `SENDGRID_API_KEY` (no longer used). SES uses
  the existing `:aws` creds.

### Changed: `deps.edn`
Add `com.cognitect.aws/email {:mvn/version "<compatible>"}` (align era with the
existing `com.cognitect.aws/s3` `809.2.797.0`; resolve exact version at impl).

### Changed: `services/api/serverless.yml`
Remove the `SENDGRID_API_KEY` env entry (lines ~93–94). No IAM block change
needed (permission goes on the IAM user, see prerequisites).

### Removed (later): `src/clj/api/emailer/sendgrid.clj`
Once SES is verified in prod. Keep during rollout for quick rollback.

## Data flow (unchanged shape)
```
Frontend  →  sendLoginEmail mutation  →  ip-jail  →  send-login-email
   →  ses/send-email (SES :SendEmail via IAM user creds)  →  recipient
```

## Error handling
`aws/invoke` returns an error map on failure; `throw-on-error` raises, caught by
the existing `try/catch` in `send-login-email` (logs "Sending login email
failed", rethrows). Sandbox rejections (unverified recipient) and
`MessageRejected` surface here — visible in CloudWatch exactly like the current
SendGrid errors.

## Testing
1. **Unit (kaocha, no AWS):** `build-email-request` shape; `login-email-html`
   contains the token + `?auth=email` link. TDD, red→green.
2. **Sandbox integration (manual):** with a *verified* test recipient, call
   `send-email` and confirm a `MessageId` (no error). Proves creds + IAM +
   region before production access lands.
3. **Post-production-access:** real login from spreadviz.org; watch CloudWatch
   for success and the email arriving.

> Note: local test runs need the `deps.edn` `:mvn/repos … :snapshots false`
> workaround under Clojure CLI 1.12 (CI uses 1.10.3, which is fine). See the
> secret-redaction change notes.

## Rollout
1. Merge code. 2. Ensure sender verified + IAM permission granted. 3. CI build +
`serverless deploy` + force ECS redeploy. 4. Verify in sandbox (verified
recipient), then after production access, verify a real login. 5. Delete
`sendgrid.clj` and the SendGrid account/key.

## Open questions
- Domain verification vs single-email for the sender? (Recommend domain + DKIM.)
- Keep `noreply@spreadviz.org`, or a different From?
- Drop SendGrid immediately, or keep behind a feature flag for one release?
