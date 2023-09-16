package com.yl.myoj.codesandbox.model;

import lombok.Data;

/**
 * @Date: 2023/9/16 - 09 - 16 - 9:13
 * @Description: com.yl.myoj.codesandbox.model
 * 控制台返回消息封装类
 */
@Data
public class ExecuteMessage {
    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

}
