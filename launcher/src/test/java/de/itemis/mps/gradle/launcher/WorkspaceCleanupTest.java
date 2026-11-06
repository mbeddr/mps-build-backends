package de.itemis.mps.gradle.launcher;

import de.itemis.mps.gradle.LauncherPlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.process.JavaForkOptions;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class WorkspaceCleanupTest {

    @Test
    public void taskDeletesConfigAndSystemDirectoriesBeforeExecution(@TempDir File tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(new File(tempDir, "project")).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final JavaExec task = project.getTasks().create("javaExec", JavaExec.class);
        final File mpsTmpDir = new File(tempDir, "mps-tmp");
        project.getExtensions().getByType(MpsBackendLauncher.class).builder()
                .withTemporaryDirectory(mpsTmpDir)
                .configure(task);

        final File configDir = new File(mpsTmpDir, "config");
        final File systemDir = new File(mpsTmpDir, "system");
        createFile(new File(configDir, "options/stale.xml"));
        createFile(new File(systemDir, "caches/stale.dat"));
        final File otherFile = createFile(new File(mpsTmpDir, "other/keep.txt"));

        runActionsBeforeTaskAction(task);

        Assertions.assertFalse(configDir.exists(), "config directory should be deleted");
        Assertions.assertFalse(systemDir.exists(), "system directory should be deleted");
        Assertions.assertTrue(otherFile.exists(), "other files in the temporary directory should be kept");
    }

    @Test
    public void nonTaskOptionsDeleteConfigAndSystemDirectoriesWhenConfigured(@TempDir File tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(new File(tempDir, "project")).build();
        project.getPluginManager().apply(LauncherPlugin.class);

        final File mpsTmpDir = new File(tempDir, "mps-tmp");
        final File configDir = new File(mpsTmpDir, "config");
        final File systemDir = new File(mpsTmpDir, "system");
        createFile(new File(configDir, "options/stale.xml"));
        createFile(new File(systemDir, "caches/stale.dat"));
        final File otherFile = createFile(new File(mpsTmpDir, "other/keep.txt"));

        final JavaForkOptions options = mock(JavaForkOptions.class, RETURNS_DEEP_STUBS);
        project.getExtensions().getByType(MpsBackendLauncher.class).builder()
                .withTemporaryDirectory(mpsTmpDir)
                .configure(options);

        Assertions.assertFalse(configDir.exists(), "config directory should be deleted");
        Assertions.assertFalse(systemDir.exists(), "system directory should be deleted");
        Assertions.assertTrue(otherFile.exists(), "other files in the temporary directory should be kept");
    }

    /**
     * Runs the actions added before the task's own action (such as {@code doFirst} actions), without starting the
     * forked JVM. In a fresh task, the task's own action is the last one.
     */
    private static void runActionsBeforeTaskAction(Task task) {
        final List<Action<? super Task>> actions = task.getActions();
        for (Action<? super Task> action : actions.subList(0, actions.size() - 1)) {
            action.execute(task);
        }
    }

    private static File createFile(File file) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.writeString(file.toPath(), "stale");
        return file;
    }
}
