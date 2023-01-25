package icu.nubbo.ioc;

import icu.nubbo.ioc.annotation.*;
import icu.nubbo.ioc.common.BeanInitInfo;
import icu.nubbo.ioc.common.MethodInitInfo;
import icu.nubbo.ioc.common.enums.InitWay;
import icu.nubbo.ioc.exception.CircularDependence;
import icu.nubbo.ioc.exception.DuplicateClassesException;
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
import java.util.stream.Collectors;

// Nubbo IOC容器，懒汉单例
public class NubboContainer {

    private static NubboContainer container;

    // 类型名与实例的映射
    private Map<String, Object> beans = new HashMap<>();

    private Map<Class<?>, BeanInitInfo> beanInitInfos = new HashMap<>();

    // 要扫描的包路径
    private String[] packages = new String[]{""};

    public static NubboContainer getContainer() {
        return container;
    }

    // 同步锁保持线程安全
    public synchronized static void run(Class<?> runClass) {
        String[] basePackages = getBasePackages(runClass);
        container = new NubboContainer(basePackages);
    }

    // 获取要扫描的包
    // 默认扫描整个项目
    private static String[] getBasePackages(Class<?> runClass) {
        NubboComponentScan annotation = runClass.getAnnotation(NubboComponentScan.class);
        if (annotation != null) {
            return annotation.basePackages();
        }
        return new String[]{""};
    }

    public <T> T getBean(Class<T> clazz) {
        return (T) beans.get(clazz.getName());
    }

    public Object getBean(String className) {
        return beans.get(className);
    }

    // 容器初始化加载
    private NubboContainer(String[] pkgs) {
        this.packages = pkgs;
        for (String pkg : packages) {
            Set<Class<?>> componentClasses = getAnnotationClasses(pkg, NubboComponent.class);
            Set<Class<?>> configClasses = getAnnotationClasses(pkg, NubboConfiguration.class);
            // 检测是否有同名的类
            checkSameNameClasses(componentClasses, configClasses);
            componentClasses.addAll(configClasses);
            // 通过构造器初始化的Bean
            Map<Class<?>, BeanInitInfo> constructorInits = componentClasses.stream()
                    .map(c -> new BeanInitInfo(c, InitWay.CONSTRUCTOR))
                    .collect(Collectors.toMap(BeanInitInfo::getClazz, BeanInitInfo -> BeanInitInfo));
            // 从配置类中获取方法Bean
            Map<Class<?>, BeanInitInfo> methodInits = getBeanInitInfoFromConfigs(configClasses);
            checkSameNameClasses(componentClasses, methodInits.keySet());
            beanInitInfos.putAll(constructorInits);
            beanInitInfos.putAll(methodInits);
        }
        // 扫描bean的依赖关系
        checkDependency();
        // 初始化bean
        try {
            initializeBeans();
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // 初始化完成，清理不必要的数据
        beanInitInfos = null;
    }

    private void checkSameNameClasses(Set<Class<?>> set1, Set<Class<?>> set2) {
        int size = set1.size();
        // 如果做差集之后出现size变化，就代表有同名的类
        set1.removeAll(set2);
        if (set1.size() != size) {
            throw new DuplicateClassesException("本容器所有实体均为单例，不支持创建多个同类型对象");
        }
    }

    private Map<Class<?>, BeanInitInfo> getBeanInitInfoFromConfigs(Set<Class<?>> configs) {
        Map<Class<?>, BeanInitInfo> beans = new HashMap<>();
        for (Class<?> config : configs) {
            for (Method method : config.getDeclaredMethods()) {
                NubboBean annotation = method.getAnnotation(NubboBean.class);
                if (annotation != null) {
                    BeanInitInfo ex = beans.putIfAbsent(method.getReturnType(),
                            new BeanInitInfo(method.getReturnType(), InitWay.METHOD,
                                    new MethodInitInfo(config, method, method.getParameterTypes())));
                    if (ex != null) {
                        throw new DuplicateClassesException("容器中禁止出现同名类");
                    }
                }
            }
        }
        return beans;
    }

    private void initializeBeans() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        for (Class<?> clazz : beanInitInfos.keySet()) {
            // 如果已经完成初始化，就跳过
            if (beans.containsKey(clazz.getName())) {
                continue;
            }
            initializeBean(beanInitInfos.get(clazz));
        }
    }

    private void initializeBean(BeanInitInfo initInfo) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (initInfo == null || beanInitInfos == null || beanInitInfos.size() == 0) {
            throw new IllegalArgumentException("初始化bean参数不足");
        }
        Class<?> clazz = initInfo.getClazz();
        switch (initInfo.getWay()) {
            case CONSTRUCTOR -> {
                // 通过无参构造器建立实体
                Object instance = clazz.getDeclaredConstructor().newInstance();
                // 寻找该类中需要注入的字段
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getAnnotation(NubboAutowired.class) != null) {
                        // 如果对应实体已经被注入到容器中，直接进行赋值
                        // 否则先创建该实例
                        Object fieldValue = getOrCreateBean(field.getType());
                        field.set(instance, fieldValue);
                    }
                }
                beans.put(clazz.getName(), instance);
            }
            case METHOD -> {
                MethodInitInfo methodInitInfo = initInfo.getMethodInitInfo();
                Method method = methodInitInfo.getMethod();
                Object invoker = getOrCreateBean(methodInitInfo.getInvoker());
                Class<?>[] parameterTypes = methodInitInfo.getParameterTypes();
                Object[] params = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    params[i] = getOrCreateBean(parameterTypes[i]);
                }
                Object bean = method.invoke(invoker, params);
                beans.put(clazz.getName(), bean);
            }
            default -> throw new IllegalArgumentException("不支持的初始化类型");
        }
    }

    private Object getOrCreateBean(Class<?> clazz) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Object ex = beans.get(clazz.getName());
        if (ex == null) {
            BeanInitInfo initInfo = beanInitInfos.get(clazz);
            if (initInfo == null) {
                throw new IllegalArgumentException("缺少必要依赖 " + clazz);
            }
            initializeBean(initInfo);
            ex = beans.get(clazz.getName());
            if (ex == null) {
                throw new LackDependencyException(clazz.getName() + "缺少依赖" + clazz.getName());
            }
        }
        return ex;
    }

    private void checkDependency() {
        Collection<BeanInitInfo> initInfos = beanInitInfos.values();
        Map<Class<?>, List<Class<?>>> graph = new HashMap<>();
        initInfos.forEach(initInfo -> graph.put(initInfo.getClazz(), getClassDependencies(initInfo)));
        // 检查是否有环
        if (GraphUtil.isCircle(graph)) {
            throw new CircularDependence("初始化失败，存在循环依赖");
        }
    }

    private List<Class<?>> getClassDependencies(BeanInitInfo initInfo) {
        List<Class<?>> dependencies = new ArrayList<>();
        switch (initInfo.getWay()) {
            case CONSTRUCTOR -> {
                for (Field field : initInfo.getClazz().getDeclaredFields()) {
                    field.setAccessible(true);
                    NubboAutowired autowired = field.getAnnotation(NubboAutowired.class);
                    if (autowired != null) {
                        dependencies.add(field.getType());
                    }
                }
            }
            case METHOD -> {
                // 方法初始化的Bean需要配置类及其参数全部完成装配
                dependencies.add(initInfo.getMethodInitInfo().getInvoker());
                dependencies.addAll(Arrays.asList(initInfo.getMethodInitInfo().getParameterTypes()));
            }
            default -> throw new IllegalArgumentException("未知初始化方式:" + initInfo.getWay());
        }
        return dependencies;
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
