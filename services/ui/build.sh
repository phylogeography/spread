#!/bin/bash

set -e

NAME=ui-service

while getopts b:p:t: flag
do
  case "${flag}" in
    b) BUILD=${OPTARG};;
    p) PUSH=${OPTARG};;
  esac
done

if [ -z ${BUILD+x} ];
then
  BUILD=true
fi

if [ -z ${PUSH+x} ];
then
  PUSH=false
fi

# BUILD

if [ $BUILD = true ]; then
    cd ../../
    yarn deps
    yarn release
fi

# PUSH

if [ $PUSH = true ]
then

  DISTRIBUTION_ID=E290TKZB4KVQPC
  # push new content to S3
  aws s3 sync ui/ s3://spreadviz.org --acl public-read
  # invalidate CF cache
  aws cloudfront create-invalidation --distribution-id $DISTRIBUTION_ID --paths '/*'

fi

echo "Done"
exit $?
