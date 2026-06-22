package com.portmaster.util;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * SSH 命令执行工具（基于 JSch）
 */
@Slf4j
public final class SshExecutor {

    private SshExecutor() {
    }

    public static Session connect(String host, int port, String username, String credential,
                                  int connectTimeoutMs) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(credential);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(connectTimeoutMs);
        return session;
    }

    public static void disconnect(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * 在 SSH 会话上执行命令并返回输出行
     */
    public static List<String> exec(Session session, int timeoutSec, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        channel.setErrStream(errStream);

        InputStream in = channel.getInputStream();
        channel.connect(timeoutSec * 1000);

        List<String> lines = new ArrayList<>();
        byte[] buffer = new byte[4096];
        StringBuilder pending = new StringBuilder();

        while (true) {
            while (in.available() > 0) {
                int read = in.read(buffer, 0, buffer.length);
                if (read < 0) break;
                pending.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
            flushLines(pending, lines);
            if (channel.isClosed()) {
                while (in.available() > 0) {
                    int read = in.read(buffer, 0, buffer.length);
                    if (read < 0) break;
                    pending.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
                flushLines(pending, lines);
                break;
            }
            Thread.sleep(100);
        }

        int exitCode = channel.getExitStatus();
        channel.disconnect();

        if (exitCode != 0 && lines.isEmpty()) {
            String err = errStream.toString(StandardCharsets.UTF_8);
            throw new RuntimeException("远程命令执行失败 (exit=" + exitCode + "): " + err.trim());
        }
        return lines;
    }

    private static void flushLines(StringBuilder pending, List<String> lines) {
        int idx;
        while ((idx = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, idx).replace("\r", "");
            lines.add(line);
            pending.delete(0, idx + 1);
        }
    }

    /**
     * 检测远程操作系统类型
     */
    public static String detectRemoteOs(Session session, int timeoutSec) {
        try {
            List<String> uname = exec(session, timeoutSec, "uname -s 2>/dev/null || echo Windows");
            if (!uname.isEmpty()) {
                String os = uname.get(0).trim().toLowerCase();
                if (os.contains("darwin")) return "macos";
                if (os.contains("linux")) return "linux";
                if (os.contains("windows")) return "windows";
            }
        } catch (Exception e) {
            log.debug("uname detection failed, try Windows", e);
        }
        try {
            List<String> ver = exec(session, timeoutSec, "ver 2>nul || echo");
            if (!ver.isEmpty() && ver.get(0).toLowerCase().contains("windows")) {
                return "windows";
            }
        } catch (Exception ignored) {
        }
        return "linux";
    }
}
