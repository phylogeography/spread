(ns api.emailer.sendgrid
  (:require [clj-http.client :as http]))

(defonce ^:private sendgrid-public-api "https://api.sendgrid.com/v3/mail/send")

(defn send-email
  [{:keys [from to subject content dynamic-template-data template-id api-key print-mode?]}]
  (if (and (not api-key)
           (not print-mode?))
    (throw (Error. "Missing api-key to send email to sendgrid"))
    (if print-mode?
      (do
        (prn "Would send email:")
        (prn "From:" from)
        (prn "To:" to)
        (prn "Subject:" subject)
        (prn "Content:" content)
        (prn "Dynamic template data:" dynamic-template-data))
      (let [body (cond-> {:from {:email from}
                          :personalizations [(cond-> {:to [{:email to}]}
                                               dynamic-template-data
                                               (assoc "dynamic_template_data"
                                                      dynamic-template-data))]}
                   subject     (assoc :subject subject)
                   template-id (assoc :template_id template-id)
                   content     (assoc :content [{:type  "text/html"
                                                 :value content}]))]
        (http/post sendgrid-public-api
                   {:form-params  body
                    :headers      {"Authorization" (str "Bearer " api-key)}
                    :content-type :json
                    :accept       :json})))))

(comment
  (let [{{:keys [api-key template-id]} :sendgrid} (api.config/load!)]
    (send-email
      {:from        "noreply@spreadviz.org"
       :to          "fbielejec@gmail.com"
       :template-id template-id
       :dynamic-template-data
       {"header"       "Login to Spread"
        "body"         "You requested a login link to Spread. Click on the button below"
        "button-title" "Login"
        "button-href"  "http://nodrama.io"}
       :api-key     api-key})))
