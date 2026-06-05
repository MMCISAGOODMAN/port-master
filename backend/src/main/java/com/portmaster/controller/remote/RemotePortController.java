package com.portmaster.controller.remote;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.service.remote.RemotePortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SSH 远程服务器端口管理接口（预留扩展，暂未实现）
 *
 * <p>预留 RESTful 接口设计，便于后续接入 JSch / Apache MINA SSHD
 */
@RestController
@RequestMapping("/api/remote")
@RequiredArgsConstructor
public class RemotePortController {

    private final RemotePortService remotePortService;

    /**
     * 远程端口扫描（预留）
     */
    @PostMapping("/scan")
    public ApiResponse<List<PortInfoDTO>> scanRemote(@RequestBody Map<String, Object> request) {
        String host = (String) request.get("host");
        int port = request.get("port") != null ? (int) request.get("port") : 22;
        String username = (String) request.get("username");
        String credential = (String) request.get("credential");
        return ApiResponse.success(remotePortService.scanRemotePorts(host, port, username, credential));
    }

    /**
     * 远程杀进程（预留）
     */
    @PostMapping("/kill")
    public ApiResponse<Boolean> killRemote(@RequestBody Map<String, Object> request) {
        String host = (String) request.get("host");
        int port = request.get("port") != null ? (int) request.get("port") : 22;
        String username = (String) request.get("username");
        String credential = (String) request.get("credential");
        long pid = ((Number) request.get("pid")).longValue();
        boolean force = request.get("force") != null && (boolean) request.get("force");
        return ApiResponse.success(remotePortService.killRemoteProcess(host, port, username, credential, pid, force));
    }

    /**
     * 测试 SSH 连接（预留）
     */
    @PostMapping("/test")
    public ApiResponse<Boolean> testConnection(@RequestBody Map<String, Object> request) {
        String host = (String) request.get("host");
        int port = request.get("port") != null ? (int) request.get("port") : 22;
        String username = (String) request.get("username");
        String credential = (String) request.get("credential");
        return ApiResponse.success(remotePortService.testConnection(host, port, username, credential));
    }
}
