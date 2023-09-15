package com.yl.myoj.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Date: 2023/9/13 - 09 - 13 - 20:53
 * @Description: com.yl.myoj.judge.codesandbox.model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 输出用例
     */
    private List<String> outputList;

    /**
     * 接口信息
     */
    private String message;

    /**
     * 执行状态
     */
    private Integer status ;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;



}
