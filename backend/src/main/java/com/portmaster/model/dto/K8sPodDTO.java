package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kubernetes Pod 端口 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sPodDTO {

    private String namespace;
    private String name;
    private String status;
    private String node;
    private String podIp;
    private List<PortInfo> ports;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortInfo {
        private Integer containerPort;
        private String protocol;
        private String name;
        private String hostPort;
    }
}
