package com.portmaster.service;

import com.portmaster.model.dto.PortProbeResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 端口连通性探测服务（TCP / HTTP / TLS）
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
                    .probeType("TCP")
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
                    .probeType("TCP")
                    .reachable(false)
                    .latencyMs(latency)
                    .message("端口 " + port + " 不可连通: " + e.getMessage())
                    .build();
        }
    }

    /**
     * HTTP 健康探测
     */
    public PortProbeResultDTO probeHttp(String host, int port, String path, int timeoutMs) {
        if (host == null || host.isBlank()) host = "127.0.0.1";
        if (path == null || path.isBlank()) path = "/";
        if (!path.startsWith("/")) path = "/" + path;
        int timeout = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        long start = System.currentTimeMillis();

        String urlStr = "http://" + host + ":" + port + path;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;
            boolean ok = status >= 200 && status < 400;

            return PortProbeResultDTO.builder()
                    .host(host)
                    .port(port)
                    .protocol("HTTP")
                    .probeType("HTTP")
                    .reachable(ok)
                    .httpStatus(status)
                    .latencyMs(latency)
                    .message("HTTP " + status + " (" + latency + "ms) " + urlStr)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return PortProbeResultDTO.builder()
                    .host(host)
                    .port(port)
                    .protocol("HTTP")
                    .probeType("HTTP")
                    .reachable(false)
                    .latencyMs(latency)
                    .message("HTTP 探测失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * TLS/SSL 证书探测
     */
    public PortProbeResultDTO probeTls(String host, int port, int timeoutMs) {
        if (host == null || host.isBlank()) host = "127.0.0.1";
        int timeout = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        long start = System.currentTimeMillis();

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{TRUST_ALL}, null);
            SSLSocketFactory factory = ctx.getSocketFactory();

            try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
                socket.connect(new InetSocketAddress(host, port), timeout);
                socket.startHandshake();
                long latency = System.currentTimeMillis() - start;

                X509Certificate cert = (X509Certificate) socket.getSession()
                        .getPeerCertificates()[0];
                Date notAfter = cert.getNotAfter();
                String subject = cert.getSubjectX500Principal().getName();
                String issuer = cert.getIssuerX500Principal().getName();
                long daysLeft = (notAfter.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);

                String certInfo = String.format("Subject: %s | Issuer: %s | 过期: %s (剩余 %d 天)",
                        subject, issuer, notAfter, daysLeft);

                return PortProbeResultDTO.builder()
                        .host(host)
                        .port(port)
                        .protocol("TLS")
                        .probeType("TLS")
                        .reachable(true)
                        .latencyMs(latency)
                        .certInfo(certInfo)
                        .message("TLS 握手成功 (" + latency + "ms)")
                        .build();
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return PortProbeResultDTO.builder()
                    .host(host)
                    .port(port)
                    .protocol("TLS")
                    .probeType("TLS")
                    .reachable(false)
                    .latencyMs(latency)
                    .message("TLS 探测失败: " + e.getMessage())
                    .build();
        }
    }

    private static final TrustManager TRUST_ALL = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    };
}
