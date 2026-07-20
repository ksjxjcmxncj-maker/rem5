package nro.services.func;

import java.io.IOException;
import java.util.ArrayList;
import nro.consts.ConstAchive;
import nro.consts.ConstItem;
import nro.consts.ConstNpc;
import nro.jdbc.daos.PlayerDAO;
import nro.models.item.CaiTrang;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.server.Manager;
import nro.server.io.Message;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.utils.Log;
import nro.utils.Util;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nro.models.skill.Skill;
import nro.server.MenuController;
import nro.utils.SkillUtil;

/**
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 */
public class ShopService {

    private static final int COST_GOLD_BAR = 500000000;
    private static final int COST_LOCK_GOLD_BAR = 300000000;

    private static final byte NORMAL_SHOP = 0;
    private static final byte SPEC_SHOP = 3;
    private static final byte LEARN_SKILL = 1;

    private static ShopService i;

    public static synchronized ShopService gI() {
        if (i == null) {
            i = new ShopService();
        }
        return i;
    }

    //Lấy ra itemshop khi mua
    private ItemShop getItemShop(Player player, int shopId, int tempId) {
        ItemShop itemShop = null;
        Shop shop = null;
        switch (shopId) {
            case ConstNpc.SHOP_BUNMA_QK_0:
                shop = getShop(player, ConstNpc.BUNMA, 0, -1);
                break;
            case ConstNpc.SHOP_DENDE_0:
                shop = getShop(player, ConstNpc.DENDE, 0, -1);
                break;
            case ConstNpc.SHOP_APPULE_0:
                shop = getShop(player, ConstNpc.APPULE, 0, -1);
                break;
            case ConstNpc.SHOP_URON_0:
                shop = getShop(player, ConstNpc.URON, 0, -1);
                break;
            case ConstNpc.SHOP_SANTA_0:
                shop = getShop(player, ConstNpc.SANTA, 0, -1);
                break;
            case ConstNpc.SHOP_SANTA_1:
                shop = getShop(player, ConstNpc.SANTA, 1, -1);
                break;
            case ConstNpc.SHOP_SANTA_2:
                shop = getShop(player, ConstNpc.SANTA, 2, -1);
                break;
            case ConstNpc.SHOP_SANTA_3:
                shop = getShop(player, ConstNpc.SANTA, 3, -1);
                break;
            case ConstNpc.SHOP_SANTA_4:
                shop = getShop(player, ConstNpc.SANTA, 4, -1);
                break;
            case ConstNpc.SHOP_BA_HAT_MIT_0:
                shop = getShop(player, ConstNpc.BA_HAT_MIT, 0, -1);
                break;
            case ConstNpc.SHOP_BA_HAT_MIT_1:
                shop = getShop(player, ConstNpc.BA_HAT_MIT, 1, -1);
                break;
            case ConstNpc.SHOP_BA_HAT_MIT_2:
                shop = getShop(player, ConstNpc.BA_HAT_MIT, 2, -1);
                break;
            case ConstNpc.SHOP_BA_HAT_MIT_3:
                shop = getShop(player, ConstNpc.BA_HAT_MIT, 3, -1);
                break;
            case ConstNpc.SHOP_BUNMA_TL_0:
                shop = getShop(player, ConstNpc.BUNMA_TL, 0, -1);
                break;
            case ConstNpc.SHOP_BILL_HUY_DIET_0:
                shop = getShop(player, ConstNpc.BILL, 0, -1);
                break;
            case ConstNpc.SHOP_WHIS_THIEN_SU:
                shop = getShop(player, ConstNpc.WHIS, 0, -1);
                break;
            case ConstNpc.SHOP_HONG_NGOC:
                shop = getShop(player, ConstNpc.QUY_LAO_KAME, 0, -1);
                break;
            case ConstNpc.SHOP_LY_TIEU_NUONG:
                shop = getShop(player, ConstNpc.LY_TIEU_NUONG, 0, -1);
                break;
            case ConstNpc.SHOP_LEARN_SKILL:
                shop = getShop(ConstNpc.QUY_LAO_KAME, 0, -1);
                break;
            case ConstNpc.SHOP_SU_KIEN_TET:
                shop = getShop(player, ConstNpc.QUY_LAO_KAME, 1, -1);
                break;
            case ConstNpc.SHOP_TORIBOT:
                shop = getShop(player, ConstNpc.TORIBOT, 0, -1);
                break;
            case ConstNpc.SHOP_TORIBOT_2:
                shop = getShop(player, ConstNpc.TORIBOT, 1, -1);
                break;    
        }
        if (shop != null) {
            for (TabShop tab : shop.tabShops) {
                for (ItemShop is : tab.itemShops) {
                    if (is.temp.id == tempId) {
                        itemShop = is;
                        break;
                    }
                }
                if (itemShop != null) {
                    break;
                }
            }
        }
        return itemShop;
    }

