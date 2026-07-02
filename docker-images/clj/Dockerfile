FROM cimg/openjdk:11.0.11
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

USER root

# install Clojure CLI tools
RUN curl -O https://download.clojure.org/install/linux-install-1.10.3.986.sh
RUN chmod +x linux-install-1.10.3.986.sh
RUN ./linux-install-1.10.3.986.sh

USER circleci

RUN java -version
RUN clojure -M -e "(System/exit 0)"
