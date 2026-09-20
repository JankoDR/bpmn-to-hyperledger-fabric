#!/usr/bin/env bash
set -eo pipefail

cd ~
cd /mnt/d/Janko/000_DR_vol2/DRnaloga/Program/bpmn2hyperledgersc

GITROOT="$(git rev-parse --show-toplevel)"
echo "$GITROOT"

GITROOT="$(git rev-parse --show-toplevel)"
cd "$GITROOT/hyperledgerfabric/fabric-samples/test-network"

export PATH="${PWD}/../bin:$PATH"
export FABRIC_CFG_PATH="${PWD}/../config"
source ./scripts/envVar.sh
setGlobals 1

CHAINCODE_NAME="ar001t"
PROCESS_ID="${PROCESS_ID:-ar001t_happy_$(date +%s)}"

invoke_submit() {
  local payload="$1"
  peer chaincode invoke \
    -o localhost:7050 \
    --ordererTLSHostnameOverride orderer.example.com \
    --tls \
    --cafile "$ORDERER_CA" \
    --waitForEvent \
    --waitForEventTimeout 30s \
    -C mychannel \
    -n "$CHAINCODE_NAME" \
    --peerAddresses localhost:7051 \
    --tlsRootCertFiles "$PEER0_ORG1_CA" \
    --peerAddresses localhost:9051 \
    --tlsRootCertFiles "$PEER0_ORG2_CA" \
    -c "$payload"
}

query_state() {
  peer chaincode query \
    -C mychannel \
    -n "$CHAINCODE_NAME" \
    -c '{"function":"ReadProcess","Args":["'"$PROCESS_ID"'"]}'
}

echo "== verify one happy path for $PROCESS_ID =="

# Create process. StartEvent_1 automatically activates TN01.
invoke_submit '{"function":"CreateProcess","Args":["'"$PROCESS_ID"'"]}'

# TN01 -> GW01 (parallel split) -> TN02 + TN03
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN01","{}"]}'

# Complete both parallel branches.
# GW02 (parallel join) continues only after both TN02 and TN03 are complete.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN02","{}"]}'
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN03","{}"]}'

# After GW02 join, TN04 becomes active; completing it reaches End01.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN04","{}"]}'

echo
echo "== final process state =="
query_state

echo
echo "== is process at end? =="
peer chaincode query \
  -C mychannel \
  -n "$CHAINCODE_NAME" \
  -c '{"function":"IsAtEnd","Args":["'"$PROCESS_ID"'"]}'

cd "$GITROOT/hyperledgerfabric"