    private Shop getShop(Player player, int npcId, int order, int gender) {
        for (Shop shop : Manager.SHOPS) {
            if (shop.npcId == npcId && shop.shopOrder == order) {
                for (TabShop tabShop : shop.tabShops) {
                    for (ItemShop item : tabShop.itemShops) {
                        switch (item.temp.id) {
                            case 517:// hành trang
                                item.gem = (player.inventory.itemsBag.size() - 19) * 50;
                                break;
                            case 518:// rương đồ
                                item.gold = (player.inventory.itemsBox.size() - 19) * 25_000_000;
                                break;
                        }
                    }
                }
                if (gender != -1) {
                    return new Shop(player, shop, gender);
                } else {
                    return shop;
                }
            }
        }
        return null;
    }

    private Shop getShop(int npcId, int order, int gender) {
        for (Shop shop : Manager.SHOPS) {
            if (shop.npcId == npcId && shop.shopOrder == order) {
                if (gender != -1) {
                    return new Shop(shop, gender);
                } else {
                    return shop;
                }
            }
        }
        return null;
    }

    private Shop getShopHuyDiet(Player player, Shop s) {
        Shop shop = new Shop(s);
        for (TabShop tabShop : shop.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                item.iconSpec = 15012 + item.temp.type;
                item.costSpec = 1;
            }
        }
        return shop;
    }

    private Shop getShoDanhHieu(Player player, Shop s) {
        Shop shop = new Shop(s);
        for (TabShop tabShop : shop.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                switch (item.temp.id) {
                    default:
                        Service.gI().sendThongBao(player, "Bạn chưa mở khóa danh hiệu này");
                }
            }
        }
        return shop;
    }
    
    private void learnSkill(Player player, ItemShop it) {
        Message msg;
        try {
            if (it != null && (it.temp.gender == player.gender || it.temp.gender == 3)) {
                long power = it.getPowerRequire();
                if (player.nPoint.tiemNang < power) {
                    Service.getInstance().sendThongBao(player, "Không đủ " + Util.powerToString(power) + " tiềm năng");
                    return;
                }
                byte level = it.getLevelSkill();
                Skill curSkill = SkillUtil.getSkillByItemID(player, it.temp.id);
                if (curSkill == null) {
                    return;
                }
                if (curSkill.point >= level || curSkill.point == 7) {
                    return;
                }
                if (curSkill.point == 0) {
                    if (level == 1) {
                        player.nPoint.tiemNang -= power;
                        Service.getInstance().point(player);
                        curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(it.temp.id), level);
                        SkillUtil.setSkill(player, curSkill);
                        msg = Service.getInstance().messageSubCommand((byte) 23);
                        msg.writer().writeShort(curSkill.skillId);
                        player.sendMessage(msg);
                        msg.cleanup();
                    } else {
                        Skill skillNeed = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(it.temp.id), level);
                        Service.getInstance().sendThongBao(player, "Vui lòng học " + skillNeed.template.name + " cấp " + skillNeed.point + " trước!");
                    }
                } else {
                    if (curSkill.point + 1 == level) {
                        player.nPoint.tiemNang -= power;
                        Service.getInstance().point(player);
                        curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(it.temp.id), level);
                        SkillUtil.setSkill(player, curSkill);
                        msg = Service.getInstance().messageSubCommand((byte) 62);
                        msg.writer().writeShort(curSkill.skillId);
                        player.sendMessage(msg);
                        msg.cleanup();
                    } else {
                        Service.getInstance().sendThongBao(player, "Vui lòng học " + curSkill.template.name + " cấp " + (curSkill.point + 1) + " trước!");
                    }
                }
                openShopLearnSkill(player, 13, ConstNpc.SHOP_LEARN_SKILL, 0, player.gender);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Shop getShopBua(Player player, Shop s) {
        Shop shop = new Shop(s);
        for (TabShop tabShop : shop.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                long min = 0;
                switch (item.temp.id) {
                    case 213:
                        long timeTriTue = player.charms.tdTriTue;
                        long current = System.currentTimeMillis();
                        min = (timeTriTue - current) / 60000;
                        break;
                    case 214:
                        min = (player.charms.tdManhMe - System.currentTimeMillis()) / 60000;
                        break;
                    case 215:
                        min = (player.charms.tdDaTrau - System.currentTimeMillis()) / 60000;
                        break;
                    case 216:
                        min = (player.charms.tdOaiHung - System.currentTimeMillis()) / 60000;
                        break;
                    case 217:
                        min = (player.charms.tdBatTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 218:
                        min = (player.charms.tdDeoDai - System.currentTimeMillis()) / 60000;
                        break;
                    case 219:
                        min = (player.charms.tdThuHut - System.currentTimeMillis()) / 60000;
                        break;
                    case 522:
                        min = (player.charms.tdDeTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 671:
                        min = (player.charms.tdTriTue3 - System.currentTimeMillis()) / 60000;
                        break;
                    case 672:
                        min = (player.charms.tdTriTue4 - System.currentTimeMillis()) / 60000;
                        break;
                    case 2025:
                        min = (player.charms.tdDeTuMabu - System.currentTimeMillis()) / 60000;
                        break;
                }
                if (min > 0) {
                    item.options.clear();
                    if (min >= 1440) {
                        item.options.add(new ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new ItemOption(65, (int) min));
                    }
                }
            }
        }
        return shop;
    }

    //shop đồ hủy diệt
    public void openShopBillHuyDiet(Player player, int shopId, int order) {
        Shop shop = getShopHuyDiet(player, getShop(player, ConstNpc.BILL, order, -1));
        openShopType0(player, shop, shopId);
    }

    public void openShopWhisThienSu(Player player, int shopId, int order) {
        Shop shop = getShop(player, ConstNpc.WHIS, order, -1);
        openShopType3(player, shop, shopId);
    }

    //shop bùa
    public void openShopBua(Player player, int shopId, int order) {
//        player.iDMark.setShopId(shopId);
        Shop shop = getShopBua(player, getShop(player, ConstNpc.BA_HAT_MIT, order, -1));
        openShopType0(player, shop, shopId);
    }
    public void openShopLearnSkill(Player player, int idNpc, int shopId, int order, int gender) {
        Shop shop = getShop(idNpc, order, gender);
        openShopType1(player, shop, shopId);
    }

    public void openShopLearnSkill(Player player, Npc npc, int shopId, int order, int gender) {
        Shop shop = getShop(npc.tempId, order, gender);
        openShopType1(player, shop, shopId);
    }

    public void openShopDanhHieu(Player player, int shopId, int order) {
        player.iDMark.setShopId(shopId);
        Shop shop = getShoDanhHieu(player, getShop(player, ConstNpc.SANTA, order, -1));
        openShopType0(player, shop, shopId);
    }

    //shop normal
    public void openShopNormal(Player player, Npc npc, int shopId, int order, int gender) {
        Shop shop = getShop(player, npc.tempId, order, gender);
        openShopType0(player, shop, shopId);
    }

    public void openShopSpecial(Player player, Npc npc, int shopId, int order, int gender) {
        Shop shop = getShop(player, npc.tempId, order, gender);
        openShopType3(player, shop, shopId);
    }
    
    private void openShopType1(Player player, Shop shop, int shopId) {
        player.iDMark.setShopId(shopId);
        if (shop != null) {
            Message msg;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(LEARN_SKILL);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    List<ItemShop> listNew = new ArrayList<>();
                    for (int i = 0; i < tab.itemShops.size(); i++) {
                        ItemShop itemShop = tab.itemShops.get(i);
                        if (itemShop != null) {
                            Skill curSkill = SkillUtil.getSkillByItemID(player, itemShop.temp.id);
                            if (curSkill == null || curSkill.point < itemShop.getLevelSkill()) {
                                listNew.add(itemShop);
                            }
                        }
                    }
                    msg.writer().writeByte(listNew.size());
                    for (ItemShop itemShop : listNew) {
                        msg.writer().writeShort(itemShop.temp.id);
                      //  msg.writer().writeLong(itemShop.getPowerRequire());
                        msg.writer().writeByte(itemShop.options.size());
                        for (ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        msg.writer().writeByte(0);
                    }
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(ShopService.class, e);
            }
        }
    }

    private void openShopType0(Player player, Shop shop, int shopId) {
        player.iDMark.setShopId(shopId);
        if (shop != null) {
            Message msg;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    if (tab.id == 39) {
                        msg.writer().writeUTF(tab.name + 0);
                    } else {
                        msg.writer().writeUTF(tab.name);
                    }
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer().writeInt(itemShop.gold);
                        msg.writer().writeInt(itemShop.gem);
                        msg.writer().writeByte(itemShop.options.size());
                        for (ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        CaiTrang caiTrang = Manager.gI().getCaiTrangByItemId(itemShop.temp.id);
                        msg.writer().writeByte(caiTrang != null ? 1 : 0);
                        if (caiTrang != null) {
                            msg.writer().writeShort(caiTrang.getID()[0]);
                            msg.writer().writeShort(caiTrang.getID()[1]);
                            msg.writer().writeShort(caiTrang.getID()[2]);
                            msg.writer().writeShort(caiTrang.getID()[3]);
                        }
                    }
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(ShopService.class, e);
            }
        }
    }

    private void openShopType3(Player player, Shop shop, int shopId) {
        player.iDMark.setShopId(shopId);
        if (shop != null) {
            Message msg;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(SPEC_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
//                    System.out.println("shopId: " + shopId);
                    //System.out.println(tab.name);
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer().writeShort(itemShop.iconSpec);
                        msg.writer().writeInt(itemShop.costSpec);
                        msg.writer().writeByte(itemShop.options.size());
                        for (ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        CaiTrang caiTrang = Manager.gI().getCaiTrangByItemId(itemShop.temp.id);
                        msg.writer().writeByte(caiTrang != null ? 1 : 0);
                        if (caiTrang != null) {
                            msg.writer().writeShort(caiTrang.getID()[0]);
                            msg.writer().writeShort(caiTrang.getID()[1]);
                            msg.writer().writeShort(caiTrang.getID()[2]);
                            msg.writer().writeShort(caiTrang.getID()[3]);
                        }
                    }
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(ShopService.class, e);
            }
        }
    }

    private void buyItemShopNormal(Player player, ItemShop is) {
        if (is != null) {
            int itemShopID = is.temp.id;
            if (is.temp.id == 517 && player.inventory.itemsBag.size() >= 59) {
                Service.getInstance().sendThongBao(player, "Đã đạt mức tối đa");
                Service.getInstance().sendMoney(player);
                return;
            }
            if (is.temp.id == 518 && player.inventory.itemsBox.size() >= 59) {
                Service.getInstance().sendThongBao(player, "Đã đạt mức tối đa");
                Service.getInstance().sendMoney(player);
                return;
            }
            if (is.temp.id == 988 && player.inventory.getGoldLimit() >= 1000000000000L) {
                Service.getInstance().sendThongBao(player, "Giới hạn vàng của bạn đã đạt tối đa");
                Service.getInstance().sendMoney(player);
                return;
            }
            if (is.temp.id == 1309 || is.temp.id == 1309 || is.temp.id == 1310) {
                Service.gI().sendThongBao(player, "Hàng chỉ trưng bày không bán");
                Service.getInstance().sendMoney(player);
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                if (is.temp.id == 361) {
                    Item item = ItemService.gI().createNewItem((short) 361, 10);
                    InventoryService.gI().addItemBag(player, item, 9999);
                    InventoryService.gI().sendItemBags(player);
                    Service.getInstance().sendMoney(player);
                    Service.getInstance().sendThongBao(player, "Mua thành công " + is.temp.name);
                    return;
                }
                int gold = is.gold;
                int gem = is.gem;
                int itemExchange = is.itemExchange;
                if (gold != 0) {
                    if (player.inventory.gold >= gold) {
                        player.inventory.subGold(gold); // FIX: dùng method an toàn
                    } else {
                        Service.getInstance().sendThongBaoOK(player, "Bạn không đủ vàng, còn thiếu "
                                + (Util.numberToMoney(gold - player.inventory.gold) + " vàng"));
                        Service.getInstance().sendMoney(player);
                        return;
                    }
                }
                if (player.nPoint.power < is.temp.strRequire) {
                    Service.gI().sendThongBao(player, "Không đủ sức mạnh để mua cải trang này");
                    Service.getInstance().sendMoney(player);
                    return;
                }
                if (gem != 0) {
                    if (player.inventory.getGem() >= gem) {
                        player.inventory.subGem(gem);
                    } else {
                        Service.getInstance().sendThongBao(player, "Bạn không đủ ngọc, còn thiếu "
                                + (gem - player.inventory.getGem()) + " ngọc");
                        Service.getInstance().sendMoney(player);
                        return;
                    }
                }
                if (itemExchange >= 0) {
                    Item itm = InventoryService.gI().findItemBagByTemp(player, itemExchange);
                    if (isLimitItem(itemShopID)) {
                        if (player.buyLimit[itemShopID - 1074] < getBuyLimit(itemShopID)) {
                            player.buyLimit[itemShopID - 1074]++;
                        } else {
                            Service.getInstance().sendThongBao(player, "Số lượt mua trong ngày đã đạt giới hạn");
                            return;
                        }
                    }
                    if (itemExchange == 861 && player.inventory.getRuby() >= is.costSpec) {
                        player.inventory.subRuby(is.costSpec);
                    } else if (itm != null && itm.isNotNullItem() && itm.quantity >= is.costSpec) {
                        InventoryService.gI().subQuantityItemsBag(player, itm, is.costSpec);
                    } else {
                        Service.getInstance().sendThongBao(player, "Bạn không đủ vật phẩm để trao đổi.");
                        return;
                    }
                }
                if (is.options.size() != 0) {
                    if (is.options.get(0).optionTemplate.id == 247) {
                        is.options.clear();
                        is.options.add(new ItemOption(50, Util.nextInt(10, 25)));
                        is.options.add(new ItemOption(77, Util.nextInt(10, 25)));
                        is.options.add(new ItemOption(103, Util.nextInt(10, 25)));
                        is.options.add(new ItemOption(30, 1));
                    }
                }
                switch (player.iDMark.getShopId()) {
                    case ConstNpc.SHOP_SANTA_1:
                        player.head = is.temp.part;
                        Service.getInstance().Send_Caitrang(player);
                        Service.getInstance().sendThongBao(player, "Đổi kiểu tóc thành công");
                        break;
                    case ConstNpc.SHOP_SANTA_3:
                        Service.gI().sendThongBao(player, "Bạn chưa mở khóa danh hiệu này");
                        break;
                    case ConstNpc.SHOP_BA_HAT_MIT_0:
                        player.charms.addTimeCharms(is.temp.id, 60);
                        openShopBua(player, player.iDMark.getShopId(), 0);
                        break;
                    case ConstNpc.SHOP_BA_HAT_MIT_1:
                        player.charms.addTimeCharms(is.temp.id, 60 * 8);
                        openShopBua(player, player.iDMark.getShopId(), 1);
                        break;
                    case ConstNpc.SHOP_BA_HAT_MIT_2:
                        player.charms.addTimeCharms(is.temp.id, 60 * 24 * 30);
                        openShopBua(player, player.iDMark.getShopId(), 2);
                        break;
                    case ConstNpc.SHOP_BA_HAT_MIT_3:
                        player.charms.addTimeCharms(is.temp.id, 60);
                        openShopBua(player, player.iDMark.getShopId(), 3);
                        break;
                    case ConstNpc.SHOP_BILL_HUY_DIET_0:
                        if (player.setClothes.godClothes) {
                            Item meal = InventoryService.gI().findMealChangeDestroyClothes(player);
                            if (meal != null) {
                                Item item = ItemService.gI().createItemFromItemShop(is);
                                int param = 0;
                                if (Util.isTrue(2, 10)) {
                                    param = Util.nextInt(10, 15);
                                } else if (Util.isTrue(3, 10)) {
                                    param = Util.nextInt(0, 10);
                                }
                                for (ItemOption io : item.itemOptions) {
                                    int optId = io.optionTemplate.id;
                                    switch (optId) {
                                        case 47: //giáp
                                        case 6: //hp
                                        case 26: //hp/30s
                                        case 22: //hp k
                                        case 0: //sức đánh
                                        case 7: //ki
                                        case 28: //ki/30s
                                        case 23: //ki k
                                        case 14: //crit
                                            io.param += ((long) io.param * param / 100);
                                            break;
                                    }
                                }
                                InventoryService.gI().subQuantityItemsBag(player, meal, 99);
                                InventoryService.gI().addItemBag(player, item, 99);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Đổi thành công " + is.temp.name);
                            } else {
                                Service.getInstance().sendThongBao(player, "Yêu cầu có 99 thức ăn");
                                return;
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Yêu cầu có đủ set Thần Linh");
                            return;
                        }
                        break;
                    default:
                        Item item = ItemService.gI().createItemFromItemShop(is);
                        if (!InventoryService.gI().canAddItemToBag(player, item)) {
                            Service.gI().sendThongBao(player, "Túi đồ đầy");
                            return;
                        }
                        InventoryService.gI().addItemBag(player, item, 99);
                        InventoryService.gI().sendItemBags(player);
                        Service.getInstance().sendMoney(player);
                        Service.getInstance().sendThongBao(player, "Mua thành công " + is.temp.name);
                        break;
                }
            } else {
                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
            }
        }
    }

    private void buyItemShopSpecial(Player player, ItemShop is) {
        if (is != null) {
            int itemShopID = is.temp.id;
            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                int spec = is.costSpec;
                if (spec != 0) {
                    switch (player.iDMark.getShopId()) {
                        case ConstNpc.SHOP_BILL_HUY_DIET_0:
                            Item m = InventoryService.gI().findMealChangeDestroyClothes(player);
                            if (m == null || m.quantity < 99) {
                                Service.getInstance().sendThongBao(player, "Bạn không đủ thức ăn để đổi");
                                return;
                            }
                            break;
                        case ConstNpc.SHOP_WHIS_THIEN_SU:
                            Item s = InventoryService.gI().findSymbolWhis(player);
                            if (s == null || s.quantity < spec) {
                                Service.getInstance().sendThongBao(player, "Bạn không đủ mảnh hồn để đổi");
                                return;
                            }
                            break;
                        case ConstNpc.SHOP_HONG_NGOC:
                            if (player.inventory.ruby < spec) {
                                Service.getInstance().sendThongBao(player, "Bạn không đủ hồng ngọc để mua");
                                return;
                            }
                            break;
                        case ConstNpc.SHOP_LY_TIEU_NUONG:
                            Item it = InventoryService.gI().findItemBagByTemp(player, (short) 2031);
                            if (it == null || it.quantity < spec) {
                                Service.getInstance().sendThongBao(player, "Bạn không đủ hoa tuyết để mua");
                                return;
                            }
                            break;
                        case ConstNpc.SHOP_SU_KIEN_TET:
                            Item dao = InventoryService.gI().findItemBagByTemp(player, (short) 695);
                            Item quat = InventoryService.gI().findItemBagByTemp(player, (short) 696);
                            Item xoi = InventoryService.gI().findItemBagByTemp(player, (short) 697);
                            Item thit = InventoryService.gI().findItemBagByTemp(player, (short) 698);
                            if (dao == null || dao.quantity < spec || quat == null || quat.quantity < spec || xoi == null || xoi.quantity < spec || thit == null || thit.quantity < spec) {
                                Service.getInstance().sendThongBao(player, "Bạn không đủ vật phẩm để đổi");
                                return;
                            }
                            break;
                    }
                }
                Item item = ItemService.gI().createItemFromItemShop(is);
                if (!InventoryService.gI().canAddItemToBag(player, item)) {
                    Service.gI().sendThongBao(player, "Túi đồ đầy");
                    return;
                }
                switch (player.iDMark.getShopId()) {
                    case ConstNpc.SHOP_BILL_HUY_DIET_0:
                        Item meal = InventoryService.gI().findMealChangeDestroyClothes(player);
                        InventoryService.gI().subQuantityItemsBag(player, meal, 99);
                        break;
                    case ConstNpc.SHOP_WHIS_THIEN_SU:
                        Item symbol = InventoryService.gI().findSymbolWhis(player);
                        InventoryService.gI().subQuantityItemsBag(player, symbol, spec);
                        break;
                    case ConstNpc.SHOP_HONG_NGOC:
                        player.inventory.subRuby(spec);
                        Service.getInstance().sendMoney(player);
                        break;
                    case ConstNpc.SHOP_LY_TIEU_NUONG:
                        Item it = InventoryService.gI().findItemBagByTemp(player, (short) 2031);
                        InventoryService.gI().subQuantityItemsBag(player, it, spec);
                        break;
                    case ConstNpc.SHOP_SU_KIEN_TET:
                        Item dao = InventoryService.gI().findItemBagByTemp(player, (short) 695);
                        Item quat = InventoryService.gI().findItemBagByTemp(player, (short) 696);
                        Item xoi = InventoryService.gI().findItemBagByTemp(player, (short) 697);
                        Item thit = InventoryService.gI().findItemBagByTemp(player, (short) 698);
                        InventoryService.gI().subQuantityItemsBag(player, dao, spec);
                        InventoryService.gI().subQuantityItemsBag(player, quat, spec);
                        InventoryService.gI().subQuantityItemsBag(player, xoi, spec);
                        InventoryService.gI().subQuantityItemsBag(player, thit, spec);
                        break;
                }
                InventoryService.gI().addItemBag(player, item, 99);
                InventoryService.gI().sendItemBags(player);
                Service.getInstance().sendThongBao(player, "Mua thành công " + is.temp.name);
            } else {
                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
            }
        }
    }

    public void buyItem(Player player, int shopId, int tempId) {
        ItemShop is = getItemShop(player, shopId, tempId);
        if (is != null) {
            if (shopId == ConstNpc.SHOP_LEARN_SKILL) {
                learnSkill(player, is);
            } else if (is.isSpec()) {
                buyItemShopSpecial(player, is);
            } else {
                buyItemShopNormal(player, is);
            }
        }
    }

    public void showConfirmBuyItem(Player player, int shopId, int tempId) {
        ItemShop is = getItemShop(player, shopId, tempId);
        if (is != null) {
            Message msg;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(2);
                msg.writer().writeByte(shopId);
                msg.writer().writeShort(tempId);
                msg.writer().writeUTF("Bạn có muốn mua " + is.temp.name + " với giá " + (is.isSpec() ? is.costSpec + " " + is.getSpecName() : (is.gold != 0 ? Util.numberToMoney(is.gold) + " vàng" : is.gem + " ngọc")) + "?");
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Log.error(ShopService.class, e);
            }
        }
    }

    private boolean isLimitItem(int id) {
        return id >= 1074 && id <= 1086;
    }

    private int getBuyLimit(int id) {
        switch (id) {
            case 1074:
            case 1075:
            case 1076:
            case 1077:
            case 1078:
            case 1079:
            case 1080:
            case 1081:
            case 1082:
            case 1083:
            case 1084:
            case 1085:
            case 1086:
                return 10;
        }
        return 0;
    }
}
