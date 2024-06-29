package de.itemis.mps.gradle.launcher;

import de.itemis.mps.gradle.LauncherPlugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.testfixtures.ProjectBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaLauncherOverrideTest {

    @Test
    public void overridesDefaultJavaLauncher(@TempDir File tempDir) {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final MpsBackendLauncher launcher = project.getExtensions().getByType(MpsBackendLauncher.class);
        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);

        final JavaLauncher customJavaLauncher = mock(JavaLauncher.class, "java launcher of backend builder");

        launcher.builder()
                .withJavaLauncher(customJavaLauncher)
                .configure(task);

        Assertions.assertSame(customJavaLauncher, task.getJavaLauncher().get());
    }

    @Test
    public void canOverrideJavaLauncher(@TempDir File tempDir) {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);

        final JavaLauncher javaLauncherOfBackendBuilder = mock(JavaLauncher.class, "java launcher of backend builder");
        final MpsBackendLauncher launcher = project.getExtensions().getByType(MpsBackendLauncher.class);
        launcher.builder().withJavaLauncher(javaLauncherOfBackendBuilder).configure(task);

        final JavaLauncher javaLauncherOfTask = mock(JavaLauncher.class, "java launcher of task");
        task.getJavaLauncher().set(javaLauncherOfTask);

        Assertions.assertSame(javaLauncherOfTask, task.getJavaLauncher().get(),
                "java launcher of backend builder should not override java launcher of task");
    }

    @Test
    public void keepsExplicitJavaExecutable(@TempDir File tempDir) {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final String executableOfTask = "executable of task";
        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);
        task.executable(executableOfTask);

        final JavaLauncher javaLauncherOfBackendBuilder = mock(JavaLauncher.class, "java launcher of backend builder");
        final MpsBackendLauncher launcher = project.getExtensions().getByType(MpsBackendLauncher.class);
        launcher.builder().withJavaLauncher(javaLauncherOfBackendBuilder).configure(task);

        when(javaLauncherOfBackendBuilder.getExecutablePath()).thenReturn(project.getLayout().getProjectDirectory().file("backend java"));

        Assertions.assertEquals(executableOfTask, task.getExecutable(),
                "java launcher of backend builder should not override java launcher of task");
    }

    @Test
    public void canUnsetLauncherAndOverrideExecutable(@TempDir File tempDir) {
        // This is a pattern that some code uses. The task is configured implicitly via backend builder, then its
        // launcher is explicitly set to null and an executable is specified.

        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);
        final JavaLauncher javaLauncherOfBackendBuilder = createLauncherWithExecutable(project, "backend java");
        final MpsBackendLauncher launcher = project.getExtensions().getByType(MpsBackendLauncher.class);
        launcher.builder().withJavaLauncher(javaLauncherOfBackendBuilder).configure(task);

        // Executable has to be valid, otherwise task.getJavaLauncher() will fail.
        final String executableOfTask = Jvm.current().getJavaExecutable().toString();

        task.getJavaLauncher().set((JavaLauncher) null);
        task.executable(executableOfTask);

        Assertions.assertEquals(executableOfTask, task.getJavaLauncher().get().getExecutablePath().toString(),
                "java launcher of backend builder should not override executable of task");
    }

    private static @NotNull JavaLauncher createLauncherWithExecutable(Project project, String executable) {
        final JavaLauncher javaLauncherOfBackendBuilder = mock(JavaLauncher.class, "java launcher of backend builder");
        when(javaLauncherOfBackendBuilder.getExecutablePath()).thenReturn(project.getLayout().getProjectDirectory().file(executable));
        return javaLauncherOfBackendBuilder;
    }
}
