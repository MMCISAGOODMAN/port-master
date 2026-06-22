package com.portmaster.controller;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.DockerContainerDTO;
import com.portmaster.service.DockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Docker 容器端口管理接口
 */
@RestController
@RequestMapping("/api/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerService dockerService;

    /** 检查 Docker 是否可用 */
    @GetMapping("/available")
    public ApiResponse<Boolean> isAvailable() {
        return ApiResponse.success(dockerService.isDockerAvailable());
    }

    /** 列出容器及端口映射 */
    @GetMapping("/containers")
    public ApiResponse<List<DockerContainerDTO>> listContainers(
            @RequestParam(defaultValue = "false") boolean all) {
        return ApiResponse.success(dockerService.listContainers(all));
    }

    /** 停止容器 */
    @PostMapping("/stop")
    public ApiResponse<String> stopContainer(@RequestBody Map<String, String> body) {
        return ApiResponse.success(dockerService.stopContainer(body.get("containerId")));
    }

    /** 重启容器 */
    @PostMapping("/restart")
    public ApiResponse<String> restartContainer(@RequestBody Map<String, String> body) {
        return ApiResponse.success(dockerService.restartContainer(body.get("containerId")));
    }
}
