package core.common.jdbc;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JdbcClassUtils {
    static final Set<Class<?>> SUPPORTED_SQL_OBJECTS = new HashSet();

    static {
        Class[] classes = {
                Boolean.TYPE, Boolean.class,
                Short.TYPE, Short.class,
                Integer.TYPE, Integer.class,
                Long.TYPE, Long.class,
                Float.TYPE, Float.class,
                Double.TYPE, Double.class,
                String.class,
                Date.class,
                Timestamp.class,
                BigDecimal.class };
        for (Class cla:classes){
            SUPPORTED_SQL_OBJECTS.add(cla);
        }
    }
    public JdbcClassUtils() {
    }

    static boolean isSupportedSQLObject(Class<?> clazz) {
        return clazz.isEnum() || SUPPORTED_SQL_OBJECTS.contains(clazz);
    }

    public static Map<String, Method> findPublicGetters(Class<?> clazz) {
        Map<String, Method> map = new HashMap();
        Method[] methods = clazz.getMethods();
        Method[] var6 = methods;
        int var5 = methods.length;

        for(int var4 = 0; var4 < var5; ++var4) {
            Method method = var6[var4];
            if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 0 && !method.getName().equals("getClass")) {
                Class<?> returnType = method.getReturnType();
                if (!Void.TYPE.equals(returnType) && isSupportedSQLObject(returnType)) {
                    if ((returnType.equals(Boolean.TYPE) || returnType.equals(Boolean.class)) && method.getName().startsWith("is") && method.getName().length() > 2) {
                        map.put(getGetterName(method), method);
                    } else if (method.getName().startsWith("get") && method.getName().length() >= 4) {
                        map.put(getGetterName(method), method);
                    }
                }
            }
        }

        return map;
    }

    public static Field[] findFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    public static Map<String, Method> findPublicSetters(Class<?> clazz) {
        Map<String, Method> map = new HashMap();
        Method[] methods = clazz.getMethods();
        Method[] var6 = methods;
        int var5 = methods.length;

        for(int var4 = 0; var4 < var5; ++var4) {
            Method method = var6[var4];
            if (!Modifier.isStatic(method.getModifiers()) && Void.TYPE.equals(method.getReturnType()) && method.getParameterTypes().length == 1 && method.getName().startsWith("set") && method.getName().length() >= 4 && isSupportedSQLObject(method.getParameterTypes()[0])) {
                map.put(getSetterName(method), method);
            }
        }

        return map;
    }

    public static String getGetterName(Method getter) {
        String name = getter.getName();
        if (name.startsWith("is")) {
            name = name.substring(2);
        } else {
            name = name.substring(3);
        }

        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static String getSetterName(Method setter) {
        String name = setter.getName().substring(3);
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
