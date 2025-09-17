package ai.dat.boot;

import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.adapter.data.Column;
import ai.dat.core.adapter.data.Table;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.DatSeed;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.data.seed.SeedColumn;
import ai.dat.core.data.seed.SeedData;
import ai.dat.core.data.seed.SeedSpec;
import ai.dat.core.exception.ValidationException;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/9/12
 */
@Slf4j
public class ProjectSeeder {

    private final Path projectPath;
    private final Path seedsPath;

    private DatProject project;

    public ProjectSeeder(@NonNull Path projectPath) {
        this.projectPath = projectPath;
        this.seedsPath = projectPath.resolve(ProjectUtil.SEEDS_DIR_NAME);
    }

    public ProjectSeeder(@NonNull Path projectPath, @NonNull DatProject project) {
        this(projectPath);
        this.project = project;
    }

    public void seedAll() {
        Map<Path, DatSchema> schemas = ProjectUtil.loadAllSchema(seedsPath);
        Map<Path, DatSeed> seeds = ProjectUtil.loadAllSeed(seedsPath);
        validate(schemas, seeds);
        List<DatSeed> list = seeds.values().stream().toList();
        seed(schemas, list);
    }

    public void seedSelect(List<String> selects) {
        Map<Path, DatSchema> schemas = ProjectUtil.loadAllSchema(seedsPath);
        Map<Path, DatSeed> seeds = ProjectUtil.loadAllSeed(seedsPath);
        validate(schemas, seeds);
//        List<String> seedNames = seeds.values().stream().map(DatSeed::getName).toList();
//        List<String> notExists = selects.stream().filter(name -> !seedNames.contains(name)).toList();
//        Preconditions.checkArgument(notExists.isEmpty(), "Following seeds not exist: " + String.join(", ", notExists));
        List<DatSeed> list = seeds.values().stream().filter(seed -> selects.contains(seed.getName())).toList();
        seed(schemas, list);
    }

    public void seedExclude(List<String> excludes) {
        Map<Path, DatSchema> schemas = ProjectUtil.loadAllSchema(seedsPath);
        Map<Path, DatSeed> seeds = ProjectUtil.loadAllSeed(seedsPath);
        validate(schemas, seeds);
        List<DatSeed> list = seeds.values().stream().filter(seed -> !excludes.contains(seed.getName())).toList();
        seed(schemas, list);
    }

    private void seed(Map<Path, DatSchema> schemas, List<DatSeed> seeds) {
        if (project == null) {
            project = ProjectUtil.loadProject(projectPath);
        }

        Map<String, SeedSpec> seedSpecs = schemas.entrySet().stream()
                .filter(e -> !e.getValue().getSeeds().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSeeds()))
                .values()
                .stream()
                .flatMap(Collection::stream).toList().stream()
                .collect(Collectors.toMap(SeedSpec::getName, s -> s));

        seeds.forEach(seed -> {
            String name = seed.getName();
            SeedSpec seedSpec = seedSpecs.get(name);
            if (seedSpec != null) {
                validateCsvWithSpec(seed, seedSpec);
            }
        });

        DatabaseAdapter databaseAdapter = ProjectUtil.createDatabaseAdapter(project, projectPath);

