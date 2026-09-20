# HyperLedger getting started

## Prerequisites

[Prerequisites](https://hyperledger-fabric.readthedocs.io/en/release-2.2/prereqs.html) for HyperLedger Fabric.

## Getting Started

[Getting Started](https://hyperledger-fabric.readthedocs.io/en/release-2.5/install.html) with HyperLedger Fabric.

## Install latest version within WSL

```bash
# you need to be in hyperledgerfabric directory
curl -sSL https://bit.ly/2ysbOFE | bash -s
```

This will install samples into subdirectory `fabric-samples` and set up the `bin` directory with Fabric binaries.
It also downloads the latest Fabric images, which may take a while.

Clear any references to cloned git repository in the `fabric-samples` directory, which may cause issues with the generator:

```bash
source remove-git-references.sh
```

## Test scenario



Start network + channel:
./network.sh up createChannel -ca

Deploy your generated Java chaincode:
./network.sh deployCC -ccn vaja20 -ccl java -ccp ../../../bpmn-to-smartcontract/generated-test/vaja20-chaincode

Load peer env:
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=$PWD/../config
source ./scripts/envVar.sh
setGlobals 1

Invoke CreateProcess:
peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C mychannel -n vaja20 --peerAddresses localhost:7051 --tlsRootCertFiles "$PEER0_ORG1_CA" -c '{"function":"CreateProcess","Args":["p1"]}'

Query state:
peer chaincode query -C mychannel -n vaja20 -c '{"function":"ReadProcess","Args":["p1"]}'

Move through BPMN flows:
peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C mychannel -n vaja20 --peerAddresses localhost:7051 --tlsRootCertFiles "$PEER0_ORG1_CA" -c '{"function":"MoveByFlow","Args":["p1","Flow_1"]}'
peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C mychannel -n vaja20 --peerAddresses localhost:7051 --tlsRootCertFiles "$PEER0_ORG1_CA" -c '{"function":"MoveByFlow","Args":["p1","Flow_2"]}'
peer chaincode query -C mychannel -n vaja20 -c '{"function":"GetNextNodeIds","Args":["p1"]}'

Finish one valid branch (example):
Flow_Da -> Flow_3 -> Flow_5 -> Flow_168ogq3

Check end-state:
peer chaincode query -C mychannel -n vaja20 -c '{"function":"IsAtEnd","Args":["p1"]}'

Useful contract reference: Test001Contract.java

Negative tests you should run
Invalid transition should fail:
MoveToNode from current node to a non-adjacent node
Wrong flow from current node should fail:
MoveByFlow with a flow not allowed from the current node
Duplicate process id should fail:
CreateProcess with existing id
I have not executed these commands in your environment in this turn; this is the exact sequence based on your generated contract and current workspace layout.
