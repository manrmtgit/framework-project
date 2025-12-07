package com.manoa.listener;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.manoa.annotations.Controller;
import com.manoa.annotations.GetUrl;
import com.manoa.annotations.PostUrl;
import com.manoa.utils.Rooter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;


@WebListener
public class ListenerClass implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("####### Application Started #######");

        ServletContext servletContext = sce.getServletContext();
        String appPath = servletContext.getRealPath("/WEB-INF/classes");

        try {
            List<Class<?>> controllers = findClassesWithAnnotation(new File(appPath), "", Controller.class);
            List<Map<String, Rooter>> rootersMap = getAllRootes(controllers);
            // RooterServlet.rooters = rootersMap;
            servletContext.setAttribute("rootersGet", rootersMap.get(0));
            servletContext.setAttribute("rootersPost", rootersMap.get(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("####### Application arretee  #######");
    }

    private List<Class<?>> findClassesWithAnnotation(File folder, String packageName, Class<?> annotationClass)
            throws Exception {
        List<Class<?>> result = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) return result;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                result.addAll(findClassesWithAnnotation(file, subPackage, annotationClass));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> cls = Class.forName(className);
                    if (cls.isAnnotationPresent(annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
                        result.add(cls);
                    }
                } catch (Throwable _) {
                }
            }
        }
        return result;
    }

    private List<Map<String, Rooter>> getAllRootes(List<Class<?>> controllers) {
        List<Map<String, Rooter>> routes = new ArrayList<>();
        Map<String, Rooter> routesPost = new HashMap<>();
        Map<String, Rooter> routesGet = new HashMap<>();
        for (Class<?> controllerClass : controllers) {
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostUrl.class)) {
                    PostUrl urlAnnotation = method.getAnnotation(PostUrl.class);
                    String urlValue = urlAnnotation.value();
                    Rooter info = new Rooter();
                    info.classe = controllerClass.getName();
                    info.method = method.getName();

                    routesPost.put(urlValue, info);
                }
                if (method.isAnnotationPresent(GetUrl.class)) {
                    GetUrl urlAnnotation = method.getAnnotation(GetUrl.class);
                    String urlValue = urlAnnotation.value();
                    Rooter info = new Rooter();
                    info.classe = controllerClass.getName();
                    info.method = method.getName();

                    routesGet.put(urlValue, info);
                }
            }
        }

        routes.add(routesGet);
        routes.add(routesPost);

        return routes;
    }
}
