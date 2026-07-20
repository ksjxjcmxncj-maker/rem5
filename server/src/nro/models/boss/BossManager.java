package nro.models.boss;

import nro.utils.Log;
import java.util.ArrayList;
import java.util.List;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.services.MapService;
import nro.services.Service;
import nro.services.func.ChangeMapService;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class BossManager {

    public static final List<Boss> BOSSES_IN_GAME;
    private static BossManager intance;

    static {
        BOSSES_IN_GAME = new ArrayList<>();
    }

    public void updateAllBoss() {
        for (int i = BOSSES_IN_GAME.size() - 1; i >= 0; i--) {
            try {
                Boss boss = BOSSES_IN_GAME.get(i);
                if (boss != null) {
                    boss.update();
                }
            } catch (Exception e) {
                Log.error(BossManager.class, e);
            }
        }

    }
    public void FindBoss(Player player, int id) {
        Boss boss = BossManager.gI().getBossById(id);
        if (boss != null && boss.zone != null && boss.zone.map != null && !boss.isDie()) {
            Zone z = MapService.gI().getMapCanJoin(player, boss.zone.map.mapId, boss.zone.zoneId);
            if (z.getNumOfPlayers() < z.maxPlayer) {
                ChangeMapService.gI().changeMap(player, boss.zone, boss.location.x, boss.location.y);
            } else {
                Service.getInstance().sendThongBao(player, "Khu vực đang full.");
            }
        } else {
            Service.getInstance().sendThongBao(player, "");
        }
    }

    private BossManager() {

    }

    public static BossManager gI() {
        if (intance == null) {
            intance = new BossManager();
        }
        return intance;
    }
    
    public Boss getBossTau77ByPlayer(Player player) {
        for (int i = BOSSES_IN_GAME.size() - 1; i >= 0; i--) {
            if (BOSSES_IN_GAME.get(i).id == (-251003 - player.id - 2000)) {
                return BOSSES_IN_GAME.get(i);
            }
        }
        return null;
    }

    public Boss getBossById(int bossId) {
        for (int i = BOSSES_IN_GAME.size() - 1; i >= 0; i--) {
            if (BOSSES_IN_GAME.get(i).id == bossId) {
                return BOSSES_IN_GAME.get(i);
            }
        }
        return null;
    }

    public void addBoss(Boss boss) {
        boolean have = false;
        for (Boss b : BOSSES_IN_GAME) {
            if (boss.equals(b)) {
                have = true;
                break;
            }
        }
        if (!have) {
            BOSSES_IN_GAME.add(boss);

        }
    }

    public void removeBoss(Boss boss) {
        BOSSES_IN_GAME.remove(boss);
        boss.dispose();
    }
}
