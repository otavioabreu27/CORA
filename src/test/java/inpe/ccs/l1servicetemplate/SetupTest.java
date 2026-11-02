package inpe.ccs.l1servicetemplate;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetupTest {
    @TempDir Path directory;

    private void fixture() throws Exception {
        Files.createDirectories(directory.resolve("tools"));
        Files.copy(Path.of("tools/Setup.java"), directory.resolve("tools/Setup.java"));
        Files.writeString(directory.resolve("gradle.properties"), "serviceName=sample-template\ngroupId=org.sample\nbasePackage=org.sample.template\nserviceVersion=1.0.0-SNAPSHOT\nserviceLevel=1\n");
        Files.writeString(directory.resolve("README.md"), "<!-- service-name -->A preencher<!-- /service-name -->\norg.sample.template\nsrc/main/java/org/sample/template/Example.java\n");
        Files.createDirectories(directory.resolve(".git"));
        Files.writeString(directory.resolve(".git/config"), "remote-marker");
        for (String root : List.of("src/main/java", "src/test/java", "src/native-test/java")) {
            var packageDirectory = directory.resolve(root + "/org/sample/template");
            Files.createDirectories(packageDirectory);
            Files.writeString(packageDirectory.resolve("Example.java"), "package org.sample.template; class Example {}\n");
        }
        Files.createDirectories(directory.resolve("src/main/proto"));
        Files.writeString(directory.resolve("src/main/proto/example.proto"), "package org.sample.template.v1;\noption java_package = \"org.sample.template\";\n");
    }

    private int setup(String... args) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
        command.add("tools/Setup.java");
        command.addAll(List.of(args));
        var process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        int result = process.waitFor();
        if (result != 0) System.out.println(output);
        return result;
    }

    @Test void renamesSourcesContractsAndDocsAndPreservesGit() throws Exception {
        fixture();
        assertEquals(0, setup("telemetry-ingest", "inpe.ccs", "2.1.0"));
        for (String root : List.of("src/main/java", "src/test/java", "src/native-test/java")) {
            assertTrue(Files.readString(directory.resolve(root + "/inpe/ccs/telemetry_ingest/Example.java")).contains("package inpe.ccs.telemetry_ingest;"));
            assertFalse(Files.exists(directory.resolve(root + "/org/sample/template/Example.java")));
        }
        assertTrue(Files.readString(directory.resolve("src/main/proto/example.proto")).contains("package inpe.ccs.telemetry_ingest.v1;"));
        assertTrue(Files.readString(directory.resolve("README.md")).contains("src/main/java/inpe/ccs/telemetry_ingest/Example.java"));
        assertTrue(Files.readString(directory.resolve("README.md")).contains("`telemetry-ingest`"));
        assertTrue(Files.readString(directory.resolve("gradle.properties")).contains("serviceVersion=2.1.0"));
        assertTrue(Files.readString(directory.resolve("gradle.properties")).contains("serviceLevel=1"));
        assertEquals("remote-marker", Files.readString(directory.resolve(".git/config")));
        String before = Files.readString(directory.resolve("README.md"));
        assertEquals(0, setup("telemetry-ingest", "inpe.ccs", "2.1.0"));
        assertEquals(before, Files.readString(directory.resolve("README.md")));
    }

    @Test void dryRunDoesNotChangeFiles() throws Exception {
        fixture();
        String before = Files.readString(directory.resolve("gradle.properties"));
        assertEquals(0, setup("--dry-run", "telemetry"));
        assertEquals(before, Files.readString(directory.resolve("gradle.properties")));
        assertTrue(Files.exists(directory.resolve("src/main/java/org/sample/template/Example.java")));
    }

    @Test void rejectsInvalidPackageBeforeAnyMutation() throws Exception {
        fixture();
        String before = Files.readString(directory.resolve("gradle.properties"));
        assertEquals(2, setup("class", "inpe.ccs"));
        assertEquals(2, setup("../outside", "inpe.ccs"));
        assertEquals(before, Files.readString(directory.resolve("gradle.properties")));
    }

    @Test void rejectsExistingDestinationBeforeAnyMutation() throws Exception {
        fixture();
        var destination = directory.resolve("src/main/java/inpe/ccs/telemetry/Example.java");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "existing-work");
        String before = Files.readString(directory.resolve("gradle.properties"));
        assertEquals(2, setup("telemetry", "inpe.ccs"));
        assertEquals(before, Files.readString(directory.resolve("gradle.properties")));
        assertEquals("existing-work", Files.readString(destination));
    }
}
