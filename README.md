# [spread](https://github.com/fbielejec/spread)

[![CircleCI](https://circleci.com/gh/fbielejec/spread/tree/master.svg?style=svg&circle-token=d5db014fe5702d820bb4bb42c93959d02fa8ddba)](https://circleci.com/gh/fbielejec/spread/tree/master)

Spread is a web application for analyzing and visualizing pathogen phylodynamic reconstructions resulting from Bayesian inference of sequence and trait evolutionary processes.

## Table of Contents

- [Technical Overview](#technical-overview)
- [Development](#development)
- [Contributors](#contributors)
- [License](#license)

## Technical Overview

To be able to easily pick up stack used to build spread, one should be familiar with following topics:
* [Java](https://www.java.com/)
* [Clojure](https://clojure.org/)
* [ClojureScript](https://clojurescript.org/)
* [GraphQL](https://graphql.org/)
* [MySQL](https://www.mysql.com/)
* [Docker](https://www.docker.com/)
* [S3](https://aws.amazon.com/s3/)
* [SQS](https://aws.amazon.com/sqs/)

The diagram below presents an overview of the architecture of spread:

<img src="https://raw.githubusercontent.com/fbielejec/spread/master/docs/system_architecture.png?token=AADSMXCN4NNXDND2OA4NCSS72KAFC" width="500" align="center">

![alt text](https://github.com/fbielejec/spread/blob/master/docs/system_architecture.png?raw=true)

- API is a gateway service, exposing [graphql](https://graphql.org/) endpoints and publishing messages to the [SQS queue](https://aws.amazon.com/sqs/).
- Worker is a messaging service wrapping the phylogeographic tree graphs parsing library [libspread](https://github.com/fbielejec/spread/tree/master/src/main/java/com/spread), multiple workers compete for the messages published by the API.
- Relational Database and S3 object storage are used for persistance.
- User-facing interface facilitates communicating with the API from the client.
- Visualization engine accepts S3 stored output, animates and displays it on the geographical map.

## Development

TODO

### Contributors

This project exists thanks to all the people who contribute.

[![](https://github.com/fbielejec.png?size=50)](https://github.com/fbielejec)
[![](https://github.com/jpmonettas.png?size=50)](https://github.com/jpmonettas)

## License

[MIT](LICENSE) © Filip Bielejec
















## [SPREAD](https://github.com/fbielejec/SPREAD)

SPREAD is a web application for analyzing and visualizing pathogen phylodynamic reconstructions resulting from Bayesian inference of sequence and trait evolutionary processes.

# Technical Overview #

To be able to easily pick up stack used to build SPREAD, one should be familiar with following topics:
* [Java](https://www.java.com/)
* [Clojure](https://clojure.org/)
* [RabbitMQ](https://www.rabbitmq.com/)
* [Docker](https://www.docker.com/)

# Development #

## Start dev infrastructure locally ##

Make sure you have docker and docker-compose installed:

```bash
sudo apt-get install -y docker.io
sudo gpasswd -a "${USER}" docker
sudo systemctl start docker
sudo systemctl enable docker
sudo curl -L "https://github.com/docker/compose/releases/download/1.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod a+x /usr/local/bin/docker-compose
```

Start the containers:

```bash
cd deployments/dev
source ../../env/dev
docker-compose -f docker-compose.yml up
```

## Backend services

Make sure you have clojure and clojure cli-tools installed locally:

```bash
curl -O https://download.clojure.org/install/linux-install-1.10.1.536.sh
chmod +x linux-install-1.10.1.536.sh
sudo ./linux-install-1.10.1.536.sh
```

### API

Invoke the `api` alias:

```bash
clj -A:run-api
```

If you're using [emacs](https://www.gnu.org/software/emacs/) and [cider](https://docs.cider.mx/cider/index.html) you can simply:

```
M+x cider-jack-in
M+n api.main
C-c C-k
(start)
```

Graphiql IDE is than started at:

http://127.0.0.1:3001/ide
