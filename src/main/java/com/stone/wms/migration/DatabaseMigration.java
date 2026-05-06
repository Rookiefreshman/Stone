package com.stone.wms.migration;

public record DatabaseMigration(int version, String description, String fileName) {
}
