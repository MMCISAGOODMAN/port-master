package com.portmaster.controller;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.PortMonitorRequest;
import com.portmaster.service.MonitorRegistryService;
import com.portmaster.websocket.MonitorWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 监控配置同步接口（供 WebSocket 后台轮询）
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorRegistryService registry;
    private final MonitorWebSocketHandler webSocketHandler;

    /** 同步监控配置到服务端 */
    @PostMapping("/config")
    public ApiResponse<Map<String, Object>> syncConfig(@RequestBody Map<String, Object> body) {
        boolean enabled = body.get("enabled") != null && (boolean) body.get("enabled");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> portMaps = (List<Map<String, Object>>) body.get("ports");

        List<PortMonitorRequest.MonitorPortItem> items = new java.util.ArrayList<>();
        if (portMaps != null) {
            for (Map<String, Object> m : portMaps) {
                PortMonitorRequest.MonitorPortItem item = new PortMonitorRequest.MonitorPortItem();
                item.setPort(((Number) m.get("port")).intValue());
                item.setProtocol(m.get("protocol") != null ? m.get("protocol").toString() : "TCP");
                item.setRemark(m.get("remark") != null ? m.get("remark").toString() : null);
                item.setExpectedState(m.get("expectedState") != null ? m.get("expectedState").toString() : "any");
                items.add(item);
            }
        }
        registry.updateConfig(enabled, items);
        return ApiResponse.success(Map.of(
                "enabled", enabled,
                "portCount", items.size(),
                "wsConnections", webSocketHandler.getConnectionCount()
        ));
    }

    /** 获取监控状态 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(Map.of(
                "enabled", registry.isEnabled(),
                "portCount", registry.getPorts().size(),
                "wsConnections", webSocketHandler.getConnectionCount()
        ));
    }
}
