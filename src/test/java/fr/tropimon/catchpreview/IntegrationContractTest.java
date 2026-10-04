package fr.tropimon.catchpreview;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Bytecode checks without loading another mod or booting Minecraft in the test JVM. */
class IntegrationContractTest {
    @Test void activeCobblemonExposesAlphaAndSizeDisplayedByPreview() throws Exception {
        try (var jar = new JarFile(System.getProperty("catchpreview.cobblemonJar"))) {
            ClassNode pokemon;
            try (var input = jar.getInputStream(jar.getJarEntry("com/cobblemon/mod/common/pokemon/Pokemon.class"))) {
                pokemon = node(input.readAllBytes());
            }
            assertTrue(pokemon.methods.stream().anyMatch(method -> method.name.equals("isAlpha")
                    && method.desc.equals("()Z")), "Alpha must come from the official Pokemon API");
            assertTrue(pokemon.methods.stream().anyMatch(method -> method.name.equals("getSizeCategory")
                    && method.desc.equals("()Lcom/cobblemon/mod/common/pokemon/PokemonSizeCategory;")));
            try (var input = jar.getInputStream(jar.getJarEntry("com/cobblemon/mod/common/pokemon/PokemonSizeCategory.class"))) {
                assertEquals("java/lang/Enum", node(input.readAllBytes()).superName,
                        "The optional adapter expects an official enum, not invented size values");
            }
        }
    }

    @Test void officialStorageTransfersAreRememberedBeforePacketsLeaveClient() throws Exception {
        String registered = Files.readString(Path.of("src/main/resources/tropimon_catch_preview.mixins.json"));
        assertTrue(registered.contains("\"StorageTransferMixin\""));
        assertTrue(registered.contains("\"SetPCBoxHandlerMixin\""));
        var mixin = node(Files.readAllBytes(Path.of(
                "build/classes/java/main/fr/tropimon/catchpreview/mixin/StorageTransferMixin.class")));
        var hook = mixin.methods.stream().filter(method -> method.name.endsWith("$transfer")).findFirst().orElseThrow();
        var injection = hook.visibleAnnotations.stream().filter(annotation -> annotation.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") var points = (List<AnnotationNode>) value(injection, "at");
        assertEquals("HEAD", value(points.getFirst(), "value"));
        assertEquals(List.of("sendToServer()V"), value(injection, "method"));
        assertTrue(java.util.stream.StreamSupport.stream(hook.instructions.spliterator(), false)
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals("fr/tropimon/catchpreview/capture/StorageTransferObserver") && call.name.equals("sending")));
    }

    @Test void officialTradeOfferIsRememberedBeforeStorageAndMixinIsRegistered() throws Exception {
        String registered = Files.readString(Path.of("src/main/resources/tropimon_catch_preview.mixins.json"));
        assertTrue(registered.contains("\"TradeUpdatedHandlerMixin\""));
        var mixin = node(Files.readAllBytes(Path.of(
                "build/classes/java/main/fr/tropimon/catchpreview/mixin/TradeUpdatedHandlerMixin.class")));
        var hook = mixin.methods.stream().filter(m -> m.name.endsWith("$offered")).findFirst().orElseThrow();
        var inject = hook.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") var points = (List<AnnotationNode>) value(inject, "at");
        assertEquals("HEAD", value(points.getFirst(), "value"));
        var calls = new ArrayList<String>();
        for (var instruction : hook.instructions) if (instruction instanceof MethodInsnNode call) calls.add(call.name);
        assertTrue(calls.contains("getPokemon"));
        assertTrue(calls.indexOf("getUuid") < calls.indexOf("remember"));
        assertFalse(calls.contains("close"), "Do not close a concurrent real capture or release confirmation");
        var state = node(Files.readAllBytes(Path.of("build/classes/java/main/fr/tropimon/catchpreview/preview/CatchPreviewController.class")));
        var check = state.methods.stream().filter(m -> m.name.equals("alreadyStored")).findFirst().orElseThrow();
        var owners = new HashSet<String>();
        for (var instruction : check.instructions) if (instruction instanceof MethodInsnNode call
                && call.name.equals("findByUUID")) owners.add(call.owner);
        assertTrue(owners.contains("com/cobblemon/mod/common/client/storage/ClientParty"));
        assertTrue(owners.contains("com/cobblemon/mod/common/client/storage/ClientPC"));
    }

