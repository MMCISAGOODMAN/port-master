package com.portmaster.service;

import com.portmaster.model.dto.DockerContainerDTO;
import com.portmaster.util.CommandExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker 容器端口映射服务
 */
@Slf4j
@Service
public class DockerService {

    private static final Pattern PORT_MAPPING = Pattern.compile(
            "(?:(?<hostIp>[\\d.]+):)?(?<hostPort>\\d+)->(?<containerPort>\\d+)/(?<protocol>tcp|udp)", Pattern.CASE_INSENSITIVE);

    /**
     * 检查 Docker 是否可用
     */
    public boolean isDockerAvailable() {
        return CommandExecutor.isCommandAvailable("docker");
    }

    /**
     * 列出运行中的容器及其端口映射
     */
    public List<DockerContainerDTO> listContainers(boolean all) {
        if (!isDockerAvailable()) {
            return List.of();
        }

        String flag = all ? "-a" : "";
        List<String> lines;
        try {
            lines = CommandExecutor.executeShell(
                    "docker ps " + flag + " --format '{{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'");
        } catch (Exception e) {
            log.warn("docker ps failed", e);
            return List.of();
        }

        List<DockerContainerDTO> containers = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", 5);
            if (parts.length < 4) continue;

            String portsStr = parts.length > 4 ? parts[4] : "";
            containers.add(DockerContainerDTO.builder()
                    .containerId(parts[0])
                    .name(parts[1])
                    .image(parts[2])
                    .status(parts[3])
                    .portMappings(parsePortMappings(portsStr))
                    .build());
        }
        return containers;
    }

    /**
     * 停止容器
     */
    public String stopContainer(String containerId) {
        if (!isDockerAvailable()) {
            throw new IllegalStateException("Docker 不可用，请确认已安装并运行 Docker");
        }
        CommandExecutor.execute("docker", "stop", containerId);
        return "容器 " + containerId + " 已停止";
    }

    /**
     * 重启容器
     */
    public String restartContainer(String containerId) {
        if (!isDockerAvailable()) {
            throw new IllegalStateException("Docker 不可用");
        }
        CommandExecutor.execute("docker", "restart", containerId);
        return "容器 " + containerId + " 已重启";
    }

    private List<DockerContainerDTO.PortMapping> parsePortMappings(String portsStr) {
        List<DockerContainerDTO.PortMapping> mappings = new ArrayList<>();
        if (portsStr == null || portsStr.isBlank()) return mappings;

        for (String segment : portsStr.split(",")) {
            Matcher m = PORT_MAPPING.matcher(segment.trim());
            if (m.find()) {
                mappings.add(DockerContainerDTO.PortMapping.builder()
                        .hostPort(m.group("hostPort"))
                        .containerPort(m.group("containerPort"))
                        .protocol(m.group("protocol").toUpperCase())
                        .build());
            }
        }
        return mappings;
    }
}
