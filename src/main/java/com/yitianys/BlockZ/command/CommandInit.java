package com.yitianys.BlockZ.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.item.BackpackItem;
import com.yitianys.BlockZ.item.ClothingItem;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncGridRulesS2C;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import com.yitianys.BlockZ.util.ItemSizeManager;

import com.yitianys.BlockZ.entity.CorpseEntity;
import net.minecraft.world.entity.Entity;
import java.util.Collection;

@Mod.EventBusSubscriber(modid = BlockZ.MODID)
public class CommandInit {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("blockz_clear_corpse")
            .requires(source -> source.hasPermission(2)) // 需要管理员权限
            .executes(context -> clearCorpse(context.getSource(), null))
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(context -> clearCorpse(context.getSource(), EntityArgument.getEntities(context, "targets")))
            )
        );

        dispatcher.register(Commands.literal("blockz_reload")
            .requires(source -> source.hasPermission(2)) // 需要管理员权限
            .executes(context -> reloadConfig(context.getSource()))
        );

        dispatcher.register(Commands.literal("blockz_pockets")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("slots", IntegerArgumentType.integer(0, 27))
                .executes(context -> {
                    var source = context.getSource();
                    int count = IntegerArgumentType.getInteger(context, "slots");
                    for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                        if (player.containerMenu instanceof com.yitianys.BlockZ.menu.DayZInventoryMenu) player.closeContainer();
                    }
                    BlockZConfigs.initialPocketSlots.set(count);
                    BlockZConfigs.initialPocketSlots.save();
                    com.yitianys.BlockZ.event.ModEvents.broadcastServerConfigs(source.getServer());
                    source.sendSuccess(() -> Component.translatable("msg.blockz.command.pockets_updated", count), true);
                    return count;
                })));

        dispatcher.register(Commands.literal("blockz_grid_item")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("width", IntegerArgumentType.integer(1, 16))
                .then(Commands.argument("height", IntegerArgumentType.integer(1, 16))
                    .executes(context -> updateHeldItemGridRule(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "width"),
                        IntegerArgumentType.getInteger(context, "height"),
                        null
                    ))
                    .then(Commands.argument("color", StringArgumentType.greedyString())
                        .executes(context -> updateHeldItemGridRule(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "width"),
                            IntegerArgumentType.getInteger(context, "height"),
                            StringArgumentType.getString(context, "color")
                        ))
                    )
                )
            )
        );

        dispatcher.register(Commands.literal("blockz_clothing_capacity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("width", IntegerArgumentType.integer(1, 16))
                .then(Commands.argument("height", IntegerArgumentType.integer(1, 16))
                    .executes(context -> updateHeldClothingCapacityRule(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "width"),
                        IntegerArgumentType.getInteger(context, "height")
                    ))
                )
            )
        );

        dispatcher.register(Commands.literal("cap")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("hand")
                .then(Commands.argument("width", IntegerArgumentType.integer(1, 9))
                    .then(Commands.argument("height", IntegerArgumentType.integer(1, 256))
                        .executes(context -> updateHeldCapacityShape(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "width"),
                            IntegerArgumentType.getInteger(context, "height")
                        ))
                    )
                )
            )
        );

        dispatcher.register(Commands.literal("size")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("hand")
                .then(Commands.argument("slots", IntegerArgumentType.integer(1, 256))
                    .executes(context -> updateHeldMaximumSlots(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "slots")
                    ))
                )
            )
        );
    }

    private static int clearCorpse(CommandSourceStack source, Collection<? extends Entity> targets) {
        int count = 0;
        if (targets == null) {
            // 清理所有尸体实体
            for (Entity entity : source.getLevel().getEntities().getAll()) {
                if (entity instanceof CorpseEntity) {
                    entity.discard();
                    count++;
                }
            }
        } else {
            // 清理指定的实体（如果是尸体）
            for (Entity entity : targets) {
                if (entity instanceof CorpseEntity) {
                    entity.discard();
                    count++;
                }
            }
        }
        
        final int finalCount = count;
        source.sendSuccess(() -> Component.translatable("msg.blockz.command.corpses_cleared", finalCount), true);
        return count;
    }

    private static int reloadConfig(CommandSourceStack source) {
        try {
            ItemSizeManager.loadCustomSizes();
            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), SyncGridRulesS2C.createServerSnapshot());
            
            source.sendSuccess(() -> Component.translatable("msg.blockz.command.reload_success"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.reload_failed"));
            e.printStackTrace();
            return 0;
        }
    }

    private static int updateHeldItemGridRule(CommandSourceStack source, int width, int height, String rawColor) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.isEmpty()) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_empty_hand"));
                return 0;
            }

            String normalizedColor = rawColor == null ? null : rawColor.trim();
            Integer parsedColor = null;
            if (normalizedColor != null && !normalizedColor.isEmpty()) {
                parsedColor = ItemSizeManager.parseColorString(normalizedColor);
                if (parsedColor == null) {
                    source.sendFailure(Component.translatable("msg.blockz.command.grid_item_invalid_color", normalizedColor));
                    return 0;
                }
            }

            final Integer finalParsedColor = parsedColor;
            final Object colorDisplay = finalParsedColor == null
                    ? Component.translatable("msg.blockz.command.grid_item_default_color")
                    : normalizedColor;

            Item item = heldStack.getItem();
            if (!ItemSizeManager.saveItemRule(heldStack, width, height, finalParsedColor)) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
                return 0;
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), SyncGridRulesS2C.createServerSnapshot());

            source.sendSuccess(() -> Component.translatable(
                "msg.blockz.command.grid_item_updated",
                heldStack.getHoverName(),
                width,
                height,
                colorDisplay
            ), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
            BlockZ.LOGGER.error("Failed to update held item grid rule", e);
            return 0;
        }
    }

    private static int updateHeldClothingCapacityRule(CommandSourceStack source, int width, int height) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.isEmpty()) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_empty_hand"));
                return 0;
            }

            if (!ItemSizeManager.saveCapacityRule(heldStack, width, height)) {
                source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
                return 0;
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), SyncGridRulesS2C.createServerSnapshot());

            source.sendSuccess(() -> Component.translatable(
                "msg.blockz.command.grid_item_updated_capacity",
                heldStack.getHoverName(),
                width,
                height,
                width * height
            ), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("msg.blockz.command.grid_item_save_failed"));
            BlockZ.LOGGER.error("Failed to update held clothing capacity rule", e);
            return 0;
        }
    }

    private static int updateHeldCapacityShape(CommandSourceStack source, int width, int height) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("msg.blockz.command.player_only"));
            return 0;
        }

        ItemStack heldStack = player.getMainHandItem();
        if (!isSupportedStorageItem(heldStack)) {
            source.sendFailure(Component.translatable(heldStack.isEmpty()
                ? "msg.blockz.command.grid_item_empty_hand"
                : "msg.blockz.command.capacity_not_storage"));
            return 0;
        }

        int area = width * height;
        if (area > 256) {
            source.sendFailure(Component.translatable("msg.blockz.command.capacity_too_large", 256));
            return 0;
        }

        int resultingSlots = ItemSizeManager.hasMaximumSlotsOverride(heldStack)
            ? ItemSizeManager.getCustomSlots(heldStack)
            : area;
        if (hasStoredItemOutsideCapacity(heldStack, resultingSlots)) {
            source.sendFailure(Component.translatable("msg.blockz.command.capacity_contains_overflow", resultingSlots));
            return 0;
        }

        ItemSizeManager.setCapacityShapeOverride(heldStack, width, height);
        syncHeldStack(player);
        source.sendSuccess(() -> Component.translatable(
            "msg.blockz.command.capacity_shape_updated", heldStack.getHoverName(), width, height, resultingSlots
        ), true);
        return 1;
    }

    private static int updateHeldMaximumSlots(CommandSourceStack source, int slots) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("msg.blockz.command.player_only"));
            return 0;
        }

        ItemStack heldStack = player.getMainHandItem();
        if (!isSupportedStorageItem(heldStack)) {
            source.sendFailure(Component.translatable(heldStack.isEmpty()
                ? "msg.blockz.command.grid_item_empty_hand"
                : "msg.blockz.command.capacity_not_storage"));
            return 0;
        }
        if (hasStoredItemOutsideCapacity(heldStack, slots)) {
            source.sendFailure(Component.translatable("msg.blockz.command.capacity_contains_overflow", slots));
            return 0;
        }

        ItemSizeManager.setMaximumSlotsOverride(heldStack, slots);
        syncHeldStack(player);
        source.sendSuccess(() -> Component.translatable(
            "msg.blockz.command.maximum_slots_updated", heldStack.getHoverName(), slots
        ), true);
        return 1;
    }

    private static boolean isSupportedStorageItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof BackpackItem
            || stack.getItem() instanceof ClothingItem
            || stack.getItem() instanceof ArmorItem
            || BlockZConfigs.getBackpackSlots(stack) > 0
            || stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    private static boolean hasStoredItemOutsideCapacity(ItemStack stack, int capacity) {
        var handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler != null) {
            for (int slot = capacity; slot < handler.getSlots(); slot++) {
                if (!handler.getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }

        CompoundTag root = stack.getTag();
        return root != null
            && (hasSerializedOverflow(root.getCompound("Inventory"), capacity)
                || hasSerializedOverflow(root.getCompound("inventory"), capacity));
    }

    private static boolean hasSerializedOverflow(CompoundTag inventory, int capacity) {
        if (!inventory.contains("Items", Tag.TAG_LIST)) {
            return false;
        }
        ListTag items = inventory.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index);
            if (item.contains("Slot", Tag.TAG_ANY_NUMERIC)
                && item.getInt("Slot") >= capacity
                && !ItemStack.of(item).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void syncHeldStack(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
}
