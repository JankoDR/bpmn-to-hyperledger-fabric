#!/usr/bin/env bash

GITROOT="$(git rev-parse --show-toplevel)"
CC_PATH="$GITROOT/generated-contracts/ar001t-chaincode-v2"

echo $CC_PATH
cd "$CC_PATH"
./gradlew clean installDist

cd "$GITROOT/hyperledgerfabric"