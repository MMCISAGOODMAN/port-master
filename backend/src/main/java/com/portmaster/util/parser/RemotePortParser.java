package com.portmaster.util.parser;

import com.portmaster.model.dto.PortInfoDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 远程 SSH 端口输出解析器
 * 解析 ss / netstat / lsof 命令输出
 */
@Slf4j
public class RemotePortParser {

    private static final Pattern NETSTAT_WIN = Pattern.compile(
            "\\s*(?<proto>TCP|UDP)\\s+(?<local>\\S+):(?<port>\\d+)\\s+(?<foreign>\\S+):(?<fport>\\S+)\\s+(?<state>\\S+)\\s+(?<pid>\\d+)");

    /**
     * 解析远程命令输出为端口列表
     */
    public List<PortInfoDTO> parse(String osType, List<String> lines) {
        if ("windows".equalsIgnoreCase(osType)) {
            return parseWindowsNetstat(lines);
        }
        return parseUnixSsOrNetstat(lines);
    }

    private List<PortInfoDTO> parseUnixSsOrNetstat(List<String> lines) {
        List<PortInfoDTO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("Netid") || line.startsWith("State")
                    || line.startsWith("Active") || line.startsWith("Proto")) {
                continue;
            }

            // ss 格式: tcp LISTEN 0 128 *:8080 *:* users:(("java",pid=1234,fd=5))
            if (line.matches("^(tcp|udp|TCP|UDP).*")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    String protocol = parts[0].toUpperCase().contains("UDP") ? "UDP" : "TCP";
                    String state = parts.length > 1 && parts[1].matches("[A-Z_]+") ? parts[1] : "UNKNOWN";
                    int localIdx = state.matches("[A-Z_]+") ? 4 : 3;
                    if (localIdx >= parts.length) continue;

                    String localPart = parts[localIdx];
                    String foreignPart = localIdx + 1 < parts.length ? parts[localIdx + 1] : "*:*";

                    Integer port = extractPort(localPart);
                    if (port == null) continue;

                    Long pid = extractPidFromLine(line);
                    String processName = extractProcessName(line);

                    String key = protocol + ":" + port + ":" + localPart + ":" + state + ":" + pid;
                    if (seen.add(key)) {
                        result.add(PortInfoDTO.builder()
                                .protocol(protocol)
                                .port(port)
                                .localAddress(localPart)
                                .foreignAddress(foreignPart)
                                .state(state)
                                .pid(pid)
                                .processName(processName != null ? processName : "-")
                                .programPath("-")
                                .build());
                    }
                }
                continue;
            }

            // netstat 格式: tcp4  0  0  *.8080  *.*  LISTEN
            if (line.startsWith("tcp") || line.startsWith("udp")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String protocol = parts[0].toUpperCase().startsWith("UDP") ? "UDP" : "TCP";
                    String localPart = parts[parts.length - 2];
                    String state = parts[parts.length - 1];
                    Integer port = extractPort(localPart);
                    if (port != null) {
                        String key = protocol + ":" + port + ":" + localPart;
                        if (seen.add(key)) {
                            result.add(PortInfoDTO.builder()
                                    .protocol(protocol)
                                    .port(port)
                                    .localAddress(localPart)
                                    .foreignAddress("*:*")
                                    .state(state)
                                    .pid(null)
                                    .processName("-")
                                    .programPath("-")
                                    .build());
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<PortInfoDTO> parseWindowsNetstat(List<String> lines) {
        List<PortInfoDTO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String line : lines) {
            Matcher m = NETSTAT_WIN.matcher(line);
            if (m.find()) {
                String protocol = m.group("proto");
                String local = m.group("local");
                int port = Integer.parseInt(m.group("port"));
                String state = m.group("state");
                long pid = Long.parseLong(m.group("pid"));
                String key = protocol + ":" + port + ":" + pid;
                if (seen.add(key)) {
                    result.add(PortInfoDTO.builder()
                            .protocol(protocol)
                            .port(port)
                            .localAddress(local + ":" + port)
                            .foreignAddress(m.group("foreign") + ":" + m.group("fport"))
                            .state(state)
                            .pid(pid)
                            .processName("-")
                            .programPath("-")
                            .build());
                }
            }
        }
        return result;
    }

    private Integer extractPort(String addr) {
        if (addr == null) return null;
        int colon = addr.lastIndexOf(':');
        if (colon < 0) return null;
        String portStr = addr.substring(colon + 1);
        if ("*".equals(portStr) || portStr.isEmpty()) return null;
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractPidFromLine(String line) {
        Matcher m = Pattern.compile("pid=(\\d+)").matcher(line);
        if (m.find()) return Long.parseLong(m.group(1));
        m = Pattern.compile("\\((\\d+)\\)").matcher(line);
        if (m.find()) return Long.parseLong(m.group(1));
        return null;
    }

    private String extractProcessName(String line) {
        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(line);
        if (m.find()) return m.group(1);
        return null;
    }
}
