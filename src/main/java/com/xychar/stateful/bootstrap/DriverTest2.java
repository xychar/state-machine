package com.xychar.stateful.bootstrap;

import com.xychar.stateful.container.Component;
import com.xychar.stateful.container.ContainerEngine;
import com.xychar.stateful.container.ContainerException;
import com.xychar.stateful.container.ContainerMetadata;
import com.xychar.stateful.engine.StepStateAccessor;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.mybatis.StepStateRow;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.StepStateTable;
import com.xychar.stateful.store.WorkflowStore;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public interface AppModule {
        final Pattern NAME_TAG = Pattern.compile("(\\\\$)|(\\$\\{\\s*(\\w\\S+).*?\\})");

        default String format(String template, Properties params) {
            StringBuffer sb = new StringBuffer();
            Matcher m = NAME_TAG.matcher(template);
            while (m.find()) {
                if (m.group(1) != null) {
                    m.appendReplacement(sb, "$");
                } else if (m.group(3) != null) {
                    String value = params.getProperty(m.group(3), "");
                    m.appendReplacement(sb, value);
                }
            }

            m.appendTail(sb);
            return sb.toString();
        }

        @Component
        default Properties config() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            try {
                Properties props = new Properties();
                props.load(loader.getResourceAsStream("database.properties"));
                return props;
            } catch (IOException e) {
                throw new ContainerException("Failed to load app properties");
            }
        }

        default String getProperty(String name) {
            String propertyValue = config().getProperty(name);
            if (propertyValue != null) {
                return format(propertyValue, System.getProperties());
            } else {
                return null;
            }
        }

        @Component
        default DataSource dataSource() {
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl(getProperty("jdbc.url"));
            return ds;
        }

        @Component
        default JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(dataSource());
        }

        @Component
        default NamedParameterJdbcTemplate template() {
            return new NamedParameterJdbcTemplate(dataSource());
        }

        @Component
        default StepStateAccessor stepStateStore() {
            return new StepStateStore(jdbcTemplate(), template());
        }

        @Component
        default WorkflowStore workflowStore() {
            return new WorkflowStore(jdbcTemplate(), template());
        }

        @Component
        default void startup() {
            stepStateStore();
            workflowStore();
        }
    }

    public static void main(String[] args) {
        AbstractApplicationContext context = AppConfig.initialize();
        ContainerEngine engine = new ContainerEngine();

        try {
            engine.buildFrom(ContainerTest1.class);

            long t1 = System.currentTimeMillis();
            ContainerMetadata<AppModule> metadata = engine.buildFrom(AppModule.class);
            long t2 = System.currentTimeMillis();
            System.out.format("build time: %d%n", t2 - t1);

            AppModule app = metadata.newInstance();
            app.startup();

            List<StepStateRow> rs = app.template().query(
                    "select * from t_step_state", Collections.emptyMap(),
                    StepStateTable.resultSetExtractor());
            System.out.println(rs.size());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
