package nro.models.mob;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.server.io.Message;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.SkillService;
import nro.utils.Util;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class MobEffectSkill {

    private final Mob mob;

    public MobEffectSkill(Mob mob) {
        this.mob = mob;
    }

    public long lastTimeStun;
    public int timeStun;
    public boolean isStun;

    public long lastTimeDameMaFuBa;

    public void update() {
        boolean isDie = mob.isDie();
        if (isStun && (Util.canDoWithTime(lastTimeStun, timeStun) || isDie)) {
            removeStun();
        }
        if (isThoiMien && (Util.canDoWithTime(lastTimeThoiMien, timeThoiMien) || isDie)) {
            removeThoiMien();
        }
        if (isBlindDCTT && (Util.canDoWithTime(lastTimeBlindDCTT, timeBlindDCTT)) || isDie) {
            removeBlindDCTT();
        }
        if (isSocola && (Util.canDoWithTime(lastTimeSocola, timeSocola) || isDie)) {
            removeSocola();
        }
        if (isMaPhongBaMOB && (Util.canDoWithTime(lastTimeMaPhongBaMOB, timeMaPhongBaMOB) || isDie)) {
            removeMaPhongBaMOB();
        }
        if (isAnTroi && (Util.canDoWithTime(lastTimeAnTroi, timeAnTroi) || isDie)) {
            removeAnTroi();
        }
        if (isMaPhongBaMOB) {
            for (int i = 0; i < this.timeMaPhongBaMOB / 1000; i++) {
                if (!this.mob.isDie()) {
                    if (Util.canDoWithTime(lastTimeDameMaFuBa, 1000)) {
                        SkillService.gI().playerAttackMob(this.mob.zone.findPlayerByID(this.mob.idPlayerMaFuBa), this.mob, false, false); // trừ dame
                        lastTimeDameMaFuBa = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    public boolean isHaveEffectSkill() {
        return isAnTroi || isBlindDCTT || isStun || isThoiMien;
    }

    public void startStun(long lastTimeStartBlind, int timeBlind) {
        this.lastTimeStun = lastTimeStartBlind;
        this.timeStun = timeBlind;
        isStun = true;
    }

    public void removeMaPhongBaMOB() {
        Message msg;
        this.isMaPhongBaMOB = false;
        try {
            msg = new Message(-112);
            msg.writer().writeByte(0);
            msg.writer().writeByte(mob.id);
            Service.gI().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private void removeStun() {
        isStun = false;
        Message msg;
        try {
            msg = new Message(-124);
            msg.writer().writeByte(0);
            msg.writer().writeByte(1);
            msg.writer().writeByte(40);
            msg.writer().writeByte(mob.id);
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }
    public boolean isThoiMien;
    public long lastTimeThoiMien;
    public int timeThoiMien;

    public void setThoiMien(long lastTimeThoiMien, int timeThoiMien) {
        this.isThoiMien = true;
        this.lastTimeThoiMien = lastTimeThoiMien;
        this.timeThoiMien = timeThoiMien;
    }

    public void removeThoiMien() {
        this.isThoiMien = false;
        Message msg;
        try {
            msg = new Message(-124);
            msg.writer().writeByte(0); //b5
            msg.writer().writeByte(1); //b6
            msg.writer().writeByte(41); //num6
            msg.writer().writeByte(mob.id); //b7
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public boolean isBlindDCTT;
    public long lastTimeBlindDCTT;
    public int timeBlindDCTT;

    public void setStartBlindDCTT(long lastTimeBlindDCTT, int timeBlindDCTT) {
        this.isBlindDCTT = true;
        this.lastTimeBlindDCTT = lastTimeBlindDCTT;
        this.timeBlindDCTT = timeBlindDCTT;
    }

    public void removeBlindDCTT() {
        this.isBlindDCTT = false;
        Message msg;
        try {
            msg = new Message(-124);
            msg.writer().writeByte(0);
            msg.writer().writeByte(1);
            msg.writer().writeByte(40);
            msg.writer().writeByte(mob.id);
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public boolean isAnTroi;
    public long lastTimeAnTroi;
    public int timeAnTroi;

    public void setTroi(long lastTimeAnTroi, int timeAnTroi) {
        this.lastTimeAnTroi = lastTimeAnTroi;
        this.timeAnTroi = timeAnTroi;
        this.isAnTroi = true;
    }

    public void removeAnTroi() {
        isAnTroi = false;
        Message msg;
        try {
            msg = new Message(-124);
            msg.writer().writeByte(0); //b4
            msg.writer().writeByte(1);//b5
            msg.writer().writeByte(32);//num8
            msg.writer().writeByte(mob.id);//b6
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    public boolean isSocola;
    private long lastTimeSocola;
    private int timeSocola;

    public boolean isMaPhongBaMOB;
    private long lastTimeMaPhongBaMOB;
    private int timeMaPhongBaMOB;

    public void removeSocola() {
        Message msg;
        this.isSocola = false;
        try {
            msg = new Message(-112);
            msg.writer().writeByte(0);
            msg.writer().writeByte(mob.id);
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    public void setSocola(long lastTimeSocola, int timeSocola) {
        this.lastTimeSocola = lastTimeSocola;
        this.timeSocola = timeSocola;
        this.isSocola = true;
    }

    public void setMaPhongBaMOB(long lastTimeMaPhongBaMOB, int timeMaPhongBaMOB) {
        this.lastTimeMaPhongBaMOB = lastTimeMaPhongBaMOB;
        this.timeMaPhongBaMOB = timeMaPhongBaMOB;
        this.isMaPhongBaMOB = true;
    }
}
