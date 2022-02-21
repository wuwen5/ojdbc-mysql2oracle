package com.cenboomh.commons.ojdbc;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class EnabledIfDbAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = ConditionEvaluationResult.enabled("");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<EnabledIfDbAvailable> optional = findAnnotation(context.getElement(),
                EnabledIfDbAvailable.class);

        if (!optional.isPresent()) {
            return ENABLED_BY_DEFAULT;
        }

        EnabledIfDbAvailable annotation = optional.get();

        return dbAvailable(annotation.ip(), annotation.port()) ?
                ConditionEvaluationResult.enabled("") : ConditionEvaluationResult.disabled("数据库环境不可用");
    }

    public static boolean dbAvailable(String ip, int port) {
        try (Socket ignored = new Socket(ip, port)) {
            return true;
        } catch (IOException e) {
            // continue
        }
        return false;
    }
}
