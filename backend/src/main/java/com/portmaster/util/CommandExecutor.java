package com.portmaster.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统命令执行统一封装工具类
 * 适配 Windows cmd 与 Linux/Mac shell
 */
@Slf4j
public final class CommandExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final Charset WINDOWS_CHARSET = Charset.forName("GBK");

    private CommandExecutor() {
    }

    /**
     * 执行系统命令并返回输出行列表
     */
    public static List<String> execute(String... command) {
        return execute(DEFAULT_TIMEOUT_SECONDS, command);
    }

    /**
     * 执行系统命令（带超时）
     */
    public static List<String> execute(int timeoutSeconds, String... command) {
        List<String> lines = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            Charset charset = OsDetector.isWindows() ? WINDOWS_CHARSET : StandardCharsets.UTF_8;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Command timeout: {}", String.join(" ", command));
            }
        } catch (Exception e) {
            log.error("Command execution failed: {}", String.join(" ", command), e);
            throw new RuntimeException("命令执行失败: " + e.getMessage(), e);
        }
        return lines;
    }

    /**
     * 执行 shell 命令（Unix 使用 sh -c，Windows 使用 cmd /c）
     */
    public static List<String> executeShell(String shellCommand) {
        if (OsDetector.isWindows()) {
            return execute("cmd", "/c", shellCommand);
        }
        return execute("sh", "-c", shellCommand);
    }

    /**
     * 检查命令是否可用
     */
    public static boolean isCommandAvailable(String command) {
        try {
            if (OsDetector.isWindows()) {
                List<String> result = execute(5, "where", command);
                return !result.isEmpty();
            }
            List<String> result = execute(5, "sh", "-c", "command -v " + command);
            return !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行命令并返回退出码
     */
    public static int executeWithExitCode(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return -1;
            }
            return process.exitValue();
        } catch (Exception e) {
            log.error("Command failed: {}", String.join(" ", command), e);
            return -1;
        }
    }
}
