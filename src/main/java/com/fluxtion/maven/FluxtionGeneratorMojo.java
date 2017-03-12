/* 
 * Copyright (C) 2017 V12 Technology Limited
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.fluxtion.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * A mojo to wrap the invocation of the Fluxtion statemachine generator
 * executable.
 *
 * @author Greg Higgins (greg.higgins@v12technology.com)
 */
@Mojo(name = "generate",
        requiresProject = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.COMPILE
)
public class FluxtionGeneratorMojo extends AbstractMojo {

    private String classPath;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            updateClasspath();
            try {
                setDefaultProperties();
                ProcessBuilder processBuilder = new ProcessBuilder();
                List<String> cmdList = new ArrayList<>();
                cmdList.add(fluxtionExePath.getCanonicalPath());
                if (logDebug) {
                    cmdList.add("--debug");
                }
                cmdList.add("-outDirectory");
                cmdList.add(outputDirectory);
                cmdList.add("-buildDirectory");
                cmdList.add(buildDirectory);
                cmdList.add("-outResDirectory");
                cmdList.add(resourcesOutputDirectory);
                cmdList.add("-outPackage");
                cmdList.add(packageName);
                cmdList.add("-outClass");
                cmdList.add(className);
                cmdList.add("-statePackage");
                cmdList.add(statePackage);
                //must be at end
                cmdList.add("-cp");
                cmdList.add(classPath);
                processBuilder.command(cmdList);
                processBuilder.redirectErrorStream(true);
                processBuilder.inheritIO();
                getLog().info(processBuilder.command().stream().collect(Collectors.joining(" ")));
                Process p = processBuilder.start();
                if (p.waitFor() < 0 && !ignoreErrors) {
                    throw new RuntimeException("unable to execute fluxtion-statemachine generator");
                }
            } catch (IOException | InterruptedException e) {
                getLog().error("error while invoking Fluxtion generator", e);
            }
        } catch (MalformedURLException | DependencyResolutionRequiredException ex) {
            getLog().error("error while building classpath", ex);
        }
    }

    private void setDefaultProperties() throws MojoExecutionException, IOException {
        try {
            if (outputDirectory == null || outputDirectory.length() < 1) {
                outputDirectory = project.getBasedir().getCanonicalPath() + "/target/generated-sources/fluxtion";
            }
            if (resourcesOutputDirectory == null || resourcesOutputDirectory.length() < 1) {
                resourcesOutputDirectory = project.getBasedir().getCanonicalPath() + "/target/generated-sources/fluxtion-meta";
            }
            if (buildDirectory == null) {
                buildDirectory = project.getBasedir().getCanonicalPath() + "/target/classes";
            }
        } catch (IOException iOException) {
            getLog().error(iOException);
            throw new MojoExecutionException("problem setting default properties", iOException);
        }
    }

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The path to fluxtion executable
     */
    @Parameter(property = "fluxtionExe", required = true)
    private File fluxtionExePath;

    /**
     * The package fluxtion will introspect to discover classes that are
     * annotated with the @StateHandler annotation.
     */
    @Parameter(property = "statePackage")
    private String statePackage;

    /**
     * The output package of the generated static event processor.
     */
    @Parameter(property = "packageName", required = true)
    private String packageName;

    /**
     * The simple class name of the generated static event processor.
     */
    @Parameter(property = "className", required = true)
    private String className;

    /**
     * The output directory for source artifacts generated by fluxtion.
     */
    @Parameter(property = "outputDirectory")
    private String outputDirectory;

    /**
     * The output directory for build artifacts generated by fluxtion.
     */
    @Parameter(property = "buildDirectory")
    private String buildDirectory;

    /**
     * The output directory for resources generated by fluxtion, such as a
     * meta-data describing the static event processor
     */
    @Parameter(property = "resourcesOutputDirectory")
    private String resourcesOutputDirectory;

    /**
     * Set log level to debug for fluxtion generation.
     */
    @Parameter(property = "logDebug", defaultValue = "false")
    public boolean logDebug;

    /**
     * continue build even if fluxtion tool returns an error
     */
    @Parameter(property = "ignoreErrors", defaultValue = "false")
    public boolean ignoreErrors;

    private void updateClasspath() throws MojoExecutionException, MalformedURLException, DependencyResolutionRequiredException {
        StringBuilder sb = new StringBuilder();
        List<String> elements = project.getRuntimeClasspathElements();
        for (String element : elements) {
            File elementFile = new File(element);
            getLog().debug("Adding element from runtime to classpath:" + elementFile.getPath());
            sb.append(elementFile.getPath()).append(";");
        }
        classPath = sb.substring(0, sb.length() - 1);
        getLog().debug("classpath:" + classPath);
    }

}
