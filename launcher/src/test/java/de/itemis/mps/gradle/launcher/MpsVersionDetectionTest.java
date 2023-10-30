package de.itemis.mps.gradle.launcher;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MpsVersionDetectionTest {

    @Test
    public void readsVersionFromBuildProperties(@TempDir File tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        final MpsVersionDetection detector = project.getObjects().newInstance(MpsVersionDetection.class);

        File mpsDir = new File(tempDir, "build/mps");
        File buildProperties = new File(mpsDir, "build.properties");

        FileUtils.writeStringToFile(buildProperties, "mps.build.number=MPS-213.7172.1079\n",
                StandardCharsets.ISO_8859_1);

        assertEquals("2021.3", MpsVersionDetection.fromMpsHome(project.getLayout(), project.getProviders(),
                project.provider(() -> mpsDir)).get());
    }

}
