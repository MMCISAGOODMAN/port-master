package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 端口连通性探测结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortProbeResultDTO {

    private String host;
    private Integer port;
    private String protocol;
    /** 是否可连通 */
    private Boolean reachable;
    /** 响应耗时 ms */
    private Long latencyMs;
    private String message;
    /** 探测类型: TCP / HTTP / TLS */
    private String probeType;
    /** HTTP 状态码 */
    private Integer httpStatus;
    /** TLS 证书信息 */
    private String certInfo;
}
