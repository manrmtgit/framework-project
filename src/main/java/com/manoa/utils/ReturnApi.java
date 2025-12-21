package com.manoa.utils;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLIntegrityConstraintViolationException;


import com.google.gson.Gson;
import com.manoa.format.RobustApi;
import com.manoa.format.SimpleApi;


public class ReturnApi {
    public static String getFormatSimple(Object result, Exception exception) throws Exception {
        SimpleApi SimpleApi = new SimpleApi();
        String status = "";
        int code = getHttpCodeFromException(exception);
        Object data = null;
        int count = 0;
        String message = "";
        if (exception != null) {
            System.out.println("Exception: " + exception.getMessage());
            message = exception.getMessage();
            data = exception.getMessage();
            status = "error";
            message = "It's error";
            count = 0;
            SimpleApi.setStatus("error");
        } else {
            status = "success";
            count = 1;
            message = "Success";
            if (result.getClass().getName().compareToIgnoreCase("com.manoa.utils.ModelView") == 0) {
                ModelView modelView = (ModelView) result;
                data = modelView.getData();
            } else
                data = result;
        }
        SimpleApi.setStatus(status);
        SimpleApi.setMessage(message);
        SimpleApi.setCode(code);
        SimpleApi.setData(data);
        SimpleApi.setCount(count);
        Gson gson = new Gson();
        return gson.toJson(SimpleApi);
    }


    public static String getFormatRobuste(Object result, Exception exception) throws Exception {
        RobustApi RobustApi = new RobustApi();
        String status = "";
        Object data = null;
        Object error = null;
        if (exception != null) {
            error = exception.getMessage();
            status = "error";
            data = null;
        } else {
            error = null;
            status = "success";
            if (result.getClass().getName().compareToIgnoreCase("com.manoa.utils.ModelView") == 0) {
                ModelView modelView = (ModelView) result;
                data = modelView.getData();
            } else
                data = result;
        }
        RobustApi.setData(data);
        RobustApi.setError(error);
        RobustApi.setStatus(status);
        Gson gson = new Gson();
        return gson.toJson(RobustApi);
    }

    public static String getFormatRest(Object result, Exception exception) throws Exception {
        Object data = null;
        if (exception != null) {
            data = exception.getMessage();
        } else {
            if (result.getClass().getName().compareToIgnoreCase("com.manoa.utils.ModelView") == 0) {
                ModelView modelView = (ModelView) result;
                data = modelView.getData();
            } else
                data = result;
        }
        Gson gson = new Gson();
        return gson.toJson(data);
    }

    public static int getHttpCodeFromException(Exception e) {

        if (e == null) return 200;

        Throwable cause = (e instanceof InvocationTargetException)
                ? ((InvocationTargetException) e).getCause()
                : e;

        if (cause instanceof NumberFormatException) return 400;
        if (cause instanceof IllegalArgumentException) return 400;
        if (cause instanceof java.time.format.DateTimeParseException) return 400;

        if (cause instanceof SecurityException) return 401;
        if (cause instanceof javax.naming.AuthenticationException) return 401;
        if (cause instanceof javax.security.sasl.AuthenticationException) return 401;


        if (cause instanceof AccessDeniedException) return 403;

        if (cause instanceof FileNotFoundException) return 404;

        if (cause instanceof SQLIntegrityConstraintViolationException) return 409;

        return 500;
    }
}
