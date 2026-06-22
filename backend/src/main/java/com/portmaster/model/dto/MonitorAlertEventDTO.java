package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 监控告警事件 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorAlertEventDTO {

    private Integer port;
    private String protocol;
    private Boolean occupied;
    private String processName;
    private Long pid;
    private String remark;
    private String reason;
    private String timestamp;
}
