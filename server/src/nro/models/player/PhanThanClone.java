package nro.models.player;

import nro.models.mob.Mob;
import nro.models.services.PlayerService;
import nro.models.utils.Util;

/**
 * Clone tạo bởi skill Phân Thân (skill 28).
 *
 * Hành vi: GƯƠNG CHIẾU master — khi master đánh mục tiêu nào,
 * clone đánh đúng mục tiêu đó với dame = master × powerPercent%.
 *
 * Không tự chọn mục tiêu độc lập (trừ auto-idle tìm mob khi master đứng yên).
 * Mirror được kích hoạt từ SkillService.mirrorClonesAttack().
 */
public class PhanThanClone extends NewPet {

    // Cooldown độc lập khi master không attack (fallback)
    private static final int IDLE_ATTACK_RANGE       = 350;
    private static final int IDLE_ATTACK_COOLDOWN_MS = 3000;

    public final int powerPercent;
    private long lastTimeIdleAttack = 0;

    /**
     * @param master       Người dùng skill Phân Thân
     * @param powerPercent Phần trăm sức mạnh so với master (20-90)
     * @param index        Thứ tự clone (1-based)
     */
    public PhanThanClone(Player master, int powerPercent, int index) {
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

    /** Sao chép chỉ số từ master, scale theo powerPercent */
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
     * MIRROR ATTACK — được gọi từ SkillService.mirrorClonesAttack()
     * mỗi khi master tấn công. Clone đánh đúng cùng mục tiêu.
     */
    public void mirrorAttack(Player plTarget, Mob mobTarget) {
        try {
            if (isDie() || this.zone == null) return;

            if (plTarget != null && !plTarget.isDie()) {
                PlayerService.gI().playerMove(this,
                    plTarget.location.x + Util.nextInt(-45, 45),
                    plTarget.location.y);
                long damage = Math.max(1L, (long) this.nPoint.dame);
                plTarget.injured(this, damage, false, false);
            }
            if (mobTarget != null && !mobTarget.isDie()) {
                PlayerService.gI().playerMove(this,
                    mobTarget.location.x + Util.nextInt(-30, 30),
                    mobTarget.location.y);
                long damage = Math.max(1L, (long) this.nPoint.dame);
                mobTarget.injured(this, damage, false);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void update() {
        try {
            if (master == null || master.zone == null) {
                dispose();
                return;
            }
            if (isDie()) {
                dispose();
                return;
            }
            // Sync zone với master
            if (this.zone == null || this.zone != master.zone) {
                joinMapMaster();
                return;
            }
            if (master.isDie()) {
                dispose();
                return;
            }
            // Idle fallback: khi master không đánh, tự tìm mob gần nhất
            if (Util.canDoWithTime(lastTimeIdleAttack, IDLE_ATTACK_COOLDOWN_MS)) {
                idleAttackMob();
                lastTimeIdleAttack = System.currentTimeMillis();
            }
        } catch (Exception ignored) {}
    }

    /** Tự tấn công mob gần nhất khi master đứng yên (idle behavior) */
    private void idleAttackMob() {
        if (this.zone == null) return;
        // Chỉ tự đánh khi master cũng không đang đánh (doesNotAttack)
        if (master != null && !master.doesNotAttack) return;

        Mob nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Mob mob : new java.util.ArrayList<>(this.zone.mobs)) {
            if (mob == null || mob.isDie()) continue;
            int dist = Util.getDistance(this, mob);
            if (dist <= IDLE_ATTACK_RANGE && dist < minDist) {
                minDist = dist;
                nearest = mob;
            }
        }
        if (nearest != null) {
            PlayerService.gI().playerMove(this,
                nearest.location.x + Util.nextInt(-25, 25),
                nearest.location.y);
            nearest.injured(this, Math.max(1L, (long) this.nPoint.dame), false);
        }
    }

    @Override
    public void dispose() {
        if (master != null && master.phanThanClones != null) {
            master.phanThanClones.remove(this);
        }
        super.dispose();
    }
}
