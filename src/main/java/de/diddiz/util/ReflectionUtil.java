package de.diddiz.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;

public class ReflectionUtil {

    private static String versionString;

    public static String getVersion() {
        if (versionString == null) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            versionString = name.substring(name.lastIndexOf('.') + 1) + ".";
        }

        return versionString;
    }

    public static Class<?> getMinecraftClass(String minecraftClassName) throws ClassNotFoundException {
        String clazzName = "net.minecraft." + minecraftClassName;
        return Class.forName(clazzName);
    }

    public static Class<?> getCraftBukkitClass(String craftBukkitClassName) throws ClassNotFoundException {
        String clazzName = "org.bukkit.craftbukkit." + getVersion() + craftBukkitClassName;
        return Class.forName(clazzName);
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) throws NoSuchMethodException {
        return clazz.getMethod(methodName, params);
    }
}
