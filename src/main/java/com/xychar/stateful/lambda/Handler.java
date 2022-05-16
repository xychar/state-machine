package com.xychar.stateful.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.xychar.stateful.mybatis.StepStateMapper;
import com.xychar.stateful.spring.AppConfig;
import org.springframework.context.support.AbstractApplicationContext;

public class Handler implements RequestHandler<StepInput, StepResult> {
    private static final AbstractApplicationContext springContext = AppConfig.initialize();

    @Override
    public StepResult handleRequest(StepInput event, Context context) {
        LambdaLogger logger = context.getLogger();

        StepStateMapper mp = springContext.getBean(StepStateMapper.class);

        StepResult result = new StepResult();
        result.configData = mp.getClass().getName();
        return result;
    }

}
