import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Production-jar check: loads the remapped (intermediary) mod jar on top of the intermediary Minecraft
 * and dependency jars, without initialising anything, and reports concrete mod classes that leave an
 * abstract method unimplemented (what the JVM later throws as AbstractMethodError).
 * Usage: java ProdCheck.java <mod.jar> <classpath jars...>
 */
public class ProdCheck {
    public static void main(String[] args) throws Exception {
        File mod = new File(args[0]);
        List<URL> urls = new ArrayList<>();
        for (String a : args) urls.add(new File(a).toURI().toURL());
        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        int checked = 0, problems = 0, unloadable = 0;
        try (JarFile jar = new JarFile(mod)) {
            for (JarEntry e : Collections.list(jar.entries())) {
                String n = e.getName();
                if (!n.endsWith(".class") || n.contains("/mixins/") || n.startsWith("META-INF")) continue;
                String cn = n.substring(0, n.length() - 6).replace('/', '.');
                Class<?> c;
                try { c = Class.forName(cn, false, loader); } catch (Throwable t) { unloadable++; continue; }
                if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) continue;
                checked++;
                try {
                    for (Method m : c.getMethods()) {
                        if (Modifier.isAbstract(m.getModifiers())) {
                            problems++;
                            System.out.println("UNIMPLEMENTED " + cn + " :: " + m.getDeclaringClass().getName() + "." + m.getName() + Arrays.toString(m.getParameterTypes()));
                        }
                    }
                } catch (Throwable t) { unloadable++; }
            }
        }
        System.out.println("checked=" + checked + " unloadable=" + unloadable + " problems=" + problems);
        System.exit(problems == 0 ? 0 : 1);
    }
}
