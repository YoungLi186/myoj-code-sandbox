package com.yl.myoj.codesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Date: 2023/9/15 - 09 - 15 - 20:26
 * @Description: com.yl.myoj.codesandbox.controller
 */

@RestController
@RequestMapping("/")
public class MainController {
    @GetMapping("/health")
    public String healthCase(){
        return "ok";
    }
}
