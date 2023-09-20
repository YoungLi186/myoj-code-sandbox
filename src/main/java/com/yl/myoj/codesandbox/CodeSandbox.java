package com.yl.myoj.codesandbox;

import com.yl.myoj.codesandbox.model.ExecuteCodeRequest;
import com.yl.myoj.codesandbox.model.ExecuteCodeResponse;
/**
 * @Date: 2023/9/13 - 09 - 13 - 20:48
 * @Description: com.yl.myoj.judge.codesandbox
 * 代码沙箱接口
 */
public interface CodeSandbox {

    /**
     * 代码沙箱执行方法
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);


}
