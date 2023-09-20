package com.yl.myoj.codesandbox;

import com.yl.myoj.codesandbox.model.ExecuteCodeRequest;
import com.yl.myoj.codesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
