package com.xychar.stateful.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class Exceptions {
    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
