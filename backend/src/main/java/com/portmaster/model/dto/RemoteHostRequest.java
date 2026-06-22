package com.portmaster.model.dto;

import lombok.Data;

/**
 * SSH 远程主机请求参数
 */
@Data
public class RemoteHostRequest {

    private String host;
    private Integer port = 22;
    private String username;
    /** 密码或私钥内容 */
    private String credential;
    /** password | key */
    private String authType = "password";
}
