/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nro.models.boss.boss_khi_gas_huy_diet;

import nro.consts.ConstPlayer;
import nro.consts.ConstRatio;
import nro.consts.MapName;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.player.Player;
import nro.services.SkillService;
import nro.utils.Util;

import nro.models.boss.Boss;
import nro.models.boss.BossManager;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.map.phoban.KhiGas;
import nro.models.skill.Skill;
import nro.services.EffectSkillService;
import nro.services.PlayerService;
import nro.services.RewardService;
import nro.services.Service;
import nro.services.func.ChangeMapService;
import nro.utils.Log;

/**
 *
 * @author kitakeyos - OBITO
 */
public class DrLychee extends BossKhiGasHuyDiet {

    private boolean isDie = false;

    public DrLychee(KhiGas khigas) {
        super(BossFactory.DR_LYCHEE, BossData.DR_LYCHEE, khigas);
    }

    @Override
    public void update() {
        try {
            long HPMax = ((long) this.nPoint.hpMax * 5) / 100;
            if (this.nPoint.hp < HPMax) {
                PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.NON_PK);
            }
            this.immortalMp();
            this.nPoint.update();
            switch (this.status) {
                case RESPAWN:
                    respawn();
                    break;
                case JUST_RESPAWN:
                    this.changeStatus(REST);
                    break;
                case REST:
                    this.changeStatus(JUST_JOIN_MAP);
                    break;
                case JUST_JOIN_MAP:
                    joinMap();
                    if (this.zone != null) {
                        changeStatus(TALK_BEFORE);
                    }
                    break;
                case TALK_BEFORE:
                    if (talk()) {
                        if (!this.joinMapIdle) {
                            changeToAttack();
                        } else {
                            this.changeStatus(IDLE);
                        }
                    }
                    break;
                case ATTACK:
                    this.talk();
                    if (this.playerSkill.prepareTuSat || this.playerSkill.prepareLaze
                            || this.playerSkill.prepareQCKK) {
                        break;
                    } else {
                        this.attack();
                    }
                    break;
                case IDLE:
                    this.idle();
                    break;
                case DIE:
                    if (this.joinMapIdle) {
                        this.changeToIdle();
                    }
                    changeStatus(TALK_AFTER);
                    break;
                case TALK_AFTER:
                    if (talk()) {
                        changeStatus(LEAVE_MAP);
                    }
                    break;
                case LEAVE_MAP:
                    isDie = true;
                    if (this.nPoint.hp < this.nPoint.hpMax) {
                        useskillttnl();
                        useskillttnl();
                    } else {
                        this.lastTimeStartLeaveMap = System.currentTimeMillis();
                        changeStatus(DO_NOTHING);
                    }
                    break;
                case DO_NOTHING:
                    if (Util.canDoWithTime(lastTimeStartLeaveMap, 5000)) {
                        rewards(plAttack);
                        leaveMap();
                        this.changeStatus(RESPAWN);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.error(Boss.class, e);
        }
    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        int dame = 0;
        if (this.isDie()) {
            return dame;
        } else {
            dame = super.injured(plAtt, damage, piercing, isMobAttack);
            long ptHPMax = ((long) this.nPoint.hpMax * 5) / 10;
            if (this.nPoint.hp < dame) {
                dame = (int) (this.nPoint.hp - ptHPMax);
            }
            if (this.isDie()) {
                rewards(plAtt);
                notifyPlayeKill(plAtt);
                die();
                EffectSkillService.gI().removeMaPhongBa(this);
            }

            return dame;
        }
    }

    @Override
    public void attack() {
        try {
            if (this.typePk == ConstPlayer.NON_PK) {
                changeStatus(TALK_AFTER);
                return;
            }
            Player pl = getPlayerAttack();
            this.playerSkill.skillSelect = this.getSkillAttack();
            if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                if (Util.isTrue(10, ConstRatio.PER100)) {
                    goToXY(pl.location.x + Util.nextInt(-20, 20), Util.nextInt(pl.location.y - 80, this.zone.map.yPhysicInTop(pl.location.x, 0)), false);
                }
                SkillService.gI().useSkill(this, pl, null, null);
                checkPlayerDie(pl);
            } else {
                goToPlayer(pl, false);
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void initTalk() {
        this.textTalkBefore = new String[]{"Ta đợi các ngươi mãi", "Bọn xayda các ngươi mau đền tội đi"};
        this.textTalkMidle = new String[]{"Đại bác báo thù...", "Heyyyyyyyy yaaaaa", "Hahaha", "ai da"};
        this.textTalkAfter = new String[]{"Các ngươi khá lắm", "Hatchiyack sẽ thay ta báo thù"};
    }

    private void immortalMp() {
        this.nPoint.mp = this.nPoint.mpg;
    }

    protected boolean useskillttnl() {
        this.playerSkill.skillSelect = getSkillSpecial();
        if (SkillService.gI().canUseSkillWithCooldown(this)) {
            SkillService.gI().useSkill(this, null, null, null);
            return true;
        } else {
            return false;
        }
    }
    

    @Override
    public void joinMap() {
        try {
            this.zone = khiGas.getMapById(148);
            ChangeMapService.gI().changeMap(this, this.zone, 513, 480);
            System.out.println("Đã join map");
        } catch (Exception e) {
        }
    }
    public void leaveMap() {
        ChangeMapService.gI().spaceShipArrive(this, (byte) 2, ChangeMapService.TENNIS_SPACE_SHIP);
        super.leaveMap();
        BossManager.gI().removeBoss(this);
    }
}
