package com.acme.biz.client.cloud;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class ProxyType {

    public static String hello(@This Object instance, @Origin Method method, @SuperCall Callable<String> callable, @AllArguments Object... args) {

        Class<?> returnType = method.getReturnType();

        if (!returnType.isAssignableFrom(void.class)) {
            System.out.println("proxy call !");
            String result;
            try {
                result = callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        }
        return null;
    }

}