    @Test void installedTeamBuilderUsesCoveredOfficialTransferPackets() throws Exception {
        String directory = System.getProperty("catchpreview.coexistenceMods");
        org.junit.jupiter.api.Assumptions.assumeTrue(directory != null, "Optional installed-mod compatibility audit");
        Path builder;
        try (var files = Files.list(Path.of(directory))) {
            builder = files.filter(p -> p.getFileName().toString().startsWith("TropimonTeamBuilder-")
                    && p.toString().endsWith(".jar")).findFirst().orElseThrow();
        }
        var mixin = node(Files.readAllBytes(Path.of("build/classes/java/main/fr/tropimon/catchpreview/mixin/StorageTransferMixin.class")));
        var annotation = mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") var targets = (List<Type>) value(annotation, "value");
        var covered = targets.stream().map(Type::getInternalName).collect(java.util.stream.Collectors.toSet());
        int count = 0;
        // Read-only bytecode audit, not a runtime dependency or reflective access.
        try (var jar = new JarFile(builder.toFile())) {
            for (var entry : Collections.list(jar.entries())) {
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode n;
                try (var in = jar.getInputStream(entry)) { n = node(in.readAllBytes()); }
                for (var method : n.methods) for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.name.equals("sendToServer")
                            && call.owner.startsWith("com/cobblemon/mod/common/net/messages/server/storage/")
                            && (call.owner.contains("Move") || call.owner.contains("Swap"))) {
                        assertTrue(covered.contains(call.owner), call.owner);
                        count++;
                    }
                }
            }
        }
        assertTrue(count >= 5, "Verify the real integration, not just a fixture");
    }

    private static ClassNode node(byte[] bytes) {
        var n = new ClassNode(); new ClassReader(bytes).accept(n, 0); return n;
    }
    private static Object value(AnnotationNode a, String key) {
        for (int i = 0; i < a.values.size(); i += 2) if (a.values.get(i).equals(key)) return a.values.get(i + 1);
        return null;
    }
    @Test void compactMarkIconFitsLeftOfLevelBelowHeader() throws Exception {
        var preview = node(Files.readAllBytes(Path.of("build/classes/java/main/fr/tropimon/catchpreview/ui/PreviewMarksRenderer.class")));
        var constants = new HashMap<String, Integer>();
        for (var field : preview.fields) if (field.value instanceof Integer number) constants.put(field.name, number);
        int x = constants.get("OFFSET_X"), y = constants.get("OFFSET_Y"), size = constants.get("SIZE");
        assertEquals(8, size);
        assertEquals(49, x);
        assertEquals(23, y);
        assertTrue(x + size < 61, "Leave a gap before the level");
        assertTrue(y > 20 && y + size < 34, "Keep clear of the header and nature row");
        assertTrue(x > 12 + 6, "Keep clear of the shiny star");
    }
    @Test void markTextAccessorsSelectTranslatedOverloadsAndUseOnlyOwnedMarks() throws Exception {
        var accessor = node(Files.readAllBytes(Path.of(
                "build/classes/java/main/fr/tropimon/catchpreview/mixin/MarkTextAccessor.class")));
        try (var jar = new JarFile(System.getProperty("catchpreview.cobblemonJar"));
             var in = jar.getInputStream(jar.getJarEntry("com/cobblemon/mod/common/api/mark/Mark.class"))) {
            var mark = node(in.readAllBytes());
            int checked = 0;
            for (var method : accessor.methods) {
                if (method.visibleAnnotations == null) continue;
                for (var annotation : method.visibleAnnotations) {
                    if (!annotation.desc.endsWith("/Invoker;")) continue;
                    String name = (String) value(annotation, "value");
                    String descriptor = method.desc.replace("Lnet/minecraft/text/MutableText;", "Lnet/minecraft/class_5250;");
                    assertTrue(mark.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)));
                    checked++;
                }
            }
            assertEquals(2, checked);
        }
        var preview = node(Files.readAllBytes(Path.of("build/classes/java/main/fr/tropimon/catchpreview/ui/PreviewMarksRenderer.class")));
        boolean owned = false, potential = false;
        for (var method : preview.methods) for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("getMarks")) owned = true;
                if (call.name.equals("getPotentialMarks")) potential = true;
            }
        }
        assertTrue(owned);
        assertFalse(potential, "Never display an unearned potential mark");
    }
    @Test void everyCobblemonInjectionTargetExistsInOfficialJar() throws Exception {
        int checked = 0;
        try (var jar = new JarFile(System.getProperty("catchpreview.cobblemonJar"));
             var files = Files.list(Path.of("build/classes/java/main/fr/tropimon/catchpreview/mixin"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                var mixin = node(Files.readAllBytes(file));
                var annotation = mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                @SuppressWarnings("unchecked") var targets = (List<Type>) value(annotation, "value");
                for (Type type : targets) {
                    if (!type.getInternalName().startsWith("com/cobblemon/")) continue;
                    var entry = jar.getJarEntry(type.getInternalName() + ".class");
                    assertNotNull(entry, type.getClassName());
                    ClassNode target;
                    try (var in = jar.getInputStream(entry)) { target = node(in.readAllBytes()); }
                    for (var method : mixin.methods) {
                        if (method.visibleAnnotations == null) continue;
                        for (var inject : method.visibleAnnotations) {
                            if (!inject.desc.endsWith("/Inject;")) continue;
                            @SuppressWarnings("unchecked") var names = (List<String>) value(inject, "method");
                            for (String name : names) {
                                String normalized = name.replace("Lnet/minecraft/client/MinecraftClient;", "Lnet/minecraft/class_310;");
                                assertTrue(target.methods.stream().anyMatch(m -> normalized.equals(m.name)
                                        || normalized.equals(m.name + m.desc)), type.getClassName() + "." + normalized);
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked >= 20, "All hooks must actually be inspected, checked=" + checked);
    }
    @Test void compiledModHasNoOtherTropimonPackageOrResourceDependency() throws Exception {
        try (var files = Files.walk(Path.of("build/classes/java/main"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                var references = java.util.regex.Pattern.compile("fr/tropimon/(?!catchpreview/)[A-Za-z0-9_/]+").matcher(bytes);
                assertFalse(references.find(), file.toString());
                assertFalse(bytes.contains("tropimodclient"), file.toString());
                boolean portraitCompatibilityAdapter = file.getFileName().toString()
                        .equals("PokemonPortraitRenderer.class");
                if (portraitCompatibilityAdapter) {
                    assertTrue(bytes.contains("com.cobblemon.mod.common.client.gui.ProfileTransformType"),
                            file.toString());
                } else if (file.getFileName().toString().equals("PokemonDetailsReader.class")) {
                    assertTrue(bytes.contains("isAlpha"));
                    assertTrue(bytes.contains("getSizeCategory"));
                    assertTrue(bytes.contains("java/lang/NoSuchMethodException"));
                    assertFalse(bytes.contains("forName"));
                } else {
                    assertFalse(bytes.contains("java/lang/reflect"), file.toString());
                    assertFalse(bytes.contains("forName"), file.toString());
                }
            }
        }
        String metadata = Files.readString(Path.of("build/resources/main/fabric.mod.json"));
        assertFalse(metadata.contains("tropimodclient"));
        assertTrue(Files.size(Path.of("build/resources/main/assets/tropimon_catch_preview/textures/gui/frame.png")) > 0);
    }
    @Test void confirmationAndDelayedPcResponseBypassLocationAndProximityCaches() throws Exception {
        ClassNode controller = node(Files.readAllBytes(Path.of(
                "build/classes/java/main/fr/tropimon/catchpreview/release/PokemonReleaseController.class")));
        for (String name : List.of("release", "onPcOpened")) {
            MethodNode method = controller.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            boolean locate = false, freshPc = false;
            for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("locate")) locate = true;
                if (call.name.equals("isPcNearby")) {
                    var previous = call.getPrevious();
                    while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                    if (previous != null && previous.getOpcode() == org.objectweb.asm.Opcodes.ICONST_1) freshPc = true;
                }
            }
            assertTrue(locate, name + " must re-find the UUID");
            assertTrue(freshPc, name + " must force the PC scan");
        }
    }
}
