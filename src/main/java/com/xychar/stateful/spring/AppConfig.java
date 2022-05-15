package com.xychar.stateful.spring;

import com.xychar.stateful.mybatis.StepStateMapper;
import com.xychar.stateful.mybatis.WorkflowMapper;
import com.xychar.stateful.scheduler.WorkflowDriver;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.WorkflowStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:database.properties")
public class AppConfig {
    private final Environment env;

    public AppConfig(@Autowired Environment environment) {
        this.env = environment;
    }

    public static AbstractApplicationContext initialize() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class, AppConfig.class);

        context.refresh();
        context.start();
        return context;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(env.getProperty("jdbc.driverClassName"));
        config.setJdbcUrl(env.getProperty("jdbc.url"));

        return new HikariDataSource(config);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate template(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, ApplicationContext context) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setMapperLocations(context.getResources("classpath:/mapper/*.sqlxml"));
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean
    public MapperFactoryBean<StepStateMapper> stepStateMapper(SqlSessionFactory sqlSessionFactory) throws Exception {
        MapperFactoryBean<StepStateMapper> factoryBean = new MapperFactoryBean<>(StepStateMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }

    @Bean
    public MapperFactoryBean<WorkflowMapper> workflowMapper(SqlSessionFactory sqlSessionFactory) throws Exception {
        MapperFactoryBean<WorkflowMapper> factoryBean = new MapperFactoryBean<>(WorkflowMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }

    @Bean
    public StepStateStore stepStateStore(StepStateMapper mapper) {
        return new StepStateStore(mapper);
    }

    @Bean
    public WorkflowStore workflowStore(WorkflowMapper mapper) {
        return new WorkflowStore(mapper);
    }

    @Bean
    public WorkflowDriver workflowDriver(StepStateStore stepStateStore, WorkflowStore workflowStore) {
        return new WorkflowDriver(stepStateStore, workflowStore);
    }
}
