package com.wesleyelliott.kubwa;

import android.content.Context;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.wesleyelliott.kubwa.rule.CheckedRule;
import com.wesleyelliott.kubwa.rule.ConfirmEmailRule;
import com.wesleyelliott.kubwa.rule.EmailRule;
import com.wesleyelliott.kubwa.rule.PasswordRule;
import com.wesleyelliott.kubwa.rule.RegexRule;
import com.wesleyelliott.kubwa.rule.Rule;

import java.util.List;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by wesley on 2016/07/28.
 */

public class CodeGenerator {

    public static TypeSpec generateClass(AnnotatedClass annotatedClass) throws KubwaException {
        String className = annotatedClass.annotatedClassName + "Validator";
        TypeSpec.Builder builder =  classBuilder(className)
                .addField(Context.class, "context")
                .addMethod(makeConstructor(annotatedClass.fieldRules))
                .addModifiers(PUBLIC, FINAL);


        for (FieldRule fieldRule : annotatedClass.fieldRules) {
            builder.addField(makeValidatorField(fieldRule));
            builder.addMethod(makeValidatorMethod(fieldRule));
            builder.addMethod(makeGetErrorMethod(fieldRule));
            builder.addMethod(makeSetErrorMethod(fieldRule));
        }

        builder.addMethod(makeIsValidMethod(annotatedClass.fieldRules));
        builder.addMethod(makeValidateAllMethod(annotatedClass.fieldRules));

        return builder.build();
    }

    private static FieldSpec makeValidatorField(FieldRule fieldRule) {
        return FieldSpec.builder(Validation.class, fieldRule.getFieldName())
                .addModifiers(PRIVATE)
                .build();
    }

    private static MethodSpec makeConstructor(List<FieldRule> fieldRuleList) throws KubwaException {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder();
        builder.addModifiers(PUBLIC)
                .addParameter(Context.class, "context")
                .addStatement("this.$N = $N", "context", "context");

        for (FieldRule fieldRule : fieldRuleList) {
            Class<? extends Rule> fieldRuleType = fieldRule.fieldRuleType;
            if (Utils.isRuleType(fieldRuleType, PasswordRule.class)) {
                builder.addStatement(fieldRule.getFieldName() + " = new $T(context, $L, new $T($T.$L))", Validation.class, fieldRule.fieldErrorResource, fieldRule.fieldRuleType, fieldRule.passwordScheme.getClass(), fieldRule.passwordScheme);
            } else if (Utils.isRuleType(fieldRuleType, RegexRule.class)) {
                builder.addStatement(fieldRule.getFieldName() + " = new $T(context, $L, new $T($S))", Validation.class, fieldRule.fieldErrorResource, fieldRule.fieldRuleType, fieldRule.regex);
            } else if (Utils.isRuleType(fieldRuleType, CheckedRule.class)) {
                builder.addStatement(fieldRule.getFieldName() + " = new $T(context, $L, new $T($L))", Validation.class, fieldRule.fieldErrorResource, fieldRule.fieldRuleType, fieldRule.checkedValue);
            } else if (Utils.isRuleType(fieldRuleType, ConfirmEmailRule.class)) {

                FieldRule emailFieldRule = Utils.getRule(fieldRuleList, EmailRule.class);
                if (emailFieldRule == null) {
                    throw new KubwaException("ConfirmEmailRule requires an EmailRule present!");
                }

                builder.addStatement(fieldRule.getFieldName() + " = new $T(context, $L, new $T())", Validation.class, fieldRule.fieldErrorResource, fieldRule.fieldRuleType);
            } else {
                builder.addStatement(fieldRule.getFieldName() + " = new $T(context, $L, new $T())", Validation.class, fieldRule.fieldErrorResource, fieldRule.fieldRuleType);
            }
        }

        return builder.build();
    }

    private static MethodSpec.Builder makeValidatorStatement(MethodSpec.Builder builder, FieldRule fieldRule) {
        if (Utils.isRuleType(fieldRule.fieldRuleType, ConfirmEmailRule.class)) {
            builder.addParameter(fieldRule.fieldRule.getType(), fieldRule.getValueName() + "1")
                    .addParameter(fieldRule.fieldRule.getType(), fieldRule.getValueName() + "2")
                    .addStatement("$L.validate($L, $L)", fieldRule.getFieldName(), fieldRule.getValueName() + "1", fieldRule.getValueName() + "2");
        } else {
            builder.addParameter(fieldRule.fieldRule.getType(), fieldRule.getValueName())
                    .addStatement("$L.validate($L)", fieldRule.getFieldName(), fieldRule.getValueName());
        }
        return builder;
    }

    private static MethodSpec makeValidatorMethod(FieldRule fieldRule) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldRule.getMethodName())
                .addModifiers(PUBLIC);
        makeValidatorStatement(builder, fieldRule);
        return builder.build();
    }

    private static MethodSpec makeGetErrorMethod(FieldRule fieldRule) {
        return MethodSpec.methodBuilder(fieldRule.getErrorMessageMethodName())
                .addModifiers(PUBLIC)
                .addStatement("return $L.getMessage()", fieldRule.getFieldName())
                .returns(String.class)
                .build();
    }

    private static MethodSpec makeSetErrorMethod(FieldRule fieldRule) {
        return MethodSpec.methodBuilder(fieldRule.setErrorMessageMethodName())
                .addModifiers(PUBLIC)
                .addParameter(String.class, fieldRule.getValueName())
                .addStatement("$L.setMessage($L)", fieldRule.getFieldName(), fieldRule.getValueName())
                .build();
    }

    private static MethodSpec makeIsValidMethod(List<FieldRule> fieldRuleList) {
        MethodSpec.Builder isValidMethodSpec = MethodSpec.methodBuilder("isValid")
                .addModifiers(PUBLIC)
                .returns(TypeName.BOOLEAN);

        StringBuilder builder = new StringBuilder();
        builder.append("true");
        for (FieldRule fieldRule : fieldRuleList) {
            builder.append(" && ");
            builder.append(fieldRule.getErrorMessageMethodName());
            builder.append("()");
            builder.append(" == null ");
        }

        isValidMethodSpec.addStatement("return $L", builder.toString());

        return isValidMethodSpec.build();
    }

    private static MethodSpec makeValidateAllMethod(List<FieldRule> fieldRuleList) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("validateAll")
                .addModifiers(PUBLIC);

        for (FieldRule fieldRule : fieldRuleList) {
            if (Utils.isRuleType(fieldRule.fieldRuleType, ConfirmEmailRule.class)) {
                FieldRule emailFieldRule = Utils.getRule(fieldRuleList, EmailRule.class);
                builder.addParameter(fieldRule.fieldRule.getType(), fieldRule.getValueName());
                builder.addStatement("$L.validate($L, $L);", fieldRule.getFieldName(), fieldRule.getValueName(), emailFieldRule.getValueName());
            } else {
                builder.addParameter(fieldRule.fieldRule.getType(), fieldRule.getValueName());
                builder.addStatement("$L.validate($L);", fieldRule.getFieldName(), fieldRule.getValueName());
            }
        }

        return builder.build();
    }
}
