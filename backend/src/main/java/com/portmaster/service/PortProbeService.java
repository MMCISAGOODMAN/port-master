package com.portmaster.service;

import com.portmaster.model.dto.PortProbeResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 端口连通性探测服务
 */
@Slf4j
@Service
public class PortProbeService {

    private static final int DEFAULT_TIMEOUT_MS = 3000;

    /**
     * TCP 端口连通性探测
     */
    public PortProbeResultDTO probeTcp(String host, int port, int timeoutMs) {
        if (host == null || host.isBlank()) {
            host = "127.0.0.1";
        }
        int timeout = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        long start = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            long latency = System.currentTimeMillis() - start;
            return PortProbeResultDTO.builder()
                    .host(host)
                    .port(port)
                    .protocol("TCP")
                    .reachable(true)
                    .latencyMs(latency)
                    .message("端口 " + port + " 可连通 (" + latency + "ms)")
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return PortProbeResultDTO.builder()
                    .host(host)
                    .port(port)
                    .protocol("TCP")
                    .reachable(false)
                    .latencyMs(latency)
                    .message("端口 " + port + " 不可连通: " + e.getMessage())
                    .build();
        }
    }
}
