package com.portmaster.controller.remote;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.PortInfoDTO;
import com.portmaster.model.dto.RemoteHostRequest;
import com.portmaster.service.remote.RemotePortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SSH 远程服务器端口管理接口
 */
@RestController
@RequestMapping("/api/remote")
@RequiredArgsConstructor
public class RemotePortController {

    private final RemotePortService remotePortService;

    /** 远程端口扫描 */
    @PostMapping("/scan")
    public ApiResponse<List<PortInfoDTO>> scanRemote(@RequestBody RemoteHostRequest request) {
        int port = request.getPort() != null ? request.getPort() : 22;
        String authType = request.getAuthType() != null ? request.getAuthType() : "password";
        return ApiResponse.success(remotePortService.scanRemotePorts(
                request.getHost(), port, request.getUsername(),
                request.getCredential(), authType));
    }

    /** 远程杀进程 */
    @PostMapping("/kill")
    public ApiResponse<Boolean> killRemote(@RequestBody Map<String, Object> request) {
        String host = (String) request.get("host");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 22;
        String username = (String) request.get("username");
        String credential = (String) request.get("credential");
        String authType = request.get("authType") != null ? request.get("authType").toString() : "password";
        long pid = ((Number) request.get("pid")).longValue();
        boolean force = request.get("force") != null && (boolean) request.get("force");
        return ApiResponse.success(remotePortService.killRemoteProcess(
                host, port, username, credential, authType, pid, force));
    }

    /** 测试 SSH 连接 */
    @PostMapping("/test")
    public ApiResponse<Boolean> testConnection(@RequestBody RemoteHostRequest request) {
        int port = request.getPort() != null ? request.getPort() : 22;
        String authType = request.getAuthType() != null ? request.getAuthType() : "password";
        return ApiResponse.success(remotePortService.testConnection(
                request.getHost(), port, request.getUsername(),
                request.getCredential(), authType));
    }

    /** 获取远程系统信息 */
    @PostMapping("/info")
    public ApiResponse<String> remoteInfo(@RequestBody RemoteHostRequest request) {
        int port = request.getPort() != null ? request.getPort() : 22;
        String authType = request.getAuthType() != null ? request.getAuthType() : "password";
        return ApiResponse.success(remotePortService.getRemoteSystemInfo(
                request.getHost(), port, request.getUsername(),
                request.getCredential(), authType));
    }
}
