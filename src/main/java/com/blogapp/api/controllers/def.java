package com.blogapp.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class def {
    @Autowired
    static ApplicationContext applicationContext;

    public static void main(String[] args) throws IOException {
        abc a = applicationContext.getBean(abc.class);
        abc b = applicationContext.getBean(abc.class);
        System.out.println(a == b);
    }
}
