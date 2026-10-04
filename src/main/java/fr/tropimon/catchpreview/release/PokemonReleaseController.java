package fr.tropimon.catchpreview.release;

import fr.tropimon.catchpreview.TropimonCatchPreviewClient;

import com.cobblemon.mod.common.block.PCBlock;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.net.messages.server.storage.party.ReleasePartyPokemonPacket;
import com.cobblemon.mod.common.net.messages.server.storage.pc.ReleasePCPokemonPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import java.util.UUID;

public final class PokemonReleaseController {
    private static final int PC_RADIUS = ReleaseRules.PC_RADIUS;
    private static final long PC_CACHE_DURATION_NANOS = 250_000_000L;
    private static final long PC_OPEN_TIMEOUT_NANOS = 5_000_000_000L;
    private static long pcCacheExpiresAtNanos;
    private static BlockPos cachedPlayerPos;
    private static BlockPos cachedPcPos;
    private static Object cachedWorld, cachedConnection, cachedStorage;
    private static final SingleEntryCache<UUID, Location> LOCATIONS = new SingleEntryCache<>();
    private static PendingPcRelease pendingPcRelease;

    private PokemonReleaseController() {}

    public static void invalidateStorage() { LOCATIONS.invalidate(); }

    public static void reset() {
        LOCATIONS.invalidate();
        cachedPlayerPos = null;
        cachedPcPos = null;
        pcCacheExpiresAtNanos = 0;
        pendingPcRelease = null;
        cachedWorld = cachedConnection = cachedStorage = null;
    }

    /** Called before every query: a dimension switch cannot reuse a one-frame-old cache. */
    public static void checkContext() {
        var client = MinecraftClient.getInstance();
        var storage = CobblemonClient.INSTANCE.getStorage();
        if (cachedWorld != client.world || cachedConnection != client.getNetworkHandler() || cachedStorage != storage) {
            reset();
            cachedWorld = client.world;
            cachedConnection = client.getNetworkHandler();
            cachedStorage = storage;
        }
        if (pendingPcRelease != null && System.nanoTime() > pendingPcRelease.expiresAtNanos) pendingPcRelease = null;
    }

    public static boolean canRelease(Pokemon pokemon) {
        checkContext();
        if (pokemon == null || !isPcNearby(false)) return false;
        UUID pokemonId = pokemon.getUuid();
        if (!LOCATIONS.contains(pokemonId)) LOCATIONS.put(pokemonId, locate(pokemonId));
        return LOCATIONS.value() != null;
    }

    public static boolean release(Pokemon pokemon) {
        checkContext();
        if (pokemon == null || pendingPcRelease != null || !isPcNearby(true)) return false;
        // Never use the rendering cache to authorize a destructive action.
        Location location = locate(pokemon.getUuid());
        if (location == null) return false;
        if (location.partyPosition != null) {
            new ReleasePartyPokemonPacket(pokemon.getUuid(), location.partyPosition).sendToServer();
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen instanceof PCGUI gui && gui.getPc().getUuid().equals(location.storeId)) {
                new ReleasePCPokemonPacket(pokemon.getUuid(), location.pcPosition).sendToServer();
            } else {
                if (client.player == null || client.interactionManager == null) return false;
                pendingPcRelease = new PendingPcRelease(pokemon.getUuid(), location, cachedPcPos,
                        System.nanoTime() + PC_OPEN_TIMEOUT_NANOS);
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(cachedPcPos), Direction.UP, cachedPcPos, false);
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
            }
        }
        TropimonCatchPreviewClient.LOGGER.info("Demande de relâchement envoyée pour {}", pokemon.getUuid());
        return true;
    }

    public static void onPcOpened(MinecraftClient client, UUID storeId) {
        checkContext();
        PendingPcRelease pending = pendingPcRelease;
        if (pending == null) return;
        pendingPcRelease = null;
        // The reply may arrive after a transfer, teleport or removal. Lock UUID AND slot.
        Location currentLocation = locate(pending.pokemonId);
        if (!pending.location.equals(currentLocation) || !pending.location.storeId.equals(storeId)
                || !isPcNearby(true) || client.world == null
                || !(client.world.getBlockState(pending.pcBlock).getBlock() instanceof PCBlock)
                || !withinRange(client.player.getBlockPos(), pending.pcBlock)
                || !(client.currentScreen instanceof PCGUI gui) || !gui.getPc().getUuid().equals(storeId)) return;
        new ReleasePCPokemonPacket(pending.pokemonId, currentLocation.pcPosition).sendToServer();
        // Never close a newer screen.
        client.execute(() -> { if (client.currentScreen == gui) client.setScreen(null); });
    }

    private static Location locate(UUID pokemonId) {
        var storage = CobblemonClient.INSTANCE.getStorage();
        var party = storage.getParty();
        if (party != null) {
            Pokemon storedPokemon = party.findByUUID(pokemonId);
            if (storedPokemon != null) {
                int partyMemberCount = 0;
                for (Pokemon member : party.getSlots()) if (member != null) partyMemberCount++;
                if (!ReleaseRules.canReleaseParty(partyMemberCount)) return null;
                var position = party.getPosition(storedPokemon);
                return position == null ? null : new Location(party.getUuid(), position, null);
            }
        }
        for (var pc : storage.getPcStores().values()) {
            Pokemon storedPokemon = pc.findByUUID(pokemonId);
            if (storedPokemon != null) {
                var position = pc.getPosition(storedPokemon);
                if (position != null) return new Location(pc.getUuid(), null, position);
            }
        }
        return null;
    }

    private static boolean withinRange(BlockPos center, BlockPos pc) {
        return ReleaseRules.withinPcRange(center.getX() - pc.getX(),
                center.getY() - pc.getY(), center.getZ() - pc.getZ());
    }

    private static boolean isPcNearby(boolean forceRescan) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;
        long nowNanos = System.nanoTime();
        BlockPos center = client.player.getBlockPos();
        if (!forceRescan && nowNanos <= pcCacheExpiresAtNanos && center.equals(cachedPlayerPos)) {
            if (cachedPcPos == null) return false;
            if (client.world.getBlockState(cachedPcPos).getBlock() instanceof PCBlock) return true;
        }
        cachedPlayerPos = center.toImmutable();
        pcCacheExpiresAtNanos = nowNanos + PC_CACHE_DURATION_NANOS;
        cachedPcPos = null;
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        // Preserve the original cuboid and scan order, including its corners.
        for (int dx = -PC_RADIUS; dx <= PC_RADIUS; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -PC_RADIUS; dz <= PC_RADIUS; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (client.world.getBlockState(cursor).getBlock() instanceof PCBlock) {
                        cachedPcPos = cursor.toImmutable();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private record Location(UUID storeId, PartyPosition partyPosition, PCPosition pcPosition) {}
    private record PendingPcRelease(UUID pokemonId, Location location, BlockPos pcBlock, long expiresAtNanos) {}
}
