#!/bin/bash

##
## EXAMPLE USAGE
## ./deploy.sh clj
##

#--- ARGS
NAME=$1

#--- FUNCTIONS

function build {
    {
        NAME=$1
        IMAGE=nodrama/$NAME
        TAG=$(git log -1 --pretty=%h)
        IMG=$IMAGE:$TAG

        echo "============================================="
        echo  "Buidling: "$IMG""
        echo "============================================="

        docker build -t $IMG -f $NAME/Dockerfile .
        docker tag $IMG $IMAGE:latest

    } || {
        echo "EXCEPTION WHEN BUIDLING "$IMG""
        exit 1
    }

}

function push {
    IMAGE=nodrama/$1
    echo "Pushing: " $IMAGE
    docker push $IMAGE
}

function login {
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
}

#--- EXECUTE

login
build $NAME
push $NAME

exit $?
