package com.xychar.stateful.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class StartPoint {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ExceptionConfig.class);
        context.register(AppConfig.class);

        context.refresh();
        context.start();

        try {
            System.out.println("Hello");
        } finally {
            context.stop();
            context.close();
        }
    }
}
