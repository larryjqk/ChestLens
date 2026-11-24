package com.larryjqk.chestlens.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import com.mojang.blaze3d.platform.InputConstants;
import com.larryjqk.chestlens.client.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChestLensClient implements ClientModInitializer {
	private static final int SLOT_SIZE = 16;
	private static final int BOX_WIDTH = 160;
	private static final int BUTTON_SIZE = 20;

	private static String query = "";
	private static boolean searchBarVisible = true;
	private static boolean searchTooltips = true;

	private static EditBox activeSearchBox;
	private static Button activeClearButton;
	private static KeyMapping focusSearchKey;
	private static KeyMapping toggleSearchKey;
	private static KeyMapping toggleTooltipSearchKey;
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("chest-lens", "main"));
	private static final Map<Slot, MatchCache> matchCache = new HashMap<>();

	@Override
	public void onInitializeClient() {
		focusSearchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.chest-lens.focus_search", InputConstants.KEY_K, KEY_CATEGORY));
		toggleSearchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.chest-lens.toggle_search", InputConstants.KEY_F8, KEY_CATEGORY));
		toggleTooltipSearchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.chest-lens.toggle_tooltip_search", InputConstants.KEY_F9, KEY_CATEGORY));

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			matchCache.clear();
			if (!(screen instanceof AbstractContainerScreen<?> container) || !isStorageScreen(container)) {
				activeSearchBox = null;
				activeClearButton = null;
				return;
			}
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) container;
			int boxX = screen.width / 2 - (BOX_WIDTH + BUTTON_SIZE) / 2;
			int boxY = accessor.chestLens$getTopPos() - 24;

			SearchBox box = new SearchBox(client, boxX, boxY, BOX_WIDTH, 20);
			box.setMaxLength(128);
			box.setValue(query);
			box.setHint(Component.translatable("gui.chest-lens.search_hint"));
			box.setFocused(false);

			Button clear = Button.builder(Component.literal("x"), b -> box.setValue(""))
					.bounds(boxX + BOX_WIDTH, boxY, BUTTON_SIZE, BUTTON_SIZE)
					.build();

			box.setResponder(value -> {
				query = value;
				clear.active = !value.isEmpty();
			});
			clear.active = !query.isEmpty();

			box.setVisible(searchBarVisible);
			clear.visible = searchBarVisible;

			Screens.getWidgets(screen).add(box);
			Screens.getWidgets(screen).add(clear);
			activeSearchBox = box;
			activeClearButton = clear;

			ScreenEvents.remove(screen).register(s -> {
				if (activeSearchBox == box) activeSearchBox = null;
				if (activeClearButton == clear) activeClearButton = null;
			});
		});
	}

	public static boolean handleKey(AbstractContainerScreen<?> screen, KeyEvent event) {
		if (!isStorageScreen(screen)) return false;

		if (isSearchFocused()) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				activeSearchBox.setFocused(false);
				screen.setFocused(null);
				return true;
			}
			if (toggleSearchKey.matches(event)) {
				setSearchBarVisible(screen, false);
				return true;
			}
			if (toggleTooltipSearchKey.matches(event)) {
				toggleTooltipSearch();
				return true;
			}

			activeSearchBox.keyPressed(event);
			return true;
		}

		if (focusSearchKey.matches(event)) {
			setSearchBarVisible(screen, true);
			if (activeSearchBox != null) {
				activeSearchBox.setFocused(true);
				screen.setFocused(activeSearchBox);
			}
			return true;
		}
		if (toggleSearchKey.matches(event)) {
			setSearchBarVisible(screen, !searchBarVisible);
			return true;
		}
		if (toggleTooltipSearchKey.matches(event)) {
			toggleTooltipSearch();
			return true;
		}
		return false;
	}

	private static void setSearchBarVisible(AbstractContainerScreen<?> screen, boolean visible) {
		searchBarVisible = visible;
		if (activeSearchBox == null) return;
		activeSearchBox.setVisible(visible);
		if (activeClearButton != null) activeClearButton.visible = visible;
		if (!visible) {
			activeSearchBox.setFocused(false);
			screen.setFocused(null);
		}
	}

	private static void toggleTooltipSearch() {
		searchTooltips = !searchTooltips;
		matchCache.clear();
		Minecraft.getInstance().gui.hud.setOverlayMessage(Component.translatable(searchTooltips
				? "message.chest-lens.tooltip_search_on" : "message.chest-lens.tooltip_search_off"), false);
	}

	public static void drawFilter(AbstractContainerScreen<?> container, GuiGraphicsExtractor graphics) {
		if (!isStorageScreen(container) || query.isBlank()) return;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) container;
		int left = accessor.chestLens$getLeftPos();
		int top = accessor.chestLens$getTopPos();
		Slot hovered = accessor.chestLens$getHoveredSlot();
		int storageSlots = container.getMenu().slots.size() - 36;

		for (int i = 0; i < storageSlots; i++) {
			Slot slot = container.getMenu().slots.get(i);
			if (!slot.isActive() || slot == hovered) continue;
			if (matches(slot, Minecraft.getInstance(), query)) continue;
			int x = left + slot.x;
			int y = top + slot.y;
			graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xAA000000);
		}
	}

	private static boolean isStorageScreen(AbstractContainerScreen<?> screen) {
		String menuName = screen.getMenu().getClass().getSimpleName();
		return menuName.contains("Chest") || menuName.contains("Shulker") || menuName.contains("Hopper")
				|| menuName.contains("Dispenser") || menuName.contains("Dropper") || menuName.contains("Barrel");
	}

	public static boolean isSearchFocused() {
		return activeSearchBox != null && activeSearchBox.isVisible() && activeSearchBox.isFocused();
	}

	private static boolean matches(Slot slot, Minecraft client, String needle) {
		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) return false;
		String normalizedNeedle = needle.toLowerCase(Locale.ROOT);
		int componentsHash = stack.getComponents().hashCode();
		MatchCache cached = matchCache.get(slot);
		if (cached != null && cached.stack == stack && cached.count == stack.getCount()
				&& cached.componentsHash == componentsHash && cached.query.equals(normalizedNeedle)
				&& cached.searchTooltips == searchTooltips) return cached.matches;
		boolean matches = stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(normalizedNeedle);
		if (!matches && searchTooltips && client.player != null) {
			matches = stack.getTooltipLines(Item.TooltipContext.EMPTY, client.player, TooltipFlag.NORMAL).stream()
					.anyMatch(line -> line.getString().toLowerCase(Locale.ROOT).contains(normalizedNeedle));
		}
		matchCache.put(slot, new MatchCache(stack, stack.getCount(), componentsHash, normalizedNeedle, searchTooltips, matches));
		return matches;
	}

	private record MatchCache(ItemStack stack, int count, int componentsHash, String query, boolean searchTooltips, boolean matches) {}

	private static final class SearchBox extends EditBox {
		private SearchBox(Minecraft client, int x, int y, int width, int height) {
			super(client.font, x, y, width, height, Component.translatable("gui.chest-lens.search_hint"));
		}
	}
}
