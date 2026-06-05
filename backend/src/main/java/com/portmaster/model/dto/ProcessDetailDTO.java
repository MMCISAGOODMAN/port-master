package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 进程详情 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDetailDTO {

    private Long pid;
    private String processName;
    private String programPath;
    private String commandLine;
    private Double cpuPercent;
    private Double memoryPercent;
    private String memoryUsage;
    private String createTime;
    private List<PortInfoDTO> boundPorts;
}
