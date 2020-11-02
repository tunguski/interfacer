package pl.matsuo.interfacer.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import pl.matsuo.interfacer.showcase.HasName;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;

@Slf4j
public class TestInterfacesAdder {

  @Test
  public void doTest() throws Exception {
    ClasspathInterfacesScanner classpathInterfacesScanner = new ClasspathInterfacesScanner();

    IfcResolve genericInterface =
        classpathInterfacesScanner.processClassFromClasspath(HasName.class);

    Assert.assertNotNull(genericInterface);

    InterfacesAdder interfacesAdder = new InterfacesAdder();

    URL sampleClassResource = getClass().getResource("/classes/test/SampleClass.java");
    File scanDir = new File(sampleClassResource.getFile()).getParentFile();
    File interfacesDir =
        new File(getClass().getResource("/interfaces/test/SampleInterface.java").getFile())
            .getParentFile();

    AtomicBoolean modified = new AtomicBoolean();
    CombinedTypeSolver typeSolver =
        interfacesAdder.createTypeSolver(scanDir, interfacesDir, getClass().getClassLoader());

    ParseResult<CompilationUnit> compilationUnitParseResult =
        interfacesAdder.parseFile(typeSolver, new File(sampleClassResource.getFile()));

    if (!compilationUnitParseResult.isSuccessful()) {
      log.info("" + compilationUnitParseResult.getProblems());
    }

    interfacesAdder.processAllFiles(
        modified, typeSolver, asList(compilationUnitParseResult), asList(genericInterface));

    Assert.assertTrue(modified.get());
  }
}
