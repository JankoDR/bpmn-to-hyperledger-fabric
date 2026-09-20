package com.bpmntosmartc.generator.v2;

import com.bpmntosmartc.generator.v2.codegen.FabricContractProjectGeneratorV2;
import com.bpmntosmartc.generator.v2.model.BpmnProcessModel;
import com.bpmntosmartc.generator.v2.parser.BpmnParser;
import com.bpmntosmartc.generator.v2.simulation.ProcessSimulationHarness;
import com.bpmntosmartc.generator.v2.util.NameUtils;
import com.bpmntosmartc.generator.v2.validation.BpmnValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(final String[] args) throws Exception {
        Config config = Config.fromArgs(args);

        if (!Files.isDirectory(config.bpmnDir)) {
            throw new IllegalArgumentException("BPMN directory does not exist: " + config.bpmnDir);
        }

        Files.createDirectories(config.outputDir);

        BpmnParser parser = new BpmnParser();
        BpmnValidator validator = new BpmnValidator();
        FabricContractProjectGeneratorV2 codeGenerator = new FabricContractProjectGeneratorV2();
        ProcessSimulationHarness simulationHarness = new ProcessSimulationHarness();

        List<Path> bpmnFiles = Files.list(config.bpmnDir)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".bpmn"))
                .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                .collect(Collectors.toList());

        if (bpmnFiles.isEmpty()) {
            System.out.println("No BPMN files found in: " + config.bpmnDir);
            return;
        }

        System.out.println("Found " + bpmnFiles.size() + " BPMN file(s).");

        for (Path bpmnFile : bpmnFiles) {
            System.out.println("\nProcessing " + bpmnFile.getFileName() + " ...");

            BpmnProcessModel model = parser.parse(bpmnFile);
            validator.validate(model);

            String baseName = stripExtension(bpmnFile.getFileName().toString());
            String packageToken = NameUtils.toPackageToken(baseName);
            String artifactId = packageToken + "-chaincode-v2";
            Path contractProjectDir = config.outputDir.resolve(packageToken + "-chaincode-v2");

            FabricContractProjectGeneratorV2.GeneratedContractInfo info = codeGenerator.generate(
                    model,
                    contractProjectDir,
                    config.basePackage,
                    packageToken,
                    artifactId);

            System.out.println("  Chaincode project: " + info.getProjectDir());
            System.out.println("  Contract class: " + info.getPackageName() + "." + info.getContractClassName());

            if (config.simulate) {
                List<String> simulationOutput = simulationHarness.runDefaultScenarios(model, bpmnFile.getFileName().toString());
                for (String line : simulationOutput) {
                    System.out.println(line);
                }
            }
        }

        System.out.println("\nGeneration completed.");
    }

    private static String stripExtension(final String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0) {
            return fileName;
        }
        return fileName.substring(0, idx);
    }

    private static class Config {
        private final Path bpmnDir;
        private final Path outputDir;
        private final String basePackage;
        private final boolean simulate;

        private Config(final Path bpmnDir, final Path outputDir, final String basePackage, final boolean simulate) {
            this.bpmnDir = bpmnDir;
            this.outputDir = outputDir;
            this.basePackage = basePackage;
            this.simulate = simulate;
        }

        private static Config fromArgs(final String[] args) {
            Path bpmnDir = Paths.get("..", "bpmn").toAbsolutePath().normalize();
            Path outputDir = Paths.get("..", "generated-contracts").toAbsolutePath().normalize();
            String basePackage = "org.example.generated.v2";
            boolean simulate = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--bpmnDir":
                        requireValue(args, i, arg);
                        bpmnDir = Paths.get(args[++i]).toAbsolutePath().normalize();
                        break;
                    case "--outDir":
                        requireValue(args, i, arg);
                        outputDir = Paths.get(args[++i]).toAbsolutePath().normalize();
                        break;
                    case "--basePackage":
                        requireValue(args, i, arg);
                        basePackage = args[++i];
                        break;
                    case "--simulate":
                        simulate = true;
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown argument: " + arg
                                        + "\nUsage: --bpmnDir <path> --outDir <path> --basePackage <package> [--simulate]");
                }
            }

            return new Config(bpmnDir, outputDir, basePackage, simulate);
        }

        private static void requireValue(final String[] args, final int i, final String arg) {
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for argument: " + arg);
            }
        }
    }
}
