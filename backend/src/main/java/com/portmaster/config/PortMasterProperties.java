package com.portmaster.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Port Master 全局配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "portmaster")
public class PortMasterProperties {

    private Monitor monitor = new Monitor();
    private Scan scan = new Scan();
    private Ssh ssh = new Ssh();

    @Data
    public static class Monitor {
        /** 监控轮询间隔（毫秒），前端可读取 */
        private long pollIntervalMs = 5000;
    }

    @Data
    public static class Scan {
        /** 扫描结果缓存 TTL（毫秒），0 表示不缓存 */
        private long cacheTtlMs = 3000;
    }

    @Data
    public static class Ssh {
        /** SSH 连接超时（毫秒） */
        private int connectTimeoutMs = 10000;
        /** 命令执行超时（秒） */
        private int commandTimeoutSec = 60;
    }
}
