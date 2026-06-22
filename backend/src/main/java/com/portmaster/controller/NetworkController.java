package com.portmaster.controller;

import com.portmaster.model.dto.ApiResponse;
import com.portmaster.model.dto.NetworkInterfaceDTO;
import com.portmaster.service.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 网络接口信息接口
 */
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    /** 获取本机网络接口列表 */
    @GetMapping("/interfaces")
    public ApiResponse<List<NetworkInterfaceDTO>> listInterfaces() {
        return ApiResponse.success(networkService.listInterfaces());
    }
}
