package com.portmaster.service;

import com.portmaster.model.dto.ProcessDetailDTO;
import com.portmaster.model.dto.ProcessInfoDTO;
import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.util.CommandExecutor;
import com.portmaster.util.OsDetector;
import com.portmaster.util.parser.UnixPortParser;
import com.portmaster.util.parser.WindowsPortParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 进程管理与详情服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final PortService portService;
    private final WindowsPortParser windowsPortParser = new WindowsPortParser();
    private final UnixPortParser unixPortParser = new UnixPortParser();

    /**
     * 获取进程详情（含 CPU、内存、绑定端口等）
     */
    public ProcessDetailDTO getProcessDetail(long pid) {
        String processName = "";
        String programPath = "";
        String commandLine = "";
        Double cpuPercent = 0.0;
        Double memoryPercent = 0.0;
        String memoryUsage = "";
        String createTime = "";

        if (OsDetector.isWindows()) {
            processName = getWindowsProcessName(pid);
            programPath = windowsPortParser.getProcessPath(pid);
            commandLine = windowsPortParser.getProcessCommandLine(pid);
            double[] metrics = getWindowsProcessMetrics(pid);
            cpuPercent = metrics[0];
            memoryPercent = metrics[1];
            memoryUsage = metrics[2] > 0 ? String.format("%.1f MB", metrics[2]) : "";
            createTime = getWindowsCreateTime(pid);
        } else {
            processName = getUnixProcessName(pid);
            programPath = unixPortParser.getProcessPath(pid);
            commandLine = unixPortParser.getProcessCommandLine(pid);
            double[] metrics = getUnixProcessMetrics(pid);
            cpuPercent = metrics[0];
            memoryPercent = metrics[1];
            memoryUsage = metrics[2] > 0 ? String.format("%.1f MB", metrics[2]) : "";
            createTime = getUnixCreateTime(pid);
        }

        List<PortInfoDTO> boundPorts = portService.queryByPid(pid);

        return ProcessDetailDTO.builder()
                .pid(pid)
                .processName(processName)
                .programPath(programPath)
                .commandLine(commandLine)
                .cpuPercent(cpuPercent)
                .memoryPercent(memoryPercent)
                .memoryUsage(memoryUsage)
                .createTime(createTime)
                .boundPorts(boundPorts)
                .build();
    }

    /**
     * 列出所有运行中的进程
     */
    public List<ProcessInfoDTO> listAllProcesses() {
        Map<Long, Integer> portCountMap = portService.scanAllPorts().stream()
                .filter(p -> p.getPid() != null)
                .collect(Collectors.groupingBy(PortInfoDTO::getPid, Collectors.summingInt(p -> 1)));

        List<ProcessInfoDTO> processes = new ArrayList<>();
        if (OsDetector.isWindows()) {
            processes = listWindowsProcesses();
        } else {
            processes = listUnixProcesses();
        }

        for (ProcessInfoDTO proc : processes) {
            proc.setPortCount(portCountMap.getOrDefault(proc.getPid(), 0));
        }
        return processes;
    }

    /**
     * 按端口杀死占用进程
     */
    public List<String> killByPort(int port, boolean force) {
        List<PortInfoDTO> ports = portService.queryByPort(port);
        Set<Long> pids = ports.stream()
                .map(PortInfoDTO::getPid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (pids.isEmpty()) {
            return List.of("端口 " + port + " 当前未被占用");
        }
        List<String> results = new ArrayList<>();
        results.add("端口 " + port + " 关联 " + pids.size() + " 个进程");
        results.addAll(killProcesses(new ArrayList<>(pids), force));
        return results;
    }

    /**
     * 正常结束进程
     */
    public boolean killProcess(long pid, boolean force) {
        if (OsDetector.isWindows()) {
            if (force) {
                int code = CommandExecutor.executeWithExitCode("taskkill", "/F", "/PID", String.valueOf(pid));
                return code == 0;
            }
            int code = CommandExecutor.executeWithExitCode("taskkill", "/PID", String.valueOf(pid));
            return code == 0;
        } else {
            if (force) {
                int code = CommandExecutor.executeWithExitCode("kill", "-9", String.valueOf(pid));
                return code == 0;
            }
            int code = CommandExecutor.executeWithExitCode("kill", String.valueOf(pid));
            return code == 0;
        }
    }

    /**
     * 批量杀进程
     */
    public List<String> killProcesses(List<Long> pids, boolean force) {
        List<String> results = new ArrayList<>();
        for (Long pid : pids) {
            try {
                boolean success = killProcess(pid, force);
                results.add("PID " + pid + ": " + (success ? "成功" : "失败"));
            } catch (Exception e) {
                results.add("PID " + pid + ": 失败 - " + e.getMessage());
            }
        }
        return results;
    }

    private List<ProcessInfoDTO> listWindowsProcesses() {
        List<ProcessInfoDTO> result = new ArrayList<>();
        try {
            List<String> lines = CommandExecutor.execute("tasklist", "/FO", "CSV", "/NH");
            for (String line : lines) {
                if (line.trim().isEmpty() || line.contains("No tasks")) {
                    continue;
                }
                String[] parts = parseCsvLine(line);
                if (parts.length >= 5) {
                    try {
                        String name = parts[0].replace("\"", "");
                        long pid = Long.parseLong(parts[1].replace("\"", ""));
                        String memStr = parts[4].replace("\"", "").replace(",", "").replace(" K", "").trim();
                        double memMb = 0;
                        try {
                            memMb = Double.parseDouble(memStr) / 1024.0;
                        } catch (NumberFormatException ignored) {
                        }
                        result.add(ProcessInfoDTO.builder()
                                .pid(pid)
                                .processName(name)
                                .commandLine(name)
                                .cpuPercent(0.0)
                                .memoryPercent(0.0)
                                .memoryUsage(memMb > 0 ? String.format("%.1f MB", memMb) : "")
                                .build());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to list Windows processes", e);
        }
        return result;
    }

    private List<ProcessInfoDTO> listUnixProcesses() {
        List<ProcessInfoDTO> result = new ArrayList<>();
        try {
            List<String> lines;
            if (OsDetector.isMacOS()) {
                lines = CommandExecutor.execute("ps", "-ax", "-o", "pid,comm,pcpu,pmem,rss");
            } else {
                lines = CommandExecutor.execute("ps", "-eo", "pid,comm,pcpu,pmem,rss", "--no-headers");
            }
            boolean headerSkipped = OsDetector.isMacOS();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (headerSkipped && line.startsWith("PID")) {
                    continue;
                }
                String[] parts = line.split("\\s+", 5);
                if (parts.length >= 4) {
                    try {
                        long pid = Long.parseLong(parts[0]);
                        String comm = parts[1];
                        double cpu = Double.parseDouble(parts[2]);
                        double mem = Double.parseDouble(parts[3]);
                        String memUsage = "";
                        if (parts.length >= 5) {
                            try {
                                double rssMb = Long.parseLong(parts[4]) / 1024.0;
                                memUsage = String.format("%.1f MB", rssMb);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        result.add(ProcessInfoDTO.builder()
                                .pid(pid)
                                .processName(comm)
                                .commandLine(comm)
                                .cpuPercent(cpu)
                                .memoryPercent(mem)
                                .memoryUsage(memUsage)
                                .build());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to list Unix processes", e);
        }
        return result;
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

    private String getWindowsProcessName(long pid) {
        try {
            List<String> lines = CommandExecutor.execute("tasklist", "/FI", "PID eq " + pid, "/FO", "CSV", "/NH");
            if (!lines.isEmpty()) {
                String line = lines.get(0);
                if (line.contains("\"")) {
                    return line.split("\"")[1];
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get process name for pid {}", pid);
        }
        return "";
    }

    private double[] getWindowsProcessMetrics(long pid) {
        try {
            List<String> lines = CommandExecutor.execute("wmic", "process", "where",
                    "ProcessId=" + pid, "get", "WorkingSetSize,PercentProcessorTime", "/VALUE");
            double memoryMb = 0;
            for (String line : lines) {
                if (line.startsWith("WorkingSetSize=")) {
                    try {
                        long bytes = Long.parseLong(line.substring(15).trim());
                        memoryMb = bytes / (1024.0 * 1024.0);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            // CPU 需要 typeperf 或 wmic cpu 计算，简化处理
            return new double[]{0.0, 0.0, memoryMb};
        } catch (Exception e) {
            return new double[]{0.0, 0.0, 0.0};
        }
    }

    private String getWindowsCreateTime(long pid) {
        try {
            List<String> lines = CommandExecutor.execute("wmic", "process", "where",
                    "ProcessId=" + pid, "get", "CreationDate", "/VALUE");
            for (String line : lines) {
                if (line.startsWith("CreationDate=")) {
                    String raw = line.substring(13).trim();
                    if (raw.length() >= 14) {
                        return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8)
                                + " " + raw.substring(8, 10) + ":" + raw.substring(10, 12) + ":" + raw.substring(12, 14);
                    }
                    return raw;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get create time for pid {}", pid);
        }
        return "";
    }

    private String getUnixProcessName(long pid) {
        try {
            List<String> lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "comm=");
            return lines.isEmpty() ? "" : lines.get(0).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private double[] getUnixProcessMetrics(long pid) {
        try {
            List<String> lines;
            if (OsDetector.isMacOS()) {
                lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "%cpu,%mem,rss");
            } else {
                lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "%cpu,%mem,rss", "--no-headers");
            }
            if (lines.size() >= 2) {
                String dataLine = lines.get(lines.size() - 1).trim();
                String[] parts = dataLine.split("\\s+");
                if (parts.length >= 3) {
                    double cpu = Double.parseDouble(parts[0]);
                    double mem = Double.parseDouble(parts[1]);
                    double rssMb = Long.parseLong(parts[2]) / 1024.0;
                    return new double[]{cpu, mem, rssMb};
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get metrics for pid {}", pid);
        }
        return new double[]{0.0, 0.0, 0.0};
    }

    private String getUnixCreateTime(long pid) {
        try {
            List<String> lines;
            if (OsDetector.isMacOS()) {
                lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "lstart=");
            } else {
                lines = CommandExecutor.execute("ps", "-p", String.valueOf(pid), "-o", "lstart=", "--no-headers");
            }
            return lines.isEmpty() ? "" : lines.get(0).trim();
        } catch (Exception e) {
            return "";
        }
    }
}
