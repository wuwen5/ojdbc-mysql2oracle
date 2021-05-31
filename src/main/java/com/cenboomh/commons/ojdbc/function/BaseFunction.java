package com.cenboomh.commons.ojdbc.function;

import net.sf.jsqlparser.expression.Function;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wuwen
 */
public abstract class BaseFunction implements java.util.function.Function<Function, Boolean> {

    private static final Map<String, java.util.function.Function<Function, Boolean>> FUNMAP = new HashMap<>();

    BaseFunction(String name) {
        FUNMAP.put(name.toUpperCase(), this);
    }

    public static boolean process(Function function) {
        return FUNMAP.getOrDefault(function.getName().toUpperCase(), fun -> false).apply(function);
    }


}
