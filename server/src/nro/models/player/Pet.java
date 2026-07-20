package nro.models.player;

import nro.consts.ConstPlayer;
import nro.models.item.CaiTrang;
import nro.models.mob.Mob;
import nro.models.skill.Skill;
import nro.server.Manager;
import nro.server.io.Message;
import nro.services.*;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;
import nro.utils.Util;

/**
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 */
public class Pet extends Player {

    private static final short ARANGE_CAN_ATTACK = 200;
    private static final short ARANGE_ATT_SKILL1 = 50;

    private static final short[][] PET_ID = {{285, 286, 287}, {288, 289, 290}, {282, 283, 284}, {304, 305, 303}};

    public static final byte FOLLOW = 0;
    public static final byte PROTECT = 1;
    public static final byte ATTACK = 2;
    public static final byte GOHOME = 3;
    public static final byte FUSION = 4;
    public static boolean ANGRY;

    public Player master;
    public byte status = 0;
    
    public byte typePet;
     
    public boolean isMabu;

    public boolean isGokuSSJ4;

    public boolean isVegetaSSJ4;

    public boolean isSuperPicolo;
    
    public boolean isBabyFide;

    public boolean isBabyKidbu;
    
    public boolean isBabyCell;
    
    public boolean isTransform;

    public long lastTimeDie;

    private boolean goingHome;

    private Mob mobAttack;
    private Player playerAttack;

    private static final int TIME_WAIT_AFTER_UNFUSION = 5000;
    private long lastTimeUnfusion;

    public byte getStatus() {
        return this.status;
    }

    @Override
    public int version() {
        return 214;
    }

    public Pet(Player master) {
        this.master = master;
        this.isPet = true;
    }

