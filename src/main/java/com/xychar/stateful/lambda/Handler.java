package com.xychar.stateful.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Handler implements RequestHandler<StepInput, StepResult> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public StepResult handleRequest(StepInput event, Context context) {
        LambdaLogger logger = context.getLogger();

        return null;
    }

}
