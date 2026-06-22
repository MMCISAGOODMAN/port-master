package com.portmaster.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portmaster.model.dto.K8sPodDTO;
import com.portmaster.model.dto.K8sServiceDTO;
import com.portmaster.util.CommandExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Kubernetes Pod/Service 端口查询服务（基于 kubectl）
 */
@Slf4j
@Service
public class K8sService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isKubectlAvailable() {
        return CommandExecutor.isCommandAvailable("kubectl");
    }

    /**
     * 列出 Pod 及容器端口
     */
    public List<K8sPodDTO> listPods(String namespace) {
        if (!isKubectlAvailable()) {
            return List.of();
        }

        String nsFlag = (namespace != null && !namespace.isBlank()) ? "-n " + namespace : "-A";
        List<String> lines;
        try {
            lines = CommandExecutor.executeShell("kubectl get pods " + nsFlag + " -o json");
        } catch (Exception e) {
            log.warn("kubectl get pods failed", e);
            return List.of();
        }

        return parsePods(String.join("\n", lines));
    }

    /**
     * 列出 Service 及端口映射
     */
    public List<K8sServiceDTO> listServices(String namespace) {
        if (!isKubectlAvailable()) {
            return List.of();
        }

        String nsFlag = (namespace != null && !namespace.isBlank()) ? "-n " + namespace : "-A";
        List<String> lines;
        try {
            lines = CommandExecutor.executeShell("kubectl get svc " + nsFlag + " -o json");
        } catch (Exception e) {
            log.warn("kubectl get svc failed", e);
            return List.of();
        }

        return parseServices(String.join("\n", lines));
    }

    /**
     * 获取当前 context 信息
     */
    public String getCurrentContext() {
        if (!isKubectlAvailable()) return "";
        try {
            List<String> lines = CommandExecutor.execute("kubectl", "config", "current-context");
            return lines.isEmpty() ? "" : lines.get(0).trim();
        } catch (Exception e) {
            return "";
        }
    }

    List<K8sPodDTO> parsePods(String json) {
        List<K8sPodDTO> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            if (!items.isArray()) return result;

            for (JsonNode item : items) {
                String ns = item.path("metadata").path("namespace").asText("");
                String name = item.path("metadata").path("name").asText("");
                String phase = item.path("status").path("phase").asText("");
                String node = item.path("spec").path("nodeName").asText("-");
                String podIp = item.path("status").path("podIP").asText("-");

                List<K8sPodDTO.PortInfo> ports = new ArrayList<>();
                JsonNode containers = item.path("spec").path("containers");
                if (containers.isArray()) {
                    for (JsonNode container : containers) {
                        JsonNode containerPorts = container.path("ports");
                        if (containerPorts.isArray()) {
                            for (JsonNode p : containerPorts) {
                                ports.add(K8sPodDTO.PortInfo.builder()
                                        .containerPort(p.path("containerPort").asInt(0))
                                        .protocol(p.path("protocol").asText("TCP"))
                                        .name(p.path("name").asText(""))
                                        .hostPort(p.path("hostPort").isMissingNode()
                                                ? null : String.valueOf(p.path("hostPort").asInt()))
                                        .build());
                            }
                        }
                    }
                }

                result.add(K8sPodDTO.builder()
                        .namespace(ns)
                        .name(name)
                        .status(phase)
                        .node(node)
                        .podIp(podIp)
                        .ports(ports)
                        .build());
            }
        } catch (Exception e) {
            log.warn("parse pods json failed", e);
        }
        return result;
    }

    List<K8sServiceDTO> parseServices(String json) {
        List<K8sServiceDTO> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            if (!items.isArray()) return result;

            for (JsonNode item : items) {
                String ns = item.path("metadata").path("namespace").asText("");
                String name = item.path("metadata").path("name").asText("");
                String type = item.path("spec").path("type").asText("ClusterIP");
                String clusterIp = item.path("spec").path("clusterIP").asText("-");

                List<K8sServiceDTO.PortMapping> ports = new ArrayList<>();
                JsonNode svcPorts = item.path("spec").path("ports");
                if (svcPorts.isArray()) {
                    for (JsonNode p : svcPorts) {
                        ports.add(K8sServiceDTO.PortMapping.builder()
                                .name(p.path("name").asText(""))
                                .port(p.path("port").asInt(0))
                                .targetPort(parseTargetPort(p.path("targetPort")))
                                .nodePort(p.path("nodePort").isMissingNode() ? null : p.path("nodePort").asInt())
                                .protocol(p.path("protocol").asText("TCP"))
                                .build());
                    }
                }

                result.add(K8sServiceDTO.builder()
                        .namespace(ns)
                        .name(name)
                        .type(type)
                        .clusterIp(clusterIp)
                        .ports(ports)
                        .build());
            }
        } catch (Exception e) {
            log.warn("parse services json failed", e);
        }
        return result;
    }

    private Integer parseTargetPort(JsonNode node) {
        if (node.isMissingNode()) return null;
        if (node.isInt()) return node.asInt();
        return null;
    }
}
