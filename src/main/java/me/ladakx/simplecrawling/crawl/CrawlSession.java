package me.ladakx.simplecrawling.crawl;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.ladakx.simplecrawling.SimpleCrawling;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class CrawlSession {

    private static final boolean FOLIA = isClassPresent("io.papermc.paper.threadedregions.RegionizedServer");
    private static final Method TELEPORT_ASYNC_METHOD = findTeleportAsyncMethod();

    private final SimpleCrawling plugin;
    private final Player player;

    private TaskImplementation<Void> task;
    private boolean active;
    private ArmorStand fakeAnchor;
    private Shulker fakeShulker;
    private boolean teleportInFlight;
    private Location pendingLocation;

    public CrawlSession(SimpleCrawling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void start() {
        this.active = true;

        if (plugin.getSchedulerAdapter().isOwnedByCurrentRegion(player)) {
            startTracking();
            return;
        }

        plugin.getSchedulerAdapter().runEntity(player, this::startTracking, this::handleRetired);
    }

    public void stop() {
        this.active = false;

        TaskImplementation<Void> currentTask = task;
        if (currentTask != null) {
            currentTask.cancel();
            task = null;
        }

        if (plugin.getSchedulerAdapter().isOwnedByCurrentRegion(player)) {
            cleanupTrackedState();
            return;
        }

        plugin.getSchedulerAdapter().runEntity(player, this::cleanupTrackedState, this::handleRetired);
    }

    private void startTracking() {
        if (!active) {
            return;
        }

        ensureFakeEntities();
        player.setSwimming(true);

        this.task = plugin.getSchedulerAdapter().runEntityTimer(player, () -> {
            if (!player.isOnline() || !player.isValid()) {
                stop();
                return;
            }

            tick();
        }, this::handleRetired, 1L, 1L);
    }

    private void cleanupTrackedState() {
        if (player.isOnline()) {
            player.setSwimming(false);
        }

        destroyFakeEntities();
    }

    private void handleRetired() {
        this.task = null;
        destroyFakeEntities();

        if (!active || !player.isOnline() || !player.isValid()) {
            return;
        }

        plugin.getSchedulerAdapter().runEntity(player, this::startTracking, this::handleRetired);
    }

    private void tick() {
        if (!ensureFakeEntities()) {
            return;
        }

        Location location = getAnchorLocation();
        if (!fakeAnchor.getWorld().equals(location.getWorld())) {
            respawnFakeEntities();
        } else {
            updateEntityPositions(location);
            syncVisibility();
        }

        if (!player.isSwimming()) {
            player.setSwimming(true);
        }
    }

    private boolean ensureFakeEntities() {
        if (player.getWorld() == null) {
            return false;
        }

        if (isSpawned()) {
            syncVisibility();
            return true;
        }

        spawnFakeEntities();
        return isSpawned();
    }

    private boolean isSpawned() {
        return fakeAnchor != null
                && fakeAnchor.isValid()
                && fakeShulker != null
                && fakeShulker.isValid();
    }

    private void respawnFakeEntities() {
        destroyFakeEntities();
        spawnFakeEntities();
    }

    private void spawnFakeEntities() {
        Location location = getAnchorLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        fakeAnchor = world.spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setSilent(true);
            armorStand.setInvulnerable(true);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setBasePlate(false);
            armorStand.setCollidable(false);
            armorStand.setPersistent(false);
        });

        fakeShulker = world.spawn(location, Shulker.class, shulker -> {
            shulker.setAI(false);
            shulker.setGravity(false);
            shulker.setSilent(true);
            shulker.setInvulnerable(true);
            shulker.setInvisible(true);
            shulker.setCollidable(false);
            shulker.setPersistent(false);
            shulker.setRemoveWhenFarAway(false);
        });

        updateEntityPositions(location);
        syncVisibility();
    }

    private void destroyFakeEntities() {
        removeEntity(fakeShulker);
        removeEntity(fakeAnchor);
        fakeShulker = null;
        fakeAnchor = null;
        teleportInFlight = false;
        pendingLocation = null;
    }

    private void removeEntity(Entity entity) {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    private void updateEntityPositions(Location location) {
        if (teleportInFlight) {
            pendingLocation = location.clone();
            return;
        }

        if (FOLIA) {
            ensurePassengerAttached();
        } else if (fakeShulker.getVehicle() == fakeAnchor) {
            fakeAnchor.removePassenger(fakeShulker);
        } else {
            ensurePassengerAttached();
        }

        teleportInFlight = true;
        teleportAnchor(fakeAnchor, location).whenComplete((success, throwable) -> {
            Entity anchor = fakeAnchor;
            if (anchor == null || !anchor.isValid()) {
                teleportInFlight = false;
                pendingLocation = null;
                return;
            }

            plugin.getSchedulerAdapter().runEntity(anchor, () -> {
                teleportInFlight = false;

                if (!isSpawned()) {
                    pendingLocation = null;
                    return;
                }

                ensurePassengerAttached();

                Location nextLocation = pendingLocation;
                pendingLocation = null;
                if (nextLocation != null) {
                    updateEntityPositions(nextLocation);
                }
            });
        });
    }

    private void ensurePassengerAttached() {
        if (fakeShulker.getVehicle() != fakeAnchor) {
            fakeAnchor.addPassenger(fakeShulker);
        }
    }

    private CompletableFuture<Boolean> teleportAnchor(ArmorStand anchor, Location location) {
        if (TELEPORT_ASYNC_METHOD == null) {
            return CompletableFuture.completedFuture(anchor.teleport(location));
        }

        try {
            Object result = TELEPORT_ASYNC_METHOD.invoke(anchor, location);
            if (result instanceof CompletableFuture) {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) result;
                return future;
            }
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Failed to use teleportAsync for crawl anchor, falling back to teleport.");
        }

        return CompletableFuture.completedFuture(anchor.teleport(location));
    }

    private void syncVisibility() {
        if (!isSpawned()) {
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) {
                onlinePlayer.showEntity(plugin, fakeAnchor);
                onlinePlayer.showEntity(plugin, fakeShulker);
            } else {
                onlinePlayer.hideEntity(plugin, fakeAnchor);
                onlinePlayer.hideEntity(plugin, fakeShulker);
            }
        }
    }

    private Location getAnchorLocation() {
        return player.getLocation().clone().add(0.0D, plugin.getHitboxOffset(), 0.0D);
    }

    private static Method findTeleportAsyncMethod() {
        try {
            return Entity.class.getMethod("teleportAsync", Location.class);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
