package com.cenboomh.commons.ojdbc;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.apiguardian.api.API.Status.STABLE;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfDbAvailableCondition.class)
@API(status = STABLE, since = "5.1")
public @interface EnabledIfDbAvailable {

    String ip() default "localhost";

    int port() default 1521;
}