        log.info("Total seeds: {}", seeds.size());
        System.out.println("🔢 Total seeds: " + seeds.size());
        seeds.forEach(seed -> {
            String name = seed.getName();
            long startTime = System.currentTimeMillis();
            log.info("Seeding '{}'...", name);
            System.out.print("Seeding '" + name + "' \t");
            AtomicBoolean isCompleted = new AtomicBoolean(false);
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                if (!isCompleted.get()) System.out.print("█");
            }, 0, 3, TimeUnit.SECONDS); // 3秒打印一次进度条
            try {
                SeedSpec seedSpec = seedSpecs.get(name);
                SeedData seedData = seedSpec != null ? parseCsvWithSpec(seed, seedSpec) : parseCsvWithoutSpec(seed);
                Table table = seedData.table();
                databaseAdapter.initTable(table, seedData.data());
            } catch (SQLException e) {
                isCompleted.set(true);
                scheduler.shutdown();
                throw new RuntimeException("Seeding '" + name + "' failed", e);
            } finally {
                isCompleted.set(true);
                scheduler.shutdown();
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            String formattedDuration = formatDuration(duration);
            log.info("Successfully seeded '{}' in {}", name, formattedDuration);
            System.out.println("\t[ " + formattedDuration + " ]");
        });
    }

    private void validate(Map<Path, DatSchema> schemas, Map<Path, DatSeed> seeds) {
        List<String> seedNames = seeds.values().stream().map(DatSeed::getName).toList();
        Map<Path, List<String>> missingNames = schemas.entrySet().stream()
                .filter(e -> !e.getValue().getSeeds().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSeeds()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(SeedSpec::getName)
                                .filter(name -> !seedNames.contains(name))
                                .toList()))
                .entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!missingNames.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            missingNames.forEach((path, missings) ->
                    sb.append(String.format("The CSV file corresponding to seeds %s does not exist " +
                                    "in the YAML file relative path: '%s'",
                            missings.stream().map(n -> String.format("'%s'", n))
                                    .collect(Collectors.joining(", ")),
                            projectPath.relativize(path).toString()
                    )).append("\n"));
            throw new ValidationException(sb.toString());
        }
    }

    private void validateCsvWithSpec(DatSeed seed, SeedSpec seedSpec) {
        String name = seed.getName();
        String delimiter = seedSpec.getConfig().getDelimiter();
        String[] lines = seed.getContent().split("\\r?\\n");
        Preconditions.checkArgument(lines.length > 0,
                "CSV content is empty for seed: " + name);
        List<String> headers = Arrays.stream(lines[0].split(delimiter))
                .map(String::trim).toList();
        List<String> notInHeader = seedSpec.getColumns().stream()
                .map(SeedColumn::getName)
                .filter(colName -> !headers.contains(colName))
                .toList();
        if (!notInHeader.isEmpty()) {
            throw new ValidationException(String.format(
                    "The columns defined do not exist in the CSV header: [%s], seed: %s",
                    String.join(", ", notInHeader), seedSpec.getName()
            ));
        }
    }

    /**
     * 使用SeedSpec配置解析CSV数据
     */
    private SeedData parseCsvWithSpec(DatSeed seed, SeedSpec seedSpec) {
        String name = seed.getName();
        String delimiter = seedSpec.getConfig().getDelimiter();
        String[] lines = seed.getContent().split("\\r?\\n");
        Preconditions.checkArgument(lines.length > 0,
                "CSV content is empty for seed: " + name);
        String[] headers = lines[0].split(delimiter);
        List<Column> columns = new ArrayList<>();
        for (String header : headers) {
            String trimmedHeader = header.trim();
            SeedColumn seedColumn = seedSpec.getColumns().stream()
                    .filter(col -> col.getName().equals(trimmedHeader))
                    .findFirst()
                    .orElse(null);
            String type = (seedColumn != null && !StringUtils.isBlank(seedColumn.getDataType())) ?
                    seedColumn.getDataType() : null;
            columns.add(new Column(trimmedHeader, type));
        }
        List<List<String>> data = parseCsvData(lines, delimiter);
        return new SeedData(new Table(name, columns), data);
    }

    private SeedData parseCsvWithoutSpec(DatSeed seed) {
        String name = seed.getName();
        String delimiter = ","; // 默认使用逗号分隔符
        String[] lines = seed.getContent().split("\\r?\\n");
        Preconditions.checkArgument(lines.length > 0,
                "CSV content is empty for seed: " + name);
        String[] headers = lines[0].split(delimiter);
        List<Column> columns = new ArrayList<>();
        for (String header : headers) {
            String trimmedHeader = header.trim();
            columns.add(new Column(trimmedHeader));
        }
        List<List<String>> data = parseCsvData(lines, delimiter);
        return new SeedData(new Table(name, columns), data);
    }

    private List<List<String>> parseCsvData(String[] lines, String delimiter) {
        List<List<String>> data = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty()) {
                String[] values = line.split(delimiter);
                data.add(Arrays.asList(values));
            }
        }
        return data;
    }

    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        }
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
            minutes = minutes % 60;
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
            seconds = seconds % 60;
        }
        if (seconds > 0 || sb.isEmpty()) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }
}
