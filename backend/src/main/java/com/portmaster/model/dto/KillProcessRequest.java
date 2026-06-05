package com.portmaster.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量杀进程请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KillProcessRequest {

    private List<Long> pids;
    /** true=强制杀死, false=正常结束 */
    private Boolean force;
}
