FROM cimg/node:12.19.0
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

USER root

# install less
#RUN apt-get install --no-install-recommends -yq \
#            less \
#            && rm -rf /tmp/* /var/{tmp,cache}/* /var/lib/{apt,dpkg}/

# install yarn, serverless CLI
RUN yarn global add serverless #serverless-domain-manager

# install aws cli
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip awscliv2.zip
RUN ./aws/install

USER circleci

RUN node --version
RUN yarn --version
RUN aws --version