    public void changeStatus(byte status) {
        if (goingHome || master.fusion.typeFusion != 0 || (this.isDie() && status == FUSION)) {
            Service.getInstance().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        Service.getInstance().chatJustForMe(master, this, getTextStatus(status));
        if (status == GOHOME) {
            goHome();
        } else if (status == FUSION) {
            fusion(false);
        }
        this.status = status;
    }

    public void joinMapMaster() {
        if (!MapService.gI().isMapVS(master.zone.map.mapId) && !MapService.gI().isMapOfflineNe(master.zone.map.mapId) && !MapService.gI().isMapOffline(master.zone.map.mapId)) {
            if (status != GOHOME && status != FUSION && !isDie()) {
                this.location.x = master.location.x + Util.nextInt(-10, 10);
                this.location.y = master.location.y;
                MapService.gI().goToMap(this, master.zone);
                this.zone.load_Me_To_Another(this);
            }
        } else {
            MapService.gI().goToMap(this, MapService.gI().getMapCanJoin(this, master.gender + 39));
            this.zone.load_Me_To_Another(this);
        }
    }

    public void goHome() {
        if (this.status == GOHOME) {
            return;
        }
        goingHome = true;
        new Thread(() -> {
            try {
                if (master == null || master.isDisposed()) return;
                Pet.this.status = Pet.ATTACK;
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            if (master == null || master.isDisposed()) return;
            MapService.gI().goToMap(this, MapService.gI().getMapCanJoin(this, master.gender + 21));
            this.zone.load_Me_To_Another(this);
            Pet.this.status = Pet.GOHOME;
            goingHome = false;
        }).start();
    }

    private String getTextStatus(byte status) {
        switch (status) {
            case FOLLOW:
                return "Ok con theo sư phụ";
            case PROTECT:
                return "Ok con sẽ bảo vệ sư phụ";
            case ATTACK:
                return "Ok sư phụ để con lo cho";
            case GOHOME:
                return "Ok con về, bibi sư phụ";
            default:
                return "";
        }
    }

    public void fusion(boolean porata) {
        if (this.isDie()) {
            Service.getInstance().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (porata) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA;
            } else {
                master.fusion.lastTimeFusion = System.currentTimeMillis();
                master.fusion.typeFusion = ConstPlayer.LUONG_LONG_NHAT_THE;
                ItemTimeService.gI().sendItemTime(master, master.gender == ConstPlayer.NAMEC ? 3901 : 3790, Fusion.TIME_FUSION / 1000);
            }
            this.status = FUSION;
            exitMapFusion();
            fusionEffect(master.fusion.typeFusion);
            Service.getInstance().Send_Caitrang(master);
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.getInstance().point(master);
        } else {
            Service.getInstance().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void fusion2(boolean porata2) {
        if (this.isDie()) {
            Service.getInstance().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (porata2) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA2;
            } else {
                master.fusion.lastTimeFusion = System.currentTimeMillis();
                master.fusion.typeFusion = ConstPlayer.LUONG_LONG_NHAT_THE;
                ItemTimeService.gI().sendItemTime(master, master.gender == ConstPlayer.NAMEC ? 3901 : 3790, Fusion.TIME_FUSION / 1000);
            }
            this.status = FUSION;
            exitMapFusion();
            fusionEffect(master.fusion.typeFusion);
            Service.getInstance().Send_Caitrang(master);
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.getInstance().point(master);
        } else {
            Service.getInstance().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void unFusion() {
        master.fusion.typeFusion = 0;
        this.status = PROTECT;
        Service.getInstance().point(master);
        joinMapMaster();
        fusionEffect(master.fusion.typeFusion);
        Service.getInstance().Send_Caitrang(master);
        Service.getInstance().point(master);
        this.lastTimeUnfusion = System.currentTimeMillis();
    }

    private void fusionEffect(int type) {
        Message msg;
        try {
            msg = new Message(125);
            msg.writer().writeByte(type);
            msg.writer().writeInt((int) master.id);
            Service.getInstance().sendMessAllPlayerInMap(master, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private void exitMapFusion() {
        if (this.zone != null) {
            MapService.gI().exitMap(this);
        }
    }

    public long lastTimeMoveIdle;
    private int timeMoveIdle;
    public boolean idle;

    private void moveIdle() {
        if (status == GOHOME || status == FUSION) {
            return;
        }
        if (idle && Util.canDoWithTime(lastTimeMoveIdle, timeMoveIdle)) {
            int dir = this.location.x - master.location.x <= 0 ? -1 : 1;
            PlayerService.gI().playerMove(this, master.location.x
                    + Util.nextInt(dir == -1 ? 30 : -50, dir == -1 ? 50 : 30), master.location.y);
            lastTimeMoveIdle = System.currentTimeMillis();
            timeMoveIdle = Util.nextInt(5000, 8000);
        }
    }

    private long lastTimeMoveAtHome;
    private byte directAtHome = -1;

    @Override
     public void update() {
        try {
            super.update();
            increasePoint(); //cộng chỉ số
            updatePower(); //check mở skill...
            if (isDie()) {
                if (System.currentTimeMillis() - lastTimeDie > 100) {
                    Service.getInstance().hsChar(this, nPoint.hpMax, nPoint.mpMax);
                } else {
                    return;
                }
            }
            if (justRevived && this.zone == master.zone) {
                Service.getInstance().chatJustForMe(master, this, "Sư phụ ơi, con đây nè!");
                justRevived = false;
            }
            if (this.zone == null || this.zone != master.zone) {
                joinMapMaster();
            }
            if (master.isDie() || this.isDie() || effectSkill.isHaveEffectSkill()) {
                return;
            }
            moveIdle();
//            if (ANGRY) {
//                Player pl = this.zone.getPlayerInMap((int) playerAttack.id);
//                int disToPlayer = Util.getDistance(this, pl);
//                if (pl.isDie() || pl == null || pl.cFlag == 0) {
//                    playerAttack = null;
//                    ANGRY = false;
//                } else {
//                    if (playerAttack != null) {
//                        if (disToPlayer <= ARANGE_ATT_SKILL1) {
//                            //đấm
//                            this.playerSkill.skillSelect = getSkill(1);
//                            if (SkillService.gI().canUseSkillWithCooldown(this)) {
//                                if (SkillService.gI().canUseSkillWithMana(this)) {
//                                    PlayerService.gI().playerMove(this, pl.location.x + Util.nextInt(-20, 20), pl.location.y);
//                                    SkillService.gI().useSkill(this, pl, null, null);
//                                } else {
//                                    askPea();
//                                }
//                            }
//                        } else {
//                            if (disToPlayer <= ARANGE_CAN_ATTACK + 50) {
//                                this.playerSkill.skillSelect = getSkill(2);
//                                if (this.playerSkill.skillSelect.skillId != -1) {
//                                    if (SkillService.gI().canUseSkillWithCooldown(this)) {
//                                        if (SkillService.gI().canUseSkillWithMana(this)) {
//                                            SkillService.gI().useSkill(this, pl, null, null);
//                                        } else {
//                                            askPea();
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        idle = true;
//                    }
//                }
//            }
            switch (status) {
                case FOLLOW:
                    followMaster(60);
                    break;
                case PROTECT:
                    if (useSkill3() || useSkill4()) {
                        break;
                    }
                    mobAttack = findMobAttack();
                    if (mobAttack != null) {
                        int disToMob = Util.getDistance(this, mobAttack);
                        if (disToMob <= ARANGE_ATT_SKILL1) {
                            //đấm
                            this.playerSkill.skillSelect = getSkill(1);
                            if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                if (SkillService.gI().canUseSkillWithMana(this)) {
                                    PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                    SkillService.gI().useSkill(this, null, mobAttack, null);
                                } else {
                                    askPea();
                                }
                            }
                        } else {
                            //chưởng
                            this.playerSkill.skillSelect = getSkill(2);
                            if (this.playerSkill.skillSelect.skillId != -1) {
                                if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        SkillService.gI().useSkill(this, null, mobAttack, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            }
                        }

                    } else {
                        idle = true;
                    }
                    break;
                case ATTACK:
                    if (useSkill3() || useSkill4()) {
                        break;
                    }
                    mobAttack = findMobAttack();
                    if (mobAttack != null) {
                        int disToMob = Util.getDistance(this, mobAttack);
                        if (disToMob <= ARANGE_ATT_SKILL1) {
                            this.playerSkill.skillSelect = getSkill(1);
                            if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                if (SkillService.gI().canUseSkillWithMana(this)) {
                                    PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                    SkillService.gI().useSkill(this, null, mobAttack, null);
                                } else {
                                    askPea();
                                }
                            }
                        } else {
                            this.playerSkill.skillSelect = getSkill(2);
                            if (this.playerSkill.skillSelect.skillId != -1) {
                                if (SkillService.gI().canUseSkillWithMana(this)) {
                                    SkillService.gI().useSkill(this, null, mobAttack, null);
                                } else {
                                    askPea();
                                }
                            } else {
                                this.playerSkill.skillSelect = getSkill(1);
                                if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                        SkillService.gI().useSkill(this, null, mobAttack, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            }
                        }

                    } else {
                        idle = true;
                    }
                    break;

                case GOHOME:
                    if (this.zone != null && (this.zone.map.mapId == 21 || this.zone.map.mapId == 22 || this.zone.map.mapId == 23)) {
                        if (System.currentTimeMillis() - lastTimeMoveAtHome <= 5000) {
                            return;
                        } else {
                            if (this.zone.map.mapId == 21) {
                                if (directAtHome == -1) {
                                    PlayerService.gI().playerMove(this, 250, 336);
                                    directAtHome = 1;
                                } else {
                                    PlayerService.gI().playerMove(this, 200, 336);
                                    directAtHome = -1;
                                }
                            } else if (this.zone.map.mapId == 22) {
                                if (directAtHome == -1) {
                                    PlayerService.gI().playerMove(this, 500, 336);
                                    directAtHome = 1;
                                } else {
                                    PlayerService.gI().playerMove(this, 452, 336);
                                    directAtHome = -1;
                                }
                            } else if (this.zone.map.mapId == 22) {
                                if (directAtHome == -1) {
                                    PlayerService.gI().playerMove(this, 250, 336);
                                    directAtHome = 1;
                                } else {
                                    PlayerService.gI().playerMove(this, 200, 336);
                                    directAtHome = -1;
                                }
                            }
                            if (!this.isBoss) {
                                Service.getInstance().chatJustForMe(master, this, "Hello sư phụ!");
                            }
                            lastTimeMoveAtHome = System.currentTimeMillis();
                        }
                    }
                    break;
            }

//            }
        } catch (Exception e) {
//            Logger.logException(Pet.class, e);
        }
    }

    @Override
    public void dispose() {
        if (zone != null) {
            MapService.gI().exitMap(this);
        }
        this.mobAttack = null;
        this.master = null;
        super.dispose();
    }

    private long lastTimeAskPea;

    public void askPea() {
        if (this.isMabu && master.charms.tdDeTuMabu > System.currentTimeMillis()) {
            InventoryService.gI().eatPea(master);
        } else if (Util.canDoWithTime(lastTimeAskPea, 10000)) {
            if (!this.isBoss) {
                Service.getInstance().chatJustForMe(master, this, "Sư phụ ơi cho con đậu thần");
                InventoryService.gI().eatPea(master);
            }
            lastTimeAskPea = System.currentTimeMillis();
        }
    }

    private int countTTNL;

    private boolean useSkill3() {
        try {
            playerSkill.skillSelect = getSkill(3);
            if (playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            switch (this.playerSkill.skillSelect.template.id) {
                case Skill.THAI_DUONG_HA_SAN:
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, null);
                        if (!this.isBoss) {
                            Service.getInstance().chatJustForMe(master, this, "Thái dương hạ san");
                        }
                        return true;
                    }
                    return false;
                case Skill.TAI_TAO_NANG_LUONG:
                    if (this.effectSkill.isCharging && this.countTTNL < Util.nextInt(3, 5)) {
                        this.countTTNL++;
                        return true;
                    }
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)
                            && (this.nPoint.getCurrPercentHP() <= 20 || this.nPoint.getCurrPercentMP() <= 20)) {
                        SkillService.gI().useSkill(this, null, null, null);
                        this.countTTNL = 0;
                        return true;
                    }
                    return false;
                case Skill.KAIOKEN:
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        mobAttack = this.findMobAttack();
                        if (mobAttack == null) {
                            return false;
                        }
                        int dis = Util.getDistance(this, mobAttack);
                        if (dis > ARANGE_ATT_SKILL1) {
                            PlayerService.gI().playerMove(this, mobAttack.location.x, mobAttack.location.y);
                        } else {
                            if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                                PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                            }
                        }
                        SkillService.gI().useSkill(this, playerAttack, mobAttack, null);
                        getSkill(1).lastTimeUseThisSkill = System.currentTimeMillis();
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean useSkill4() {
        try {
            this.playerSkill.skillSelect = getSkill(4);
            if (this.playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            switch (this.playerSkill.skillSelect.template.id) {
                case Skill.BIEN_KHI:
                    if (!this.effectSkill.isMonkey && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, null);
                        return true;
                    }
                    return false;
                case Skill.KHIEN_NANG_LUONG:
                    if (!this.effectSkill.isShielding && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, null);
                        return true;
                    }
                    return false;
                case Skill.DE_TRUNG:
                    if (this.mobMe == null && SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, null, null);
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private long lastTimeIncreasePoint;

    private void increasePoint() {
        if (status != FUSION) {
            this.nPoint.increasePoint((byte) Util.nextInt(0, 2), (short) 10);
        }
    }

    public void followMaster() {
        if (this.isDie() || effectSkill.isHaveEffectSkill()) {
            return;
        }
        switch (this.status) {
            case ATTACK:
                if (ANGRY) {
                    followMaster(80);
                } else {
                    if ((mobAttack != null && Util.getDistance(this, master) <= 500)) {
                        break;
                    }
                }
            case FOLLOW:
            case PROTECT:
                followMaster(60);
                break;
        }
    }

    private void followMaster(int dis) {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= dis) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, dis);
            } else {
                this.location.x = mX + Util.nextInt(0, dis);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    public short getAvatar() {
        if (this.isMabu) {
            return 297;
        }
        if (this.isGokuSSJ4) {
            return 1382;
        }
        if (this.isVegetaSSJ4) {
            return 1385;
        }
        if (this.isBabyFide) {
            return 1464;
        }
        if (this.isBabyKidbu) {
            return 1467;
        }
        if (this.isBabyCell) {
            return 1470;    
        } else {
            return PET_ID[3][this.gender];
        }
    }

    @Override
    public short getHead() {
        if (effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        } else if (effectSkill.isSocola || effectSkin.isSocola) {
            return 412;
        } else if (effectSkin != null && effectSkin.isHoaDa) {
            return 454;
        } else if (this.isMabu && !this.isTransform) {
            return 297;
        } else if (this.isGokuSSJ4) {
            return 1382;
        } else if (this.isVegetaSSJ4) {
            return 1385;
        } else if (this.isBabyFide) {
            return 1464;
        } else if (this.isBabyKidbu) {
            return 1467;    
        } else if (this.isBabyCell) {
            return 1470;    
        } else if (inventory.itemsBody.get(5).isNotNullItem()) {
            CaiTrang ct = Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
            if (ct != null) {
                return (short) ((short) ct.getID()[0] != -1 ? ct.getID()[0] : inventory.itemsBody.get(5).template.part);
            }
        }
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][0];
        } else {
            return PET_ID[3][this.gender];
        }
    }

    @Override
    public short getBody() {
        if (effectSkill.isMonkey) {
            return 193;
        } else if (effectSkill.isSocola || effectSkin.isSocola) {
            return 413;
        } else if (effectSkin != null && effectSkin.isHoaDa) {
            return 455;
        } else if (this.isMabu && !this.isTransform) {
            return 298;
        } else if (this.isGokuSSJ4) {
            return 1383;
        } else if (this.isVegetaSSJ4) {
            return 1386;
        } else if (this.isBabyFide && !this.isTransform) {
            return 1465;
        } else if (this.isBabyKidbu && !this.isTransform) {
            return 1468;
        } else if (this.isBabyCell && !this.isTransform) {
            return 1471;
        
        } else if (inventory.itemsBody.get(5).isNotNullItem()) {
            CaiTrang ct = Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
            if (ct != null && ct.getID()[1] != -1) {
                return (short) ct.getID()[1];
            }
        }
        if (inventory.itemsBody.get(0).isNotNullItem()) {
            return inventory.itemsBody.get(0).template.part;
        }
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][1];
        } else {
            return (short) (gender == ConstPlayer.NAMEC ? 59 : 57);
        }
    }

    @Override
    public short getLeg() {
        if (effectSkill.isMonkey) {
            return 194;
        } else if (effectSkill.isSocola || effectSkin.isSocola) {
            return 414;
        } else if (effectSkin != null && effectSkin.isHoaDa) {
            return 456;
        } else if (this.isMabu && !this.isTransform) {
            return 299;
        } else if (this.isGokuSSJ4) {
            return 1384;
        } else if (this.isVegetaSSJ4) {
            return 1387;
        } else if (this.isBabyFide) {
            return 1466;
        } else if (this.isBabyKidbu) {
            return 1469;
        } else if (this.isBabyCell) {
            return 1472;
        
        } else if (inventory.itemsBody.get(5).isNotNullItem()) {
            CaiTrang ct = Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
            if (ct != null && ct.getID()[2] != -1) {
                return (short) ct.getID()[2];
            }
        }
        if (inventory.itemsBody.get(1).isNotNullItem()) {
            return inventory.itemsBody.get(1).template.part;
        }

        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][2];
        } else {
            return (short) (gender == ConstPlayer.NAMEC ? 60 : 58);
        }
    }

    private Mob findMobAttack() {
        int dis = ARANGE_CAN_ATTACK;
        Mob mobAtt = null;
        for (Mob mob : zone.mobs) {
            if (mob.isDie()) {
                continue;
            }
            int d = Util.getDistance(this, mob);
            if (d <= dis) {
                dis = d;
                mobAtt = mob;
            }
        }
        return mobAtt;
    }

    @Override
    public void setSession(nro.server.io.Session session) {
    }

    @Override
    public void sendMessage(nro.server.io.Message msg) {
    }

    public Skill getSkill(int index) {
        if (index >= 1 && index <= playerSkill.skills.size()) {
            return playerSkill.skills.get(index - 1);
        }
        return SkillUtil.createSkillLevel0(-1);
    }

    public void updatePower() {
        if (this.playerSkill != null) {
            int sizeSkill = this.playerSkill.skills.size();
            if (sizeSkill > 0) {
                if (sizeSkill < 2 && this.nPoint.power >= 1500000) {
                    openedSkill(1);
                } else if (sizeSkill < 3 && this.nPoint.power >= 15000000) {
                    openedSkill(2);
                } else if (sizeSkill < 4 && this.nPoint.power >= 150000000) {
                    openedSkill(3);
                } else if (sizeSkill < 5 && this.nPoint.power >= 1500000000) {
                    openedSkill(4);
                }
            }
        }
    }

    public void openedSkill(int index) {
        if (playerSkill.skills.size() > index) {
            return;
        }
        int gender = this.gender;
        short skillId = -1;
        switch (index) {
            case 1: //skill 2
                skillId = (short) (gender == ConstPlayer.TRAI_DAT ? Skill.KAMEJOKO : (gender == ConstPlayer.NAMEC ? Skill.MASENKO : Skill.ANTOMIC));
                break;
            case 2: //skill 3
                skillId = (short) (gender == ConstPlayer.TRAI_DAT ? Skill.KAIOKEN : (gender == ConstPlayer.NAMEC ? Skill.TAI_TAO_NANG_LUONG : Skill.THAI_DUONG_HA_SAN));
                break;
            case 3: //skill 4
                skillId = (short) (gender == ConstPlayer.TRAI_DAT ? Skill.BIEN_KHI : (gender == ConstPlayer.NAMEC ? Skill.DE_TRUNG : Skill.KHIEN_NANG_LUONG));
                break;
            case 4: //skill 5
                skillId = (short) (gender == ConstPlayer.TRAI_DAT ? Skill.TU_SAT : (gender == ConstPlayer.NAMEC ? Skill.MAKANKOSAPPO : Skill.TAI_TAO_NANG_LUONG));
                break;
        }
        if (skillId != -1) {
            Skill skill = SkillUtil.createSkill(skillId, 1);
            playerSkill.skills.add(skill);
            Service.getInstance().chatJustForMe(master, this, "Sư phụ ơi, con vừa học được kỹ năng " + skill.template.name);
        }
    }

    @Override
    public void move(int x, int y) {
        this.location.x = x;
        this.location.y = y;
        PlayerService.gI().playerMove(this, x, y);
    }
}
