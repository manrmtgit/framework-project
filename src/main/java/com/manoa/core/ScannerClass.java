package com.manoa.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ScannerClass : scan annotated class in all packages
 */
public class ScannerClass {
    public static List<Class<?>> scanClass(String packageName)
            throws URISyntaxException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace(".", "/");
        URL url = Thread.currentThread().getContextClassLoader().getResource(packagePath);

        if (url == null) {
            throw new RuntimeException("Package not found: " + packagePath);
        }

        File file = new File(url.toURI());
        for (File fileEntry : Objects.requireNonNull(file.listFiles())) {
            if (fileEntry.getName().endsWith(".class")) {
                String className = packageName + '.' + fileEntry.getName().replace(".class", "");
                Class<?> anotherClass = Class.forName(className);
                classes.add(anotherClass);
            }
        }

        return classes;
    }


    public static void scanPackage(String packageName)
            throws
            URISyntaxException,
            ClassNotFoundException,
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException {
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> controllerAnnotation =
                (Class<? extends Annotation>) Class.forName("com.manoa.annotations.Controller");
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> annotationUrl =
                (Class<? extends Annotation>) Class.forName("com.manoa.annotations.Url");

        List<Class<?>> classes = scanClass(packageName);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(controllerAnnotation)) {
                System.out.println("Controller found : " + clazz.getName());
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(annotationUrl)) {
                        Annotation annotation = method.getAnnotation(annotationUrl);
                        String urlValue = (String) annotation.getClass().getMethod("value").invoke(annotation);
                        System.out.println("   Method: " + method.getName() + "  URL: " + urlValue);
                    }
                }
            }
        }
    }
}
