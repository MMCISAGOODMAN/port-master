package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 端口冲突检测结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortConflictDTO {

    private Integer port;
    private String protocol;
    private List<Long> pids;
    private List<String> processNames;
    private String message;
}
