package com.acme.biz.client.cloud;


import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * 被代理类
 */
public class TargetType {

    public String hello() {
        return "hello !";
    }

    @ProxyMethod
    public String hello(String msg) {
        return "hello : " + msg;
    }

    @ProxyMethod
    public String hello(String msg, Integer num) {
        return "hello : " + msg + num;
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {

        TargetType targetType = new ByteBuddy()
                .subclass(TargetType.class)
                .method(ElementMatchers.isAnnotatedWith(ProxyMethod.class))
                .intercept(MethodDelegation.to(ProxyType.class))
                .make()
                .load(TargetType.class.getClassLoader())
                .getLoaded().newInstance();

        System.out.println(targetType.hello());

        System.out.println(targetType.hello("123"));

        System.out.println(targetType.hello("123",456));


    }

}
