package com.stone.wms.startup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WMS 启动入口，集中配置组件扫描和 MyBatis Mapper 扫描。
 */
@SpringBootApplication(scanBasePackages = "com.stone.wms")
@MapperScan("com.stone.wms.infrastructure.mapper")
public class WmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsApplication.class, args);
    }
}
