import java.io.File;
import java.lang.reflect.InvocationTargetException;

public interface BeanFactory {
    void loadInjectProperties(File file);

    void loadValueProperties(File file);

    <T> T createInstance(Class<T> clazz);
}
