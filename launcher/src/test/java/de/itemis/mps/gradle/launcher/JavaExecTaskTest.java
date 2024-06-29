package de.itemis.mps.gradle.launcher;

import de.itemis.mps.gradle.LauncherPlugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.JavaExec;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

public class JavaExecTaskTest {

    @Test
    public void overridesDefaultJavaLauncher(@TempDir File tempDir) {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final MpsBackendLauncher launcher = project.getExtensions().getByType(MpsBackendLauncher.class);
        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);

        final JavaLauncher customJavaLauncher = new CustomJavaLauncher("java launcher of backend builder");

        launcher.builder()
                .withJavaLauncher(customJavaLauncher)
                .configure(task);

        Assertions.assertSame(customJavaLauncher, task.getJavaLauncher().get());
    }

    @Test
    public void keepsExplicitJavaLauncher(@TempDir File tempDir) {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final JavaLauncher javaLauncherOfTask = new CustomJavaLauncher("java launcher of task");
        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);
        task.getJavaLauncher().set(javaLauncherOfTask);

        final JavaLauncher javaLauncherOfBackendBuilder = new CustomJavaLauncher("java launcher of backend builder");
        final MpsBackendLauncher launcher = project.getExtensions().getByType(MpsBackendLauncher.class);
        launcher.builder().withJavaLauncher(javaLauncherOfBackendBuilder).configure(task);

        Assertions.assertSame(javaLauncherOfTask, task.getJavaLauncher().get(),
                "java launcher of backend builder should not override java launcher of task");
    }

    private static class CustomJavaLauncher implements JavaLauncher {
        private final String description;

        public CustomJavaLauncher(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public JavaInstallationMetadata getMetadata() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RegularFile getExecutablePath() {
            throw new UnsupportedOperationException();
        }
    }
}
