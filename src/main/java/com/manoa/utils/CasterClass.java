package com.manoa.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;

public class CasterClass {
    public static Object cast(String value, Class<?> type) throws Exception {
        if (value == null) return null;
        if (value.trim().isEmpty() || value.isEmpty()) {
            return null;
        }

        if (type == String.class) return value;

        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);

        if (type == LocalDate.class) return LocalDate.parse(value);
        if (type == LocalDateTime.class) return LocalDateTime.parse(value);

        if (type == java.sql.Date.class) return java.sql.Date.valueOf(value);
        if (type == java.util.Date.class) return java.sql.Date.valueOf(value);

        throw new Exception("Type non géré : " + type.getName());
    }

    public static boolean isComplexObject(Parameter parameter) {
        Class<?> type = parameter.getType();
        return isComplexObject(type);
    }

    public static boolean isComplexObject(Class<?> type) {
        if (type.isPrimitive()) return false;

        if (type == String.class) return false;
        if (Number.class.isAssignableFrom(type)) return false;
        if (type == Boolean.class || type == Character.class) return false;

        if (java.util.Date.class.isAssignableFrom(type)) return false;
        if (java.time.temporal.Temporal.class.isAssignableFrom(type)) return false;

        if (Collection.class.isAssignableFrom(type)) return false;
        if (Map.class.isAssignableFrom(type)) return false;

        Package pkg = type.getPackage();
        return pkg == null || !pkg.getName().startsWith("java.");
    }

    public static Object castObject(Parameter parameter, HttpServletRequest request) throws Exception {
        Class<?> type = parameter.getType();
        Object instanceParametre = type.getDeclaredConstructor().newInstance();
        String nameParameter = parameter.getName();
        return completedFieldParameter(instanceParametre, nameParameter, request);
    }

    public static Object completedFieldParameter(Object instance, String nameObject, HttpServletRequest request) throws Exception {
        Class<?> classInstance = instance.getClass();
        Field[] fields = classInstance.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            String nameParameter = nameObject + "." + fieldName;
            Class<?> fieldType = field.getType();
            String[] rawValues = request.getParameterValues(nameParameter);
            if (rawValues == null) {
                rawValues = new String[0];
            }
            // tableau
            if (field.getType().isArray()) {
                Class<?> componentType = fieldType.getComponentType();
                Object array = Array.newInstance(componentType, rawValues.length);
                for (int i = 0; i < rawValues.length; i++) {
                    Object casted = cast(rawValues[i], componentType);
                    Array.set(array, i, casted);
                }
                field.set(instance, array);
                continue;
            }

            // List , Vector , HashSet
            if (field.get(instance) == null) {
                if (Collection.class.isAssignableFrom(fieldType)) {
                    System.out.println(nameParameter + " isany " + rawValues.length);
                    Class<?> genericType = String.class;
                    Type generic = field.getGenericType();
                    if (generic instanceof ParameterizedType pt) {
                        Type[] args = pt.getActualTypeArguments();
                        if (args.length == 1 && args[0] instanceof Class<?> gt) {
                            genericType = gt;
                        }
                    }

                    Collection<Object> collection;

                    if (fieldType == List.class || fieldType == ArrayList.class) {
                        collection = new ArrayList<>();
                    } else if (fieldType == Vector.class) {
                        collection = new Vector<>();
                    } else if (fieldType == Set.class || fieldType == HashSet.class) {
                        collection = new HashSet<>();
                    } else {
                        collection = (Collection<Object>) fieldType.getDeclaredConstructor().newInstance();
                    }

                    for (String v : rawValues) {
                        if (!v.isEmpty()) {
                            collection.add(cast(v, genericType));
                        }
                    }

                    field.set(instance, collection);
                    continue;

                }
            }

            // simple
            if (field.get(instance) == null) {
                String valueString = request.getParameter(nameParameter);
                field.set(instance, cast(valueString, field.getType()));
            }

            // object
            if (field.get(instance) == null) {
                if (isComplexObject(field.getType())) {
                    Object fieldObject = field.getType().getDeclaredConstructor().newInstance();
                    field.set(instance, completedFieldParameter(fieldObject, nameParameter, request));
                }
            }
        }
        return instance;
    }
}
