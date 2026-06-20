package me.danjono.inventoryrollback.inventory;

import com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus;
import com.tcoded.lightlibs.bukkitversion.BukkitVersion;
import me.danjono.inventoryrollback.config.ConfigData;
import me.danjono.inventoryrollback.config.MessageData;
import me.danjono.inventoryrollback.config.SoundData;
import me.danjono.inventoryrollback.data.LogType;
import me.danjono.inventoryrollback.data.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PlayerRestoreService {

    private final InventoryRollbackPlus main;
    private final PendingRestoreQueue queue;

    public PlayerRestoreService(InventoryRollbackPlus main, PendingRestoreQueue queue) {
        this.main = main;
        this.queue = queue;
    }

    public void restoreOrQueue(OfflinePlayer offlinePlayer, Player staff, LogType logType, long timestamp,
                               RestoreAttribute attribute) {
        Player target = offlinePlayer.getPlayer();
        if (target == null || !target.isOnline()) {
            queueRestore(offlinePlayer, staff, logType, timestamp, attribute);
            return;
        }

        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
            try {
                PlayerData data = loadBackupData(offlinePlayer.getUniqueId(), logType, timestamp);
                Future<Boolean> future = main.getServer().getScheduler().callSyncMethod(main, () -> {
                    Player onlineTarget = main.getServer().getPlayer(offlinePlayer.getUniqueId());
                    if (onlineTarget == null || !onlineTarget.isOnline()) {
                        queueRestore(offlinePlayer, staff, logType, timestamp, attribute);
                        return false;
                    }

                    applyRestore(onlineTarget, data, attribute, staff.getName(), staff.getUniqueId(), staff);
                    return true;
                });
                future.get();
            } catch (ExecutionException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void processQueuedRestores(Player player) {
        List<PendingRestoreQueue.QueuedRestore> restores = queue.getQueuedRestores(player.getUniqueId());
        if (restores.isEmpty()) {
            return;
        }

        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
            for (PendingRestoreQueue.QueuedRestore restore : restores) {
                if (!player.isOnline()) {
                    return;
                }

                try {
                    PlayerData data = loadBackupData(restore.getTargetUuid(), restore.getLogType(), restore.getTimestamp());
                    Future<Boolean> future = main.getServer().getScheduler().callSyncMethod(main, () -> {
                        if (!player.isOnline()) {
                            return false;
                        }

                        applyRestore(player, data, restore.getAttribute(), restore.getStaffName(), restore.getStaffUuid(), null);
                        return true;
                    });

                    if (future.get()) {
                        queue.remove(player.getUniqueId(), restore.getId());
                    }
                } catch (ExecutionException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void queueRestore(OfflinePlayer offlinePlayer, Player staff, LogType logType, long timestamp,
                              RestoreAttribute attribute) {
        String targetName = offlinePlayer.getName() == null ? offlinePlayer.getUniqueId().toString() : offlinePlayer.getName();
        queue.queue(offlinePlayer.getUniqueId(), targetName, staff.getUniqueId(), staff.getName(),
                logType, timestamp, attribute);
        staff.sendMessage(MessageData.getPluginPrefix()
                + MessageData.getRestoreQueued(targetName, attribute.getDisplayName()));
    }

    private PlayerData loadBackupData(UUID targetUuid, LogType logType, long timestamp)
            throws ExecutionException, InterruptedException {
        PlayerData data = new PlayerData(targetUuid, logType, timestamp);
        if (ConfigData.getSaveType() == ConfigData.SaveType.MYSQL) {
            data.getAllBackupData().get();
        }
        return data;
    }

    private void applyRestore(Player player, PlayerData data, RestoreAttribute attribute, String staffName,
                              UUID staffUuid, Player staffToNotify) {
        if (attribute == RestoreAttribute.MAIN_INVENTORY) {
            restoreMainInventory(player, data);
            player.sendMessage(MessageData.getPluginPrefix() + MessageData.getMainInventoryRestoredPlayer(staffName));
            if (staffToNotify != null && !staffUuid.equals(player.getUniqueId())) {
                staffToNotify.sendMessage(MessageData.getPluginPrefix() + MessageData.getMainInventoryRestored(player.getName()));
            }
        } else if (attribute == RestoreAttribute.ENDER_CHEST) {
            restoreEnderChest(player, data);
            player.sendMessage(MessageData.getPluginPrefix() + MessageData.getEnderChestRestoredPlayer(staffName));
            if (staffToNotify != null && !staffUuid.equals(player.getUniqueId())) {
                staffToNotify.sendMessage(MessageData.getPluginPrefix() + MessageData.getEnderChestRestored(player.getName()));
            }
        } else if (attribute == RestoreAttribute.HEALTH) {
            player.setHealth(data.getHealth());
            if (SoundData.isFoodRestoredEnabled()) {
                player.playSound(player.getLocation(), SoundData.getFoodRestored(), 1, 1);
            }
            player.sendMessage(MessageData.getPluginPrefix() + MessageData.getHealthRestoredPlayer(staffName));
            if (staffToNotify != null && !staffUuid.equals(player.getUniqueId())) {
                staffToNotify.sendMessage(MessageData.getPluginPrefix() + MessageData.getHealthRestored(player.getName()));
            }
        } else if (attribute == RestoreAttribute.HUNGER) {
            player.setFoodLevel(data.getFoodLevel());
            player.setSaturation(data.getSaturation());
            if (SoundData.isHungerRestoredEnabled()) {
                player.playSound(player.getLocation(), SoundData.getHungerRestored(), 1, 1);
            }
            player.sendMessage(MessageData.getPluginPrefix() + MessageData.getHungerRestoredPlayer(staffName));
            if (staffToNotify != null && !staffUuid.equals(player.getUniqueId())) {
                staffToNotify.sendMessage(MessageData.getPluginPrefix() + MessageData.getHungerRestored(player.getName()));
            }
        } else if (attribute == RestoreAttribute.EXPERIENCE) {
            float xp = data.getXP();
            RestoreInventory.setTotalExperience(player, xp);
            if (SoundData.isExperienceRestoredEnabled()) {
                player.playSound(player.getLocation(), SoundData.getExperienceSound(), 1, 1);
            }
            int level = (int) RestoreInventory.getLevel(xp);
            player.sendMessage(MessageData.getPluginPrefix() + MessageData.getExperienceRestoredPlayer(staffName, level));
            if (staffToNotify != null && !staffUuid.equals(player.getUniqueId())) {
                staffToNotify.sendMessage(MessageData.getPluginPrefix() + MessageData.getExperienceRestored(player.getName(), level));
            }
        }
    }

    private void restoreMainInventory(Player player, PlayerData data) {
        ItemStack[] inventory = data.getMainInventory();
        ItemStack[] armour = data.getArmour();

        player.getInventory().setContents(inventory);
        if (main.getVersion().lessOrEqThan(BukkitVersion.v1_8_R3)) {
            player.getInventory().setArmorContents(armour);
        }

        if (SoundData.isInventoryRestoreEnabled()) {
            player.playSound(player.getLocation(), SoundData.getInventoryRestored(), 1, 1);
        }
    }

    private void restoreEnderChest(Player player, PlayerData data) {
        ItemStack[] enderChest = data.getEnderChest();
        if (enderChest == null) {
            enderChest = new ItemStack[0];
        }
        player.getEnderChest().setContents(enderChest);

        if (SoundData.isInventoryRestoreEnabled()) {
            player.playSound(player.getLocation(), SoundData.getInventoryRestored(), 1, 1);
        }
    }
}
