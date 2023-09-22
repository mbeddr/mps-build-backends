package de.itemis.mps.gradle.launcher;

import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileContents;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.JavaExec;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JvmVendorSpec;
import org.gradle.process.CommandLineArgumentProvider;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Properties;

public class MpsBackendLauncher {

    private final JavaToolchainService javaToolchainService;
    private final ProviderFactory providers;

    @Inject
    public MpsBackendLauncher(JavaToolchainService javaToolchainService, ProviderFactory providers) {
        this.javaToolchainService = javaToolchainService;
        this.providers = providers;
    }

    public void configureJavaForMpsVersion(JavaExec javaExec, File mpsHome, String mpsVersion) {
        configureJavaForMpsVersion(javaExec,
                providers.provider(() -> mpsHome),
                providers.provider(() -> mpsVersion));
    }


    /**
     * Retrieves the MPS platform version from `$mpsHome/build.properties`, property `mps.build.number`.
     */
    public Provider<String> mpsVersionFromMpsHome(Provider<Directory> mpsHome) {
        Provider<RegularFile> buildPropertiesFile = mpsHome.map((Directory it) -> it.file("build.properties"));
        return buildPropertiesFile.map(file -> {
            FileContents fileContents = providers.fileContents(buildPropertiesFile);
            final String contents = fileContents.getAsText().get();

            Properties properties = new Properties();
            try (StringReader reader = new StringReader(contents)) {
                properties.load(reader);
            } catch (IOException io) {
                throw new GradleException("Error loading properties from file " + file, io);
            }

            String fullNumber = properties.getProperty("mps.build.number");
            if (fullNumber == null) {
                throw new GradleException("Could not read mps.build.number property from file " + buildPropertiesFile.get().getAsFile());
            }

            int dash = fullNumber.indexOf("-");
            return fullNumber.substring(dash + 1);
        });
    }

    public void configureJavaForMpsVersion(JavaExec javaExec, Provider<File> mpsHome, Provider<String> mpsVersion) {
        Provider<JavaLauncher> launcher = mpsVersion.flatMap((mpsVersionValue) ->
                javaToolchainService.launcherFor((spec) -> {
                    spec.getVendor().set(JvmVendorSpec.matching("JetBrains"));
                    spec.getLanguageVersion().set(JavaLanguageVersion.of(mpsVersionValue.compareTo("2022") < 0 ? 11 : 17));
                }));

        javaExec.getJavaLauncher().set(launcher);

        // Gradle needs this to be an inner class rather than a lambda so that it can be properly cached.
        //noinspection Convert2Lambda
        javaExec.getJvmArgumentProviders().add(new CommandLineArgumentProvider() {
            @Override
            public Iterable<String> asArguments() {
                if (mpsVersion.get().compareTo("2022.3") >= 0) {
                    return Collections.singleton("-Djna.boot.library.path=" + new File(mpsHome.get(), "lib/jna/" + System.getProperty("os.arch")).getPath());
                } else {
                    return Collections.emptyList();
                }
            }
        });

        final String[] modules = new String[]{
                "java.base/java.io",
                "java.base/java.lang",
                "java.base/java.lang.reflect",
                "java.base/java.net",
                "java.base/java.nio",
                "java.base/java.nio.charset",
                "java.base/java.text",
                "java.base/java.time",
                "java.base/java.util",
                "java.base/java.util.concurrent",
                "java.base/java.util.concurrent.atomic",
                "java.base/jdk.internal.vm",
                "java.base/sun.nio.ch",
                "java.base/sun.nio.fs",
                "java.base/sun.security.ssl",
                "java.base/sun.security.util",
                "java.desktop/java.awt",
                "java.desktop/java.awt.dnd.peer",
                "java.desktop/java.awt.event",
                "java.desktop/java.awt.image",
                "java.desktop/java.awt.peer",
                "java.desktop/javax.swing",
                "java.desktop/javax.swing.plaf.basic",
                "java.desktop/javax.swing.text.html",
                "java.desktop/sun.awt.datatransfer",
                "java.desktop/sun.awt.image",
                "java.desktop/sun.awt",
                "java.desktop/sun.font",
                "java.desktop/sun.java2d",
                "java.desktop/sun.swing",
                "jdk.attach/sun.tools.attach",
                "jdk.compiler/com.sun.tools.javac.api",
                "jdk.internal.jvmstat/sun.jvmstat.monitor",
                "jdk.jdi/com.sun.tools.jdi",
                "java.desktop/sun.lwawt",
                "java.desktop/sun.lwawt.macosx",
                "java.desktop/com.apple.laf",
                "java.desktop/com.apple.eawt",
                "java.desktop/com.apple.eawt.event"
        };

        for (String module : modules) {
            javaExec.jvmArgs("--add-opens=" + module + "=ALL-UNNAMED");
        }

        // MPS versions up to and including 2021.x create logs under their working directory so set it to a temporary
        // directory to avoid polluting the checkout directory or MPS home.
        javaExec.setWorkingDir(javaExec.getTemporaryDir());
        javaExec.systemProperty("idea.config.path", new File(javaExec.getTemporaryDir(), "config"));
        javaExec.systemProperty("idea.system.path", new File(javaExec.getTemporaryDir(), "system"));
    }
}
