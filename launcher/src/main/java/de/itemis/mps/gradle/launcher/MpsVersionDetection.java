package de.itemis.mps.gradle.launcher;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileContents;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class MpsVersionDetection {
    /**
     * Retrieves the MPS platform version from `$mpsHome/build.properties`, property `mps.build.number`.
     */
    public static Provider<String> fromMpsHome(ProjectLayout layout, ProviderFactory providers, Provider<File> mpsHome) {
        Provider<RegularFile> buildPropertiesProvider = layout.file(mpsHome.map((File it) -> new File(it, "build.properties")));
        return buildPropertiesProvider.map(buildPropertiesFile -> {
            FileContents fileContents = providers.fileContents(buildPropertiesFile);
            final String contents = fileContents.getAsText().get();

            Properties properties = new Properties();
            try (StringReader reader = new StringReader(contents)) {
                properties.load(reader);
            } catch (IOException io) {
                throw new GradleException("Error loading properties from file " + buildPropertiesFile, io);
            }

            String buildNumber = properties.getProperty("mps.build.number");
            if (buildNumber == null) {
                throw new GradleException("Could not read mps.build.number property from file " + buildPropertiesFile.getAsFile());
            }

            return buildNumberToVersion(buildNumber);
        });
    }

    private static String buildNumberToVersion(String buildNumber) {
        return buildNumber.replaceFirst("^(?:.*-)?(\\d{2})(\\d)\\..*", "20$1.$2");
    }
}
