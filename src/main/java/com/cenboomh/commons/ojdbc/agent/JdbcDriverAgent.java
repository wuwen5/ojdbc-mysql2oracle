package com.cenboomh.commons.ojdbc.agent;

import com.cenboomh.commons.ojdbc.driver.MysqlToOracleDriver;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Mysql驱动替换
 * 在驱动类没有提供配置参数的情况下使用agent替换
 *
 * @author wuwen
 */
public final class JdbcDriverAgent {

    private static final byte[] EMPTY_BYTE_ARRAY = {};

    private static final String DEFAULT_MYSQL_JDBC_DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_DRIVER_CLASS_NAME_KEY = "driverClassName";

    private static final String MTO_DRIVER_NAME = MysqlToOracleDriver.class.getName();

    public static void premain(String agentArgs, Instrumentation inst) {

        final ClassFileTransformer transformer = (loader, classFile, classBeingRedefined, protectionDomain, classfileBuffer) -> {

            //Lambda
            if (classFile == null) {
                return EMPTY_BYTE_ARRAY;
            }

            final String className = toClassName(classFile);

            final String mysqlDriverClassName = splitCommaColonStringToKV(agentArgs).getOrDefault(MYSQL_DRIVER_CLASS_NAME_KEY, DEFAULT_MYSQL_JDBC_DRIVER_NAME);

            if (mysqlDriverClassName.equals(className)) {

                ClassLoader classLoader = loader == null ? ClassLoader.getSystemClassLoader() : loader;

                final ClassPool classPool = new ClassPool(true);
                if (classLoader == null) {
                    classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
                } else {
                    classPool.appendClassPath(new LoaderClassPath(classLoader));
                }

                try {
                    CtClass ctClass = classPool.get(MTO_DRIVER_NAME);
                    ctClass.setName(mysqlDriverClassName);
                    return ctClass.toBytecode();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            return EMPTY_BYTE_ARRAY;
        };

        inst.addTransformer(transformer, true);
    }

    private static String toClassName(final String classFile) {
        return classFile.replace('/', '.');
    }

    /**
     * Split to {@code json} like String({@code "k1:v1,k2:v2"}) to KV map({@code "k1"->"v1", "k2"->"v2"}).
     */
    static Map<String, String> splitCommaColonStringToKV(String commaColonString) {
        Map<String, String> ret = new HashMap<>();
        if (commaColonString == null || commaColonString.trim().isEmpty()) {
            return ret;
        }

        final String[] splitKvArray = commaColonString.trim().split("\\s*,\\s*");

        for (String kvString : splitKvArray) {
            final String[] kv = kvString.trim().split("\\s*:\\s*");
            if (kv.length == 0) {
                continue;
            }

            if (kv.length == 1) {
                ret.put(kv[0], "");
            } else {
                ret.put(kv[0], kv[1]);
            }
        }

        return ret;
    }
}
