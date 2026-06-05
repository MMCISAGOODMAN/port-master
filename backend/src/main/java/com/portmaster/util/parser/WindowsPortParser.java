package com.portmaster.util.parser;

import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.util.CommandExecutor;
import com.portmaster.util.OsDetector;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Windows 端口解析器 - 使用 netstat、tasklist、wmic
 */
@Slf4j
public class WindowsPortParser {

    private static final Pattern NETSTAT_LINE = Pattern.compile(
            "^(?<proto>\\S+)\\s+(?<local>\\S+)\\s+(?<foreign>\\S+)\\s+(?<state>\\S+)\\s+(?<pid>\\d+|\\*)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * 扫描全部 TCP/UDP 端口
     */
    public List<PortInfoDTO> scanAllPorts() {
        List<String> tcpLines = CommandExecutor.execute("netstat", "-ano");
        List<String> udpLines = CommandExecutor.execute("netstat", "-ano", "-p", "udp");

        Map<Long, String> pidNameMap = buildPidNameMap();
        Map<Long, String> pidPathMap = buildPidPathMap(new ArrayList<>(pidNameMap.keySet()));

        Set<String> seen = new HashSet<>();
        List<PortInfoDTO> result = new ArrayList<>();

        parseNetstatLines(tcpLines, pidNameMap, pidPathMap, seen, result);
        parseNetstatLines(udpLines, pidNameMap, pidPathMap, seen, result);

        return result;
    }

    private void parseNetstatLines(List<String> lines, Map<Long, String> pidNameMap,
                                   Map<Long, String> pidPathMap, Set<String> seen,
                                   List<PortInfoDTO> result) {
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("Active") || line.startsWith("Proto")) {
                continue;
            }
            Matcher m = NETSTAT_LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }

            String proto = m.group("proto").toUpperCase();
            if (!proto.startsWith("TCP") && !proto.startsWith("UDP")) {
                continue;
            }
            String protocol = proto.startsWith("TCP") ? "TCP" : "UDP";

            String local = m.group("local");
            String foreign = m.group("foreign");
            String state = m.group("state");
            String pidStr = m.group("pid");

            Integer port = extractPort(local);
            if (port == null) {
                continue;
            }

            Long pid = "*".equals(pidStr) ? null : Long.parseLong(pidStr);
            String key = protocol + ":" + port + ":" + local + ":" + foreign + ":" + state + ":" + pid;
            if (!seen.add(key)) {
                continue;
            }

            String processName = pid != null ? pidNameMap.getOrDefault(pid, "") : "";
            String programPath = pid != null ? pidPathMap.getOrDefault(pid, "") : "";

            if ("UDP".equals(protocol) && ("*".equals(state) || state.matches("\\d+"))) {
                state = "LISTEN";
            }

            result.add(PortInfoDTO.builder()
                    .protocol(protocol)
                    .port(port)
                    .localAddress(local)
                    .foreignAddress(foreign)
                    .pid(pid)
                    .processName(processName)
                    .programPath(programPath)
                    .state(normalizeState(state))
                    .build());
        }
    }

    /**
     * 构建 PID -> 进程名 映射
     */
    public Map<Long, String> buildPidNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            List<String> lines = CommandExecutor.execute("tasklist", "/FO", "CSV", "/NH");
            for (String line : lines) {
                String[] parts = parseCsvLine(line);
                if (parts.length >= 2) {
                    try {
                        String name = parts[0].replace("\"", "");
                        long pid = Long.parseLong(parts[1].replace("\"", ""));
                        map.put(pid, name);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get tasklist", e);
        }
        return map;
    }

    /**
     * 批量获取进程路径
     */
    public Map<Long, String> buildPidPathMap(List<Long> pids) {
        Map<Long, String> map = new HashMap<>();
        if (pids.isEmpty()) {
            return map;
        }
        // 分批查询避免命令过长
        int batchSize = 50;
        for (int i = 0; i < pids.size(); i += batchSize) {
            List<Long> batch = pids.subList(i, Math.min(i + batchSize, pids.size()));
            String pidFilter = batch.stream().map(String::valueOf).collect(Collectors.joining(" or ProcessId="));
            try {
                List<String> lines = CommandExecutor.execute("wmic", "process", "where",
                        "ProcessId=" + pidFilter, "get", "ProcessId,ExecutablePath", "/FORMAT:CSV");
                for (String line : lines) {
                    if (line.contains("ProcessId") || line.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        try {
                            long pid = Long.parseLong(parts[2].trim());
                            String path = parts.length > 3 ? parts[3].trim() : "";
                            map.put(pid, path);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("WMIC query failed for batch", e);
            }
        }
        return map;
    }

    /**
     * 获取单个进程详情
     */
    public String getProcessPath(long pid) {
        try {
            List<String> lines = CommandExecutor.execute("wmic", "process", "where",
                    "ProcessId=" + pid, "get", "ExecutablePath", "/VALUE");
            for (String line : lines) {
                if (line.startsWith("ExecutablePath=")) {
                    return line.substring("ExecutablePath=".length()).trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get path for pid {}", pid, e);
        }
        return "";
    }

    public String getProcessCommandLine(long pid) {
        try {
            List<String> lines = CommandExecutor.execute("wmic", "process", "where",
                    "ProcessId=" + pid, "get", "CommandLine", "/VALUE");
            for (String line : lines) {
                if (line.startsWith("CommandLine=")) {
                    return line.substring("CommandLine=".length()).trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get command line for pid {}", pid, e);
        }
        return "";
    }

    public static Integer extractPort(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        int idx = address.lastIndexOf(':');
        if (idx < 0) {
            return null;
        }
        try {
            return Integer.parseInt(address.substring(idx + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeState(String state) {
        if (state == null || state.isEmpty() || "*".equals(state)) {
            return "UNKNOWN";
        }
        return state.toUpperCase();
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
