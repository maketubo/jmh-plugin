package com.github.jmh;

import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName JmhExternalLibraryDescriptor
 * @description
 * @date 2020/8/9 23:23
 * @since JDK 1.8
 */
public class JmhExternalLibraryDescriptor extends ExternalLibraryDescriptor {

    public static final ExternalLibraryDescriptor JMH_CORE = new
            JmhExternalLibraryDescriptor("org.openjdk.jmh",
            "jmh-core", "1.21", "1.21");

    public static final ExternalLibraryDescriptor JMH_ANNO = new
            JmhExternalLibraryDescriptor("org.openjdk.jmh",
            "jmh-generator-annprocess", "1.21", "1.21");
    public JmhExternalLibraryDescriptor(@NotNull String libraryGroupId, @NotNull String libraryArtifactId, @Nullable String minVersion, @Nullable String maxVersion) {
        super(libraryGroupId, libraryArtifactId, minVersion, maxVersion);
    }
}
