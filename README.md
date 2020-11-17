# [SPREAD](https://github.com/fbielejec/SPREAD)[SPREAD](https://github.com/fbielejec/SPREAD)

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
