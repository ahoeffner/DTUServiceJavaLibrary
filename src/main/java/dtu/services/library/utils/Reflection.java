package dtu.services.library.utils;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;


/**
 * The library uses reflection to hide internal classes from public.
 * Partly to protect users from doing harm, partly to make it easier to use
 */
public class Reflection
{
    /**
     * Dynamically creates an instance using any number of arguments.
     */
    public static <T> T newInstance(String clazz) throws Exception
    {
        return(newInstance(clazz,new Object[0]));
    }


    @SuppressWarnings("unchecked")
    public static <T> T newInstance(String clazz, Object... args) throws Exception
    {
        Class<?> jclazz = Class.forName(clazz);
        Class<?>[] types = getParameterTypes(args);
        Constructor<?> constructor = jclazz.getDeclaredConstructor(types);

        constructor.setAccessible(true);
        return((T) constructor.newInstance(args));
    }


    public static Object invoke(Object obj, String method) throws Exception
    {
        return(invoke(obj,method,new Object[0]));
    }


    public static Object invoke(Object obj, String method, Object... args) throws Exception
    {
        Class<?>[] types = getParameterTypes(args);
        Method meth = obj.getClass().getDeclaredMethod(method, types);

        meth.setAccessible(true);
        return(meth.invoke(obj, args));
    }


    public static Object invokeStatic(String clazz, String method) throws Exception
    {
        return(invokeStatic(clazz,method,new Object[0]));
    }


    public static Object invokeStatic(String clazz, String method, Object... args) throws Exception
    {
        Class<?> jclazz = Class.forName(clazz);
        Class<?>[] types = getParameterTypes(args);
        Method meth = jclazz.getDeclaredMethod(method, types);

        meth.setAccessible(true);
        return(meth.invoke(null, args));
    }


    public static Object getField(Object instance, String field) throws Exception
    {
        Class<?> jclazz = instance.getClass();
        Field jfield = jclazz.getDeclaredField(field);

        jfield.setAccessible(true);
        return(jfield.get(instance));
    }


    public static Object getStaticField(String clazz, String field) throws Exception
    {
        Class<?> jclazz = Class.forName(clazz);
        Field jfield = jclazz.getDeclaredField(field);

        jfield.setAccessible(true);
        return(jfield.get(null));
    }


    private static Class<?>[] getParameterTypes(Object... args)
    {
        return
        (
                Arrays.stream(args)
                .map(arg -> arg == null ? Object.class : arg.getClass())
                .toArray(Class<?>[]::new)
        );
    }
}