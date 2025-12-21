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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.manoa.web.FileUploader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Caster Class
 */
public class CasterClass {
    public static Object cast(String value, Class<?> type) throws Exception {
        if (value == null)
            return null;
        if (value.trim().length() == 0 || value.isEmpty()) {
            return null;
        }

        if (type == String.class)
            return value;

        if (type == int.class || type == Integer.class)
            return Integer.parseInt(value);
        if (type == double.class || type == Double.class)
            return Double.parseDouble(value);
        if (type == float.class || type == Float.class)
            return Float.parseFloat(value);
        if (type == long.class || type == Long.class)
            return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class)
            return Boolean.parseBoolean(value);

        if (type == LocalDate.class)
            return LocalDate.parse(value);
        if (type == LocalDateTime.class)
            return LocalDateTime.parse(value);

        if (type == java.sql.Date.class)
            return java.sql.Date.valueOf(value);
        if (type == java.util.Date.class)
            return java.sql.Date.valueOf(value);

        throw new Exception("Type non géré : " + type.getName());
    }

    public static boolean isComplexObject(Parameter parameter) {
        Class<?> type = parameter.getType();
        return isComplexObject(type);
    }

    public static boolean isComplexObject(Class<?> type) {
        if (type.isPrimitive())
            return false;

        if (type == String.class)
            return false;
        if (Number.class.isAssignableFrom(type))
            return false;
        if (type == Boolean.class || type == Character.class)
            return false;

        if (java.util.Date.class.isAssignableFrom(type))
            return false;
        if (java.time.temporal.Temporal.class.isAssignableFrom(type))
            return false;

        if (Collection.class.isAssignableFrom(type))
            return false;
        if (Map.class.isAssignableFrom(type))
            return false;

        Package pkg = type.getPackage();
        if (pkg != null && pkg.getName().startsWith("java."))
            return false;

        return true;
    }

    public static Object castObject(Parameter parameter, HttpServletRequest request) throws Exception {
        Class<?> type = parameter.getType();
        Object instanceParametre = type.getDeclaredConstructor().newInstance();
        String nameParameter = parameter.getName();
        return completedFieldParameter(instanceParametre, nameParameter, request);
    }

    public static int getArrayDimension(Class<?> type) {
        int dimension = 0;
        while (type.isArray()) {
            dimension++;
            type = type.getComponentType();
        }
        return dimension;
    }

    public static int[] parseIndexes(String index) {
        return java.util.Arrays.stream(
                        index.replace("[", "")
                                .split("]")
                )
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public static void setValueAtIndex(Object array, String index, Object value) {
        int[] indexes = parseIndexes(index);

        Object current = array;

        for (int i = 0; i < indexes.length - 1; i++) {
            current = Array.get(current, indexes[i]);
        }

        Array.set(current, indexes[indexes.length - 1], value);
    }

    public static String extractIndexes(String s, String textBefore) {
        String regex = Pattern.quote(textBefore) + "((\\[\\d+\\])+)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);

        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    public static Object completeArray(Object array, String indexString, String nameObject, HttpServletRequest request) throws Exception {
        Class<?> type = array.getClass().getComponentType();
        return walk(array, "", indexString, nameObject, request, type);

    }

    private static Object walk(Object array, String indexPath, String indexString, String nameObject, HttpServletRequest request, Class<?> type) throws Exception {
        System.out.println(indexPath+" : "+indexString);
        if (indexPath.compareToIgnoreCase(indexString) != 0) {
            if (array != null) {
                if (array.getClass().isArray()) {
                    int length = Array.getLength(array);
                    for (int i = 0; i < length; i++) {
                        Object element = Array.get(array, i);
                        type = array.getClass().getComponentType();
                        element = walk(element, indexPath + "[" + i + "]", indexString, nameObject, request, type);
                        Array.set(array, i, element);
                    }
                    return array;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            if (array == null) {
                array = type.getDeclaredConstructor().newInstance();
                return completedFieldParameter(array, nameObject + indexPath, request);
            }else{
                if (!array.getClass().isArray()) {
                    return completedFieldParameter(array, nameObject + indexPath, request);
                }
            }

            return null;
        }
    }

    public static Object completedFieldParameter(Object instance, String nameObject, HttpServletRequest request)
            throws Exception {
        System.out.println("=====>" +nameObject);
        Class<?> classInstance = instance.getClass();
        System.out.println("=====>"+classInstance.getSimpleName());
        Field[] fields = classInstance.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            String nameParameter = nameObject + "." + fieldName;
            System.out.println("=====>" + nameParameter );
            Class<?> fieldType = field.getType();
            String[] rawValues = request.getParameterValues(nameParameter);
            if (rawValues == null) {
                rawValues = new String[0];
            }
            // tableau
            if (field.getType().isArray()) {
                Class<?> componentType = fieldType.getComponentType();
                if (isComplexObject(componentType)) {
                    int numberDimension = getArrayDimension(field.getType());
                    Map<String, String[]> paramMap = request.getParameterMap();
                    List<String> keyStartWithNameParameter = new ArrayList<>();
                    for (String key : paramMap.keySet()) {
                        if (key.startsWith(nameParameter)) {
                            keyStartWithNameParameter.add(key);
                        }
                    }
                    int[] numberMaxTab = new int[numberDimension];
                    List<String> keyWithoutEnd = new ArrayList<>();
                    for (String key : keyStartWithNameParameter) {
                        int lastDot = key.lastIndexOf('.');
                        String base = (lastDot != -1) ? key.substring(0, lastDot) : key;
                        keyWithoutEnd.add(base);
                    }
                    List<String> indexString = new ArrayList<>();
                    for (String key : keyWithoutEnd) {
                        indexString.add(extractIndexes(key, nameParameter));
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[(\\d+)]").matcher(key);
                        int dim = 0;
                        while (matcher.find() && dim < numberDimension) {
                            int value = Integer.parseInt(matcher.group(1));
                            if (value > numberMaxTab[dim]) {
                                numberMaxTab[dim] = value + 1;
                            }
                            dim++;
                        }
                    }

                    Class<?> typeTab = fieldType.getComponentType();
                    for (int i = 1; i < numberDimension; i++) {
                        typeTab = typeTab.getComponentType();
                    }

                    Object arrayMultiDimension =  Array.newInstance(typeTab, numberMaxTab);
                    for (String index : indexString) {
                        arrayMultiDimension = completeArray(arrayMultiDimension, index, nameParameter, request);
                    }
                    field.set(instance, arrayMultiDimension);
                }else{
                    Object array = Array.newInstance(componentType, rawValues.length);
                    for (int i = 0; i < rawValues.length; i++) {
                        Object casted = cast(rawValues[i], componentType);
                        Array.set(array, i, casted);
                    }
                    field.set(instance, array);
                }
                continue;
            }

            // List , Vector , HashSet
            if (field.get(instance) == null) {
                if (Collection.class.isAssignableFrom(fieldType)) {
                    Class<?> genericType = String.class;
                    Type generic = field.getGenericType();
                    if (generic instanceof ParameterizedType pt) {
                        Type[] args = pt.getActualTypeArguments();
                        if (args != null && args.length == 1 && args[0] instanceof Class<?> gt) {
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
                        if (v.trim().length() > 0 || v.isEmpty() == false) {
                            collection.add(cast(v, genericType));
                        }
                    }

                    field.set(instance, collection);
                    continue;
                }
            }

            // tsotra
            if (field.get(instance) == null) {
                String valueString = request.getParameter(nameParameter);
                field.set(instance, cast(valueString, field.getType()));
            }

            // object
            if (field.get(instance) == null) {
                if (isComplexObject(field.getType())) {
                    if (field.getType() == FileUploader.class) {
                        boolean isMultipart =
                                request.getContentType() != null &&
                                        request.getContentType().toLowerCase().startsWith("multipart/");
                        if (isMultipart) {
                            Part part = request.getPart(nameParameter);
                            if (part != null) {
                                FileUploader jf = (FileUploader) field.getType().getDeclaredConstructor().newInstance();
                                if (jf == null){
                                    jf = new FileUploader();
                                    System.out.println("Null");
                                } else {
                                    System.out.println("Not Null");
                                }
                                jf.setPart(part);
                                field.set(instance, jf);
                                continue;
                            }
                        }
                    } else {

                        Object fieldObject = field.getType().getDeclaredConstructor().newInstance();
                        field.set(instance, completedFieldParameter(fieldObject, nameParameter, request));
                    }
                }
            }

        }
        return instance;
    }
}
