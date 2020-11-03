package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import pl.matsuo.interfacer.model.ifc.IfcResolve;
import pl.matsuo.interfacer.showcase.GenericInterface;
import pl.matsuo.interfacer.showcase.HasName;
import pl.matsuo.interfacer.showcase.MutableOwner;
import pl.matsuo.interfacer.util.Pair;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class TestInterfacesAdder {

  @Test
  public void testHasName() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleClass.java", HasName.class);
    assertEquals(1, modifications.size());
  }

  @Test
  public void testGenericInterface() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleGenericClass.java", GenericInterface.class);
    assertEquals(1, modifications.size());
  }

  @Test
  public void testNotGenericInterface() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleNotGenericClass.java", GenericInterface.class);
    assertEquals(0, modifications.size());
  }

  @Test
  public void testMutableOwner() {
    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        doTestInterface("/classes/test/SampleMutableClass.java", MutableOwner.class);
    assertEquals(1, modifications.size());
  }

  public static File fileForResource(String resourcePath) {
    return new File(TestInterfacesAdder.class.getResource(resourcePath).getFile());
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> doTestInterface(
      String className, Class<?> ifc) {
    InterfacesAdder interfacesAdder = new InterfacesAdder();

    File sampleClassFile = fileForResource(className);
    File scanDir = sampleClassFile.getParentFile();

    ParsingContext parsingContext = new ParsingContext(emptyList(), scanDir, scanDir);

    ClasspathInterfacesScanner interfacesScanner = new ClasspathInterfacesScanner();
    IfcResolve genericInterface =
        interfacesScanner.processClassFromClasspath(ifc, parsingContext.typeSolver);

    assertNotNull(genericInterface);

    ParseResult<CompilationUnit> compilationUnitParseResult =
        interfacesAdder.parseFile(parsingContext.javaParser, sampleClassFile);

    if (!compilationUnitParseResult.isSuccessful()) {
      log.info("" + compilationUnitParseResult.getProblems());
    }

    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        interfacesAdder.processAllFiles(
            asList(compilationUnitParseResult),
            asList(genericInterface),
            parsingContext.javaParser);

    modifications.forEach(mod -> log.info(mod.toString()));

    return modifications;
  }

  public List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> doTestInterface(
      String className, File interfacesDir) {
    SourceInterfacesScanner interfacesScanner = new SourceInterfacesScanner();

    List<IfcResolve> ifcResolves = interfacesScanner.scanInterfacesFromSrc(null, interfacesDir);
    assertFalse(ifcResolves.isEmpty());

    InterfacesAdder interfacesAdder = new InterfacesAdder();

    File sampleClassFile = fileForResource(className);
    File scanDir = sampleClassFile.getParentFile();

    ParsingContext parsingContext = new ParsingContext(emptyList(), scanDir, interfacesDir);

    ParseResult<CompilationUnit> compilationUnitParseResult =
        interfacesAdder.parseFile(parsingContext.javaParser, sampleClassFile);

    if (!compilationUnitParseResult.isSuccessful()) {
      log.info("" + compilationUnitParseResult.getProblems());
    }

    List<Pair<IfcResolve, ClassOrInterfaceDeclaration>> modifications =
        interfacesAdder.processAllFiles(
            asList(compilationUnitParseResult), ifcResolves, parsingContext.javaParser);

    modifications.forEach(mod -> log.info(mod.toString()));

    return modifications;
  }
}
