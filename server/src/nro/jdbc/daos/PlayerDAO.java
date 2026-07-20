package nro.jdbc.daos;

import com.google.gson.Gson;
import nro.consts.ConstMap;
import nro.jdbc.DBService;
import nro.manager.AchiveManager;
import nro.manager.SieuHangManager;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTime;
import nro.models.player.*;
import nro.models.skill.Skill;
import nro.models.task.Achivement;
import nro.models.task.AchivementTemplate;
import nro.server.Manager;
import nro.services.MapService;
import nro.utils.Log;
import nro.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.logging.Level;
import nro.services.InventoryService;

/**
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 */
public class PlayerDAO {

    public static boolean updateTimeLogout;

    public static void createNewPlayer(Connection con, int userId, String name, byte gender, int hair) {
        PreparedStatement ps = null;
        try {
            JSONArray dataInventory = new JSONArray();

            dataInventory.add(2000);
            dataInventory.add(10000);
            dataInventory.add(0);
            String inventory = dataInventory.toJSONString();

            JSONArray dataLocation = new JSONArray();
            dataLocation.add(100);
            dataLocation.add(384);
            dataLocation.add(39 + gender);
            String location = dataLocation.toJSONString();

            JSONArray dataPoint = new JSONArray();
            dataPoint.add(0);// nang dong
            dataPoint.add(gender == 1 ? 200 : 100);//mp
            dataPoint.add(gender == 1 ? 200 : 100);//mpg
            dataPoint.add(0);//critg
            dataPoint.add(0);//limitpower
            dataPoint.add(1000);//stamina
            dataPoint.add(gender == 0 ? 200 : 100);//hp
            dataPoint.add(0);//defg
            dataPoint.add(2000);//tn
            dataPoint.add(1000);//maxsta
            dataPoint.add(gender == 2 ? 15 : 10);//damg
            dataPoint.add(2000);//pow
            dataPoint.add(gender == 0 ? 200 : 100);//hpg
            String point = dataPoint.toJSONString();

            JSONArray dataMagicTree = new JSONArray();
            dataMagicTree.add(0);//isupgr
            dataMagicTree.add(new Date().getTime());
            dataMagicTree.add(1);//LV
            dataMagicTree.add(new Date().getTime());
            dataMagicTree.add(5);//curr_pea
            String magicTree = dataMagicTree.toJSONString();

            int idAo = gender == 0 ? 0 : gender == 1 ? 1 : 2;
            int idQuan = gender == 0 ? 6 : gender == 1 ? 7 : 8;
            int def = gender == 2 ? 3 : 2;
            int hp = gender == 0 ? 30 : 20;

            JSONArray dataBody = new JSONArray();
            for (int i = 0; i < 13; i++) {
                JSONObject item = new JSONObject();
                JSONArray options = new JSONArray();
                JSONArray option = new JSONArray();
                if (i == 0) {
                    option.add(47);
                    option.add(def);
                    options.add(option);
                    item.put("temp_id", idAo);
                    item.put("create_time", System.currentTimeMillis());
                    item.put("quantity", 1);
                } else if (i == 1) {
                    option.add(6);
                    option.add(hp);
                    options.add(option);
                    item.put("temp_id", idQuan);
                    item.put("create_time", System.currentTimeMillis());
                    item.put("quantity", 1);
                } else {
                    item.put("temp_id", -1);
                    item.put("create_time", 0);
                    item.put("quantity", 1);
                }
                item.put("option", options);
                dataBody.add(item);
            }
            String itemsBody = dataBody.toJSONString();

            JSONArray dataBag = new JSONArray();
            for (int i = 0; i < 20; i++) {
                JSONObject item = new JSONObject();
                JSONArray options = new JSONArray();
                JSONArray option = new JSONArray();
                if (i == 0) {
                    option.add(30);
                    option.add(0);
                    options.add(option);
                    item.put("temp_id", 457);
                    item.put("create_time", System.currentTimeMillis());
                    item.put("quantity", 10);
                } else {
                    item.put("temp_id", -1);
                    item.put("create_time", 0);
                    item.put("quantity", 1);
                }
                item.put("option", options);
                dataBag.add(item);
            }
            String itemsBag = dataBag.toJSONString();

            JSONArray dataBox = new JSONArray();
            for (int i = 0; i < 20; i++) {
                JSONObject item = new JSONObject();
                JSONArray options = new JSONArray();
                JSONArray option = new JSONArray();
                if (i == 0) {
                    item.put("temp_id", 12);
                    option.add(14);
                    option.add(1);
                    options.add(option);
                    item.put("create_time", System.currentTimeMillis());
                } else {
                    item.put("temp_id", -1);
                    item.put("create_time", 0);
                }
                item.put("option", options);
                item.put("quantity", 1);
                dataBox.add(item);
            }
            String itemsBox = dataBox.toJSONString();

            JSONArray dataLuckyRound = new JSONArray();
            for (int i = 0; i < 110; i++) {
                JSONObject item = new JSONObject();
                JSONArray options = new JSONArray();
                item.put("temp_id", -1);
                item.put("option", options);
                item.put("create_time", 0);
                item.put("quantity", 1);
                dataLuckyRound.add(item);
            }
            String itemsBoxLuckyRound = dataLuckyRound.toJSONString();

            String friends = "[]";
            String enemies = "[]";

            JSONArray dataIntrinsic = new JSONArray();
            dataIntrinsic.add(0);
            dataIntrinsic.add(0);
            dataIntrinsic.add(0);
            dataIntrinsic.add(0);
            String intrinsic = dataIntrinsic.toJSONString();

            JSONArray dataItemTime = new JSONArray();
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            dataItemTime.add(0);
            String itemTime = dataItemTime.toJSONString();

            JSONArray dataTask = new JSONArray();
            dataTask.add(0);
            dataTask.add(0);
            dataTask.add(0);
            String task = dataTask.toJSONString();

            JSONArray dataAchive = new JSONArray();
            for (AchivementTemplate a : AchiveManager.getInstance().getList()) {
                JSONObject jobj = new JSONObject();
                jobj.put("id", a.getId());
                jobj.put("count", 0);
                jobj.put("finish", 0);
                jobj.put("receive", 0);
                dataAchive.add(jobj);
            }
            String achive = dataAchive.toJSONString();

            String mabuEgg = "{}";

            JSONArray dataCharms = new JSONArray();
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            dataCharms.add(0);
            String charms = dataCharms.toJSONString();

            int[] skillsArr = gender == 0 ? new int[]{0, 1, 6, 9, 10, 20, 22, 19, 24, 27,28}
                    : gender == 1 ? new int[]{2, 3, 7, 11, 12, 17, 18, 19, 26, 27,28}
                    : new int[]{4, 5, 8, 13, 14, 21, 23, 19, 25, 27,28};

            JSONArray dataSkills = new JSONArray();
            for (int i = 0; i < skillsArr.length; i++) {
                JSONArray skill = new JSONArray();
                skill.add(skillsArr[i]);
                skill.add(0);
                if (i == 0) {
                    skill.add(1);
                } else {
                    skill.add(0);
                }
                dataSkills.add(skill);
            }
            String skills = dataSkills.toJSONString();

            JSONArray dataSkillShortcut = new JSONArray();
            dataSkillShortcut.add(gender == 0 ? 0 : gender == 1 ? 2 : 4);
            for (int i = 0; i < 10; i++) {
                dataSkillShortcut.add(-1);
            }
            String skillsShortcut = dataSkillShortcut.toJSONString();

            String petInfo = "{}";
            String petPoint = "{}";
            String petBody = "[]";
            String petSkill = "[]";

            JSONArray dataBlackBall = new JSONArray();
            for (int i = 1; i <= 7; i++) {
                JSONArray arr = new JSONArray();
                arr.add(0);
                arr.add(0);
                dataBlackBall.add(arr);
            }
            String blackBall = dataBlackBall.toJSONString();

            ps = con.prepareStatement("insert into player"
                    + "(account_id, name, head, gender, have_tennis_space_ship, clan_id_sv" + (int) Manager.SERVER + ", "
                    + "data_inventory, data_location, data_point, data_magic_tree, items_body, "
                    + "items_bag, items_box, items_box_lucky_round, friends, enemies, data_intrinsic, data_item_time,"
                    + "data_task, data_mabu_egg, data_charm, skills, skills_shortcut, pet_info, pet_point, pet_body, pet_skill,"
                    + "data_black_ball, thoi_vang, data_side_task,achivements,lastimelogin) "
                    + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setInt(3, hair);
            ps.setByte(4, gender);
            ps.setBoolean(5, false);
            ps.setInt(6, -1);
            ps.setString(7, inventory);
            ps.setString(8, location);
            ps.setString(9, point);
            ps.setString(10, magicTree);
            ps.setString(11, itemsBody);
            ps.setString(12, itemsBag);
            ps.setString(13, itemsBox);
            ps.setString(14, itemsBoxLuckyRound);
            ps.setString(15, friends);
            ps.setString(16, enemies);
            ps.setString(17, intrinsic);
            ps.setString(18, itemTime);
            ps.setString(19, task);
            ps.setString(20, mabuEgg);
            ps.setString(21, charms);
            ps.setString(22, skills);
            ps.setString(23, skillsShortcut);
            ps.setString(24, petInfo);
            ps.setString(25, petPoint);
            ps.setString(26, petBody);
            ps.setString(27, petSkill);
            ps.setString(28, blackBall);
            ps.setInt(29, 10); //gold bar
            ps.setString(30, "{}");
            ps.setString(31, achive);
            LocalDateTime currentTime = LocalDateTime.now();
            Timestamp currentTimestamp = Timestamp.valueOf(currentTime);
            ps.setTimestamp(32, currentTimestamp);
            if (ps.executeUpdate() > 0) {
                SieuHangManager.InsertNewPlayer(userId);
            }
        } catch (Exception e) {
            Log.error(PlayerDAO.class, e, "Lỗi tạo player mới");
        } finally {
            try {
                ps.close();
            } catch (Exception e) {
            }
        }

    }

    public static void updatePlayer(Player player, Connection connection) {
        synchronized (player) {
            if (player.isDisposed() || player.isSaving()) {
                return;
            }
            player.setSaving(true);
        }
        try {
            int n1s = 0;
            int n2s = 0;
            int n3s = 0;
            int tv = 0;
            if (player.loaded) {
                long st = System.currentTimeMillis();
                try {

                    JSONArray dataInventory = new JSONArray();
                    //data kim lượng
                    dataInventory.add(player.inventory.gold);
                    dataInventory.add(player.inventory.gem);
                    dataInventory.add(player.inventory.ruby);
                    dataInventory.add(player.inventory.goldLimit);
                    String inventory = dataInventory.toJSONString();

                    int mapId = -1;
                    mapId = player.mapIdBeforeLogout;
                    int x = player.location.x;
                    int y = player.location.y;
                    int hp = player.nPoint.hp;
                    int mp = player.nPoint.mp;
                    if (player.isDie()) {
                        mapId = (player.gender >= 0 && player.gender <= 2) ? player.gender + 21 : 21;
                        x = 300;
                        y = 336;
                        hp = 1;
                        mp = 1;
                    } else {
                        if (MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId) || mapId == 126 || mapId == ConstMap.CON_DUONG_RAN_DOC
                                || mapId == ConstMap.CON_DUONG_RAN_DOC_142 || mapId == ConstMap.CON_DUONG_RAN_DOC_143 || mapId == ConstMap.HOANG_MAC) {
                            mapId = (player.gender >= 0 && player.gender <= 2) ? player.gender + 21 : 21;
                            x = 300;
                            y = 336;
                        }
                    }

                    //data vị trí
                    JSONArray dataLocation = new JSONArray();
                    dataLocation.add(x);
                    dataLocation.add(y);
                    dataLocation.add(mapId);
                    String location = dataLocation.toJSONString();
                    //data chỉ số
                    JSONArray dataPoint = new JSONArray();
                    dataPoint.add(0);
                    dataPoint.add(mp);
                    dataPoint.add(player.nPoint.mpg);
                    dataPoint.add(player.nPoint.critg);
                    dataPoint.add(player.nPoint.limitPower);
                    dataPoint.add(player.nPoint.stamina);
                    dataPoint.add(hp);
                    dataPoint.add(player.nPoint.defg);
                    dataPoint.add(player.nPoint.tiemNang);
                    dataPoint.add(player.nPoint.maxStamina);
                    dataPoint.add(player.nPoint.dameg);
                    dataPoint.add(player.nPoint.power);
                    dataPoint.add(player.nPoint.hpg);
                    String point = dataPoint.toJSONString();

                    //data đậu thần
                    JSONArray dataMagicTree = new JSONArray();
                    dataMagicTree.add(player.magicTree.isUpgrade ? 1 : 0);
                    dataMagicTree.add(player.magicTree.lastTimeUpgrade);
                    dataMagicTree.add(player.magicTree.level);
                    dataMagicTree.add(player.magicTree.lastTimeHarvest);
                    dataMagicTree.add(player.magicTree.currPeas);
                    String magicTree = dataMagicTree.toJSONString();

                    //data body
                    JSONArray dataBody = new JSONArray();
                    for (Item item : player.inventory.itemsBody) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);
                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataBody.add(dataItem);
                    }

                    String itemsBody = dataBody.toJSONString();

                    //data bag
                    JSONArray dataBag = new JSONArray();
                    for (Item item : player.inventory.itemsBag) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            JSONArray options = new JSONArray();
                            switch (item.template.id) {
                                case 14:
                                    n1s += item.quantity;
                                    break;
                                case 15:
                                    n2s += item.quantity;
                                    break;
                                case 16:
                                    n3s += item.quantity;
                                    break;
                                case 457:
                                    tv += item.quantity;
                                    break;
                            }
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);

                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataBag.add(dataItem);
                    }
                    String itemsBag = dataBag.toJSONString();

