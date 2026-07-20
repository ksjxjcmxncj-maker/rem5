package nro.models.boss.robotsatthu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.services.RewardService;
import nro.services.Service;
import nro.services.TaskService;
import nro.utils.Util;

/**
 *
 * @author ❤Girlkun75❤
 * @copyright ❤Obito - Đâu Phải Tuấn❤
 */
public class Android13 extends Boss {

    public Android13() {
        super(BossFactory.ANDROID_13, BossData.ANDROID_13);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        BossFactory.createBoss(BossFactory.ANDROID_14).zone = this.zone;
        BossFactory.createBoss(BossFactory.ANDROID_15).zone = this.zone;
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public void rewards(Player pl) {
        TaskService.gI().checkDoneTaskKillBoss(pl, this);
    }

    @Override
    public void idle() {
    }

    @Override
    public void checkPlayerDie(Player pl) {
    }

    @Override
    public void initTalk() {
    }

}
