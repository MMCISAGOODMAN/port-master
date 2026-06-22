package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Docker 容器端口映射 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerContainerDTO {

    private String containerId;
    private String name;
    private String image;
    private String status;
    private List<PortMapping> portMappings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortMapping {
        private String hostPort;
        private String containerPort;
        private String protocol;
    }
}
