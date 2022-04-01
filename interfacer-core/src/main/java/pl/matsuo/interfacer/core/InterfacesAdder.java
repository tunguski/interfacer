package pl.matsuo.interfacer.core;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static pl.matsuo.core.util.collection.CollectionUtil.anyMatch;
import static pl.matsuo.core.util.collection.CollectionUtil.filterMap;
import static pl.matsuo.core.util.collection.CollectionUtil.flatMap;
import static pl.matsuo.core.util.collection.Pair.pair;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import pl.matsuo.core.util.collection.Pair;
import pl.matsuo.interfacer.model.ifc.IfcResolve;

@Slf4j
public class InterfacesAdder {

  /**
   * Add interfaces found in <code>interfacesDirectory</code> and <code>compileClasspathElements
   * </code> to classes found in <code>scanDirectory</code>.
   *
   * <p>Method will execute multiple passes of adding. When classes implement additional interfaces,
   * it's possible that some classes may now match new interfaces.
   *
   * <pre>
   *         interface SampleInterface {
   *           Integer getValue();
   *         }
   *
   *         interface SampleInterface2 {
   *           SampleInterface getResult();
   *         }
   *
   *         class SampleResult {
   *           Integer getValue();
   *         }
   *
   *         class Sample {
   *             SampleResult getResult();
   *         }
   *     </pre>
   *
   * In first pass we can add <code>SampleInterface</code> to <code>SampleResult</code>. Now in
   * second pass we can add <code>SampleInterface2</code> to <code>Sample</code>.
   */
  public void addInterfacesAllFiles(
      @NonNull File scanDirectory,
      File interfacesDirectory,
      String interfacePackage,
      List<String> compileClasspathElements) {

    if (interfacesDirectory == null
        && (interfacePackage == null || compileClasspathElements == null)) {
      throw new RuntimeException(
          "No interface source defined: interfacesDirectory "
              + interfacesDirectory
              + " interfacePackage "
              + interfacePackage
              + " compileClasspathElements "
              + compileClasspathElements);
    }

    log.info("Start processing");

    try {
      List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> allModifications = new ArrayList<>();
      while (true) {
        List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
            addInterfacesAllFiles(
                scanDirectory,
                interfacesDirectory,
                interfacePackage,
                compileClasspathElements,
                allModifications);

        if (modifications.isEmpty()) {
          break;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading from source directory", e);
    }
  }

  /** Single pass of interface adding. */
  private List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> addInterfacesAllFiles(
      File scanDirectory,
      File interfacesDirectory,
      String interfacePackage,
      List<String> compileClasspathElements,
      List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> allModifications)
      throws IOException {
    ParsingContext parsingContext =
        new ParsingContext(compileClasspathElements, scanDirectory, interfacesDirectory);

    final SourceRoot source =
        new SourceRoot(scanDirectory.toPath(), parsingContext.parserConfiguration);

    List<IfcResolve> ifcs = scanInterfaces(interfacesDirectory, interfacePackage, parsingContext);

    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        processAllFiles(source.tryToParse(), ifcs, parsingContext.javaParser);
    allModifications.addAll(modifications);

    // save changes on disk
    source.saveAll();
    return modifications;
  }

  /** Parse file using internal {@link JavaParser}. */
  public ParseResult<CompilationUnit> parseFile(JavaParser javaParser, File file) {
    try {
      return javaParser.parse(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** Search for interfaces on classpath and in source folder. */
  public List<IfcResolve> scanInterfaces(
      File interfacesDirectory, String interfacePackage, ParsingContext parsingContext) {
    List<IfcResolve> ifcs = new ArrayList<>();
    ifcs.addAll(
        new ClasspathInterfacesScanner()
            .scanInterfacesFromClasspath(
                parsingContext.classLoader, interfacePackage, parsingContext.typeSolver));
    ifcs.addAll(
        new SourceInterfacesScanner()
            .scanInterfacesFromSrc(parsingContext.parserConfiguration, interfacesDirectory));
    Comparator<IfcResolve> comparator = comparing(i -> -i.getMethods().size());
    ifcs.sort(comparator);
    return ifcs;
  }

  /** Go through all parsed generated classes and try adding interfaces to them. */
  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> processAllFiles(
      List<ParseResult<CompilationUnit>> parseResults,
      List<IfcResolve> ifcs,
      JavaParser javaParser) {

    return flatMap(
        parseResults,
        parseResult -> {
          // Only deal with files without parse errors
          if (parseResult.isSuccessful()) {
            return parseResult
                .getResult()
                .map(
                    cu -> {
                      // Do the actual logic
                      return addInterfaces(cu, ifcs, javaParser);
                    })
                .orElse(emptyList());
          } else {
            log.warn("Parse failure for " + parseResult.getProblems());
            return emptyList();
          }
        });
  }

  /** Add interfaces to class parsed into <code>compilationUnit</code>. */
  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> addInterfaces(
      CompilationUnit compilationUnit, List<IfcResolve> ifcs, JavaParser javaParser) {
    return compilationUnit
        .getPrimaryType()
        .map(
            primaryType ->
                primaryType.isClassOrInterfaceDeclaration()
                    ? (ClassOrInterfaceDeclaration) primaryType
                    : null)
        .filter(declaration -> !declaration.isInterface())
        .map(
            declaration ->
                filterMap(
                    ifcs, ifc -> processDeclarationWithInterface(declaration, ifc, javaParser)))
        .orElse(emptyList());
  }

  /**
   * Check if class <code>declaration</code> is matching interface <code>ifc</code>. If true, add
   * interface to the class. Return <code>pair</code> representing interface and class. Return
   * <code>null</code> if interface was not added.
   */
  public Pair<IfcResolve, ClassOrInterfaceDeclaration> processDeclarationWithInterface(
      ClassOrInterfaceDeclaration declaration, IfcResolve ifc, JavaParser javaParser) {

    Map<String, String> resolvedTypeVariables = ifc.matches(declaration);

    // if any of the declaration's ancestors is already assignable to ifc
    boolean canBeAssignedTo = canBeAssignedTo(declaration, ifc);
    if (resolvedTypeVariables != null && !canBeAssignedTo) {
      return addInterfaceToClassDeclaration(declaration, ifc, javaParser, resolvedTypeVariables);
    }

    return null;
  }

  /**
   * Create interface <code>ifc</code> representation in <code>javaparser</code> and add it to the
   * class <code>declaration</code>.
   */
  private Pair<IfcResolve, ClassOrInterfaceDeclaration> addInterfaceToClassDeclaration(
      ClassOrInterfaceDeclaration declaration,
      IfcResolve ifc,
      JavaParser javaParser,
      Map<String, String> resolvedTypeVariables) {
    log.info(
        "Modifying the class: "
            + declaration.getFullyQualifiedName()
            + " with ifc "
            + ifc.getName());

    ClassOrInterfaceType type = // new ClassOrInterfaceType(ifc.getName());
        javaParser
            .parseClassOrInterfaceType(ifc.getGenericName(resolvedTypeVariables))
            .getResult()
            .orElseThrow(() -> new RuntimeException(""));
    // type.setTypeArguments(ifc.getTypeArguments(resolvedTypeVariables));

    declaration.addImplementedType(type);
    return pair(ifc, declaration);
  }

  /** Check if <code>declaration</code> is representing subtype of interface <code>ifc</code>. */
  private boolean canBeAssignedTo(ClassOrInterfaceDeclaration declaration, IfcResolve ifc) {
    return anyMatch(
        declaration.resolve().getAncestors(),
        ancestor -> {
          try {
            return ifc.getResolvedTypeDeclaration().isAssignableBy(ancestor);
          } catch (RuntimeException e) {
            // e.printStackTrace();
            return false;
          }
        });
  }
}
