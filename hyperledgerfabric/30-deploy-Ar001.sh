#!/usr/bin/env bash

GITROOT="$(git rev-parse --show-toplevel)"
TEST_NETWORK_DIR="$GITROOT/hyperledgerfabric/fabric-samples/test-network"
CHAINCODE_PATH="../../../generated-contracts/ar001t-chaincode-v2"

cd "$TEST_NETWORK_DIR"

# Default behavior is clean redeploy to avoid lifecycle conflicts on repeated runs.
if [[ "${SKIP_RESTART:-0}" != "1" ]]; then
  ./network.sh down
  ./network.sh up createChannel -ca
fi

./network.sh deployCC \
  -ccn ar001t \
  -ccl java \
  -ccp "$CHAINCODE_PATH"

cd "$GITROOT/hyperledgerfabric"
