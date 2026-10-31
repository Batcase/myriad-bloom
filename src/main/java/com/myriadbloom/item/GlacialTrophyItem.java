package com.myriadbloom.item;

import com.myriadbloom.BossContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 极冰战利品 (glacial trophy): the glacial conglomeration's head.
 *
 * <p>Worn as a helmet, exactly like the origin ape trophy, and right clicking a block places it on the
 * ground as a {@code myriadbloom:trophy} block.</p>
 */
public class GlacialTrophyItem extends Item implements Equipable {
    public GlacialTrophyItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_TURTLE;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.AQUA);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            var placeContext = new net.minecraft.world.item.context.BlockPlaceContext(context);
            var state = BossContent.TROPHY.get().getStateForPlacement(placeContext);
            if (state == null) {
                state = BossContent.TROPHY.get().defaultBlockState();
            }
            level.setBlock(pos, state.setValue(com.myriadbloom.world.block.TrophyBlock.GLACIAL, true), 3);
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
