package com.portmaster.controller;

import com.portmaster.model.dto.*;
import com.portmaster.service.PortProbeService;
import com.portmaster.service.PortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 端口扫描与查询接口
 */
@RestController
@RequestMapping("/api/ports")
@RequiredArgsConstructor
public class PortController {

    private final PortService portService;
    private final PortProbeService portProbeService;

    /** 全量端口扫描 */
    @GetMapping("/scan")
    public ApiResponse<List<PortInfoDTO>> scanAll(
            @RequestParam(defaultValue = "false") boolean refresh) {
        return ApiResponse.success(portService.scanAllPorts(refresh));
    }

    /** 单端口查询 */
    @GetMapping("/query/{port}")
    public ApiResponse<List<PortInfoDTO>> queryByPort(@PathVariable int port) {
        return ApiResponse.success(portService.queryByPort(port));
    }

    /** 批量端口查询（逗号分隔，支持 8000-8100 范围） */
    @GetMapping("/query")
    public ApiResponse<List<PortInfoDTO>> queryByPorts(@RequestParam String ports) {
        return ApiResponse.success(portService.queryByPorts(ports));
    }

    /** 端口范围查询 */
    @GetMapping("/query/range")
    public ApiResponse<List<PortInfoDTO>> queryByRange(
            @RequestParam int start,
            @RequestParam int end) {
        return ApiResponse.success(portService.queryByRange(start, end));
    }

    /** 检测端口冲突 */
    @GetMapping("/conflicts")
    public ApiResponse<List<PortConflictDTO>> detectConflicts() {
        return ApiResponse.success(portService.detectConflicts());
    }

    /** 根据进程名称反查端口 */
    @GetMapping("/query/process")
    public ApiResponse<List<PortInfoDTO>> queryByProcessName(@RequestParam String name) {
        return ApiResponse.success(portService.queryByProcessName(name));
    }

    /** 根据 PID 反查端口 */
    @GetMapping("/query/pid/{pid}")
    public ApiResponse<List<PortInfoDTO>> queryByPid(@PathVariable long pid) {
        return ApiResponse.success(portService.queryByPid(pid));
    }

    /** 空闲端口生成 */
    @GetMapping("/free")
    public ApiResponse<FreePortResultDTO> generateFreePorts(
            @RequestParam(defaultValue = "8080") int start,
            @RequestParam(defaultValue = "5") int count) {
        return ApiResponse.success(portService.generateFreePorts(start, count));
    }

    /** 端口监控探测 */
    @PostMapping("/monitor")
    public ApiResponse<List<PortMonitorResultDTO>> monitorPorts(@RequestBody PortMonitorRequest request) {
        return ApiResponse.success(portService.monitorPorts(request));
    }

    /** 扫描结果汇总统计 */
    @GetMapping("/summary")
    public ApiResponse<PortSummaryDTO> getSummary() {
        return ApiResponse.success(portService.getSummary());
    }

    /** TCP 端口连通性探测 */
    @GetMapping("/probe")
    public ApiResponse<PortProbeResultDTO> probePort(
            @RequestParam int port,
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "3000") int timeout) {
        return ApiResponse.success(portProbeService.probeTcp(host, port, timeout));
    }

    /** 批量 TCP 端口探测 */
    @PostMapping("/probe/batch")
    public ApiResponse<List<PortProbeResultDTO>> probeBatch(@RequestBody Map<String, Object> body) {
        String host = body.get("host") != null ? body.get("host").toString() : "127.0.0.1";
        int timeout = body.get("timeout") != null ? ((Number) body.get("timeout")).intValue() : 3000;
        @SuppressWarnings("unchecked")
        List<Integer> ports = (List<Integer>) body.get("ports");
        List<PortProbeResultDTO> results = new java.util.ArrayList<>();
        if (ports != null) {
            for (Integer port : ports) {
                if (port != null) {
                    results.add(portProbeService.probeTcp(host, port, timeout));
                }
            }
        }
        return ApiResponse.success(results);
    }

    /** HTTP 健康探测 */
    @GetMapping("/probe/http")
    public ApiResponse<PortProbeResultDTO> probeHttp(
            @RequestParam int port,
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "3000") int timeout) {
        return ApiResponse.success(portProbeService.probeHttp(host, port, path, timeout));
    }

    /** TLS/SSL 证书探测 */
    @GetMapping("/probe/tls")
    public ApiResponse<PortProbeResultDTO> probeTls(
            @RequestParam int port,
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "3000") int timeout) {
        return ApiResponse.success(portProbeService.probeTls(host, port, timeout));
    }
}
