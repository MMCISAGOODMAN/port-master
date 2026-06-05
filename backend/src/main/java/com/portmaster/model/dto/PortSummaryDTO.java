package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 端口扫描汇总统计 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortSummaryDTO {

    private Integer total;
    private Integer tcpCount;
    private Integer udpCount;
    private Integer listenCount;
    private Integer establishedCount;
    private Integer uniquePortCount;
    private Integer uniquePidCount;
    private Integer localhostCount;
    private Integer allInterfaceCount;
}
