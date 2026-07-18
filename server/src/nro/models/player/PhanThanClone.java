package nro.models.player;

import nro.models.mob.Mob;
import nro.models.services.PlayerService;
import nro.models.utils.Util;
import java.util.ArrayList;

/**
 * Clone tạo bởi skill Phân Thân (skill 28).
 * Tự động tìm mob gần nhất trong zone và tấn công mỗi 2.5s.
 * Stats = master × powerPercent%.
 */
public class PhanThanClone extends NewPet {

    private static final int ATTACK_RANGE       = 350;
    private static final int ATTACK_COOLDOWN_MS = 2500;

    public final int powerPercent;
    private long lastTimeAttack = 0;

    /**
     * @param master       Người dùng skill
     * @param powerPercent Phần trăm sức mạnh so với master (20-90)
     * @param index        Thứ tự clone (1-based, dùng cho tên hiển thị)
     */
    public PhanThanClone(Player master, int powerPercent, int index) {
        super(master, master.getHead(), master.getBody(), master.getLeg());
        this.powerPercent = powerPercent;
        this.name  = master.name + "#" + index;
        this.gender = master.gender;

        // Vị trí ban đầu: lệch nhỏ so với master
        if (master.location != null) {
            this.location.x = master.location.x + Util.nextInt(-80, 80);
            this.location.y = master.location.y;
        }

        setupStats();
    }

    /** Copy stats từ master rồi scale theo powerPercent */
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
            // Tự join zone master nếu bị lạc
            if (this.zone == null || this.zone != master.zone) {
                joinMapMaster();
                return;
            }
            // Master chết → clone tan biến
            if (master.isDie()) {
                dispose();
                return;
            }
            // Tấn công mob gần nhất theo cooldown
            if (Util.canDoWithTime(lastTimeAttack, ATTACK_COOLDOWN_MS)) {
                attackNearestMob();
                lastTimeAttack = System.currentTimeMillis();
            }
        } catch (Exception ignored) {}
    }

    private void attackNearestMob() {
        if (this.zone == null) return;

        Mob nearestMob = null;
        int minDist = Integer.MAX_VALUE;

        for (Mob mob : new ArrayList<>(this.zone.mobs)) {
            if (mob == null || mob.isDie()) continue;
            int dist = Util.getDistance(this, mob);
            if (dist <= ATTACK_RANGE && dist < minDist) {
                minDist = dist;
                nearestMob = mob;
            }
        }

        if (nearestMob != null) {
            // Di chuyển sát mục tiêu
            PlayerService.gI().playerMove(this,
                nearestMob.location.x + Util.nextInt(-25, 25),
                nearestMob.location.y);
            // Gây dame dựa trên nPoint.dame của clone
            long damage = Math.max(1L, (long) this.nPoint.dame);
            nearestMob.injured(this, damage, false);
        }
    }

    @Override
    public void dispose() {
        // Xóa khỏi danh sách clone của master trước khi dispose
        if (master != null && master.phanThanClones != null) {
            master.phanThanClones.remove(this);
        }
        super.dispose();
    }
}
