package nro.models.player;

import nro.models.mob.Mob;
import nro.models.services.PlayerService;
import nro.models.utils.Util;

/**
 * Clone tạo bởi skill Phân Thân (skill 28).
 * Skin y hệt master. AI chủ động đánh mob + boss gần nhất.
 * Mirror attack khi master tấn công mục tiêu cụ thể.
 */
public class PhanThanClone extends NewPet {

    private static final int IDLE_ATTACK_RANGE       = 400;
    private static final int IDLE_ATTACK_COOLDOWN_MS = 2000;

    public final int powerPercent;
    private long lastTimeIdleAttack = 0;

    public PhanThanClone(Player master, int powerPercent, int index) {
        // Skin y hệt master tại thời điểm tạo (bản sao chính chủ)
        super(master, master.getHead(), master.getBody(), master.getLeg());
        this.powerPercent = powerPercent;
        this.name   = master.name + "#" + index;
        this.gender = master.gender;
        if (master.location != null) {
            this.location.x = master.location.x + Util.nextInt(-80, 80);
            this.location.y = master.location.y;
        }
        setupStats();
    }

    private void setupStats() {
        if (master == null || master.nPoint == null) return;
        long pct = powerPercent;
        this.nPoint.hpMax = (int) Math.max(1000L, master.nPoint.hpMax * pct / 100L);
        this.nPoint.hp    = this.nPoint.hpMax;
        this.nPoint.mpMax = (int) Math.max(100L,  master.nPoint.mpMax * pct / 100L);
        this.nPoint.mp    = this.nPoint.mpMax;
        this.nPoint.dame  = (int) Math.max(1L,    master.nPoint.dame  * pct / 100L);
        this.nPoint.def   = (int) Math.max(0L,    master.nPoint.def   * pct / 100L);
        this.nPoint.crit  = master.nPoint.crit;
        this.nPoint.speed = master.nPoint.speed;
    }

    /**
     * Mirror attack — gọi từ SkillService.mirrorClonesAttack()
     * khi master tấn công mục tiêu cụ thể (mob hoặc player/boss).
     */
    public void mirrorAttack(Player plTarget, Mob mobTarget) {
        try {
            if (isDie() || this.zone == null) return;
            if (plTarget != null && !plTarget.isDie()) {
                PlayerService.gI().playerMove(this,
                    plTarget.location.x + Util.nextInt(-45, 45),
                    plTarget.location.y);
                plTarget.injured(this, Math.max(1L, (long) this.nPoint.dame), false, false);
            }
            if (mobTarget != null && !mobTarget.isDie()) {
                PlayerService.gI().playerMove(this,
                    mobTarget.location.x + Util.nextInt(-30, 30),
                    mobTarget.location.y);
                mobTarget.injured(this, Math.max(1L, (long) this.nPoint.dame), false);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Mirror skill AoE — gọi khi master dùng skill không có mục tiêu cụ thể.
     * Clone tự tìm và tấn công mục tiêu gần nhất (mob hoặc boss).
     */
    public void mirrorSkill() {
        try {
            if (isDie() || this.zone == null) return;
            attackNearest();
            lastTimeIdleAttack = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    @Override
    public void update() {
        try {
            if (master == null || master.zone == null) { dispose(); return; }
            if (isDie()) { dispose(); return; }
            if (this.zone == null || this.zone != master.zone) { joinMapMaster(); return; }
            if (master.isDie()) { dispose(); return; }
            // AI chủ động tấn công khi cooldown cho phép
            if (Util.canDoWithTime(lastTimeIdleAttack, IDLE_ATTACK_COOLDOWN_MS)) {
                attackNearest();
                lastTimeIdleAttack = System.currentTimeMillis();
            }
        } catch (Exception ignored) {}
    }

    /** Tìm và tấn công mục tiêu gần nhất: mob HOẶC boss */
    private void attackNearest() {
        if (this.zone == null) return;

        // Tìm mob thường gần nhất
        Mob nearestMob = null;
        int minDist = IDLE_ATTACK_RANGE + 1;
        for (Mob mob : new java.util.ArrayList<>(this.zone.mobs)) {
            if (mob == null || mob.isDie()) continue;
            int d = Util.getDistance(this, mob);
            if (d < minDist) { minDist = d; nearestMob = mob; }
        }

        // Tìm boss gần nhất (Boss là Player với isBoss=true, nằm trong zone.bosses)
        Player nearestBoss = null;
        for (Player boss : this.zone.getBosses()) {
            if (boss == null || boss.isDie()) continue;
            int d = Util.getDistance(this, boss);
            if (d < minDist) { minDist = d; nearestBoss = boss; nearestMob = null; }
        }

        if (nearestMob != null) {
            PlayerService.gI().playerMove(this,
                nearestMob.location.x + Util.nextInt(-25, 25),
                nearestMob.location.y);
            nearestMob.injured(this, Math.max(1L, (long) this.nPoint.dame), false);
        } else if (nearestBoss != null) {
            PlayerService.gI().playerMove(this,
                nearestBoss.location.x + Util.nextInt(-25, 25),
                nearestBoss.location.y);
            nearestBoss.injured(this, Math.max(1L, (long) this.nPoint.dame), false, false);
        }
    }

    @Override
    public void dispose() {
        if (master != null && master.phanThanClones != null)
            master.phanThanClones.remove(this);
        super.dispose();
    }
}
