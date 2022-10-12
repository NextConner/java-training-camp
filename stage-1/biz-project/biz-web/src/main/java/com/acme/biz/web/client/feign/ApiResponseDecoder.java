package com.acme.biz.web.client.feign;

import com.acme.biz.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jintaoZou
 * @date 2022/10/12-9:12
 */


public class ApiResponseDecoder implements Decoder {

    private List<HttpMessageConverter<?>> messageConverters;


    private static final boolean jackson2Present;
    private static final boolean jackson2XmlPresent;
    private static final boolean jackson2SmilePresent;
    private static final boolean jackson2CborPresent;
    private static final boolean gsonPresent;
    private static final boolean jsonbPresent;


    static {
        ClassLoader classLoader = ApiResponseDecoder.class.getClassLoader();
        jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
        jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
        jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
        jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
        gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
        jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
    }

    public ApiResponseDecoder() {
        messageConverters = new ArrayList<>();

        this.messageConverters.add(new ByteArrayHttpMessageConverter());
        this.messageConverters.add(new StringHttpMessageConverter());
        this.messageConverters.add(new ResourceHttpMessageConverter(false));
        if (jackson2Present) {
            this.messageConverters.add(new MappingJackson2HttpMessageConverter());
        } else if (gsonPresent) {
            this.messageConverters.add(new GsonHttpMessageConverter());
        } else if (jsonbPresent) {
            this.messageConverters.add(new JsonbHttpMessageConverter());
        }
        this.messageConverters.add(new ApiResponseHttpMessageConvertor(new ObjectMapper()));
        this.messageConverters.add(new BooleanHttpMessageConvertor());
    }

    @SneakyThrows
    @Override
    public Object decode(Response response, Type type) throws FeignException {


        int status = response.status();
        String reason = response.reason();
        //非成功响应请求返回失败类型响应
        if (!HttpStatus.valueOf(status).is2xxSuccessful()) {
            return ApiResponse.failed(reason);
        }
        Class clazz = null;
        Class innerType = null;
        //检查是否是返回带有泛型
        if (type instanceof ParameterizedTypeImpl) {
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) type;

            Class rawType = parameterizedType.getRawType();
            if (rawType.equals(ApiResponse.class)) {
                innerType = getClass().getClassLoader().loadClass(parameterizedType.getActualTypeArguments()[0].getTypeName());
            } else {
                clazz = rawType;
            }
        } else {
            clazz = getClass().getClassLoader().loadClass(type.getTypeName());
        }

        Collection<String> contentTypes = response.headers().get(HttpHeaders.CONTENT_TYPE);
        MediaType mediaType = Optional.of(MediaType.valueOf(contentTypes.toArray(new String[0])[0])).orElse(MediaType.ALL);

        Object readResult = null;
        for (HttpMessageConverter converter : messageConverters) {
            if (converter.canRead(clazz, mediaType)) {
                try {

                    try {
                        readResult = converter.read(clazz, new ApiResponseHttpInputMessage(response));
                    } catch (Exception e) {
                    }


                    if (null == readResult && innerType != null) {
                        try {
                            readResult = converter.read(innerType, new ApiResponseHttpInputMessage(response));
                        } catch (Exception e) {
                        }
                    } else if (null == readResult) {
                        readResult = converter.read(clazz, new ApiResponseHttpInputMessage(response));
                    }

                } catch (Exception e) {
                } finally {
                    if (null != readResult) {
                        break;
                    }
                }
            }
        }

        if (null == readResult) {
            return ApiResponse.failed("can not read the response!");
        }
        if (readResult instanceof ApiResponse) {
            return readResult;
        } else {
            return ApiResponse.ok(readResult);
        }

    }
}
