package com.portmaster.service.remote;

import com.portmaster.model.dto.PortInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * SSH 远程服务器端口查询服务（预留扩展，暂未实现）
 *
 * <p>未来可通过 JSch 或 Apache MINA SSHD 实现远程端口扫描与进程管理。
 * 接口设计：
 * <ul>
 *   <li>connect(host, port, username, password/key) - 建立 SSH 连接</li>
 *   <li>scanRemotePorts(sessionId) - 远程全量端口扫描</li>
 *   <li>killRemoteProcess(sessionId, pid, force) - 远程杀进程</li>
 * </ul>
 */
@Slf4j
@Service
public class RemotePortService {

    /**
     * 远程端口扫描（预留）
     */
    public List<PortInfoDTO> scanRemotePorts(String host, int sshPort, String username, String credential) {
        log.info("Remote port scan not implemented yet. host={}, port={}", host, sshPort);
        throw new UnsupportedOperationException("SSH 远程端口查询功能尚未实现，敬请期待");
    }

    /**
     * 远程杀进程（预留）
     */
    public boolean killRemoteProcess(String host, int sshPort, String username, String credential,
                                     long pid, boolean force) {
        log.info("Remote kill process not implemented yet. host={}, pid={}", host, pid);
        throw new UnsupportedOperationException("SSH 远程杀进程功能尚未实现，敬请期待");
    }

    /**
     * 测试 SSH 连接（预留）
     */
    public boolean testConnection(String host, int sshPort, String username, String credential) {
        log.info("Remote connection test not implemented yet. host={}", host);
        return false;
    }
}
