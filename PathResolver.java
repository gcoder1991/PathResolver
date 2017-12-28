import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;

/**
 * author : gcoder
 */
public final class PathResolver {

    /**
     * 获取指定包内继承自father并且含有注解annotationClass
     * @param pkgPath
     * @param annotationClass
     * @param father
     * @param <F>
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static final <F> Collection<Class<? extends F>> scanPkg(String pkgPath, Class<? extends Annotation> annotationClass, Class<F> father) {
        List<Class<? extends F>> result = new ArrayList<>();
        Collection<Class<?>> classes = scanPkg(pkgPath);
        classes.stream().filter(clazz -> father.isAssignableFrom(clazz) && clazz.isAnnotationPresent(annotationClass))
                .forEach(t -> result.add((Class<? extends F>) t));
        return result;
    }

    /**
     * 获取指定包内所有含有指定注解的类
     *
     * @param pkgPath
     * @param annotationClass
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static final Collection<Class<?>> scanPkgWithAnnotation(String pkgPath, Class<? extends Annotation> annotationClass) {
        List<Class<?>> result = new ArrayList<>();
        Collection<Class<?>> classes = scanPkg(pkgPath);
        classes.stream().filter(clazz -> clazz.isAnnotationPresent(annotationClass))
                .forEach(result::add);
        return result;
    }

    /**
     * 获取指定包内所有father的子类
     *
     * @param pkgPath
     * @param father
     * @param <F>
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static final <F> Collection<Class<? extends F>> scanPkgWithFather(String pkgPath, Class<F> father) {
        List<Class<? extends F>> result = new ArrayList<>();
        Collection<Class<?>> classes = scanPkg(pkgPath);
        classes.stream().filter(clazz -> father.isAssignableFrom(clazz))
                .forEach(t -> result.add((Class<? extends F>) t));
        return result;
    }

    public static Collection<Class<?>> scanPkg(String packagePath) {
        List<Class<?>> result = new ArrayList<>();

        String path = packagePath.replace('.', '/');

        try {
            Enumeration<URL> resources = ClassLoader.getSystemResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                switch (url.getProtocol()) {
                    case "file":
                        result.addAll(fileClassScan(Paths.get(url.toURI()), packagePath));
                        break;
                    case "jar":
                        result.addAll(jarClassScan(url, packagePath));
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Collection<Class<?>> jarClassScan(URL url, String pkgName) throws IOException {
        List<Class<?>> result = new ArrayList<>();
        String path = pkgName.replace('.', '/');
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        Enumeration<JarEntry> entries = conn.getJarFile().entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            if (name.contains(path) && !entry.isDirectory()) {
                name = name.substring(0, name.lastIndexOf(".class")).replace('/', '.');
                try {
                    Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(name);
                    result.add(clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static Collection<Class<?>> fileClassScan(Path p, String pkgName) throws IOException {
        ConcurrentLinkedQueue<Class<?>> result = new ConcurrentLinkedQueue<>();
        Files.list(p).forEach((Path path) -> {
                    String fileName = path.toFile().getName();
                    if (Files.isDirectory(path)) {
                        try {
                            result.addAll(fileClassScan(path, pkgName + "." + fileName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (fileName.endsWith(".class")) {
                            String className = (pkgName + "." + fileName.substring(0, fileName.lastIndexOf(".class")));
                            try {
                                Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className);
                                result.offer(clazz);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
        return result;
    }

}
