#!/bin/bash -e

GITROOT=$(git rev-parse --show-toplevel)

cd $GITROOT/hyperledgerfabric/fabric-samples/test-network
./network.sh down
cd $GITROOT/hyperledgerfabric

