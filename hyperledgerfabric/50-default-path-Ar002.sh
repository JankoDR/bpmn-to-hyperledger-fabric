#!/usr/bin/env bash
set -eo pipefail

cd ~
cd /mnt/d/Janko/000_DR_vol2/DRnaloga/Program/bpmn2hyperledgersc

GITROOT="$(git rev-parse --show-toplevel)"
echo "$GITROOT"

cd "$GITROOT/hyperledgerfabric/fabric-samples/test-network"

export PATH="${PWD}/../bin:$PATH"
export FABRIC_CFG_PATH="${PWD}/../config"

source ./scripts/envVar.sh
setGlobals 1

CHAINCODE_NAME="ar002t"
PROCESS_ID="${PROCESS_ID:-ar002t_default_$(date +%s)}"

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

echo "== verify alternative/default path for $PROCESS_ID =="

# Create process.
# StartEvent_1 automatically activates TN01.
invoke_submit '{"function":"CreateProcess","Args":["'"$PROCESS_ID"'"]}'

# TN01 -> GW01.
#
# isYes=false:
#   Flow_Yes01: =isYes       -> false
#   Flow_No:    =not(isYes)  -> true
#
# Therefore GW01 activates TN03.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN01","{\"isYes\":false}"]}'

# TN03 -> GW02 -> Gateway_0eywdf9.
#
# At Gateway_0eywdf9:
#   Flow_Yes02: =isYes -> false
#
# Therefore the gateway uses its BPMN default flow Flow_17zenw2
# and activates TN05.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN03","{}"]}'

# TN05 -> Gateway_16y6r15 -> End01.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","TN05","{}"]}'

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
