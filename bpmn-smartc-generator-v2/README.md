# BPMN to Hyperledger Fabric Java Smart Contract Generator V2

This generator replaces the matrix-based approach with a flow-aware model for a constrained BPMN subset.

## Supported BPMN Subset

- `startEvent`
- `endEvent`
- `task` (and all BPMN `*Task` variants)
- `exclusiveGateway` (with optional default flow)
- `parallelGateway` (split or join)
- `sequenceFlow`

Any unsupported BPMN process-level element fails validation before code generation.

## Runtime Semantics in Generated Contract

- BPMN tasks are exposed as contract transactions (one transaction method per task).
- Parallel join waits for all required predecessor branches.
- Exclusive gateway routing uses process metadata (variables updated by predecessor task transactions).
- Default flow is used when no condition is true.

## Build and Run

From this folder:

```bash
mvn clean compile
mvn exec:java
```

Optional arguments:

```bash
mvn exec:java -Dexec.args="--bpmnDir ../bpmn --outDir ../generated-contracts --basePackage org.example.generated.v2"
```

Run with built-in simulation harness:

```bash
mvn exec:java -Dexec.args="--bpmnDir ../bpmn --outDir ../generated-contracts --basePackage org.example.generated.v2 --simulate"
```

## Output

For each BPMN model, the generator creates:

- `<model>-chaincode-v2/`
  - `build.gradle` and `settings.gradle`
  - Gradle wrapper (`gradlew`, `gradlew.bat`, and `gradle/wrapper/`)
  - `pom.xml`
  - generated Java contract and state classes
  - three generated JUnit test classes under `src/test/java`
  - `README.md`

Build each generated chaincode project with:

```bash
./gradlew clean installDist
```

The Fabric-compatible application distribution is created under `build/install`.

Run each generated project's baseline tests with:

```bash
./gradlew test
```

The Maven build remains available through `mvn clean package` and `mvn test`.

## Simulation Harness

The `--simulate` mode runs predefined scenarios directly in the generator runtime and prints state snapshots.

- Scenario A: happy path to approved end
- Scenario B: document rework followed by rejection path

For each simulated step it prints active nodes and metadata map, so you can inspect:

- Parallel synchronization (for example `T05A` and `T05B` before continuing)
- Exclusive routing driven by metadata fields (for example `docsComplete`, `eligible`, `approved`, `collateralOk`)
