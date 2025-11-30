package com.lewiczzz.gargoyle.entity.custom;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class PerchGoal extends Goal {
    private final GargoyleEntity mob;
    private final double speed;
    private double targetX;
    private double targetY;
    private double targetZ;

    public PerchGoal(GargoyleEntity mob, double speed){
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // Gargulec nie szuka miejsca, jeśli ma cel ataku lub już się rusza
        if (this.mob.getTarget() != null) return false;

        // Losowa szansa na uruchomienie (nie chce ciągle zmieniać krawędzi)
        if (this.mob.getRandom().nextInt(100) != 0) return false;

        BlockPos edgePos = findNearbyEdge();
        if (edgePos != null) {
            this.targetX = edgePos.getX() + 0.5;
            this.targetY = edgePos.getY();
            this.targetZ = edgePos.getZ() + 0.5;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return !this.mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
    }

    // Logika szukania krawędzi
    private BlockPos findNearbyEdge() {
        BlockPos mobPos = this.mob.getBlockPos();
        World world = this.mob.getWorld();

        // Szukamy w promieniu 8 kratek
        for (int i = 0; i < 20; i++) {
            int dx = this.mob.getRandom().nextInt(16) - 8;
            int dz = this.mob.getRandom().nextInt(16) - 8;
            int dy = this.mob.getRandom().nextInt(6) - 2;

            BlockPos candidate = mobPos.add(dx, dy, dz);

            // 1. Sprawdź czy kandydat jest solidny i czy nad nim jest powietrze (żeby tam stanąć)
            if (world.getBlockState(candidate).isSolid() && world.isAir(candidate.up())) {

                // 2. Sprawdź, czy któryś z sąsiadów to przepaść (powietrze + powietrze niżej)
                if (isDropOff(world, candidate.north()) ||
                        isDropOff(world, candidate.south()) ||
                        isDropOff(world, candidate.east()) ||
                        isDropOff(world, candidate.west())) {

                    return candidate.up(); // Zwracamy pozycję STANIA (czyli blok wyżej niż podłoga)
                }
            }
        }
        return null;
    }

    private boolean isDropOff(World world, BlockPos pos) {
        // Jest przepaść, jeśli dany blok jest powietrzem I 3 bloki w dół też są powietrzem
        return world.isAir(pos) && world.isAir(pos.down()) && world.isAir(pos.down(2));
    }
}
