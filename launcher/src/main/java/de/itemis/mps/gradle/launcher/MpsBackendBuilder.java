package de.itemis.mps.gradle.launcher;

import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.internal.provider.PropertyFactory;
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
import org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.process.JavaExecSpec;
import org.gradle.process.JavaForkOptions;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;

public class MpsBackendBuilder {
    private final PropertyFactory propertyFactory;
    private final JavaToolchainService javaToolchainService;
    private final ProjectLayout layout;

    private final DirectoryProperty mpsHome;
    private final Property<String> mpsVersion;
    private final Property<JvmVendorSpec> jvmVendorSpec;
    private final Property<JavaLauncher> javaLauncher;

    private File temporaryDirectory;

    @Inject
    public MpsBackendBuilder(JavaToolchainService javaToolchainService, ProjectLayout layout, ProviderFactory providers, ObjectFactory objects, PropertyFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
        this.javaToolchainService = javaToolchainService;
        this.layout = layout;

        mpsHome = objects.directoryProperty();
        mpsVersion = objects.property(String.class).convention(MpsVersionDetection.fromMpsHome(layout, providers, mpsHome.getAsFile()));
        jvmVendorSpec = objects.property(JvmVendorSpec.class).convention(DefaultJvmVendorSpec.any());
        Provider<JavaLauncher> javaLauncherConvention = javaToolchainService.launcherFor(spec -> {
            spec.getVendor().set(jvmVendorSpec);
            spec.getLanguageVersion().set(
                    mpsVersion.map(v -> JavaLanguageVersion.of(v.compareTo("2022") < 0 ? 11 : 17)));
        });
        javaLauncher = objects.property(JavaLauncher.class).convention(javaLauncherConvention);
    }

    public MpsBackendBuilder withMpsHome(File mpsHome) {
        this.mpsHome.set(mpsHome);
        return this;
    }

    public MpsBackendBuilder withMpsHome(Provider<File> mpsHome) {
        this.mpsHome.set(layout.dir(mpsHome));
        return this;
    }

    public MpsBackendBuilder withMpsHomeDirectory(Provider<Directory> mpsHome) {
        this.mpsHome.set(mpsHome);
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

    public MpsBackendBuilder withJavaExecutable(Provider<String> javaExecutable) {
        this.javaLauncher.set(javaExecutable
                .map(path -> SpecificInstallationToolchainSpec.fromJavaExecutable(propertyFactory, path))
                .flatMap(javaToolchainService::launcherFor));
        return this;
    }

    public MpsBackendBuilder withJavaLauncher(Provider<JavaLauncher> javaLauncher) {
        this.javaLauncher.set(javaLauncher);
        return this;
    }

    public MpsBackendBuilder withJavaLauncher(JavaLauncher javaLauncher) {
        this.javaLauncher.set(javaLauncher);
        return this;
    }

    public void configure(JavaExecSpec javaExec) {
        configure((JavaForkOptions) javaExec);
    }

    public void configure(JavaForkOptions options) {
        configureJavaExecutableOrLauncher(options);
        configureVersionSpecificProperties(options);
        configureOpens(options);
        configureWorkspace(options);
    }

    private static class LazyToString {
        private final Provider<String> provider;

        public LazyToString(Provider<String> provider) {
            this.provider = provider;
        }

        @Override
        public String toString() {
            return provider.get();
        }
    }

    private void configureJavaExecutableOrLauncher(JavaForkOptions options) {
        if (options instanceof JavaExec) {
            ((JavaExec) options).getJavaLauncher().set(javaLauncher);
        } else {
            options.setExecutable(new LazyToString(javaLauncher.map(l -> l.getExecutablePath().toString())));
        }
    }

    private void configureVersionSpecificProperties(JavaForkOptions options) {
        // Gradle needs this to be an inner class rather than a lambda so that it can be properly cached.
        //noinspection Convert2Lambda
        options.getJvmArgumentProviders().add(new CommandLineArgumentProvider() {
            @Override
            public Iterable<String> asArguments() {
                ArrayList<String> result = new ArrayList<>();
                if (mpsVersion.get().compareTo("2023.3") >= 0) {
                    result.add("-Dintellij.platform.load.app.info.from.resources=true");
                }
                if (mpsVersion.get().compareTo("2022.3") >= 0) {
                    result.add("-Djna.boot.library.path=" + mpsHome.get().file("lib/jna/" + System.getProperty("os.arch")).getAsFile());
                }
                return result;
            }
        });
    }

    private static void configureOpens(JavaForkOptions options) {
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
            options.jvmArgs("--add-opens=" + module + "=ALL-UNNAMED");
        }
    }

    private void configureWorkspace(JavaForkOptions options) {
        File tmpDir = getTemporaryDirectory(options);

        // MPS versions up to and including 2021.x create logs under their working directory so set it to a temporary
        // directory to avoid polluting the checkout directory or MPS home.
        options.setWorkingDir(tmpDir);
        options.systemProperty("idea.config.path", new File(tmpDir, "config"));
        options.systemProperty("idea.system.path", new File(tmpDir, "system"));
    }

    @Nonnull
    private File getTemporaryDirectory(JavaForkOptions options) {
        if (temporaryDirectory != null) {
            // Explicitly set directory overrides heuristics
            return temporaryDirectory;
        }

        if (options instanceof Task) {
            return ((Task) options).getTemporaryDir();
        }

        throw new IllegalStateException("Temporary directory for MPS should be specified");
    }

    public MpsBackendBuilder withTemporaryDirectory(File temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
        return this;
    }
}
