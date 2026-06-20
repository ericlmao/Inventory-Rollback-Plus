package me.danjono.inventoryrollback.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerRestoreServiceTest {

    @Test
    void normalizeContentsReturnsEmptyDestinationSizedArrayForNullItems() {
        ItemStack[] normalized = PlayerRestoreService.normalizeContents(null, 4);

        assertEquals(4, normalized.length);
        assertNull(normalized[0]);
        assertNull(normalized[1]);
        assertNull(normalized[2]);
        assertNull(normalized[3]);
    }

    @Test
    void normalizeContentsPadsShortArrays() {
        ItemStack item = new ItemStack(Material.STONE);

        ItemStack[] normalized = PlayerRestoreService.normalizeContents(new ItemStack[] { item }, 3);

        assertEquals(3, normalized.length);
        assertSame(item, normalized[0]);
        assertNull(normalized[1]);
        assertNull(normalized[2]);
    }

    @Test
    void normalizeContentsTruncatesLongArrays() {
        ItemStack first = new ItemStack(Material.STONE);
        ItemStack second = new ItemStack(Material.DIRT);
        ItemStack third = new ItemStack(Material.GRASS_BLOCK);

        ItemStack[] normalized = PlayerRestoreService.normalizeContents(
                new ItemStack[] { first, second, third }, 2);

        assertArrayEquals(new ItemStack[] { first, second }, normalized);
    }

    @Test
    void normalizeContentsPreservesCorrectlySizedArraysByContent() {
        ItemStack first = new ItemStack(Material.STONE);
        ItemStack second = new ItemStack(Material.DIRT);
        ItemStack[] items = new ItemStack[] { first, second };

        ItemStack[] normalized = PlayerRestoreService.normalizeContents(items, 2);

        assertArrayEquals(items, normalized);
    }
}
