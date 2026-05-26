package com.bossscaler.mixin;

import com.bossscaler.config.BossScalerConfig;
import com.bossscaler.data.BossState;
import com.bossscaler.data.BossStateManager;
import com.bossscaler.compat.SpellResistanceCompat;
import com.bossscaler.util.BossHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void onDropLoot(DamageSource damageSource, boolean causedByPlayer, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!BossHelper.isBoss(self)) return;

        BossState state = BossStateManager.get(self.getUuid());
        if (state == null) return;

        BossScalerConfig config = BossScalerConfig.get();

        // ── Shared loot disabled: fall through to vanilla drop ────────────
        if (!config.sharedLoot) {
            BossStateManager.remove(self.getUuid());
            return; // vanilla dropLoot runs normally
        }

        // ── Shared loot enabled: give each qualifying player their own roll ─
        // Copy to avoid mutating the unmodifiable sets from BossState
        Set<UUID> attackers = new HashSet<>(state.getAttackedPlayers());
        Set<UUID> qualifying;
        if (config.strictLootRadius) {
            qualifying = new HashSet<>(state.getActiveLootPlayers());
            qualifying.retainAll(attackers);
        } else {
            qualifying = attackers;
        }

        if (qualifying.size() <= 1) {
            BossStateManager.remove(self.getUuid());
            return; // only 1 (or 0) players — vanilla drop is fine
        }

        ci.cancel();

        LootTable lootTable = serverWorld.getServer()
                .getLootManager().getLootTable(self.getLootTable());

        for (UUID playerId : qualifying) {
            ServerPlayerEntity player = serverWorld.getServer()
                    .getPlayerManager().getPlayer(playerId);
            if (player == null) continue;

            LootContextParameterSet params = new LootContextParameterSet.Builder(serverWorld)
                    .add(LootContextParameters.THIS_ENTITY, self)
                    .add(LootContextParameters.ORIGIN, self.getPos())
                    .add(LootContextParameters.DAMAGE_SOURCE, damageSource)
                    .addOptional(LootContextParameters.KILLER_ENTITY, damageSource.getAttacker())
                    .addOptional(LootContextParameters.DIRECT_KILLER_ENTITY, damageSource.getSource())
                    .addOptional(LootContextParameters.LAST_DAMAGE_PLAYER, player)
                    .build(LootContextTypes.ENTITY);

            List<ItemStack> drops = lootTable.generateLoot(params);
            Vec3d pos = player.getPos();

            for (ItemStack stack : drops) {
                ItemEntity item = new ItemEntity(serverWorld, pos.x, pos.y, pos.z, stack);
                item.setVelocity(
                        (serverWorld.random.nextDouble() - 0.5) * 0.1,
                        0.2,
                        (serverWorld.random.nextDouble() - 0.5) * 0.1
                );
                serverWorld.spawnEntity(item);
            }
        }

        BossStateManager.remove(self.getUuid());
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (BossHelper.isBoss(self)) {
            SpellResistanceCompat.removeResistance(self);
            BossStateManager.remove(self.getUuid());
        }
    }
}
