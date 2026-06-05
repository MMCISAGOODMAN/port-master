package com.portmaster.controller;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.KillProcessRequest;
import com.portmaster.model.dto.ProcessDetailDTO;
import com.portmaster.model.dto.ProcessInfoDTO;
import com.portmaster.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 进程管理与详情接口
 */
@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    /** 列出所有运行进程 */
    @GetMapping("/list")
    public ApiResponse<List<ProcessInfoDTO>> listAll() {
        return ApiResponse.success(processService.listAllProcesses());
    }

    /** 批量杀进程 */
    @PostMapping("/kill/batch")
    public ApiResponse<List<String>> batchKill(@RequestBody KillProcessRequest request) {
        boolean force = request.getForce() != null && request.getForce();
        List<String> results = processService.killProcesses(request.getPids(), force);
        return ApiResponse.success(results);
    }

    /** 按端口杀死占用进程 */
    @DeleteMapping("/by-port/{port}")
    public ApiResponse<List<String>> killByPort(
            @PathVariable int port,
            @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.success(processService.killByPort(port, force));
    }

    /** 按端口强制杀死 */
    @DeleteMapping("/by-port/{port}/force")
    public ApiResponse<List<String>> forceKillByPort(@PathVariable int port) {
        return ApiResponse.success(processService.killByPort(port, true));
    }

    /** 获取进程详情 */
    @GetMapping("/{pid}")
    public ApiResponse<ProcessDetailDTO> getDetail(@PathVariable long pid) {
        return ApiResponse.success(processService.getProcessDetail(pid));
    }

    /** 正常结束进程 */
    @DeleteMapping("/{pid}")
    public ApiResponse<String> killProcess(@PathVariable long pid) {
        boolean success = processService.killProcess(pid, false);
        if (success) {
            return ApiResponse.success("进程 " + pid + " 已正常结束");
        }
        return ApiResponse.error("进程 " + pid + " 结束失败，可能需要管理员权限");
    }

    /** 强制杀死进程 */
    @DeleteMapping("/{pid}/force")
    public ApiResponse<String> forceKillProcess(@PathVariable long pid) {
        boolean success = processService.killProcess(pid, true);
        if (success) {
            return ApiResponse.success("进程 " + pid + " 已强制终止");
        }
        return ApiResponse.error("进程 " + pid + " 强制终止失败，可能需要管理员权限");
    }
}
