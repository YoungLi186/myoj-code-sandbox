package com.yl.myoj.codesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yl.myoj.codesandbox.model.ExecuteCodeRequest;
import com.yl.myoj.codesandbox.model.ExecuteCodeResponse;
import com.yl.myoj.codesandbox.model.ExecuteMessage;
import com.yl.myoj.codesandbox.model.JudgeInfo;
import com.yl.myoj.codesandbox.utils.ProcessUtils;
import org.apache.coyote.http11.filters.IdentityOutputFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @Date: 2023/9/15 - 09 - 15 - 21:04
 * @Description: com.yl.myoj.codesandbox
 */
public class JavaNativeCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);

    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();

        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //1)判断全局代码目录是否存在,没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        //2)将用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();//加上uuid父目录
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;

        File useCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        //3)编译代码得到.class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", useCodeFile.getAbsolutePath());
        try {
            Process process = Runtime.getRuntime().exec(compileCmd);
            String opName = "编译";
            ProcessUtils.runProcessAndGetMessage(process, opName);//编译
        } catch (Exception e) {
            return getErrorResponse(e);
        }
        
        //4)执行代码,获得所有的输出信息
        List<ExecuteMessage> executeMessageList =new ArrayList<>();
        for (String inputArgs :inputList) {

            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            try{
                Process process = Runtime.getRuntime().exec(runCmd);
                String opName = "运行";
                //ExecuteMessage executeMessage = ProcessUtils.runInterProcessAndGetMessage(process, opName,inputArgs);交互式执行
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(process, opName);
                executeMessageList.add(executeMessage);
            }catch (Exception e){
                return getErrorResponse(e);
            }
        }


        //5)整理输出信息
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outPutList = new ArrayList<>();
        long maxTime = 0;//取用时最大值
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);//执行中出现错误
                break;
            }
            outPutList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time!=null){
                maxTime =Math.max(maxTime,time);
            }
        }


        if (outPutList.size()==executeMessageList.size()){
            executeCodeResponse.setStatus(1);//没有错误输出
        }
        executeCodeResponse.setOutputList(outPutList);//设置输出
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //judgeInfo.setMemory();非常麻烦，不实现
        executeCodeResponse.setJudgeInfo(judgeInfo);

        //6)文件清理
        if (useCodeFile.getParentFile()!=null){
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("文件删除"+(del?"成功":"失败"));
        }


        return executeCodeResponse;

    }


    /**
     * 异常处理方法
     * @param e 异常
     * @return ExecuteCodeResponse
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);//代码沙箱错误
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        return executeCodeResponse;
    }
}