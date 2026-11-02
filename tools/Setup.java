import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.lang.model.SourceVersion;

/** Portable setup, executed in Java source-file mode by setup.sh and setup.bat. */
public class Setup {
    public static void main(String[] arguments) throws Exception {
        try {
            configure(arguments);
        } catch (IllegalArgumentException e) {
            System.err.println("Setup: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void configure(String[] arguments) throws IOException {
        var args = new ArrayList<>(List.of(arguments));
        if (args.remove("--help") || args.remove("-h")) {
            System.out.println("Uso: setup.sh|setup.bat [--dry-run] <nome-do-servico> [grupo] [versao]");
            System.out.println("Exemplo: setup.sh telemetry-ingest inpe.ccs 1.0.0-SNAPSHOT");
            return;
        }
        boolean dryRun = args.remove("--dry-run");
        require(args.size() <= 3, "Informe nome, grupo e versao, no maximo.");
        if (args.isEmpty()) {
            System.out.print("Nome do servico: ");
            var input = new Scanner(System.in, StandardCharsets.UTF_8);
            require(input.hasNextLine(), "Nome do servico obrigatorio.");
            args.add(input.nextLine().strip());
        }
        Path root = Path.of("").toAbsolutePath().normalize();
        Path propertiesFile = root.resolve("gradle.properties");
        require(Files.isRegularFile(propertiesFile), "Execute o setup na raiz do template.");
        String propertiesText = Files.readString(propertiesFile);
        var properties = new Properties();
        properties.load(new java.io.StringReader(propertiesText));
        String name = args.get(0);
        String group = args.size() > 1 ? args.get(1) : properties.getProperty("groupId", "inpe.ccs");
        String version = args.size() > 2 ? args.get(2) : properties.getProperty("serviceVersion", "1.0.0-SNAPSHOT");
        require(name.matches("[a-z][a-z0-9-]{0,62}"), "Nome: 1 a 63 caracteres; letras minusculas, numeros e hifens.");
        require(group.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*"), "Grupo Java invalido.");
        String basePackage = group + "." + name.replace('-', '_');
        require(SourceVersion.isName(basePackage), "Pacote contem palavra reservada do Java.");
        require(version.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9][A-Za-z0-9.-]*)?"), "Versao invalida; exemplo: 1.0.0-SNAPSHOT.");
        String oldName = properties.getProperty("serviceName");
        String oldPackage = properties.getProperty("basePackage");
        require(oldName != null && !oldName.isBlank() && oldPackage != null && SourceVersion.isName(oldPackage),
                "serviceName/basePackage invalidos em gradle.properties.");
        String updatedProperties = property(propertiesText, "serviceName", name);
        updatedProperties = property(updatedProperties, "groupId", group);
        updatedProperties = property(updatedProperties, "basePackage", basePackage);
        updatedProperties = property(updatedProperties, "serviceVersion", version);
        var changes = new LinkedHashMap<Path, Change>();
        changes.put(propertiesFile, new Change(propertiesFile, updatedProperties));
        var candidates = new ArrayList<Path>();
        candidates.add(root.resolve("README.md"));
        for (String directory : List.of("src", "docs")) {
            Path path = root.resolve(directory);
            if (!Files.isDirectory(path)) continue;
            try (var stream = Files.walk(path)) {
                candidates.addAll(stream.filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                        .filter(Setup::isText).toList());
            }
        }
        for (Path file : candidates) {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
            String original = Files.readString(file);
            String text = original.replace(oldPackage, basePackage)
                    .replace(oldPackage.replace('.', '/'), basePackage.replace('.', '/')).replace(oldName, name);
            // Update the README identity cell, but preserve its purpose/owner placeholders.
            if (file.equals(root.resolve("README.md"))) {
                text = text.replace("<!-- service-name -->A preencher<!-- /service-name -->",
                        "<!-- service-name -->`" + name + "`<!-- /service-name -->");
            }
            Path destination = file;
            for (String sourceRoot : List.of("src/main/java", "src/test/java", "src/native-test/java")) {
                Path oldRoot = root.resolve(sourceRoot).resolve(oldPackage.replace('.', '/'));
                if (file.startsWith(oldRoot)) {
                    destination = root.resolve(sourceRoot).resolve(basePackage.replace('.', '/')).resolve(oldRoot.relativize(file));
                    break;
                }
            }
            if (!original.equals(text) || !file.equals(destination)) changes.put(file, new Change(destination, text));
        }
        // Preflight every destination before changing any file.
        var destinations = new HashSet<Path>();
        for (var entry : changes.entrySet()) {
            Path destination = entry.getValue().destination;
            require(destinations.add(destination), "Destino duplicado: " + root.relativize(destination));
            require(entry.getKey().equals(destination) || !Files.exists(destination), "Destino ja existe: " + root.relativize(destination));
        }
        System.out.printf("Servico: %s%nGrupo: %s%nPacote: %s%nVersao: %s%nNivel: %s%n", name, group, basePackage, version,
                properties.getProperty("serviceLevel", "1"));
        if (dryRun) {
            changes.forEach((source, change) -> System.out.println(root.relativize(source) + " -> " + root.relativize(change.destination)));
            System.out.println("Simulacao concluida; nenhum arquivo alterado.");
            return;
        }
        // Source files are retained until all new files have been written successfully.
        for (var entry : changes.entrySet()) {
            Files.createDirectories(entry.getValue().destination.getParent());
            Files.writeString(entry.getValue().destination, entry.getValue().text);
        }
        for (var entry : changes.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().destination)) Files.delete(entry.getKey());
        }
        try (var stream = Files.walk(root.resolve("src"))) {
            for (Path directory : stream.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList()) {
                if (directory.equals(root.resolve("src"))) continue;
                try (var children = Files.list(directory)) {
                    if (children.findAny().isEmpty()) Files.delete(directory);
                }
            }
        }
        System.out.println("Configuracao concluida. Proximo passo: ./gradlew test (Windows: gradlew.bat test).");
        System.out.println("A pasta do repositorio, o historico Git e o remote foram preservados.");
    }

    private static boolean isText(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".proto") || name.endsWith(".properties")
                || name.endsWith(".md") || name.endsWith(".yml") || name.endsWith(".yaml") || name.startsWith("Dockerfile");
    }
    private static String property(String text, String key, String value) {
        Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(key) + "=.*$");
        require(pattern.matcher(text).find(), "Propriedade ausente: " + key);
        return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(key + "=" + value));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    private record Change(Path destination, String text) {}
}
