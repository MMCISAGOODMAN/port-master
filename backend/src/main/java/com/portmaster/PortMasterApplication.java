package com.portmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Port Master 应用入口
 * 端口与进程管理工具 - 跨平台 B/S 架构
 */
@SpringBootApplication
@EnableScheduling
public class PortMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortMasterApplication.class, args);
    }
}
