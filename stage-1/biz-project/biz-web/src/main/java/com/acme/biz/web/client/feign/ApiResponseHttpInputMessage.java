package com.acme.biz.web.client.feign;

import feign.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author jintaoZou
 * @date 2022/10/12-10:58
 */
public class ApiResponseHttpInputMessage implements HttpInputMessage {

    private InputStream bodyStream;
    private HttpHeaders headers;

    public ApiResponseHttpInputMessage(Response response) throws IOException {
        this.bodyStream = response.body().asInputStream();
        this.headers = new HttpHeaders();
        response.headers().entrySet().forEach(entry -> this.headers.addAll(entry.getKey(), new ArrayList(entry.getValue())));
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.bodyStream;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }
}
