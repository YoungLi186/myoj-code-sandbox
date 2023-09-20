package com.yl.myoj.codesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.yl.myoj.codesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Date: 2023/9/16 - 09 - 16 - 9:13
 * @Description: com.yl.myoj.codesandbox.utils
 * 进程执行工具类
 */
public class ProcessUtils {

    /**
     * 编译执行程序返回正确和错误的信息
     * @param runProcess 进程
     * @param opName 操作名
     * @return ExecuteMessage
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess,String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            StopWatch stopWatch=new StopWatch();
            stopWatch.start();//计时开始
            //等待程序执行
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(opName+"成功");
                //分批获取正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outPutList= new ArrayList<>();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outPutList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outPutList,"\n"));
            } else {
                //异常退出
                System.out.println(opName+"失败，错误码：" + exitValue);
                //分批获取正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outPutList= new ArrayList<>();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outPutList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outPutList,"\n"));


                //分批获取错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                List<String> errorOutPutList= new ArrayList<>();
                //逐行读取
                String errorCompileOutputLine;

                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutPutList.add(errorCompileOutputLine);
                }

                executeMessage.setErrorMessage(StringUtils.join(errorOutPutList,"\n"));
            }
            stopWatch.stop();//计时结束
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }


        return executeMessage;

    }


    /**
     * 用于执行交互式程序
     * @param runProcess 进程
     * @param opName 操作名
     * @param args 参数
     * @return ExecuteMessage
     */
    public static ExecuteMessage runInterProcessAndGetMessage(Process runProcess,String opName,String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try{
            OutputStream outputStream = runProcess.getOutputStream();
            InputStream inputStream = runProcess.getInputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s)+"\n";
            outputStreamWriter.write(join);
            outputStreamWriter.flush();
            System.out.println(opName+"成功");
            //分批获取正常输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            //逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            outputStreamWriter.close();
            outputStream.close();
            bufferedReader.close();
            runProcess.destroy();
        }catch (Exception e){
           e.printStackTrace();
        }


        return executeMessage;

    }
}
