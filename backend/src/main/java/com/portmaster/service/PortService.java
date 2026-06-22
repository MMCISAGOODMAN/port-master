package com.portmaster.service;

import com.portmaster.config.PortMasterProperties;
import com.portmaster.model.dto.*;
import com.portmaster.util.OsDetector;
import com.portmaster.util.parser.UnixPortParser;
import com.portmaster.util.parser.WindowsPortParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 端口扫描与查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortService {

    private final PortMasterProperties properties;
    private final WindowsPortParser windowsPortParser = new WindowsPortParser();
    private final UnixPortParser unixPortParser = new UnixPortParser();

    private volatile List<PortInfoDTO> cachedScan;
    private volatile long cacheTimestamp;

    /**
     * 全量扫描本机全部 TCP/UDP 端口
     */
    public List<PortInfoDTO> scanAllPorts() {
        return scanAllPorts(false);
    }

    /**
     * 全量扫描，forceRefresh=true 时跳过缓存
     */
    public List<PortInfoDTO> scanAllPorts(boolean forceRefresh) {
        long ttl = properties.getScan().getCacheTtlMs();
        if (!forceRefresh && ttl > 0 && cachedScan != null
                && System.currentTimeMillis() - cacheTimestamp < ttl) {
            return cachedScan;
        }

        List<PortInfoDTO> result;
        if (OsDetector.isWindows()) {
            result = windowsPortParser.scanAllPorts();
        } else {
            result = unixPortParser.scanAllPorts();
        }

        if (ttl > 0) {
            cachedScan = result;
            cacheTimestamp = System.currentTimeMillis();
        }
        return result;
    }

    /** 清除扫描缓存 */
    public void invalidateCache() {
        cachedScan = null;
        cacheTimestamp = 0;
    }

    /**
     * 单端口查询
     */
    public List<PortInfoDTO> queryByPort(int port) {
        return scanAllPorts().stream()
                .filter(p -> Objects.equals(p.getPort(), port))
                .collect(Collectors.toList());
    }

    /**
     * 批量端口查询（逗号分隔，支持范围如 8000-8100,9090）
     */
    public List<PortInfoDTO> queryByPorts(String ports) {
        Set<Integer> portSet = parsePortSpec(ports);
        return scanAllPorts().stream()
                .filter(p -> portSet.contains(p.getPort()))
                .collect(Collectors.toList());
    }

    /**
     * 端口范围查询
     */
    public List<PortInfoDTO> queryByRange(int start, int end) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        if (max - min > 10000) {
            throw new IllegalArgumentException("端口范围不能超过 10000");
        }
        return scanAllPorts().stream()
                .filter(p -> p.getPort() != null && p.getPort() >= min && p.getPort() <= max)
                .collect(Collectors.toList());
    }

    /**
     * 解析端口规格：8080,9090 或 8000-8100
     */
    public Set<Integer> parsePortSpec(String ports) {
        Set<Integer> result = new HashSet<>();
        for (String part : ports.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (part.contains("-")) {
                String[] range = part.split("-", 2);
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                int min = Math.min(start, end);
                int max = Math.max(start, end);
                for (int i = min; i <= max && i <= 65535; i++) {
                    result.add(i);
                }
            } else {
                result.add(Integer.parseInt(part));
            }
        }
        return result;
    }

    /**
     * 检测端口冲突（同一端口被多个进程监听）
     */
    public List<PortConflictDTO> detectConflicts() {
        List<PortInfoDTO> allPorts = scanAllPorts();
        Map<String, List<PortInfoDTO>> listenMap = allPorts.stream()
                .filter(p -> "LISTEN".equalsIgnoreCase(p.getState()) && p.getPort() != null && p.getPid() != null)
                .collect(Collectors.groupingBy(p -> p.getProtocol() + ":" + p.getPort()));

        List<PortConflictDTO> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<PortInfoDTO>> entry : listenMap.entrySet()) {
            Set<Long> pids = entry.getValue().stream()
                    .map(PortInfoDTO::getPid)
                    .collect(Collectors.toSet());
            if (pids.size() > 1) {
                PortInfoDTO first = entry.getValue().get(0);
                List<String> names = entry.getValue().stream()
                        .map(PortInfoDTO::getProcessName)
                        .distinct()
                        .collect(Collectors.toList());
                conflicts.add(PortConflictDTO.builder()
                        .port(first.getPort())
                        .protocol(first.getProtocol())
                        .pids(new ArrayList<>(pids))
                        .processNames(names)
                        .message(String.format("端口 %d (%s) 被 %d 个进程同时监听",
                                first.getPort(), first.getProtocol(), pids.size()))
                        .build());
            }
        }
        return conflicts;
    }

    /**
     * 根据进程名称反查占用端口
     */
    public List<PortInfoDTO> queryByProcessName(String processName) {
        String keyword = processName.toLowerCase();
        return scanAllPorts().stream()
                .filter(p -> {
                    if (p.getProcessName() != null && p.getProcessName().toLowerCase().contains(keyword)) {
                        return true;
                    }
                    if (p.getProgramPath() != null && p.getProgramPath().toLowerCase().contains(keyword)) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据 PID 反查绑定端口
     */
    public List<PortInfoDTO> queryByPid(long pid) {
        return scanAllPorts().stream()
                .filter(p -> Objects.equals(p.getPid(), pid))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有已占用端口集合
     */
    public Set<Integer> getOccupiedPorts() {
        return scanAllPorts().stream()
                .map(PortInfoDTO::getPort)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 空闲端口生成器
     */
    public FreePortResultDTO generateFreePorts(int startPort, int count) {
        if (startPort < 1 || startPort > 65535) {
            return FreePortResultDTO.builder()
                    .startPort(startPort)
                    .count(count)
                    .freePorts(Collections.emptyList())
                    .message("起始端口必须在 1-65535 范围内")
                    .build();
        }
        if (count < 1 || count > 100) {
            return FreePortResultDTO.builder()
                    .startPort(startPort)
                    .count(count)
                    .freePorts(Collections.emptyList())
                    .message("端口数量必须在 1-100 范围内")
                    .build();
        }

        Set<Integer> occupied = getOccupiedPorts();
        List<Integer> freePorts = new ArrayList<>();
        int current = startPort;

        while (freePorts.size() < count && current <= 65535) {
            if (!occupied.contains(current)) {
                freePorts.add(current);
            }
            current++;
        }

        String message = freePorts.size() < count
                ? "仅找到 " + freePorts.size() + " 个连续空闲端口（已达端口上限）"
                : "成功找到 " + freePorts.size() + " 个空闲端口";

        return FreePortResultDTO.builder()
                .startPort(startPort)
                .count(count)
                .freePorts(freePorts)
                .message(message)
                .build();
    }

    /**
     * 端口监控探测
     */
    public List<PortMonitorResultDTO> monitorPorts(PortMonitorRequest request) {
        List<PortInfoDTO> allPorts = scanAllPorts();
        Map<Integer, List<PortInfoDTO>> portMap = allPorts.stream()
                .filter(p -> p.getPort() != null)
                .collect(Collectors.groupingBy(PortInfoDTO::getPort));

        List<PortMonitorResultDTO> results = new ArrayList<>();
        if (request.getPorts() == null) {
            return results;
        }

        for (PortMonitorRequest.MonitorPortItem item : request.getPorts()) {
            List<PortInfoDTO> matched = portMap.getOrDefault(item.getPort(), Collections.emptyList());
            boolean occupied = !matched.isEmpty();

            PortMonitorResultDTO result = PortMonitorResultDTO.builder()
                    .port(item.getPort())
                    .protocol(item.getProtocol())
                    .occupied(occupied)
                    .remark(item.getRemark())
                    .build();

            if (occupied) {
                PortInfoDTO first = matched.get(0);
                result.setProcessName(first.getProcessName());
                result.setPid(first.getPid());
                result.setState(first.getState());
            } else {
                result.setState("FREE");
            }

            results.add(result);
        }
        return results;
    }

    /**
     * 扫描结果汇总统计
     */
    public PortSummaryDTO getSummary() {
        List<PortInfoDTO> all = scanAllPorts();
        Set<Integer> uniquePorts = new HashSet<>();
        Set<Long> uniquePids = new HashSet<>();
        int tcp = 0, udp = 0, listen = 0, established = 0, localhost = 0, allIf = 0;

        for (PortInfoDTO p : all) {
            if (p.getPort() != null) uniquePorts.add(p.getPort());
            if (p.getPid() != null) uniquePids.add(p.getPid());
            if ("TCP".equalsIgnoreCase(p.getProtocol())) tcp++;
            if ("UDP".equalsIgnoreCase(p.getProtocol())) udp++;
            if ("LISTEN".equalsIgnoreCase(p.getState())) listen++;
            if ("ESTABLISHED".equalsIgnoreCase(p.getState())) established++;
            String addr = p.getLocalAddress() != null ? p.getLocalAddress().toLowerCase() : "";
            if (addr.contains("127.0.0.1") || addr.contains("localhost") || addr.startsWith("[")) {
                if (addr.contains("127.0.0.1") || addr.contains("localhost")) localhost++;
            }
            if (addr.startsWith("*:") || addr.startsWith("0.0.0.0:") || addr.contains("*.")) {
                allIf++;
            }
        }

        return PortSummaryDTO.builder()
                .total(all.size())
                .tcpCount(tcp)
                .udpCount(udp)
                .listenCount(listen)
                .establishedCount(established)
                .uniquePortCount(uniquePorts.size())
                .uniquePidCount(uniquePids.size())
                .localhostCount(localhost)
                .allInterfaceCount(allIf)
                .build();
    }
}
