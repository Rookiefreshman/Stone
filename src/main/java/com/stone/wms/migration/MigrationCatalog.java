package com.stone.wms.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationCatalog {
    private static final Pattern MIGRATION_FILE = Pattern.compile("^V(\\d+)__([a-z0-9_]+)\\.sql$");

    private final List<DatabaseMigration> migrations;

    private MigrationCatalog(List<DatabaseMigration> migrations) {
        this.migrations = List.copyOf(migrations);
    }

    public static MigrationCatalog fromDirectory(Path migrationDirectory) {
        if (!Files.isDirectory(migrationDirectory)) {
            throw new IllegalArgumentException("migration directory does not exist: " + migrationDirectory);
        }
        try (var stream = Files.list(migrationDirectory)) {
            List<DatabaseMigration> migrations = stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(MigrationCatalog::parse)
                    .sorted(Comparator.comparingInt(DatabaseMigration::version))
                    .toList();
            validateVersions(migrations);
            return new MigrationCatalog(migrations);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load migrations from " + migrationDirectory, ex);
        }
    }

    public List<DatabaseMigration> migrations() {
        return migrations;
    }

    private static DatabaseMigration parse(Path path) {
        String fileName = path.getFileName().toString();
        Matcher matcher = MIGRATION_FILE.matcher(fileName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid migration file name: " + fileName);
        }
        return new DatabaseMigration(Integer.parseInt(matcher.group(1)), matcher.group(2), fileName);
    }

    private static void validateVersions(List<DatabaseMigration> migrations) {
        int previous = 0;
        for (DatabaseMigration migration : migrations) {
            if (migration.version() <= previous) {
                throw new IllegalArgumentException("migration versions must be strictly increasing at " + migration.fileName());
            }
            previous = migration.version();
        }
    }
}
