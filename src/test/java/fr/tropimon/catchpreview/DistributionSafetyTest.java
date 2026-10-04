package fr.tropimon.catchpreview;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DistributionSafetyTest {
    private static final List<String> REMOVED_RUNTIME = List.of(
            "TropimonSelfUpdater", "TropimonUpdateInstaller", "UpdateScreen",
            "tropimonupdates", "tropimon-consent-updater:", "api.github.com",
            "api.modrinth.com", "java/net/http/", "java/lang/ProcessBuilder",
            "java/lang/ProcessHandle", "installer.jar", "install.properties");

    private static void assertNoUpdater(String name, byte[] content) {
        String constants = name + new String(content, StandardCharsets.ISO_8859_1);
        for (String forbidden : REMOVED_RUNTIME) {
            assertFalse(constants.contains(forbidden), name + ": forbidden update runtime " + forbidden);
        }
    }

    @Test void compiledRuntimeCannotCheckDownloadOrLaunchAnInstaller() throws Exception {
        try (var paths = Files.walk(Path.of("build/classes/java/main"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                assertNoUpdater(path.getFileName().toString(), Files.readAllBytes(path));
            }
        }
    }

    @Test void finalJarContainsNoUpdaterOrPrivateDeliveryTools() throws Exception {
        try (var jar = new JarFile(System.getProperty("catchpreview.finalJar"))) {
            var entries = jar.entries();
            int classes = 0;
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                assertFalse(name.endsWith(".ps1") || name.endsWith(".jar") || name.startsWith("tools/"), name);
                try (var input = jar.getInputStream(entry)) {
                    assertNoUpdater(name, input.readAllBytes());
                }
                if (name.endsWith(".class")) classes++;
            }
            assertTrue(classes > 20, "Inspect the actual compiled mod, not an empty fixture");
            assertNotNull(jar.getEntry("fr/tropimon/catchpreview/mixin/PreviewHudLayerMixin.class"));
            assertNotNull(jar.getEntry("tropimon_catch_preview.mixins.json"));
        }
    }

    @Test void finalJarContainsRenamedClassesWithoutObsoleteCopies() throws Exception {
        String[][] renames = {
                {"CatchPreviewState", "preview/CatchPreviewController"},
                {"CaptureHistory", "capture/KnownPokemonHistory"},
                {"ConfirmationQueue", "preview/PreviewState"},
                {"RevisionCache", "release/SingleEntryCache"},
                {"StorageTransfers", "capture/StorageTransferObserver"},
                {"PreviewMarks", "ui/PreviewMarksRenderer"},
                {"WindowPosition", "ui/DraggableWindowPosition"},
                {"PokemonDetails", "preview/PokemonDetailsReader"}
        };
        try (var jar = new JarFile(System.getProperty("catchpreview.finalJar"))) {
            for (String[] rename : renames) {
                assertNull(jar.getEntry("fr/tropimon/catchpreview/" + rename[0] + ".class"));
                assertNotNull(jar.getEntry("fr/tropimon/catchpreview/" + rename[1] + ".class"));
            }
        }
    }

    @Test void finalJarUsesResponsibilityPackagesWithoutRootDuplicates() throws Exception {
        String[] paths = {
                "capture/KnownPokemonHistory", "capture/StorageTransferObserver",
                "preview/CatchPreviewController", "preview/PreviewState", "preview/PokemonDetailsReader",
                "ui/CatchPreviewRenderer", "ui/PokemonPortraitRenderer", "ui/PreviewMarksRenderer",
                "ui/PreviewPosition", "ui/DraggableWindowPosition",
                "release/PokemonReleaseController", "release/ReleaseRules", "release/SingleEntryCache"
        };
        try (var jar = new JarFile(System.getProperty("catchpreview.finalJar"))) {
            for (String path : paths) {
                assertNotNull(jar.getEntry("fr/tropimon/catchpreview/" + path + ".class"));
                String simpleName = path.substring(path.indexOf('/') + 1);
                assertNull(jar.getEntry("fr/tropimon/catchpreview/" + simpleName + ".class"));
            }
            assertNotNull(jar.getEntry("fr/tropimon/catchpreview/TropimonCatchPreviewClient.class"));
        }
    }

    @Test void guardDetectsSyntheticReintroductions() {
        for (String forbidden : REMOVED_RUNTIME) {
            assertThrows(AssertionError.class, () -> assertNoUpdater("Example.class",
                    forbidden.getBytes(StandardCharsets.ISO_8859_1)));
        }
        assertDoesNotThrow(() -> assertNoUpdater("Example.class", "By FastedCorsi".getBytes(StandardCharsets.UTF_8)));
    }
}
