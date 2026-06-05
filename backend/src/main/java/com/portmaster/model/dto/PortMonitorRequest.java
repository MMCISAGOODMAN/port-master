package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 端口监控探测请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortMonitorRequest {

    private List<MonitorPortItem> ports;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitorPortItem {
        private Integer port;
        private String protocol;
        private String remark;
        /** 期望状态: occupied / free / any */
        private String expectedState;
    }
}
