package com.xychar.stateful.bootstrap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class DriverHandler implements RequestStreamHandler {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input, UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, UTF_8));

    }
}
