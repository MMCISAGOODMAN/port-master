package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 端口连接信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortInfoDTO {

    /** 协议 TCP/UDP */
    private String protocol;

    /** 端口号 */
    private Integer port;

    /** 本地地址 */
    private String localAddress;

    /** 外部/远程地址 */
    private String foreignAddress;

    /** 进程 PID */
    private Long pid;

    /** 进程名称 */
    private String processName;

    /** 程序完整路径 */
    private String programPath;

    /** 连接状态 LISTEN/ESTABLISHED/TIME_WAIT/CLOSE_WAIT 等 */
    private String state;
}
