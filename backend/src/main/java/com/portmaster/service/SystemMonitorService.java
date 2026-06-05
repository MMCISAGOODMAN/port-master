package com.portmaster.service;

import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.model.dto.SystemStatsDTO;
import com.portmaster.util.CommandExecutor;
import com.portmaster.util.OsDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统监控大盘服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMonitorService {

    private final PortService portService;

    /**
     * 获取系统监控统计数据
     */
    public SystemStatsDTO getSystemStats() {
        double cpuUsage = getCpuUsage();
        double[] memory = getMemoryUsage();
        List<PortInfoDTO> allPorts = portService.scanAllPorts();

        long listenCount = allPorts.stream()
                .filter(p -> "LISTEN".equalsIgnoreCase(p.getState()))
                .count();
        long activeCount = allPorts.stream()
                .filter(p -> "ESTABLISHED".equalsIgnoreCase(p.getState()))
                .count();

        int processCount = getProcessCount();

        return SystemStatsDTO.builder()
                .cpuUsage(cpuUsage)
                .memoryUsage(memory[0])
                .memoryUsedMb(memory[1])
                .memoryTotalMb(memory[2])
                .listenPortCount((int) listenCount)
                .activeConnectionCount((int) activeCount)
                .processCount(processCount)
                .osType(OsDetector.getOsName())
                .needAdminHint(needAdminHint())
                .build();
    }

    private double getCpuUsage() {
        try {
            if (OsDetector.isWindows()) {
                List<String> lines = CommandExecutor.execute("wmic", "cpu", "get", "LoadPercentage", "/VALUE");
                for (String line : lines) {
                    if (line.startsWith("LoadPercentage=")) {
                        String val = line.substring(15).trim();
                        if (!val.isEmpty()) {
                            return Double.parseDouble(val);
                        }
                    }
                }
            } else if (OsDetector.isMacOS()) {
                List<String> lines = CommandExecutor.execute("top", "-l", "1", "-n", "0", "-s", "0");
                for (String line : lines) {
                    if (line.contains("CPU usage")) {
                        Matcher m = Pattern.compile("([\\d.]+)%\\s+user").matcher(line);
                        if (m.find()) {
                            return Double.parseDouble(m.group(1));
                        }
                    }
                }
            } else {
                // Linux: 读取 /proc/stat
                List<String> lines = CommandExecutor.execute("sh", "-c",
                        "grep 'cpu ' /proc/stat | awk '{usage=($2+$4)*100/($2+$3+$4+$5)} END {print usage}'");
                if (!lines.isEmpty()) {
                    return Double.parseDouble(lines.get(0).trim());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get CPU usage", e);
        }
        return 0.0;
    }

    private double[] getMemoryUsage() {
        try {
            if (OsDetector.isWindows()) {
                List<String> lines = CommandExecutor.execute("wmic", "OS", "get",
                        "TotalVisibleMemorySize,FreePhysicalMemory", "/VALUE");
                double totalKb = 0, freeKb = 0;
                for (String line : lines) {
                    if (line.startsWith("TotalVisibleMemorySize=")) {
                        totalKb = Double.parseDouble(line.substring(23).trim());
                    } else if (line.startsWith("FreePhysicalMemory=")) {
                        freeKb = Double.parseDouble(line.substring(19).trim());
                    }
                }
                if (totalKb > 0) {
                    double usedMb = (totalKb - freeKb) / 1024.0;
                    double totalMb = totalKb / 1024.0;
                    double percent = (totalKb - freeKb) / totalKb * 100.0;
                    return new double[]{percent, usedMb, totalMb};
                }
            } else if (OsDetector.isMacOS()) {
                List<String> lines = CommandExecutor.execute("vm_stat");
                long pageSize = 4096;
                long free = 0, active = 0, inactive = 0, wired = 0;
                for (String line : lines) {
                    if (line.startsWith("Pages free:")) {
                        free = Long.parseLong(line.replaceAll("[^0-9]", ""));
                    } else if (line.startsWith("Pages active:")) {
                        active = Long.parseLong(line.replaceAll("[^0-9]", ""));
                    } else if (line.startsWith("Pages inactive:")) {
                        inactive = Long.parseLong(line.replaceAll("[^0-9]", ""));
                    } else if (line.startsWith("Pages wired down:")) {
                        wired = Long.parseLong(line.replaceAll("[^0-9]", ""));
                    }
                }
                double usedMb = (active + inactive + wired) * pageSize / (1024.0 * 1024.0);
                double totalMb = (free + active + inactive + wired) * pageSize / (1024.0 * 1024.0);
                double percent = totalMb > 0 ? usedMb / totalMb * 100.0 : 0;
                return new double[]{percent, usedMb, totalMb};
            } else {
                List<String> lines = CommandExecutor.execute("free", "-m");
                if (lines.size() >= 2) {
                    String[] parts = lines.get(1).trim().split("\\s+");
                    if (parts.length >= 3) {
                        double totalMb = Double.parseDouble(parts[1]);
                        double usedMb = Double.parseDouble(parts[2]);
                        double percent = totalMb > 0 ? usedMb / totalMb * 100.0 : 0;
                        return new double[]{percent, usedMb, totalMb};
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get memory usage", e);
        }
        return new double[]{0.0, 0.0, 0.0};
    }

    private int getProcessCount() {
        try {
            if (OsDetector.isWindows()) {
                List<String> lines = CommandExecutor.execute("tasklist", "/FO", "CSV", "/NH");
                return (int) lines.stream().filter(l -> !l.trim().isEmpty()).count();
            }
            List<String> lines = CommandExecutor.execute("ps", "-e");
            return Math.max(0, lines.size() - 1);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean needAdminHint() {
        // 简单检测：尝试执行需要权限的操作
        if (OsDetector.isWindows()) {
            try {
                List<String> lines = CommandExecutor.execute(5, "net", "session");
                return lines.stream().anyMatch(l -> l.contains("Access is denied"));
            } catch (Exception e) {
                return true;
            }
        }
        return !isRoot();
    }

    private boolean isRoot() {
        if (OsDetector.isWindows()) {
            try {
                List<String> lines = CommandExecutor.execute("net", "session");
                return !lines.isEmpty();
            } catch (Exception e) {
                return false;
            }
        }
        return "0".equals(System.getProperty("user.name")) ||
                System.getProperty("user.name", "").equals("root");
    }
}
