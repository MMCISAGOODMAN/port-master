package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 空闲端口生成结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreePortResultDTO {

    private Integer startPort;
    private Integer count;
    private List<Integer> freePorts;
    private String message;
}
