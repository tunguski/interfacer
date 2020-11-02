package pl.matsuo.interfacer.core;

import lombok.extern.slf4j.Slf4j;
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

import static java.util.Collections.emptyList;
import static pl.matsuo.interfacer.core.CollectionUtil.filterMap;
import static pl.matsuo.interfacer.core.CollectionUtil.map;

@Slf4j
public class ClasspathInterfacesScanner {

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
    log.info("Processing classpath type: " + type.getCanonicalName());
    if (type.isInterface()) {
      log.info("Adding interface: " + type.getName());
      IfcResolve ifcResolve = new IfcResolve(type.getName(), null, type);

      for (Method method : type.getMethods()) {
        log.info("Adding method: " + method.getName());
        ifcResolve.methods.add(new TypeWithName(method));
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

  public static ClassLoader getCompileClassLoader(List<String> compileClasspathElements) {
    List<URL> jars = map(compileClasspathElements, ClasspathInterfacesScanner::toUrl);
    jars.forEach(element -> log.info("Compile classloader entry: " + element));

    return new URLClassLoader(jars.toArray(new URL[0]));
  }

  public static URL toUrl(String name) {
    try {
      return new File(name).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
