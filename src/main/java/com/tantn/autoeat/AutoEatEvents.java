package com.tantn.autoeat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = AutoEat.MODID)
public final class AutoEatEvents {
    private static final int MAX_FOOD_LEVEL = 20;
    private static final int START_EAT_LEVEL = MAX_FOOD_LEVEL / 2; // 50%
    private static final int STOP_EAT_LEVEL = (int) Math.floor(MAX_FOOD_LEVEL * 0.85D); // 85%
    private static final String MESSAGES_RESOURCE = "/autoeat_messages.txt";
    private static final List<String> EAT_MESSAGES = loadEatMessages();
    private static final Set<UUID> AUTO_EATING_PLAYERS = new HashSet<>();

    private AutoEatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
            AUTO_EATING_PLAYERS.remove(serverPlayer.getUUID());
            return;
        }

        FoodData foodData = serverPlayer.getFoodData();
        int foodLevel = foodData.getFoodLevel();
        UUID playerId = serverPlayer.getUUID();

        if (foodLevel <= START_EAT_LEVEL) {
            AUTO_EATING_PLAYERS.add(playerId);
        }

        if (foodLevel > STOP_EAT_LEVEL || !foodData.needsFood()) {
            AUTO_EATING_PLAYERS.remove(playerId);
            return;
        }

        if (!AUTO_EATING_PLAYERS.contains(playerId)) {
            return;
        }

        // Check every 10 ticks to avoid running inventory scans every tick.
        if (serverPlayer.tickCount % 10 != 0) {
            return;
        }

        Inventory inventory = serverPlayer.getInventory();
        boolean consumed = consumeSingleItemFromTop(serverPlayer, inventory, foodData);

        if (consumed) {
            inventory.setChanged();
            serverPlayer.containerMenu.broadcastChanges();
        } else {
            // Stop tracking until new food appears to avoid unnecessary inventory scans.
            AUTO_EATING_PLAYERS.remove(playerId);
        }

        if (foodData.getFoodLevel() > STOP_EAT_LEVEL || !foodData.needsFood()) {
            AUTO_EATING_PLAYERS.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AUTO_EATING_PLAYERS.remove(serverPlayer.getUUID());
        }
    }

    private static boolean consumeSingleItemFromTop(ServerPlayer player, Inventory inventory, FoodData foodData) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            FoodProperties properties = stack.getFoodProperties(player);
            if (properties == null) {
                continue;
            }

            foodData.eat(properties.nutrition(), properties.saturation());
            stack.shrink(1);
            sendEatChat(player, stack.getCount());
            return true;
        }

        return false;
    }

    private static void sendEatChat(ServerPlayer player, int remainingInStack) {
        player.sendSystemMessage(Component.literal("[AutoEat] Remaining in stack: " + remainingInStack));
        player.sendSystemMessage(Component.literal("[AutoEat] " + randomEatMessage()));
    }

    private static String randomEatMessage() {
        return EAT_MESSAGES.get(ThreadLocalRandom.current().nextInt(EAT_MESSAGES.size()));
    }

    private static List<String> loadEatMessages() {
        try (InputStream stream = AutoEatEvents.class.getResourceAsStream(MESSAGES_RESOURCE)) {
            if (stream == null) {
                AutoEat.LOGGER.warn("Could not find {}. Falling back to default messages.", MESSAGES_RESOURCE);
                return List.of("AutoEat active.");
            }

            List<String> lines = new ArrayList<>(512);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        lines.add(trimmed);
                    }
                }
            }

            if (lines.isEmpty()) {
                AutoEat.LOGGER.warn("{} is empty. Falling back to default messages.", MESSAGES_RESOURCE);
                return List.of("AutoEat active.");
            }

            return Collections.unmodifiableList(lines);
        } catch (IOException exception) {
            AutoEat.LOGGER.warn("Failed reading {}. Falling back to default messages.", MESSAGES_RESOURCE, exception);
            return List.of("AutoEat active.");
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AUTO_EATING_PLAYERS.remove(serverPlayer.getUUID());
        }
    }
}
