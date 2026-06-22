package com.portmaster.service;

import com.portmaster.model.dto.NetworkInterfaceDTO;
import com.portmaster.util.CommandExecutor;
import com.portmaster.util.OsDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网络接口信息服务
 */
@Slf4j
@Service
public class NetworkService {

    /**
     * 获取本机网络接口列表
     */
    public List<NetworkInterfaceDTO> listInterfaces() {
        List<NetworkInterfaceDTO> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback()) continue;

                byte[] mac = ni.getHardwareAddress();
                String macStr = mac != null ? formatMac(mac) : "-";

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                boolean hasAddr = false;
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || addr.getHostAddress().contains(":")) continue;
                    hasAddr = true;
                    result.add(NetworkInterfaceDTO.builder()
                            .name(ni.getName())
                            .ipAddress(addr.getHostAddress())
                            .macAddress(macStr)
                            .status(ni.isUp() ? "UP" : "DOWN")
                            .type(ni.getDisplayName())
                            .build());
                }
                if (!hasAddr && ni.isUp()) {
                    result.add(NetworkInterfaceDTO.builder()
                            .name(ni.getName())
                            .ipAddress("-")
                            .macAddress(macStr)
                            .status("UP")
                            .type(ni.getDisplayName())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Java NetworkInterface enumeration failed, fallback to command", e);
            return listInterfacesViaCommand();
        }
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }

    private List<NetworkInterfaceDTO> listInterfacesViaCommand() {
        if (OsDetector.isWindows()) {
            return parseWindowsIpconfig();
        }
        try {
            if (CommandExecutor.isCommandAvailable("ip")) {
                return parseLinuxIp();
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private List<NetworkInterfaceDTO> parseLinuxIp() {
        List<NetworkInterfaceDTO> result = new ArrayList<>();
        List<String> lines = CommandExecutor.execute("ip", "-4", "addr", "show");
        String currentName = null;
        for (String line : lines) {
            if (line.matches("^\\d+:.*")) {
                int colon = line.indexOf(':');
                int at = line.indexOf(':', colon + 1);
                currentName = at > colon ? line.substring(colon + 1, at).trim() : line.substring(colon + 1).trim();
            } else if (line.trim().startsWith("inet ") && currentName != null) {
                Matcher m = Pattern.compile("inet (\\S+)").matcher(line.trim());
                if (m.find()) {
                    result.add(NetworkInterfaceDTO.builder()
                            .name(currentName)
                            .ipAddress(m.group(1).split("/")[0])
                            .macAddress("-")
                            .status("UP")
                            .type("network")
                            .build());
                }
            }
        }
        return result;
    }

    private List<NetworkInterfaceDTO> parseWindowsIpconfig() {
        List<NetworkInterfaceDTO> result = new ArrayList<>();
        try {
            List<String> lines = CommandExecutor.execute("ipconfig");
            String currentName = null;
            for (String line : lines) {
                if (line.endsWith(":") && !line.startsWith(" ")) {
                    currentName = line.replace(":", "").trim();
                } else if (line.trim().startsWith("IPv4") && currentName != null) {
                    Matcher m = Pattern.compile(": ([\\d.]+)").matcher(line);
                    if (m.find()) {
                        result.add(NetworkInterfaceDTO.builder()
                                .name(currentName)
                                .ipAddress(m.group(1))
                                .macAddress("-")
                                .status("UP")
                                .type("network")
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("ipconfig failed", e);
        }
        return result;
    }

    private String formatMac(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format("%02X", mac[i]));
        }
        return sb.toString();
    }
}
