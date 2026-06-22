package com.portmaster.service;

import com.portmaster.model.dto.PortMonitorRequest;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 端口监控配置内存注册表（供 WebSocket 后台轮询使用）
 */
@Service
public class MonitorRegistryService {

    private volatile boolean enabled;
    private final List<PortMonitorRequest.MonitorPortItem> ports = new ArrayList<>();
    private final ConcurrentHashMap<Integer, Boolean> lastOccupied = new ConcurrentHashMap<>();

    public synchronized void updateConfig(boolean enabled, List<PortMonitorRequest.MonitorPortItem> portList) {
        this.enabled = enabled;
        this.ports.clear();
        if (portList != null) {
            this.ports.addAll(portList);
        }
        if (!enabled) {
            lastOccupied.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public synchronized List<PortMonitorRequest.MonitorPortItem> getPorts() {
        return new ArrayList<>(ports);
    }

    public Boolean getLastOccupied(int port) {
        return lastOccupied.get(port);
    }

    public void setLastOccupied(int port, boolean occupied) {
        lastOccupied.put(port, occupied);
    }

    public void initSnapshot(int port, boolean occupied) {
        lastOccupied.putIfAbsent(port, occupied);
    }
}
