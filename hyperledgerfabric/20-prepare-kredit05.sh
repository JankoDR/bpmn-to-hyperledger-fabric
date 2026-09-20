#!/usr/bin/env bash

GITROOT="$(git rev-parse --show-toplevel)"
CC_PATH="$GITROOT/generated-contracts/kredit05_log-chaincode-v2"

cd "$CC_PATH"
./gradlew clean installDist

cd "$GITROOT/hyperledgerfabric"
