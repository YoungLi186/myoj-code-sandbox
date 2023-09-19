package com.yl.myoj.codesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.yl.myoj.codesandbox.model.ExecuteCodeRequest;
import com.yl.myoj.codesandbox.model.ExecuteCodeResponse;
import com.yl.myoj.codesandbox.model.ExecuteMessage;
import com.yl.myoj.codesandbox.model.JudgeInfo;
import com.yl.myoj.codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Java代码沙箱模板方法的实现
 */
@Slf4j
public abstract  class JavaCodeSandboxTemplate implements CodeSandbox {


    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    private static final long TIME_OUT = 5000L;
    private static final Boolean FIRST_INIT = false;
    private static final String IMAGES = "openjdk:8-alpine";


    /**
     * 将用户的代码隔离存放
     *
     * @param code 代码
     * @return
     */
    public File saveCodeToFile(String code) {
        //判断全局代码目录是否存在,没有则新建
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();//加上uuid父目录
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;

        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 编译代码得到.class文件
     *
     * @param useCodeFile 代码文件
     * @return
     */
    public void compileFile(File useCodeFile) throws IOException {
        String compileCmd = String.format("javac -encoding utf-8 %s", useCodeFile.getAbsolutePath());
        Process process = Runtime.getRuntime().exec(compileCmd);
        String opName = "编译";
        ProcessUtils.runProcessAndGetMessage(process, opName);//编译
    }

    /**
     * 拉取镜像
     *
     * @param dockerClient Java操作docker的客户端
     */
    public void pullImages(DockerClient dockerClient) {
        if (FIRST_INIT) {
            //拉取镜像
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(IMAGES);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };

            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");

        }
    }


    /**
     * 运行代码，获得结果
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws IOException {

        String userCodeParentPath = userCodeFile.getParentFile().getPath();//获取父目录
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGES);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);//设置运行内存
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        //hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));//文件路径映射，容器挂载目录
        CreateContainerResponse createContainerResponse = containerCmd
                .withNetworkDisabled(true)//设置网络配置为关闭
                .withReadonlyRootfs(true)//限制往根目录写文件
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();

        String containerId = createContainerResponse.getId();

        //启动容器
        dockerClient.startContainerCmd(containerId).exec();
        //执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)//得到控制台的输出
                    .exec();
            System.out.println("创建执行命令:" + execCreateCmdResponse);
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            String id = execCreateCmdResponse.getId();
            long time = 0;
            final boolean[] timeOut = {true};

            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeOut[0] = false;//若调用此函数则说明未超时,将超时标志位置为false
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出结果错误：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果正常：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};
            //获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {

                @Override
                public void close() throws IOException {

                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

            };

            statsCmd.exec(statisticsResultCallback);
            try {
                stopWatch.start();
                dockerClient
                        .execStartCmd(id)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);//限制最大运行时长
                stopWatch.stop();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            time = stopWatch.getLastTaskTimeMillis();
            executeMessage.setTime(time);
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        return executeMessageList;

    }




    /**
     * 整理输出信息
     *
     * @param executeMessageList 执行结果集
     * @return executeCodeResponse
     */
    public ExecuteCodeResponse formatExecuteMessage(List<ExecuteMessage> executeMessageList) {
        //5)整理输出信息
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outPutList = new ArrayList<>();
        long maxTime = 0;//取用时最大值
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);//执行中出现错误
                break;
            }
            outPutList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }

            Long memory = executeMessage.getMemory();
            if (memory != null) {
                maxMemory = Math.max(memory, maxMemory);
            }

        }
        if (outPutList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);//没有错误输出
        }
        executeCodeResponse.setOutputList(outPutList);//设置输出
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除代码文件
     *
     * @param userCodeFile 代码文件
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeFile.getParentFile().getPath());
            System.out.println("文件删除" + (del ? "成功" : "失败"));
            return del;
        }

        return true;
    }

    /**
     * 代码沙箱工作流程
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();

        try {

            File userCodeFile = saveCodeToFile(code);//1.保存代码到本地
            compileFile(userCodeFile);//2.编译


            List<ExecuteMessage> executeMessageList = runFile(userCodeFile,inputList);//3.执行程序,获取执行结果

            ExecuteCodeResponse executeCodeResponse = formatExecuteMessage(executeMessageList);//4.整理执行结果

            boolean success = deleteFile(userCodeFile);//5.清理文件
            if (!success) {
                log.error("deleteFile error,userCodeFilePath={}", userCodeFile.getAbsolutePath());
            }
            return executeCodeResponse;
        } catch (Exception e) {
            return getErrorResponse(e);
        }
    }


    /**
     * 异常处理方法
     *
     * @param e 异常
     * @return ExecuteCodeResponse
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);//代码沙箱错误
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
