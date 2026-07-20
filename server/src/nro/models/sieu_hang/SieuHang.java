package nro.models.sieu_hang;

import lombok.Getter;
import lombok.Setter;
import nro.consts.ConstMap;
import nro.consts.ConstPlayer;
import nro.manager.SieuHangManager;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.map.Zone;
import nro.models.map.challenge.MartialCongressService;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.services.*;
import nro.services.func.ChangeMapService;
import nro.utils.Util;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SieuHang {

    @Getter
    @Setter
    private int id;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Setter
    @Getter
    private Player player;

    @Setter
    private Boss boss;

    private boolean closed;

    @Setter
    private int time;

    @Setter
    private int timeWait;

    @Setter
    private SieuHangModel rankBoss;

    @Setter
    private SieuHangModel rankPlayer;

    public static Zone getMapChalllenge(int mapId) {
        lock.readLock().lock();
        try {
            Zone map = MapService.gI().getMapWithRandZone(mapId);
            if (map.getNumOfBosses() < 1) {
                return map;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update() {
        try {
            if (time > 0) {
                time--;
                if (!player.isDie()) {
                    if (boss.isDie()) {
                        boss.leaveMap();
                        endChallenge(player);
                    }
                    if (player.location.y > 264 && time > 10) {
                        leave(boss);
                    }
                } else {
                    endChallenge(boss);
                }
            } else {
                timeOut(boss);
            }
            if (timeWait > 0) {
                if (timeWait == 10) {
                    ready();
                }
                timeWait--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ready() {
        Util.setTimeout(() -> {
            MartialCongressService.gI().sendTypePK(player, boss);
            PlayerService.gI().changeAndSendTypePK(this.player, ConstPlayer.PK_PVP);
            boss.typePk = ConstPlayer.PK_ALL;
            boss.setStatus((byte) 3);
        }, 10000);
    }

    private void timeOut(Player plWin) {
        endChallenge(plWin);
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void initClonePlayer(Player plClone) {
        PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.NON_PK);
        BossData data = getBossDataFromPlayer(plClone);
        CloneSieuHang bossA = new CloneSieuHang(player, data);
        bossA.typePk = ConstPlayer.NON_PK;
        bossA.setStatus((byte) 71);
        bossA.joinMap();

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Service.getInstance().chat(bossA, "Sẵn sàng chưa?");
                Thread.sleep(1000);
                Service.getInstance().chat(player, "OK");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        PlayerService.gI().setPos(player, 335, 264, 0);
        setTimeWait(11);
        setBoss(bossA);
        setTime(185);
    }

    public static BossData getBossDataFromPlayer(Player pl) {
        List<Skill> skills = pl.playerSkill.skills.stream().filter(s -> s != null && (s.point > 0 || s.template.id == Skill.KHIEN_NANG_LUONG)).toList();
        int[][] skillTemp = new int[skills.size()][3];
        for (byte i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            skillTemp[i][0] = skill.template.id;
//            skillTemp[i][1] = skill.point == 0 ? 7 : skill.point;
            skillTemp[i][1] = 7;
            skillTemp[i][2] = skill.coolDown;
        }
        BossData boss = new BossData(
                pl.name, //name
                pl.gender, //gender
                Boss.DAME_NORMAL, //type dame
                Boss.HP_NORMAL, //type hp
                pl.nPoint.getDameAttack(false), //dame
                new int[][]{{pl.nPoint.hpMax}}, //hp
                new short[]{pl.getHead(), pl.getBody(), pl.getLeg()}, //outfit
                new short[]{113}, //map join
                skillTemp,
                0
        );
        return boss;
    }

    public void endChallenge(Player plWin) {
        reward(plWin);
        PlayerService.gI().hoiSinh(player);
        PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.NON_PK);
        if (player != null && player.zone != null && player.zone.map.mapId == ConstMap.DAI_HOI_VO_THUAT_113) {
            Util.setTimeout(() -> {
                ChangeMapService.gI().changeMapNonSpaceship(player, ConstMap.DAI_HOI_VO_THUAT_113, player.location.x, 360);
            }, 500);
        }
        if (boss != null) {
            boss.leaveMap();
        }

        SieuHangManager.UpdateStatusFight(rankBoss.player_id, 1);

        this.closed = true;
    }

    public void leave(Player plWin) {
        setTime(0);
        EffectSkillService.gI().removeStun(player);
        endChallenge(plWin);
    }

    private void reward(Player plWin) {
        int status = 0;

        if (player.equals(plWin)) {
            swap(rankPlayer, rankBoss);
            Service.getInstance().sendThongBao(plWin, "Chúc mừng " + plWin.name + " đã lên hạng " + rankPlayer.rank);
            status = 1;

            SieuHangManager.UpdateBXH(rankPlayer, rankBoss);
        } else {
            Service.getInstance().chat(player, "Thua rồi");
            int turn = SieuHangManager.GetFreeTurn(player);
            if (turn > 0) {
                SieuHangManager.UpdateTurn(player.id);
            }
        }

        SieuHangManager.InsertHistory(rankPlayer.player_id, rankBoss.player_id, status, rankPlayer.rank, rankBoss.rank);
    }

    public void swap(SieuHangModel plWin, SieuHangModel plLose) {
        lock.writeLock().lock();
        try {
            if (plWin.rank > plLose.rank) {
                int rankWin = plWin.rank;
                plWin.rank = plLose.rank;
                plLose.rank = rankWin;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
