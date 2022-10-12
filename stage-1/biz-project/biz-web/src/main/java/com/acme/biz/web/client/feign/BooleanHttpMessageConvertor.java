package com.acme.biz.web.client.feign;

import feign.Util;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author jintaoZou
 * @date 2022/10/12-17:15
 */
public class BooleanHttpMessageConvertor implements HttpMessageConverter<Boolean> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return clazz.equals(Boolean.class) || clazz.equals(boolean.class)
                || mediaType.toString().contains(APPLICATION_JSON_VALUE);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(APPLICATION_JSON);
    }

    @Override
    public Boolean read(Class<? extends Boolean> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

        try {
            String str = Util.toString(new InputStreamReader(inputMessage.getBody()));
            return StringUtils.hasLength(str) ?  Boolean.parseBoolean(str) : null;
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public void write(Boolean aBoolean, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

    }
}
