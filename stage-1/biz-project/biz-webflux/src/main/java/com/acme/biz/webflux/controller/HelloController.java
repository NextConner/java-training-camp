package com.acme.biz.webflux.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jintaoZou
 * @date 2022/10/24-8:50
 */
@RestController
@RequestMapping
public class HelloController {


    @GetMapping(value = "/hello/{msg}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hello(@PathVariable String msg) {

        return "hello : " + msg + (1/0);
    }

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hello() {
        return "hello : Default!";
    }


}
