package de.itemis.mps.gradle.launcher;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.JavaExec;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JvmVendorSpec;
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.process.JavaExecSpec;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.util.Collections;

public class MpsBackendBuilder {
    private final JavaToolchainService javaToolchainService;
    private final ProjectLayout layout;

    private final DirectoryProperty mpsHome;
    private final Property<String> mpsVersion;
    private final Property<JvmVendorSpec> jvmVendorSpec;

    private File temporaryDirectory;

    @Inject
    public MpsBackendBuilder(JavaToolchainService javaToolchainService, ProjectLayout layout, ProviderFactory providers, ObjectFactory objects) {
        this.javaToolchainService = javaToolchainService;
        this.layout = layout;

        mpsHome = objects.directoryProperty();
        mpsVersion = objects.property(String.class).convention(MpsVersionDetection.fromMpsHome(layout, providers, mpsHome.getAsFile()));
        jvmVendorSpec = objects.property(JvmVendorSpec.class).convention(DefaultJvmVendorSpec.any());
    }

    public MpsBackendBuilder withMpsHome(File mpsHome) {
        this.mpsHome.set(mpsHome);
        return this;
    }

    public MpsBackendBuilder withMpsHome(Provider<File> mpsHome) {
        this.mpsHome.set(layout.dir(mpsHome));
        return this;
    }

    public MpsBackendBuilder withMpsVersion(String mpsVersion) {
        this.mpsVersion.set(mpsVersion);
        return this;
    }

    public MpsBackendBuilder withMpsVersion(Provider<String> mpsVersion) {
        this.mpsVersion.set(mpsVersion);
        return this;
    }

    public MpsBackendBuilder withJetBrainsJvm() {
        return withJvmVendorSpec(JvmVendorSpec.matching("JetBrains"));
    }

    public MpsBackendBuilder withJvmVendorSpec(JvmVendorSpec jvmVendorSpec) {
        this.jvmVendorSpec.set(jvmVendorSpec);
        return this;
    }

    public void configure(JavaExecSpec javaExec) {
        if (!mpsHome.isPresent()) {
            throw new IllegalStateException("mpsHome was not set");
        }
        if (!mpsVersion.isPresent()) {
            throw new IllegalStateException("mpsVersion was not set or could not be detected");
        }

        configureLauncher(javaExec);
        configureJna(javaExec);
        configureOpens(javaExec);
        configureWorkspace(javaExec);
    }

    private void configureLauncher(JavaExecSpec javaExec) {
        Provider<JavaLauncher> launcher = mpsVersion.flatMap((mpsVersionValue) ->
                javaToolchainService.launcherFor((spec) -> {
                    spec.getVendor().set(jvmVendorSpec);
                    spec.getLanguageVersion().set(JavaLanguageVersion.of(mpsVersionValue.compareTo("2022") < 0 ? 11 : 17));
                }));

        if (javaExec instanceof JavaExec) {
            ((JavaExec) javaExec).getJavaLauncher().set(launcher);
        } else {
            javaExec.setExecutable(launcher.get().getExecutablePath());
        }
    }

    private void configureJna(JavaExecSpec javaExec) {
        // Gradle needs this to be an inner class rather than a lambda so that it can be properly cached.
        //noinspection Convert2Lambda
        javaExec.getJvmArgumentProviders().add(new CommandLineArgumentProvider() {
            @Override
            public Iterable<String> asArguments() {
                if (mpsVersion.get().compareTo("2022.3") >= 0) {
                    return Collections.singleton("-Djna.boot.library.path=" + mpsHome.get().file("lib/jna/" + System.getProperty("os.arch")).getAsFile());
                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    private static void configureOpens(JavaExecSpec javaExec) {
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
    }

    private void configureWorkspace(JavaExecSpec javaExec) {
        File tmpDir = getTemporaryDirectory(javaExec);

        // MPS versions up to and including 2021.x create logs under their working directory so set it to a temporary
        // directory to avoid polluting the checkout directory or MPS home.
        javaExec.setWorkingDir(tmpDir);
        javaExec.systemProperty("idea.config.path", new File(tmpDir, "config"));
        javaExec.systemProperty("idea.system.path", new File(tmpDir, "system"));
    }

    @Nonnull
    private File getTemporaryDirectory(JavaExecSpec spec) {
        if (temporaryDirectory != null) {
            // Explicitly set directory overrides heuristics
            return temporaryDirectory;
        }

        if (spec instanceof JavaExec) {
            return ((JavaExec) spec).getTemporaryDir();
        }

        throw new IllegalStateException("Temporary directory for MPS should be specified");
    }

    public MpsBackendBuilder withTemporaryDirectory(File temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
        return this;
    }
}
