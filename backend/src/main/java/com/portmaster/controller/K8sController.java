package com.portmaster.controller;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.K8sPodDTO;
import com.portmaster.model.dto.K8sServiceDTO;
import com.portmaster.service.K8sService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes 端口管理接口
 */
@RestController
@RequestMapping("/api/k8s")
@RequiredArgsConstructor
public class K8sController {

    private final K8sService k8sService;

    @GetMapping("/available")
    public ApiResponse<Boolean> isAvailable() {
        return ApiResponse.success(k8sService.isKubectlAvailable());
    }

    @GetMapping("/context")
    public ApiResponse<String> currentContext() {
        return ApiResponse.success(k8sService.getCurrentContext());
    }

    @GetMapping("/pods")
    public ApiResponse<List<K8sPodDTO>> listPods(
            @RequestParam(required = false) String namespace) {
        return ApiResponse.success(k8sService.listPods(namespace));
    }

    @GetMapping("/services")
    public ApiResponse<List<K8sServiceDTO>> listServices(
            @RequestParam(required = false) String namespace) {
        return ApiResponse.success(k8sService.listServices(namespace));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(
            @RequestParam(required = false) String namespace) {
        List<K8sPodDTO> pods = k8sService.listPods(namespace);
        List<K8sServiceDTO> services = k8sService.listServices(namespace);
        long running = pods.stream().filter(p -> "Running".equalsIgnoreCase(p.getStatus())).count();
        return ApiResponse.success(Map.of(
                "context", k8sService.getCurrentContext(),
                "podCount", pods.size(),
                "runningPods", running,
                "serviceCount", services.size()
        ));
    }
}
