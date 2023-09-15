package com.yl.myoj.codesandbox.model;

import lombok.Data;

/**
 * @Date: 2023/9/8 - 09 - 08 - 21:40
 * @Description: com.yl.myoj.model.dto.question
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗内存（kb）
     */
    private Long memory;

    /**
     * 消耗时间（ms）
     */
    private Long time;
}
