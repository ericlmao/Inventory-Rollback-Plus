package me.danjono.inventoryrollback.inventory;

import com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus;
import me.danjono.inventoryrollback.data.LogType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PendingRestoreQueue {

    private static final String ROOT = "pending-restores";

    private final File file;
    private YamlConfiguration data;

    public PendingRestoreQueue(InventoryRollbackPlus main) {
        this.file = new File(main.getDataFolder(), "pending-restores.yml");
        reload();
    }

    public synchronized QueuedRestore queue(UUID targetUuid, String targetName, UUID staffUuid, String staffName,
                                            LogType logType, long timestamp, RestoreAttribute attribute) {
        reload();

        long queuedAt = System.currentTimeMillis();
        String id = queuedAt + "-" + attribute.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String path = ROOT + "." + targetUuid + "." + id + ".";

        data.set(path + "target-name", targetName);
        data.set(path + "staff-uuid", staffUuid.toString());
        data.set(path + "staff-name", staffName);
        data.set(path + "log-type", logType.name());
        data.set(path + "timestamp", timestamp);
        data.set(path + "action", attribute.name());
        data.set(path + "queued-at", queuedAt);
        save();

        return new QueuedRestore(id, targetUuid, targetName, staffUuid, staffName, logType, timestamp, attribute, queuedAt);
    }

    public synchronized List<QueuedRestore> getQueuedRestores(UUID targetUuid) {
        reload();

        List<QueuedRestore> restores = new ArrayList<>();
        ConfigurationSection playerSection = data.getConfigurationSection(ROOT + "." + targetUuid);
        if (playerSection == null) {
            return restores;
        }

        for (String id : playerSection.getKeys(false)) {
            String path = ROOT + "." + targetUuid + "." + id + ".";
            try {
                RestoreAttribute attribute = RestoreAttribute.valueOf(data.getString(path + "action"));
                LogType logType = LogType.valueOf(data.getString(path + "log-type"));
                UUID staffUuid = UUID.fromString(data.getString(path + "staff-uuid"));
                restores.add(new QueuedRestore(
                        id,
                        targetUuid,
                        data.getString(path + "target-name"),
                        staffUuid,
                        data.getString(path + "staff-name"),
                        logType,
                        data.getLong(path + "timestamp"),
                        attribute,
                        data.getLong(path + "queued-at")
                ));
            } catch (IllegalArgumentException | NullPointerException ex) {
                InventoryRollbackPlus.getInstance().getLogger()
                        .warning("Skipping invalid pending restore entry " + targetUuid + "/" + id + ": " + ex.getMessage());
            }
        }

        return restores;
    }

    public synchronized void remove(UUID targetUuid, String id) {
        reload();
        data.set(ROOT + "." + targetUuid + "." + id, null);

        ConfigurationSection playerSection = data.getConfigurationSection(ROOT + "." + targetUuid);
        if (playerSection != null && playerSection.getKeys(false).isEmpty()) {
            data.set(ROOT + "." + targetUuid, null);
        }

        save();
    }

    private void reload() {
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException ex) {
            InventoryRollbackPlus.getInstance().getLogger().severe("Could not save pending restores: " + ex.getMessage());
        }
    }

    public static class QueuedRestore {
        private final String id;
        private final UUID targetUuid;
        private final String targetName;
        private final UUID staffUuid;
        private final String staffName;
        private final LogType logType;
        private final long timestamp;
        private final RestoreAttribute attribute;
        private final long queuedAt;

        public QueuedRestore(String id, UUID targetUuid, String targetName, UUID staffUuid, String staffName,
                             LogType logType, long timestamp, RestoreAttribute attribute, long queuedAt) {
            this.id = id;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.staffUuid = staffUuid;
            this.staffName = staffName;
            this.logType = logType;
            this.timestamp = timestamp;
            this.attribute = attribute;
            this.queuedAt = queuedAt;
        }

        public String getId() {
            return id;
        }

        public UUID getTargetUuid() {
            return targetUuid;
        }

        public String getTargetName() {
            return targetName;
        }

        public UUID getStaffUuid() {
            return staffUuid;
        }

        public String getStaffName() {
            return staffName;
        }

        public LogType getLogType() {
            return logType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public RestoreAttribute getAttribute() {
            return attribute;
        }

        public long getQueuedAt() {
            return queuedAt;
        }
    }
}
