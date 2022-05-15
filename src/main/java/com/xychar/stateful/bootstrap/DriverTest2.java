package com.xychar.stateful.bootstrap;

import com.xychar.stateful.container.ContainerEngine;
import com.xychar.stateful.container.ContainerMetadata;
import com.xychar.stateful.spring.AppConfig;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.UUID;

public class DriverTest2 {
    public interface ContainerTest1 {
        default String getP1() {
            return "P1-" + getP2();
        }

        default String getP2() {
            return "P2:" + UUID.randomUUID().toString();
        }
    }

    public interface ContainerTest2 extends ContainerTest1 {
        default String getHello() {
            return "H-" + getWorld();
        }

        default String getWorld() {
            return "W:" + UUID.randomUUID().toString();
        }
    }

    public static void main(String[] args) {
        AbstractApplicationContext context = AppConfig.initialize();
        ContainerEngine engine = new ContainerEngine();

        try {
            engine.buildFrom(ContainerTest1.class);

            long t1 = System.currentTimeMillis();
            ContainerMetadata<ContainerTest2> metadata = engine.buildFrom(ContainerTest2.class);
            long t2 = System.currentTimeMillis();
            System.out.format("build time: %d%n", t2 - t1);

            ContainerTest2 app = metadata.newInstance();
            app.getHello();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
