package pl.matsuo.interfacer.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import pl.matsuo.interfacer.core.InterfacesAdder

import java.util.function.Supplier

class InterfacerTask extends DefaultTask {

    @Input
    @Optional
    final Property<String> interfacePackage = project.objects.property(String)

    @InputDirectory
    final Property<File> interfacesDirectory = project.objects.property(File)

    @InputDirectory
    final Property<File> scanDirectory = project.objects.property(File)

    def makeJavaParserLogToMavenOutput() {
        com.github.javaparser.utils.Log.setAdapter(new com.github.javaparser.utils.Log.Adapter() {
            void info(Supplier<String> message) { println "[parser adapter info] " + message.get() }

            void trace(Supplier<String> message) {
                println "[parser adapter trace] " + message.get()
            }

            void error(Supplier<Throwable> throwableSupplier, Supplier<String> messageSupplier) {
                println "[parser adapter error] " + messageSupplier.get()
                println throwableSupplier.get()
            }
        });
    }

    @TaskAction
    def addInterfaces() {
        try {
            makeJavaParserLogToMavenOutput()

            SourceDirectorySet sourceDirectorySet = project.objects.sourceDirectorySet('sources', 'sources')
            File source =
                    interfacesDirectory.isPresent() ?
                            interfacesDirectory.get() :
                            sourceDirectorySet.srcDirs.isEmpty() ?
                                    null : sourceDirectorySet.srcDirs.iterator().next()

            println "Ok, we have the source " + source

            File scanDir = scanDirectory.get()
            println scanDir

            println "dependencies:"
            println project.configurations.compileClasspath.allDependencies
            List<String> files = project.configurations.compileClasspath.getIncoming().getFiles().toList().stream()
                    .collect { it.getPath().replaceAll("[\\\\]", "/") }

            new InterfacesAdder({ msg -> println msg })
                    .addInterfacesAllFiles(
                            scanDir,
                            source,
                            interfacePackage.getOrNull(),
                            files);
        } catch (Exception e) {
            e.printStackTrace()
            throw e
        }
    }
}
