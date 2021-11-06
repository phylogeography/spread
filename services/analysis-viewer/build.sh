#!/bin/bash

set -e

NAME=analysis-viewer-service

while getopts b:p:t: flag
do
  case "${flag}" in
    b) BUILD=${OPTARG};;
    p) PUSH=${OPTARG};;
  esac
done

# defaults

if [ -z ${BUILD+x} ];
then
  BUILD=true
fi

if [ -z ${PUSH+x} ];
then
  PUSH=false
fi

# BUILD

if [ $BUILD = true ]
then
  cd ../../
  yarn deps
  yarn shadow-cljs release analysis-viewer
fi

# PUSH

if [ $PUSH = true ]
then

  DISTRIBUTION_ID=E1H7C6SLAJ6XK7
  # push new content to S3
  aws s3 sync analysis-viewer/ s3://view.spreadviz.org --acl public-read
  # invalidate CF cache
  aws cloudfront create-invalidation --distribution-id $DISTRIBUTION_ID --paths '/*'

fi

echo "Done"
exit $?
