package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 【亡灵序曲】：用带有该附魔的武器击杀敌对生物时，有概率把它变成临时友军，10 秒后消散。
 */
@EventBusSubscriber(modid = MyriadBloom.MODID)
public final class RequiemHandler {
    /** 每级提供的转化概率。 */
    private static final float CHANCE_PER_LEVEL = 0.2F;
    /** 友军存活时间：10 秒。 */
    private static final int ALLY_LIFETIME = 200;

    private RequiemHandler() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        if (!(killer.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Monster)) {
            return;
        }
        ItemStack weapon = killer.getMainHandItem();
        int level$ = enchantLevel(level, weapon);
        if (level$ <= 0 || killer.getRandom().nextFloat() > CHANCE_PER_LEVEL * level$) {
            return;
        }
        var copy = victim.getType().create(level);
        if (!(copy instanceof Mob mob)) {
            return;
        }
        mob.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        mob.setPersistenceRequired();
        mob.setHealth(mob.getMaxHealth());
        // 让它去打附近的敌对生物
        var enemy = level.getEntitiesOfClass(Monster.class, mob.getBoundingBox().inflate(16.0D),
                        candidate -> candidate != mob && candidate.isAlive())
                .stream()
                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (enemy != null) {
            mob.setTarget(enemy);
        }
        level.addFreshEntity(mob);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                mob.getX(), mob.getY() + 1.0D, mob.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.02D);
        level.getServer().tell(new TickTask(ALLY_LIFETIME, mob::discard));
    }

    private static int enchantLevel(ServerLevel level, ItemStack stack) {
        var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        return registry.getHolder(MyriadBloomApi.id("requiem"))
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
