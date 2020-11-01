package pl.matsuo.interfacer.core;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static pl.matsuo.interfacer.core.CollectionUtil.filterMap;

public class ClasspathInterfacesScanner {

  private final Consumer<String> log;

  public ClasspathInterfacesScanner(Consumer<String> log) {
    this.log = log;
  }

  public List<IfcResolve> scanInterfacesFromClasspath(
      ClassLoader classLoader, String interfacePackage) {
    if (interfacePackage == null) {
      return emptyList();
    }

    String[] interfacePackages = interfacePackage.split(",");
    Reflections reflections = createReflections(classLoader, interfacePackages);

    List<IfcResolve> ifcs =
        filterMap(reflections.getSubTypesOf(Object.class), this::processClassFromClasspath);

    return ifcs;
  }

  public IfcResolve processClassFromClasspath(Class<?> type) {
    log.accept("Processing classpath type: " + type.getCanonicalName());
    if (type.isInterface()) {
      log.accept("Adding interface: " + type.getName());
      IfcResolve ifcResolve = new IfcResolve(type.getName(), null, type);

      for (Method method : type.getMethods()) {
        if (method.getParameterCount() == 0 && method.getName().startsWith("get")) {
          log.accept("Adding method: " + method.toString());
          ifcResolve.methods.add(new TypeWithName(method.getName(), null, method.getReturnType()));
        }
      }

      return ifcResolve;
    }

    return null;
  }

  public Reflections createReflections(ClassLoader classLoader, String[] interfacePackages) {
    return new Reflections(
        new ConfigurationBuilder()
            .addClassLoader(classLoader)
            .setUrls(ClasspathHelper.forClassLoader(classLoader))
            .setScanners(new SubTypesScanner(false))
            .filterInputsBy(new FilterBuilder().includePackage(interfacePackages)));
  }

  public ClassLoader getCompileClassLoader(List<String> compileClasspathElements) {
    List<URL> jars =
        filterMap(compileClasspathElements, name -> name.endsWith(".jar"), this::toUrl);
    jars.forEach(element -> log.accept("Compile classloader entry: " + element));

    ClassLoader classLoader = new URLClassLoader(jars.toArray(new URL[0]));
    return classLoader;
  }

  public URL toUrl(String name) {
    try {
      return new File(name).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
