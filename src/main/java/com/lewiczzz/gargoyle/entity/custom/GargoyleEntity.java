package com.lewiczzz.gargoyle.entity.custom;

import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GargoyleEntity extends AnimalEntity {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public GargoyleEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createGargoyleAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // Jest z kamienia
                .add(EntityAttributes.GENERIC_JUMP_STRENGTH, 1.2); // Bardzo wysoki skok
    }


    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new PerchGoal(this,1.0D));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));

        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0D, 0.001F));

        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));


    }


    @Override
    public void tick() {
        super.tick();

        // LOGIKA SZYBOWANIA (jak kurczak)
        // Jeśli nie jest na ziemi i spada (velocity Y < 0)
        if (!this.isOnGround() && this.getVelocity().y < 0) {
            this.setVelocity(this.getVelocity().multiply(1.0, 0.6, 1.0));
        }

        // LOGIKA AGRESJI PRZY UPADKU
        // Wykonujemy co 10 ticków (0.5 sekundy), żeby nie obciążać serwera
        if (!this.getWorld().isClient && this.age % 10 == 0) {
            checkNearbyFalls();
        }
    }

    // Wykrywanie graczy/mobów, które otrzymały obrażenia od upadku
    private void checkNearbyFalls() {
        // Pobierz zasięg 10 kratek wokół
        Box box = this.getBoundingBox().expand(15.0);
        List<LivingEntity> nearbyEntities = this.getWorld().getEntitiesByClass(LivingEntity.class, box, entity -> entity != this);

        for (LivingEntity entity : nearbyEntities) {
            // Sprawdź, czy entity otrzymało obrażenia w ostatnim czasie (hurtTime > 0)
            // I czy źródłem obrażeń był upadek (FALL)
            if (entity.hurtTime > 0 && entity.getRecentDamageSource() != null) {
                if (entity.getRecentDamageSource().isOf(DamageTypes.FALL)) {
                    // Stań się agresywny wobec niezdary
                    this.setTarget(entity);
                    // Opcjonalnie: wydaj dźwięk
                    // this.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.0f);
                    break; // Namierzamy pierwszego znalezionego
                }
            }
        }
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }
}
