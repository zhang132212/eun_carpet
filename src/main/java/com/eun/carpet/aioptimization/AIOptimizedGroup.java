package com.eun.carpet.aioptimization;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIOptimizedGroup {
    private final List<Mob> members = new ArrayList<>();
    private Mob retained;
    private int checkCooldown = 0;
    private Vec3 lastRetainedPos = Vec3.ZERO;

    public void addMember(Mob mob) {
        members.add(mob);
        ((AIOptimizedEntity) mob).eun$setGroup(this);
    }

    public void removeMember(Mob mob) {
        members.remove(mob);
        ((AIOptimizedEntity) mob).eun$setGroup(null);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public void clear() {
        for (Mob mob : members) {
            ((AIOptimizedEntity) mob).eun$setGroup(null);
            ((AIOptimizedEntity) mob).eun$setOptimized(false);
        }
        members.clear();
        retained = null;
    }

    public void optimize(ServerLevel level, int checkInterval) {
        if (members.isEmpty()) return;
        Random rand = new Random();
        retained = members.get(rand.nextInt(members.size()));
        for (Mob mob : members) {
            boolean isRetained = (mob == retained);
            ((AIOptimizedEntity) mob).eun$setOptimized(!isRetained);
        }
        lastRetainedPos = retained.position();
        checkCooldown = checkInterval;
    }

    public void tick(ServerLevel level, int checkInterval, double moveThreshold) {
        if (retained == null || retained.isRemoved()) {
            // 重新选举
            if (!members.isEmpty()) {
                retained = members.get(0);
                ((AIOptimizedEntity) retained).eun$setOptimized(false);
                for (Mob mob : members) {
                    if (mob != retained) ((AIOptimizedEntity) mob).eun$setOptimized(true);
                }
                lastRetainedPos = retained.position();
                checkCooldown = checkInterval;
            }
            return;
        }

        checkCooldown--;
        if (checkCooldown <= 0) {
            checkCooldown = checkInterval;
            Vec3 currentPos = retained.position();
            double dist = currentPos.distanceTo(lastRetainedPos);
            if (dist > moveThreshold) {
                // 解除优化
                for (Mob mob : members) {
                    ((AIOptimizedEntity) mob).eun$setOptimized(false);
                }
                members.clear();
                retained = null;
            } else {
                lastRetainedPos = currentPos;
            }
        }
    }

    public boolean containsRetained(Mob mob) {
        return retained == mob;
    }
}