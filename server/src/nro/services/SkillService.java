package nro.services;

import nro.consts.ConstAchive;
import nro.consts.ConstPlayer;
import nro.models.intrinsic.Intrinsic;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.mob.MobMe;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.pvp.PVP;
import nro.models.skill.Hit;
import nro.models.skill.Skill;
import nro.models.skill.SkillNotFocus;
import nro.server.io.Message;
import nro.services.func.PVPServcice;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.Util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import nro.models.player.PlayerClone;
import nro.models.sieu_hang.CloneSieuHang;
import nro.models.skill.SkillSpecial;
import nro.services.func.RadaService;

/**
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 */
public class SkillService {

    private static SkillService i;

    private SkillService() {

    }

    public static SkillService gI() {
        if (i == null) {
            i = new SkillService();
        }
        return i;
    }

    public boolean useSkill(Player player, Player plTarget, Mob mobTarget, Message message) {
        try {
            if (player.playerSkill.skillSelect == null && player.playerSkill == null && player.playerSkill.skillSelect.template == null) {
                return false;
            }
            if (player.clone != null) {
                useSkill(player.clone, plTarget, mobTarget, null);
            }
//            if (player.playerSkill.skillSelect.template.type == 2 && canUseSkillWithMana(player) && canUseSkillWithCooldown(player)) {
//                useSkillBuffToPlayer(player, plTarget);
//                return true;
//            }
            if ((player.effectSkill.isHaveEffectSkill()
                    && (player.playerSkill.skillSelect.template.id != Skill.TU_SAT
                    && player.playerSkill.skillSelect.template.id != Skill.QUA_CAU_KENH_KHI
                    && player.playerSkill.skillSelect.template.id != Skill.MAKANKOSAPPO))
                    || (plTarget != null && !canAttackPlayer(player, plTarget))
                    || (mobTarget != null && mobTarget.isDie())
                    || !canUseSkillWithMana(player) || !canUseSkillWithCooldown(player)) {
                return false;
            }
            if (player.effectSkill.useTroi) {
                EffectSkillService.gI().removeUseTroi(player);
            }
            if (player.effectSkill.isCharging) {
                EffectSkillService.gI().stopCharge(player);
            }
            if (player.isPet) {
            }
            byte st = -1;
            byte skillId = -1;
            Short dx = -1;
            Short dy = -1;
            byte dir = -1;
            Short x = -1;
            Short y = -1;

            try {
                st = message.reader().readByte();
                skillId = message.reader().readByte();
                dx = message.reader().readShort();
                dy = message.reader().readShort();
                dir = message.reader().readByte();
                x = message.reader().readShort();
                y = message.reader().readShort();
            } catch (Exception ex) {

            }
            if (st == 20 && skillId != player.playerSkill.skillSelect.template.id) {
                selectSkill(player, skillId);
                return false;
            }
            switch (player.playerSkill.skillSelect.template.type) {
                case 1:
                    useSkillAttack(player, plTarget, mobTarget);
                    break;
                case 3:
                    useSkillAlone(player);
                    break;
                case 4:
                    userSkillSpecial(player, st, skillId, dx, dy, dir, x, y);
                    break;
                default:
                    return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void userSkillSpecial(Player player, byte st, byte skillId, Short dx, Short dy, byte dir, Short x, Short y) {
        try {

            switch (skillId) {

                case Skill.PHAN_THAN:
                    useSkillAlone(player);
                    return;
                case Skill.SUPER_KAME:
                    if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                        for (int i = 1; i < player.inventory.itemsBody.get(7).itemOptions.size(); i++) {
                            if (player.inventory.itemsBody.get(7).itemOptions.get(i).optionTemplate.id == 231) {
                                if (player.inventory.itemsBody.get(7).itemOptions.get(i).param == 0) {
                                    Service.getInstance().sendThongBao(player, "Phục hồi sách hoặc tháo sách ra để dùng skill");
                                    return;
                                }
                            }
                        }
                        if (player.inventory.itemsBody.get(7).template.id == 1285) {
                            Service.getInstance().SendImgSkill9(skillId, 2);
                            sendEffSkillSpecialID24(player, dir, 2);
                            break;
                        } else {
                            Service.getInstance().SendImgSkill9(skillId, 3);
                            sendEffSkillSpecialID24(player, dir, 3);
                            break;
                        }
                    } else {
                        sendEffSkillSpecialID24(player, dir, 0);
                    }
                    break;
                case Skill.SUPER_ANTOMIC:
                    if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                        for (int i = 1; i < player.inventory.itemsBody.get(7).itemOptions.size(); i++) {
                            if (player.inventory.itemsBody.get(7).itemOptions.get(i).optionTemplate.id == 231) {
                                if (player.inventory.itemsBody.get(7).itemOptions.get(i).param == 0) {
                                    Service.getInstance().sendThongBao(player, "Phục hồi sách hoặc tháo sách ra để dùng skill");
                                    return;
                                }
                            }
                        }
                        if (player.inventory.itemsBody.get(7).template.id == 1289) {
                            Service.getInstance().SendImgSkill9(skillId, 2);// gửi ảnh tới cilent
                            sendEffSkillSpecialID25(player, dir, 2);
                            break;
                        } else {
                            Service.getInstance().SendImgSkill9(skillId, 3);// gửi ảnh tới cilent
                            sendEffSkillSpecialID25(player, dir, 3);
                            break;
                        }
                    } else {
                        sendEffSkillSpecialID25(player, dir, 0);
                    }
                    break;
                case Skill.MAFUBA:
                    if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                        for (int i = 1; i < player.inventory.itemsBody.get(7).itemOptions.size(); i++) {
                            if (player.inventory.itemsBody.get(7).itemOptions.get(i).optionTemplate.id == 231) {
                                if (player.inventory.itemsBody.get(7).itemOptions.get(i).param == 0) {
                                    Service.getInstance().sendThongBao(player, "Phục hồi sách hoặc tháo sách ra để dùng skill");
                                    return;
                                }
                            }
                        }
                        if (player.inventory.itemsBody.get(7).template.id == 1287) {
                            Service.getInstance().SendImgSkill9(skillId, 2);// gửi ảnh tới cilent
                            sendEffSkillSpecialID26(player, dir, 2);
                            break;
                        } else {
                            Service.getInstance().SendImgSkill9(skillId, 3);// gửi ảnh tới cilent
                            sendEffSkillSpecialID26(player, dir, 3);
                            break;
                        }
                    } else {
                        sendEffSkillSpecialID26(player, dir, 0);
                    }
                    break;
            }
            player.skillSpecial.setSkillSpecial(dir, dx, dy, x, y);
            affterUseSkill(player, player.playerSkill.skillSelect.template.id);

        } catch (Exception ex) {
        }
    }

