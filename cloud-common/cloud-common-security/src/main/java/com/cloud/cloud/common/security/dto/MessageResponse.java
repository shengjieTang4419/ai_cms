package com.cloud.cloud.common.security.dto;

import lombok.Data;

/**
 * 消息响应
 */
@Data
public class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }
}
