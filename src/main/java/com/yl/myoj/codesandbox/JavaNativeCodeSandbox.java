package com.yl.myoj.codesandbox;

import com.github.dockerjava.api.DockerClient;
import com.yl.myoj.codesandbox.model.ExecuteCodeRequest;
import com.yl.myoj.codesandbox.model.ExecuteCodeResponse;
import com.yl.myoj.codesandbox.model.ExecuteMessage;
import com.yl.myoj.codesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 原生实现，重写部分方法即可
 */
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    private static final long TIME_OUT = 5000L;


    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws IOException {
        //执行代码,获得所有的输出信息
        String userCodeParentPath = userCodeFile.getParentFile().getPath();
        List<ExecuteMessage> executeMessageList =new ArrayList<>();
        for (String inputArgs :inputList) {
            //-Xmx256m 限制初始堆空间大小
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);

                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(()->{
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();//限制执行的时间
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                String opName = "运行";
                //ExecuteMessage executeMessage = ProcessUtils.runInterProcessAndGetMessage(process, opName,inputArgs);交互式执行
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, opName);
                executeMessageList.add(executeMessage);

        }
        return executeMessageList;
    }



    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
