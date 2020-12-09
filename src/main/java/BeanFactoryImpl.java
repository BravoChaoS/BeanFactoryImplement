import annotations.Inject;
import annotations.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class BeanFactoryImpl implements BeanFactory {
    private Properties injectProperties;

    private Properties valueProperties;

    public Properties getInjectProperties() {
        return injectProperties;
    }

    public Properties getValueProperties() {
        return valueProperties;
    }

    Class<?> getInjectClass(String className) {
        try {
            return Class.forName(injectProperties.getProperty(className, className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    Object getValueObjectSingle(String valueName, Class<?> valueType) {
        String value = valueProperties.getProperty(valueName);

        Class<?> boxed = valueType;
        if (valueType.isPrimitive())
            boxed = Array.get(Array.newInstance(valueType, 1), 0).getClass();

        System.out.println(boxed.getName());
        Object obj = null;
        try {
            Constructor<?> cons = boxed.getDeclaredConstructor(String.class);
            obj = cons.newInstance(value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        return obj;
    }

    Object arrayWrapperToPrimitive(Object obj, Class<?> type) {
        Object[] objs = (Object[]) obj;
        int len = objs.length;
        if (int.class.equals(type)) {
            int[] ret = new int[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (int) objs[i];
            }
            return ret;
        } else if (long.class.equals(type)) {
            long[] ret = new long[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (long) objs[i];
            }
            return ret;

        } else if (float.class.equals(type)) {
            float[] ret = new float[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (float) objs[i];
            }
            return ret;

        } else if (double.class.equals(type)) {
            double[] ret = new double[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (double) objs[i];
            }
            return ret;

        } else if (boolean.class.equals(type)) {
            boolean[] ret = new boolean[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (boolean) objs[i];
            }
            return ret;

        } else if (char.class.equals(type)) {
            char[] ret = new char[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (char) objs[i];
            }
            return ret;

        } else if (short.class.equals(type)) {
            short[] ret = new short[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (short) objs[i];
            }
            return ret;

        } else if (byte.class.equals(type)) {
            byte[] ret = new byte[len];
            for (int i = 0; i < len; ++i) {
                ret[i] = (byte) objs[i];
            }
            return ret;

        } else {
            System.out.println("not primitive type");
            return obj;
        }
    }

    Object getValueObjectArray(String valueName, Class<?> valueType, String split) {
        String value = valueProperties.getProperty(valueName);
        String[] values = value.split(split);
//        System.out.println(Arrays.toString(values));

        Class<?> boxed = valueType;
        if (valueType.isPrimitive())
            boxed = Array.get(Array.newInstance(valueType, 1), 0).getClass();

        Object[] objs = (Object[]) Array.newInstance(boxed, values.length);
        for (int i = 0; i < objs.length; ++i) {
            try {
                Constructor<?> cons = boxed.getDeclaredConstructor(String.class);
                objs[i] = cons.newInstance(values[i]);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        }


        if (valueType.isPrimitive()) {
            return arrayWrapperToPrimitive(objs, valueType);
        } else {
            return objs;
        }

    }

    <T> T getValueObject(String valueName, Class<T> valueType, String split) {
        T ret;

        if (valueType.isArray()) {
            ret = (T) getValueObjectArray(valueName, valueType.getComponentType(), split);
        } else {
            ret = (T) getValueObjectSingle(valueName, valueType);
        }

        return ret;
    }

    @Override
    public void loadInjectProperties(File file) {
        injectProperties = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            injectProperties.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadValueProperties(File file) {
        valueProperties = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            valueProperties.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> T createInstance(Class<T> clazz) {
        T instance = null;

        Class<?> injectClass = getInjectClass(clazz.getName());

        Constructor<?>[] constructors = injectClass.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            if (constructors.length <= 1 || constructor.isAnnotationPresent(Inject.class)) {
                Parameter[] parameters = constructor.getParameters();
                ArrayList<Object> values = new ArrayList<>();
                for (Parameter parameter : parameters) {
                    if (parameter.isAnnotationPresent(Value.class)) {
                        Value annotation = parameter.getAnnotation(Value.class);
                        values.add(getValueObject(annotation.value(), parameter.getType(), annotation.delimiter()));
                    } else {
                        Class<?> injectType = getInjectClass(parameter.getType().getName());
                        values.add(createInstance(injectType));
                    }
                }
                try {
                    instance = (T) constructor.newInstance(values.toArray());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        Field[] fields = injectClass.getDeclaredFields();

        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Inject) {
                    field.setAccessible(true);
                    Class<?> injectType = getInjectClass(field.getType().getName());
                    try {
                        field.set(instance, createInstance(injectType));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if (annotation instanceof Value) {
                    field.setAccessible(true);
                    try {
                        field.set(instance, getValueObject(((Value) annotation).value(), field.getType(), ((Value) annotation).delimiter()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        return instance;
    }
}
