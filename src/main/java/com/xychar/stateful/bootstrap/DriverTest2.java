package com.xychar.stateful.bootstrap;

import com.xychar.stateful.container.ContainerEngine;
import com.xychar.stateful.container.ContainerHandler;
import com.xychar.stateful.container.ContainerProxy;
import com.xychar.stateful.container.GetterAndSetter;
import com.xychar.stateful.spring.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Callable;

public class DriverTest2 {
    static final Logger logger = LoggerFactory.getLogger(DriverTest2.class);

    public interface ContainerTest1 {
        default String getHello() {
            return "H-" + getWorld();
        }

        default String getWorld() {
            return "W" + UUID.randomUUID().toString();
        }
    }

    public static void main(String[] args) {
        AbstractApplicationContext context = AppConfig.initialize();
        ContainerEngine engine = new ContainerEngine();

        try {
            Constructor<? extends ContainerProxy> out = engine.buildContainerProxy(ContainerTest1.class);
            ContainerProxy p = out.newInstance();

            p.handler = new ContainerHandler() {

                @Override
                public Object interceptProperty(ContainerProxy parent, Method method,
                                                Method defaultMethod, Callable<?> invocation,
                                                GetterAndSetter field, Object... args) throws Throwable {
                    Object value = field.getValue();
                    if (value == null) {
                        value = invocation.call();
                        field.setValue(value);
                    }
                    return value;
                }

                @Override
                public Object interceptProperty(ContainerProxy instance, Method method, Object... args) throws Throwable {
                    return null;
                }
            };

            ContainerTest1 ct1 = (ContainerTest1) p;

            System.out.format("Hello1: %s%n", ct1.getHello());
            System.out.format("World1: %s%n", ct1.getWorld());
            System.out.format("Hello2: %s%n", ct1.getHello());
            System.out.format("World2: %s%n", ct1.getWorld());
            System.out.format("Hello3: %s%n", ct1.getHello());
            System.out.format("World3: %s%n", ct1.getWorld());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
