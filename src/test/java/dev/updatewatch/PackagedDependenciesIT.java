package dev.updatewatch;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Runs after shading, against the installable JAR rather than Maven's dependency classpath. */
class PackagedDependenciesIT {
    @Test void packagedDependenciesAreRelocatedAndBstatsPassesItsOwnCheck() throws Exception {
        var path = Path.of(System.getProperty("packagedJar"));
        try (var jar = new JarFile(path.toFile())) {
            var build = new java.util.Properties();
            try (var in = jar.getInputStream(jar.getJarEntry("build.properties"))) { build.load(in); }
            assertTrue(build.getProperty("version").matches("[0-9]+(?:\\.[0-9]+)+(?:[-+][A-Za-z0-9.-]+)?"));
            for (String original : new String[]{"org/bstats/", "org/jsoup/", "com/google/gson/"})
                assertFalse(jar.stream().anyMatch(e -> e.getName().startsWith(original) && e.getName().endsWith(".class")), original);
            for (String relocated : new String[]{"bstats/MetricsBase", "jsoup/Jsoup", "gson/Gson"})
                assertNotNull(jar.getEntry("dev/updatewatch/lib/" + relocated + ".class"), relocated);
        }
        try (var loader = new URLClassLoader(new java.net.URL[]{path.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var type = loader.loadClass("dev.updatewatch.lib.bstats.MetricsBase");
            // Reporting disabled, relocation check enabled. No metrics request or reporting task is started.
            var client = type.getConstructors()[0].newInstance("bukkit", "packaging-test", 34400, false,
                    null, null, null, null, null, null, false, false, false, false);
            type.getMethod("shutdown").invoke(client);
        }
    }
}
