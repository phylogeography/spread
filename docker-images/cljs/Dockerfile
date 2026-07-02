FROM cimg/openjdk:16.0.0-node
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

USER root

# install Clojure CLI tools
RUN curl -O https://download.clojure.org/install/linux-install-1.10.3.814.sh
RUN chmod +x linux-install-1.10.3.814.sh
RUN ./linux-install-1.10.3.814.sh

USER circleci

RUN java -version
RUN node --version
RUN yarn --version
RUN clojure -M -e "(System/exit 0)"
