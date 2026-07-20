/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.models.sieu_hang;

import nro.consts.ConstPlayer;
import nro.consts.ConstRatio;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.services.SkillService;
import nro.services.func.ChangeMapService;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.Util;

/**
 *
 * @author Administrator
 */
public class CloneSieuHang extends Boss {

    public CloneSieuHang(Player plAttack, BossData data) {
        super(BossFactory.CLONE_PLAYER, data);
        playerAtt = plAttack;
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public void rewards(Player pl) {

    }

    @Override
    public void idle() {
    }

    @Override
    public void initTalk() {
    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        int dame = 0;
        if (this.isDie()) {
            return dame;
        } else if (plAtt == null) {
            return 0;
        } else if (!plAtt.equals(playerAtt)) {
            return dame;
        } else {
            dame = super.injured(plAtt, damage, piercing, isMobAttack);
            if (this.isDie()) {
                rewards(plAtt);
                notifyPlayeKill(plAtt);
                die();
            }
            return dame;
        }
    }
    
    private Skill getSkill() {
        for (Skill skill : this.playerSkill.skills) {
            if (skill.template.id == Skill.KHIEN_NANG_LUONG) {
                return skill;
            }
        }

        return null;
    }

    @Override
    public void attack() {
        try {
            if (playerAtt != null && playerAtt.zone != null && this.zone != null && this.zone.equals(playerAtt.zone)) {
                if (this.isDie()) {
                    return;
                }
                if (this.effectSkill.isHaveEffectSkill()) {
                    return;
                }
                if (playerAtt.typePk == ConstPlayer.NON_PK) {
                    return;
                }
                
                this.playerSkill.skillSelect = this.getSkill();

                if (!this.effectSkill.isShielding && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    SkillService.gI().useSkill(this, null, null);
                } else {
                    this.playerSkill.skillSelect = this.getSkillAttack();
                    if (Util.getDistance(this, playerAtt) <= this.getRangeCanAttackWithSkillSelect()) {
                        if (Util.isTrue(15, ConstRatio.PER100) && SkillUtil.isUseSkillChuong(this)) {
                            goToXY(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)),
                                    Util.nextInt(10) % 2 == 0 ? playerAtt.location.y : playerAtt.location.y - Util.nextInt(0, 50), false);
                        }
                        SkillService.gI().useSkill(this, playerAtt, null);
                        checkPlayerDie(playerAtt);
                    } else {
                        goToPlayer(playerAtt, false);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void immortalMp() {
        this.nPoint.mp = this.nPoint.mpg;
    }

    @Override
    public void update() {
        super.update();
        try {
            if (!this.effectSkill.isHaveEffectSkill()
                    && !this.effectSkill.isCharging) {
                this.immortalMp();
                switch (this.status) {
                    case JUST_JOIN_MAP:
                        joinMap();
                        if (this.zone != null) {
                            changeStatus(ATTACK);
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
                }
            }
        } catch (Exception e) {
            Log.error(CloneSieuHang.class, e);
        }
    }

    @Override
    public void joinMap() {
        if (playerAtt.zone != null) {
            this.zone = playerAtt.zone;
            ChangeMapService.gI().changeMap(this, this.zone, 435, 264);
        }
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        BossManager.gI().removeBoss(this);
    }

    @Override
    public void checkPlayerDie(Player pl) {
    }

}
