package com.stone.wms.migration;

import java.nio.file.Path;
import java.util.Objects;

class MigrationCatalogTest {
    public static void main(String[] args) {
        MigrationCatalog catalog = MigrationCatalog.fromDirectory(Path.of("src/main/resources/db/migration"));

        assertEquals(2, catalog.migrations().size());
        assertEquals(1, catalog.migrations().get(0).version());
        assertEquals("init_wms_core", catalog.migrations().get(0).description());
        assertEquals(2, catalog.migrations().get(1).version());
        assertEquals("inventory_query_indexes", catalog.migrations().get(1).description());
        System.out.println("MigrationCatalogTest passed");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected %s but got %s".formatted(expected, actual));
        }
    }
}
