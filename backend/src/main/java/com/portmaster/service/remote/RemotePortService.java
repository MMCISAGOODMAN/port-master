package com.portmaster.service.remote;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.portmaster.config.PortMasterProperties;
import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.util.SshExecutor;
import com.portmaster.util.parser.RemotePortParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SSH 远程服务器端口查询与进程管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemotePortService {

    private final PortMasterProperties properties;
    private final RemotePortParser remotePortParser = new RemotePortParser();

    /**
     * 远程端口扫描
     */
    public List<PortInfoDTO> scanRemotePorts(String host, int sshPort, String username, String credential) {
        return scanRemotePorts(host, sshPort, username, credential, "password");
    }

    public List<PortInfoDTO> scanRemotePorts(String host, int sshPort, String username,
                                              String credential, String authType) {
        Session session = null;
        try {
            session = createSession(host, sshPort, username, credential, authType);
            String osType = SshExecutor.detectRemoteOs(session, properties.getSsh().getCommandTimeoutSec());
            String command = buildScanCommand(osType);
            List<String> output = SshExecutor.exec(session, properties.getSsh().getCommandTimeoutSec(), command);
            List<PortInfoDTO> ports = remotePortParser.parse(osType, output);
            ports.forEach(p -> {
                p.setProgramPath("[remote:" + host + "]");
                if (p.getProcessName() == null || "-".equals(p.getProcessName())) {
                    p.setProcessName("[remote]");
                }
            });
            return ports;
        } catch (Exception e) {
            log.error("Remote scan failed: host={}", host, e);
            throw new RuntimeException("远程扫描失败: " + e.getMessage(), e);
        } finally {
            SshExecutor.disconnect(session);
        }
    }

    /**
     * 远程杀进程
     */
    public boolean killRemoteProcess(String host, int sshPort, String username, String credential,
                                     long pid, boolean force) {
        return killRemoteProcess(host, sshPort, username, credential, "password", pid, force);
    }

    public boolean killRemoteProcess(String host, int sshPort, String username, String credential,
                                     String authType, long pid, boolean force) {
        Session session = null;
        try {
            session = createSession(host, sshPort, username, credential, authType);
            String osType = SshExecutor.detectRemoteOs(session, properties.getSsh().getCommandTimeoutSec());
            String command = buildKillCommand(osType, pid, force);
            SshExecutor.exec(session, properties.getSsh().getCommandTimeoutSec(), command);
            return true;
        } catch (Exception e) {
            log.error("Remote kill failed: host={}, pid={}", host, pid, e);
            throw new RuntimeException("远程杀进程失败: " + e.getMessage(), e);
        } finally {
            SshExecutor.disconnect(session);
        }
    }

    /**
     * 测试 SSH 连接
     */
    public boolean testConnection(String host, int sshPort, String username, String credential) {
        return testConnection(host, sshPort, username, credential, "password");
    }

    public boolean testConnection(String host, int sshPort, String username,
                                   String credential, String authType) {
        Session session = null;
        try {
            session = createSession(host, sshPort, username, credential, authType);
            List<String> output = SshExecutor.exec(session, 10, "echo ok");
            return !output.isEmpty() && output.get(0).contains("ok");
        } catch (Exception e) {
            log.warn("SSH connection test failed: host={}", host, e);
            return false;
        } finally {
            SshExecutor.disconnect(session);
        }
    }

    /**
     * 获取远程系统信息
     */
    public String getRemoteSystemInfo(String host, int sshPort, String username,
                                       String credential, String authType) {
        Session session = null;
        try {
            session = createSession(host, sshPort, username, credential, authType);
            String osType = SshExecutor.detectRemoteOs(session, properties.getSsh().getCommandTimeoutSec());
            List<String> output = SshExecutor.exec(session, 10,
                    "windows".equals(osType) ? "ver" : "uname -a");
            return output.isEmpty() ? osType : String.join(" ", output);
        } catch (Exception e) {
            return "unknown";
        } finally {
            SshExecutor.disconnect(session);
        }
    }

    private Session createSession(String host, int sshPort, String username,
                                   String credential, String authType) throws Exception {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        JSch jsch = new JSch();
        if ("key".equalsIgnoreCase(authType) && credential != null && !credential.isBlank()) {
            jsch.addIdentity("remote-key", credential.getBytes(), null, null);
        }

        Session session = jsch.getSession(username, host, sshPort);
        if (!"key".equalsIgnoreCase(authType)) {
            session.setPassword(credential != null ? credential : "");
        }
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(properties.getSsh().getConnectTimeoutMs());
        return session;
    }

    private String buildScanCommand(String osType) {
        return switch (osType) {
            case "windows" -> "netstat -ano";
            case "macos" -> "lsof -i -P -n 2>/dev/null; netstat -an 2>/dev/null";
            default -> "ss -tulnp 2>/dev/null || ss -tunap 2>/dev/null || netstat -tulnp 2>/dev/null || lsof -i -P -n 2>/dev/null";
        };
    }

    private String buildKillCommand(String osType, long pid, boolean force) {
        if ("windows".equals(osType)) {
            return force ? "taskkill /F /PID " + pid : "taskkill /PID " + pid;
        }
        return force ? "kill -9 " + pid : "kill -15 " + pid;
    }
}
