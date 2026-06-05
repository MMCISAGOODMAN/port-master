package com.portmaster.util.parser;

import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.util.CommandExecutor;
import com.portmaster.util.OsDetector;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Linux/Mac 端口解析器 - 使用 ss、lsof、ps
 * macOS 若无 ss 则回退 netstat + lsof
 */
@Slf4j
public class UnixPortParser {

    private static final Pattern SS_LINE = Pattern.compile(
            "^(?<state>\\S+)?\\s*(?<recv>\\d+)\\s+(?<send>\\d+)\\s+(?<local>\\S+):(?<port>\\d+|\\*)\\s+(?<foreign>\\S+):(?<fport>\\d+|\\*)\\s*(?<proc>.*)$");

    private static final Pattern LSOF_LINE = Pattern.compile(
            "^(?<name>\\S+)\\s+(?<pid>\\d+)\\s+\\S+\\s+(?<proto>\\S+)\\s+(?<device>\\S+)\\s+(?<size>\\S+)\\s+(?<node>\\S+)\\s+(?<type>\\S+)\\s+(?<port>\\S+)(\\s+(?<state>\\S+))?(\\s+->\\s+(?<remote>\\S+))?$");

    /**
     * 扫描全部 TCP/UDP 端口
     */
    public List<PortInfoDTO> scanAllPorts() {
        if (CommandExecutor.isCommandAvailable("ss") && !OsDetector.isMacOS()) {
            return scanWithSs();
        }
        return scanWithLsofAndNetstat();
    }

    /**
     * 使用 ss 命令扫描 (Linux)
     */
    private List<PortInfoDTO> scanWithSs() {
        List<PortInfoDTO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 监听端口
        try {
            List<String> listenLines = CommandExecutor.execute("ss", "-tulnp");
            parseSsLines(listenLines, result, seen, true);
        } catch (Exception e) {
            log.warn("ss -tulnp failed, fallback to lsof", e);
            return scanWithLsofAndNetstat();
        }

        // 所有连接
        try {
            List<String> connLines = CommandExecutor.execute("ss", "-tunap");
            parseSsLines(connLines, result, seen, false);
        } catch (Exception e) {
            log.warn("ss -tunap failed", e);
        }

        enrichProcessPaths(result);
        return result;
    }

    private void parseSsLines(List<String> lines, List<PortInfoDTO> result,
                              Set<String> seen, boolean listenOnly) {
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("Netid") || line.startsWith("State")) {
                continue;
            }

            // ss 输出格式: State Recv-Q Send-Q Local:Port Peer:Port Process
            String[] parts = line.split("\\s+");
            if (parts.length < 5) {
                continue;
            }

            String netId = parts[0];
            String protocol = netId.toUpperCase().contains("UDP") ? "UDP" : "TCP";

            int localIdx = listenOnly ? 3 : (parts[0].matches("[A-Z_]+") ? 4 : 3);
            if (parts[0].matches("[A-Z_]+") && !listenOnly) {
                localIdx = 4;
            }

            // 重新解析 - ss 格式多样
            String localAddr = "";
            String foreignAddr = "";
            String state = "LISTEN";
            Long pid = null;
            String processName = "";

            if (parts[0].matches("[A-Z_]+")) {
                state = parts[0];
                if (parts.length > 4) {
                    localAddr = parts[4].contains(":") ? parts[4] : parts[3];
                    foreignAddr = parts.length > 5 ? parts[5] : "*:*";
                }
            } else {
                // netid 在第一列
                if (parts.length >= 6) {
                    state = "LISTEN".equals(parts[1]) || parts[1].matches("\\d+") ? 
                            (parts[1].matches("\\d+") ? "LISTEN" : parts[1]) : parts[1];
                    localAddr = parts[4];
                    foreignAddr = parts.length > 5 ? parts[5] : "*:*";
                }
            }

            // 简化解析：找 users:(( 部分
            int usersIdx = line.indexOf("users:((");
            if (usersIdx > 0) {
                String usersPart = line.substring(usersIdx);
                Matcher pidMatcher = Pattern.compile("pid=(\\d+)").matcher(usersPart);
                if (pidMatcher.find()) {
                    pid = Long.parseLong(pidMatcher.group(1));
                }
                Matcher nameMatcher = Pattern.compile("\"([^\"]+)\"").matcher(usersPart);
                if (nameMatcher.find()) {
                    processName = nameMatcher.group(1);
                }
            }

