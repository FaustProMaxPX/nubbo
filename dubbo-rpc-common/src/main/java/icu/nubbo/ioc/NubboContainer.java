package icu.nubbo.ioc;

import icu.nubbo.ioc.annotation.NubboAutowired;
import icu.nubbo.ioc.annotation.NubboBean;
import icu.nubbo.ioc.annotation.NubboComponent;
import icu.nubbo.ioc.annotation.NubboConfiguration;
import icu.nubbo.ioc.exception.CircularDependence;
import icu.nubbo.ioc.exception.LackDependencyException;
import icu.nubbo.utils.GraphUtil;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Nubbo IOC容器，饿汉单例
public class NubboContainer {

    private static final NubboContainer container = new NubboContainer();

    // 类型名与实例的映射
    private Map<String, Object> beans = new HashMap<>();

    // 要扫描的包路径
    private String[] packages;

    public static NubboContainer getContainer() {
        return container;
    }

    public <T> T getBean(Class<T> clazz) {
        return (T) beans.get(clazz.getName());
    }

    public Object getBean(String className) {
        return beans.get(className);
    }

    // 容器初始化加载
    private NubboContainer() {
        // 默认扫描整个项目
        // TODO: 支持自定义扫描范围
        packages = new String[]{""};
        Set<Class<?>> classes = new HashSet<>();
        for (String pkg : packages) {
            Set<Class<?>> componentClasses = getAnnotationClasses(pkg, NubboComponent.class);
            Set<Class<?>> configClasses = getAnnotationClasses(pkg, NubboConfiguration.class);
            Set<Class<?>> beanClasses = getBeanClassesFromConfigs(configClasses);
            classes.addAll(componentClasses);
            classes.addAll(configClasses);
            classes.addAll(beanClasses);
        }
        // 扫描bean的依赖关系
        checkDependency(classes);
        // 初始化bean
        try {
            initializeBeans(classes);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeBeans(Set<Class<?>> classes) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        for (Class<?> clazz : classes) {
            if (beans.containsKey(clazz.getName())) {
                continue;
            }
            initializeBean(clazz);
        }
    }

    private void initializeBean(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // 通过无参构造器建立实体
        Object instance = clazz.getDeclaredConstructor().newInstance();
        // 寻找该类中需要注入的字段
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(NubboAutowired.class) != null) {
                // 如果对应实体已经被注入到容器中，直接进行赋值
                // 否则先创建该实例
                Object fieldValue = beans.get(field.getType().getName());
                if (fieldValue == null) {
                    initializeBean(field.getType());
                    fieldValue = beans.get(field.getType().getName());
                    if (fieldValue == null) {
                        throw new LackDependencyException(clazz.getName() + "缺少依赖" + field.getType().getName());
                    }
                }
                field.set(instance, fieldValue);
            }
        }
        beans.put(clazz.getName(), instance);
    }

    private void checkDependency(Set<Class<?>> classes) {
        Map<Class<?>, List<Class<?>>> graph = new HashMap<>();
        classes.forEach(clazz -> graph.put(clazz, getClassDependencies(clazz)));
        // 检查是否有环
        if (GraphUtil.isCircle(graph)) {
            throw new CircularDependence("初始化失败，存在循环依赖");
        }
    }

    private List<Class<?>> getClassDependencies(Class<?> clazz) {
        List<Class<?>> dependencies = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            NubboAutowired autowired = field.getAnnotation(NubboAutowired.class);
            if (autowired != null) {
                dependencies.add(field.getType());
            }
        }
        return dependencies;
    }

    // 获取配置类中的Bean
    private Set<Class<?>> getBeanClassesFromConfigs(Set<Class<?>> configs) {
        Set<Class<?>> beans = new HashSet<>();
        for (Class<?> config : configs) {
            for (Method method : config.getDeclaredMethods()) {
                NubboBean annotation = method.getAnnotation(NubboBean.class);
                if (annotation != null) {
                    beans.add(method.getReturnType());
                }
            }
        }
        return beans;
    }

    private Set<Class<?>> getClasses(String pkgName,
                                           boolean recursive) {
        Set<Class<?>> classes = new HashSet<>();
        String pkgDirName = pkgName.replaceAll("\\.", File.separator);
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(pkgDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
                    addClasses(classes, filePath, pkgName, recursive);
                }
                // TODO: 支持装配jar包中被指定注解标记的类
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return classes;
    }

    private void addClasses(Set<Class<?>> classes, String filePath, String pkgName, boolean recursive) throws ClassNotFoundException {
        File[] files =
                new File(filePath).listFiles(file -> (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory());
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (file.isFile()) {
                String className = name.substring(0, name.lastIndexOf("."));
                if (!pkgName.isEmpty()) {
                    className = pkgName + "." + className;
                }
                doAddClass(classes, className);
            } else if (file.isDirectory() && recursive) {
                String innerPkgName;
                if (!pkgName.isEmpty()) {
                    innerPkgName = pkgName + "." + name;
                } else {
                    innerPkgName = name;
                }
                String innerFilePath = filePath + File.separator + name;
                addClasses(classes, innerFilePath, innerPkgName, true);
            }
        }
    }

    private void doAddClass(Set<Class<?>> classes, String className) throws ClassNotFoundException {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return super.loadClass(name);
            }
        };
        classes.add(classLoader.loadClass(className));
    }

    private <A extends Annotation> Set<Class<?>> getAnnotationClasses(String pkgName, Class<A> annotationClass) {
        Set<Class<?>> ret = new HashSet<>();
        Set<Class<?>> classes = getClasses(pkgName, true);
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotation(annotationClass) != null) {
                ret.add(clazz);
            }
        }
        return ret;
    }
}
