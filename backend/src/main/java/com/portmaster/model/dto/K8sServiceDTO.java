package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kubernetes Service 端口 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sServiceDTO {

    private String namespace;
    private String name;
    private String type;
    private String clusterIp;
    private List<PortMapping> ports;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortMapping {
        private String name;
        private Integer port;
        private Integer targetPort;
        private Integer nodePort;
        private String protocol;
    }
}
