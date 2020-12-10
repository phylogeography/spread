#!/bin/bash

set -e

NAME=worker-service

while getopts b:p:t: flag
do
  case "${flag}" in
    b) BUILD=${OPTARG};;
    p) PUSH=${OPTARG};;
    t) TAG=${OPTARG};;
  esac
done

# defaults

if [ -z ${TAG+x} ];
then
  TAG=latest
fi

if [ -z ${BUILD+x} ];
then
  BUILD=true
fi

if [ -z ${PUSH+x} ];
then
  PUSH=false
fi

# BUILD

IMG=$NAME:$TAG

if [ $BUILD = true ]
then
    cd ../../
    clojure -Spom;
    clojure -A:uberjar worker-service.jar -C -m worker.main;    
    docker build --tag $IMG -f services/worker/Dockerfile .
fi

# PUSH

if [ $PUSH = true ]
then
  echo "Pushing $IMG to Dockerhub"
  # authenticate docker to use dockerhub registry
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin  
  # tag image  
  docker tag $IMG spread/$NAME:$TAG
  # push to registry
  docker push spread/$NAME:$TAG
fi

echo "Done"
exit $?
