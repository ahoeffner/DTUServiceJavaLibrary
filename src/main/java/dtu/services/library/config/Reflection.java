package dtu.services.library.config;

import java.util.Arrays;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;


/**
 * The library uses reflection to hide internal classes from public.
 * Partly to protect users from doing harm, partly to make it easier to use
 */
class Reflection
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

    /**
     * Dynamically invokes a method with any number of arguments.
     */
    public static Object invoke(Object obj, String method, Object... args) throws Exception
    {
        Class<?>[] types = getParameterTypes(args);
        Method meth = obj.getClass().getDeclaredMethod(method, types);

        meth.setAccessible(true);
        return(meth.invoke(obj, args));
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