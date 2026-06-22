package com.portmaster.service;

import com.portmaster.config.PortMasterProperties;
import com.portmaster.model.dto.MonitorAlertEventDTO;
import com.portmaster.model.dto.PortMonitorRequest;
import com.portmaster.model.dto.PortMonitorResultDTO;
import com.portmaster.websocket.MonitorWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台监控轮询调度器，通过 WebSocket 推送告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorAlertScheduler {

    private final MonitorRegistryService registry;
    private final PortService portService;
    private final MonitorWebSocketHandler webSocketHandler;
    private final PortMasterProperties properties;

    @Scheduled(fixedDelayString = "${portmaster.monitor.poll-interval-ms:5000}")
    public void pollAndBroadcast() {
        if (!registry.isEnabled() || registry.getPorts().isEmpty()) {
            return;
        }

        PortMonitorRequest request = PortMonitorRequest.builder()
                .ports(registry.getPorts())
                .build();

        List<PortMonitorResultDTO> results;
        try {
            results = portService.monitorPorts(request);
        } catch (Exception e) {
            log.debug("monitor poll failed", e);
            return;
        }

        List<MonitorAlertEventDTO> alerts = new ArrayList<>();

        for (PortMonitorResultDTO r : results) {
            int port = r.getPort();
            boolean occupied = Boolean.TRUE.equals(r.getOccupied());
            Boolean prev = registry.getLastOccupied(port);

            if (prev == null) {
                registry.initSnapshot(port, occupied);
                continue;
            }

            PortMonitorRequest.MonitorPortItem item = registry.getPorts().stream()
                    .filter(p -> p.getPort() != null && p.getPort() == port)
                    .findFirst().orElse(null);

            String expected = item != null ? item.getExpectedState() : "any";

            if (!prev.equals(occupied)) {
                alerts.add(buildAlert(r, item, occupied ? "occupied" : "released"));
            } else if ("occupied".equals(expected) && !occupied) {
                alerts.add(buildAlert(r, item, "expected_occupied"));
            } else if ("free".equals(expected) && occupied) {
                alerts.add(buildAlert(r, item, "expected_free"));
            }

            registry.setLastOccupied(port, occupied);
        }

        if (!alerts.isEmpty()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "alert");
            payload.put("alerts", alerts);
            webSocketHandler.broadcast(payload);
        }
    }

    private MonitorAlertEventDTO buildAlert(PortMonitorResultDTO r,
                                             PortMonitorRequest.MonitorPortItem item,
                                             String reason) {
        return MonitorAlertEventDTO.builder()
                .port(r.getPort())
                .protocol(r.getProtocol())
                .occupied(r.getOccupied())
                .processName(r.getProcessName())
                .pid(r.getPid())
                .remark(item != null ? item.getRemark() : null)
                .reason(reason)
                .timestamp(Instant.now().toString())
                .build();
    }
}
