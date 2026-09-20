#!/bin/bash -e

GITROOT=$(git rev-parse --show-toplevel)
cd $GITROOT/hyperledgerfabric
source remove-git-references.sh || true

cd $GITROOT/hyperledgerfabric/fabric-samples/test-network
./network.sh up createChannel -ca

export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config
source ./scripts/envVar.sh
setGlobals 1

cd $GITROOT/hyperledgerfabric