    public void updateSkillSpecial(Player player) {
        try {
            player.zone.loadAnotherToMe(player);
            player.zone.load_Me_To_Another(player);
            if (player.isDie() || player.effectSkill.isHaveEffectSkill()) {
                player.skillSpecial.closeSkillSpecial();
                return;
            }
            if (player.skillSpecial.skillSpecial.template.id == Skill.MAFUBA) {
                if (Util.canDoWithTime(player.skillSpecial.lastTimeSkillSpecial, SkillSpecial.TIME_GONG)) {
                    player.skillSpecial.lastTimeSkillSpecial = System.currentTimeMillis();
                    player.skillSpecial.closeSkillSpecial();
                    Skill curSkill = SkillUtil.getSkillbyId(player, Skill.MAFUBA);
                    int timeBinh = SkillUtil.getTimeBinh(curSkill.point);//thời gian biến thành bình

                    //hút người
                    for (Player playerMap : player.zone.getHumanoids()) {
                        if (playerMap != null && playerMap != player) {
                            if (player.skillSpecial.dir == -1 && !playerMap.isDie() && Util.getDistance(player, playerMap) <= 500 && this.canAttackPlayer(player, playerMap)) {
                                player.skillSpecial.playersTaget.add(playerMap);
                            } else if (player.skillSpecial.dir == 1 && !playerMap.isDie() && Util.getDistance(player, playerMap) <= 500 && this.canAttackPlayer(player, playerMap)) {
                                player.skillSpecial.playersTaget.add(playerMap);
                            }
                        }
                    }

//                    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                    //hút quái
                    for (Mob mobMap : player.zone.mobs) {
                        if (player.skillSpecial.dir == -1 && !mobMap.isDie() && Util.getDistance(player, mobMap) <= 500) {
                            player.skillSpecial.mobsTaget.add(mobMap);
                        } else if (player.skillSpecial.dir == 1 && !mobMap.isDie() && Util.getDistance(player, mobMap) <= 500) {
                            player.skillSpecial.mobsTaget.add(mobMap);
                        }
                        if (mobMap == null) {
                            continue;
                        }
                    }

                    //bắt đầu hút
                    if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                        for (int i = 1; i < player.inventory.itemsBody.get(7).itemOptions.size(); i++) {
                            if (player.inventory.itemsBody.get(7).itemOptions.get(i).optionTemplate.id == 231) {
                                player.inventory.itemsBody.get(7).itemOptions.get(i).param -= 1;
                                InventoryService.gI().sendItemBody(player);
                            }
                        }
                        if (player.inventory.itemsBody.get(7).template.id == 1287) {
                            this.startSkillSpecialID26(player, 2, 0);
                        } else {
                            this.startSkillSpecialID26(player, 3, 0);
                        }
                    } else {
                        this.startSkillSpecialID26(player, 0, 0);
                    }
                    Thread.sleep(3000);//nghỉ 3s

                    //biến quái - bình
                    for (Mob mobMap : player.zone.mobs) {
                        if (!MapService.gI().isMapOfflineNe(player.zone.map.mapId)) {
                            if (player.skillSpecial.dir == -1 && !mobMap.isDie() && Util.getDistance(player, mobMap) <= 500) {
                                player.skillSpecial.mobsTaget.add(mobMap);
                            } else if (player.skillSpecial.dir == 1 && !mobMap.isDie() && Util.getDistance(player, mobMap) <= 500) {
                                player.skillSpecial.mobsTaget.add(mobMap);
                            }
                            if (mobMap == null) {
                                continue;
                            }
                            EffectSkillService.gI().sendMobToMaPhongBa(player, mobMap, timeBinh);//biến mob thành bình
                            mobMap.idPlayerMaFuBa = player.id;
                        }
                    }

                    //biến người - bình
                    for (Player playerMap : player.zone.getHumanoids()) {
                        if (!MapService.gI().isMapOfflineNe(player.zone.map.mapId)) {
                            if (playerMap != null && playerMap != player) {
                                if (player.skillSpecial != null && !playerMap.isDie() && Util.getDistance(player, playerMap) <= 500 && this.canAttackPlayer(player, playerMap)) {
                                    player.skillSpecial.playersTaget.add(playerMap);

                                    if (playerMap != null && playerMap.id != player.id) {

                                        double ptdame = 0;

                                        switch (curSkill.point) {
                                            case 1:
                                            case 2:
                                                ptdame = 0.01;
                                                break;
                                            case 3:
                                            case 4:
                                                ptdame = 0.02;
                                                break;
                                            case 5:
                                            case 6:
                                                ptdame = 0.03;
                                                break;
                                            case 7:
                                            case 8:
                                                ptdame = 0.04;
                                                break;
                                            case 9:
                                                ptdame = 0.06;
                                                break;
                                            default:
                                                ptdame = 0.01;
                                                break;
                                        }

                                        int dameHit = (int) (player.nPoint.hpMax * ptdame);

                                        ItemTimeService.gI().sendItemTime(playerMap, 11175, timeBinh / 1000);
                                        EffectSkillService.gI().setMaPhongBa(playerMap, System.currentTimeMillis(), timeBinh, dameHit);
                                        Service.getInstance().Send_Caitrang(playerMap);
                                    }
                                }
                            }
                        } else {
                            if (playerMap != null && playerMap != player) {
                                if (playerMap.idPlayerForNPC == player.id) {
                                    if (player.skillSpecial != null && !playerMap.isDie() && Util.getDistance(player, playerMap) <= 500 && this.canAttackPlayer(player, playerMap)) {
                                        player.skillSpecial.playersTaget.add(playerMap);

                                        if (playerMap != null && playerMap.id != player.id) {

                                            double ptdame = 0;

                                            switch (curSkill.point) {
                                                case 1:
                                                case 2:
                                                    ptdame = 0.01;
                                                    break;
                                                case 3:
                                                case 4:
                                                    ptdame = 0.02;
                                                    break;
                                                case 5:
                                                case 6:
                                                    ptdame = 0.03;
                                                    break;
                                                case 7:
                                                case 8:
                                                    ptdame = 0.04;
                                                    break;
                                                case 9:
                                                    ptdame = 0.06;
                                                    break;
                                                default:
                                                    ptdame = 0.01;
                                                    break;
                                            }

                                            int dameHit = (int) (player.nPoint.hpMax * ptdame);

                                            ItemTimeService.gI().sendItemTime(playerMap, 11175, timeBinh / 1000);
                                            EffectSkillService.gI().setMaPhongBa(playerMap, System.currentTimeMillis(), timeBinh, dameHit);
                                            Service.getInstance().Send_Caitrang(playerMap);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Sau khi hoàn thành tất cả các tác vụ, hủy bỏ ScheduledExecutorService
//                    executorService.shutdown();
                }
            } else {
                // SUPER KAME
                if (player.skillSpecial.stepSkillSpecial == 0 && Util.canDoWithTime(player.skillSpecial.lastTimeSkillSpecial, SkillSpecial.TIME_GONG)) {
                    player.skillSpecial.lastTimeSkillSpecial = System.currentTimeMillis();
                    player.skillSpecial.stepSkillSpecial = 1;
                    if (player.skillSpecial.skillSpecial.template.id == Skill.SUPER_KAME) {
                        if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                            for (int i = 1; i < player.inventory.itemsBody.get(7).itemOptions.size(); i++) {
                                if (player.inventory.itemsBody.get(7).itemOptions.get(i).optionTemplate.id == 231) {
                                    player.inventory.itemsBody.get(7).itemOptions.get(i).param -= 1;
                                    InventoryService.gI().sendItemBody(player);
                                }
                            }
                            if (player.inventory.itemsBody.get(7).template.id == 1285) {
                                this.startSkillSpecialID24(player, 2);
                            } else {
                                this.startSkillSpecialID24(player, 3);
                            }
                        } else {
                            this.startSkillSpecialID24(player, 0);
                        }
                    } else {
                        // CA DIC LIEN HOAN CHUONG
                        if (player.inventory.itemsBody.get(7).isNotNullItem()) {
                            for (int i = 1; i < player.inventory.itemsBody.get(7).itemOptions.size(); i++) {
                                if (player.inventory.itemsBody.get(7).itemOptions.get(i).optionTemplate.id == 231) {
                                    player.inventory.itemsBody.get(7).itemOptions.get(i).param -= 1;
                                    InventoryService.gI().sendItemBody(player);
                                }
                            }
                            if (player.inventory.itemsBody.get(7).template.id == 1289) {
                                this.startSkillSpecialID25(player, 2);
                            } else {
                                this.startSkillSpecialID25(player, 3);
                            }
                        } else {
                            this.startSkillSpecialID25(player, 0);
                        }
                    }
                } else if (player.skillSpecial.stepSkillSpecial == 1 && !Util.canDoWithTime(player.skillSpecial.lastTimeSkillSpecial, SkillSpecial.TIME_END_24_25)) {
                    if (MapService.gI().isMapOfflineNe(player.zone.map.mapId)) {
                        for (Player playerMap : player.zone.getHumanoids()) {
                            if (playerMap != null && playerMap.idPlayerForNPC == player.id) {
                                if (player.skillSpecial.dir == -1 && !playerMap.isDie()
                                        && playerMap.location.x <= player.location.x - 15
                                        && Math.abs(playerMap.location.x - player.skillSpecial._xPlayer) <= player.skillSpecial._xObjTaget
                                        && Math.abs(playerMap.location.y - player.skillSpecial._yPlayer) <= player.skillSpecial._yObjTaget
                                        && this.canAttackPlayer(player, playerMap)) {
                                    this.playerAttackPlayer(player, playerMap, false);
                                    PlayerService.gI().sendInfoHpMpMoney(playerMap);
                                }
                                if (player.skillSpecial.dir == 1 && !playerMap.isDie()
                                        && playerMap.location.x >= player.location.x + 15
                                        && Math.abs(playerMap.location.x - player.skillSpecial._xPlayer) <= player.skillSpecial._xObjTaget
                                        && Math.abs(playerMap.location.y - player.skillSpecial._yPlayer) <= player.skillSpecial._yObjTaget
                                        && this.canAttackPlayer(player, playerMap)) {
                                    this.playerAttackPlayer(player, playerMap, false);
                                    PlayerService.gI().sendInfoHpMpMoney(playerMap);
                                }
                            }
                        }
                        return;
                    }
                    for (Player playerMap : player.zone.getHumanoids()) {
                        if (player.skillSpecial.dir == -1 && !playerMap.isDie()
                                && playerMap.location.x <= player.location.x - 15
                                && Math.abs(playerMap.location.x - player.skillSpecial._xPlayer) <= player.skillSpecial._xObjTaget
                                && Math.abs(playerMap.location.y - player.skillSpecial._yPlayer) <= player.skillSpecial._yObjTaget
                                && this.canAttackPlayer(player, playerMap)) {
                            this.playerAttackPlayer(player, playerMap, false);
                            PlayerService.gI().sendInfoHpMpMoney(playerMap);
                        }
                        if (player.skillSpecial.dir == 1 && !playerMap.isDie()
                                && playerMap.location.x >= player.location.x + 15
                                && Math.abs(playerMap.location.x - player.skillSpecial._xPlayer) <= player.skillSpecial._xObjTaget
                                && Math.abs(playerMap.location.y - player.skillSpecial._yPlayer) <= player.skillSpecial._yObjTaget
                                && this.canAttackPlayer(player, playerMap)) {
                            this.playerAttackPlayer(player, playerMap, false);
                            PlayerService.gI().sendInfoHpMpMoney(playerMap);
                        }
                        if (playerMap == null) {
                            continue;
                        }
                    }
                    for (Mob mobMap : player.zone.mobs) {
                        if (player.skillSpecial.dir == -1 && !mobMap.isDie()
                                && mobMap.location.x <= player.skillSpecial._xPlayer - 15
                                && Math.abs(mobMap.location.x - player.skillSpecial._xPlayer) <= player.skillSpecial._xObjTaget
                                && Math.abs(mobMap.location.y - player.skillSpecial._yPlayer) <= player.skillSpecial._yObjTaget) {
                            this.playerAttackMob(player, mobMap, false, false);
                        }
                        if (player.skillSpecial.dir == 1 && !mobMap.isDie()
                                && mobMap.location.x >= player.skillSpecial._xPlayer + 15
                                && Math.abs(mobMap.location.x - player.skillSpecial._xPlayer) <= player.skillSpecial._xObjTaget
                                && Math.abs(mobMap.location.y - player.skillSpecial._yPlayer) <= player.skillSpecial._yObjTaget) {
                            this.playerAttackMob(player, mobMap, false, false);
                        }
                        if (mobMap == null) {
                            continue;
                        }
                    }
                } else if (player.skillSpecial.stepSkillSpecial == 1) {
                    player.skillSpecial.closeSkillSpecial();
                }
            }
        } catch (Exception e) {
        }
    }

    public void sendCurrLevelSpecial(Player player, Skill skill) {
        Message message = null;
        try {
            message = Service.getInstance().messageSubCommand((byte) 62);
            message.writer().writeShort(skill.skillId);
            message.writer().writeByte(0);
            message.writer().writeShort(skill.currLevel);
            player.sendMessage(message);
        } catch (final Exception ex) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    // Skill SuperKame
    public void sendEffSkillSpecialID24(Player player, byte dir, int TypePaintSkill) {
        Message message = null;
        try {
            message = new Message(-45);// passt code k dc vcb 
            message.writer().writeByte(20);
            message.writer().writeInt((int) player.id);
            message.writer().writeShort(24);
            message.writer().writeByte(1);
            message.writer().writeByte(dir); // -1 trai | 1 phai
            message.writer().writeShort(2000);
            message.writer().writeByte(0);
            message.writer().writeByte(TypePaintSkill);// đoạn này là skill paint 
            message.writer().writeByte(0);
            Service.getInstance().sendMessAllPlayerInMap(player, message);
            message.cleanup();
        } catch (Exception e) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    // Skill liên hoàn chưởng
    public void sendEffSkillSpecialID25(Player player, byte dir, int typeskill) { //Tư thế gồng + hào quang
        Message message = null;
        try {
            message = new Message(-45);// passt code k dc vcb 
            message.writer().writeByte(20);
            message.writer().writeInt((int) player.id);
            message.writer().writeShort(25);
            message.writer().writeByte(2);
            message.writer().writeByte(dir); // -1 trai | 1 phai
            message.writer().writeShort(2000);
            message.writer().writeByte(0);
            message.writer().writeByte(typeskill); // type skill : 0 = defaule, 1,2 = type mới
            message.writer().writeByte(0);
            Service.getInstance().sendMessAllPlayerInMap(player, message);
            message.cleanup();
        } catch (Exception e) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    // Skill Ma phong ba
    public void sendEffSkillSpecialID26(Player player, byte dir, int typeskill) {
        Message message = null;
        try {
            message = new Message(-45);// passt code k dc vcb 
            message.writer().writeByte(20);
            message.writer().writeInt((int) player.id);
            message.writer().writeShort(26); // id effect
            message.writer().writeByte(3);
            message.writer().writeByte(dir); // -1 trai | 1 phai
            message.writer().writeShort(SkillSpecial.TIME_GONG);
            message.writer().writeByte(0);
            message.writer().writeByte(typeskill);
            message.writer().writeByte(0);
            Service.getInstance().sendMessAllPlayerInMap(player, message);
            message.cleanup();
        } catch (Exception e) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    public void startSkillSpecialID24(Player player, int TypePaintSkill) {
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(21);
            message.writer().writeInt((int) player.id);
            message.writer().writeShort(player.skillSpecial.skillSpecial.template.id);
            message.writer().writeShort(player.skillSpecial._xPlayer + ((player.skillSpecial.dir == -1) ? (-player.skillSpecial._xObjTaget) : player.skillSpecial._xObjTaget));
            message.writer().writeShort(player.skillSpecial._xPlayer);
            message.writer().writeShort(3000); // thời gian skill chưởng chưởng nè
            message.writer().writeShort(player.skillSpecial._yObjTaget);
            message.writer().writeByte(TypePaintSkill);
            message.writer().writeByte(TypePaintSkill);
            message.writer().writeByte(TypePaintSkill);
            Service.getInstance().sendMessAllPlayerInMap(player, message);
            message.cleanup();
        } catch (final Exception ex) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    public void startSkillSpecialID25(Player player, int typeskill) { // bắt đầu sử dụng skill
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(21);
            message.writer().writeInt((int) player.id);
            message.writer().writeShort(player.skillSpecial.skillSpecial.template.id);
            message.writer().writeShort(player.skillSpecial._xPlayer + ((player.skillSpecial.dir == -1) ? (-player.skillSpecial._xObjTaget) : player.skillSpecial._xObjTaget));
            message.writer().writeShort(player.skillSpecial._yPlayer);
            message.writer().writeShort(3000); // thời gian skill chưởng chưởng nè
            message.writer().writeShort(25);
            message.writer().writeByte(typeskill); // skill tung ra : 0 = skill mặc định
            message.writer().writeByte(typeskill); // skill kết : 0 = skill mặc định
            message.writer().writeByte(typeskill); // skill kết : 0 = skill mặc định
            Service.getInstance().sendMessAllPlayerInMap(player, message);
            message.cleanup();
        } catch (final Exception ex) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    public void startSkillSpecialID26(Player player, int typeskill, int imgBinh) {
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(21);
            message.writer().writeInt((int) player.id);
            message.writer().writeShort(26);
            message.writer().writeShort(player.skillSpecial._xPlayer + ((player.skillSpecial.dir == -1) ? (-75) : 75));
            message.writer().writeShort(player.skillSpecial._yPlayer);
            message.writer().writeShort(3000);
            message.writer().writeShort(player.skillSpecial._yObjTaget);
            message.writer().writeByte(typeskill);
            final byte size = (byte) (player.skillSpecial.playersTaget.size() + player.skillSpecial.mobsTaget.size());
            message.writer().writeByte(size);
            for (Player playerMap : player.skillSpecial.playersTaget) {
                message.writer().writeByte(1);
                message.writer().writeInt((int) playerMap.id);
            }
            for (Mob mobMap : player.skillSpecial.mobsTaget) {
                message.writer().writeByte(0);
                message.writer().writeByte(mobMap.id);
            }
            message.writer().writeByte(imgBinh); // ảnh bình để hút vào : 0 = defaule ; 1 = ảnh cạnh ảnh 0; 2 = ảnh cạnh ảnh 1
            Service.getInstance().sendMessAllPlayerInMap(player, message);
            message.cleanup();
        } catch (final Exception ex) {
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }
        }
    }

    // này hoc5 skill nha
    public void learSkillSpecial(Player player, byte skillID) {
        Message message = null;
        try {
            Skill curSkill = SkillUtil.createSkill(skillID, 1);
            SkillUtil.setSkill(player, curSkill);
            message = Service.getInstance().messageSubCommand((byte) 23);
            message.writer().writeShort(curSkill.skillId);
            player.sendMessage(message);
            message.cleanup();
        } catch (Exception e) {
//            System.out.println("88888");
        } finally {
            if (message != null) {
                message.cleanup();
                message = null;
            }

        }
    }

    private void useSkillAttack(Player player, Player plTarget, Mob mobTarget) {
        if (!player.isBoss) {
            if (player.isPet) {
                if (player.isClone) {
                    if (player.nPoint.stamina > 0) {
                        player.nPoint.numAttack++;
                        boolean haveCharmPet = ((Pet) player).master.charms.tdDeTu > System.currentTimeMillis();
                        if (haveCharmPet ? player.nPoint.numAttack >= 5 : player.nPoint.numAttack >= 2) {
                            player.nPoint.numAttack = 0;
                            player.nPoint.stamina--;
                        }
                    } else {
                        ((Pet) player).askPea();
                        return;
                    }
                } else {
                    if (player.nPoint.stamina > 0) {
                        if (player.charms.tdDeoDai < System.currentTimeMillis()) {
                            player.nPoint.numAttack++;
                            if (player.nPoint.numAttack == 5) {
                                player.nPoint.numAttack = 0;
                                player.nPoint.stamina--;
                                PlayerService.gI().sendCurrentStamina(player);
                            }
                        }
                    } else {
                        Service.getInstance().sendThongBao(player, "Thể lực đã cạn kiệt, hãy nghỉ ngơi để lấy lại sức");
                        return;
                    }
                }
            }
        }
        List<Mob> mobs;
        boolean miss = false;
        if (player.playerSkill.skillSelect.template.id == Skill.KAMEJOKO || player.playerSkill.skillSelect.template.id == Skill.MASENKO || player.playerSkill.skillSelect.template.id == Skill.ANTOMIC) {
            if (!player.isBoss && !player.isPet && !player.isMiniPet && !player.isClone) {
                player.playerTask.achivements.get(ConstAchive.NOI_CONG_CAO_CUONG).count++;
            }

        }
        switch (player.playerSkill.skillSelect.template.id) {
            case Skill.KAIOKEN: //kaioken
                int hpUse = player.nPoint.hpMax / 100 * 10;
                if (player.nPoint.hp <= hpUse) {
                    break;
                } else {
                    player.nPoint.setHp(player.nPoint.mp - hpUse);
                    PlayerService.gI().sendInfoHpMpMoney(player);
                    Service.getInstance().Send_Info_NV(player);
                }
            case Skill.DRAGON:
            case Skill.DEMON:
            case Skill.GALICK:
            case Skill.LIEN_HOAN:
                if (plTarget != null && Util.getDistance(player, plTarget) > Skill.RANGE_ATTACK_CHIEU_DAM) {
                    miss = true;
                }
                if (mobTarget != null && Util.getDistance(player, mobTarget) > Skill.RANGE_ATTACK_CHIEU_DAM) {
                    miss = true;
                }
            case Skill.KAMEJOKO:
            case Skill.MASENKO:
            case Skill.ANTOMIC:
                if (plTarget != null) {
                    playerAttackPlayer(player, plTarget, miss);
                }
                if (mobTarget != null) {
                    playerAttackMob(player, mobTarget, miss, false);
                }
                if (player.mobMe != null) {
                    player.mobMe.attack(plTarget, mobTarget);
                }

                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            //******************************************************************
            case Skill.QUA_CAU_KENH_KHI:
                if (!player.playerSkill.prepareQCKK) {
                    //bắt đầu tụ quả cầu
                    player.playerSkill.prepareQCKK = !player.playerSkill.prepareQCKK;
                    sendPlayerPrepareSkill(player, 4000);
                } else {
                    //ném cầu
                    player.playerSkill.prepareQCKK = !player.playerSkill.prepareQCKK;
                    mobs = new ArrayList<>();
                    if (plTarget != null) {
                        playerAttackPlayer(player, plTarget, false);
                        for (Mob mob : player.zone.mobs) {
                            if (!mob.isDie()
                                    && Util.getDistance(plTarget, mob) <= SkillUtil.getRangeQCKK(player.playerSkill.skillSelect.point)) {
                                mobs.add(mob);
                            }
                        }
                    }
                    if (mobTarget != null) {
                        playerAttackMob(player, mobTarget, false, true);
                        for (Mob mob : player.zone.mobs) {
                            if (!mob.equals(mobTarget) && !mob.isDie()
                                    && Util.getDistance(mob, mobTarget) <= SkillUtil.getRangeQCKK(player.playerSkill.skillSelect.point)) {
                                mobs.add(mob);
                            }
                        }
                    }
                    for (Mob mob : mobs) {
//                        mob.injured(player, player.point.getDameAttack(), true);
                    }
                    PlayerService.gI().sendInfoHpMpMoney(player);
                    affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                }
                break;
            case Skill.MAKANKOSAPPO:
                if (!player.playerSkill.prepareLaze) {
                    //bắt đầu nạp laze
                    player.playerSkill.prepareLaze = !player.playerSkill.prepareLaze;
                    sendPlayerPrepareSkill(player, 3000);
                } else {
                    //bắn laze
                    player.playerSkill.prepareLaze = !player.playerSkill.prepareLaze;
                    if (plTarget != null) {
                        playerAttackPlayer(player, plTarget, false);
                    }
                    if (mobTarget != null) {
                        playerAttackMob(player, mobTarget, false, true);
//                        mobTarget.attackMob(player, false, true);
                    }
                    affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                }
                PlayerService.gI().sendInfoHpMpMoney(player);
                break;
            case Skill.SOCOLA:
                EffectSkillService.gI().sendEffectUseSkill(player, Skill.SOCOLA);
                int timeSocola = SkillUtil.getTimeSocola();
                if (plTarget != null) {
                    EffectSkillService.gI().setSocola(plTarget, System.currentTimeMillis(), timeSocola);
                    Service.getInstance().Send_Caitrang(plTarget);
                    ItemTimeService.gI().sendItemTime(plTarget, 3780, timeSocola / 1000);
                }
                if (mobTarget != null) {
                    EffectSkillService.gI().sendMobToSocola(player, mobTarget, timeSocola);
                }
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.DICH_CHUYEN_TUC_THOI:
                int timeChoangDCTT = SkillUtil.getTimeDCTT(player.playerSkill.skillSelect.point);
                if (plTarget != null) {
                    Service.getInstance().setPos(player, plTarget.location.x, plTarget.location.y);
                    playerAttackPlayer(player, plTarget, miss);
                    EffectSkillService.gI().setBlindDCTT(plTarget, System.currentTimeMillis(), timeChoangDCTT);
                    EffectSkillService.gI().sendEffectPlayer(player, plTarget, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.BLIND_EFFECT);
                    PlayerService.gI().sendInfoHpMpMoney(plTarget);
                    ItemTimeService.gI().sendItemTime(plTarget, 3779, timeChoangDCTT / 1000);
                }
                if (mobTarget != null) {
                    Service.getInstance().setPos(player, mobTarget.location.x, mobTarget.location.y);
//                    mobTarget.attackMob(player, false, false);
                    playerAttackMob(player, mobTarget, false, false);
                    mobTarget.effectSkill.setStartBlindDCTT(System.currentTimeMillis(), timeChoangDCTT);
                    EffectSkillService.gI().sendEffectMob(player, mobTarget, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.BLIND_EFFECT);
                }
                player.nPoint.isCrit100 = true;
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.THOI_MIEN:
                EffectSkillService.gI().sendEffectUseSkill(player, Skill.THOI_MIEN);
                int timeSleep = SkillUtil.getTimeThoiMien(player.playerSkill.skillSelect.point);
                if (plTarget != null) {
                    EffectSkillService.gI().setThoiMien(plTarget, System.currentTimeMillis(), timeSleep);
                    EffectSkillService.gI().sendEffectPlayer(player, plTarget, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.SLEEP_EFFECT);
                    ItemTimeService.gI().sendItemTime(plTarget, 3782, timeSleep / 1000);
                }
                if (mobTarget != null) {
                    mobTarget.effectSkill.setThoiMien(System.currentTimeMillis(), timeSleep);
                    EffectSkillService.gI().sendEffectMob(player, mobTarget, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.SLEEP_EFFECT);
                }
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.TROI:
                EffectSkillService.gI().sendEffectUseSkill(player, Skill.TROI);
                int timeHold = SkillUtil.getTimeTroi(player.playerSkill.skillSelect.point);
                EffectSkillService.gI().setUseTroi(player, System.currentTimeMillis(), timeHold);
                if (plTarget != null && (!plTarget.playerSkill.prepareQCKK && !plTarget.playerSkill.prepareLaze && !plTarget.playerSkill.prepareTuSat)) {
                    player.effectSkill.plAnTroi = plTarget;
                    EffectSkillService.gI().sendEffectPlayer(player, plTarget, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.HOLD_EFFECT);
                    EffectSkillService.gI().setAnTroi(plTarget, player, System.currentTimeMillis(), timeHold);
                }
                if (mobTarget != null) {
                    player.effectSkill.mobAnTroi = mobTarget;
                    EffectSkillService.gI().sendEffectMob(player, mobTarget, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.HOLD_EFFECT);
                    mobTarget.effectSkill.setTroi(System.currentTimeMillis(), timeHold);
                }
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
        }
        if (!player.isBoss) {
            player.effectSkin.lastTimeAttack = System.currentTimeMillis();
        }
    }

    private void useSkillAlone(Player player) {
        List<Mob> mobs;
        List<Player> players;
        switch (player.playerSkill.skillSelect.template.id) {
            case Skill.THAI_DUONG_HA_SAN:
                int timeStun = SkillUtil.getTimeStun(player.playerSkill.skillSelect.point);
                if (player.setClothes.thienXinHang == 5) {
                    timeStun *= 2;
                }
                mobs = new ArrayList<>();
                players = new ArrayList<>();
                if (!player.zone.map.isMapOffline || !MapService.gI().isMapOfflineNe(player.zone.map.mapId)) {
                    List<Player> playersMap = player.zone.getHumanoids();
                    for (Player pl : playersMap) {
                        if (pl != null && !player.equals(pl)) {
                            int distance = Util.getDistance(player, pl);
                            int rangeStun = SkillUtil.getRangeStun(player.playerSkill.skillSelect.point);
                            if (distance <= rangeStun && canAttackPlayer(player, pl)) {//&& (!pl.playerSkill.prepareQCKK && !pl.playerSkill.prepareLaze && !pl.playerSkill.prepareTuSat)
                                if (player.isPet && ((Pet) player).master.equals(pl)) {
                                    continue;
                                }
                                EffectSkillService.gI().startStun(pl, System.currentTimeMillis(), timeStun);
                                if (pl.typePk != ConstPlayer.NON_PK) {
                                    players.add(pl);
                                }
                            }
                        }
                    }
                }
                if (!player.isBoss) {
                    for (Mob mob : player.zone.mobs) {
                        if (Util.getDistance(player, mob) <= SkillUtil.getRangeStun(player.playerSkill.skillSelect.point)) {
                            mob.effectSkill.startStun(System.currentTimeMillis(), timeStun);
                            mobs.add(mob);
                        }
                    }
                }
                EffectSkillService.gI().sendEffectBlindThaiDuongHaSan(player, players, mobs, timeStun);
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.DE_TRUNG:
                EffectSkillService.gI().sendEffectUseSkill(player, Skill.DE_TRUNG);
                if (player.mobMe != null) {
                    player.mobMe.mobMeDie();
                }
                player.mobMe = new MobMe(player);
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.BIEN_KHI:
                EffectSkillService.gI().sendEffectMonkey(player);
                EffectSkillService.gI().setIsMonkey(player);
                EffectSkillService.gI().sendEffectMonkey(player);
                player.nPoint.setFullHpMp();

                Service.getInstance().sendSpeedPlayer(player, 0);
                Service.getInstance().Send_Caitrang(player);
                Service.getInstance().sendSpeedPlayer(player, -1);
                if (!player.isPet) {
                    PlayerService.gI().sendInfoHpMp(player);
                    player.nPoint.setFullHpMp();
                }
                Service.getInstance().point(player);
                player.nPoint.setFullHpMp();
                Service.getInstance().Send_Info_NV(player);
                Service.getInstance().sendInfoPlayerEatPea(player);
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;

            case Skill.KHIEN_NANG_LUONG:
                EffectSkillService.gI().setStartShield(player);
                EffectSkillService.gI().sendEffectPlayer(player, player, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.SHIELD_EFFECT);
                ItemTimeService.gI().sendItemTime(player, 3784, player.effectSkill.timeShield / 1000);
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.HUYT_SAO:
                int tileHP = SkillUtil.getPercentHPHuytSao(player.playerSkill.skillSelect.point);
                if (player.zone != null) {
                    if (!player.zone.map.isMapOffline || !MapService.gI().isMapOfflineNe(player.zone.map.mapId)) {
                        List<Player> playersMap = player.zone.getHumanoids();
                        for (Player pl : playersMap) {
                            if (pl.effectSkill.useTroi) {
                                EffectSkillService.gI().removeUseTroi(pl);
                            }
                            if (!pl.isBoss && pl.gender != ConstPlayer.NAMEC
                                    && player.cFlag == pl.cFlag) {
                                EffectSkillService.gI().setStartHuytSao(pl, tileHP);
                                EffectSkillService.gI().sendEffectPlayer(pl, pl, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.HUYT_SAO_EFFECT);
                                pl.nPoint.calPoint();
                                pl.nPoint.setHp(pl.nPoint.hp + (int) ((long) pl.nPoint.hp * tileHP / 100));
                                Service.getInstance().point(pl);
                                Service.getInstance().Send_Info_NV(pl);
                                ItemTimeService.gI().sendItemTime(pl, 3781, 30);
                                PlayerService.gI().sendInfoHpMp(pl);
                            }

                        }
                    } else {
                        EffectSkillService.gI().setStartHuytSao(player, tileHP);
                        EffectSkillService.gI().sendEffectPlayer(player, player, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.HUYT_SAO_EFFECT);
                        player.nPoint.calPoint();
                        player.nPoint.setHp(player.nPoint.hp + (int) ((long) player.nPoint.hp * tileHP / 100));
                        Service.getInstance().point(player);
                        Service.getInstance().Send_Info_NV(player);
                        ItemTimeService.gI().sendItemTime(player, 3781, 30);
                        PlayerService.gI().sendInfoHpMp(player);
                    }
                }
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.TAI_TAO_NANG_LUONG:
                EffectSkillService.gI().startCharge(player);
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
            case Skill.TU_SAT:
                if (!player.playerSkill.prepareTuSat) {
                    //gồng tự sát
                    player.playerSkill.prepareTuSat = true;
                    sendPlayerPrepareBom(player, 2500);
                } else {
                    //nổ
                    player.playerSkill.prepareTuSat = false;
                    int rangeBom = SkillUtil.getRangeBom(player.playerSkill.skillSelect.point);
                    int dame = player.nPoint.hpMax;
//                    if (player.effectSkill.isMonkey) {
//                        int percent = SkillUtil.getPercentHpMonkey(player.effectSkill.levelMonkey);
//                        dame -= percent * (dame / 100);
//                    }
                    for (Mob mob : player.zone.mobs) {
                        if (Util.getDistance(player, mob) <= rangeBom) {
                            mob.injured(player, dame, true);
                        }
                    }
                    List<Player> playersMap = null;
                    if (player.isBoss) {
                        playersMap = player.zone.getNotBosses();
                    } else {
                        playersMap = player.zone.getHumanoids();
                    }
                    if (!player.zone.map.isMapOffline || !MapService.gI().isMapOfflineNe(player.zone.map.mapId)) {
                        for (Player pl : playersMap) {
                            if (!player.equals(pl) && canAttackPlayer(player, pl)
                                    && Util.getDistance(player, pl) <= rangeBom) {
                                pl.injured(player, pl.isBoss ? dame / 2 : dame, false, false);
                                PlayerService.gI().sendInfoHpMpMoney(pl);
                                Service.getInstance().Send_Info_NV(pl);
                            }
                        }
                    }
                    affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                    player.injured(null, 2100000000, true, false);
                }
                break;
            case Skill.BIEN_HINH:
                EffectSkillService.gI().sendEffectbienhinh(player);
                if (player.effectSkill.levelBienHinh < player.playerSkill.skillSelect.point) {
                    EffectSkillService.gI().setBienHinh(player);
                    EffectSkillService.gI().sendEffectbienhinh(player);
                    Service.getInstance().Send_Caitrang(player);
                    Service.getInstance().point(player);
                    player.nPoint.setFullHpMp();
                    PlayerService.gI().sendInfoHpMp(player);
                    RadaService.getInstance().setIDAuraEff(player, player.getAura());
                    // ICON Ở GÓC TD / NM/XD
                    //   ItemTimeService.gI().sendItemTime(player, player.gender == 0 ? 30011 : player.gender == 1 ? 30011 : 30011, player.effectSkill.timeBienHinh / 1000);
                    ItemTimeService.gI().sendItemTimeBienHinh(player, player.effectSkill.levelBienHinh);

                    affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                }
                break;
            case Skill.PHAN_THAN:
                EffectSkillService.gI().sendEffectPhanThan(player);
                if (player.clone != null) {
                    player.clone.dispose();
                }
                player.clone = new PlayerClone(player);
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
        }
        if (player.playerTask.achivements.size() > 0) {
            player.playerTask.achivements.get(ConstAchive.KY_NANG_THANH_THAO).count++;
        }
    }

    private void useSkillBuffToPlayer(Player player, Player plTarget) {
        switch (player.playerSkill.skillSelect.template.id) {
            case Skill.TRI_THUONG:
                List<Player> players = new ArrayList();
                int percentTriThuong = SkillUtil.getPercentTriThuong(player.playerSkill.skillSelect.point);
                int point = player.playerSkill.skillSelect.point;
                if (canHsPlayer(player, plTarget)) {
                    players.add(plTarget);
                    List<Player> playersMap = player.zone.getNotBosses();
                    for (Player pl : playersMap) {
                        if (!pl.equals(plTarget)) {
                            if (canHsPlayer(player, plTarget) && Util.getDistance(player, pl) <= 300) {
                                players.add(pl);
                            }
                        }
                    }
                    playerAttackPlayer(player, plTarget, false);
                    for (Player pl : players) {
                        boolean isDie = pl.isDie();
                        int hpHoi = pl.nPoint.hpMax * percentTriThuong / 100;
                        int mpHoi = pl.nPoint.mpMax * percentTriThuong / 100;
                        pl.nPoint.addHp(hpHoi);
                        pl.nPoint.addMp(mpHoi);
                        if (isDie) {
                            Service.getInstance().hsChar(pl, hpHoi, mpHoi);
                            PlayerService.gI().sendInfoHpMp(pl);
                        } else {
                            Service.getInstance().Send_Info_NV(pl);
                            PlayerService.gI().sendInfoHpMp(pl);
                        }
                    }
                    long hpHoiMe = player.nPoint.hp * percentTriThuong / 100;
                    player.nPoint.addHp(hpHoiMe);
                    PlayerService.gI().sendInfoHp(player);
                }
                affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                break;
        }
    }

    private void phanSatThuong(Player plAtt, Player plTarget, int dame) {
        if (plAtt.id == 202) {
            return;
        }
        int percentPST = plTarget.nPoint.tlPST;
        if (percentPST != 0) {
            int damePST = dame * percentPST / 100;
            Message msg;
            try {
                msg = new Message(56);
                msg.writer().writeInt((int) plAtt.id);
                if (damePST >= plAtt.nPoint.hp) {
                    damePST = plAtt.nPoint.hp - 1;
                }
                damePST = plAtt.injured(null, damePST, true, false);
                msg.writer().writeInt(plAtt.nPoint.hp);
                msg.writer().writeInt(damePST);
                msg.writer().writeBoolean(false);
                msg.writer().writeByte(36);
                Service.getInstance().sendMessAllPlayerInMap(plAtt, msg);
                msg.cleanup();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void hutHPMP(Player player, int dame, boolean attackMob) {
        int tiLeHutHp = player.nPoint.getTileHutHp(attackMob);
        int tiLeHutMp = player.nPoint.getTiLeHutMp();
        int hpHoi = dame * tiLeHutHp / 100;
        int mpHoi = dame * tiLeHutMp / 100;
        if (hpHoi > 0 || mpHoi > 0) {
            PlayerService.gI().hoiPhuc(player, hpHoi, mpHoi);
        }
    }

    private void playerAttackPlayer(Player plAtt, Player plInjure, boolean miss) {
        if (plInjure.effectSkill.anTroi) {
            plAtt.nPoint.isCrit100 = true;
        }
        int dameHit = plInjure.injured(plAtt, miss ? 0 : plAtt.nPoint.getDameAttack(false), false, false);
        phanSatThuong(plAtt, plInjure, dameHit);
        hutHPMP(plAtt, dameHit, false);
        Message msg;
        try {
            msg = new Message(-60);
            msg.writer().writeInt((int) plAtt.id); //id pem
            msg.writer().writeByte(plAtt.playerSkill.skillSelect.skillId); //skill pem
            msg.writer().writeByte(1); //số người pem
            msg.writer().writeInt((int) plInjure.id); //id ăn pem
            byte typeSkill = SkillUtil.getTyleSkillAttack(plAtt.playerSkill.skillSelect);
            msg.writer().writeByte(typeSkill == 2 ? 0 : 1); //read continue
            msg.writer().writeByte(typeSkill); //type skill
            msg.writer().writeInt(dameHit); //dame ăn
            msg.writer().writeBoolean(plInjure.isDie()); //is die
            msg.writer().writeBoolean(plAtt.nPoint.isCrit); //crit
            if (typeSkill != 1) {
                Service.getInstance().sendMessAllPlayerInMap(plAtt, msg);
                msg.cleanup();
            } else {
                plInjure.sendMessage(msg);
                msg.cleanup();
                msg = new Message(-60);
                msg.writer().writeInt((int) plAtt.id); //id pem
                msg.writer().writeByte(plAtt.playerSkill.skillSelect.skillId); //skill pem
                msg.writer().writeByte(1); //số người pem
                msg.writer().writeInt((int) plInjure.id); //id ăn pem
                msg.writer().writeByte(typeSkill == 2 ? 0 : 1); //read continue
                msg.writer().writeByte(0); //type skill
                msg.writer().writeInt(dameHit); //dame ăn
                msg.writer().writeBoolean(plInjure.isDie()); //is die
                msg.writer().writeBoolean(plAtt.nPoint.isCrit); //crit
                Service.getInstance().sendMessAnotherNotMeInMap(plInjure, msg);
                msg.cleanup();
            }
            Service.getInstance().addSMTN(plInjure, (byte) 2, 1, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playerAttackMob(Player plAtt, Mob mob, boolean miss, boolean dieWhenHpFull) {
        if (!mob.isDie()) {
            if (plAtt.effectSkin.isVoHinh) {
                plAtt.effectSkin.isVoHinh = false;
            }
            int dameHit = plAtt.nPoint.getDameAttack(true);
            if (plAtt.charms.tdBatTu > System.currentTimeMillis() && plAtt.nPoint.hp == 1) {
                dameHit = 0;
            }
            if (plAtt.charms.tdManhMe > System.currentTimeMillis()) {
                dameHit += (dameHit * 150 / 100);
            }
            if (plAtt.isPet) {
                if (((Pet) plAtt).charms.tdDeTu > System.currentTimeMillis()) {
                    dameHit *= 2;
                }
            }
            if (miss) {
                dameHit = 0;
            }
            hutHPMP(plAtt, dameHit, true);
            sendPlayerAttackMob(plAtt, mob);
            mob.injured(plAtt, dameHit, dieWhenHpFull);
        }
    }

    private void sendPlayerPrepareSkill(Player player, int affterMiliseconds) {
        Message msg;
        try {
            msg = new Message(-45);
            msg.writer().writeByte(4);
            msg.writer().writeInt((int) player.id);
            msg.writer().writeShort(player.playerSkill.skillSelect.skillId);
            msg.writer().writeShort(affterMiliseconds);
            Service.getInstance().sendMessAllPlayerInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void sendPlayerPrepareBom(Player player, int affterMiliseconds) {
        Message msg;
        try {
            msg = new Message(-45);
            msg.writer().writeByte(7);
            msg.writer().writeInt((int) player.id);
//            msg.writer().writeShort(player.playerSkill.skillSelect.skillId);
            msg.writer().writeShort(104);
            msg.writer().writeShort(affterMiliseconds);
            Service.getInstance().sendMessAllPlayerInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public boolean canUseSkillWithMana(Player player) {
        if (player.playerSkill.skillSelect != null) {
            if (player.playerSkill.skillSelect.template.id == Skill.KAIOKEN) {
                long hpUse = player.nPoint.hpMax / 100 * 10;
                if (player.nPoint.hp <= hpUse) {
                    return false;
                }
            }
            switch (player.playerSkill.skillSelect.template.manaUseType) {
                case 0:
                    if (player.nPoint.mp >= player.playerSkill.skillSelect.manaUse) {
                        return true;
                    } else {
                        return false;
                    }
                case 1:
                    int mpUse = (int) (player.nPoint.mpMax * player.playerSkill.skillSelect.manaUse / 100);
                    if (player.nPoint.mp >= mpUse) {
                        return true;
                    } else {
                        return false;
                    }
                case 2:
                    if (player.nPoint.mp > 0) {
                        return true;
                    } else {
                        return false;
                    }
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    public boolean canUseSkillWithCooldown(Player player) {
        return Util.canDoWithTime(player.playerSkill.skillSelect.lastTimeUseThisSkill,
                player.playerSkill.skillSelect.coolDown - 50);
    }

    public void affterUseSkill(Player player, int skillId) {
        Intrinsic intrinsic = player.playerIntrinsic.intrinsic;
        switch (skillId) {
            case Skill.DICH_CHUYEN_TUC_THOI:
                if (intrinsic.id == 6) {
                    player.nPoint.dameAfter = intrinsic.param1;
                }
                break;
            case Skill.THOI_MIEN:
                if (intrinsic.id == 7) {
                    player.nPoint.dameAfter = intrinsic.param1;
                }
                break;
            case Skill.SOCOLA:
                if (intrinsic.id == 14) {
                    player.nPoint.dameAfter = intrinsic.param1;
                }
                break;
            case Skill.TROI:
                if (intrinsic.id == 22) {
                    player.nPoint.dameAfter = intrinsic.param1;
                }
                break;

        }
        setMpAffterUseSkill(player);
        setLastTimeUseSkill(player, skillId);
    }

    private void setMpAffterUseSkill(Player player) {
        if (player.playerSkill.skillSelect != null) {
            switch (player.playerSkill.skillSelect.template.manaUseType) {
                case 0:
                    if (player.nPoint.mp >= player.playerSkill.skillSelect.manaUse) {
                        player.nPoint.setMp(player.nPoint.mp - player.playerSkill.skillSelect.manaUse);
                    }
                    break;
                case 1:
                    int mpUse = (int) (player.nPoint.mpMax * player.playerSkill.skillSelect.manaUse / 100);
                    if (player.nPoint.mp >= mpUse) {
                        player.nPoint.setMp(player.nPoint.mp - mpUse);
                    }
                    break;
                case 2:
                    player.nPoint.setMp(0);
                    break;
            }
            PlayerService.gI().sendInfoHpMpMoney(player);
        }
    }

    private void setLastTimeUseSkill(Player player, int skillId) {
        Intrinsic intrinsic = player.playerIntrinsic.intrinsic;
        int subTimeParam = 0;
        switch (skillId) {
            case Skill.TRI_THUONG:
                if (intrinsic.id == 10) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.THAI_DUONG_HA_SAN:
                if (intrinsic.id == 3) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.QUA_CAU_KENH_KHI:
                if (intrinsic.id == 4) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.KHIEN_NANG_LUONG:
                if (intrinsic.id == 5 || intrinsic.id == 15 || intrinsic.id == 20) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.MAKANKOSAPPO:
                if (intrinsic.id == 11) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.DE_TRUNG:
                if (intrinsic.id == 12) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.TU_SAT:
                if (intrinsic.id == 19) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.HUYT_SAO:
                if (intrinsic.id == 21) {
                    subTimeParam = intrinsic.param1;
                }
                break;
            case Skill.BIEN_HINH:
                subTimeParam = 1;
                break;
            case Skill.PHAN_THAN:
                subTimeParam = 1;
                break;
        }
        int coolDown = player.playerSkill.skillSelect.coolDown;
        player.playerSkill.skillSelect.lastTimeUseThisSkill = System.currentTimeMillis() - (coolDown * subTimeParam / 100);
        if (subTimeParam != 0) {
            Service.getInstance().sendTimeSkill(player);
        }
    }

    private boolean canHsPlayer(Player player, Player plTarget) {
        if (plTarget == null) {
            return false;
        }
        if (plTarget.isBoss) {
            return false;
        }
        if (plTarget.typePk == ConstPlayer.PK_ALL) {
            return false;
        }
        if (plTarget.typePk == ConstPlayer.PK_PVP) {
            return false;
        }
        if (player.cFlag != 0) {
            if (plTarget.cFlag != 0 && plTarget.cFlag != player.cFlag) {
                return false;
            }
        } else if (plTarget.cFlag != 0) {
            return false;
        }
        return true;
    }

    public boolean canAttackPlayer(Player pl1, Player pl2) {
        if (pl2 != null && !pl1.isDie() && !pl2.isDie()) {
            if (pl1.typePk > 0 || pl2.typePk > 0) {
                return true;
            }
            if ((pl1.cFlag != 0 && pl2.cFlag != 0)
                    && (pl1.cFlag == 8 || pl2.cFlag == 8 || pl1.cFlag != pl2.cFlag)) {
                return true;
            }
            PVP pvp = PVPServcice.gI().findPvp(pl1);
            if (pvp != null) {
                if ((pvp.player1.equals(pl1) && pvp.player2.equals(pl2)
                        || (pvp.player1.equals(pl2) && pvp.player2.equals(pl1)))) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private void sendPlayerAttackMob(Player plAtt, Mob mob) {
        Message msg;
        try {
            msg = new Message(54);
            msg.writer().writeInt((int) plAtt.id);
            msg.writer().writeByte(plAtt.playerSkill.skillSelect.skillId);
            msg.writer().writeByte(mob.id);
            Service.getInstance().sendMessAllPlayerInMap(plAtt, msg);
            msg.cleanup();

        } catch (Exception e) {

        }
    }

//    public void selectSkill(Player player, int skillId) {
//        Skill skillBefore = player.playerSkill.skillSelect;
//        for (Skill skill : player.playerSkill.skills) {
//            if (skill.skillId != -1 && skill.template.id == skillId) {
//                player.playerSkill.skillSelect = skill;
//                switch (skillBefore.template.id) {
//                    case Skill.DRAGON:
//                    case Skill.KAMEJOKO:
//                    case Skill.DEMON:
//                    case Skill.MASENKO:
//                    case Skill.LIEN_HOAN:
//                    case Skill.GALICK:
//                    case Skill.ANTOMIC:
//                        switch (skill.template.id) {
//                            case Skill.KAMEJOKO:
//                                skill.lastTimeUseThisSkill = System.currentTimeMillis() + (5000 / 2);
//                                break;
//                            case Skill.DRAGON:
//                            case Skill.DEMON:
//                            case Skill.MASENKO:
//                            case Skill.LIEN_HOAN:
//                            case Skill.GALICK:
//                            case Skill.ANTOMIC:
//                                skill.lastTimeUseThisSkill = System.currentTimeMillis() + (skill.coolDown / 2);
//                                break;
//                        }
//                        break;
//                }
//                break;
//            }
//        }
//    }
    public void selectSkill(Player player, int skillId) {
        for (Skill skill : player.playerSkill.skills) {
            if (skill.skillId != -1 && skill.template.id == skillId) {
                player.playerSkill.skillSelect = skill;
                break;
            }
        }
        if (player.clone != null) {
            selectSkill(player.clone, skillId);
        }
    }

    public void useSKillNotFocus(Player player, short skillID, short xPlayer, short yPlayer, byte dir, short x, short y) {
        try {
            //  short skillId = player.playerSkill.skillSelect.template.id;
            System.out.println("phân thân 1");

            if (canUseSkillWithMana(player) && canUseSkillWithCooldown(player)) {
                Skill skillSelect = player.playerSkill.skillSelect;
                if (skillSelect instanceof SkillNotFocus skill) {
                    if (player.location.x != xPlayer || player.location.y != yPlayer) {
                        return;
                    }
                    int skillRange = skill.getRange();
                    int range = xPlayer + (dir == 1 ? skillRange : -skillRange);
                    sendEffStartSkillNotFocus(player, skillID, dir, 5000, (byte) 0);
                    Util.setTimeout(() -> {
                        List<Mob> mobs = new ArrayList<>();
                        List<Player> players = new ArrayList<>();
                        Hit hit = new Hit();
                        int dameAttack = player.nPoint.getDameAttackSkillNotFocus();
                        for (Mob mob : player.zone.mobs) {
                            if (player.location.y == mob.location.y) {
                                if (dir == 1) {// phải
                                    if (mob.location.x >= xPlayer && Util.getDistanceByDir(player.location.x, mob.location.x, dir) <= skillRange) {
                                        mobs.add(mob);
                                    }
                                } else {//trái
                                    if (mob.location.x <= xPlayer && Util.getDistanceByDir(player.location.x, mob.location.x, dir) >= skillRange) {
                                        mobs.add(mob);
                                    }
                                }
                                hit.addTarget(mob.id, 0);
                            }
                        }

                        for (Player p : player.zone.getPlayers()) {
                            if (SkillService.i.canAttackPlayer(player, p)) {
                                if (Math.abs(yPlayer - player.location.y) <= 100) {
                                    if (dir == 1) {// phải
                                        if (p.location.x >= xPlayer && Util.getDistanceByDir(player.location.x, p.location.x, dir) <= skillRange) {
                                            players.add(p);
                                        }
                                    } else {//trái
                                        if (p.location.x <= xPlayer && Util.getDistanceByDir(player.location.x, p.location.x, dir) >= skillRange) {
                                            players.add(p);
                                        }
                                    }
                                    hit.addTarget((int) player.id, 1);
                                }
                            }
                        }
                        sendEffEndUseSkillNotFocus(player, skillID, range, skill.getTimeDame(), hit);
                        if (skillID == Skill.MAFUBA) {
                            try {
                                Thread.sleep(skill.getTimePre());
                            } catch (InterruptedException e) {
                            }
                            int timeSocola = SkillUtil.getTimeSocola();
                            Zone z = player.zone;
                            for (Map.Entry<Integer, Integer> entry : hit.getTargets().entrySet()) {
                                int type = entry.getValue();
                                if (type == 0) {
                                    Mob mobTarget = z.findMobByID(entry.getKey());
                                    if (mobTarget != null) {
                                        EffectSkillService.gI().sendMobToSocola(player, mobTarget, timeSocola);
                                    }
                                } else {
                                    Player plTarget = z.findPlayerByID(entry.getKey());
                                    if (plTarget != null) {
                                        EffectSkillService.gI().setSocola(plTarget, System.currentTimeMillis(), timeSocola);
                                        Service.getInstance().Send_Caitrang(plTarget);
                                        ItemTimeService.gI().sendItemTime(plTarget, 3780, timeSocola / 1000);
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < 10; i++) {
                                if (i == 9) {
                                    hit.addHit((dameAttack + (dameAttack / 2)));
                                } else {
                                    hit.addHit(dameAttack);
                                }
                            }
                            dealDamageSkillNotFocus(player, players, mobs, hit);
                        }
                    }, skill.getTimePre());

                }
            }
        } catch (Exception e) {
            Log.error(SkillService.class,
                    e);
        }
    }

    private void sendEffStartSkillNotFocus(Player player, short skillID, byte dir, int timePre, byte isFly) {
        try {
            Message m = new Message(-45);
            DataOutputStream ds = m.writer();
            ds.writeByte(20);
            ds.writeInt((int) player.id);
            ds.writeShort(skillID);
            ds.writeByte(player.gender + 1);//typeFrame
            ds.writeByte(dir);
            ds.writeShort(timePre);
            ds.writeByte(isFly);//isfly
            ds.writeByte(player.gender);//typepaint
            ds.writeByte(0);//typeItem
            ds.flush();
            Service.getInstance().sendMessAllPlayerInMap(player.zone, m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEffEndUseSkillNotFocus(Player player, short skillID, int x, int time, Hit hits) {
        Message m = new Message(-45);
        DataOutputStream ds = m.writer();
        try {
            ds.writeByte(21);
            ds.writeInt((int) player.id);
            ds.writeShort(skillID);
            ds.writeShort(x);
            ds.writeShort(player.location.y);
            ds.writeShort(time);
            ds.writeShort(player.location.y);

            ds.writeByte(player.gender);//type paint
            Map<Integer, Integer> targets = hits.getTargets();
            ds.writeByte(targets.size());
            for (Map.Entry<Integer, Integer> entry : targets.entrySet()) {
                int type = entry.getValue();
                ds.writeByte(type);
                if (type == 0) {
                    ds.writeByte(entry.getKey());
                } else {
                    ds.writeInt(entry.getKey());
                }
            }

            ds.writeByte(0);//type item
            ds.flush();
            Service.getInstance().sendMessAllPlayerInMap(player.zone, m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dealDamageSkillNotFocus(Player player, List<Player> players, List<Mob> mobs, Hit hit) {
        List<Integer> hits = hit.getHits();
        final int maxHit = hits.size();
        final int[] damageCount = {0};
        Timer timer = player.playerSkill.timer;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                int damage = hits.get(damageCount[0]);
                damageCount[0]++;
                for (Player p : players) {
                    p.injured(player, damage, false, false);
                }
                for (Mob mob : mobs) {
                    mob.injured(player, damage, false);
                }
                if (damageCount[0] >= maxHit) {
                    affterUseSkill(player, player.playerSkill.skillSelect.template.id);
                    cancel();
                }
            }
        }, 0, 500);
    }

    public boolean useSkill(Player player, Player plTarget, Mob mobTarget) {
        try {
            if (player.playerSkill.skillSelect == null && player.playerSkill == null && player.playerSkill.skillSelect.template == null) {
                return false;
            }
            if (player.playerSkill.skillSelect.template.type == 2 && canUseSkillWithMana(player) && canUseSkillWithCooldown(player)) {
                useSkillBuffToPlayer(player, plTarget);
                return true;
            }
            if ((player.effectSkill.isHaveEffectSkill()
                    && (player.playerSkill.skillSelect.template.id != Skill.TU_SAT
                    && player.playerSkill.skillSelect.template.id != Skill.QUA_CAU_KENH_KHI
                    && player.playerSkill.skillSelect.template.id != Skill.MAKANKOSAPPO))
                    || (plTarget != null && !canAttackPlayer(player, plTarget))
                    || (mobTarget != null && mobTarget.isDie())
                    || !canUseSkillWithMana(player) || !canUseSkillWithCooldown(player)) {
                return false;
            }
            if (player.effectSkill.useTroi) {
                EffectSkillService.gI().removeUseTroi(player);
            }
            if (player.effectSkill.isCharging) {
                EffectSkillService.gI().stopCharge(player);
            }
            if (player.isPet) {
            }
            switch (player.playerSkill.skillSelect.template.type) {
                case 1:
                    useSkillAttack(player, plTarget, mobTarget);
                    break;
                case 3:
                    useSkillAlone(player);
                    break;
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
