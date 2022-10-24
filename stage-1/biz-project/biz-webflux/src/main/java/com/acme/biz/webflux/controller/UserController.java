package com.acme.biz.webflux.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jintaoZou
 * @date 2022/10/24-13:40
 */

@RestController
public class UserController {

    @GetMapping("/user")
    public String user(){
        return "User : " + System.nanoTime();
    }

}
