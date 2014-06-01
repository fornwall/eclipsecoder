package net.fornwall.eclipsecoder.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {

	public static Object getField(Object instance, String name) {
		return getInstanceField(instance, name, false);
	}

	public static Object getInstanceField(Object instance, String name, boolean inSuper) {
		try {
			Class<?> clazz = instance.getClass();
			if (inSuper) {
				clazz = clazz.getSuperclass();
			}
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(instance);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object invokeInstanceMethod(Object instance, String name, Object... args) {
		try {
			Class<?>[] parameterTypes = new Class[args.length];
			for (int i = 0; i < args.length; i++) {
				parameterTypes[i] = args[i].getClass();
			}
			Method method = instance.getClass().getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method.invoke(instance, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setInstanceField(Object instance, String name, Object value) {
		try {
			Field field = instance.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(instance, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}