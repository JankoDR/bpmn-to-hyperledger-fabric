# BPMN 2 HyperLedger Smart Contract

## Introduction

This project is a tool that allows users to convert BPMN 2.0 diagrams into HyperLedger smart contracts.

## Getting started

Project is setup to be used with a Visual Studio Code. The recommended way to get started is to clone the repository and open it in Visual Studio Code. You can also use the built-in terminal in Visual Studio Code to run commands and manage your project.

Key extensions to install in Visual Studio Code:

- Red Hat BPMN Editor: [redhat.vscode-extension-bpmn-editor](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-extension-bpmn-editor)
- Java Extension: [Oracle Java](https://marketplace.visualstudio.com/items?itemName=Oracle.oracle-java)
- Maven Extension: [vscjava.vscode-maven](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-maven)
- Hyperledger Debugger Extension: [Spydra Hyperledger Fabric Debugger](https://marketplace.visualstudio.com/items?itemName=Spydra.hyperledger-fabric-debugger)

## Prerequisites

### Windows

- Java 17+ (OpenJDK or Oracle JDK)
- Maven 3.6+
- Git (optional, for cloning the repository)

Windows installation can be done using package managers like Winget or Chocolatey. Below are the steps to install Java and Maven using these tools.

Install chocolatey: [Chocolatey Install on Windows](https://chocolatey.org/install#individual)

Install Java and Maven, and ensure they are added to your system PATH. You can verify installation by running `java -version` and `mvn -version` in Command Prompt.

Install Java, Maven and Git via Chocolatey:

```ps1
# Install Git using Chocolatey
choco install git
# Install Java using chocolatey
choco install microsoft-openjdk --version=25.0.2
# Install Maven using Chocolatey
choco install maven
```

## Running the generator

This will generate the smart contracts in the 'generated-contracts' folder. You can then compile and test the generated smart contract with (for example with 'vaja20.bpmn').

First compile the generated smart contract using Maven:

```powershell
cd generated-contracts/vaja20-chaincode
mvn clean package
```

Then test it by simple execution of the main method in the 'App' class. You can do this by running the following command in the terminal:

```powershell
java -jar target/vaja20-chaincode-1.0.0.jar -i vaja20-chaincode
```

If there are no class/manifest errors, the generated contract is working correctly. You can then deploy it to a HyperLedger Fabric network and test it with the provided transactions.

## Running the generator v2 within WSL

If you are using Windows Subsystem for Linux (WSL), you can run the generator within WSL. First, ensure you have Java and Maven installed in your WSL environment. You can install them using your distribution's package manager (e.g., `apt` for Ubuntu).

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git

# make JAVA_HOME explicit (optional but recommended)
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# verify toolchain
java -version
javac -version
mvn -v
```

Then, you can run the generator with the following command:

```bash

# build the generator
mvn clean compile

# this generates the smart contracts in the 'generated-contracts' folder with the base package 'org.example.generated.v2'. You can change the base package as needed.
mvn exec:java -Dexec.args="--bpmnDir ../bpmn --outDir ../generated-contracts --basePackage org.example.generated.v2"

# this executes the generator with simulation mode, which generates additional code for simulating the contract logic without deploying to HyperLedger Fabric. This is useful for testing and debugging the generated contract logic.
mvn exec:java -Dexec.args="--bpmnDir ../bpmn --outDir ../generated-contracts --basePackage org.example.generated.v2 --simulate"
```

## Deploying the smart contract

### Testing with HyperLedger Fabric

[Prerequisites](https://hyperledger-fabric.readthedocs.io/en/release-2.2/prereqs.html) for HyperLedger Fabric.

[Getting Started](https://hyperledger-fabric.readthedocs.io/en/release-2.5/install.html) with HyperLedger Fabric.

### Install latest version of HyperLedger Fabric within WSL

```bash
# you need to be in hyperledgerfabric directory
curl -sSL https://bit.ly/2ysbOFE | bash -s
```

This will install samples into subdirectory `fabric-samples` and set up the `bin` directory with Fabric binaries.
It also downloads the latest Fabric images, which may take a while.

Clear any references to cloned git repository in the `fabric-samples` directory, which may cause issues with the generator:

```bash
GITROOT=$(git rev-parse --show-toplevel)
cd "$GITROOT/hyperledgerfabric"
source remove-git-references.sh
```

Start network and channel (using WSL in Windows):

```bash
GITROOT=$(git rev-parse --show-toplevel)
cd $GITROOT/hyperledgerfabric/fabric-samples/test-network
./network.sh up createChannel -ca
```

Shutdown network:

```bash
cd $GITROOT/hyperledgerfabric/fabric-samples/test-network
./network.sh down
```

## Test smart contract kredit05_log

Test the generated smart contract `kredit05_log` by following the steps in the scripts in the `hyperledgerfabric` directory. You can run them one by one to see the deployment and testing process.

```bash
GITROOT=$(git rev-parse --show-toplevel)
cd $GITROOT/hyperledgerfabric
```

Test the generated smart contract by running the following scripts in order:

```bash
./20-prepare-kredit05.sh
./30-deploy-kredit05.sh
./40-verify-kredit05.sh
./50-happy-path-kredit05.sh
```

You can shutdown the server after testing with the following command:

```bash
GITROOT=$(git rev-parse --show-toplevel)
cd $GITROOT/hyperledgerfabric
./90-stop-server.sh
```

## License

Academia license for academic use only. For commercial use, please contact the authors.

## Project status

Digging into PhD thesis of Janko Hriberšek.
