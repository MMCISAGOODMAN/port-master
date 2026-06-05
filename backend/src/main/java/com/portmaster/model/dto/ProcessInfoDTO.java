package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进程列表项 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInfoDTO {

    private Long pid;
    private String processName;
    private String commandLine;
    private Double cpuPercent;
    private Double memoryPercent;
    private String memoryUsage;
    /** 绑定的端口数量 */
    private Integer portCount;
}
