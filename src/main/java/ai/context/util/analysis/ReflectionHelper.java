package ai.context.util.analysis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionHelper {

    public String getMethods(Object o){
        return getMethodsForType(o.getClass(), false);
    }

    public String getFields(Object o){
        return getFieldsForType(o.getClass(), false);
    }

    public String getMethodsForType(Class type, boolean publicOnly){
        String methods = "";
        for (Method method : type.getDeclaredMethods()) {
            if(!publicOnly ||  method.getModifiers() == 1) {
                methods += method.getName() + Arrays.toString(method.getGenericParameterTypes()) + " -> " + method.getGenericReturnType() + "\n";
            }
        }
        return methods;
    }

    public String getFieldsForType(Class type, boolean publicOnly){
        String fields = "";
        for (Field field : type.getDeclaredFields()) {
            if(!publicOnly || field.isAccessible()) {
                fields += field.getName() + " : " + field.getType() + "\n";
            }
            //System.out.println(field.getName() + " : " + field.getType() + " " + field.getModifiers());
        }
        return fields;
    }
}
