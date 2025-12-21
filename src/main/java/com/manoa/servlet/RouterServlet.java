package com.manoa.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import com.manoa.annotations.Api;
import com.manoa.annotations.RequestParameter;
import com.manoa.utils.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@MultipartConfig
public class RouterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private RequestDispatcher dispatcher;

    // public static Map<String, Rooter> rooters;
    @Override
    public void init() throws ServletException {
        dispatcher = getServletContext().getNamedDispatcher("default");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {

            processRequest(request, response, "get");
        } catch (Exception e) {
            e.printStackTrace();
            new ServletException(e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response, "post");
        } catch (Exception e) {
            e.printStackTrace();
            new ServletException(e.getMessage());
        }
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response, String typeMethod)
            throws Exception {
        ServletContext context = request.getServletContext();
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        String relativePath = path.substring(req.getContextPath().length());
        if (fileExists(context, relativePath)) {
            dispatcher.forward(request, response);
        } else {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                // String url = request.getRequestURL().toString();
                Map<String, Rooter> rooters = null;
                if (typeMethod.compareToIgnoreCase("get") == 0) {
                    rooters = (Map<String, Rooter>) context.getAttribute("rootersGet");
                } else if (typeMethod.compareToIgnoreCase("post") == 0) {
                    rooters = (Map<String, Rooter>) context.getAttribute("rootersPost");
                }
                Rooter rooter = rooters.get(relativePath);

                if (rooter == null) {
                    boolean isMatch = false;
                    for (Entry<String, Rooter> root : rooters.entrySet()) {
                        String regex = root.getKey().replaceAll("\\{[^/]+\\}", "([^/]+)");
                        regex = "^" + regex + "$";
                        if (relativePath.matches(regex)) {
                            isMatch = true;
                            String pathParameter = (String) root.getKey();
                            Rooter rooterParameter = (Rooter) rooters.get(pathParameter);
                            execRoote(request, response, rooterParameter, out, relativePath, root.getKey());
                        } else {
                            String[] urlInterogation = relativePath.split("\\?");
                            if (urlInterogation.length > 1) {
                                if (urlInterogation[0].compareToIgnoreCase(root.getKey()) == 0) {
                                    rooter = rooters.get(urlInterogation[0]);
                                    if (rooter != null) {
                                        execRoote(request, response, rooter, out, relativePath, root.getKey());
                                    }
                                }
                            }
                        }
                    }
                    if (isMatch)
                        return;
                    if (!isMatch)
                        out.println("<h1> 404 Not Found</h1>");
                } else {
                    execRoote(request, response, rooter, out, relativePath, relativePath);
                }
            }
        }
    }

    private boolean fileExists(ServletContext context, String relativePath) {
        if (relativePath.compareToIgnoreCase("/") == 0)
            return false;

        try {
            URL resource = context.getResource(relativePath);
            return (resource != null);
        } catch (Exception e) {
            return false;
        }

    }

    private void execRoote(HttpServletRequest request, HttpServletResponse response, Rooter rooter, PrintWriter out,
                           String pathClient, String pathController)
            throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        String className = rooter.classe;
        String methodName = rooter.method;
        // Charger la classe dynamiquement
        Class<?> clazz = Class.forName(className);

        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                // mi executer methode
                Object instance = clazz.getDeclaredConstructor().newInstance();

                Parameter[] parameters = m.getParameters();
                Object result = null;
                Exception exceptionInvocation = null;

                if (parameters.length == 0) {
                    // méthode sans paramètre
                    try {
                        result = m.invoke(instance);
                    } catch (Exception e) {
                        exceptionInvocation = e;
                    }
                } else {
                    String[] keyParam = pathController.split("/");
                    String[] valueParam = pathClient.split("/");
                    HashMap<String, String> paramUrl = new HashMap<>();
                    int j = 0;
                    for (String parameter : keyParam) {
                        paramUrl.put(keyParam[j], valueParam[j]);
                        j++;
                    }
                    Object[] values = new Object[parameters.length];

                    for (int i = 0; i < parameters.length; i++) {
                        Class<?> type = parameters[i].getType();
                        if (Map.class.isAssignableFrom(type)) {
                            System.out.println("intyhy");
                            Type genericType = parameters[i].getParameterizedType();
                            ParameterizedType pt = (ParameterizedType) genericType;
                            Type keyType = pt.getActualTypeArguments()[0];
                            Type valType = pt.getActualTypeArguments()[1];

                            if (keyType == String.class && valType == Object.class) {

                                Map<String, Object> paramMap = new HashMap<>();

                                request.getParameterMap().forEach((k, v) -> {
                                    if (v != null) {
                                        if (v.length == 1) {
                                            paramMap.put(k, v[0]);
                                        } else {
                                            paramMap.put(k, v);
                                        }
                                    }
                                });

                                values[i] = paramMap;
                                continue;
                            } else if (keyType == String.class && valType.getTypeName().compareToIgnoreCase("java.util.List<byte[]>") == 0) {
                                Map<String, List<byte[]>> fichiers = new HashMap<>();
                                for (Part part : request.getParts()) {

                                    String inputName = part.getName();
                                    String fileName = part.getSubmittedFileName();

                                    if (fileName == null || fileName.isEmpty()) {
                                        continue;
                                    }

                                    byte[] data;
                                    try (InputStream is = part.getInputStream()) {
                                        data = is.readAllBytes();
                                    }

                                    fichiers
                                            .computeIfAbsent(inputName, k -> new List<byte[]>() {
                                                @Override
                                                public int size() {
                                                    return 0;
                                                }

                                                @Override
                                                public boolean isEmpty() {
                                                    return false;
                                                }

                                                @Override
                                                public boolean contains(Object o) {
                                                    return false;
                                                }

                                                @Override
                                                public Iterator<byte[]> iterator() {
                                                    return null;
                                                }

                                                @Override
                                                public Object[] toArray() {
                                                    return new Object[0];
                                                }

                                                @Override
                                                public <T> T[] toArray(T[] a) {
                                                    return null;
                                                }

                                                @Override
                                                public boolean add(byte[] bytes) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean remove(Object o) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean containsAll(Collection<?> c) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean addAll(Collection<? extends byte[]> c) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean addAll(int index, Collection<? extends byte[]> c) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean removeAll(Collection<?> c) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean retainAll(Collection<?> c) {
                                                    return false;
                                                }

                                                @Override
                                                public void clear() {

                                                }

                                                @Override
                                                public boolean equals(Object o) {
                                                    return false;
                                                }

                                                @Override
                                                public int hashCode() {
                                                    return 0;
                                                }

                                                @Override
                                                public byte[] get(int index) {
                                                    return new byte[0];
                                                }

                                                @Override
                                                public byte[] set(int index, byte[] element) {
                                                    return new byte[0];
                                                }

                                                @Override
                                                public void add(int index, byte[] element) {

                                                }

                                                @Override
                                                public byte[] remove(int index) {
                                                    return new byte[0];
                                                }

                                                @Override
                                                public int indexOf(Object o) {
                                                    return 0;
                                                }

                                                @Override
                                                public int lastIndexOf(Object o) {
                                                    return 0;
                                                }

                                                @Override
                                                public ListIterator<byte[]> listIterator() {
                                                    return null;
                                                }

                                                @Override
                                                public ListIterator<byte[]> listIterator(int index) {
                                                    return null;
                                                }

                                                @Override
                                                public List<byte[]> subList(int fromIndex, int toIndex) {
                                                    return List.of();
                                                }
                                            })
                                            .add(data);
                                }

                                values[i] = fichiers;
                                continue;
                            }

                        }
                        String paramName = parameters[i].getName();
                        String rawValue = request.getParameter(paramName);
                        if (rawValue == null) {
                            rawValue = paramUrl.get("{" + paramName + "}");
                        }
                        if (rawValue == null) {
                            if (parameters[i].isAnnotationPresent(RequestParameter.class)) {
                                RequestParameter RequestParameter = parameters[i].getAnnotation(RequestParameter.class);
                                rawValue = request.getParameter(RequestParameter.value());
                            }
                        }

                        // parameter type object

                        if (rawValue == null || rawValue.isEmpty() || rawValue.trim().length() == 0) {
                            if (type.isPrimitive()) {
                                if (type == int.class)
                                    values[i] = 0;
                                if (type == double.class)
                                    values[i] = 0.0;
                                if (type == boolean.class)
                                    values[i] = false;
                            } else {
                                values[i] = null;
                            }
                        } else {
                            values[i] = CasterClass.cast(rawValue, type);
                        }
                        if (values[i] == null) {
                            if (CasterClass.isComplexObject(parameters[i])) {
                                values[i] = CasterClass.castObject(parameters[i], request);
                            }
                        }
                    }

                    try {
                        result = m.invoke(instance, values);
                    } catch (Exception e) {
                        exceptionInvocation = e;
                    }
                }
                if (m.isAnnotationPresent(Api.class)) {
                    response.setContentType("application/json; charset=UTF-8");
                    Api typeAnnotationApi = m.getAnnotation(Api.class);
                    String resultJson = "";
                    if (typeAnnotationApi.format() == FormatApi.ROBUST) {
                        resultJson = ReturnApi.getFormatRobuste(result, exceptionInvocation);
                    } else if (typeAnnotationApi.format() == FormatApi.REST) {
                        resultJson = ReturnApi.getFormatRest(result, exceptionInvocation);
                        response.setStatus(ReturnApi.getHttpCodeFromException(exceptionInvocation));
                    } else {
                        resultJson = ReturnApi.getFormatSimple(result, exceptionInvocation);
                    }
                    response.getWriter().write(resultJson);

                } else {
                    if (result.getClass().getName().compareToIgnoreCase("java.lang.String") == 0) {
                        out.println(result);
                    } else if (result.getClass().getName().compareToIgnoreCase("com.manoa.utils.ModelView") == 0) {
                        ModelView modelView = (ModelView) result;

                        for (Map.Entry<String, Object> data : modelView.getData().entrySet()) {
                            request.setAttribute(data.getKey(), data.getValue());
                        }
                        String pathDispatch = modelView.getView();
                        if (!pathDispatch.startsWith("/")) {
                            pathDispatch = "/" + pathDispatch;
                        }
                        RequestDispatcher dispat = req.getRequestDispatcher(pathDispatch);
                        dispat.forward(request, response);
                    } else {
                        out.println("<h1> Erreur 500 </h1>");
                        out.println("<p> Type de retour de " + rooter.classe + " : " + rooter.method + " est invalide");
                    }
                }
            }
        }
    }
}