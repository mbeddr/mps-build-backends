package de.itemis.mps.gradle.launcher;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;

import javax.inject.Inject;
import java.io.File;

public class MpsBackendLauncher {

    private final ObjectFactory objects;

    @Inject
    public MpsBackendLauncher(ObjectFactory objects) {
        this.objects = objects;
    }

    public MpsBackendBuilder builder() {
        return objects.newInstance(MpsBackendBuilder.class);
    }

    public MpsBackendBuilder forMpsHome(File mpsHome) {
        return objects.newInstance(MpsBackendBuilder.class).withMpsHome(mpsHome);
    }

    public MpsBackendBuilder forMpsHome(Provider<File> mpsHome) {
        return objects.newInstance(MpsBackendBuilder.class).withMpsHome(mpsHome);
    }

}
