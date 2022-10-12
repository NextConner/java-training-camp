package com.acme.biz.web.client.feign;

import com.acme.biz.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author jintaoZou
 * @date 2022/10/12-15:51
 */
public class ApiResponseHttpMessageConvertor extends AbstractJackson2HttpMessageConverter{


    public static Class[] allReadTypes = {ApiResponse.class, int.class, Integer.class, long.class, Long.class, char.class, Character.class,
            short.class, Short.class, byte.class, Byte.class, boolean.class, Boolean.class, float.class, Float.class, double.class, Double.class, String.class};


    protected ApiResponseHttpMessageConvertor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return clazz.equals(ApiResponse.class) && mediaType.equals(MediaType.APPLICATION_JSON);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.APPLICATION_JSON);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return super.readInternal(clazz, inputMessage);
    }
}
