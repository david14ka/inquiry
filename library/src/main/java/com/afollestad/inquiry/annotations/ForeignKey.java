package com.afollestad.inquiry.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Aidan Follestad (afollestad)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ForeignKey {

    String tableName();

    String foreignColumnName();

    String inverseFieldName() default "";
}