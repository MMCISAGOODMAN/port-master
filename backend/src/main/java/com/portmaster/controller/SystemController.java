package com.portmaster.controller;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.SystemStatsDTO;
import com.portmaster.service.SystemMonitorService;
import com.portmaster.util.OsDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统监控大盘接口
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemMonitorService systemMonitorService;

    /** 获取系统监控统计 */
    @GetMapping("/stats")
    public ApiResponse<SystemStatsDTO> getStats() {
        return ApiResponse.success(systemMonitorService.getSystemStats());
    }

    /** 获取系统信息与权限提示 */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getSystemInfo() {
        String osType = OsDetector.getOsName();
        String permissionHint;
        if (OsDetector.isWindows()) {
            permissionHint = "Windows 系统需要以管理员身份运行 jar 包，才能终止进程和查看完整信息";
        } else {
            permissionHint = "Linux/macOS 系统建议使用 sudo java -jar 启动，以获取完整权限";
        }
        return ApiResponse.success(Map.of(
                "osType", osType,
                "osCategory", OsDetector.getOsType().name(),
                "javaVersion", System.getProperty("java.version"),
                "permissionHint", permissionHint
        ));
    }
}
