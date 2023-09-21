package de.itemis.mps.gradle;

import de.itemis.mps.gradle.launcher.MpsBackendLauncher;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class LauncherPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("mpsBackendLauncher", MpsBackendLauncher.class);
    }
}
