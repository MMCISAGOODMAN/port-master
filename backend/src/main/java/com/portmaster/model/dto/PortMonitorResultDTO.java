package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 端口监控探测结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortMonitorResultDTO {

    private Integer port;
    private String protocol;
    private Boolean occupied;
    private String processName;
    private Long pid;
    private String state;
    private String remark;
}
