package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统监控大盘 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDTO {

    /** CPU 使用率百分比 */
    private Double cpuUsage;

    /** 内存使用率百分比 */
    private Double memoryUsage;

    /** 已用内存 (MB) */
    private Double memoryUsedMb;

    /** 总内存 (MB) */
    private Double memoryTotalMb;

    /** 监听端口总数 */
    private Integer listenPortCount;

    /** 活跃连接数 */
    private Integer activeConnectionCount;

    /** 运行进程数量 */
    private Integer processCount;

    /** 操作系统类型 */
    private String osType;

    /** 是否需要管理员权限提示 */
    private Boolean needAdminHint;
}
