# CI images

Source for the custom Docker images that `.circleci/config.yml` runs its jobs in. They
are published to Docker Hub under the `nodrama` namespace.

| Image                   | Dockerfile                    | Used by (`.circleci/config.yml`)             |
|-------------------------|-------------------------------|----------------------------------------------|
| `nodrama/clj:latest`    | [`clj/Dockerfile`](clj/Dockerfile)       | `clj-service/build` (api, worker)            |
| `nodrama/cljs:latest`   | [`cljs/Dockerfile`](cljs/Dockerfile)     | `cljs-service/build` (ui, analysis-viewer)   |
| `nodrama/deploy:latest` | [`deploy/Dockerfile`](deploy/Dockerfile) | all `*/deploy` jobs + `Instrument DB`        |

All three build on CircleCI's [`cimg/*`](https://circleci.com/developer/images) convenience
images (which already include Docker, git and caching tooling), then layer on:

- **clj** — Clojure CLI (1.10.3.986) on JDK 11
- **cljs** — Clojure CLI (1.10.3.814) on JDK 16 + Node/Yarn
- **deploy** — Serverless Framework + AWS CLI v2 on Node 12

## Build & push

`deploy.sh` logs in, builds `nodrama/<name>` (tagged with the current git short-SHA and
`latest`) and pushes it. Run it from this directory, with `DOCKER_USERNAME` /
`DOCKER_PASSWORD` set for the `nodrama` Docker Hub account:

```bash
cd docker-images
./deploy.sh clj
./deploy.sh cljs
./deploy.sh deploy
```
