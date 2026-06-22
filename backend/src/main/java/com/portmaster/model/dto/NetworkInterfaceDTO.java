package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络接口信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkInterfaceDTO {

    private String name;
    private String ipAddress;
    private String macAddress;
    private String status;
    private String type;
}
