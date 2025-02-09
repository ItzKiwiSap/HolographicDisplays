/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.core.tracking;

import me.filoghost.holographicdisplays.core.base.BaseItemHologramLine;
import me.filoghost.holographicdisplays.core.listener.LineClickListener;
import me.filoghost.holographicdisplays.core.tick.CachedPlayer;
import me.filoghost.holographicdisplays.nms.common.NMSManager;
import me.filoghost.holographicdisplays.nms.common.entity.ItemNMSPacketEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.List;
import java.util.Objects;

public class ItemLineTracker extends ClickableLineTracker<Viewer> {

    private final BaseItemHologramLine line;
    private final ItemNMSPacketEntity itemEntity;

    private ItemStack itemStack;
    private boolean itemStackChanged;

    private boolean spawnItemEntity;
    private boolean spawnItemEntityChanged;

    public ItemLineTracker(
            BaseItemHologramLine line,
            NMSManager nmsManager,
            LineClickListener lineClickListener) {
        super(line, nmsManager, lineClickListener);
        this.line = line;
        this.itemEntity = nmsManager.newItemPacketEntity();
    }

    @Override
    public BaseItemHologramLine getLine() {
        return line;
    }

    @MustBeInvokedByOverriders
    @Override
    protected void update(List<CachedPlayer> onlinePlayers, List<CachedPlayer> movedPlayers, int maxViewRange) {
        super.update(onlinePlayers, movedPlayers, maxViewRange);

        if (spawnItemEntity && hasViewers() && line.hasPickupCallback()) {
            for (Viewer viewer : getViewers()) {
                if (viewer.getLocation() != null && CollisionHelper.isInPickupRange(viewer.getLocation(), positionCoordinates)) {
                    line.onPickup(viewer.getBukkitPlayer());
                }
            }
        }
    }

    @Override
    protected boolean updatePlaceholders() {
        return false;
    }

    @Override
    protected Viewer createViewer(CachedPlayer cachedPlayer) {
        return new Viewer(cachedPlayer);
    }

    @MustBeInvokedByOverriders
    @Override
    protected void detectChanges() {
        super.detectChanges();

        ItemStack itemStack = line.getItemStack();
        if (!Objects.equals(this.itemStack, itemStack)) {
            this.itemStack = itemStack;
            this.itemStackChanged = true;
        }

        boolean spawnItemEntity = itemStack != null;
        if (this.spawnItemEntity != spawnItemEntity) {
            this.spawnItemEntity = spawnItemEntity;
            this.spawnItemEntityChanged = true;
        }
    }

    @MustBeInvokedByOverriders
    @Override
    protected void clearDetectedChanges() {
        super.clearDetectedChanges();
        this.itemStackChanged = false;
        this.spawnItemEntityChanged = false;
    }

    @MustBeInvokedByOverriders
    @Override
    protected void sendSpawnPackets(Viewers<Viewer> viewers) {
        super.sendSpawnPackets(viewers);

        if (spawnItemEntity) {
            viewers.sendPackets(itemEntity.newSpawnPackets(positionCoordinates, itemStack));
        }
    }

    @MustBeInvokedByOverriders
    @Override
    protected void sendDestroyPackets(Viewers<Viewer> viewers) {
        super.sendDestroyPackets(viewers);

        if (spawnItemEntity) {
            viewers.sendPackets(itemEntity.newDestroyPackets());
        }
    }

    @MustBeInvokedByOverriders
    @Override
    protected void sendChangesPackets(Viewers<Viewer> viewers) {
        super.sendChangesPackets(viewers);

        if (spawnItemEntityChanged) {
            if (spawnItemEntity) {
                viewers.sendPackets(itemEntity.newSpawnPackets(positionCoordinates, itemStack));
            } else {
                viewers.sendPackets(itemEntity.newDestroyPackets());
            }
        } else if (itemStackChanged) {
            // Only send item changes if full spawn/destroy packets were not sent
            viewers.sendPackets(itemEntity.newChangePackets(itemStack));
        }
    }

    @MustBeInvokedByOverriders
    @Override
    protected void sendPositionChangePackets(Viewers<Viewer> viewers) {
        super.sendPositionChangePackets(viewers);

        if (spawnItemEntity) {
            viewers.sendPackets(itemEntity.newTeleportPackets(positionCoordinates));
        }
    }

    @Override
    protected double getViewRange() {
        return 16;
    }

}
