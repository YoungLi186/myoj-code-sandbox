package com.yl.myoj.codesandbox.controller;

import com.yl.myoj.codesandbox.CodeSandbox;
import com.yl.myoj.codesandbox.JavaNativeCodeSandbox;
import com.yl.myoj.codesandbox.model.ExecuteCodeRequest;
import com.yl.myoj.codesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Date: 2023/9/15 - 09 - 15 - 20:26
 * @Description: com.yl.myoj.codesandbox.controller
 */

@RestController
@RequestMapping("/")
@Slf4j
public class MainController {


    //鉴权请求头和秘钥
    private static final String AUTH_REQUEST_HEADER="auth";
    private static final String AUTH_REQUEST_SECRET="secretKey";




    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;


    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if(!authHeader.equals(AUTH_REQUEST_SECRET)){
            response.setStatus(403);
            return null;
        }

        if (executeCodeRequest==null){
            throw new RuntimeException();
        }
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);

        System.out.println(executeCodeResponse);

        return executeCodeResponse;

    }

}