            // 解析地址
            Pattern addrPattern = Pattern.compile("(\\S+):(\\d+|\\*)");
            Matcher localMatcher = Pattern.compile("\\s(\\S+:\\d+|\\S+:\\*)\\s").matcher(" " + line + " ");
            if (localMatcher.find()) {
                localAddr = localMatcher.group(1);
            }
            Matcher foreignMatcher = Pattern.compile("\\s(\\S+:\\d+|\\S+:\\*)\\s+(users:|$)").matcher(line);
            if (foreignMatcher.find()) {
                foreignAddr = foreignMatcher.group(1);
            }

            Integer port = WindowsPortParser.extractPort(localAddr);
            if (port == null) {
                continue;
            }

            String key = protocol + ":" + port + ":" + localAddr + ":" + foreignAddr + ":" + state + ":" + pid;
            if (!seen.add(key)) {
                continue;
            }

            result.add(PortInfoDTO.builder()
                    .protocol(protocol)
                    .port(port)
                    .localAddress(localAddr)
                    .foreignAddress(foreignAddr)
                    .pid(pid)
                    .processName(processName)
                    .programPath("")
                    .state(state.toUpperCase())
                    .build());
        }
    }

    /** lsof 标准输出 NAME 列解析：TCP *:8080 (LISTEN) 或 TCP 1.2.3.4:8080->5.6.7.8:443 (ESTABLISHED) */
    private static final Pattern LSOF_NAME_PATTERN = Pattern.compile(
            "(?<proto>UDP|TCP)\\s+(?<local>\\S+?)(?:->(?<foreign>\\S+))?\\s*(?:\\((?<state>[^)]+)\\))?$");

    /**
     * 使用 lsof + netstat 扫描 (macOS 或 ss 不可用时)
     */
    private List<PortInfoDTO> scanWithLsofAndNetstat() {
        List<PortInfoDTO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try {
            List<String> lsofLines = CommandExecutor.execute("lsof", "-i", "-P", "-n");
            parseLsofStandardOutput(lsofLines, result, seen);
        } catch (Exception e) {
            log.warn("lsof failed", e);
        }

        // netstat 作为补充（需权限，失败时忽略）
        try {
            List<String> netstatLines = CommandExecutor.execute("netstat", "-an");
            parseNetstatLines(netstatLines, result, seen);
        } catch (Exception e) {
            log.warn("netstat failed", e);
        }

        enrichProcessPaths(result);
        return result;
    }

    /**
     * 解析 lsof 标准表格输出
     * 格式: COMMAND PID USER FD TYPE DEVICE SIZE/OFF [NODE] NAME
     */
    private void parseLsofStandardOutput(List<String> lines, List<PortInfoDTO> result, Set<String> seen) {
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("COMMAND")) {
                continue;
            }

            // NAME 列以 TCP/UDP 开头，从末尾定位
            int nameIdx = line.lastIndexOf(" TCP ");
            if (nameIdx < 0) {
                nameIdx = line.lastIndexOf(" UDP ");
            }
            if (nameIdx < 0) {
                // 行尾格式: ... TCP *:8080 (LISTEN)
                if (line.endsWith("(LISTEN)") || line.contains(" TCP ") || line.contains(" UDP ")) {
                    nameIdx = Math.max(line.lastIndexOf(" TCP "), line.lastIndexOf(" UDP "));
                }
            }
            if (nameIdx < 0) {
                continue;
            }

            String nameField = line.substring(nameIdx + 1).trim();
            String prefix = line.substring(0, nameIdx).trim();
            String[] prefixParts = prefix.split("\\s+");
            if (prefixParts.length < 2) {
                continue;
            }

            String processName = prefixParts[0];
            Long pid;
            try {
                pid = Long.parseLong(prefixParts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            PortInfoDTO portInfo = parseLsofNameField(nameField, pid, processName);
            if (portInfo == null) {
                continue;
            }

            String key = portInfo.getProtocol() + ":" + portInfo.getPort() + ":"
                    + portInfo.getLocalAddress() + ":" + portInfo.getForeignAddress()
                    + ":" + portInfo.getState() + ":" + portInfo.getPid();
            if (seen.add(key)) {
                result.add(portInfo);
            }
        }
    }

    private PortInfoDTO parseLsofNameField(String value, Long pid, String processName) {
        Matcher m = LSOF_NAME_PATTERN.matcher(value.trim());
        if (!m.find()) {
            return null;
        }

        String protocol = m.group("proto");
        String local = m.group("local");
        String foreign = m.group("foreign") != null ? m.group("foreign") : "*:*";
        String state = m.group("state") != null ? m.group("state") : ("*:*".equals(local) ? "UNKNOWN" : "LISTEN");

        Integer port = WindowsPortParser.extractPort(local);
        if (port == null && local.startsWith("*:")) {
            try {
                port = Integer.parseInt(local.substring(2));
            } catch (NumberFormatException ignored) {
            }
        }
        if (port == null) {
            return null;
        }

        return PortInfoDTO.builder()
                .protocol(protocol)
                .port(port)
                .localAddress(local)
                .foreignAddress(foreign)
                .pid(pid)
                .processName(processName != null ? processName : "")
                .programPath("")
                .state(state.toUpperCase())
                .build();
    }

    private void parseNetstatLines(List<String> lines, List<PortInfoDTO> result, Set<String> seen) {
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            // tcp4  0  0  *.8080   *.*   LISTEN
            String[] parts = line.split("\\s+");
            if (parts.length < 6) {
                continue;
            }
            String proto = parts[0].toUpperCase();
            if (!proto.startsWith("TCP") && !proto.startsWith("UDP")) {
                continue;
            }
            String protocol = proto.startsWith("TCP") ? "TCP" : "UDP";
            String local = parts[3];
            String foreign = parts[4];
            String state = parts.length > 5 ? parts[5] : "UNKNOWN";

            Integer port = WindowsPortParser.extractPort(local.replace("*", "0.0.0.0"));
            if (port == null) {
                // 尝试 *.8080 格式
                if (local.startsWith("*.")) {
                    try {
                        port = Integer.parseInt(local.substring(2));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (port == null) {
                continue;
            }

            String key = protocol + ":" + port + ":" + local + ":" + foreign + ":" + state + ":null";
            if (!seen.add(key)) {
                continue;
            }

            result.add(PortInfoDTO.builder()
                    .protocol(protocol)
                    .port(port)
                    .localAddress(local)
                    .foreignAddress(foreign)
                    .pid(null)
                    .processName("")
                    .programPath("")
                    .state(state.toUpperCase())
                    .build());
        }
    }

    /**
     * 补充进程路径信息
     */
    private void enrichProcessPaths(List<PortInfoDTO> ports) {
        Set<Long> pids = ports.stream()
                .map(PortInfoDTO::getPid)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, String> pathMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();

        for (Long pid : pids) {
            try {
                if (OsDetector.isMacOS()) {
                    List<String> lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid),
                            "-o", "comm=,command=");
                    if (!lines.isEmpty()) {
                        String[] psParts = lines.get(0).trim().split("\\s+", 2);
                        nameMap.putIfAbsent(pid, psParts[0]);
                        if (psParts.length > 1) {
                            pathMap.put(pid, psParts[1]);
                        }
                    }
                } else {
                    List<String> lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid),
                            "-o", "comm=,args=", "--no-headers");
                    if (!lines.isEmpty()) {
                        String[] psParts = lines.get(0).trim().split("\\s+", 2);
                        nameMap.putIfAbsent(pid, psParts[0]);
                        if (psParts.length > 1) {
                            pathMap.put(pid, psParts[1]);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to get ps info for pid {}", pid);
            }
        }

        for (PortInfoDTO port : ports) {
            if (port.getPid() != null) {
                if (port.getProcessName() == null || port.getProcessName().isEmpty()) {
                    port.setProcessName(nameMap.getOrDefault(port.getPid(), ""));
                }
                if (port.getProgramPath() == null || port.getProgramPath().isEmpty()) {
                    port.setProgramPath(pathMap.getOrDefault(port.getPid(), ""));
                }
            }
        }
    }

    /**
     * 获取进程命令行
     */
    public String getProcessCommandLine(long pid) {
        try {
            List<String> lines;
            if (OsDetector.isMacOS()) {
                lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "command=");
            } else {
                lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "args=", "--no-headers");
            }
            return lines.isEmpty() ? "" : lines.get(0).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取进程路径
     */
    public String getProcessPath(long pid) {
        try {
            if (OsDetector.isMacOS()) {
                List<String> lines = CommandExecutor.execute("lsof", "-p", String.valueOf(pid), "-Fn");
                for (String line : lines) {
                    if (line.startsWith("n") && (line.endsWith(".app") || !line.contains(" "))) {
                        // 尝试 pwdx 替代
                    }
                }
                List<String> psLines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "command=");
                return psLines.isEmpty() ? "" : psLines.get(0).trim().split("\\s+")[0];
            }
            List<String> lines = CommandExecutor.execute("readlink", "-f", "/proc/" + pid + "/exe");
            return lines.isEmpty() ? "" : lines.get(0).trim();
        } catch (Exception e) {
            try {
                List<String> psLines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "comm=");
                return psLines.isEmpty() ? "" : psLines.get(0).trim();
            } catch (Exception ex) {
                return "";
            }
        }
    }
}
