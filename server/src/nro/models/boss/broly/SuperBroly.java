package nro.models.boss.broly;

import nro.consts.ConstRatio;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.player.Player;
import nro.server.ServerNotify;
import nro.services.PetService;
import nro.services.SkillService;
import nro.services.func.ChangeMapService;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.Util;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class SuperBroly extends Broly {

    public SuperBroly() {
        super(BossFactory.SUPER_BROLY, BossData.SUPER_BROLY);
        this.nPoint.defg = (short) (this.nPoint.hpg / 1000);
        if (this.nPoint.defg < 0) {
            this.nPoint.defg = (short) -this.nPoint.defg;
        }
    }

    public SuperBroly(int id, BossData data) {
        super(id, data);
        this.nPoint.defg = (short) (this.nPoint.hpg / 1000);
        if (this.nPoint.defg < 0) {
            this.nPoint.defg = (short) -this.nPoint.defg;
        }
    }

    @Override
    public void attack() {
        try {
            if (!charge()) {
                Player pl = getPlayerAttack();
                if (pl != null) {
                        this.playerSkill.skillSelect = this.getSkillAttack();
                        if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                            if (Util.isTrue(15, ConstRatio.PER100) && SkillUtil.isUseSkillChuong(this)) {
                                goToXY(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)),
                                        Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50), false);
                            }
                            this.effectCharger();
                            try {
                                SkillService.gI().useSkill(this, pl, null, null);
                            } catch (Exception e) {
                                Log.error(SuperBroly.class, e);
                            }
                        } else {
                            goToPlayer(pl, false);
                        }
                        if (Util.isTrue(5, ConstRatio.PER100)) {
                            this.changeIdle();
                        }
                    }
            }
        } catch (Exception ex) {
            Log.error(SuperBroly.class, ex);
        }
    }

    @Override
    public void joinMap() {
        this.zone = getMapCanJoin(mapJoin[Util.nextInt(0, mapJoin.length - 1)]);
        int x = Util.nextInt(50, this.zone.map.mapWidth - 50);
        ChangeMapService.gI().changeMap(this, this.zone, x, this.zone.map.yPhysicInTop(x, 0));
        ServerNotify.gI().notify("Boss " + this.name + " vừa xuất hiện tại " + this.zone.map.mapName + "");
        Pet_Broly.getInstance().init(this, this.location.x, this.location.y);
    }

    @Override
    public Player getPlayerAttack() throws Exception {
        if (countChangePlayerAttack < targetCountChangePlayerAttack
                && plAttack != null && plAttack.zone != null && plAttack.zone.equals(this.zone)
                && !plAttack.effectSkin.isVoHinh) {
            if (!plAttack.isDie()) {
                this.countChangePlayerAttack++;
                return plAttack;
            } else {
                plAttack = null;
            }
        } else {
            this.targetCountChangePlayerAttack = Util.nextInt(10, 20);
            this.countChangePlayerAttack = 0;
            plAttack = this.zone.getRandomPlayerInMap();
        }
        return plAttack;
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
    }

    @Override
    public void die() {
        this.secondTimeRestToNextTimeAppear = 200; //15p
        super.die();
    }

    @Override
    public void rewards(Player pl) {
//        for (int i = 0; i < 20; i++) {
  //          this.dropItemReward(568, (int) pl.id);
    //    }
        if (pl.pet == null) {
            PetService.gI().createNormalPet(pl);
        }
        generalRewards(pl);
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

}
