package pl.matsuo.interfacer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.javaparser.utils.CodeGenerationUtils.f;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * This plugin is a sample for building your own plugins. It takes a directory of source code and
 * adds a trace line to each method.
 */
@Mojo(
    name = "add-interfaces",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class InterfacerPluginMojo extends AbstractMojo {

  /** Package aggregating interfaces that should be scanned through entities. */
  @Parameter(required = false)
  String interfacePackage;

  /** Location for the source files with added trace lines. */
  @Parameter(required = true)
  File interfacesDirectory;

  /** Location where the modified source files should be saved. */
  @Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/avro")
  File scanDirectory;

  /** The current Maven project. */
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    JavaParserMavenUtils.makeJavaParserLogToMavenOutput(getLog());

    addInterfacesAllFiles(scanDirectory);
  }

  private List<IfcResolve> scanInterfacesFromSrc(ParserConfiguration parserConfiguration)
      throws MojoExecutionException {
    List<IfcResolve> ifcs = new ArrayList<>();

    final SourceRoot source = new SourceRoot(interfacesDirectory.toPath(), parserConfiguration);
    try {
      for (ParseResult<CompilationUnit> parseResult : source.tryToParse()) {
        // Only deal with files without parse errors
        if (parseResult.isSuccessful()) {
          parseResult
              .getResult()
              .ifPresent(
                  cu -> {
                    // Do the actual logic
                    IfcResolve ifcResolve = getIfcResolve(cu);
                    if (ifcResolve != null) {
                      ifcs.add(ifcResolve);
                    }
                  });
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading from source directory", e);
    }

    return ifcs;
  }

  private IfcResolve getIfcResolve(CompilationUnit cu) {
    return cu.getPrimaryType()
        .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
        .map(type -> (ClassOrInterfaceDeclaration) type)
        .filter(ClassOrInterfaceDeclaration::isInterface)
        .map(
            type -> {
              getLog().info("Adding interface: " + type.getNameAsString());
              IfcResolve ifcResolve =
                  new IfcResolve(
                      cu.getPackageDeclaration()
                              .map(packageDeclaration -> packageDeclaration.getNameAsString() + ".")
                              .orElse("")
                          + type.getNameAsString(),
                      type.resolve(),
                      null);

              type.getMethods()
                  .forEach(
                      methodDeclaration -> {
                        if (methodDeclaration.getParameters().size() == 0
                            && methodDeclaration.getNameAsString().startsWith("get")) {
                          getLog().info("Adding method: " + methodDeclaration);
                          ifcResolve.methods.add(
                              new TypeWithName(
                                  methodDeclaration.getNameAsString(),
                                  methodDeclaration.getType().resolve(),
                                  null));
                        }
                      });

              return ifcResolve;
            })
        .orElse(null);
  }

  private List<IfcResolve> scanInterfacesFromClasspath(ClassLoader classLoader)
      throws MojoExecutionException {
    if (interfacePackage == null) {
      return emptyList();
    }

    String[] interfacePackages = interfacePackage.split(",");
    Reflections reflections =
        new Reflections(
            new ConfigurationBuilder()
                .addClassLoader(classLoader)
                .setUrls(
                    asList(interfacePackages).stream()
                        .flatMap(ifcPack -> ClasspathHelper.forPackage(ifcPack).stream())
                        .collect(toList()))
                .setScanners(new SubTypesScanner(false))
                .filterInputsBy(new FilterBuilder().includePackage(interfacePackages)));

    List<IfcResolve> ifcs = new ArrayList<>();

    reflections
        .getSubTypesOf(Object.class)
        .forEach(
            type -> {
              getLog().info("Processing type: " + type.getCanonicalName());
              if (type.isInterface()) {
                getLog().info("Adding interface: " + type.getName());
                IfcResolve ifcResolve = new IfcResolve(type.getName(), null, type);

                for (Method method : type.getMethods()) {
                  if (method.getParameterCount() == 0 && method.getName().startsWith("get")) {
                    getLog().info("Adding method: " + method.toString());
                    ifcResolve.methods.add(
                        new TypeWithName(method.getName(), null, method.getReturnType()));
                  }
                }

                ifcs.add(ifcResolve);
              }
            });

    return ifcs;
  }

  private ClassLoader getCompileClassLoader() throws MojoExecutionException {
    try {
      List<String> compileClasspathElements = project.getCompileClasspathElements();

      List<URL> jars =
          compileClasspathElements.stream()
              .filter(name -> name.endsWith(".jar"))
              .map(this::toUrl)
              .collect(toList());
      jars.forEach(element -> getLog().info("Processing entry: " + element));

      ClassLoader classLoader = new URLClassLoader(jars.toArray(new URL[0]));
      return classLoader;
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("DependencyResolutionRequiredException", e);
    }
  }

  private URL toUrl(String name) {
    try {
      return new File(name).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private void addInterfacesAllFiles(File scanDirectory) throws MojoExecutionException {
    try {
      AtomicBoolean modified = new AtomicBoolean(true);
      while (modified.get()) {
        modified.set(false);

        ClassLoader classLoader = getCompileClassLoader();

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ClassLoaderTypeSolver(classLoader) {});
        combinedTypeSolver.add(new JavaParserTypeSolver(scanDirectory.toPath()));
        combinedTypeSolver.add(new JavaParserTypeSolver(interfacesDirectory.toPath()));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(symbolSolver);
        final SourceRoot source = new SourceRoot(scanDirectory.toPath(), parserConfiguration);

        List<IfcResolve> ifcs = new ArrayList<>();
        ifcs.addAll(scanInterfacesFromClasspath(classLoader));
        ifcs.addAll(scanInterfacesFromSrc(parserConfiguration));

        for (ParseResult<CompilationUnit> parseResult : source.tryToParse()) {
          // Only deal with files without parse errors
          if (parseResult.isSuccessful()) {
            parseResult
                .getResult()
                .ifPresent(
                    cu -> {
                      // Make the plugin a little noisier
                      cu.getStorage()
                          .ifPresent(
                              storage -> Log.info(f("Processing %s...", storage.getFileName())));
                      // Do the actual logic
                      boolean modifiedState = addInterfaces(cu, ifcs, combinedTypeSolver);
                      modified.set(modified.get() || modifiedState);
                    });
          }
        }

        // save changes on disk
        source.saveAll();
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading from source directory", e);
    }
  }

  private boolean addInterfaces(
      CompilationUnit compilationUnit,
      List<IfcResolve> ifcs,
      CombinedTypeSolver combinedTypeSolver) {
    AtomicBoolean modified = new AtomicBoolean();

    compilationUnit
        .getPrimaryType()
        .map(
            primaryType ->
                primaryType.isClassOrInterfaceDeclaration()
                    ? (ClassOrInterfaceDeclaration) primaryType
                    : null)
        .filter(declaration -> !declaration.isInterface())
        .ifPresent(
            declaration ->
                ifcs.forEach(
                    ifc ->
                        processDeclarationWithInterface(
                            combinedTypeSolver, modified, declaration, ifc)));

    return modified.get();
  }

  private void processDeclarationWithInterface(
      CombinedTypeSolver combinedTypeSolver,
      AtomicBoolean modified,
      ClassOrInterfaceDeclaration declaration,
      IfcResolve ifc) {
    boolean allMatch = true;
    for (TypeWithName methodDecl : ifc.methods) {
      boolean found =
          declaration.getMethodsBySignature(methodDecl.name).stream()
              .anyMatch(
                  method -> methodDecl.resolvedType.isAssignableBy(method.getType().resolve()));
      allMatch = allMatch && found;
    }

    getLog()
        .info(
            "Processing declaration "
                + declaration.getFullyQualifiedName()
                + " with ifc "
                + ifc.name);
    ResolvedReferenceTypeDeclaration other =
        ifc.resolve != null
            ? ifc.resolve
            : new ReflectionInterfaceDeclaration(ifc.clazz, combinedTypeSolver);

    boolean canBeAssignedTo =
        declaration.resolve().getAncestors().stream()
            .anyMatch(ancestor -> other.isAssignableBy(ancestor));
    if (allMatch && !canBeAssignedTo) {
      getLog().info("Modifying the class!");
      declaration.addImplementedType(ifc.name);
      modified.set(true);
    }
  }
}
