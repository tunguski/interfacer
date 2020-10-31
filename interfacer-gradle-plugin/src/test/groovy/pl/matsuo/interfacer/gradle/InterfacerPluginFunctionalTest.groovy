package pl.matsuo.interfacer.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class InterfacerPluginFunctionalTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'java-library'
                id 'pl.matsuo.interfacer'
            }
            
            repositories {
                mavenLocal()
                jcenter()
            }
            
            dependencies {
                implementation 'org.apache.avro:avro:1.10.0'
                implementation 'pl.matsuo:sample-interfaces:1.0-SNAPSHOT'
            }
        """
    }

    def "can successfully diff 2 files"() {
        given:
        File srcJava = testProjectDir.newFolder("src", "main", "java")
        File genJava = testProjectDir.newFolder('build', 'generated')
        File testInterface = new File(testProjectDir.newFolder("src", "main", "java", "ifc"), 'SampleInterface.java')
        File testScannedClass = new File(testProjectDir.newFolder('build', 'generated', 'gen'), 'SampleClass.java')

        testInterface << """
        package ifc;
        
        public interface SampleInterface {
        
          String getName();
        }
        """

        testScannedClass << """
        package gen;
        
        public class SampleClass {
        
          public String getName() {
            return "My name is";
          }
        }
        """

        buildFile << """
            interfacer {
                interfacePackage = 'pl.matsuo.interfacer.showcase'
                interfacesDirectory = file('${srcJava.getPath().replaceAll("[\\\\]", "/")}')
                scanDirectory = file('${genJava.getPath().replaceAll("[\\\\]", "/")}')
            }
        """

        println "testScannedClass:"
        println testScannedClass.text

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('interfacer')
                .withPluginClasspath()
                .build()

        println "testScannedClass:"
        println testScannedClass.exists()

        then:
        testScannedClass.text.contains("pl.matsuo.interfacer.showcase.HasName, ifc.SampleInterface")
        result.output.contains("Modifying the class!")
        result.task(":interfacer").outcome == SUCCESS
    }
}