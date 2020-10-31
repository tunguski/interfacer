package pl.matsuo.interfacer.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class InterfacerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('interfacer', InterfacerExtension)

        project.tasks.create('interfacer', InterfacerTask) {
            interfacePackage = project.interfacer.interfacePackage
            interfacesDirectory = project.interfacer.interfacesDirectory
            scanDirectory = project.interfacer.scanDirectory
        }
    }
}