                    //data box
                    JSONArray dataBox = new JSONArray();
                    for (Item item : player.inventory.itemsBox) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            JSONArray options = new JSONArray();
                            switch (item.template.id) {
                                case 14:
                                    n1s += item.quantity;
                                    break;
                                case 15:
                                    n2s += item.quantity;
                                    break;
                                case 16:
                                    n3s += item.quantity;
                                    break;
                                case 457:
                                    tv += item.quantity;
                                    break;
                            }
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);

                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataBox.add(dataItem);
                    }
                    String itemsBox = dataBox.toJSONString();

                    //data box crack ball
                    JSONArray dataCrackBall = new JSONArray();
                    for (Item item : player.inventory.itemsBoxCrackBall) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);
                            JSONArray options = new JSONArray();
                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataCrackBall.add(dataItem);
                    }
                    String itemsBoxLuckyRound = dataCrackBall.toJSONString();

                    //data bạn bè
                    JSONArray dataFriends = new JSONArray();
                    for (Friend f : player.friends) {
                        JSONObject friend = new JSONObject();
                        friend.put("id", f.id);
                        friend.put("name", f.name);
                        friend.put("power", f.power);
                        friend.put("head", f.head);
                        friend.put("body", f.body);
                        friend.put("leg", f.leg);
                        friend.put("bag", f.bag);
                        dataFriends.add(friend);
                    }
                    String friend = dataFriends.toJSONString();

                    //data kẻ thù
                    JSONArray dataEnemies = new JSONArray();
                    for (Friend e : player.enemies) {
                        JSONObject enemy = new JSONObject();
                        enemy.put("id", e.id);
                        enemy.put("name", e.name);
                        enemy.put("power", e.power);
                        enemy.put("head", e.head);
                        enemy.put("body", e.body);
                        enemy.put("leg", e.leg);
                        enemy.put("bag", e.bag);
                        dataEnemies.add(enemy);
                    }
                    String enemy = dataEnemies.toJSONString();

                    //data nội tại
                    JSONArray dataIntrinsic = new JSONArray();
                    dataIntrinsic.add(player.playerIntrinsic.intrinsic.id);
                    dataIntrinsic.add(player.playerIntrinsic.intrinsic.param1);
                    dataIntrinsic.add(player.playerIntrinsic.countOpen);
                    dataIntrinsic.add(player.playerIntrinsic.intrinsic.param2);
                    String intrinsic = dataIntrinsic.toJSONString();

                    //data item time
                    JSONArray dataItemTime = new JSONArray();
                    dataItemTime.add(player.itemTime.isUseBoKhi ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi)) : 0);
                    dataItemTime.add(player.itemTime.isUseAnDanh ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh)) : 0);
                    dataItemTime.add(player.itemTime.isOpenPower ? (ItemTime.TIME_OPEN_POWER - (System.currentTimeMillis() - player.itemTime.lastTimeOpenPower)) : 0);
                    dataItemTime.add(player.itemTime.isUseCuongNo ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo)) : 0);
                    dataItemTime.add(player.itemTime.isUseMayDo ? (ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo)) : 0);
                    dataItemTime.add(player.itemTime.isUseBoHuyet ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet)) : 0);
                    dataItemTime.add(player.itemTime.iconMeal);
                    dataItemTime.add(player.itemTime.isEatMeal ? (ItemTime.TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal)) : 0);
                    dataItemTime.add(player.itemTime.isUseGiapXen ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen)) : 0);
                    dataItemTime.add(player.itemTime.isUseBanhChung ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBanhChung)) : 0);
                    dataItemTime.add(player.itemTime.isUseBanhTet ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBanhTet)) : 0);

                    dataItemTime.add(player.itemTime.isUseBoKhi2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi2)) : 0);
                    dataItemTime.add(player.itemTime.isUseGiapXen2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen2)) : 0);
                    dataItemTime.add(player.itemTime.isUseCuongNo2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo2)) : 0);
                    dataItemTime.add(player.itemTime.isUseBoHuyet2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet2)) : 0);
                    String itemTime = dataItemTime.toJSONString();

                    //data nhiệm vụ
                    JSONArray dataTask = new JSONArray();
                    if (player.playerTask.taskMain != null && player.playerTask.taskMain.subTasks != null) {
                        dataTask.add(player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count);
                        dataTask.add(player.playerTask.taskMain.id);
                        dataTask.add(player.playerTask.taskMain.index);
                    } else {
                        dataTask.add(0);
                        dataTask.add(0);
                        dataTask.add(0);
                    }
                    String task = dataTask.toJSONString();

                    //data nhiệm vụ hàng ngày
                    JSONArray dataSideTask = new JSONArray();
                    dataSideTask.add(player.playerTask.sideTask.level);
                    dataSideTask.add(player.playerTask.sideTask.count);
                    dataSideTask.add(player.playerTask.sideTask.leftTask);
                    dataSideTask.add(player.playerTask.sideTask.template != null ? player.playerTask.sideTask.template.id : -1);
                    dataSideTask.add(player.playerTask.sideTask.receivedTime);
                    dataSideTask.add(player.playerTask.sideTask.maxCount);
                    String sideTask = dataSideTask.toJSONString();

                    JSONArray dataAchive = new JSONArray();
                    for (Achivement a : player.playerTask.achivements) {
                        JSONObject jobj = new JSONObject();
                        jobj.put("id", a.id);
                        jobj.put("count", a.count);
                        jobj.put("finish", a.finish ? 1 : 0);
                        jobj.put("receive", a.receive ? 1 : 0);
                        dataAchive.add(jobj);
                    }
                    String achive = dataAchive.toJSONString();

                    //data bùa
                    JSONArray dataCharms = new JSONArray();
                    dataCharms.add(player.charms.tdBoHuyet > System.currentTimeMillis() ? (player.charms.tdBoHuyet - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdBoKhi > System.currentTimeMillis() ? (player.charms.tdBoKhi - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdGiapXen > System.currentTimeMillis() ? (player.charms.tdGiapXen - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdThanMeo > System.currentTimeMillis() ? (player.charms.tdThanMeo - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdMaBa > System.currentTimeMillis() ? (player.charms.tdMaBa - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdDeTu > System.currentTimeMillis() ? (player.charms.tdDeTu - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue > System.currentTimeMillis() ? (player.charms.tdTriTue - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue2 > System.currentTimeMillis() ? (player.charms.tdTriTue2 - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue3 > System.currentTimeMillis() ? (player.charms.tdTriTue3 - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue4 > System.currentTimeMillis() ? (player.charms.tdTriTue4 - System.currentTimeMillis()) : 0);
                    String charms = dataCharms.toJSONString();

                    //data kỹ năng
                    JSONArray dataSkills = new JSONArray();
                    for (Skill skill : player.playerSkill.skills) {
                        JSONArray dataSkill = new JSONArray();
                        dataSkill.add(skill.template.id);
                        dataSkill.add(skill.lastTimeUseThisSkill);
                        dataSkill.add(skill.point);
                        dataSkills.add(dataSkill);
                    }
                    String skills = dataSkills.toJSONString();

                    //data shortcut kỹ năng
                    JSONArray dataSkillShortcut = new JSONArray();
                    for (int shortcut : player.playerSkill.skillShortCut) {
                        dataSkillShortcut.add(shortcut);
                    }
                    String skillsShortcut = dataSkillShortcut.toJSONString();

                    //data đệ tử
                    JSONObject petInfo = new JSONObject();
                    JSONObject petPoint = new JSONObject();
                    JSONArray petBody = new JSONArray();
                    JSONArray petSkill = new JSONArray();
                    if (player.pet != null) {
                        petInfo.put("name", player.pet.name);
                        petInfo.put("gender", player.pet.gender);
                        petInfo.put("is_mabu", player.pet.typePet);
                        petInfo.put("status", player.pet.status);
                        petInfo.put("type_fusion", player.fusion.typeFusion);
                        int leftFusion = (int) (Fusion.TIME_FUSION - (System.currentTimeMillis() - player.fusion.lastTimeFusion));
                        petInfo.put("left_fusion", leftFusion < 0 ? 0 : leftFusion);
                        petInfo.put("level", player.pet.getLever());

                        petPoint.put("hp", player.pet.nPoint.hp);
                        petPoint.put("mp", player.pet.nPoint.mp);
                        petPoint.put("hpg", player.pet.nPoint.hpg);
                        petPoint.put("mpg", player.pet.nPoint.mpg);
                        petPoint.put("damg", player.pet.nPoint.dameg);
                        petPoint.put("defg", player.pet.nPoint.defg);
                        petPoint.put("critg", player.pet.nPoint.critg);
                        petPoint.put("stamina", player.pet.nPoint.stamina);
                        petPoint.put("max_stamina", player.pet.nPoint.maxStamina);
                        petPoint.put("power", player.pet.nPoint.power);
                        petPoint.put("tiem_nang", player.pet.nPoint.tiemNang);
                        petPoint.put("limit_power", player.pet.nPoint.limitPower);

                        for (Item item : player.pet.inventory.itemsBody) {
                            JSONObject dataItem = new JSONObject();
                            if (item.isNotNullItem()) {
                                dataItem.put("temp_id", item.template.id);
                                dataItem.put("quantity", item.quantity);
                                dataItem.put("create_time", item.createTime);
                                JSONArray options = new JSONArray();
                                for (ItemOption io : item.itemOptions) {
                                    JSONArray option = new JSONArray();
                                    option.add(io.optionTemplate.id);
                                    option.add(io.param);
                                    options.add(option);
                                }
                                dataItem.put("option", options);
                            } else {
                                JSONArray options = new JSONArray();
                                dataItem.put("temp_id", -1);
                                dataItem.put("quantity", 0);
                                dataItem.put("create_time", 0);
                                dataItem.put("option", options);
                            }
                            petBody.add(dataItem);
                        }

                        for (Skill skill : player.pet.playerSkill.skills) {
                            JSONArray dataSkill = new JSONArray();
                            dataSkill.add(skill.template.id);
                            dataSkill.add(skill.point);
                            petSkill.add(dataSkill);
                        }
                    }

                    String petInfoData = petInfo.toJSONString();
                    String petPointData = petPoint.toJSONString();
                    String petBodyData = petBody.toJSONString();
                    String petSkillData = petSkill.toJSONString();

                    //data quả trứng
                    JSONObject mabuEgg = new JSONObject();
                    if (player.mabuEgg != null) {
                        mabuEgg.put("last_time_harvest", player.mabuEgg.lastTimeHarvest);
                        mabuEgg.put("time_done", player.mabuEgg.timeDone);
                    }
                    String mabuEggData = mabuEgg.toJSONString();

                    //data quả trứng bill
                    JSONObject billEgg = new JSONObject();
                    if (player.billEgg != null) {
                        billEgg.put("last_time_harvest", player.billEgg.lastTimeHarvest);
                        billEgg.put("time_done", player.billEgg.timeDone);
                    }
                    String billEggData = billEgg.toJSONString();

                    //data black ball
                    JSONArray dataBlackBall = new JSONArray();
                    for (int i = 0; i < player.rewardBlackBall.timeOutOfDateReward.length; i++) {
                        JSONArray arr = new JSONArray();
                        arr.add(player.rewardBlackBall.timeOutOfDateReward[i]);
                        arr.add(player.rewardBlackBall.lastTimeGetReward[i]);
                        dataBlackBall.add(arr);
                    }
                    String blackBall = dataBlackBall.toJSONString();

                    String query = "update player set head = ?, have_tennis_space_ship = ?, clan_id_sv" + (int) Manager.SERVER + " = ?, "
                            + "data_inventory = ?, data_location = ?, data_point = ?, data_magic_tree = ?, items_body = ?, "
                            + "items_bag = ?, items_box = ?, items_box_lucky_round = ?, friends = ?, enemies = ?, data_intrinsic = ?, data_item_time = ?, "
                            + "data_task = ?, data_mabu_egg = ?, data_bill_egg = ?, data_charm = ?, skills = ?, skills_shortcut = ?, pet_info = ?, pet_point = ?, pet_body = ?, pet_skill = ?, "
                            + "data_black_ball = ?, thoi_vang = ?, data_side_task = ?, last_logout_time = ?, achivements = ?, is_jaco = ? where id = ?";
                    PreparedStatement ps = connection.prepareStatement(query);
                    ps.setShort(1, player.head);
                    ps.setBoolean(2, player.haveTennisSpaceShip);
                    ps.setInt(3, (player.clan != null ? (int) player.clan.id : -1));
                    ps.setString(4, inventory);
                    ps.setString(5, location);
                    ps.setString(6, point);
                    ps.setString(7, magicTree);
                    ps.setString(8, itemsBody);
                    ps.setString(9, itemsBag);
                    ps.setString(10, itemsBox);
                    ps.setString(11, itemsBoxLuckyRound);
                    ps.setString(12, friend);
                    ps.setString(13, enemy);
                    ps.setString(14, intrinsic);
                    ps.setString(15, itemTime);
                    ps.setString(16, task);
                    ps.setString(17, mabuEggData);
                    ps.setString(18, billEggData);
                    ps.setString(19, charms);
                    ps.setString(20, skills);
                    ps.setString(21, skillsShortcut);
                    ps.setString(22, petInfoData);
                    ps.setString(23, petPointData);
                    ps.setString(24, petBodyData);
                    ps.setString(25, petSkillData);
                    ps.setString(26, blackBall);
                    ps.setInt(27, tv);
                    ps.setString(28, sideTask);
                    ps.setTimestamp(29, new Timestamp(System.currentTimeMillis()));
                    ps.setString(30, achive);
                    ps.setBoolean(31, player.isJaco);
                    ps.setInt(32, (int) player.id);
                    if (ps.executeUpdate() > 0) {
                        SieuHangManager.UpdatePlayerInfo(player);
                    }
                    ps.close();
                } catch (Exception e) {
                    Log.error(PlayerDAO.class, e, "Lỗi update player " + player.name);
                }
            }
        } catch (Exception e) {
            Log.error(PlayerDAO.class, e, "Lỗi update player " + player.name);
        } finally {
            player.setSaving(false);
        }
    }

    public static void savePlayer(Player player) {
        synchronized (player) {
            if (player.isDisposed() || player.isSaving()) {
                return;
            }
            player.setSaving(true);
        }
        try {
            int n1s = 0;
            int n2s = 0;
            int n3s = 0;
            int tv = 0;
            if (player.loaded) {
                long st = System.currentTimeMillis();
                try (Connection connection = DBService.gI().getConnectionForGame()) {

                    JSONArray dataInventory = new JSONArray();
                    //data kim lượng
                    dataInventory.add(player.inventory.gold);
                    dataInventory.add(player.inventory.gem);
                    dataInventory.add(player.inventory.ruby);
                    dataInventory.add(player.inventory.goldLimit);
                    String inventory = dataInventory.toJSONString();

                    int mapId = -1;
                    mapId = player.mapIdBeforeLogout;
                    int x = player.location.x;
                    int y = player.location.y;
                    int hp = player.nPoint.hp;
                    int mp = player.nPoint.mp;
                    if (player.isDie()) {
                        mapId = (player.gender >= 0 && player.gender <= 2) ? player.gender + 21 : 21;
                        x = 300;
                        y = 336;
                        hp = 1;
                        mp = 1;
                    } else {
                        if (MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId) || mapId == 126 || mapId == ConstMap.CON_DUONG_RAN_DOC
                                || mapId == ConstMap.CON_DUONG_RAN_DOC_142 || mapId == ConstMap.CON_DUONG_RAN_DOC_143 || mapId == ConstMap.HOANG_MAC) {
                            mapId = (player.gender >= 0 && player.gender <= 2) ? player.gender + 21 : 21;
                            x = 300;
                            y = 336;
                        }
                    }

                    //data vị trí
                    JSONArray dataLocation = new JSONArray();
                    dataLocation.add(x);
                    dataLocation.add(y);
                    dataLocation.add(mapId);
                    String location = dataLocation.toJSONString();
                    //data chỉ số
                    JSONArray dataPoint = new JSONArray();
                    dataPoint.add(0);
                    dataPoint.add(mp);
                    dataPoint.add(player.nPoint.mpg);
                    dataPoint.add(player.nPoint.critg);
                    dataPoint.add(player.nPoint.limitPower);
                    dataPoint.add(player.nPoint.stamina);
                    dataPoint.add(hp);
                    dataPoint.add(player.nPoint.defg);
                    dataPoint.add(player.nPoint.tiemNang);
                    dataPoint.add(player.nPoint.maxStamina);
                    dataPoint.add(player.nPoint.dameg);
                    dataPoint.add(player.nPoint.power);
                    dataPoint.add(player.nPoint.hpg);
                    String point = dataPoint.toJSONString();

                    //data đậu thần
                    JSONArray dataMagicTree = new JSONArray();
                    dataMagicTree.add(player.magicTree.isUpgrade ? 1 : 0);
                    dataMagicTree.add(player.magicTree.lastTimeUpgrade);
                    dataMagicTree.add(player.magicTree.level);
                    dataMagicTree.add(player.magicTree.lastTimeHarvest);
                    dataMagicTree.add(player.magicTree.currPeas);
                    String magicTree = dataMagicTree.toJSONString();

                    //data body
                    JSONArray dataBody = new JSONArray();
                    for (Item item : player.inventory.itemsBody) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);
                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataBody.add(dataItem);
                    }

                    String itemsBody = dataBody.toJSONString();

                    //data bag
                    JSONArray dataBag = new JSONArray();
                    for (Item item : player.inventory.itemsBag) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            JSONArray options = new JSONArray();
                            switch (item.template.id) {
                                case 14:
                                    n1s += item.quantity;
                                    break;
                                case 15:
                                    n2s += item.quantity;
                                    break;
                                case 16:
                                    n3s += item.quantity;
                                    break;
                                case 457:
                                    tv += item.quantity;
                                    break;
                            }
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);

                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataBag.add(dataItem);
                    }
                    String itemsBag = dataBag.toJSONString();

                    //data box
                    JSONArray dataBox = new JSONArray();
                    for (Item item : player.inventory.itemsBox) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            JSONArray options = new JSONArray();
                            switch (item.template.id) {
                                case 14:
                                    n1s += item.quantity;
                                    break;
                                case 15:
                                    n2s += item.quantity;
                                    break;
                                case 16:
                                    n3s += item.quantity;
                                    break;
                                case 457:
                                    tv += item.quantity;
                                    break;
                            }
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);

                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataBox.add(dataItem);
                    }
                    String itemsBox = dataBox.toJSONString();

                    //data box crack ball
                    JSONArray dataCrackBall = new JSONArray();
                    for (Item item : player.inventory.itemsBoxCrackBall) {
                        JSONObject dataItem = new JSONObject();
                        if (item.isNotNullItem()) {
                            dataItem.put("temp_id", item.template.id);
                            dataItem.put("quantity", item.quantity);
                            dataItem.put("create_time", item.createTime);
                            JSONArray options = new JSONArray();
                            for (ItemOption io : item.itemOptions) {
                                JSONArray option = new JSONArray();
                                option.add(io.optionTemplate.id);
                                option.add(io.param);
                                options.add(option);
                            }
                            dataItem.put("option", options);
                        } else {
                            JSONArray options = new JSONArray();
                            dataItem.put("temp_id", -1);
                            dataItem.put("quantity", 0);
                            dataItem.put("create_time", 0);
                            dataItem.put("option", options);
                        }
                        dataCrackBall.add(dataItem);
                    }
                    String itemsBoxLuckyRound = dataCrackBall.toJSONString();

                    //data bạn bè
                    JSONArray dataFriends = new JSONArray();
                    for (Friend f : player.friends) {
                        JSONObject friend = new JSONObject();
                        friend.put("id", f.id);
                        friend.put("name", f.name);
                        friend.put("power", f.power);
                        friend.put("head", f.head);
                        friend.put("body", f.body);
                        friend.put("leg", f.leg);
                        friend.put("bag", f.bag);
                        dataFriends.add(friend);
                    }
                    String friend = dataFriends.toJSONString();

                    //data kẻ thù
                    JSONArray dataEnemies = new JSONArray();
                    for (Friend e : player.enemies) {
                        JSONObject enemy = new JSONObject();
                        enemy.put("id", e.id);
                        enemy.put("name", e.name);
                        enemy.put("power", e.power);
                        enemy.put("head", e.head);
                        enemy.put("body", e.body);
                        enemy.put("leg", e.leg);
                        enemy.put("bag", e.bag);
                        dataEnemies.add(enemy);
                    }
                    String enemy = dataEnemies.toJSONString();

                    //data nội tại
                    JSONArray dataIntrinsic = new JSONArray();
                    dataIntrinsic.add(player.playerIntrinsic.intrinsic.id);
                    dataIntrinsic.add(player.playerIntrinsic.intrinsic.param1);
                    dataIntrinsic.add(player.playerIntrinsic.countOpen);
                    dataIntrinsic.add(player.playerIntrinsic.intrinsic.param2);
                    String intrinsic = dataIntrinsic.toJSONString();

                    //data item time
                    JSONArray dataItemTime = new JSONArray();
                    dataItemTime.add(player.itemTime.isUseBoKhi ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi)) : 0);
                    dataItemTime.add(player.itemTime.isUseAnDanh ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh)) : 0);
                    dataItemTime.add(player.itemTime.isOpenPower ? (ItemTime.TIME_OPEN_POWER - (System.currentTimeMillis() - player.itemTime.lastTimeOpenPower)) : 0);
                    dataItemTime.add(player.itemTime.isUseCuongNo ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo)) : 0);
                    dataItemTime.add(player.itemTime.isUseMayDo ? (ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo)) : 0);
                    dataItemTime.add(player.itemTime.isUseBoHuyet ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet)) : 0);
                    dataItemTime.add(player.itemTime.iconMeal);
                    dataItemTime.add(player.itemTime.isEatMeal ? (ItemTime.TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal)) : 0);
                    dataItemTime.add(player.itemTime.isUseGiapXen ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen)) : 0);
                    dataItemTime.add(player.itemTime.isUseBanhChung ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBanhChung)) : 0);
                    dataItemTime.add(player.itemTime.isUseBanhTet ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBanhTet)) : 0);

                    dataItemTime.add(player.itemTime.isUseBoKhi2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi2)) : 0);
                    dataItemTime.add(player.itemTime.isUseGiapXen2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen2)) : 0);
                    dataItemTime.add(player.itemTime.isUseCuongNo2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo2)) : 0);
                    dataItemTime.add(player.itemTime.isUseBoHuyet2 ? (ItemTime.TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet2)) : 0);
                    String itemTime = dataItemTime.toJSONString();

                    //data nhiệm vụ
                    JSONArray dataTask = new JSONArray();
                    if (player.playerTask.taskMain != null && player.playerTask.taskMain.subTasks != null) {
                        dataTask.add(player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count);
                        dataTask.add(player.playerTask.taskMain.id);
                        dataTask.add(player.playerTask.taskMain.index);
                    } else {
                        dataTask.add(0);
                        dataTask.add(0);
                        dataTask.add(0);
                    }
                    String task = dataTask.toJSONString();

                    //data nhiệm vụ hàng ngày
                    JSONArray dataSideTask = new JSONArray();
                    dataSideTask.add(player.playerTask.sideTask.level);
                    dataSideTask.add(player.playerTask.sideTask.count);
                    dataSideTask.add(player.playerTask.sideTask.leftTask);
                    dataSideTask.add(player.playerTask.sideTask.template != null ? player.playerTask.sideTask.template.id : -1);
                    dataSideTask.add(player.playerTask.sideTask.receivedTime);
                    dataSideTask.add(player.playerTask.sideTask.maxCount);
                    String sideTask = dataSideTask.toJSONString();

                    JSONArray dataAchive = new JSONArray();
                    for (Achivement a : player.playerTask.achivements) {
                        JSONObject jobj = new JSONObject();
                        jobj.put("id", a.id);
                        jobj.put("count", a.count);
                        jobj.put("finish", a.finish ? 1 : 0);
                        jobj.put("receive", a.receive ? 1 : 0);
                        dataAchive.add(jobj);
                    }
                    String achive = dataAchive.toJSONString();

                    //data bùa
                    JSONArray dataCharms = new JSONArray();
                    dataCharms.add(player.charms.tdBoHuyet > System.currentTimeMillis() ? (player.charms.tdBoHuyet - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdBoKhi > System.currentTimeMillis() ? (player.charms.tdBoKhi - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdGiapXen > System.currentTimeMillis() ? (player.charms.tdGiapXen - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdThanMeo > System.currentTimeMillis() ? (player.charms.tdThanMeo - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdMaBa > System.currentTimeMillis() ? (player.charms.tdMaBa - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdDeTu > System.currentTimeMillis() ? (player.charms.tdDeTu - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue > System.currentTimeMillis() ? (player.charms.tdTriTue - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue2 > System.currentTimeMillis() ? (player.charms.tdTriTue2 - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue3 > System.currentTimeMillis() ? (player.charms.tdTriTue3 - System.currentTimeMillis()) : 0);
                    dataCharms.add(player.charms.tdTriTue4 > System.currentTimeMillis() ? (player.charms.tdTriTue4 - System.currentTimeMillis()) : 0);
                    String charms = dataCharms.toJSONString();

                    //data kỹ năng
                    JSONArray dataSkills = new JSONArray();
                    for (Skill skill : player.playerSkill.skills) {
                        JSONArray dataSkill = new JSONArray();
                        dataSkill.add(skill.template.id);
                        dataSkill.add(skill.lastTimeUseThisSkill);
                        dataSkill.add(skill.point);
                        dataSkills.add(dataSkill);
                    }
                    String skills = dataSkills.toJSONString();

                    //data shortcut kỹ năng
                    JSONArray dataSkillShortcut = new JSONArray();
                    for (int shortcut : player.playerSkill.skillShortCut) {
                        dataSkillShortcut.add(shortcut);
                    }
                    String skillsShortcut = dataSkillShortcut.toJSONString();

                    //data đệ tử
                    JSONObject petInfo = new JSONObject();
                    JSONObject petPoint = new JSONObject();
                    JSONArray petBody = new JSONArray();
                    JSONArray petSkill = new JSONArray();
                    if (player.pet != null) {
                        petInfo.put("name", player.pet.name);
                        petInfo.put("gender", player.pet.gender);
                        petInfo.put("is_mabu", player.pet.typePet);
                        petInfo.put("status", player.pet.status);
                        petInfo.put("type_fusion", player.fusion.typeFusion);
                        int leftFusion = (int) (Fusion.TIME_FUSION - (System.currentTimeMillis() - player.fusion.lastTimeFusion));
                        petInfo.put("left_fusion", leftFusion < 0 ? 0 : leftFusion);
                        petInfo.put("level", player.pet.getLever());

                        petPoint.put("hp", player.pet.nPoint.hp);
                        petPoint.put("mp", player.pet.nPoint.mp);
                        petPoint.put("hpg", player.pet.nPoint.hpg);
                        petPoint.put("mpg", player.pet.nPoint.mpg);
                        petPoint.put("damg", player.pet.nPoint.dameg);
                        petPoint.put("defg", player.pet.nPoint.defg);
                        petPoint.put("critg", player.pet.nPoint.critg);
                        petPoint.put("stamina", player.pet.nPoint.stamina);
                        petPoint.put("max_stamina", player.pet.nPoint.maxStamina);
                        petPoint.put("power", player.pet.nPoint.power);
                        petPoint.put("tiem_nang", player.pet.nPoint.tiemNang);
                        petPoint.put("limit_power", player.pet.nPoint.limitPower);

                        for (Item item : player.pet.inventory.itemsBody) {
                            JSONObject dataItem = new JSONObject();
                            if (item.isNotNullItem()) {
                                dataItem.put("temp_id", item.template.id);
                                dataItem.put("quantity", item.quantity);
                                dataItem.put("create_time", item.createTime);
                                JSONArray options = new JSONArray();
                                for (ItemOption io : item.itemOptions) {
                                    JSONArray option = new JSONArray();
                                    option.add(io.optionTemplate.id);
                                    option.add(io.param);
                                    options.add(option);
                                }
                                dataItem.put("option", options);
                            } else {
                                JSONArray options = new JSONArray();
                                dataItem.put("temp_id", -1);
                                dataItem.put("quantity", 0);
                                dataItem.put("create_time", 0);
                                dataItem.put("option", options);
                            }
                            petBody.add(dataItem);
                        }

                        for (Skill skill : player.pet.playerSkill.skills) {
                            JSONArray dataSkill = new JSONArray();
                            dataSkill.add(skill.template.id);
                            dataSkill.add(skill.point);
                            petSkill.add(dataSkill);
                        }
                    }

                    String petInfoData = petInfo.toJSONString();
                    String petPointData = petPoint.toJSONString();
                    String petBodyData = petBody.toJSONString();
                    String petSkillData = petSkill.toJSONString();

                    //data quả trứng
                    JSONObject mabuEgg = new JSONObject();
                    if (player.mabuEgg != null) {
                        mabuEgg.put("last_time_harvest", player.mabuEgg.lastTimeHarvest);
                        mabuEgg.put("time_done", player.mabuEgg.timeDone);
                    }
                    String mabuEggData = mabuEgg.toJSONString();

                    //data quả trứng bill
                    JSONObject billEgg = new JSONObject();
                    if (player.billEgg != null) {
                        billEgg.put("last_time_harvest", player.billEgg.lastTimeHarvest);
                        billEgg.put("time_done", player.billEgg.timeDone);
                    }
                    String billEggData = billEgg.toJSONString();

                    //data black ball
                    JSONArray dataBlackBall = new JSONArray();
                    for (int i = 0; i < player.rewardBlackBall.timeOutOfDateReward.length; i++) {
                        JSONArray arr = new JSONArray();
                        arr.add(player.rewardBlackBall.timeOutOfDateReward[i]);
                        arr.add(player.rewardBlackBall.lastTimeGetReward[i]);
                        dataBlackBall.add(arr);
                    }
                    String blackBall = dataBlackBall.toJSONString();

                    String query = "update player set head = ?, have_tennis_space_ship = ?, clan_id_sv" + (int) Manager.SERVER + " = ?, "
                            + "data_inventory = ?, data_location = ?, data_point = ?, data_magic_tree = ?, items_body = ?, "
                            + "items_bag = ?, items_box = ?, items_box_lucky_round = ?, friends = ?, enemies = ?, data_intrinsic = ?, data_item_time = ?, "
                            + "data_task = ?, data_mabu_egg = ?, data_bill_egg = ?, data_charm = ?, skills = ?, skills_shortcut = ?, pet_info = ?, pet_point = ?, pet_body = ?, pet_skill = ?, "
                            + "data_black_ball = ?, thoi_vang = ?, data_side_task = ?, last_logout_time = ?, achivements = ?, is_jaco = ? where id = ?";
                    PreparedStatement ps = connection.prepareStatement(query);
                    ps.setShort(1, player.head);
                    ps.setBoolean(2, player.haveTennisSpaceShip);
                    ps.setInt(3, (player.clan != null ? (int) player.clan.id : -1));
                    ps.setString(4, inventory);
                    ps.setString(5, location);
                    ps.setString(6, point);
                    ps.setString(7, magicTree);
                    ps.setString(8, itemsBody);
                    ps.setString(9, itemsBag);
                    ps.setString(10, itemsBox);
                    ps.setString(11, itemsBoxLuckyRound);
                    ps.setString(12, friend);
                    ps.setString(13, enemy);
                    ps.setString(14, intrinsic);
                    ps.setString(15, itemTime);
                    ps.setString(16, task);
                    ps.setString(17, mabuEggData);
                    ps.setString(18, billEggData);
                    ps.setString(19, charms);
                    ps.setString(20, skills);
                    ps.setString(21, skillsShortcut);
                    ps.setString(22, petInfoData);
                    ps.setString(23, petPointData);
                    ps.setString(24, petBodyData);
                    ps.setString(25, petSkillData);
                    ps.setString(26, blackBall);
                    ps.setInt(27, tv);
                    ps.setString(28, sideTask);
                    ps.setTimestamp(29, new Timestamp(System.currentTimeMillis()));
                    ps.setString(30, achive);
                    ps.setBoolean(31, player.isJaco);
                    ps.setInt(32, (int) player.id);
                    if (ps.executeUpdate() > 0) {
                        SieuHangManager.UpdatePlayerInfo(player);
                    }
                    ps.close();
                } catch (Exception e) {
                    Log.error(PlayerDAO.class, e, "Lỗi update player " + player.name);
                }
            }
        } catch (Exception e) {
            Log.error(PlayerDAO.class, e, "Lỗi update player " + player.name);
        } finally {
            player.setSaving(false);
        }
    }
}
