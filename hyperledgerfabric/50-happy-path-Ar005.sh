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

CHAINCODE_NAME="ar005"
PROCESS_ID="${PROCESS_ID:-$CHAINCODE_NAME_happy_$(date +%s)}"

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

# Create process.
# E_START automatically activates T01.
invoke_submit '{"function":"CreateProcess","Args":["'"$PROCESS_ID"'"]}'

# T01 -> T02.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T01","{}"]}'

# T02 -> G01_XOR_DOCS_OK.
# docsComplete=true selects F04_YES and activates the parallel split PG01,
# which activates both T05A and T05B.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T02","{\"docsComplete\":true}"]}'

# Parallel analysis branch 1: T05A -> PG02_AND_JOIN_ANALYSIS.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T05A","{}"]}'

# Parallel analysis branch 2: T05B -> PG02_AND_JOIN_ANALYSIS.
# After both arrivals, the AND join continues to T05C.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T05B","{}"]}'

# T05C -> G02_XOR_ELIGIBLE.
# eligible=true selects F11_YES and activates T06.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T05C","{\"eligible\":true}"]}'

# T06 -> T07.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T06","{}"]}'

# T07 -> T08.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T07","{}"]}'

# T08 -> G03_XOR_OFFER_ACCEPT.
# offerAccepted=true selects F15_YES and activates T09.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T08","{\"offerAccepted\":true}"]}'

# T09 -> T10.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T09","{}"]}'

# T10 -> G04_XOR_APPROVED.
# approved=true selects F18_YES and activates T11.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T10","{\"approved\":true}"]}'

# T11 -> T12.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T11","{}"]}'

# T12 -> T13.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T12","{}"]}'

# T13 -> G05_XOR_COLLATERAL_OK.
# collateralOk=true selects F22_YES and activates T14.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T13","{\"collateralOk\":true}"]}'

# T14 -> E_END_APPROVED.
invoke_submit '{"function":"CompleteActivity","Args":["'"$PROCESS_ID"'","T14","{}"]}'

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
