#!/usr/bin/env bash

cd ~
cd /mnt/d/Janko/000_DR_vol2/DRnaloga/Program/bpmn2hyperledgersc

GITROOT="$(git rev-parse --show-toplevel)"
echo "$GITROOT"

GITROOT="$(git rev-parse --show-toplevel)"
cd "$GITROOT/hyperledgerfabric/fabric-samples/test-network"

PROCESS_ID="${PROCESS_ID:-ar001t_p1_$(date +%s)}"
CHAINCODE_NAME="ar001t"

export PATH="${PWD}/../bin:$PATH"
export FABRIC_CFG_PATH="${PWD}/../config"
source ./scripts/envVar.sh
setGlobals 1

echo "== committed chaincodes on mychannel =="
peer lifecycle chaincode querycommitted --channelID mychannel

echo
echo "== create and read one process instance ($PROCESS_ID) =="
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
  -c "{\"function\":\"CreateProcess\",\"Args\":[\"$PROCESS_ID\"]}"

peer chaincode query \
  -C mychannel \
  -n "$CHAINCODE_NAME" \
  -c "{\"function\":\"ReadProcess\",\"Args\":[\"$PROCESS_ID\"]}"

cd "$GITROOT/hyperledgerfabric"