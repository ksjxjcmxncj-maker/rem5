package nro.models.npc;

import nro.services.func.minigame.ChonAiDay_Gem;
import nro.services.func.minigame.ChonAiDay_Ruby;
import nro.services.func.minigame.ChonAiDay_Gold;
import nro.attr.Attribute;
import nro.attr.AttributeManager;
import nro.consts.*;
import nro.dialog.ConfirmDialog;
import nro.dialog.MenuDialog;
import nro.jdbc.daos.PlayerDAO;
import nro.lib.RandomCollection;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.boss.event.EscortedBoss;
import nro.models.boss.event.Qilin;
import nro.models.boss.mapoffline.Boss_Kingkai;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTemplate;
import nro.models.map.ItemMap;
import nro.models.map.Map;
import nro.models.map.SantaCity;
import nro.models.map.Zone;
import nro.models.map.DaiHoiVoThuat.DHVT23Service;
import nro.models.map.dungeon.SnakeRoad;
import nro.models.map.dungeon.zones.ZSnakeRoad;
import nro.models.map.mabu.MabuWar;
import nro.models.map.phoban.DoanhTrai;
import nro.models.map.war.BlackBallWar;
import nro.models.map.war.NamekBallWar;
import nro.models.player.Inventory;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.noti.NotiManager;
import nro.server.Maintenance;
import nro.server.Manager;
import nro.server.ServerManager;
import nro.server.io.Message;
import nro.services.*;
import nro.services.func.*;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;
import nro.utils.Util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import nro.models.boss.BossData;
import nro.models.boss.Potaufeu.Boss_NhanBan;
import nro.models.boss.mabu_war.Goku_Tang5;
import nro.models.boss.mapoffline.Boss_MrPôPô;
import nro.models.boss.mapoffline.Boss_ThanMeo;
import nro.models.boss.mapoffline.Boss_ThuongDe;
import nro.models.boss.mapoffline.Boss_Whis;
import nro.models.boss.mapoffline.Boss_Yanjiro;
import nro.models.boss.mapoffline.NPC_ToSuKaio;
import nro.models.consignment.ConsignmentShop;
import static nro.models.item.ItemTime.TEXT_NHIEM_VU_HANG_NGAY;
import nro.models.map.DaiHoiVoThuat.DaiHoiVoThuatManager;
import nro.models.map.DaiHoiVoThuat.DaiHoiVoThuatService;
import nro.models.map.VoDaiSinhTu.VoDaiSinhTuService;
import nro.models.map.mabu.MabuWar14h;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.map.phoban.KhiGas;
import nro.models.npc.NpcForge.*;
import nro.models.npc.NpcForge.CuaHangKyGui;
import nro.server.Controller;

import static nro.server.Manager.*;
import nro.server.TaiXiu;
import static nro.services.func.CombineServiceNew.CHE_TAO_DO_THIEN_SU;
import static nro.services.func.CombineServiceNew.NANG_CAP_BONG_TAI;
import static nro.services.func.CombineServiceNew.NANG_CAP_SKH;
import static nro.services.func.Input.ADD_ITEM;
import static nro.services.func.Input.NUMERIC;
import static nro.services.func.SummonDragon.*;

/**
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 */
public class NpcFactory {

    private static boolean nhanVang = true;
    private static boolean nhanDeTu = true;

    // playerid - object
    public static final java.util.Map<Long, Object> PLAYERID_OBJECT = new ConcurrentHashMap<>(); // FIX: thread-safe

    private NpcFactory() {

    }

    public static Npc createNPC(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        Npc npc = null;
        try {
            switch (tempId) {
                case ConstNpc.TORIBOT:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Chào mừng bạn đến với cửa hàng đá quý số 1 thời đại",
                                    "Cửa Hàng\n(1)", "Cửa Hàng\n(2)");
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (select) {
                                    case 0:
                                        ShopService.gI().openShopSpecial(player, this, ConstNpc.SHOP_TORIBOT, 0, -1);
                                        break;
                                    case 1:
                                        ShopService.gI().openShopSpecial(player, this, ConstNpc.SHOP_TORIBOT_2, 1, -1);
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.NGO_KHONG:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Chu mi nga", "Tặng quả\nHồng đào\nChín");
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                int itemNeed = ConstItem.QUA_HONG_DAO_CHIN;
                                Item item = InventoryService.gI().findItemBagByTemp(player, itemNeed);
                                if (item != null) {
                                    RandomCollection<Integer> rc = Manager.HONG_DAO_CHIN;
                                    int itemID = rc.next();
                                    int x = cx + Util.nextInt(-50, 50);
                                    int y = player.zone.map.yPhysicInTop(x, cy - 24);
                                    int quantity = 1;
                                    if (itemID == ConstItem.HONG_NGOC) {
                                        quantity = Util.nextInt(1, 2);
                                    }
                                    InventoryService.gI().subQuantityItemsBag(player, item, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    ItemMap itemMap = new ItemMap(player.zone, itemID, quantity, x, y, player.id);
                                    Service.getInstance().dropItemMap(player.zone, itemMap);
                                    npcChat(player.zone, "Xie xie");
                                } else {
                                    Service.getInstance().sendThongBao(player, "Không tìm thấy!");
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.DUONG_TANG:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (this.mapId == MapName.LANG_ARU) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                        "A mi phò phò, thí chủ hãy giúp giải cứu đồ đệ của bần tăng đang bị phong ấn tại ngũ hành sơn.",
                                        "Đồng ý", "Từ chối");
                            }
                            if (this.mapId == MapName.NGU_HANH_SON_3) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                        "A mi phò phò, thí chủ hãy thu thập bùa 'giải khai phong ấn', mỗi chữ 10 cái.",
                                        "Về\nLàng Aru", "Từ chối");
                            }
                            if (this.mapId == MapName.NGU_HANH_SON) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                        "A mi phò phò, thí chủ hãy thu thập bùa 'giải khai phong ấn', mỗi chữ 10 cái.",
                                        "Đổi đào chín", "Giải phong ấn", "Từ chối");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                /*                                if (this.mapId == MapName.LANG_ARU) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:

                                                if (!Manager.gI().getGameConfig().isOpenPrisonPlanet()) {
                                                    Service.getInstance().sendThongBao(player,
                                                            "Lối vào ngũ hành sơn chưa mở");
                                                    return;
                                                }

                                                Zone zone = MapService.gI().getZoneJoinByMapIdAndZoneId(player, 124, 0);
                                                if (zone != null) {
                                                    player.location.x = 100;
                                                    player.location.y = 384;
                                                    MapService.gI().goToMap(player, zone);
                                                    Service.getInstance().clearMap(player);
                                                    zone.mapInfo(player);
                                                    player.zone.loadAnotherToMe(player);
                                                    player.zone.load_Me_To_Another(player);
                                                }
                                                // Service.getInstance().sendThongBao(player, "Lối vào ngũ hành sơn chưa
                                                // mở");
                                                break;
                                        }
                                    }
                                }*/
                                if (this.mapId == MapName.NGU_HANH_SON_3) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                Zone zone = MapService.gI().getZoneJoinByMapIdAndZoneId(player, 0, 0);
                                                if (zone != null) {
                                                    player.location.x = 600;
                                                    player.location.y = 432;
                                                    MapService.gI().goToMap(player, zone);
                                                    Service.getInstance().clearMap(player);
                                                    zone.mapInfo(player);
                                                    player.zone.loadAnotherToMe(player);
                                                    player.zone.load_Me_To_Another(player);
                                                }
                                                break;
                                        }
                                    }
                                }
                                if (this.mapId == MapName.NGU_HANH_SON) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                // Đổi đào
                                                Item item = InventoryService.gI().findItemBagByTemp(player,
                                                        ConstItem.QUA_HONG_DAO);
                                                if (item == null || item.quantity < 10) {
                                                    npcChat(player,
                                                            "Cần 10 quả đào xanh để đổi lấy đào chín từ bần tăng.");
                                                    return;
                                                }
                                                if (InventoryService.gI().getCountEmptyBag(player) == 0) {
                                                    npcChat(player, "Túi đầy rồi kìa.");
                                                    return;
                                                }
                                                Item newItem = ItemService.gI()
                                                        .createNewItem((short) ConstItem.QUA_HONG_DAO_CHIN, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, item, 10);
                                                InventoryService.gI().addItemBag(player, newItem, 0);
                                                InventoryService.gI().sendItemBags(player);
                                                npcChat(player,
                                                        "Ta đã đổi cho thí chủ rồi đó, hãy mang cho đệ tử ta đi nào.");
                                                break;

                                            case 1:
                                                // giải phong ấn
                                                if (InventoryService.gI().getCountEmptyBag(player) == 0) {
                                                    npcChat(player, "Túi đầy rồi kìa.");
                                                    return;
                                                }
                                                int[] itemsNeed = {ConstItem.CHU_GIAI, ConstItem.CHU_KHAI,
                                                    ConstItem.CHU_PHONG, ConstItem.CHU_AN};
                                                List<Item> items = InventoryService.gI().getListItem(player, itemsNeed)
                                                        .stream().filter(i -> i.quantity >= 10)
                                                        .collect(Collectors.toList());
                                                boolean[] flags = new boolean[4];
                                                for (Item i : items) {
                                                    switch ((int) i.template.id) {
                                                        case ConstItem.CHU_GIAI:
                                                            flags[0] = true;
                                                            break;

                                                        case ConstItem.CHU_KHAI:
                                                            flags[1] = true;
                                                            break;

                                                        case ConstItem.CHU_PHONG:
                                                            flags[2] = true;
                                                            break;

                                                        case ConstItem.CHU_AN:
                                                            flags[3] = true;
                                                            break;
                                                    }
                                                }
                                                for (int i = 0; i < flags.length; i++) {
                                                    if (!flags[i]) {
                                                        ItemTemplate template = ItemService.gI()
                                                                .getTemplate(itemsNeed[i]);
                                                        npcChat("Thí chủ còn thiếu " + template.name);
                                                        return;
                                                    }
                                                }

                                                for (Item i : items) {
                                                    InventoryService.gI().subQuantityItemsBag(player, i, 10);
                                                }

                                                RandomCollection<Integer> rc = new RandomCollection<>();
                                                rc.add(10, ConstItem.CAI_TRANG_TON_NGO_KHONG_DE_TU);
                                                rc.add(10, ConstItem.CAI_TRANG_BAT_GIOI_DE_TU);
                                                rc.add(50, ConstItem.GAY_NHU_Y);
                                                switch (player.gender) {
                                                    case ConstPlayer.TRAI_DAT:
                                                        rc.add(30, ConstItem.CAI_TRANG_TON_NGO_KHONG);
                                                        break;

                                                    case ConstPlayer.NAMEC:
                                                        rc.add(30, ConstItem.CAI_TRANG_TON_NGO_KHONG_545);
                                                        break;

                                                    case ConstPlayer.XAYDA:
                                                        rc.add(30, ConstItem.CAI_TRANG_TON_NGO_KHONG_546);
                                                        break;
                                                }
                                                int itemID = rc.next();
                                                Item nItem = ItemService.gI().createNewItem((short) itemID);
                                                boolean all = itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_DE_TU
                                                        || itemID == ConstItem.CAI_TRANG_BAT_GIOI_DE_TU
                                                        || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG
                                                        || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_545
                                                        || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_546;
                                                if (all) {
                                                    nItem.itemOptions.add(new ItemOption(50, Util.nextInt(20, 30)));
                                                    nItem.itemOptions.add(new ItemOption(77, Util.nextInt(20, 100)));
                                                    nItem.itemOptions.add(new ItemOption(103, Util.nextInt(20, 100)));
                                                    nItem.itemOptions.add(new ItemOption(93, Util.nextInt(5, 10)));
                                                    nItem.itemOptions.add(new ItemOption(100, Util.nextInt(10, 2000)));
                                                    nItem.itemOptions.add(new ItemOption(101, Util.nextInt(100, 1000)));
                                                }
                                                if (itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG
                                                        || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_545
                                                        || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_546) {
                                                    nItem.itemOptions.add(new ItemOption(80, Util.nextInt(5, 15)));
                                                    nItem.itemOptions.add(new ItemOption(81, Util.nextInt(5, 15)));
                                                    nItem.itemOptions.add(new ItemOption(106, 0));
                                                } else if (itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_DE_TU
                                                        || itemID == ConstItem.CAI_TRANG_BAT_GIOI_DE_TU) {
                                                    nItem.itemOptions.add(new ItemOption(197, 0));
                                                }
                                                if (all) {
                                                    if (Util.isTrue(499, 500)) {
                                                        nItem.itemOptions.add(new ItemOption(93, Util.nextInt(3, 30)));
                                                    }
                                                } else if (itemID == ConstItem.GAY_NHU_Y) {
                                                    RandomCollection<Integer> rc2 = new RandomCollection<>();
                                                    rc2.add(60, 30);
                                                    rc2.add(30, 90);
                                                    rc2.add(10, 365);
                                                    nItem.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                                                    nItem.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));
                                                    nItem.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                                                    nItem.itemOptions.add(new ItemOption(93, rc2.next()));
                                                }
                                                InventoryService.gI().addItemBag(player, nItem, 0);
                                                InventoryService.gI().sendItemBags(player);
                                                npcChat(player.zone,
                                                        "A mi phò phò, đa tạ thí chủ tương trợ, xin hãy nhận món quà mọn này, bần tăng sẽ niệm chú giải thoát cho Ngộ Không");
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.TAPION:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 19) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Ác quỷ truyền thuyết Hirudegarn\nđã thoát khỏi phong ấn ngàn năm\nHãy giúp tôi chế ngự nó",
                                            "OK", "Từ chối");
                                }
                                if (this.mapId == 126) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Tôi sẽ đưa bạn về", "OK",
                                            "Từ chối");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 19) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                SantaCity santaCity = (SantaCity) MapService.gI().getMapById(126);
                                                if (santaCity != null) {
                                                    if (!santaCity.isOpened() || santaCity.isClosed()) {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Hẹn gặp bạn lúc 22h mỗi ngày");
                                                        return;
                                                    }
                                                    santaCity.enter(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Có lỗi xảy ra!");
                                                }
                                                break;
                                        }
                                    }
                                }
                                if (this.mapId == 126) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                SantaCity santaCity = (SantaCity) MapService.gI().getMapById(126);
                                                if (santaCity != null) {
                                                    santaCity.leave(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Có lỗi xảy ra!");
                                                }
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case 77:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 0 || this.mapId == 7 || this.mapId == 14) {
                                    if (Manager.gI().demTimeSuKien2() != 0) {
                                        this.createOtherMenu(player, ConstNpc.MENU_DUA_TOP,
                                                "|2|Sự kiện đua TOP chào mừng khai mở máy chủ Ngọc Rồng Online\n"
                                                + "diễn ra từ " + Manager.timeStartDuaTop + " đến " + Manager.timeEndDuaTop + "\n"
                                                + "Giải thưởng khủng chưa từng có, xem chi tiết tại diễn đàn, fanpage\n"
                                                + Manager.demTimeSuKien(), "Top\nSức mạnh", "Top\nĐại gia", "Từ chối");
                                    } else {
                                        this.createOtherMenu(player, ConstNpc.MENU_DUA_TOP,
                                                "|2|Sự kiện đua TOP chào mừng khai mở máy chủ Ngọc Rồng Online\n"
                                                + "diễn ra từ " + Manager.timeStartDuaTop + " đến " + Manager.timeEndDuaTop + "\n"
                                                + "Giải thưởng khủng chưa từng có, xem chi tiết tại diễn đàn, fanpage\n"
                                                + Manager.demTimeSuKien(), "Top\nSức mạnh", "Top\nĐại gia", "Nhận thưởng\nSự kiện\nĐua Top", "Từ chối");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.getIndexMenu() == ConstNpc.MENU_DUA_TOP) {
                                    if (Manager.gI().demTimeSuKien2() > 0) {
                                        switch (select) {
                                            case 0:
                                                Service.getInstance().showTopPower(player);
                                                break;
                                            case 1:
                                                Service.getInstance().showTopRichMan(player);
                                                break;
                                        }
                                    } else {
                                        switch (select) {
                                            case 0:
                                                Service.getInstance().showTopPower(player);
                                                break;
                                            case 1:
                                                Service.getInstance().showTopRichMan(player);
                                                break;
                                            case 2: // xem điểm
                                                ShopService.gI().openBoxItemReward(player);
                                                break;
                                        }
                                    }

                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.MR_POPO:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 0) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Thượng đế vừa phát hiện 1 loại khí đang âm thầm\n"
                                            + "hủy diệt mọi mầm sống trên Trái Đất,\n"
                                            + "nó được gọi là Destron Gas.\n"
                                            + "Ta sẽ đưa các cậu đến nơi ấy, các cậu sẵn sàng chưa?",
                                            "Thông tin\nChi tiết", "Top 100\nBang hội",
                                            "Thành tích\nBang", "OK", "Từ chối");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 0) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:// Thông tin chi tiết
                                                NpcService.gI().createTutorial(player, avartar, "Chúng ta gặp rắc rối rồi\b"
                                                        + "Thượng Đế nói với tôi rằng có 1 loại khí\bgọi là Destron Gas, thứ này không thuộc về nơi đây\n"
                                                        + "Nó tích tụ trên Trái Đất\bvà nó sẽ hủy diệt mọi mô tế bào sống\b"
                                                        + "Có tất cả 4 địa điểm mà Thượng Đế bảo tôi nói với cậu\bCậu có thể đến kiểm tra...\n"
                                                        + "Đầu tiên là Thành phố Santa tọa lạc ở phía Tây nam của thủ đô ở Viễn Đông.\n"
                                                        + "Thứ hai là gần Kim Tự Tháp ở vùng Sa Mạc viễn tây của thủ đô phía Bắc\n"
                                                        + "Thứ ba Vùng Đất Băng Giá ở Phương Bắc xa xôi\n"
                                                        + "Thứ tư là Hành tinh Bóng Tối đang che phủ một phần địa cầu\bCậu đã hiểu rõ chưa?");
                                                break;
                                            case 1:// Top 100 bang hội

                                                break;
                                            case 2:// Thành tích Bang

                                                break;
                                            case 3: //OK
                                                if (player.clan != null) {
                                                    if (player.clan.isLeader(player)) {
                                                        if (player.clan.khiGas != null) {
                                                            this.createOtherMenu(player, ConstNpc.MENU_OPENED_KGHD,
                                                                    "Bang hội của cậu đang tham gia Destron Gas cấp độ 110\n"
                                                                    + "cậu có muốn đi cùng họ không? (" + Util.convertSecondsToTime2((System.currentTimeMillis() - player.clan.khiGas.lastTimeOpen) / 1000) + ")", "Đồng ý", "Từ chối");
                                                        } else {
                                                            Input.gI().createFormChooseLevelKhiGas(player);
                                                        }
                                                    } else {
                                                        Service.getInstance().sendThongBao(player, "Chức năng chỉ dành cho bang chủ");
                                                    }
                                                }
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_ACCEPT_GO_TO_KGHD) {
                                        switch (select) {
                                            case 0:
                                                KhiGasHuyDietService.gI().openKhiGas(player, Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPENED_KGHD) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().goToKhiGas(player);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };

                    break;
                case ConstNpc.LY_TIEU_NUONG_1:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                createOtherMenu(player, ConstNpc.BASE_MENU, "Mini game.", "Kéo\nBúa\nBao", "Con số\nmay mắn\nthỏi vàng", "Con số\nmay mắn\nngọc xanh", "Chọn ai đây", "Đóng");
                                return;
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            String time = ((ChonAiDay_Gold.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                            if (((ChonAiDay_Gold.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                ChonAiDay_Gold.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                            }
                            String time2 = ((ChonAiDay_Ruby.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                            if (((ChonAiDay_Ruby.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                ChonAiDay_Ruby.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                            }
                            String time3 = ((ChonAiDay_Gem.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                            if (((ChonAiDay_Gem.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                ChonAiDay_Gem.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                            }
                            if (canOpenNpc(player)) {
                                if (this.mapId == 5) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0: // kéo, búa, bao
                                                // Thêm logic cho trường hợp 0
                                                break;
                                            case 1: // Con số may mắn vàng
                                                xửLýLựaChọnMiniGame_Gold(player);
                                                break;
                                            case 2:
                                                xửLýLựaChọnMiniGame(player);
                                                break;
                                            case 3: // chọn ai đây
                                                createOtherMenu(player, ConstNpc.CHON_AI_DAY, "Trò chơi Chọn Ai Đây đang được diễn ra, nếu bạn tin tưởng mình đang tràn đầy "
                                                        + "may mắn thì có thể tham gia thử", "Thể lệ", "Chọn\nVàng", "Chọn\nhồng ngọc", "Chọn\nngọc xanh");
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CON_SO_MAY_MAN_NGOC_XANH) {
                                        xửLýConSoMayManNgocXanh(player, select);
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CON_SO_MAY_MAN_VANG) {
                                        xửLýConSoMayManVang(player, select);
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CHON_AI_DAY) {
                                        xửLýChonAiDay(player, select, time);
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CHON_AI_DAY_VANG) {
                                        xửLýChonAiDayVang(player, select, time);
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CHON_AI_DAY_HONG_NGOC) {
                                        xửLýChonAiDayRuby(player, select, time2);
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CHON_AI_DAY_NGOC) {
                                        xửLýChonAiDayGem(player, select, time3);
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.UPDATE_CHON_AI_DAY_NGOC) {
                                        switch (select) {
                                            case 0:
                                                createOtherMenu(player, ConstNpc.UPDATE_CHON_AI_DAY_NGOC, "Thời gian từ 8h đến hết 21h59 hằng ngày\n"
                                                        + "Mỗi lượt được chọn 10 con số từ 0 đến 99\n"
                                                        + "Thời gian mỗi lượt là 5 phút", "Cập nhật", "Đóng");
                                                break;
                                        }
                                    }
                                }
                            }
                        }

                        // Thêm các phương thức mới để xử lý logic cho mỗi trường hợp
                        private void xửLýLựaChọnMiniGame(Player player) {
                            LocalTime thoi_gian_hien_tai = LocalTime.now();
                            int gio = thoi_gian_hien_tai.getHour();
                            int phut = thoi_gian_hien_tai.getMinute();
                            String plWin = MiniGame.gI().MiniGame_S1.result_name;
                            String KQ = MiniGame.gI().MiniGame_S1.result + "";
                            String Money = MiniGame.gI().MiniGame_S1.money + "";
                            String count = MiniGame.gI().MiniGame_S1.players.size() + "";
                            String second = MiniGame.gI().MiniGame_S1.second + "";
                            String number = MiniGame.gI().MiniGame_S1.strNumber((int) player.id);
                            StringBuilder previousResults = new StringBuilder("");
                            if (MiniGame.gI().MiniGame_S1.dataKQ_CSMM != null && !MiniGame.gI().MiniGame_S1.dataKQ_CSMM.isEmpty()) {
                                int maxResultsToShow = Math.min(10, MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size());
                                for (int i = MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size() - maxResultsToShow; i < MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size(); i++) {
                                    previousResults.append(MiniGame.gI().MiniGame_S1.dataKQ_CSMM.get(i));
                                    if (i < MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size() - 1) {
                                        previousResults.append(",");
                                    }
                                }
                            }

                            String npcSay = ""
                                    + "Kết quả giải trước: " + KQ + "\n"
                                    + (previousResults.toString() != "" ? previousResults.toString() + "\n" : "")
                                    + "Tổng giải thưởng: " + Money + " ngọc\n"
                                    + "<" + second + ">giây\n"
                                    + (number != "" ? "Các số bạn chọn: " + number : "");
                            String[] Menus = {
                                "Cập nhật",
                                "1 Số\n5 ngọc xanh",
                                "Ngẫu nhiên\n1 số lẻ\n5 ngọc xanh",
                                "Ngẫu nhiên\n1 số chẵn\n5 ngọc xanh",
                                "Hướng\ndẫn\nthêm",
                                "Đóng"
                            };
                            createOtherMenu(player, ConstNpc.CON_SO_MAY_MAN_NGOC_XANH, npcSay, Menus);
                            return;
                        }

                        // Thêm các phương thức mới để xử lý logic cho mỗi trường hợp
                        private void xửLýLựaChọnMiniGame_Gold(Player player) {
                            LocalTime thoi_gian_hien_tai = LocalTime.now();
                            int gio = thoi_gian_hien_tai.getHour();
                            int phut = thoi_gian_hien_tai.getMinute();
                            String plWin = MiniGame.gI().MiniGame_S1.result_name;
                            String KQ = MiniGame.gI().MiniGame_S1.result + "";
                            String Money = Util.mumberToLouis(MiniGame.gI().MiniGame_S1.gold) + "";
                            String count = MiniGame.gI().MiniGame_S1.players.size() + "";
                            String second = MiniGame.gI().MiniGame_S1.second + "";
                            String number = MiniGame.gI().MiniGame_S1.strNumber((int) player.id);
                            StringBuilder previousResults = new StringBuilder("");
                            if (MiniGame.gI().MiniGame_S1.dataKQ_CSMM != null && !MiniGame.gI().MiniGame_S1.dataKQ_CSMM.isEmpty()) {
                                int maxResultsToShow = Math.min(10, MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size());
                                for (int i = MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size() - maxResultsToShow; i < MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size(); i++) {
                                    previousResults.append(MiniGame.gI().MiniGame_S1.dataKQ_CSMM.get(i));
                                    if (i < MiniGame.gI().MiniGame_S1.dataKQ_CSMM.size() - 1) {
                                        previousResults.append(",");
                                    }
                                }
                            }

                            String npcSay = ""
                                    + "Kết quả giải trước: " + KQ + "\n"
                                    + (previousResults.toString() != "" ? previousResults.toString() + "\n" : "")
                                    + "Tổng giải thưởng: " + Money + " thỏi vàng\n"
                                    + "<" + second + ">giây\n"
                                    + (number != "" ? "Các số bạn chọn: " + number : "");
                            String[] Menus = {
                                "Cập nhật",
                                "1 Số\n 1 thỏi vàng",
                                "Ngẫu nhiên\n1 số lẻ\n 1 thỏi vàng",
                                "Ngẫu nhiên\n1 số chẵn\n 1 thỏi vàng",
                                "Hướng\ndẫn\nthêm",
                                "Đóng"
                            };
                            createOtherMenu(player, ConstNpc.CON_SO_MAY_MAN_VANG, npcSay, Menus);
                            return;
                        }

                        private void xửLýConSoMayManNgocXanh(Player player, int select) {
                            switch (select) {
                                case 0: // cập nhật
                                    xửLýLựaChọnMiniGame(player);
                                    break;
                                case 1: // chọn 1 số
                                    Input.gI().createFormConSoMayMan_Gem(player);
                                    break;
                                case 2: // chọn 1 số lẻ
                                    MiniGame.gI().MiniGame_S1.ramdom1SoLe(player, 1);
                                    break;
                                case 3: // chọn 1 số chẵn
                                    MiniGame.gI().MiniGame_S1.ramdom1SoChan(player, 1);
                                    break;
                                case 4:
                                    createOtherMenu(player, 1, "Thời gian từ 8h đến hết 21h59 hằng ngày\n"
                                            + "Mỗi lượt được chọn 10 con số từ 0 đến 99\n"
                                            + "Thời gian mỗi lượt là 5 phút.", "Đồng ý");
                                    break;
                            }
                        }

                        private void xửLýConSoMayManVang(Player player, int select) {
                            switch (select) {
                                case 0: // cập nhật
                                    xửLýLựaChọnMiniGame_Gold(player);
                                    break;
                                case 1: // chọn 1 số
                                    Input.gI().createFormConSoMayMan_Gold(player);
                                    break;
                                case 2: // chọn 1 số lẻ
                                    MiniGame.gI().MiniGame_S1.ramdom1SoLe(player, 0);
                                    break;
                                case 3: // chọn 1 số chẵn
                                    MiniGame.gI().MiniGame_S1.ramdom1SoChan(player, 0);
                                    break;
                                case 4:
                                    createOtherMenu(player, 1, "Thời gian từ 8h đến hết 21h59 hằng ngày\n"
                                            + "Mỗi lượt được chọn 10 con số từ 0 đến 99\n"
                                            + "Thời gian mỗi lượt là 5 phút.", "Đồng ý");
                                    break;
                            }
                        }

                        private void xửLýChonAiDay(Player player, int select, String time) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.IGNORE_MENU, "Mỗi lượt chơi có 6 giải thưởng\n"
                                            + "Được chọn tối đa 10 lần mỗi giải\n"
                                            + "Thời gian 1 lượt chọn là 5 phút\n"
                                            + "Khi hết giờ, hệ thống sẽ ngẫu nhiên chọn ra 1 người may mắn\n"
                                            + "của từng giải và trao thưởng.\n"
                                            + "Lưu ý: Nếu tham gia bằng Ngọc Xanh hoặc Hồng ngọc thì người thắng sẽ nhận thưởng là hồng ngọc.", "OK");
                                    break;
                                case 1:
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_VANG, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldNormar) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(0) + "%\n"
                                            + "Tổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldVip) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(1) + "%\n"
                                            + "Thời gian còn lại: " + time, "Cập nhập", "Thường\n1 triệu\nvàng", "VIP\n10 triệu\nvàng", "Đóng");
                                    break;
                                case 2:
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_HONG_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 hồng\nngọc", "VIP\n100 hồng\nngọc", "Đóng");
                                    break;
                                case 3:
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 ngọc\nxanh", "VIP\n100 ngọc\nxanh", "Đóng");
                                    break;
                            }
                        }

                        private void xửLýChonAiDayVang(Player player, int select, String time) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_VANG, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldNormar) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldVip) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n1 triệu\nvàng", "VIP\n10 triệu\nvàng", "Đóng");
                                    break;
                                case 1:
                                    xửLýThuong1TrieuVang(player);
                                    break;
                                case 2:
                                    xửLýVIP10TrieuVang(player);
                                    break;
                            }
                        }

                        private void xửLýChonAiDayRuby(Player player, int select, String time) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_HONG_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 hồng\nngọc", "VIP\n100 hồng\nngọc", "Đóng");
                                    break;
                                case 1:
                                    xửLýThuong10HongNgoc(player);
                                    break;
                                case 2:
                                    xửLýVIP100HongNgoc(player);
                                    break;
                            }
                        }

                        private void xửLýChonAiDayGem(Player player, int select, String time) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 ngọc\nxanh", "VIP\n100 ngọc\nxanh", "Đóng");
                                    break;
                                case 1:
                                    xửLýThuong10NgocXanh(player);
                                    break;
                                case 2:
                                    xửLýVIP100NgocXanh(player);
                                    break;
                            }
                        }

                        // Thêm các phương thức mới để xử lý logic cho mỗi trường hợp
                        private void xửLýThuong1TrieuVang(Player player) {
                            try {
                                String time = ((ChonAiDay_Gold.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                                if (((ChonAiDay_Gold.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                    ChonAiDay_Gold.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                                }
                                if (player.inventory.gold >= 1_000_000) {
                                    player.inventory.subGold(1_000_000); // FIX: dùng method an toàn
                                    Service.gI().sendMoney(player);
                                    player.goldNormar += 1_000_000;
                                    ChonAiDay_Gold.gI().goldNormar += 1_000_000;
                                    ChonAiDay_Gold.gI().addPlayerNormar(player);
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_VANG, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldNormar) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldVip) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n1 triệu\nvàng", "VIP\n10 triệu\nvàng", "Đóng");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ vàng");
                                }
                            } catch (Exception ex) {
                                System.out.println("Lỗi CHON_AI_DAY_VANG");
                            }
                        }

                        private void xửLýVIP10TrieuVang(Player player) {
                            try {
                                String time = ((ChonAiDay_Gold.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                                if (((ChonAiDay_Gold.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                    ChonAiDay_Gold.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                                }
                                if (player.inventory.gold >= 10_000_000) {
                                    player.inventory.subGold(10_000_000); // FIX: dùng method an toàn
                                    Service.gI().sendMoney(player);
                                    player.goldVIP += 10_000_000;
                                    ChonAiDay_Gold.gI().goldVip += 10_000_000;
                                    ChonAiDay_Gold.gI().addPlayerVIP(player);
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_VANG, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldNormar) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gold.gI().goldVip) + " vàng, cơ hội trúng của bạn là: " + player.percentGold(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n1 triệu\nvàng", "VIP\n10 triệu\nvàng", "Đóng");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ vàng");
                                }
                            } catch (Exception ex) {
                                System.out.println("Lỗi CHON_AI_DAY_VANG VIP");
                            }
                        }

                        // Thêm các phương thức mới để xử lý logic cho mỗi trường hợp
                        private void xửLýThuong10HongNgoc(Player player) {
                            try {
                                String time = ((ChonAiDay_Ruby.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                                if (((ChonAiDay_Ruby.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                    ChonAiDay_Ruby.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                                }
                                if (player.inventory.ruby >= 10) {
                                    player.inventory.subRuby(10); // FIX
                                    Service.gI().sendMoney(player);
                                    player.rubyNormar += 10;
                                    ChonAiDay_Ruby.gI().rubyNormar += 10;
                                    ChonAiDay_Ruby.gI().addPlayerNormar(player);
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_HONG_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 hồng\nngọc", "VIP\n100 hồng\nngọc", "Đóng");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ hồng ngọc");
                                }
                            } catch (Exception ex) {
                                System.out.println("Lỗi CHON_AI_DAY_HONG_NGOC");
                            }
                        }

                        private void xửLýVIP100HongNgoc(Player player) {
                            try {
                                String time = ((ChonAiDay_Ruby.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                                if (((ChonAiDay_Ruby.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                    ChonAiDay_Ruby.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                                }
                                if (player.inventory.ruby >= 100) {
                                    player.inventory.subRuby(100); // FIX
                                    Service.gI().sendMoney(player);
                                    player.rubyVIP += 100;
                                    ChonAiDay_Ruby.gI().rubyVip += 100;
                                    ChonAiDay_Ruby.gI().addPlayerVIP(player);
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_HONG_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Ruby.gI().rubyVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentRuby(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 hồng\nngọc", "VIP\n100 hồng\nngọc", "Đóng");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ hồng ngọc");
                                }
                            } catch (Exception ex) {
                                System.out.println("Lỗi CHON_AI_DAY_HONG_NGOC VIP");
                            }
                        }

                        // Thêm các phương thức mới để xử lý logic cho mỗi trường hợp
                        private void xửLýThuong10NgocXanh(Player player) {
                            try {
                                String time = ((ChonAiDay_Gem.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                                if (((ChonAiDay_Gem.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                    ChonAiDay_Gem.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                                }
                                if (player.inventory.gem >= 10) {
                                    player.inventory.subGem(10); // FIX
                                    Service.gI().sendMoney(player);
                                    player.gemNormar += 10;
                                    ChonAiDay_Gem.gI().gemNormar += 10;
                                    ChonAiDay_Gem.gI().addPlayerNormar(player);
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 ngọc\nxanh", "VIP\n100 ngọc\nxanh", "Đóng");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ ngọc xanh");
                                }
                            } catch (Exception ex) {
                                System.out.println("Lỗi CHON_AI_DAY_NGOC_XANH");
                            }
                        }

                        private void xửLýVIP100NgocXanh(Player player) {
                            try {
                                String time = ((ChonAiDay_Gem.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                                if (((ChonAiDay_Gem.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) < 0) {
                                    ChonAiDay_Gem.gI().lastTimeEnd = System.currentTimeMillis() + 300000;
                                }
                                if (player.inventory.gem >= 100) {
                                    player.inventory.subGem(100); // FIX
                                    Service.gI().sendMoney(player);
                                    player.gemVIP += 100;
                                    ChonAiDay_Gem.gI().gemVip += 100;
                                    ChonAiDay_Gem.gI().addPlayerVIP(player);
                                    createOtherMenu(player, ConstNpc.CHON_AI_DAY_NGOC, "Tổng giải thường: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemNormar) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(0) + "%\nTổng giải VIP: " + Util.numberToMoney(ChonAiDay_Gem.gI().gemVip) + " hồng ngọc, cơ hội trúng của bạn là: " + player.percentGem(1) + "%\n Thời gian còn lại: " + time, "Cập nhập", "Thường\n10 ngọc\nxanh", "VIP\n100 ngọc\nxanh", "Đóng");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ ngọc xanh");
                                }
                            } catch (Exception ex) {
                                System.out.println("Lỗi CHON_AI_DAY_NGOC_XANH VIP");
                            }
                        }
                    };
                    break;

                case ConstNpc.QUY_LAO_KAME:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Con muốn hỏi gì nào?", "Nói\nchuyện", "Hồi Skill\n100 Tr vàng");
                                }
                            }
                            return;
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0: // Nói chuyện
                                            if (player.clan != null) {
                                                if (player.clan.isLeader(player)) {
                                                    this.createOtherMenu(player, ConstNpc.MENU_NOI_CHUYEN,
                                                            "Chào con, ta rất vui khi gặp con\n"
                                                            + "Con muốn làm gì nào ?\n",
                                                            "Nhiệm vụ\n", "Học\nKỹ năng\n", "Về khu\nvực bang\n", "Giải tán\nBang hội",
                                                            "Kho báu\ndưới biển");
                                                } else {
                                                    this.createOtherMenu(player, ConstNpc.MENU_NOI_CHUYEN,
                                                            "Chào con, ta rất vui khi gặp con\n"
                                                            + "Con muốn làm gì nào ?\n",
                                                            "Nhiệm vụ\n", "Học\nKỹ năng\n", "Về khu\nvực bang\n",
                                                            "Kho báu\ndưới biển");
                                                }
                                            } else {
                                                this.createOtherMenu(player, ConstNpc.MENU_NOI_CHUYEN,
                                                        "Chào con, ta rất vui khi gặp con\n"
                                                        + "Con muốn làm gì nào ?\n",
                                                        "Nhiệm vụ\n", "Học\nKỹ năng\n", "Kho báu\ndưới biển");
                                            }
                                            break;
                                        case 1:
                                            if (player.inventory.gold >= 100_000_000_0) {
                                                Skill skill;
                                                for (int i = 0; i < player.playerSkill.skills.size(); i++) {
                                                    skill = player.playerSkill.skills.get(i);
                                                    skill.lastTimeUseThisSkill = System.currentTimeMillis() - (long) skill.coolDown;
                                                    player.nPoint.setFullHpMp();
                                                    PlayerService.gI().sendInfoHpMp(player);

                                                }
                                                Service.getInstance().sendTimeSkill(player);
                                                player.inventory.subGold(100_000_000_0); // FIX: dùng method an toàn
                                                Service.getInstance().sendMoney(player);
                                                Service.getInstance().sendThongBao(player, "Hồi skill Thành Công");
                                            } else {
                                                Service.getInstance().sendThongBao(player, "Bạn không đủ vàng");
                                                return;
                                            }
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_NOI_CHUYEN) {
                                    if (player.clan != null) {
                                        if (player.clan.isLeader(player)) {
                                            switch (select) {
                                                case 0:// nhiệm vụ
                                                    NpcService.gI().createTutorial(player, avartar, "Nhiệm vụ hiện tại của con: " + player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                                                    break;
                                                case 1:
                                                    this.openShopLearnSkill(player, ConstNpc.SHOP_LEARN_SKILL, 0);
                                                    break;
                                                case 2:
                                                    if (player.clan == null) {
                                                        Service.getInstance().sendThongBao(player, "Chưa có bang hội");
                                                        return;
                                                    }
                                                    ChangeMapService.gI().changeMap(player, player.clan.getClanArea(), 910, 190);
                                                    break;
                                                case 3: // Giải tán bang hội
                                                    if (player.clan.isLeader(player)) {
                                                        this.createOtherMenu(player, ConstNpc.ACP_GIAI_TAN_BANG,
                                                                "Con có chắc chắn giải tán bang hội không?",
                                                                "Đồng ý", "Từ chối");
                                                    }
                                                    break;
                                                case 4:
                                                    if (player.clan != null) {
                                                        if (player.clan.banDoKhoBau != null) {
                                                            this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                                                    "Bang hội của con đang đi tìm kho báu dưới biển cấp độ "
                                                                    + player.clan.banDoKhoBau.level
                                                                    + "\nCon có muốn đi theo không?",
                                                                    "Đồng ý", "Từ chối");
                                                        } else {
                                                            this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                                                    "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\n"
                                                                    + "Ở đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                                                    "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
                                                        }
                                                    } else {
                                                        this.npcChat(player, "Con phải có bang hội ta mới có thể cho con đi");
                                                    }
                                                    break;
                                            }
                                        } else {
                                            switch (select) {
                                                case 0:// nhiệm vụ
                                                    NpcService.gI().createTutorial(player, avartar, "Nhiệm vụ hiện tại của con: " + player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                                                    break;
                                                case 1:
                                                    this.openShopLearnSkill(player, ConstNpc.SHOP_LEARN_SKILL, 0);
                                                    break;
                                                case 2:
                                                    if (player.clan == null) {
                                                        Service.getInstance().sendThongBao(player, "Chưa có bang hội");
                                                        return;
                                                    }
                                                    ChangeMapService.gI().changeMap(player, player.clan.getClanArea(), 910, 190);
                                                    break;
                                                case 3:
                                                    if (player.clan != null) {
                                                        if (player.clan.banDoKhoBau != null) {
                                                            this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                                                    "Bang hội của con đang đi tìm kho báu dưới biển cấp độ "
                                                                    + player.clan.banDoKhoBau.level
                                                                    + "\nCon có muốn đi theo không?",
                                                                    "Đồng ý", "Từ chối");
                                                        } else {
                                                            this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                                                    "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\n"
                                                                    + "Ở đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                                                    "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
                                                        }
                                                    } else {
                                                        this.npcChat(player, "Con phải có bang hội ta mới có thể cho con đi");
                                                    }
                                                    break;
                                            }
                                        }
                                    } else {
                                        switch (select) {
                                            case 0:// nhiệm vụ
                                                NpcService.gI().createTutorial(player, avartar, player.playerTask.taskMain.name);
                                                break;
                                            case 1:
                                                this.openShopLearnSkill(player, ConstNpc.SHOP_LEARN_SKILL, 0);
                                                break;
                                            case 2:
                                                if (player.clan != null) {
                                                    if (player.clan.banDoKhoBau != null) {
                                                        this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                                                "Bang hội của con đang đi tìm kho báu dưới biển cấp độ "
                                                                + player.clan.banDoKhoBau.level
                                                                + "\nCon có muốn đi theo không?",
                                                                "Đồng ý", "Từ chối");
                                                    } else {
                                                        this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                                                "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\n"
                                                                + "Ở đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                                                "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
                                                    }
                                                } else {
                                                    this.npcChat(player, "Con phải có bang hội ta mới có thể cho con đi");
                                                }
                                                break;
                                        }
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.ACP_GIAI_TAN_BANG) {
                                    switch (select) {
                                        case 0:
                                            Input.gI().createFormGiaiTanBang(player);
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPEN_SUKIEN) {
                                    openMenuSuKien(player, this, tempId, select);
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPENED_DBKB) {
                                    switch (select) {
                                        case 0:
                                            ChangeMapService.gI().goToDBKB(player);
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPEN_DBKB) {
                                    switch (select) {
                                        case 0:// Top bang hội
                                            Service.gI().showTopClanBDKB(player);
                                            break;
                                        case 1:// Thách tích bang
                                            Service.getInstance().showMyTopClanBDKB(player);
                                            break;
                                        case 2:
                                            if (player.isAdmin()
                                                    || player.nPoint.power >= BanDoKhoBau.POWER_CAN_GO_TO_DBKB) {
                                                Input.gI().createFormChooseLevelBDKB(player);
                                            } else {
                                                this.npcChat(player, "Sức mạnh của con phải ít nhất phải đạt "
                                                        + Util.numberToMoney(BanDoKhoBau.POWER_CAN_GO_TO_DBKB));
                                            }
                                            break;
                                    }

                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_ACCEPT_GO_TO_BDKB) {
                                    switch (select) {
                                        case 0:
                                            BanDoKhoBauService.gI().openBanDoKhoBau(player, Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
                                            break;
                                    }

                                } else if (player.iDMark.getIndexMenu() == ConstNpc.ESCORT_QILIN_MENU) {
                                    switch (select) {
                                        case 0: {
                                            if (InventoryService.gI().getCountEmptyBag(player) == 0) {
                                                this.npcChat(player,
                                                        "Con phải có ít nhất 1 ô trống trong hành trang ta mới đưa cho con được");
                                                return;
                                            }
                                            EscortedBoss escortedBoss = player.getEscortedBoss();
                                            if (escortedBoss != null) {
                                                escortedBoss.stopEscorting();
                                                Item item = ItemService.gI()
                                                        .createNewItem((short) ConstItem.CAPSULE_TET_2022);
                                                item.quantity = 1;
                                                InventoryService.gI().addItemBag(player, item, 0);
                                                InventoryService.gI().sendItemBags(player);
                                                Service.getInstance().sendThongBao(player,
                                                        "Bạn nhận được " + item.template.name);
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.TRUONG_LAO_GURU:
                case ConstNpc.VUA_VEGETA:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                EscortedBoss escortedBoss = player.getEscortedBoss();
                                if (escortedBoss != null && escortedBoss instanceof Qilin) {
                                    this.createOtherMenu(player, ConstNpc.ESCORT_QILIN_MENU,
                                            "Ah con đã tìm thấy lân con thất lạc của ta\nTa sẽ thưởng cho con 1 viên Capsule Tết 2023.",
                                            "Đồng ý", "Từ chối");
                                } else {
                                    if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                        super.openBaseMenu(player);
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.getIndexMenu() == ConstNpc.ESCORT_QILIN_MENU) {
                                    switch (select) {
                                        case 0: {
                                            if (InventoryService.gI().getCountEmptyBag(player) == 0) {
                                                this.npcChat(player,
                                                        "Con phải có ít nhất 1 ô trống trong hành trang ta mới đưa cho con được");
                                                return;
                                            }
                                            EscortedBoss escortedBoss = player.getEscortedBoss();
                                            if (escortedBoss != null) {
                                                escortedBoss.stopEscorting();
                                                Item item = ItemService.gI()
                                                        .createNewItem((short) ConstItem.CAPSULE_TET_2022);
                                                item.quantity = 1;
                                                InventoryService.gI().addItemBag(player, item, 0);
                                                InventoryService.gI().sendItemBags(player);
                                                Service.getInstance().sendThongBao(player,
                                                        "Bạn nhận được " + item.template.name);
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.ONG_GOHAN:
                case ConstNpc.ONG_MOORI:
                case ConstNpc.ONG_PARAGUS:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                    if (player.thanhVien) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                "Con cần ta giúp gì nào?", "Mã Quà Tặng", "Nạp tiền", "Hỗ trợ\nnhiệm vụ", "Điểm Danh", "Đổi Mật\nKhẩu");
                                    } else {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                "Con cần ta giúp gì nào?", "Mã Quà Tặng", "Nạp tiền", "Mở\nThành viên", "Hỗ trợ\nnhiệm vụ", "Đổi Mật\nKhẩu", "Đóng");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:
                                            Input.gI().createFormGiftCode(player);
                                            break;
                                        case 1:
                                            this.createOtherMenu(player, ConstNpc.MENU_NAP_TIEN,
                                                    "Số dư của con là: " + Util.mumberToLouis(player.soDuVND) + " VND dùng để nạp qua đơn vị khác\n"
                                                    + "Ta đang giữ giúp con " + Util.mumberToLouis(player.soThoiVang) + " thỏi vàng",
                                                    "Nạp vàng", "Nhận\nThỏi vàng", "Nhận\nNgọc Xanh\n(Miễn phí)", "Đóng");
                                            return;
                                        case 2:
                                            if (!player.thanhVien) {
                                                this.createOtherMenu(player, ConstNpc.MENU_MO_THANH_VIEN,
                                                        "Mở thành viên con sẽ được sử dụng các chức năng\n"
                                                        + "Giao dịch, con số may mắn, kênh thế giới, cửa hàng kí gửi",
                                                        "Mở\nThành viên\n10.000 VND", "Từ chối");
                                                return;
                                            } else {
                                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_9_0 || TaskService.gI().getIdTask(player) == ConstTask.TASK_9_1 || TaskService.gI().getIdTask(player) == ConstTask.TASK_9_2) {
                                                    player.playerTask.taskMain.id = 10;
                                                    player.playerTask.taskMain.index = 3;
                                                    TaskService.gI().sendTaskMain(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Tàu 77.");
                                                }
                                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_18_0 || TaskService.gI().getIdTask(player) == ConstTask.TASK_18_1) {
                                                    player.playerTask.taskMain.id = 18;
                                                    player.playerTask.taskMain.index = 2;
                                                    TaskService.gI().sendTaskMain(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ DHVT.");
                                                }
                                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_19_0 || TaskService.gI().getIdTask(player) == ConstTask.TASK_19_1) {
                                                    player.playerTask.taskMain.id = 19;
                                                    player.playerTask.taskMain.index = 2;
                                                    TaskService.gI().sendTaskMain(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Trung Úy Trắng.");
                                                }
                                            }
                                            break;
                                        case 3:
                                            if (!player.thanhVien) {
                                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_9_0 || TaskService.gI().getIdTask(player) == ConstTask.TASK_9_1 || TaskService.gI().getIdTask(player) == ConstTask.TASK_9_2) {
                                                    player.playerTask.taskMain.id = 10;
                                                    player.playerTask.taskMain.index = 3;
                                                    TaskService.gI().sendTaskMain(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Tàu 77.");
                                                }
                                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_18_0 || TaskService.gI().getIdTask(player) == ConstTask.TASK_18_1) {
                                                    player.playerTask.taskMain.id = 18;
                                                    player.playerTask.taskMain.index = 2;
                                                    TaskService.gI().sendTaskMain(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ DHVT.");
                                                }
                                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_19_0 || TaskService.gI().getIdTask(player) == ConstTask.TASK_19_1) {
                                                    player.playerTask.taskMain.id = 19;
                                                    player.playerTask.taskMain.index = 2;
                                                    TaskService.gI().sendTaskMain(player);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Trung Úy Trắng.");
                                                }
                                            }
                                            break;
                                        case 4:
                                            Input.gI().createFormChangePassword(player);
                                            break;
                                        /* case 5:
                                                  Service.gI().sendThongBaoFromAdmin(player,
                                                  "|7|Chúc Mừng Bạn Đã Nhận Được Đệ Tử!");
                                                  if (player.pet == null) {
                                                  PetService.gI().createNormalPet(player);
                                                  Service.getInstance().sendThongBao(player,
                                                  "Con vừa nhận được đệ tử! Hãy chăm sóc nó nhé");
                                                  } else {
                                                  this.npcChat(player, "Đã có đệ tử rồi mà!");

                                                  }
                                             break; */
                                    }
                                }
                                if (player.iDMark.getIndexMenu() == ConstNpc.MENU_NAP_TIEN) {
                                    switch (select) {
                                        case 0: // Nạp vàng
                                            this.createOtherMenu(player, ConstNpc.MENU_DOI_VANG,
                                                    "Ta sẽ tạm giữ giúp con\n"
                                                    + "Nếu con cần dùng tới hãy quay lại đây gặp ta!",
                                                    "10.000\n20 Thỏi\nvàng", "20.000\n40 Thỏi\nvàng",
                                                    "30.000\n72 Thỏi\nvàng", "50.000\n120 Thỏi\nvàng",
                                                    "100.000\n280 Thỏi\nvàng", "200.000\n720 Thỏi\nvàng",
                                                    "500.000\n2.000 Thỏi\nvàng", "1.000.000\n4.400 Thỏi\nvàng");
                                            return;
                                        case 1: // Nhận thỏi vàng
                                            Input.gI().createFormNhanThoiVang(player);
                                            break;
                                        case 2:
                                            if (player.inventory.gem >= 1000000) {
                                                Service.getInstance().sendThongBao(player, "Tiêu bớt ngọc xanh đi bạn ơi");
                                                return;
                                            } else {
                                                player.inventory.addGem(100000); // FIX
                                                Service.getInstance().sendMoney(player);
                                            }
                                            break;
                                    }
                                }
                                if (player.iDMark.getIndexMenu() == ConstNpc.MENU_MO_THANH_VIEN) {
                                    if (select == 0) {
                                        if (player.soDuVND >= 10000) {
                                            Item thoivang = ItemService.gI().createNewItem((short) 457, 20);
                                            thoivang.itemOptions.add(new ItemOption(83, 0));
                                            thoivang.itemOptions.add(new ItemOption(100, 0));
                                            player.thanhVien = true;
                                            player.soDuVND -= 10000;
                                            PlayerDAO.subVndBar(player, 10000);
                                            PlayerDAO.moThanhVien(player);
                                            InventoryService.gI().addItemBag(player, thoivang, 99999);
                                            InventoryService.gI().sendItemBags(player);
                                            Service.getInstance().sendThongBao(player, "Bạn nhận được " + thoivang.getName());
                                        } else {
                                            Service.gI().sendThongBao(player, "Bạn không đủ số dư để mở thành viên");
                                        }
                                    }
                                }

                                if (player.iDMark.getIndexMenu() == ConstNpc.MENU_DOI_VANG) {
                                    switch (select) {
                                        case 0:
                                            processThoiVangPurchase(player, 10_000, 20);
                                            break;
                                        case 1:
                                            processThoiVangPurchase(player, 20_000, 40);
                                            break;
                                        case 2:
                                            processThoiVangPurchase(player, 30_000, 72);
                                            break;
                                        case 3:
                                            processThoiVangPurchase(player, 50_000, 120);
                                            break;
                                        case 4:
                                            processThoiVangPurchase(player, 100_000, 280);
                                            break;
                                        case 5:
                                            processThoiVangPurchase(player, 200_000, 720);
                                            break;
                                        case 6:
                                            processThoiVangPurchase(player, 500_000, 2_000);
                                            break;
                                        case 7:
                                            processThoiVangPurchase(player, 1_000_000, 4_400);
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.BUNMA:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                    if (player.gender == ConstPlayer.TRAI_DAT) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                "Cậu cần trang bị gì cứ đến chỗ tôi nhé", "Cửa\nhàng");
                                    } else {
                                        NpcService.gI().createTutorial(player, this.avartar, "Xin lỗi cưng, chị chỉ bán đồ cho người Trái Đất");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:// Shop
                                            this.openShopWithGender(player, ConstNpc.SHOP_BUNMA_QK_0, 0);
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.DENDE:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                    if (player.isHoldNamecBall) {
                                        this.createOtherMenu(player, ConstNpc.ORTHER_MENU,
                                                "Ô,ngọc rồng Namek,anh thật may mắn,nếu tìm đủ 7 viên ngọc có thể triệu hồi Rồng Thần Namek,",
                                                "Gọi rồng", "Từ chối");
                                    } else {
                                        if (player.gender == ConstPlayer.NAMEC) {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                    "Anh cần trang bị gì cứ đến chỗ em nhé", "Cửa\nhàng");
                                        } else {
                                            NpcService.gI().createTutorial(player, this.avartar, "Xin lỗi anh, em chỉ bán đồ cho dân tộc Namếc");
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:// Shop
                                            this.openShopWithGender(player, ConstNpc.SHOP_DENDE_0, 0);
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.ORTHER_MENU) {
                                    NamekBallWar.gI().summonDragon(player, this);
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.APPULE:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                    if (player.gender == ConstPlayer.XAYDA) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                "Ngươi cần trang bị gì cứ đến chỗ ta nhé", "Cửa\nhàng");
                                    } else {
                                        NpcService.gI().createTutorial(player, this.avartar, "Về hành tinh hạ đẳng của ngươi mà mua đồ cùi nhé. Tại đây ta chỉ bán đồ cho người Xayda thôi");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:// Shop

                                            this.openShopWithGender(player, ConstNpc.SHOP_APPULE_0, 0);

                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.DR_DRIEF:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player pl) {
                            if (canOpenNpc(pl)) {
                                if (this.mapId == 84) {
                                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                            "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                                            pl.gender == ConstPlayer.TRAI_DAT ? "Đến\nTrái Đất"
                                                    : pl.gender == ConstPlayer.NAMEC ? "Đến\nNamếc" : "Đến\nXayda");
                                } else if (this.mapId == 153) {
                                    Clan clan = pl.clan;
                                    ClanMember cm = pl.clanMember;
                                    if (cm.role == Clan.LEADER) {
                                        this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                                "Cần 1000 capsule bang [đang có " + clan.clanPoint
                                                + " capsule bang] để nâng cấp bang hội lên cấp "
                                                + (clan.level++) + "\n"
                                                + "+1 tối đa số lượng thành viên",
                                                "Về\nĐảoKame", "Góp " + cm.memberPoint + " capsule", "Nâng cấp",
                                                "Từ chối");
                                    } else {
                                        this.createOtherMenu(pl, ConstNpc.BASE_MENU, "Bạn đang có " + cm.memberPoint
                                                + " capsule bang,bạn có muốn đóng góp toàn bộ cho bang hội của mình không ?",
                                                "Về\nĐảoKame", "Đồng ý", "Từ chối");
                                    }
                                } else if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                                    if (pl.playerTask.taskMain.id == 7) {
                                        NpcService.gI().createTutorial(pl, this.avartar,
                                                "Hãy lên đường cứu đứa bé nhà tôi\n"
                                                + "Chắc bây giờ nó đang sợ hãi lắm rồi");
                                    } else {
                                        this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                                "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                                                "Đến\nNamếc", "Đến\nXayda", "Siêu thị");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 84) {
                                    ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 24, -1, -1);
                                } else if (mapId == 153) {
                                    if (select == 0) {
                                        ChangeMapService.gI().changeMap(player, ConstMap.DAO_KAME, -1, 1059, 408);
                                        return;
                                    }
                                    Clan clan = player.clan;
                                    ClanMember cm = player.clanMember;
                                    if (select == 1) {
                                        player.clan.clanPoint += cm.memberPoint;
                                        cm.clanPoint += cm.memberPoint;
                                        cm.memberPoint = 0;
                                        Service.getInstance().sendThongBao(player, "Đóng góp thành công");
                                    } else if (select == 2 && cm.role == Clan.LEADER) {
                                        if (clan.level >= 5) {
                                            Service.getInstance().sendThongBao(player,
                                                    "Bang hội của bạn đã đạt cấp tối đa");
                                            return;
                                        }
                                        if (clan.clanPoint < 1000) {
                                            Service.getInstance().sendThongBao(player, "Không đủ capsule");
                                            return;
                                        }
                                        clan.level++;
                                        clan.maxMember++;
                                        clan.clanPoint -= 1000;
                                        Service.getInstance().sendThongBao(player,
                                                "Bang hội của bạn đã được nâng cấp lên cấp " + clan.level);
                                    }
                                } else if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:
                                            ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                                            break;
                                        case 1:
                                            ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                                            break;
                                        case 2:
                                            ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.CARGO:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player pl) {
                            if (canOpenNpc(pl)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                                    if (pl.playerTask.taskMain.id == 7) {
                                        NpcService.gI().createTutorial(pl, this.avartar,
                                                "Hãy lên đường cứu đứa bé nhà tôi\n"
                                                + "Chắc bây giờ nó đang sợ hãi lắm rồi");
                                    } else {
                                        this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                                "Tàu vũ trụ Namếc tuy cũ nhưng tốc độ không hề kém bất kỳ loại tàu nào khác. Cậu muốn đi đâu?",
                                                "Đến\nTrái Đất", "Đến\nXayda", "Siêu thị");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:
                                            ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                                            break;
                                        case 1:
                                            ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                                            break;
                                        case 2:
                                            ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.CUI:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        private final int COST_FIND_BOSS = 20000000;

                        @Override
                        public void openBaseMenu(Player pl) {
                            if (canOpenNpc(pl)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                                    if (pl.playerTask.taskMain.id == 7) {
                                        NpcService.gI().createTutorial(pl, this.avartar,
                                                "Hãy lên đường cứu đứa bé nhà tôi\n"
                                                + "Chắc bây giờ nó đang sợ hãi lắm rồi");
                                    } else {
                                        if (this.mapId == 19) {

                                            int taskId = TaskService.gI().getIdTask(pl);
                                            switch (taskId) {
                                                case ConstTask.TASK_21_0:
                                                    this.createOtherMenu(pl, ConstNpc.MENU_FIND_KUKU,
                                                            "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                                            "Đến chỗ\nKuku\n(" + Util.numberToMoney(COST_FIND_BOSS)
                                                            + " vàng)",
                                                            "Đến Cold", "Đến\nNappa", "Từ chối");
                                                    break;
                                                case ConstTask.TASK_21_1:
                                                    this.createOtherMenu(pl, ConstNpc.MENU_FIND_MAP_DAU_DINH,
                                                            "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                                            "Đến chỗ\nMập đầu đinh\n("
                                                            + Util.numberToMoney(COST_FIND_BOSS) + " vàng)",
                                                            "Đến Cold", "Đến\nNappa", "Từ chối");
                                                    break;
                                                case ConstTask.TASK_21_2:
                                                    this.createOtherMenu(pl, ConstNpc.MENU_FIND_RAMBO,
                                                            "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                                            "Đến chỗ\nRambo\n(" + Util.numberToMoney(COST_FIND_BOSS)
                                                            + " vàng)",
                                                            "Đến Cold", "Đến\nNappa", "Từ chối");
                                                    break;
                                                default:
                                                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                                            "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                                            "Đến Cold", "Đến\nNappa", "Từ chối");

                                                    break;
                                            }
                                        } else if (this.mapId == 68) {
                                            this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                                    "Ngươi muốn về Thành Phố Vegeta", "Đồng ý", "Từ chối");
                                        } else {
                                            this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                                    "Tàu vũ trụ Xayda sử dụng công nghệ mới nhất, có thể đưa ngươi đi bất kỳ đâu, chỉ cần trả tiền là được.",
                                                    "Đến\nTrái Đất", "Đến\nNamếc", "Siêu thị");
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 26) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                                                break;
                                            case 2:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                                                break;
                                        }
                                    }
                                }
                                if (this.mapId == 19) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_FIND_KUKU) {
                                        switch (select) {
                                            case 0:
                                                Boss boss = BossManager.gI().getBossById(BossFactory.KUKU);
                                                if (boss != null && !boss.isDie()) {
                                                    if (player.inventory.gold >= COST_FIND_BOSS) {
                                                        player.inventory.subGold(COST_FIND_BOSS); // FIX: dùng method an toàn
                                                        ChangeMapService.gI().changeMap(player, boss.zone,
                                                                boss.location.x, boss.location.y);
                                                        Service.getInstance().sendMoney(player);
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Không đủ vàng, còn thiếu "
                                                                + Util.numberToMoney(
                                                                        COST_FIND_BOSS - player.inventory.gold)
                                                                + " vàng");
                                                    }
                                                }
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                                                break;
                                            case 2:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_FIND_MAP_DAU_DINH) {
                                        switch (select) {
                                            case 0:
                                                Boss boss = BossManager.gI().getBossById(BossFactory.MAP_DAU_DINH);
                                                if (boss != null && !boss.isDie()) {
                                                    if (player.inventory.gold >= COST_FIND_BOSS) {
                                                        player.inventory.subGold(COST_FIND_BOSS); // FIX: dùng method an toàn
                                                        ChangeMapService.gI().changeMap(player, boss.zone,
                                                                boss.location.x, boss.location.y);
                                                        Service.getInstance().sendMoney(player);
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Không đủ vàng, còn thiếu "
                                                                + Util.numberToMoney(
                                                                        COST_FIND_BOSS - player.inventory.gold)
                                                                + " vàng");
                                                    }
                                                }
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                                                break;
                                            case 2:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_FIND_RAMBO) {
                                        switch (select) {
                                            case 0:
                                                Boss boss = BossManager.gI().getBossById(BossFactory.RAMBO);
                                                if (boss != null && !boss.isDie()) {
                                                    if (player.inventory.gold >= COST_FIND_BOSS) {
                                                        player.inventory.subGold(COST_FIND_BOSS); // FIX: dùng method an toàn
                                                        ChangeMapService.gI().changeMap(player, boss.zone,
                                                                boss.location.x, boss.location.y);
                                                        Service.getInstance().sendMoney(player);
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Không đủ vàng, còn thiếu "
                                                                + Util.numberToMoney(
                                                                        COST_FIND_BOSS - player.inventory.gold)
                                                                + " vàng");
                                                    }
                                                }
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                                                break;
                                            case 2:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                                                break;
                                        }
                                    }
                                }
                                if (this.mapId == 68) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 19, -1, 1100);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.SANTA:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                createOtherMenu(player, ConstNpc.BASE_MENU,
                                        "Xin chào, ta có một số vật phẩm đặt biệt cậu có muốn xem không?",
                                        "Cửa hàng",
                                        "Mở rộng\nHành trang\nRương đồ",
                                        "Nhập mã\n quà tặng",
                                        "Cửa hàng\nHạn sử dụng",
                                        "Tiệm\nHớt tóc",
                                        "Danh\nhiệu");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0: // shop
                                                this.openShopWithGender(player, ConstNpc.SHOP_SANTA_0, 0);
                                                break;
                                            case 1:
                                                this.openShopWithGender(player, ConstNpc.SHOP_SANTA_2, 2);
                                                break;
                                            case 2: // giftcode
                                                Input.gI().createFormGiftCode(player);
                                                break;
                                            case 3: // cửa hàng hạn sử dụng
                                                this.openShopWithGender(player, ConstNpc.SHOP_SANTA_3, 4);
                                                break;
                                            case 4: // tiệm hớt tóc
                                                this.openShopWithGender(player, ConstNpc.SHOP_SANTA_1, 1);
                                                break;
                                            case 5: // danh hiệu
                                                this.openShopWithGender(player, ConstNpc.SHOP_SANTA_4, 3);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.URON:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player pl) {
                            if (canOpenNpc(pl)) {
                                this.openShopWithGender(pl, ConstNpc.SHOP_URON_0, 0);
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {

                            }
                        }
                    };
                    break;
                case ConstNpc.BA_HAT_MIT:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            Item bongTai = InventoryService.gI().findItemBagByTemp(player, (short) 454);
                            Item bongTaiCap2 = InventoryService.gI().findItemBagByTemp(player, (short) 921);
                            if (canOpenNpc(player)) {
                                if (this.mapId == 5) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                            "Chức năng\nPha lê", "Võ đài\nSinh tử", "Nâng Sét\n kích hoạt", "Trang Bị\n Thiên Sứ");
                                } else if (this.mapId == 112) {
                                    if (player.DoneVoDaiBaHatMit == 1) {
                                        this.createOtherMenu(player, ConstNpc.NHAN_QUA_VO_DAI, "Đây là phẩn thưởng của con.", "1 vệ tinh\n bất kì", "1 bùa 1h\n bất kỳ");
                                    } else {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi muốn đăng ký thi đấu võ đài?\n"
                                                + "nhiều phần thưởng giá trị đang đợi ngươi đó", "Top 100", "Đồng ý\n0 ngọc", "Từ chối", "Về\nđảo rùa");
                                    }
                                } else {
                                    if (player.event.luotNhanBuaMienPhi == 1) {
                                        if (bongTaiCap2 != null) {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                                    "Thưởng\nBùa 1h\nngẫu nhiên", "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm", "Mở chỉ số\nBông tai\nPorata cấp 2", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                                        } else if (bongTai != null) {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                                    "Thưởng\nBùa 1h\nngẫu nhiên", "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm", "Nâng cấp\nBông tai\nPorata", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                                        } else {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                                    "Thưởng\nBùa 1h\nngẫu nhiên", "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                                        }
                                    } else {
                                        if (bongTaiCap2 != null) {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                                    "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm", "Mở chỉ số\nBông tai\nPorata cấp 2", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                                        } else if (bongTai != null) {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                                    "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm", "Nâng cấp\nBông tai\nPorata", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                                        } else {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                                    "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            Item bongTai = InventoryService.gI().findItemBagByTemp(player, (short) 454);
                            Item bongTaiCap2 = InventoryService.gI().findItemBagByTemp(player, (short) 921);
                            if (canOpenNpc(player)) {
                                if (this.mapId == 5) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                createOtherMenu(player, ConstNpc.CHUC_NANG_SAO_PHA_LE, "Ta có thể giúp gì cho ngươi?", "Ép sao\ntrang bị", "Pha lê\nhóa\ntrang bị", "Nâng cấp\nSao pha lê", "Đánh bóng\nSao pha lê", "Cường hóa\nlỗ sao\npha lê", "Tạo đá\nHematite");
                                                return;
                                            case 2:
                                                createOtherMenu(player, ConstNpc.MENU_CHUYEN_HOA_SKH, "Ta sẽ nâng trang bị hủy diệt của người\nlên một tầm cao mới hoàn toàn khác", "Nâng cấp\nSKH", "Nâng Level");
                                                return;
                                            case 1:
                                                ChangeMapService.gI().changeMap(player, 112, -1, 55, 408);
                                                return;
                                            case 3:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.CHE_TAO_DO_THIEN_SU);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.CHUC_NANG_SAO_PHA_LE) {
                                        switch (select) {
                                            case 0:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.EP_SAO_TRANG_BI);
                                                break;
                                            case 1:
                                                createOtherMenu(player, ConstNpc.MENU_PHA_LE_HOA_TRANG_BI, "Ngươi muốn pha lê hóa trang bị bằng cách nào?", "Bằng ngọc", "Từ chối");
                                                return;
                                            case 2: // NANG CAP SAO PHA LE
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.NANG_CAP_SAO_PHA_LE);
                                                break;
                                            case 3:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.DANH_BONG_SAO_PHA_LE);
                                                break;
                                            case 4:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.CUONG_HOA_LO_SAO_PHA_LE);
                                                break;
                                            case 5:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.TAO_DA_HEMATILE);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHUYEN_HOA_SKH) {
                                        switch (select) {

                                            case 0: // NANG CAP SAO PHA LE
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.DAP_SET_KICH_HOAT_CAO_CAP);
                                                break;
                                            case 1: // NANG CAP SAO PHA LE
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.NANG_CAP_SKH);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_PHA_LE_HOA_TRANG_BI) {
                                        switch (select) {
                                            case 0:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.PHA_LE_HOA_TRANG_BI);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHUYEN_HOA_TRANG_BI) {
                                        switch (select) {
                                            case 0:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.CHUYEN_HOA_BANG_VANG);
                                                break;
                                            case 1:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.CHUYEN_HOA_BANG_NGOC);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHUYEN_HOA_TRANG_BI) {
                                        switch (select) {
                                            case 0:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.CHUYEN_HOA_BANG_VANG);
                                                break;
                                            case 1:
                                                CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.CHUYEN_HOA_BANG_NGOC);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                                        switch (player.combineNew.typeCombine) {
                                            case CombineServiceNew.EP_SAO_TRANG_BI:
                                            case CombineServiceNew.PHA_LE_HOA_TRANG_BI:
                                            case CombineServiceNew.DOI_VE_HUY_DIET:
                                            case CombineServiceNew.DAP_SET_KICH_HOAT:
                                            case CombineServiceNew.DAP_SET_KICH_HOAT_CAO_CAP:
                                            case CombineServiceNew.CHUYEN_HOA_BANG_VANG:
                                            case CombineServiceNew.CHUYEN_HOA_BANG_NGOC:
                                            case CombineServiceNew.GIA_HAN_CAI_TRANG:
                                            case CombineServiceNew.NANG_CAP_SKH:
                                            case CombineServiceNew.CHE_TAO_DO_THIEN_SU:
                                                CombineServiceNew.gI().startCombine(player, select);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_NANG_CAP_DO_TS) {
                                        if (select == 0) {
                                            CombineServiceNew.gI().startCombine(player, select);
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.ORTHER_MENU) {
                                        switch (select) {
                                            case 0:
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.DAP_SET_KICH_HOAT);
                                                break;
                                            case 1:
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.DAP_SET_KICH_HOAT_CAO_CAP);
                                                break;

                                        }
                                    }
                                } else if (this.mapId == 112) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:// Top 100 gì đó đéo biết

                                                break;
                                            case 1:// xác nhận lên võ đài
                                                VoDaiSinhTuService.gI().startChallenge(player);
                                                break;
                                            case 2:// từ chối

                                                break;
                                            case 3:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.NHAN_QUA_VO_DAI) {
                                        switch (select) {
                                            case 0:
                                                if (player.DoneVoDaiBaHatMit == 1) {
                                                    player.DoneVoDaiBaHatMit = 0;
                                                    Service.getInstance().sendThongBao(player, "Bạn đã nhận được 1 vệ tinh ngẫu nhiên");
                                                    break;
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Bạn đã nhận phần thưởng này rồi");
                                                }
                                                break;
                                            case 1:
                                                if (player.DoneVoDaiBaHatMit == 1) {
                                                    player.DoneVoDaiBaHatMit = 0;
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Bạn đã nhận được 1 bùa 1h ngẫu nhiên");
                                                }
                                                break;
                                        }
                                    }
                                } else if (this.mapId == 42 || this.mapId == 43 || this.mapId == 44 || this.mapId == 84) { // BA_HAT_MIT_BUA
                                    if (player.iDMark.isBaseMenu()) {
                                        if (player.event.luotNhanBuaMienPhi == 1) {
                                            if (bongTaiCap2 != null) {
                                                switch (select) {
                                                    case 0: // Ngẫu nhiên bùa 1h
                                                        if (player.event.luotNhanBuaMienPhi == 1) {
                                                            int idItem = Util.nextInt(213, 219);
                                                            player.charms.addTimeCharms(idItem, 60);
                                                            Item bua = ItemService.gI().createNewItem((short) idItem);
                                                            Service.getInstance().sendThongBao(player, "Bạn vừa nhận thưởng " + bua.getName());
                                                            player.event.luotNhanBuaMienPhi = 0;
                                                        } else {
                                                            Service.getInstance().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                                        }
                                                        break;
                                                    case 1: // Sách tuyệt kỹ
                                                        createOtherMenu(player, ConstNpc.SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                                                "Đóng thành\nSách cũ",
                                                                "Đổi Sách\nTuyệt kỹ",
                                                                "Giám định\nSách",
                                                                "Tẩy\nSách",
                                                                "Nâng cấp\nSách\nTuyệt kỹ",
                                                                "Hồi phục\nSách",
                                                                "Phân rã\nSách");
                                                        break;
                                                    case 2: // shop bùa
                                                        createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                                                "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                                                "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                                                "Đóng");
                                                        break;
                                                    case 3: // nâng cấp vật phẩm
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_VAT_PHAM);
                                                        break;
                                                    case 4: // mở chỉ số bông tai cấp 2
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.MO_CHI_SO_BONG_TAI);
                                                        break;
                                                    case 5: //Làm phép nhập đá
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.LAM_PHEP_NHAP_DA);
                                                        break;
                                                    case 6:// 
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NHAP_NGOC_RONG);
                                                        break;
                                                }
                                            } else if (bongTai != null) {
                                                switch (select) {
                                                    case 0: // Ngẫu nhiên bùa 1h
                                                        if (player.event.luotNhanBuaMienPhi == 1) {
                                                            int idItem = Util.nextInt(213, 219);
                                                            player.charms.addTimeCharms(idItem, 60);
                                                            Item bua = ItemService.gI().createNewItem((short) idItem);
                                                            Service.getInstance().sendThongBao(player, "Bạn vừa nhận thưởng " + bua.getName());
                                                            player.event.luotNhanBuaMienPhi = 0;
                                                        } else {
                                                            Service.getInstance().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                                        }
                                                        break;
                                                    case 1: // Sách tuyệt kỹ
                                                        createOtherMenu(player, ConstNpc.SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                                                "Đóng thành\nSách cũ",
                                                                "Đổi Sách\nTuyệt kỹ",
                                                                "Giám định\nSách",
                                                                "Tẩy\nSách",
                                                                "Nâng cấp\nSách\nTuyệt kỹ",
                                                                "Hồi phục\nSách",
                                                                "Phân rã\nSách");
                                                        break;
                                                    case 2: // shop bùa
                                                        createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                                                "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                                                "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                                                "Đóng");
                                                        break;
                                                    case 3: // nâng cấp vật phẩm
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_VAT_PHAM);
                                                        break;
                                                    case 4: // nâng cấp bông tai cấp 2
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_BONG_TAI);
                                                        break;
                                                    case 5: //Làm phép nhập đá
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.LAM_PHEP_NHAP_DA);
                                                        break;
                                                    case 6:// 
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NHAP_NGOC_RONG);
                                                        break;
                                                }
                                            } else {
                                                switch (select) {
                                                    case 0: // Ngẫu nhiên bùa 1h
                                                        if (player.event.luotNhanBuaMienPhi == 1) {
                                                            int idItem = Util.nextInt(213, 219);
                                                            player.charms.addTimeCharms(idItem, 60);
                                                            Item bua = ItemService.gI().createNewItem((short) idItem);
                                                            Service.getInstance().sendThongBao(player, "Bạn vừa nhận thưởng " + bua.getName());
                                                            player.event.luotNhanBuaMienPhi = 0;
                                                        } else {
                                                            Service.getInstance().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                                        }
                                                        break;
                                                    case 1: // Sách tuyệt kỹ
                                                        createOtherMenu(player, ConstNpc.SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                                                "Đóng thành\nSách cũ",
                                                                "Đổi Sách\nTuyệt kỹ",
                                                                "Giám định\nSách",
                                                                "Tẩy\nSách",
                                                                "Nâng cấp\nSách\nTuyệt kỹ",
                                                                "Hồi phục\nSách",
                                                                "Phân rã\nSách");
                                                        break;
                                                    case 2: // shop bùa
                                                        createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                                                "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                                                "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                                                "Đóng");
                                                        break;
                                                    case 3: // nâng cấp vật phẩm
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_VAT_PHAM);
                                                        break;
                                                    case 4: //Làm phép nhập đá
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.LAM_PHEP_NHAP_DA);
                                                        break;
                                                    case 5:// 
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NHAP_NGOC_RONG);
                                                        break;
                                                }
                                            }
                                        } else {
                                            if (bongTaiCap2 != null) {
                                                switch (select) {
                                                    case 0: // Sách tuyệt kỹ
                                                        createOtherMenu(player, ConstNpc.SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                                                "Đóng thành\nSách cũ",
                                                                "Đổi Sách\nTuyệt kỹ",
                                                                "Giám định\nSách",
                                                                "Tẩy\nSách",
                                                                "Nâng cấp\nSách\nTuyệt kỹ",
                                                                "Hồi phục\nSách",
                                                                "Phân rã\nSách");
                                                        break;
                                                    case 1: // shop bùa
                                                        createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                                                "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                                                "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                                                "Đóng");
                                                        break;
                                                    case 2: // nâng cấp vật phẩm
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_VAT_PHAM);
                                                        break;
                                                    case 3:
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.MO_CHI_SO_BONG_TAI);
                                                        break;
                                                    case 4: //Làm phép nhập đá
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.LAM_PHEP_NHAP_DA);
                                                        break;
                                                    case 5:// 
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NHAP_NGOC_RONG);
                                                        break;
                                                }
                                            } else if (bongTai != null) {
                                                switch (select) {
                                                    case 0: // Sách tuyệt kỹ
                                                        createOtherMenu(player, ConstNpc.SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                                                "Đóng thành\nSách cũ",
                                                                "Đổi Sách\nTuyệt kỹ",
                                                                "Giám định\nSách",
                                                                "Tẩy\nSách",
                                                                "Nâng cấp\nSách\nTuyệt kỹ",
                                                                "Hồi phục\nSách",
                                                                "Phân rã\nSách");
                                                        break;
                                                    case 1: // shop bùa
                                                        createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                                                "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                                                "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                                                "Đóng");
                                                        break;
                                                    case 2: // nâng cấp vật phẩm
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_VAT_PHAM);
                                                        break;
                                                    case 3:
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_BONG_TAI);
                                                        break;
                                                    case 4: //Làm phép nhập đá
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.LAM_PHEP_NHAP_DA);
                                                        break;
                                                    case 5:// 
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NHAP_NGOC_RONG);
                                                        break;
                                                }
                                            } else {
                                                switch (select) {
                                                    case 0: // Sách tuyệt kỹ
                                                        createOtherMenu(player, ConstNpc.SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                                                "Đóng thành\nSách cũ",
                                                                "Đổi Sách\nTuyệt kỹ",
                                                                "Giám định\nSách",
                                                                "Tẩy\nSách",
                                                                "Nâng cấp\nSách\nTuyệt kỹ",
                                                                "Hồi phục\nSách",
                                                                "Phân rã\nSách");
                                                        break;
                                                    case 1: // shop bùa
                                                        createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                                                "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                                + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                                                "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                                                "Đóng");
                                                        break;
                                                    case 2: // nâng cấp vật phẩm
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NANG_CAP_VAT_PHAM);
                                                        break;
                                                    case 3: //Làm phép nhập đá
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.LAM_PHEP_NHAP_DA);
                                                        break;
                                                    case 4:// 
                                                        CombineServiceNew.gI().openTabCombine(player,
                                                                CombineServiceNew.NHAP_NGOC_RONG);
                                                        break;
                                                }
                                            }
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.SACH_TUYET_KY) {
                                        switch (select) {
                                            case 0:
                                                Item trangSachCu = InventoryService.gI().findItemBagByTemp(player, 1291);

                                                Item biaSach = InventoryService.gI().findItemBagByTemp(player, 1281);
                                                if ((trangSachCu != null && trangSachCu.quantity >= 9999) && (biaSach != null && biaSach.quantity >= 1)) {
                                                    createOtherMenu(player, ConstNpc.DONG_THANH_SACH_CU,
                                                            "|2|Chế tạo Cuốn sách cũ\n"
                                                            + "|1|Trang sách cũ " + trangSachCu.quantity + "/9999\n"
                                                            + "Bìa sách " + biaSach.quantity + "/1\n"
                                                            + "Tỉ lệ thành công: 20%\n"
                                                            + "Thất bại mất 99 trang sách và 1 bìa sách", "Đồng ý", "Từ chối");
                                                    break;
                                                } else {
                                                    String NpcSay = "|2|Chế tạo Cuốn sách cũ\n";
                                                    if (trangSachCu == null) {
                                                        NpcSay += "|7|Trang sách cũ " + "0/9999\n";
                                                    } else {
                                                        NpcSay += "|1|Trang sách cũ " + trangSachCu.quantity + "/9999\n";
                                                    }
                                                    if (biaSach == null) {
                                                        NpcSay += "|7|Bìa sách " + "0/1\n";
                                                    } else {
                                                        NpcSay += "|1|Bìa sách " + biaSach.quantity + "/1\n";
                                                    }

                                                    NpcSay += "|7|Tỉ lệ thành công: 20%\n";
                                                    NpcSay += "|7|Thất bại mất 99 trang sách và 1 bìa sách";
                                                    createOtherMenu(player, ConstNpc.DONG_THANH_SACH_CU_2,
                                                            NpcSay, "Từ chối");
                                                    break;
                                                }
                                            case 1:
                                                Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, 1284);
                                                Item kimBam = InventoryService.gI().findItemBagByTemp(player, 1282);

                                                if ((cuonSachCu != null && cuonSachCu.quantity >= 10) && (kimBam != null && kimBam.quantity >= 1)) {
                                                    createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY,
                                                            "|2|Đổi sách tuyệt kỹ 1\n"
                                                            + "|1|Cuốn sách cũ " + cuonSachCu.quantity + "/10\n"
                                                            + "Kìm bấm giấy " + kimBam.quantity + "/1\n"
                                                            + "Tỉ lệ thành công: 20%\n", "Đồng ý", "Từ chối");
                                                    break;
                                                } else {
                                                    String NpcSay = "|2|Đổi sách Tuyệt kỹ 1\n";
                                                    if (cuonSachCu == null) {
                                                        NpcSay += "|7|Cuốn sách cũ " + "0/10\n";
                                                    } else {
                                                        NpcSay += "|1|Cuốn sách cũ " + cuonSachCu.quantity + "/10\n";
                                                    }
                                                    if (kimBam == null) {
                                                        NpcSay += "|7|Kìm bấm giấy " + "0/1\n";
                                                    } else {
                                                        NpcSay += "|1|Kìm bấm giấy " + kimBam.quantity + "/1\n";
                                                    }
                                                    NpcSay += "|7|Tỉ lệ thành công: 20%\n";
                                                    createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY_2,
                                                            NpcSay, "Từ chối");
                                                }
                                                break;
                                            case 2:// giám định sách
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.GIAM_DINH_SACH);
                                                break;
                                            case 3:// tẩy sách
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.TAY_SACH);
                                                break;
                                            case 4:// nâng cấp sách
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.NANG_CAP_SACH_TUYET_KY);
                                                break;
                                            case 5:// phục hồi sách
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.PHUC_HOI_SACH);
                                                break;
                                            case 6:// phân rã sách
                                                CombineServiceNew.gI().openTabCombine(player,
                                                        CombineServiceNew.PHAN_RA_SACH);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.DOI_SACH_TUYET_KY) {
                                        switch (select) {
                                            case 0:
                                                Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, 1284);
                                                Item kimBam = InventoryService.gI().findItemBagByTemp(player, 1282);

                                                short baseValue = 1287;
                                                short genderModifier = (player.gender == 0) ? -2 : ((player.gender == 2) ? 2 : (short) 0);

                                                Item sachTuyetKy = ItemService.gI().createNewItem((short) (baseValue + genderModifier));

                                                if (Util.isTrue(20, 100)) {

                                                    sachTuyetKy.itemOptions.add(new ItemOption(229, 0));
                                                    sachTuyetKy.itemOptions.add(new ItemOption(21, 40));
                                                    sachTuyetKy.itemOptions.add(new ItemOption(30, 0));
                                                    sachTuyetKy.itemOptions.add(new ItemOption(87, 1));
                                                    sachTuyetKy.itemOptions.add(new ItemOption(230, 5));
                                                    sachTuyetKy.itemOptions.add(new ItemOption(231, 1000));
                                                    try { // send effect susscess
                                                        Message msg = new Message(-81);
                                                        msg.writer().writeByte(0);
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeShort(tempId);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(1);
                                                        msg.writer().writeByte(2);
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, kimBam));
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, cuonSachCu));
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(7);
                                                        msg.writer().writeShort(sachTuyetKy.template.iconID);
                                                        msg.writer().writeShort(-1);
                                                        msg.writer().writeShort(-1);
                                                        msg.writer().writeShort(-1);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                    } catch (Exception e) {
                                                    }
                                                    InventoryService.gI().addItemList(player.inventory.itemsBag, sachTuyetKy, 1);
                                                    InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 10);
                                                    InventoryService.gI().subQuantityItemsBag(player, kimBam, 1);
                                                    InventoryService.gI().sendItemBags(player);
                                                    return;
                                                } else {
                                                    try { // send effect faile
                                                        Message msg = new Message(-81);
                                                        msg.writer().writeByte(0);
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeShort(tempId);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(1);
                                                        msg.writer().writeByte(2);
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, kimBam));
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, cuonSachCu));
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(8);
                                                        msg.writer().writeShort(-1);
                                                        msg.writer().writeShort(-1);
                                                        msg.writer().writeShort(-1);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                    } catch (Exception e) {
                                                    }
                                                    InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 5);
                                                    InventoryService.gI().subQuantityItemsBag(player, kimBam, 1);
                                                    InventoryService.gI().sendItemBags(player);
                                                }
                                                return;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.DONG_THANH_SACH_CU) {
                                        switch (select) {
                                            case 0:
                                                Item trangSachCu = InventoryService.gI().findItemBagByTemp(player, 1291);
                                                Item biaSach = InventoryService.gI().findItemBagByTemp(player, 1281);
                                                Item cuonSachCu = ItemService.gI().createNewItem((short) 1284);
                                                if (Util.isTrue(20, 100)) {
                                                    cuonSachCu.itemOptions.add(new ItemOption(30, 0));

                                                    try { // send effect susscess

                                                        Message msg = new Message(-81);
                                                        msg.writer().writeByte(0);
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeShort(tempId);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();

                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(1);
                                                        msg.writer().writeByte(2);
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, trangSachCu));
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, biaSach));
                                                        player.sendMessage(msg);
                                                        msg.cleanup();

                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(7);
                                                        msg.writer().writeShort(cuonSachCu.template.iconID);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                    } catch (Exception e) {
                                                    }

                                                    InventoryService.gI().addItemList(player.inventory.itemsBag, cuonSachCu, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, trangSachCu, 9999);
                                                    InventoryService.gI().subQuantityItemsBag(player, biaSach, 1);
                                                    InventoryService.gI().sendItemBags(player);
                                                    return;
                                                } else {
                                                    try { // send effect faile
                                                        Message msg = new Message(-81);
                                                        msg.writer().writeByte(0);
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeUTF("test");
                                                        msg.writer().writeShort(tempId);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();

                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(1);
                                                        msg.writer().writeByte(2);
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, biaSach));
                                                        msg.writer().writeByte(InventoryService.gI().getIndexBag(player, trangSachCu));
                                                        player.sendMessage(msg);
                                                        msg.cleanup();

                                                        msg = new Message(-81);
                                                        msg.writer().writeByte(8);
                                                        player.sendMessage(msg);
                                                        msg.cleanup();
                                                    } catch (Exception e) {
                                                    }
                                                    InventoryService.gI().subQuantityItemsBag(player, trangSachCu, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, biaSach, 1);
                                                    InventoryService.gI().sendItemBags(player);
                                                }
                                                return;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA) {
                                        switch (select) {
                                            case 0:
                                                ShopService.gI().openShopBua(player, ConstNpc.SHOP_BA_HAT_MIT_0, 0);
                                                break;
                                            case 1:
                                                ShopService.gI().openShopBua(player, ConstNpc.SHOP_BA_HAT_MIT_1, 1);
                                                break;
                                            case 2:
                                                ShopService.gI().openShopBua(player, ConstNpc.SHOP_BA_HAT_MIT_2, 2);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                                        switch (player.combineNew.typeCombine) {
                                            case CombineServiceNew.NANG_CAP_VAT_PHAM:
                                                if (select == 0) {
                                                    player.iDMark.isUseTuiBaoVeNangCap = false;
                                                    CombineServiceNew.gI().startCombine(player, select);
                                                } else if (select == 1) {
                                                    player.iDMark.isUseTuiBaoVeNangCap = true;
                                                    CombineServiceNew.gI().startCombine(player, select);
                                                }
                                                break;
                                            case CombineServiceNew.NANG_CAP_BONG_TAI:
                                            case CombineServiceNew.MO_CHI_SO_BONG_TAI:
                                            case CombineServiceNew.LAM_PHEP_NHAP_DA:
                                            case CombineServiceNew.NHAP_NGOC_RONG:
                                            //START _ SÁCH TUYỆT KỸ//
                                            case CombineServiceNew.GIAM_DINH_SACH:
                                            case CombineServiceNew.TAY_SACH:
                                            case CombineServiceNew.NANG_CAP_SACH_TUYET_KY:
                                            case CombineServiceNew.PHUC_HOI_SACH:
                                            case CombineServiceNew.PHAN_RA_SACH:
                                                //END _ SÁCH TUYỆT KỸ//
                                                CombineServiceNew.gI().startCombine(player, select);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.RUONG_DO:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                InventoryService.gI().sendItemBox(player);
                                InventoryService.gI().openBox(player);
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {

                            }
                        }
                    };
                    break;
                case ConstNpc.DAU_THAN:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                player.magicTree.openMenuTree();
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                TaskService.gI().checkDoneTaskConfirmMenuNpc(player, this, (byte) select);
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.MAGIC_TREE_NON_UPGRADE_LEFT_PEA:
                                        if (select == 0) {
                                            player.magicTree.harvestPea();
                                        } else if (select == 1) {
                                            if (player.magicTree.level == 10) {
                                                player.magicTree.fastRespawnPea();
                                            } else {
                                                player.magicTree.showConfirmUpgradeMagicTree();
                                            }
                                        } else if (select == 2) {
                                            player.magicTree.fastRespawnPea();
                                        }
                                        break;
                                    case ConstNpc.MAGIC_TREE_NON_UPGRADE_FULL_PEA:
                                        if (select == 0) {
                                            player.magicTree.harvestPea();
                                        } else if (select == 1) {
                                            player.magicTree.showConfirmUpgradeMagicTree();
                                        }
                                        break;
                                    case ConstNpc.MAGIC_TREE_CONFIRM_UPGRADE:
                                        if (select == 0) {
                                            player.magicTree.upgradeMagicTree();
                                        }
                                        break;
                                    case ConstNpc.MAGIC_TREE_UPGRADE:
                                        if (select == 0) {
                                            player.magicTree.fastUpgradeMagicTree();
                                        } else if (select == 1) {
                                            player.magicTree.showConfirmUnuppgradeMagicTree();
                                        }
                                        break;
                                    case ConstNpc.MAGIC_TREE_CONFIRM_UNUPGRADE:
                                        if (select == 0) {
                                            player.magicTree.unupgradeMagicTree();
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.CALICK:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        private void changeMap_CaLich() {
                            if (this.mapId != 102) {
                                this.map.npcs.remove(this);
                                Map map = MapService.gI().getMapForCalich();
                                this.mapId = map.mapId;
                                this.cx = Util.nextInt(100, map.mapWidth - 100);
                                this.cy = map.yPhysicInTop(this.cx, 0);
                                this.map = map;
                                this.map.npcs.add(this);
                            }
                        }

                        @Override
                        public void openBaseMenu(Player player) {
                            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
                            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                            }
                            if (TaskService.gI().getIdTask(player) < ConstTask.TASK_20_0) {
                                Service.getInstance().hideWaitDialog(player);
                                Service.getInstance().sendThongBao(player, "Không thể thực hiện");
                                return;
                            }
                            if (this.mapId == 102) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Chào chú, cháu có thể giúp gì?",
                                        "Kể\nChuyện", "Quay về\nQuá khứ");
                            } else {
                                changeMap_CaLich();
                                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Chào chú, cháu có thể giúp gì?",
                                        "Kể\nChuyện", "Đi đến\nTương lai", "Từ chối");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (this.mapId == 102) {
                                if (player.iDMark.isBaseMenu()) {
                                    if (select == 0) {
                                        // kể chuyện
                                        NpcService.gI().createTutorial(player, this.avartar, ConstNpc.CALICK_KE_CHUYEN);
                                    } else if (select == 1) {
                                        // về quá khứ
                                        ChangeMapService.gI().goToQuaKhu(player);
                                    }
                                }
                            } else if (player.iDMark.isBaseMenu()) {
                                if (select == 0) {
                                    // kể chuyện
                                    NpcService.gI().createTutorial(player, this.avartar, ConstNpc.CALICK_KE_CHUYEN);
                                } else if (select == 1) {
                                    // đến tương lai
                                    // changeMap();
                                    if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_20_0) {
                                        ChangeMapService.gI().goToTuongLai(player);
                                    }
                                } else {
                                    Service.getInstance().sendThongBao(player, "Không thể thực hiện");
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.JACO:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
                            if (this.mapId == 24) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Gô Tên, Calích và Monaka đang gặp chuyện ở hành tinh Potaufeu\nHãy đến đó ngay", "Đến\nPotaufeu", "Từ chối");
                            } else {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?", "Đến\nTrái Đất", "Đến\nNamếc", "Đến\nXayda", "Từ chối");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 24) {
                                    if (player.iDMark.isBaseMenu()) {
                                        if (select == 0) {
                                            // đến potaufeu
                                            ChangeMapService.gI().goToPotaufeu(player);
                                        }
                                    }
                                } else if (this.mapId == 139) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                                                break;
                                            case 2:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.POTAGE:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Hãy giúp ta đánh bại bản sao\nNgươi chỉ có 5 phút để hạ hắn\nPhần thưởng cho ngươi là 1 bình Commeson",
                                    "Hướng\ndẫn\nthêm", "OK", "Từ chối");
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:
                                            NpcService.gI().createTutorial(player, avartar, "Thứ bị phong ấn tại đây là vũ khí có tên Commeson\b"
                                                    + "được tạo ra nhằm bảo vệ cho hành tinh Potaufeu\b"
                                                    + "Tuy nhiên nó đã tàn phá mọi thứ trong quá khứ\n"
                                                    + "Khiến cư dân Potaufeu niêm phong nó với cái giá\b phải trả là mạng sống của họ\b Ta, Potage là người duy nhất sống sót\b"
                                                    + "và ta đã bảo vệ phong ấn hơn một trăm năm.\n"
                                                    + "Tuy nhiên bọn xâm lược Gryll đã đến và giải thoát Commeson\b"
                                                    + "Hãy giúp ta tiêu diệt bản sao do Commeson tạo ra\b"
                                                    + "và niêm phong Commeson một lần và mãi mãi");
                                            break;
                                        case 1:// gọi nhân bản
                                            if (player.zone.getBosses().size() != 0) {
                                                this.createOtherMenu(player, 251003, "Đang có 1 nhân bản của " + player.zone.getBosses().get(0).name + " hãy chờ kết quả trận đấu", "OK");
                                                return;
                                            }
                                            if (!player.itemTime.doneDanhNhanBan) {
                                                player.itemTime.isDanhNhanBan = true;
                                                player.itemTime.lasttimeDanhNhanBan = System.currentTimeMillis();

                                                ItemTimeService.gI().sendAllItemTime(player);
                                                List<Skill> skillList = new ArrayList<>();
                                                for (byte i = 0; i < player.playerSkill.skills.size(); i++) {
                                                    Skill skill = player.playerSkill.skills.get(i);
                                                    if (skill.point > 0) {
                                                        skillList.add(skill);
                                                    }
                                                }
                                                int[][] skillTemp = new int[skillList.size()][5];
                                                for (byte i = 0; i < skillList.size(); i++) {
                                                    Skill skill = skillList.get(i);
                                                    if (skill.point > 0) {
                                                        skillTemp[i][0] = skill.template.id;
                                                        skillTemp[i][1] = skill.point;
                                                        skillTemp[i][2] = skill.coolDown;
                                                    }
                                                }

                                                BossData bossdataa = BossData.builder()
                                                        .name(player.name)
                                                        .gender(player.gender)
                                                        .typeDame(Boss.DAME_NORMAL)
                                                        .typeHp(Boss.HP_NORMAL)
                                                        .dame(player.nPoint.hpMax / 10)
                                                        .hp(new int[][]{{player.nPoint.dame * 10}})
                                                        .outfit(new short[]{player.getHead(), player.getBody(), player.getLeg(), player.getFlagBag(), player.getAura(), player.getEffFront()})
                                                        .skillTemp(skillTemp)
                                                        .secondsRest(BossData._0_GIAY)
                                                        .build();

                                                try {
                                                    Boss_NhanBan dt = new Boss_NhanBan(Util.createIdDuongTank((int) ((byte) player.id)), bossdataa, player.zone, player.location.x, player.location.y, (int) player.id);
                                                } catch (Exception ex) {
                                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                                break;
                                            } else {
                                                Service.getInstance().sendThongBao(player, "Hãy chờ đến ngày mai");
                                            }
                                    }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.THAN_MEO_KARIN:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (mapId == ConstMap.THAP_KARIN) {
                                    player.thachDauNPC = 0;
                                    if (player.zone instanceof ZSnakeRoad) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                "Hãy cầm lấy hai hạt đậu cuối cùng ở đây\nCố giữ mình nhé "
                                                + player.name,
                                                "Cảm ơn\nsư phụ");
                                    } else if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                        if (player.doneThachDauThanMeo == 0) {
                                            this.createOtherMenu(player, ConstNpc.THACH_DAU_THAN_MEO, "Muốn chiến thắng Tàu Pảy Pảy phải đánh bại được ta đã", "Đăng ký\n tập\n tự động", "Nhiệm vụ", "Tập luyện\n với\n Thần Mèo", "Thách đấu\nThần Mèo");
                                        } else {
                                            if (player.doneThachDauYanjiro == 1) {
                                                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                        "Con hãy bay theo cây Gậy Như Ý trên đỉnh tháp để đến Thần Điện gặp Thượng đế\nCon rất xứng đáng để làm đệ tử ông ấy.", "Đăng ký\n tập\n tự động", "Tập luyện\n với\n Thần Mèo", "Tập luyện\n với\n Yajiro");
                                            } else {
                                                this.createOtherMenu(player, ConstNpc.THACH_DAU_YAJIRO,
                                                        "Từ giờ Yajirô sẽ luyện tập cùng ngươi. Yajirô đã từng lên đây tập luyện và bây giờ hắn mạnh hơn ta đấy", "Đăng ký\n tập\n tự động", "Tập luyện\n với\n Yajirô", "Thách đấu\nYajirô");
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                Boss boss = BossManager.gI().getBossById((int) ((int) -251003 - player.id) - 300000);
                                if (mapId == ConstMap.THAP_KARIN) {
                                    switch (player.iDMark.getIndexMenu()) {
                                        case ConstNpc.BASE_MENU:
                                            if (player.zone instanceof ZSnakeRoad) {
                                                switch (select) {
                                                    case 0:
                                                        player.setInteractWithKarin(true);
                                                        Service.getInstance().sendThongBao(player,
                                                                "Hãy mau bay xuống chân tháp Karin");
                                                        break;
                                                }
                                            } else {
                                                switch (select) {
                                                    case 1: // tập luyện với Thần mèo
                                                        createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_THAN_MEO, "Con có chắc chắn muốn tập luyện ?\nTập luyện với ta sẽ tăng 20 sức mạnh mỗi phút", "Đồng ý\nluyện tập", "Không\nđồng ý");
                                                        break;
                                                    case 2: // tập luyện với yajirô
                                                        createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_YAJIRO, "Con có chắc chắn muốn tập luyện ?\nTập luyện với Yajirô sẽ tăng 40 sức mạnh mỗi phút", "Đồng ý\nluyện tập", "Không\nđồng ý");
                                                        break;
                                                }
                                            }
                                            break;
                                        case ConstNpc.COFIRM_LUYEN_TAP_THAN_MEO:
                                            switch (select) {
                                                case 0:

                                                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                                                    Runnable task = () -> {
                                                        hide_npc(player, 18, 0);
                                                        scheduler.shutdown();
                                                    };
                                                    scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                                    try {
                                                        Boss_ThanMeo dt = new Boss_ThanMeo(Util.createIdDuongTank((int) ((byte) player.id)), BossData.THAN_MEO, player.zone, this.cx, this.cy, (int) player.id);
                                                    } catch (Exception ex) {
                                                        Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.zone.load_Me_To_Another(player);
                                                    break;
                                                default:
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.COFIRM_LUYEN_TAP_YAJIRO:
                                            switch (select) {
                                                case 0:
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    break;
                                                default:
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.THACH_DAU_THAN_MEO:
                                            switch (select) {
                                                case 2: // luyện tập với thần mèo
                                                    createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_THAN_MEO, "Con có chắc chắn muốn tập luyện ?\nTập luyện với ta sẽ tăng 20 sức mạnh mỗi phút", "Đồng ý\nluyện tập", "Không\nđồng ý");
                                                    break;
                                                case 3:
                                                    createOtherMenu(player, ConstNpc.COFIRM_THACH_DAU_THAN_MEO, "Con có chắc chắn muốn thách đấu ?\n"
                                                            + "Nếu thắng ta sẽ được tập với Yajirô, tăng 40 sức mạnh mỗi phút", "Đồng ý\ngiao đấu", "Không\nđồng ý");
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.COFIRM_THACH_DAU_THAN_MEO:
                                            switch (select) {
                                                case 0:
                                                    try {
                                                    Boss_ThanMeo dt = new Boss_ThanMeo(Util.createIdDuongTank((int) ((byte) player.id)), BossData.THAN_MEO, player.zone, this.cx, this.cy, (int) player.id);
                                                } catch (Exception ex) {
                                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                }

                                                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                                                Runnable task = () -> {
                                                    hide_npc(player, 18, 0);
                                                    scheduler.shutdown();
                                                };
                                                scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                player.thachDauNPC = 1;
                                                player.zone.load_Me_To_Another(player);
                                                break;
                                                default:
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.THACH_DAU_YAJIRO:
                                            switch (select) {
                                                case 1:// luyện tập với Yajirô
                                                    createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_YAJIRO, "Con có chắc chắn muốn tập luyện ?\nTập luyện với Yajirô sẽ tăng 40 sức mạnh mỗi phút", "Đồng ý\nluyện tập", "Không\nđồng ý");
                                                    break;
                                                case 2:// thách đấu với Yajirô
                                                    createOtherMenu(player, ConstNpc.COFIRM_THACH_DAU_YAJIRO, "Con có chắc chắn muốn thách đấu ?\n"
                                                            + "Nếu thắng được Yajirô, con sẽ được học võ với người mạnh hơn để tăng đến 80 sức mạnh mỗi phút", "Đồng ý\ngiao đấu", "Không\nđồng ý");
                                                    break;
                                            }
                                        case ConstNpc.COFIRM_THACH_DAU_YAJIRO:
                                            switch (select) {
                                                case 0:
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.thachDauNPC = 1;
                                                    break;
                                                default:
                                                    break;
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.THUONG_DE:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 45) {
                                    if (player.doneThachDauPoPo == 0) {
                                        this.createOtherMenu(player, ConstNpc.THACH_DAU_POPO, "Pôpô là đệ tử của ta, luyện tập với Pôpô con sẽ có thêm nhiều kinh nghiệm đánh bại được Pôpô ta sẽ dạy võ công cho con", "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nMr.Pôpô", "Thách đấu\nMr.Pôpô", "Quay ngọc\nmay mắn");
                                    }
                                    if (player.doneThachDauPoPo == 1 && player.doneThachDauThuongDe == 0) {
                                        this.createOtherMenu(player, ConstNpc.THACH_DAU_THUONG_DE, "Từ nay con sẽ là đệ tử của ta. Ta sẽ truyền cho con tất cả tuyệt kĩ", "Đăng ký\ntập\nTự động", "Tập luyện\nvới\nThượng Đế", "Thách đấu\nThượng Đế", "Quay ngọc\nMay mắn");
                                    }
                                    if (player.doneThachDauPoPo == 1 && player.doneThachDauThuongDe == 1) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Con đã mạnh hơn ta, ta sẽ chỉ đường cho con đến Kaio để gặp thần Vũ\nTrụ Phương Bắc\nNgài là thần cai quản vũ trụ này, hãy theo ngài ấy học võ công",
                                                "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nMr.PôPô", "Tập luyện\nvới\nThượng Đế", "Đến\nKaio", "Quay ngọc\nMay mắn");
                                    }
                                } else if (player.zone instanceof ZSnakeRoad) {
                                    if (mapId == ConstMap.CON_DUONG_RAN_DOC) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Hãy lắm lấy tay ta mau",
                                                "Về thần điện");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            Boss boss = BossManager.gI().getBossById(Util.createIdDuongTank((int) player.id) - 200000);
                            if (canOpenNpc(player)) {
                                if (this.mapId == 45) {
                                    switch (player.iDMark.getIndexMenu()) {
                                        case ConstNpc.BASE_MENU:
                                            switch (select) {
                                                case 0: // Đăng ký tập tự động

                                                    break;
                                                case 1: // luyện tập với popo
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.thachDauNPC = 1;
                                                    break;
                                                case 2: // tập luyện với thượng đế
                                                    ChangeMapService.gI().changeMap(player, 49, 0, 384, 440);
                                                    try {
                                                        Boss_ThuongDe dt = new Boss_ThuongDe(Util.createIdDuongTank((int) player.id), BossData.THUONG_DE, player.zone, this.cx, this.cy, (int) player.id);
                                                    } catch (Exception ex) {
                                                        Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.zone.load_Me_To_Another(player);
                                                    break;
                                                case 3: // Đến kaio
                                                    ChangeMapService.gI().changeMapBySpaceShip(player, 48, -1, 354);
                                                    break;
                                                case 4:// Vòng quay may mắn
                                                    this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                                            "Con có thể chọn từ 1 đến 7 viên\n"
                                                            + "giá mỗi viên là 5k hồng ngọc.\n"
                                                            + "Ưu tiên dùng vé quay trước.", "Rương",
                                                            "Rương phụ\nĐang có "
                                                            + (player.inventory.itemsBoxCrackBall.size()
                                                            - InventoryService.gI().getCountEmptyListItem(
                                                                    player.inventory.itemsBoxCrackBall))
                                                            + "\nmón", "Đóng");
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.MENU_CHOOSE_LUCKY_ROUND:
                                            switch (select) {
                                                case 0:
                                                    LuckyRoundService.gI().openCrackBallUI(player,
                                                            LuckyRoundService.USING_GEM);
                                                    break;
                                                case 1:
                                                    ShopService.gI().openBoxItemLuckyRound(player);
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.THACH_DAU_POPO:
                                            switch (select) {
                                                case 0:// đăng ký tập luyện tự động

                                                    break;
                                                case 1:// luyện tập pôpô
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.thachDauNPC = 1;
                                                    break;
                                                case 2:// thách đầu pôpô
                                                    player.thachDauNPC = 1;
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.thachDauNPC = 1;
                                                    break;
                                                case 3:
                                                    this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                                            "Con có thể chọn từ 1 đến 7 viên\n"
                                                            + "giá mỗi viên là 4 ngọc hoặc 5 triệu vàng.\n"
                                                            + "Ưu tiên dùng vé quay trước.", "Vòng quay\nHồng ngọc", "Rương",
                                                            "Rương phụ\nĐang có "
                                                            + (player.inventory.itemsBoxCrackBall.size()
                                                            - InventoryService.gI().getCountEmptyListItem(
                                                                    player.inventory.itemsBoxCrackBall))
                                                            + "\nmón", "Đóng");
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.THACH_DAU_THUONG_DE:
                                            switch (select) {
                                                case 0:
                                                    break;
                                                case 1:
                                                    ChangeMapService.gI().changeMap(player, 49, 0, 384, 440);
                                                    try {
                                                        Boss_ThuongDe dt = new Boss_ThuongDe(Util.createIdDuongTank((int) player.id), BossData.THUONG_DE, player.zone, this.cx, this.cy, (int) player.id);
                                                    } catch (Exception ex) {
                                                        Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.zone.load_Me_To_Another(player);
                                                    break;
                                                case 2:
                                                    ChangeMapService.gI().changeMap(player, 49, 0, 384, 440);
                                                    player.thachDauNPC = 1;
                                                    try {
                                                        Boss_ThuongDe dt = new Boss_ThuongDe(Util.createIdDuongTank((int) player.id), BossData.THUONG_DE, player.zone, this.cx, this.cy, (int) player.id);
                                                    } catch (Exception ex) {
                                                        Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.zone.load_Me_To_Another(player);
                                                    break;
                                                case 3:
                                                    this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                                            "Con có thể chọn từ 1 đến 7 viên\n"
                                                            + "giá mỗi viên là 4 ngọc hoặc 5 triệu vàng.\n"
                                                            + "Ưu tiên dùng vé quay trước.", "Vòng quay\nVàng", "Rương",
                                                            "Rương phụ\nĐang có "
                                                            + (player.inventory.itemsBoxCrackBall.size()
                                                            - InventoryService.gI().getCountEmptyListItem(
                                                                    player.inventory.itemsBoxCrackBall))
                                                            + "\nmón", "Đóng");
                                                    break;
                                            }
                                            break;
                                    }
                                } else if (player.zone instanceof ZSnakeRoad) {
                                    if (mapId == ConstMap.CON_DUONG_RAN_DOC) {
                                        ZSnakeRoad zroad = (ZSnakeRoad) player.zone;
                                        if (zroad.isKilledAll()) {
                                            SnakeRoad road = (SnakeRoad) zroad.getDungeon();
                                            ZSnakeRoad egr = (ZSnakeRoad) road.find(ConstMap.THAN_DIEN);
                                            egr.enter(player, 360, 408);
                                            Service.getInstance().sendThongBao(player, "Hãy xuống gặp thần mèo Karin");
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Hãy tiêu diệt hết quái vật ở đây!");
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.THAN_VU_TRU:

                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 48) {
                                    if (player.doneThachDauBubbles == 0) {
                                        this.createOtherMenu(player, ConstNpc.THACH_DAU_BUBBLES, "Thượng đế đưa ngươi đến đây, chắc muốn ta dạy võ chứ gì\n"
                                                + "Bắt được con khỉ Bubbles rồi hãy tính", "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nBubbles", "Thách đấu\nBubbles", "Di chuyển");
                                    } else if (player.doneThachDauThanVuTru == 0) {
                                        this.createOtherMenu(player, ConstNpc.THACH_DAU_THAN_VU_TRU, "Ta là Thần Vũ Trụ Phương Bắc cai quản khu vực bắc vũ trụ\n"
                                                + "nếu thắng được ta, ngươi sẽ được đến\n"
                                                + "Lành Đại Kaio, nơi ở của Thần Linh", "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nThần Vũ Trụ", "Thách đấu\nThần Vũ Trụ", "Di chuyển");
                                    } else {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Con mạnh nhất phía bắc vũ trụ này rồi đấy\n"
                                                + "nhưng ngoài vũ trụ bao la kia vẫn có những kẻ mạnh hơn nhìu\n"
                                                + "con cần phải luyện tập để mạnh hơn nữa", "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nBubbles", "Tập luyện\nvới\nThần Vũ Trụ", "Di chuyển");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            Boss boss = BossManager.gI().getBossById(Util.createIdDuongTank((int) player.id) - 250000);
                            if (canOpenNpc(player)) {
                                if (this.mapId == 48) {
                                    switch (player.iDMark.getIndexMenu()) {
                                        case ConstNpc.BASE_MENU:
                                            switch (select) {
                                                case 0:// tập tự động

                                                    break;
                                                case 1:// tập luyện với bubbles

                                                case 2:// tập luyện với thần vũ trụ

                                                    break;
                                                case 3:
                                                    this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                                            "Con muốn đi đâu?", "Về\nthần điện", "Thánh địa\nKaio",
                                                            "Con\nđường\nrắn độc", "Từ chối");
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.THACH_DAU_BUBBLES:
                                            switch (select) {
                                                case 0:// tập tự động

                                                    break;
                                                case 1:// tập luyện với bubbles
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.thachDauNPC = 1;

                                                    break;
                                                case 2:// thách đấu với bubble
                                                    player.activeYajiro = 1;
                                                    PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.thachDauNPC = 1;

                                                    break;
                                                case 3:
                                                    this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                                            "Con muốn đi đâu?", "Về\nthần điện", "Thánh địa\nKaio",
                                                            "Con\nđường\nrắn độc", "Từ chối");
                                                    break;
                                            }
                                            break;

                                        case ConstNpc.THACH_DAU_THAN_VU_TRU:
                                            switch (select) {
                                                case 0:// tập tự động
                                                    break;
                                                case 1:// tập luyện với kingkai
                                                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                                                    Runnable task = () -> {
                                                        hide_npc(player, 20, 0);
                                                        scheduler.shutdown();
                                                    };
                                                    scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                                    try {
                                                        Boss_Kingkai dt = new Boss_Kingkai(Util.createIdDuongTank((int) ((byte) player.id)), BossData.KINGKAI, player.zone, this.cx, this.cy, (int) player.id);
                                                    } catch (Exception ex) {
                                                        Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                    PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                    player.zone.load_Me_To_Another(player);

                                                    break;
                                                case 2:// tập luyện với thần vũ trụ

                                                    break;
                                                case 3:
                                                    this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                                            "Con muốn đi đâu?", "Về\nthần điện", "Thánh địa\nKaio",
                                                            "Con\nđường\nrắn độc", "Từ chối");
                                                    break;
                                            }
                                            break;

                                        case ConstNpc.MENU_DI_CHUYEN:
                                            switch (select) {
                                                case 0:
                                                    ChangeMapService.gI().changeMapBySpaceShip(player, 45, -1, 354);
                                                    break;
                                                case 1:
                                                    ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                                                    break;
                                                case 2:
                                                    // con đường rắn độc
                                                    if (player.clan != null) {
                                                        Calendar calendar = Calendar.getInstance();
                                                        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                                                        if (!(dayOfWeek == Calendar.MONDAY
                                                                || dayOfWeek == Calendar.WEDNESDAY
                                                                || dayOfWeek == Calendar.FRIDAY
                                                                || dayOfWeek == Calendar.SUNDAY)) {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Chỉ mở vào thứ 2, 4, 6, CN hàng tuần!");
                                                            return;
                                                        }
                                                        if (player.clanMember.getNumDateFromJoinTimeToToday() < 2) {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Phải tham gia bang hội ít nhất 2 ngày mới có thể tham gia!");
                                                            return;
                                                        }
                                                        if (player.clan.snakeRoad == null) {
                                                            this.createOtherMenu(player, ConstNpc.MENU_CHON_CAP_DO,
                                                                    "Hãy mau trở về bằng con đường rắn độc\nbọn Xayda đã đến Trái Đất",
                                                                    "Chọn\ncấp độ", "Từ chối");
                                                        } else {
                                                            if (player.clan.snakeRoad.isClosed()) {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Bang hội đã hết lượt tham gia!");
                                                            } else {
                                                                this.createOtherMenu(player,
                                                                        ConstNpc.MENU_ACCEPT_GO_TO_CDRD,
                                                                        "Con có chắc chắn muốn đến con đường rắn độc cấp độ "
                                                                        + player.clan.snakeRoad.getLevel() + "?",
                                                                        "Đồng ý", "Từ chối");
                                                            }
                                                        }
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Chỉ dành cho những người trong bang hội!");
                                                    }
                                                    break;

                                            }
                                            break;
                                        case ConstNpc.MENU_ACCEPT_GO_TO_CDRD:
                                            switch (select) {
                                                case 0:
                                                    if (player.clan != null) {
                                                        synchronized (player.clan) {
                                                            if (player.clan.snakeRoad == null) {
                                                                int level = Byte.parseByte(
                                                                        String.valueOf(PLAYERID_OBJECT.get(player.id)));
                                                                SnakeRoad road = new SnakeRoad(level);
                                                                ServerManager.gI().getDungeonManager().addDungeon(road);
                                                                road.join(player);
                                                                player.clan.snakeRoad = road;
                                                            } else {
                                                                player.clan.snakeRoad.join(player);
                                                            }
                                                        }
                                                    }
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.MENU_CHON_CAP_DO:
                                            switch (select) {
                                                case 0:
                                                    Input.gI().createFormChooseLevelCDRD(player);
                                                    break;
                                            }
                                            break;
                                    }
                                }
                            }
                        }

                    };
                    break;
                case ConstNpc.TO_SU_KAIO:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 50) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Con muốn nâng giới hạn sức mạnh cho đệ tử?", "Đệ tử", "Đóng",
                                            "Từ chối");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 50) {
                                    switch (select) {
                                        case 0:
                                            if (player.pet != null) {
                                                if (player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                                                    this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                                            "Ta sẽ truền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                                            + Util.numberToMoney(
                                                                    player.pet.nPoint.getPowerNextLimit()),
                                                            "Nâng ngay\n" + Util.numberToMoney(
                                                                    OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER)
                                                            + " vàng",
                                                            "Đóng");
                                                } else {
                                                    this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                                            "Sức mạnh của đệ con đã đạt tới giới hạn", "Đóng");
                                                }
                                            } else {
                                                Service.getInstance().sendThongBao(player, "Không thể thực hiện");
                                            }
                                            break;
                                        case 1:
                                            //                           CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.UPGRADE_THAN_LINH);
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.OPEN_POWER_PET) {
                                    if (select == 0) {
                                        if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                                            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                                                player.inventory.subGold(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER); // FIX: dùng method an toàn
                                                Service.getInstance().sendMoney(player);
                                            }
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Bạn không đủ vàng để mở, còn thiếu " + Util
                                                            .numberToMoney((OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER
                                                                    - player.inventory.gold))
                                                    + " vàng");
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.DOC_NHAN:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 57) {
                                    if (player.zone.isCheckKilledAll(57) && !player.clan.doanhTrai.isHaveDoneDoanhTrai) {
                                        player.clan.doanhTrai.isHaveDoneDoanhTrai = true;
                                        player.clan.doanhTrai.lastTimeDoneDoanhTrai = System.currentTimeMillis();
                                        player.clan.doanhTrai.DropNgocRong();
                                        Service.getInstance().sendThongBao(player, "Trại Độc Nhãn đã bị tiêu diệt, bạn có 5 phút để tìm kiếm viên ngọc rồng 4 sao trước khi phi thuyền đến đón");
                                        NpcService.gI().createTutorial(player, avartar, "Ta chịu thua, nhưng các ngươi đừng có mong lấy được ngọc của ta\b"
                                                + "ta đã giấu ngọc 4 sao và 1 đống 7 sao trong doanh trại này\b"
                                                + "Các ngươi chỉ có 5 phút đi tìm, đố các ngươi tìm ra hahaha");
                                    } else {
                                        NpcService.gI().createTutorial(player, avartar, "hãy tiêu diệt hết quái");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                        }
                    };
                    break;
                case ConstNpc.KIBIT:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 50) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                                            "Đến\nKaio", "Từ chối");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 50) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMap(player, 48, -1, 354, 240);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.TRONG_TAI:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 113) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Đại hội võ thuật Siêu hạng\ndiễn ra 24/7 kể cả ngày lễ và chủ nhật\nHãy thi đấu ngay để khẳng định đẳng cấp của mình nhé",
                                            "Top 100\nCao thủ",
                                            "Hướng\ndẫn\nthêm",
                                            "Ưu tiên\nđấu ngay",
                                            "Tạo bản sao siêu hạng",
                                            "Về\nĐại Hội\nVõ Thuật");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 113) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                SieuHangService.ShowTop(player, 0);
                                                break;
                                            case 1:
                                                NpcService.gI().createTutorial(player, -1,
                                                        "Giải đấu thể hiện đẳng cấp thực sự\bCác trận đấu diễn ra liên tục bất kể ngày đêm\bBạn hãy tham gia thi đấu để nâng hạng\bvà nhận giải thưởng khủng nhé\nCơ cấu giải thưởng như sau\b(chốt và trao giải ngẫu nhiên từ 20h-23h mỗi ngày)\bTop 1 thưởng 100 ngọc\bTop 2-10 thưởng 20 ngọc\bTop 11-100 thưởng 5 ngọc\bTop 101-1000 thưởng 1 ngọc\nMỗi ngày các bạn được tặng 1 vé tham dự miễn phí\b(tích lũy tối đa 3 vé) khi thua sẽ mất đi 1 vé\bKhi hết vé bạn phải trả 1 ngọc để đấu tiếp\b(trừ ngọc khi trận đấu kết thúc)\nBạn không thể thi đấu với đấu thủ\bcó hạng thấp hơn mình\bChúc bạn may mắn, chào đoàn kết và quyết thắng");
                                                break;
                                            case 2:
                                                SieuHangService.ShowTop(player, 1);
                                                break;
                                            case 3:
                                                SieuHangService.ShowTop(player, 1);
                                                break;
                                            case 5:
                                                ChangeMapService.gI().changeMapNonSpaceship(player, ConstMap.DAI_HOI_VO_THUAT, player.location.x, 336);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.OSIN:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 50) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                                            "Đến\nKaio", "Đến\nhành tinh\nBill", "Từ chối");
                                } else if (this.mapId == 52) {
                                    if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                        if (MabuWar.gI().isTimeMabuWar() || MabuWar14h.gI().isTimeMabuWar()) {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                    "Bây giờ tôi sẽ bí mật...\n đuổi theo 2 tên đồ tể... \n"
                                                    + "Quý vị nào muốn đi theo thì xin mời !",
                                                    "Ok", "Chê");
                                        } else {
                                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                    "Vào lúc 12h tôi sẽ bí mật...\n đuổi theo 2 tên đồ tể... \n"
                                                    + "Quý vị nào muốn đi theo thì xin mời !",
                                                    "Ok");
                                        }
                                    }
                                } else if (this.mapId == 154) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                                            "Về thánh địa", "Đến\nhành tinh\nngục tù", "Từ chối");
                                } else if (this.mapId == 155) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                                            "Quay về", "Từ chối");
                                } else if (MapService.gI().isMapMabuWar(this.mapId)) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Đừng vội xem thường Babyđây,ngay đến cha hắn là thần ma đạo sĩ\n"
                                            + "Bibiđây khi còn sống cũng phải sợ hắn đấy",
                                            "Giải trừ\nphép thuật\n50Tr Vàng",
                                            player.zone.map.mapId != 120 ? "Xuống\nTầng Dưới" : "Rời\nKhỏi đây");
                                } else if (MapService.gI().isMapMabuWar(this.mapId)) {
                                    if (MabuWar.gI().isTimeMabuWar()) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                                "Đừng vội xem thường Babyđây,ngay đến cha hắn là thần ma đạo sĩ\n"
                                                + "Bibiđây khi còn sống cũng phải sợ hắn đấy",
                                                "Giải trừ\nphép thuật\n50Tr Vàng",
                                                player.zone.map.mapId != 120 ? "Xuống\nTầng Dưới" : "Rời\nKhỏi đây");
                                    } else if (MabuWar14h.gI().isTimeMabuWar()) {
                                        createOtherMenu(player, ConstNpc.BASE_MENU, "Ta sẽ phù hộ cho ngươi bằng nguồn sức mạnh của Thần Kaiô\n+1 triệu HP, +1 triệu MP, +10k Sức đánh\nLưu ý: sức mạnh sẽ biến mất khi ngươi rời khỏi đây", "Phù hộ\n55 hồng ngọc", "Từ chối", "Về\nĐại Hội\nVõ Thuật");
                                    }
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 50) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMap(player, 48, -1, 354, 240);
                                                break;
                                            case 1:
                                                ChangeMapService.gI().changeMap(player, 154, -1, 200, 312);
                                                break;
                                        }
                                    }
                                } else if (this.mapId == 52) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                if (MabuWar.gI().isTimeMabuWar() || MabuWar14h.gI().isTimeMabuWar()) {
                                                    ChangeMapService.gI().changeMap(player, 114, -1, 354, 240);
                                                    break;
                                                }
                                        }
                                    }
                                } else if (this.mapId == 154) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                                                break;
                                            case 1:
                                                if (!Manager.gI().getGameConfig().isOpenPrisonPlanet()) {
                                                    Service.getInstance().sendThongBao(player,
                                                            "Lối vào hành tinh ngục tù chưa mở");
                                                    return;
                                                }
                                                if (player.nPoint.power < 60000000000L) {
                                                    Service.getInstance().sendThongBao(player,
                                                            "Yêu cầu tối thiếu 60tỷ sức mạnh");
                                                    return;
                                                }
                                                ChangeMapService.gI().changeMap(player, 155, -1, 111, 792);
                                                break;
                                        }
                                    }
                                } else if (this.mapId == 155) {
                                    if (player.iDMark.isBaseMenu()) {
                                        if (select == 0) {
                                            ChangeMapService.gI().changeMap(player, 154, -1, 200, 312);
                                        }
                                    }
                                } else if (MapService.gI().isMapMabuWar(this.mapId)) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                if (player.inventory.getGold() >= 50000000) {
                                                    Service.getInstance().changeFlag(player, 9);
                                                    player.inventory.subGold(50000000);

                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Không đủ vàng");
                                                }
                                                break;
                                            case 1:
                                                if (player.zone.map.mapId == 120) {
                                                    ChangeMapService.gI().changeMapBySpaceShip(player,
                                                            player.gender + 21, -1, 250);
                                                }
                                                if (player.cFlag == 9) {
                                                    if (player.getPowerPoint() >= 20) {
                                                        if (!(player.zone.map.mapId == 119)) {
                                                            int idMapNextFloor = player.zone.map.mapId == 115
                                                                    ? player.zone.map.mapId + 2
                                                                    : player.zone.map.mapId + 1;
                                                            ChangeMapService.gI().changeMap(player, idMapNextFloor, -1,
                                                                    354, 240);
                                                        } else {
                                                            Zone zone = MabuWar.gI().getMapLastFloor(120);
                                                            if (zone != null) {
                                                                ChangeMapService.gI().changeMap(player, zone, 354, 240);
                                                            } else {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Trận đại chiến đã kết thúc, tàu vận chuyển sẽ đưa bạn về nhà");
                                                            }
                                                        }
                                                        player.resetPowerPoint();
                                                        player.sendMenuGotoNextFloorMabuWar = false;
                                                        Service.getInstance().sendPowerInfo(player, "%",
                                                                player.getPowerPoint());
                                                        if (Util.isTrue(1, 30)) {
                                                            player.inventory.addRuby(1); // FIX
                                                            PlayerService.gI().sendInfoHpMpMoney(player);
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Bạn nhận được 1 Hồng Ngọc");
                                                        } else {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Bạn đen vô cùng luôn nên không nhận được gì cả");
                                                        }
                                                    } else {
                                                        this.npcChat(player,
                                                                "Ngươi cần có đủ điểm để xuống tầng tiếp theo");
                                                    }
                                                    break;
                                                } else {
                                                    this.npcChat(player,
                                                            "Ngươi đang theo phe Babiđây,Hãy qua bên đó mà thể hiện");
                                                }
                                        }
                                    } else if (MabuWar14h.gI().isTimeMabuWar()) {
                                        switch (select) {
                                            case 0:
                                                if (player.effectSkin.isPhuHo) {
                                                    this.npcChat("Con đã mang trong mình sức mạnh của thần Kaiô!");
                                                    return;
                                                }
                                                if (player.inventory.ruby < 55) {
                                                    Service.getInstance().sendThongBao(player, "Bạn không đủ hồng ngọc");
                                                } else {
                                                    player.inventory.subRuby(55); // FIX
                                                    player.effectSkin.isPhuHo = true;
                                                    Service.getInstance().point(player);
                                                    this.npcChat("Ta đã phù hộ cho con hãy giúp ta tiêu diệt Mabư!");
                                                }
                                                break;
                                            case 2:
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 52, -1, 250);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.BABIDAY:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (MapService.gI().isMapMabuWar(this.mapId)) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Đừng vội xem thường Babyđây,ngay đến cha hắn là thần ma đạo sĩ\n"
                                            + "Bibiđây khi còn sống cũng phải sợ hắn đấy",
                                            "Yểm bùa\n50Tr Vàng",
                                            player.zone.map.mapId != 120 ? "Xuống\nTầng Dưới" : "Rời\nKhỏi đây");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (MapService.gI().isMapMabuWar(this.mapId)) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                if (player.inventory.getGold() >= 50000000) {
                                                    Service.getInstance().changeFlag(player, 10);
                                                    player.inventory.subGold(50000000);
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Không đủ vàng");
                                                }
                                                break;
                                            case 1:
                                                if (player.zone.map.mapId == 120) {
                                                    ChangeMapService.gI().changeMapBySpaceShip(player,
                                                            player.gender + 21, -1, 250);
                                                }
                                                if (player.cFlag == 10) {
                                                    if (player.getPowerPoint() >= 20) {
                                                        if (!(player.zone.map.mapId == 119)) {
                                                            int idMapNextFloor = player.zone.map.mapId == 115
                                                                    ? player.zone.map.mapId + 2
                                                                    : player.zone.map.mapId + 1;
                                                            ChangeMapService.gI().changeMap(player, idMapNextFloor, -1,
                                                                    354, 240);
                                                        } else {
                                                            Zone zone = MabuWar.gI().getMapLastFloor(120);
                                                            if (zone != null) {
                                                                ChangeMapService.gI().changeMap(player, zone, 354, 240);
                                                            } else {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Trận đại chiến đã kết thúc, tàu vận chuyển sẽ đưa bạn về nhà");
                                                                ChangeMapService.gI().changeMapBySpaceShip(player,
                                                                        player.gender + 21, -1, 250);
                                                            }
                                                        }
                                                        player.resetPowerPoint();
                                                        player.sendMenuGotoNextFloorMabuWar = false;
                                                        Service.getInstance().sendPowerInfo(player, "TL",
                                                                player.getPowerPoint());
                                                        if (Util.isTrue(1, 30)) {
                                                            player.inventory.addRuby(1); // FIX
                                                            PlayerService.gI().sendInfoHpMpMoney(player);
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Bạn nhận được 1 Hồng Ngọc");
                                                        } else {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Bạn đen vô cùng luôn nên không nhận được gì cả");
                                                        }
                                                    } else {
                                                        this.npcChat(player,
                                                                "Ngươi cần có đủ điểm để xuống tầng tiếp theo");
                                                    }
                                                    break;
                                                } else {
                                                    this.npcChat(player,
                                                            "Ngươi đang theo phe Ôsin,Hãy qua bên đó mà thể hiện");
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.LINH_CANH:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (player.clan == null) {
                                    this.createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                            "Chỉ tiếp các bang hội, miễn tiếp khách vãng lai", "Đóng");
                                } else if (player.clan.getMembers().size() < 5) {

                                    NpcService.gI().createTutorial(player, avartar, "Bang hội phải có từ 5 thành viên mới được tham gia");
                                } else {
                                    ClanMember clanMember = player.clan.getClanMember((int) player.id);
                                    int days = (int) (((System.currentTimeMillis() / 1000) - clanMember.joinTime) / 60
                                            / 60 / 24);
                                    if (days < 2) {
                                        NpcService.gI().createTutorial(player, avartar,
                                                "Chỉ những thành viên gia nhập bang hội tối thiểu 2 ngày mới có thể tham gia");
                                        return;
                                    }
                                    if (!player.clan.haveGoneDoanhTrai && player.clan.timeOpenDoanhTrai != 0) {
                                        createOtherMenu(player, ConstNpc.MENU_VAO_DT,
                                                "Bang hội của ngươi đang đánh trại độc nhãn\n" + "Thời gian còn lại là "
                                                + TimeUtil.chuyenDoiTuGiaySangPhut(TimeUtil.getSecondLeft(player.clan.timeOpenDoanhTrai,
                                                        DoanhTrai.TIME_DOANH_TRAI / 1000))
                                                + ". Ngươi có muốn tham gia không?",
                                                "Tham gia", "Không", "Hướng\ndẫn\nthêm");
                                    } else {
                                        List<Player> plSameClans = new ArrayList<>();
                                        List<Player> playersMap = player.zone.getPlayers();
                                        synchronized (playersMap) {
                                            for (Player pl : playersMap) {
                                                if (!pl.equals(player) && pl.clan != null
                                                        && pl.clan.id == player.clan.id && pl.location.x >= 1285
                                                        && pl.location.x <= 1645) {
                                                    plSameClans.add(pl);
                                                }

                                            }
                                        }
                                        if (plSameClans.size() >= 2) {
                                            if (!player.isAdmin() && player.clanMember.getNumDateFromJoinTimeToToday() < DoanhTrai.DATE_WAIT_FROM_JOIN_CLAN) {
                                                createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                                        "Bang hội chỉ cho phép những người ở trong bang trên 1 ngày. Hẹn ngươi quay lại vào lúc khác",
                                                        "OK", "Hướng\ndẫn\nthêm");
                                            } else if (player.clan.haveGoneDoanhTrai) {
                                                createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                                        "Bang hội của ngươi đã đi trại lúc "
                                                        + Util.formatTime(player.clan.timeOpenDoanhTrai)
                                                        + " hôm nay. Người mở\n" + "("
                                                        + player.clan.playerOpenDoanhTrai.name
                                                        + "). Hẹn ngươi quay lại vào ngày mai",
                                                        "OK", "Hướng\ndẫn\nthêm");

                                            } else {
                                                createOtherMenu(player, ConstNpc.MENU_CHO_VAO_DT,
                                                        "Hôm nay bang hội của ngươi chưa vào trại lần nào. Ngươi có muốn vào\n"
                                                        + "không?\nĐể vào, ta khuyên ngươi nên có 3-4 người cùng bang đi cùng",
                                                        "Vào\n(miễn phí)", "Không", "Hướng\ndẫn\nthêm");
                                            }
                                        } else {
                                            createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                                    "Ngươi phải có ít nhất 2 đồng đội cùng bang đứng gần mới có thể vào "
                                                    + "tuy nhiên ta khuyên ngươi nên đi cùng với 3-4 người để khỏi chết.\n"
                                                    + "Hahaha.",
                                                    "OK", "Hướng\ndẫn\nthêm");
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 27) {
                                    switch (player.iDMark.getIndexMenu()) {
                                        case ConstNpc.MENU_KHONG_CHO_VAO_DT:
                                            if (select == 1) {
                                                NpcService.gI().createTutorial(player, this.avartar,
                                                        ConstNpc.HUONG_DAN_DOANH_TRAI);
                                            }
                                            break;
                                        case ConstNpc.MENU_CHO_VAO_DT:
                                            switch (select) {
                                                case 0:
                                                    DoanhTraiService.gI().openDoanhTrai(player);
                                                    break;
                                                case 2:
                                                    NpcService.gI().createTutorial(player, this.avartar,
                                                            ConstNpc.HUONG_DAN_DOANH_TRAI);
                                                    break;
                                            }
                                            break;
                                        case ConstNpc.MENU_VAO_DT:
                                            switch (select) {
                                                case 0:
                                                    ChangeMapService.gI().changeMap(player, 53, 0, 35, 432);
                                                    break;
                                                case 2:
                                                    NpcService.gI().createTutorial(player, this.avartar,
                                                            ConstNpc.HUONG_DAN_DOANH_TRAI);
                                                    break;
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.QUA_TRUNG:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        private final int COST_AP_TRUNG_NHANH = 1000000000;

                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                player.mabuEgg.sendMabuEgg();
                                if (player.mabuEgg.getSecondDone() != 0) {
                                    this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG, "Hãy thu thập năng lượng\nbằng cách làm nhiệm vụ hằng ngày\ntại Bò Mộng từ mức độ khó trở lên\nđể trứng mau nở nhé.",
                                            "Nở trứng\nnhanh\n1 tỷ vàng", "Hủy bỏ\ntrứng", "Đóng");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG, "Hãy thu thập năng lượng\nbằng cách làm nhiệm vụ hằng ngày\ntại Bò Mộng từ mức độ khó trở lên\nđể trứng mau nở nhé.", "Nở",
                                            "Hủy bỏ\ntrứng", "Đóng");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.CAN_NOT_OPEN_EGG:
                                        switch (select) {
                                            case 0:
                                                player.mabuEgg.timeDone = 0;
                                                player.inventory.subGold(1000000000); // FIX: dùng method an toàn
                                                Service.getInstance().sendMoney(player);
                                                Service.getInstance().sendThongBao(player, "Đã nở trứng nhanh thành công");
                                                break;
                                            case 1:
                                                this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                                        "Bạn có chắc chắn muốn hủy bỏ trứng Mabư?", "Đồng ý", "Từ chối");
                                                break;
                                        }
                                        break;
                                    case ConstNpc.CAN_OPEN_EGG:
                                        switch (select) {
                                            case 0:
                                                this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                                        "Bạn có chắc thay thế đệ tử hiện tại bằng Đệ tử Mabư",
                                                        "Thay thế", "Từ chối");
                                                break;
                                            case 1:
                                                this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                                        "Bạn có chắc chắn muốn hủy bỏ trứng Mabư?", "Đồng ý",
                                                        "Từ chối");
                                                break;
                                        }
                                        break;
                                    case ConstNpc.CONFIRM_OPEN_EGG:
                                        switch (select) {
                                            case 0:
                                                player.mabuEgg.openEgg(player.gender);
                                                break;
                                            default:
                                                break;
                                        }
                                        break;
                                    case ConstNpc.CONFIRM_DESTROY_EGG:
                                        if (select == 0) {
                                            player.mabuEgg.destroyEgg();
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.QUOC_VUONG:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?", "Bản thân", "Đệ tử",
                                    "Từ chối");
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    switch (select) {
                                        case 0:
                                            if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                                                this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                                                        "Ta sẽ truền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                                                        + Util.numberToMoney(player.nPoint.getPowerNextLimit()),
                                                        "Nâng\ngiới hạn\nsức mạnh",
                                                        "Nâng ngay\n"
                                                        + Util.numberToMoney(
                                                                OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER)
                                                        + " vàng",
                                                        "Đóng");
                                            } else {
                                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                                        "Sức mạnh của con đã đạt tới giới hạn", "Đóng");
                                            }
                                            break;
                                        case 1:
                                            if (player.pet != null) {
                                                if (player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                                                    this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                                            "Ta sẽ truền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                                            + Util.numberToMoney(
                                                                    player.pet.nPoint.getPowerNextLimit()),
                                                            "Nâng ngay\n" + Util.numberToMoney(
                                                                    OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER)
                                                            + " vàng",
                                                            "Đóng");
                                                } else {
                                                    this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                                            "Sức mạnh của đệ con đã đạt tới giới hạn", "Đóng");
                                                }
                                            } else {
                                                Service.getInstance().sendThongBao(player, "Không thể thực hiện");
                                            }
                                            // giới hạn đệ tử
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.OPEN_POWER_MYSEFT) {
                                    switch (select) {
                                        case 0:
                                            OpenPowerService.gI().openPowerBasic(player);
                                            break;
                                        case 1:
                                            if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                                                if (OpenPowerService.gI().openPowerSpeed(player)) {
                                                    player.inventory.subGold(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER); // FIX: dùng method an toàn
                                                    Service.getInstance().sendMoney(player);
                                                }
                                            } else {
                                                Service.getInstance().sendThongBao(player,
                                                        "Bạn không đủ vàng để mở, còn thiếu " + Util.numberToMoney(
                                                                (OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER
                                                                - player.inventory.gold))
                                                        + " vàng");
                                            }
                                            break;
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.OPEN_POWER_PET) {
                                    if (select == 0) {
                                        if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                                            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                                                player.inventory.subGold(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER); // FIX: dùng method an toàn
                                                Service.getInstance().sendMoney(player);
                                            }
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Bạn không đủ vàng để mở, còn thiếu " + Util
                                                            .numberToMoney((OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER
                                                                    - player.inventory.gold))
                                                    + " vàng");
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.BUNMA_TL:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Cậu bé muốn mua gì nào?",
                                            "Cửa hàng", "Đóng");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    if (select == 0) {
                                        ShopService.gI().openShopNormal(player, this, ConstNpc.SHOP_BUNMA_TL_0, 0,
                                                player.gender);
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.RONG_OMEGA:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                BlackBallWar.gI().setTime();
                                if (this.mapId == 24 || this.mapId == 25 || this.mapId == 26) {
                                    try {
                                        long now = System.currentTimeMillis();
                                        if (now > BlackBallWar.TIME_OPEN && now < BlackBallWar.TIME_CLOSE) {
                                            this.createOtherMenu(player, ConstNpc.MENU_OPEN_BDW,
                                                    "Đường đến với ngọc rồng sao đen đã mở, "
                                                    + "ngươi có muốn tham gia không?",
                                                    "Hướng dẫn\nthêm", "Tham gia", "Từ chối");
                                        } else {
                                            String[] optionRewards = new String[7];
                                            int index = 0;
                                            for (int i = 0; i < 7; i++) {
                                                if (player.rewardBlackBall.timeOutOfDateReward[i] > System
                                                        .currentTimeMillis()) {
                                                    optionRewards[index] = "Nhận thưởng\n" + (i + 1) + " sao";
                                                    index++;
                                                }
                                            }
                                            if (index != 0) {
                                                String[] options = new String[index + 1];
                                                for (int i = 0; i < index; i++) {
                                                    options[i] = optionRewards[i];
                                                }
                                                options[options.length - 1] = "Từ chối";
                                                this.createOtherMenu(player, ConstNpc.MENU_REWARD_BDW,
                                                        "Ngươi có một vài phần thưởng ngọc " + "rồng sao đen đây!",
                                                        options);
                                            } else {
                                                this.createOtherMenu(player, ConstNpc.MENU_NOT_OPEN_BDW,
                                                        "Ta có thể giúp gì cho ngươi?", "Hướng dẫn", "Từ chối");
                                            }
                                        }
                                    } catch (Exception ex) {
                                        Log.error("Lỗi mở menu rồng Omega");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.MENU_REWARD_BDW:
                                        player.rewardBlackBall.getRewardSelect((byte) select);
                                        break;
                                    case ConstNpc.MENU_OPEN_BDW:
                                        if (select == 0) {
                                            NpcService.gI().createTutorial(player, this.avartar,
                                                    ConstNpc.HUONG_DAN_BLACK_BALL_WAR);
                                        } else if (select == 1) {
                                            player.iDMark.setTypeChangeMap(ConstMap.CHANGE_BLACK_BALL);
                                            ChangeMapService.gI().openChangeMapTab(player);
                                        }
                                        break;
                                    case ConstNpc.MENU_NOT_OPEN_BDW:
                                        if (select == 0) {
                                            NpcService.gI().createTutorial(player, this.avartar,
                                                    ConstNpc.HUONG_DAN_BLACK_BALL_WAR);
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.RONG_1S:
                case ConstNpc.RONG_2S:
                case ConstNpc.RONG_3S:
                case ConstNpc.RONG_4S:
                case ConstNpc.RONG_5S:
                case ConstNpc.RONG_6S:
                case ConstNpc.RONG_7S:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (player.isHoldBlackBall) {
                                    this.createOtherMenu(player, ConstNpc.MENU_PHU_HP, "Ta có thể giúp gì cho ngươi?",
                                            "Phù hộ", "Từ chối");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.MENU_OPTION_GO_HOME,
                                            "Ta có thể giúp gì cho ngươi?", "Về nhà", "Từ chối");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.getIndexMenu() == ConstNpc.MENU_PHU_HP) {
                                    if (select == 0) {
                                        this.createOtherMenu(player, ConstNpc.MENU_OPTION_PHU_HP,
                                                "Ta sẽ giúp ngươi tăng HP lên mức kinh hoàng, ngươi chọn đi",
                                                "x3 HP\n" + Util.numberToMoney(BlackBallWar.COST_X3) + " vàng",
                                                "x5 HP\n" + Util.numberToMoney(BlackBallWar.COST_X5) + " vàng",
                                                "x7 HP\n" + Util.numberToMoney(BlackBallWar.COST_X7) + " vàng",
                                                "Từ chối");
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_GO_HOME) {
                                    if (select == 0) {
                                        ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                                    }
                                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_PHU_HP) {
                                    switch (select) {
                                        case 0:
                                            BlackBallWar.gI().xHPKI(player, BlackBallWar.X3);
                                            break;
                                        case 1:
                                            BlackBallWar.gI().xHPKI(player, BlackBallWar.X5);
                                            break;
                                        case 2:
                                            BlackBallWar.gI().xHPKI(player, BlackBallWar.X7);
                                            break;
                                        case 3:
                                            this.npcChat(player, "Để ta xem ngươi trụ được bao lâu");
                                            break;
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.NPC_64:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi muốn xem thông tin gì?",
                                        "Top\nsức mạnh", "Đóng");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (player.iDMark.isBaseMenu()) {
                                    if (select == 0) {
                                        Service.getInstance().showTopPower(player);
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.BILL:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {

                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 48) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Đói bụng quá...ngươi mang cho ta 99 phần đồ ăn,\n"
                                            + "ta sẽ cho ngươi một món đồ Hủy Diệt.\n"
                                            + "Nếu tâm trạng ta vui ngươi có thể nhận được trang bị tăng đến 15%", "OK", "Từ chối");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (this.mapId) {
                                    case 48:
                                        if (player.iDMark.isBaseMenu()) {
                                            switch (select) {
                                                case 0:
                                                    if (player.setClothes.godClothes) {
                                                        ShopService.gI().openShopBillHuyDiet(player, ConstNpc.SHOP_BILL_HUY_DIET_0, 0);
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Yêu cầu có đủ trang bị thần linh");
                                                    }
                                                    break;
                                            }
                                        }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.WHIS:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            switch (mapId) {
                                case 5:
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta là Whis được Đại thiên sứ cử xuống Trái đất để thu thập lại các trang bị Thần linh bị kẻ xấu đánh cắp. Ta sẽ bạn lại cho ngươi trang bị kích hoạt trong truyền thuyết nếu ngươi giao cho ta trang bị Thần linh.", "Hiến tế\nThần linh", "Hướng\ndẫn", "Đóng");
                                    return;
                                case 154:
                                    createOtherMenu(player, ConstNpc.BASE_MENU, "Thử đánh ta xem nào.\n"
                                            + "Ngươi còn 1 lượt nữa cơ mà.", "Chế Tạo", "Học\ntuyệt kỹ", "Top 100", "[LV:" + player.levelKillWhis + "]");
                                    return;
                            }

                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.BASE_MENU:
                                        switch (mapId) {
                                            case 154:
                                                switch (select) {
                                                    case 0:
                                                        CombineServiceNew.gI().openTabCombine(player, CombineServiceNew.NANG_CAP_DO_THIEN_SU);
                                                        break;
                                                    case 1: // Học tuyệt kỹ
                                                        Item biKipTuyetKy = InventoryService.gI().findItemBagByTemp(player, (short) 1229);
                                                        if (biKipTuyetKy != null && biKipTuyetKy.quantity >= 9999 && player.inventory.gold >= 10_000_000 && player.inventory.gem >= 99) {
                                                            int skillID = player.gender == 0 ? 24 : player.gender == 1 ? 26 : 25;
                                                            Skill newSkill = SkillUtil.createSkill(skillID, 1);
                                                            String npcSay = "|1|Qua sẽ dạy ngươi tuyệt kỹ " + newSkill.template.name + "\n";
                                                            npcSay += "|2|" + biKipTuyetKy.getName() + " " + biKipTuyetKy.quantity + "/9999\n";
                                                            npcSay += "Giá vàng: 10.000.000\n";
                                                            npcSay += "Giá ngọc: 99";
                                                            createOtherMenu(player, ConstNpc.HOC_TUYET_KY, npcSay, "Đồng ý", "Từ chối");
                                                            return;
                                                        } else {
                                                            int skillID = player.gender == 0 ? 24 : player.gender == 1 ? 26 : 25;
                                                            Skill newSkill = SkillUtil.createSkill(skillID, 1);
                                                            String npcSay = "|1|Qua sẽ dạy ngươi tuyệt kỹ " + newSkill.template.name + " 1\n";
                                                            if (biKipTuyetKy == null || biKipTuyetKy.quantity < 9999) {
                                                                if (biKipTuyetKy == null) {
                                                                    npcSay += "|7|Bí kíp tuyệt kỹ" + " " + "0/9999\n";
                                                                } else {
                                                                    npcSay += "|7|Bí kíp tuyệt kỹ" + " " + biKipTuyetKy.quantity + "/9999\n";
                                                                }
                                                            } else {
                                                                npcSay += "|2|" + biKipTuyetKy.getName() + " " + biKipTuyetKy.quantity + "/9999\n";
                                                            }
                                                            if (player.inventory.gold < 10_000_000) {
                                                                npcSay += "|7|Giá vàng: 10.000.000\n";
                                                            } else {
                                                                npcSay += "|2|Giá vàng: 10.000.000\n";
                                                            }
                                                            if (player.inventory.gem < 99) {
                                                                npcSay += "|7|Giá ngọc: 99";
                                                            } else {
                                                                npcSay += "|2|Giá ngọc: 99";
                                                            }
                                                            createOtherMenu(player, ConstNpc.HOC_TUYET_KY_2, npcSay, "Từ chối");
                                                            return;
                                                        }

                                                    case 2: // Top đánh NPC whis
                                                        Service.getInstance().showToplevelWhis(player);
                                                        break;
                                                    case 3:// khiêu chiến NPC whis
                                                        player.lastTimeSwapWhis = System.currentTimeMillis();
                                                        PlayerService.gI().savePlayer(player);

                                                        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                                                        Runnable task = () -> {
                                                            hide_npc(player, (int) ConstNpc.WHIS, 0);
                                                            PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                                            PlayerService.gI().playerMove(player, 485, 360);
                                                            PlayerService.gI().setPos(player, 488, 360, 55);
                                                            scheduler.shutdown();
                                                        };
                                                        scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                                        try {
                                                            Boss_Whis dt = new Boss_Whis(Util.createIdDuongTank((int) player.id), BossData.WHIS_NPC, player.zone, this.cx, this.cy, player);
                                                        } catch (Exception ex) {
                                                            Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null, ex);
                                                        }
                                                        player.zone.load_Me_To_Another(player);
                                                }
                                                return;
                                            case 5:
                                                switch (select) {
                                                    case 0:
                                                        createOtherMenu(player, 799455479, "Ngươi muốn hiến tế cho Bản thân hay Đệ tử", "Cho\nBản thân", "Cho\nĐệ tử", "Đóng");
                                                        return;
                                                    case 1:
                                                        NpcService.gI().createTutorial(player, avartar, "Ta là Whis được Đại thiên sứ cử xuống Trái đất để thu thập lại trang bị Thần linh\bbị kẻ xấu đánh cắp\n"
                                                                + "Hãy đi tiêu diệt kẻ xấu để giành lại trang bị Thần linh bị đánh cắp\n"
                                                                + "Hiến tế cho ta trang bị Thần linh, Ta sẽ ban cho ngươi trang bị kích hoạt tương ứng trong truyền thuyết\n"
                                                                + "Yêu cầu khi hiến tế:\b- Bản thân đang sử dụng trang bị Thần linh\b- Vàng trong hành trang: 2 Tỷ vàng\b(không giới hạn số trang bị Thần linh trong 1 lần hiến tế)");
                                                        return;
                                                }
                                        }

                                    case ConstNpc.HOC_TUYET_KY:
                                        switch (select) {
                                            case 0:
                                                Item biKipTuyetKy = InventoryService.gI().findItemBagByTemp(player, (short) 1229);
                                                int skillID = player.gender == 0 ? 24 : player.gender == 1 ? 26 : 25;
                                                Skill newSkill = SkillUtil.createSkill(skillID, 1);
                                                try {
                                                    Message msg = Service.getInstance().messageSubCommand((byte) 23);
                                                    msg.writer().writeShort(newSkill.skillId);
                                                    player.sendMessage(msg);
                                                    msg.cleanup();
                                                } catch (IOException e) {
                                                }
                                                try { // send effect susscess
                                                    Message msg = new Message(-81);
                                                    msg.writer().writeByte(0);
                                                    msg.writer().writeUTF("test");
                                                    msg.writer().writeUTF("test");
                                                    msg.writer().writeShort(tempId);
                                                    player.sendMessage(msg);
                                                    msg.cleanup();

                                                    msg = new Message(-81);
                                                    msg.writer().writeByte(1);
                                                    msg.writer().writeByte(2);
                                                    msg.writer().writeByte(InventoryService.gI().getIndexBag(player, biKipTuyetKy));
                                                    msg.writer().writeByte(-1);
                                                    player.sendMessage(msg);
                                                    msg.cleanup();

                                                    msg = new Message(-81);
                                                    msg.writer().writeByte(7);
                                                    msg.writer().writeShort(newSkill.template.iconId);
                                                    player.sendMessage(msg);
                                                    msg.cleanup();
                                                } catch (Exception e) {
                                                }
                                                Inventory inv = player.inventory;
                                                InventoryService.gI().subQuantityItemsBag(player, biKipTuyetKy, 9999);
                                                InventoryService.gI().sendItemBags(player);
                                                inv.subGold(10_000_000);
                                                inv.subGem(99);
                                                SkillUtil.setSkill(player, newSkill);
                                        }
                                        return;
                                    case 799455479:
                                        switch (select) {
                                            case 0:
                                                if (!player.getSession().actived) {
                                                    NpcService.gI().createTutorial(player, avartar, "Truy cập Trang chủ  để mở Thành viên");
                                                    return;
                                                }
                                                int gender = player.gender;
                                                List<Integer> ao = Arrays.asList(555, 557, 559);
                                                List<Integer> quan = Arrays.asList(556, 558, 560);
                                                List<Integer> gang = Arrays.asList(562, 564, 566);
                                                List<Integer> giay = Arrays.asList(563, 565, 567);
                                                int nhan = 561;

                                                boolean dieuKien1 = player.inventory.itemsBody.get(0).isNotNullItem();
                                                boolean dieuKien2 = player.inventory.itemsBody.get(1).isNotNullItem();
                                                boolean dieuKien3 = player.inventory.itemsBody.get(2).isNotNullItem();
                                                boolean dieuKien4 = player.inventory.itemsBody.get(3).isNotNullItem();
                                                boolean dieuKien5 = player.inventory.itemsBody.get(4).isNotNullItem();

                                                boolean dieuKien1_1 = dieuKien1 && (player.inventory.itemsBody.get(0).template.id == ao.get(gender));
                                                boolean dieuKien2_1 = dieuKien2 && (player.inventory.itemsBody.get(1).template.id == quan.get(gender));
                                                boolean dieuKien3_1 = dieuKien3 && (player.inventory.itemsBody.get(2).template.id == gang.get(gender));
                                                boolean dieuKien4_1 = dieuKien4 && (player.inventory.itemsBody.get(3).template.id == giay.get(gender));
                                                boolean dieuKien5_1 = dieuKien5 && (player.inventory.itemsBody.get(4).template.id == nhan);

                                                boolean condition1 = dieuKien1_1;
                                                boolean condition2 = dieuKien2_1;
                                                boolean condition3 = dieuKien3_1;
                                                boolean condition4 = dieuKien4_1;
                                                boolean condition5 = dieuKien5_1;

                                                if (condition1 || condition2 || condition3 || condition4 || condition5) {
                                                    String npcsay = "Danh sách hiến tế cho Whis:\n";
                                                    int i = 1;
                                                    if (condition1) {
                                                        npcsay += i + ". " + player.inventory.itemsBody.get(0).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition2) {
                                                        npcsay += i + ". " + player.inventory.itemsBody.get(1).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition3) {
                                                        npcsay += i + ". " + player.inventory.itemsBody.get(2).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition4) {
                                                        npcsay += i + ". " + player.inventory.itemsBody.get(3).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition5) {
                                                        npcsay += i + ". " + player.inventory.itemsBody.get(4).template.name + "\n";
                                                        i++;
                                                    }

                                                    npcsay += "Ngươi sẽ nhận lại một trang bị kích hoạt tương ứng trong truyền thuyết.";

                                                    createOtherMenu(player, ConstNpc.HIEN_TE_SU_PHU, npcsay, "Hiến tế\n(2 Tỷ vàng)", "Đóng");
                                                    return;
                                                } else {
                                                    NpcService.gI().createTutorial(player, avartar, "Khi nào ngươi mặc trang bị thần linh thì tới đây ta nói chuyện tiếp");
                                                    break;
                                                }

                                            case 1:

                                                if (!player.getSession().actived) {
                                                    NpcService.gI().createTutorial(player, avartar, "Truy cập Trang chủ NgocRongKakarot.Com để mở Thành viên");
                                                    return;
                                                }

                                                if (player.pet == null) {
                                                    NpcService.gI().createTutorial(player, avartar, "Ngươi cần phải có Đệ tử trước khi thực hiện");
                                                    return;
                                                }

                                                int gender_detu = player.pet.gender;

                                                List<Integer> ao2 = Arrays.asList(555, 557, 559);
                                                List<Integer> quan2 = Arrays.asList(556, 558, 560);
                                                List<Integer> gang2 = Arrays.asList(562, 564, 566);
                                                List<Integer> giay2 = Arrays.asList(563, 565, 567);
                                                int nhan2 = 561;

                                                boolean dieuKien12 = player.pet.inventory.itemsBody.get(0).isNotNullItem();
                                                boolean dieuKien22 = player.pet.inventory.itemsBody.get(1).isNotNullItem();
                                                boolean dieuKien32 = player.pet.inventory.itemsBody.get(2).isNotNullItem();
                                                boolean dieuKien42 = player.pet.inventory.itemsBody.get(3).isNotNullItem();
                                                boolean dieuKien52 = player.pet.inventory.itemsBody.get(4).isNotNullItem();

                                                boolean dieuKien1_12 = dieuKien12 && (player.pet.inventory.itemsBody.get(0).template.id == ao2.get(gender_detu));
                                                boolean dieuKien2_12 = dieuKien22 && (player.pet.inventory.itemsBody.get(1).template.id == quan2.get(gender_detu));
                                                boolean dieuKien3_12 = dieuKien32 && (player.pet.inventory.itemsBody.get(2).template.id == gang2.get(gender_detu));
                                                boolean dieuKien4_12 = dieuKien42 && (player.pet.inventory.itemsBody.get(3).template.id == giay2.get(gender_detu));
                                                boolean dieuKien5_12 = dieuKien52 && (player.pet.inventory.itemsBody.get(4).template.id == nhan2);

                                                boolean condition12 = dieuKien1_12;
                                                boolean condition22 = dieuKien2_12;
                                                boolean condition32 = dieuKien3_12;
                                                boolean condition42 = dieuKien4_12;
                                                boolean condition52 = dieuKien5_12;

                                                if (condition12 || condition22 || condition32 || condition42 || condition52) {
                                                    String npcsay = "Danh sách hiến tế cho Whis:\n";
                                                    int i = 1;
                                                    if (condition12) {
                                                        npcsay += i + ". " + player.pet.inventory.itemsBody.get(0).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition22) {
                                                        npcsay += i + ". " + player.pet.inventory.itemsBody.get(1).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition32) {
                                                        npcsay += i + ". " + player.pet.inventory.itemsBody.get(2).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition42) {
                                                        npcsay += i + ". " + player.pet.inventory.itemsBody.get(3).template.name + "\n";
                                                        i++;
                                                    }
                                                    if (condition52) {
                                                        npcsay += i + ". " + player.pet.inventory.itemsBody.get(4).template.name + "\n";
                                                        i++;
                                                    }

                                                    npcsay += "Ngươi sẽ nhận lại một trang bị kích hoạt tương ứng trong truyền thuyết.";

                                                    createOtherMenu(player, ConstNpc.HIEN_TE_DE_TU, npcsay, "OK");
                                                    return;
                                                } else {
                                                    NpcService.gI().createTutorial(player, avartar, "Khi nào đệ tử ngươi mặc trang bị thần linh thì tới đây ta nói chuyện tiếp");
                                                    break;
                                                }
                                        }

                                    case ConstNpc.HIEN_TE_SU_PHU:
                                        switch (select) {
                                            case 0:
                                                int i = 0;
                                                int gender = player.gender;
                                                List<Integer> ao = Arrays.asList(555, 557, 559);
                                                List<Integer> quan = Arrays.asList(556, 558, 560);
                                                List<Integer> gang = Arrays.asList(562, 564, 566);
                                                List<Integer> giay = Arrays.asList(563, 565, 567);

                                                List<Integer> aoSKH = Arrays.asList(555, 557, 559);
                                                List<Integer> quanSKH = Arrays.asList(556, 558, 560);
                                                List<Integer> gangSKH = Arrays.asList(562, 564, 566);
                                                List<Integer> giaySKH = Arrays.asList(563, 565, 567);
                                                int rada = 12;

                                                int[][] options = {{128, 129, 127}, {130, 131, 132}, {133, 135, 134}};
//                                                int skhId = ItemService.gI().randomSKHId((byte) 0);

                                                short itemId;

                                                int nhan = 561;

                                                boolean dieuKien1 = player.inventory.itemsBody.get(0).isNotNullItem();
                                                boolean dieuKien2 = player.inventory.itemsBody.get(1).isNotNullItem();
                                                boolean dieuKien3 = player.inventory.itemsBody.get(2).isNotNullItem();
                                                boolean dieuKien4 = player.inventory.itemsBody.get(3).isNotNullItem();
                                                boolean dieuKien5 = player.inventory.itemsBody.get(4).isNotNullItem();

                                                boolean dieuKien1_1 = dieuKien1 && (player.inventory.itemsBody.get(0).template.id == ao.get(gender));
                                                boolean dieuKien2_1 = dieuKien2 && (player.inventory.itemsBody.get(1).template.id == quan.get(gender));
                                                boolean dieuKien3_1 = dieuKien3 && (player.inventory.itemsBody.get(2).template.id == gang.get(gender));
                                                boolean dieuKien4_1 = dieuKien4 && (player.inventory.itemsBody.get(3).template.id == giay.get(gender));
                                                boolean dieuKien5_1 = dieuKien5 && (player.inventory.itemsBody.get(4).template.id == nhan);

                                                boolean condition1 = dieuKien1_1;
                                                boolean condition2 = dieuKien2_1;
                                                boolean condition3 = dieuKien3_1;
                                                boolean condition4 = dieuKien4_1;
                                                boolean condition5 = dieuKien5_1;

                                                if (condition1 || condition2 || condition3 || condition4 || condition5) {

                                                    if (player.inventory.gold < 2_000_000_000) {
                                                        NpcService.gI().createTutorial(player, avartar, "Ngươi còn thiếu " + Util.numberToMoney(2_000_000_000 - player.inventory.gold) + " vàng");
                                                        return;
                                                    }

                                                    player.inventory.subGold(2000000000); // FIX: dùng method an toàn
                                                    Service.getInstance().sendMoney(player);

                                                    if (condition1) { // ÁO
                                                        Item ao2 = ItemService.gI().createNewItem((short) gender);

                                                        InventoryService.gI().removeItem(player.inventory.itemsBody, 0);

                                                        Random random = new Random();
                                                        int tyle = random.nextInt(100);
                                                        ao2.itemOptions.add(new ItemOption(47, 3));

                                                        if (tyle < 30) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(129, 1));
                                                                ao2.itemOptions.add(new ItemOption(141, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(131, 1));
                                                                ao2.itemOptions.add(new ItemOption(143, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(135, 1));
                                                                ao2.itemOptions.add(new ItemOption(138, 1));
                                                            }
                                                        } else if (tyle < 60) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(128, 1));
                                                                ao2.itemOptions.add(new ItemOption(140, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(130, 1));
                                                                ao2.itemOptions.add(new ItemOption(142, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(133, 1));
                                                                ao2.itemOptions.add(new ItemOption(136, 1));
                                                            }

                                                        } else {
                                                            ao2.itemOptions.add(new ItemOption(134, 1));
                                                            ao2.itemOptions.add(new ItemOption(137, 1));
                                                        }
                                                        InventoryService.gI().addItemBody(player, ao2);
                                                        InventoryService.gI().sendItemBody(player);
                                                        i++;
                                                    }

                                                    if (condition2) { // QUẦN
                                                        Item ao2 = ItemService.gI().createNewItem((short) (gender + 6));
                                                        InventoryService.gI().removeItem(player.inventory.itemsBody, 1);
//                                                      ao2.template.id = (short) (gender + 6);
                                                        List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh sách các ItemOption cần xóa
                                                        for (ItemOption itopt : ao2.itemOptions) {
                                                            int optionId = itopt.optionTemplate.id;
                                                            if (optionId >= 0 && optionId <= 233) {
                                                                optionsToRemove.add(itopt);
                                                            }
                                                        }

                                                        Random random = new Random();
                                                        int tyle = random.nextInt(100);

                                                        ao2.itemOptions.removeAll(optionsToRemove);
                                                        ao2.itemOptions.add(new ItemOption(6, 20));

                                                        if (tyle < 30) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(129, 1));
                                                                ao2.itemOptions.add(new ItemOption(141, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(131, 1));
                                                                ao2.itemOptions.add(new ItemOption(143, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(135, 1));
                                                                ao2.itemOptions.add(new ItemOption(138, 1));
                                                            }
                                                        } else if (tyle < 60) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(128, 1));
                                                                ao2.itemOptions.add(new ItemOption(140, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(130, 1));
                                                                ao2.itemOptions.add(new ItemOption(142, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(133, 1));
                                                                ao2.itemOptions.add(new ItemOption(136, 1));
                                                            }

                                                        } else {
                                                            ao2.itemOptions.add(new ItemOption(134, 1));
                                                            ao2.itemOptions.add(new ItemOption(137, 1));
                                                        }
                                                        InventoryService.gI().addItemBody(player, ao2);
                                                        InventoryService.gI().sendItemBody(player);
                                                        Service.getInstance().Send_Caitrang(player);
                                                        Service.getInstance().Send_Info_NV(player);
                                                        i++;
                                                    }
                                                    if (condition3) { // GĂNG

                                                        Item ao2 = ItemService.gI().createNewItem((short) (gender + 21));
                                                        InventoryService.gI().removeItem(player.inventory.itemsBody, 2);

                                                        List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh sách các ItemOption cần xóa
                                                        for (ItemOption itopt : ao2.itemOptions) {
                                                            int optionId = itopt.optionTemplate.id;
                                                            if (optionId >= 0 && optionId <= 233) {
                                                                optionsToRemove.add(itopt);
                                                            }
                                                        }

                                                        Random random = new Random();
                                                        int tyle = random.nextInt(100);

                                                        ao2.itemOptions.removeAll(optionsToRemove);
                                                        ao2.itemOptions.add(new ItemOption(0, 5));

                                                        if (tyle < 30) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(129, 1));
                                                                ao2.itemOptions.add(new ItemOption(141, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(131, 1));
                                                                ao2.itemOptions.add(new ItemOption(143, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(135, 1));
                                                                ao2.itemOptions.add(new ItemOption(138, 1));
                                                            }
                                                        } else if (tyle < 60) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(128, 1));
                                                                ao2.itemOptions.add(new ItemOption(140, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(130, 1));
                                                                ao2.itemOptions.add(new ItemOption(142, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(133, 1));
                                                                ao2.itemOptions.add(new ItemOption(136, 1));
                                                            }

                                                        } else {
                                                            ao2.itemOptions.add(new ItemOption(134, 1));
                                                            ao2.itemOptions.add(new ItemOption(137, 1));
                                                        }
                                                        InventoryService.gI().addItemBody(player, ao2);
                                                        InventoryService.gI().sendItemBody(player);
                                                        Service.getInstance().Send_Caitrang(player);
                                                        Service.getInstance().Send_Info_NV(player);
                                                        i++;
                                                    }
                                                    if (condition4) { // GIẦY

                                                        Item ao2 = ItemService.gI().createNewItem((short) (gender + 27));
                                                        InventoryService.gI().removeItem(player.inventory.itemsBody, 3);

                                                        List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh sách các ItemOption cần xóa
                                                        for (ItemOption itopt : ao2.itemOptions) {
                                                            int optionId = itopt.optionTemplate.id;
                                                            if (optionId >= 0 && optionId <= 233) {
                                                                optionsToRemove.add(itopt);
                                                            }
                                                        }

                                                        Random random = new Random();
                                                        int tyle = random.nextInt(100);

                                                        ao2.itemOptions.removeAll(optionsToRemove);
                                                        ao2.itemOptions.add(new ItemOption(7, 10));

                                                        if (tyle < 30) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(129, 1));
                                                                ao2.itemOptions.add(new ItemOption(141, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(131, 1));
                                                                ao2.itemOptions.add(new ItemOption(143, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(135, 1));
                                                                ao2.itemOptions.add(new ItemOption(138, 1));
                                                            }
                                                        } else if (tyle < 60) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(128, 1));
                                                                ao2.itemOptions.add(new ItemOption(140, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(130, 1));
                                                                ao2.itemOptions.add(new ItemOption(142, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(133, 1));
                                                                ao2.itemOptions.add(new ItemOption(136, 1));
                                                            }

                                                        } else {
                                                            ao2.itemOptions.add(new ItemOption(134, 1));
                                                            ao2.itemOptions.add(new ItemOption(137, 1));
                                                        }
                                                        InventoryService.gI().addItemBody(player, ao2);
                                                        InventoryService.gI().sendItemBody(player);
                                                        Service.getInstance().Send_Caitrang(player);
                                                        Service.getInstance().Send_Info_NV(player);
                                                        i++;
                                                    }
                                                    if (condition5) { // RADA
                                                        Item ao2 = ItemService.gI().createNewItem((short) 12);
                                                        InventoryService.gI().removeItem(player.inventory.itemsBody, 4);

                                                        List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh sách các ItemOption cần xóa
                                                        for (ItemOption itopt : ao2.itemOptions) {
                                                            int optionId = itopt.optionTemplate.id;
                                                            if (optionId >= 0 && optionId <= 233) {
                                                                optionsToRemove.add(itopt);
                                                            }
                                                        }

                                                        Random random = new Random();
                                                        int tyle = random.nextInt(100);

                                                        ao2.itemOptions.removeAll(optionsToRemove);
                                                        ao2.itemOptions.add(new ItemOption(14, 1));

                                                        if (tyle < 30) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(129, 1));
                                                                ao2.itemOptions.add(new ItemOption(141, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(131, 1));
                                                                ao2.itemOptions.add(new ItemOption(143, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(135, 1));
                                                                ao2.itemOptions.add(new ItemOption(138, 1));
                                                            }
                                                        } else if (tyle < 60) {
                                                            if (gender == 0) {
                                                                ao2.itemOptions.add(new ItemOption(128, 1));
                                                                ao2.itemOptions.add(new ItemOption(140, 1));
                                                            }
                                                            if (gender == 1) {
                                                                ao2.itemOptions.add(new ItemOption(130, 1));
                                                                ao2.itemOptions.add(new ItemOption(142, 1));
                                                            } else {
                                                                ao2.itemOptions.add(new ItemOption(133, 1));
                                                                ao2.itemOptions.add(new ItemOption(136, 1));
                                                            }

                                                        } else {
                                                            ao2.itemOptions.add(new ItemOption(134, 1));
                                                            ao2.itemOptions.add(new ItemOption(137, 1));
                                                        }
                                                        InventoryService.gI().addItemBody(player, ao2);
                                                        InventoryService.gI().sendItemBody(player);
                                                        Service.getInstance().Send_Caitrang(player);
                                                        Service.getInstance().Send_Info_NV(player);
                                                        i++;
                                                    }
                                                    NpcService.gI().createTutorial(player, avartar, "Ba la ca ... ca ca ca... Um ba mi xa ki... ca ca...\n"
                                                            + "Na na ca ca... la la... sa da ma ta ro bu ki....\n"
                                                            + "....................\n"
                                                            + "Bạn vừa hiến tế thành công cho Whis " + i + " trang bị Thần linh và nhận được " + i + " trang bị kích hoạt trong truyền thuyết.");
                                                    InventoryService.gI().sendItemBody(player);
                                                    Service.getInstance().Send_Caitrang(player);
                                                    Service.getInstance().Send_Info_NV(player);
                                                    break;
                                                }
                                                break;
                                        }
                                }
                            }
                        }
                    };
                    break;

                case ConstNpc.BO_MONG:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 47 || this.mapId == 84) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Xin chào, cậu muốn tôi giúp gì?",
                                            "Nhiệm vụ\nhàng ngày", "Mã quà tặng", "Nhận ngọc\nmiễn phí", "Từ chối");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 47 || this.mapId == 84) {
                                    if (player.iDMark.isBaseMenu()) {
                                        switch (select) {
                                            case 0:
                                                if (player.playerTask.sideTask.template != null) {
                                                    String npcSay = "Nhiệm vụ hiện tại: "
                                                            + player.playerTask.sideTask.getName() + " ("
                                                            + player.playerTask.sideTask.getLevel() + ")"
                                                            + "\nHiện tại đã hoàn thành: "
                                                            + player.playerTask.sideTask.count + "/"
                                                            + player.playerTask.sideTask.maxCount + " ("
                                                            + player.playerTask.sideTask.getPercentProcess()
                                                            + "%)\nSố nhiệm vụ còn lại trong ngày: "
                                                            + player.playerTask.sideTask.leftTask + "/"
                                                            + ConstTask.MAX_SIDE_TASK;
                                                    this.createOtherMenu(player, ConstNpc.MENU_OPTION_PAY_SIDE_TASK,
                                                            npcSay, "Trả nhiệm\nvụ", "Hủy nhiệm\nvụ");
                                                } else {
                                                    this.createOtherMenu(player, ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,
                                                            "Tôi có vài nhiệm vụ theo cấp bậc, "
                                                            + "sức cậu có thể làm được cái nào?",
                                                            "Dễ", "Bình thường", "Khó", "Siêu khó", "Từ chối");
                                                }
                                                break;

                                            case 1:
                                                Input.gI().createFormGiftCode(player);
                                                break;
                                            case 2:
                                                TaskService.gI().checkDoneAchivements(player);
                                                TaskService.gI().sendAchivement(player);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK) {
                                        switch (select) {
                                            case 0:
                                            case 1:
                                            case 2:
                                            case 3:
                                                TaskService.gI().changeSideTask(player, (byte) select);
                                                break;
                                        }
                                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_PAY_SIDE_TASK) {
                                        switch (select) {
                                            case 0:
                                                TaskService.gI().paySideTask(player);
                                                break;
                                            case 1:
                                                TaskService.gI().removeSideTask(player);
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.GOKU_SSJ:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 80) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Xin chào, tôi có thể giúp gì cho cậu?", "Tới hành tinh\nYardart",
                                            "Từ chối");
                                } else if (this.mapId == 131) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Xin chào, tôi có thể giúp gì cho cậu?", "Quay về", "Từ chối");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.BASE_MENU:
                                        if (this.mapId == 80) {
                                            // if (select == 0) {
                                            // if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_24_0) {
                                            // ChangeMapService.gI().changeMapBySpaceShip(player, 160, -1, 168);
                                            // } else {
                                            // this.npcChat(player, "Xin lỗi, tôi chưa thể đưa cậu tới nơi đó lúc
                                            // này...");
                                            // }
                                            // } else
                                            if (select == 0) {
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 131, -1, 940);
                                            }
                                        } else if (this.mapId == 131) {
                                            if (select == 0) {
                                                ChangeMapService.gI().changeMapBySpaceShip(player, 80, -1, 870);
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.GOKU_SSJ_:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 133) {
                                    Item biKiep = InventoryService.gI().findItem(player.inventory.itemsBag, 590);
                                    int soLuong = 0;
                                    if (biKiep != null) {
                                        soLuong = biKiep.quantity;
                                    }
                                    if (soLuong >= 10000) {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Bạn đang có " + soLuong
                                                + " bí kiếp.\n"
                                                + "Hãy kiếm đủ 10000 bí kiếp tôi sẽ dạy bạn cách dịch chuyển tức thời của người Yardart",
                                                "Học dịch\nchuyển", "Đóng");
                                    } else {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Bạn đang có " + soLuong
                                                + " bí kiếp.\n"
                                                + "Hãy kiếm đủ 10000 bí kiếp tôi sẽ dạy bạn cách dịch chuyển tức thời của người Yardart",
                                                "Đóng");
                                    }
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == 133) {
                                    Item biKiep = InventoryService.gI().findItem(player.inventory.itemsBag, 590);
                                    int soLuong = 0;
                                    if (biKiep != null) {
                                        soLuong = biKiep.quantity;
                                    }
                                    if (soLuong >= 10000 && InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        Item yardart = ItemService.gI().createNewItem((short) (player.gender + 592));
                                        yardart.itemOptions.add(new ItemOption(47, 400));
                                        yardart.itemOptions.add(new ItemOption(108, 10));
                                        InventoryService.gI().addItemBag(player, yardart, 0);
                                        InventoryService.gI().subQuantityItemsBag(player, biKiep, 10000);
                                        InventoryService.gI().sendItemBags(player);
                                        Service.getInstance().sendThongBao(player,
                                                "Bạn vừa nhận được trang phục tộc Yardart");
                                    }
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.GHI_DANH:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        String[] menuselect = new String[]{};

                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId == ConstMap.DAI_HOI_VO_THUAT) {
                                    int crrHOUR = TimeUtil.getCurrHour();
                                    if (DaiHoiVoThuatManager.gI().openDHVT && (System.currentTimeMillis() <= DaiHoiVoThuatManager.gI().tOpenDHVT)) {
                                        String nameDH = DaiHoiVoThuatManager.gI().nameRoundDHVT();
                                        this.createOtherMenu(player, ConstNpc.DAI_HOI_VO_THUAT, "Chào mừng bạn đến với đại hội võ thuật\n"
                                                + "Giải " + nameDH + " đang có " + DaiHoiVoThuatManager.gI().lstIDPlayers.size() + " người đăng ký thi đấu\n" + DaiHoiVoThuatService.gI().textDaiHoi(player.nPoint.power), "Thông tin\nChi tiết", "Đăng kí", "Giải\nSiêu Hạng", "Đại Hội\nVõ Thuật\nLần thứ\n23");
                                    } else {
                                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Đã hết hạn đăng ký thi đấu, xin vui lòng chờ đến giải sau" + DaiHoiVoThuatManager.gI().timeDHVTnext(crrHOUR) + "\n" + DaiHoiVoThuatService.gI().textDaiHoi(player.nPoint.power), new String[]{"Thông tin\bChi tiết", "Giải\nSiêu Hạng", "Đại Hội\nVõ Thuật\nLần thứ\n23", "Ðóng"});
                                    }
                                } else if (this.mapId == ConstMap.DAI_HOI_VO_THUAT_129) {
                                    int goldchallenge = player.goldChallenge;
                                    if (player.levelWoodChest == 0) {
                                        menuselect = new String[]{
                                            "Hướng\ndẫn\nthêm",
                                            "Thi đấu\n" + player.gemChallenge + " ngọc",
                                            "Thi đấu\n" + Util.numberToMoney(goldchallenge) + "\nvàng",
                                            "Về\nĐại Hội\nVõ Thuật"};
                                    } else {
                                        menuselect = new String[]{
                                            "Hướng\ndẫn\nthêm",
                                            "Thi đấu\n" + player.gemChallenge + " ngọc",
                                            "Thi đấu\n" + Util.numberToMoney(goldchallenge) + "\nvàng",
                                            "Nhận\nthưởng\n Rương Cấp " + player.levelWoodChest,
                                            "Về\nĐại Hội\nVõ Thuật"};
                                    }
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "Đại hội võ thuật lần thứ 23\n"
                                            + "Diễn ra bất kể ngày đêm, ngày nghỉ, ngày lễ\n"
                                            + "Phần thưởng vô cùng quý giá\n"
                                            + "Nhanh chóng tham gia nào",
                                            menuselect, "Từ chối");
                                } else {
                                    super.openBaseMenu(player);
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.DAI_HOI_VO_THUAT:
                                        switch (select) {
                                            case 0:
                                                NpcService.gI().createTutorial(player, avartar, "Lịch thi đấu trong ngày\b Giải Nhi đồng: 8,14,18h\b Giải Siêu cấp 1: 9,13,19h\b Giải Siêu cấp 2: 10,15,20h\b Giải Siêu cấp 3: 11,16,21h\b Giải Ngoại hạng: 12,17,22,23h\n"
                                                        + "Giải thưởng khi thắng mỗi vòng\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\b Vô địch: 5 viên đá nâng cấp\n"
                                                        + "Lệ phí đăng ký các giải đấu\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\n"
                                                        + "Vui lòng đến đúng giờ để đăng ký thi đấu");
                                                break;
                                            case 1:
                                                //    this.createOtherMenu(player, ConstNpc.DANG_KY_DAI_HOI_VO_THUAT, "Hiện đang có giải đấu " + DaiHoiVoThuatManager.gI().nameRoundDHVT() + " bạn có muốn đăng ký không?", "Giải\n " + DaiHoiVoThuatManager.gI().nameRoundDHVT() + "\n(" + DaiHoiVoThuatManager.gI().costRoundDHVT() + ")", "Từ chối");
                                                NpcService.gI().createTutorial(player, avartar, "Chức năng đang được phát triển");
                                                break;
                                            case 2:

                                                ChangeMapService.gI().changeMapNonSpaceship(player, 113, player.location.x, 360);
                                                break;
                                            case 3:
                                                ChangeMapService.gI().changeMapNonSpaceship(player, 129, player.location.x, 360);
                                                break;
                                        }
                                        return;
                                    case ConstNpc.DANG_KY_DAI_HOI_VO_THUAT:
                                        switch (select) {
                                            case 0:
                                                if (DaiHoiVoThuatService.gI().canRegisDHVT(player.nPoint.power)) {
                                                    if (DaiHoiVoThuatManager.gI().lstIDPlayers.size() < 256) {
                                                        if (DaiHoiVoThuatManager.gI().typeDHVT == (byte) 5 && player.inventory.gold >= 10000) {
                                                            if (DaiHoiVoThuatManager.gI().isAssignDHVT(player.id)) {
                                                                Service.getInstance().sendThongBao(player, "Bạn đã đăng ký tham gia đại hội võ thuật rồi");
                                                            } else {
                                                                player.inventory.subGold(10000); // FIX: dùng method an toàn
                                                                Service.getInstance().sendMoney(player);
                                                                Service.getInstance().sendThongBao(player, "Bạn đã đăng ký thành công, nhớ có mặt tại đây trước giờ thi đấu");
                                                                DaiHoiVoThuatManager.gI().lstIDPlayers.add(player.id);
                                                            }
                                                        } else if (DaiHoiVoThuatManager.gI().typeDHVT > (byte) 0 && DaiHoiVoThuatManager.gI().typeDHVT < (byte) 5 && player.inventory.gem >= (int) (2 * DaiHoiVoThuatManager.gI().typeDHVT)) {
                                                            if (DaiHoiVoThuatManager.gI().isAssignDHVT(player.id)) {
                                                                Service.getInstance().sendThongBao(player, "Bạn đã đăng ký tham gia đại hội võ thuật rồi");
                                                            } else {
                                                                player.inventory.subGem((int) (2 * DaiHoiVoThuatManager.gI().typeDHVT)); // FIX
                                                                Service.getInstance().sendMoney(player);
                                                                Service.getInstance().sendThongBao(player, "Bạn đã đăng ký thành công, nhớ có mặt tại đây trước giờ thi đấu");
                                                                DaiHoiVoThuatManager.gI().lstIDPlayers.add(player.id);
                                                            }
                                                        } else {
                                                            Service.getInstance().sendThongBao(player, "Không đủ vàng ngọc để đăng ký thi đấu");
                                                        }
                                                    } else {
                                                        Service.getInstance().sendThongBao(player, "Hiện tại đã đạt tới số lượng người đăng ký tối đa, xin hãy chờ đến giải sau");
                                                    }

                                                } else {
                                                    NpcService.gI().createTutorial(player, avartar, DaiHoiVoThuatService.gI().textDaiHoi2(player.nPoint.power));
                                                }
                                        }
                                        return;
                                    case ConstNpc.MENU_NHAN_RUONG:
                                        switch (select) {
                                            case 0:
                                                if (!player.receivedWoodChest) {
                                                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                                        Item it = ItemService.gI()
                                                                .createNewItem((short) ConstItem.RUONG_GO);
                                                        it.itemOptions.add(new ItemOption(72, player.levelWoodChest));
                                                        it.createTime = System.currentTimeMillis();
                                                        InventoryService.gI().addItemBag(player, it, 0);
                                                        InventoryService.gI().sendItemBags(player);
                                                        NpcService.gI().createMenuConMeo(player, 251020003, -1, "Bạn nhận được\n"
                                                                + "|1|Rương gỗ\n"
                                                                + "|2|Giấu bên trong nhiều vật phẩm quý giá", "OK");
                                                        player.receivedWoodChest = true;
                                                        player.levelWoodChest = 0;
                                                        return;
                                                    } else {
                                                        this.npcChat(player, "Hành trang đã đầy");
                                                    }
                                                } else {
                                                    Service.getInstance().sendThongBao(player,
                                                            "Mỗi ngày chỉ có thể nhận rương báu 1 lần");
                                                }
                                                break;
                                        }
                                        break;
                                    case ConstNpc.BASE_MENU:
                                        if (this.mapId == ConstMap.DAI_HOI_VO_THUAT) {
                                            if (player.iDMark.isBaseMenu()) {
                                                switch (select) {
                                                    case 0:
                                                        NpcService.gI().createTutorial(player, avartar, "Lịch thi đấu trong ngày\b Giải Nhi đồng: 8,14,18h\b Giải Siêu cấp 1: 9,13,19h\b Giải Siêu cấp 2: 10,15,20h\b Giải Siêu cấp 3: 11,16,21h\b Giải Ngoại hạng: 12,17,22,23h\n"
                                                                + "Giải thưởng khi thắng mỗi vòng\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\b Vô địch: 5 viên đá nâng cấp\n"
                                                                + "Lệ phí đăng ký các giải đấu\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\n"
                                                                + "Vui lòng đến đúng giờ để đăng ký thi đấu");
                                                        break;
                                                    case 1:
                                                        //  NpcService.gI().createTutorial(player, avartar, "Chức năng đang được phát triển");
                                                        ChangeMapService.gI().changeMapNonSpaceship(player, 113, player.location.x, 360);
                                                        break;
                                                    case 2:
                                                        ChangeMapService.gI().changeMapNonSpaceship(player, 129, player.location.x, 360);
                                                        break;
                                                }
                                            }
                                        } else if (this.mapId == ConstMap.DAI_HOI_VO_THUAT_129) {
                                            int goldchallenge = player.goldChallenge;
                                            if (player.levelWoodChest == 0) {
                                                switch (select) {
                                                    case 0:
                                                        NpcService.gI().createTutorial(player, avartar, "Đại hội quy tụ nhiều cao thủ như Jacky Chun, Thiên Xin Hăng, Tàu Bảy Bảy...\bPhần thưởng là 1 rương gỗ chứa nhiều vật phẩm giá trị\bKhi hạ được 1 đối thủ, phần thưởng sẽ nâng lên 1 cấp\bRương càng cao cấp, vật phẩm trong đó càng giá trị hơn\n"
                                                                + "Mỗi ngày bạn chỉ được nhận 1 phần thưởng\bBạn hãy cố gắng hết sức mình để\b nhận phần thưởng xứng đáng nhất nhé");
                                                        break;
                                                    case 1:
                                                        if (!player.receivedWoodChest) {
                                                            if (InventoryService.gI().finditemWoodChest(player)) {
                                                                if (player.inventory.getGem() >= player.gemChallenge) {
                                                                    DHVT23Service.gI().startChallenge(player);
                                                                    player.inventory.subGem(player.gemChallenge);
                                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                                    player.goldChallenge += 50000;
                                                                    player.gemChallenge += 1;
                                                                } else {
                                                                    Service.getInstance().sendThongBao(player,
                                                                            "Không đủ ngọc, còn thiếu "
                                                                            + Util.numberToMoney(player.gemChallenge
                                                                                    - player.inventory.gem)
                                                                            + " ngọc nữa");
                                                                }
                                                            } else {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Hãy mở rương báu vật trước");
                                                            }
                                                        } else {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Hãy chờ đến ngày mai");
                                                        }
                                                        break;
                                                    case 2:
                                                        if (!player.receivedWoodChest) {
                                                            if (InventoryService.gI().finditemWoodChest(player)) {
                                                                if (player.inventory.getGold() >= goldchallenge) {
                                                                    DHVT23Service.gI().startChallenge(player);
                                                                    player.inventory.subGold(goldchallenge);
                                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                                    player.goldChallenge += 50000;
                                                                    player.gemChallenge += 1;
                                                                } else {
                                                                    Service.getInstance().sendThongBao(player,
                                                                            "Không đủ vàng, còn thiếu "
                                                                            + Util.numberToMoney(goldchallenge
                                                                                    - player.inventory.gold)
                                                                            + " vàng nữa");
                                                                }
                                                            } else {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Hãy mở rương báu vật trước");
                                                            }
                                                            break;
                                                        } else {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Hãy chờ đến ngày mai");
                                                        }
                                                        break;
                                                    case 3:
                                                        ChangeMapService.gI().changeMapNonSpaceship(player,
                                                                ConstMap.DAI_HOI_VO_THUAT, player.location.x, 336);
                                                        break;
                                                }
                                            } else {
                                                switch (select) {
                                                    case 0:
                                                        NpcService.gI().createTutorial(player, avartar, "Đại hội quy tụ nhiều cao thủ như Jacky Chun, Thiên Xin Hăng, Tàu Bảy Bảy...\bPhần thưởng là 1 rương gỗ chứa nhiều vật phẩm giá trị\bKhi hạ được 1 đối thủ, phần thưởng sẽ nâng lên 1 cấp\bRương càng cao cấp, vật phẩm trong đó càng giá trị hơn\n"
                                                                + "Mỗi ngày bạn chỉ được nhận 1 phần thưởng\bBạn hãy cố gắng hết sức mình để\b nhận phần thưởng xứng đáng nhất nhé");
                                                        break;
                                                    case 1:
                                                        if (!player.receivedWoodChest) {
                                                            if (InventoryService.gI().finditemWoodChest(player)) {
                                                                if (player.inventory.getGem() >= player.gemChallenge) {
                                                                    DHVT23Service.gI().startChallenge(player);
                                                                    player.inventory.subGem(player.gemChallenge);
                                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                                    player.goldChallenge += 50000;
                                                                    player.gemChallenge += 1;
                                                                } else {
                                                                    Service.getInstance().sendThongBao(player,
                                                                            "Không đủ ngọc, còn thiếu "
                                                                            + Util.numberToMoney(player.gemChallenge
                                                                                    - player.inventory.gem)
                                                                            + " ngọc nữa");
                                                                }
                                                            } else {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Hãy mở rương báu vật trước");
                                                            }
                                                        } else {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Hãy chờ đến ngày mai");
                                                        }

                                                        break;
                                                    case 2:
                                                        if (!player.receivedWoodChest) {
                                                            if (InventoryService.gI().finditemWoodChest(player)) {
                                                                if (player.inventory.getGold() >= goldchallenge) {
                                                                    DHVT23Service.gI().startChallenge(player);
                                                                    player.inventory.subGold(goldchallenge);
                                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                                    player.goldChallenge += 2000000;
                                                                } else {
                                                                    Service.getInstance().sendThongBao(player,
                                                                            "Không đủ vàng, còn thiếu "
                                                                            + Util.numberToMoney(goldchallenge
                                                                                    - player.inventory.gold)
                                                                            + " vàng");
                                                                }
                                                            } else {
                                                                Service.getInstance().sendThongBao(player,
                                                                        "Hãy mở rương báu vật trước");
                                                            }
                                                        } else {
                                                            Service.getInstance().sendThongBao(player,
                                                                    "Hãy chờ đến ngày mai");
                                                        }
                                                        break;
                                                    case 3:
                                                        createOtherMenu(player, ConstNpc.MENU_NHAN_RUONG, "Phần thưởng của bạn đang ở cấp " + player.levelWoodChest + " / 12\n"
                                                                + "Mỗi ngày chỉ được nhận phần thưởng 1 lần\n"
                                                                + "bạn có chắc sẽ nhận phần thưởng ngay bây giờ?",
                                                                "OK", "Từ chối");
                                                        break;
                                                    case 4:
                                                        ChangeMapService.gI().changeMapNonSpaceship(player,
                                                                ConstMap.DAI_HOI_VO_THUAT, player.location.x, 336);
                                                        break;
                                                }
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.NOI_BANH:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                        "Xin chào " + player.name + "\nTôi là nồi nấu bánh\nTôi có thể giúp gì cho bạn",
                                        "Làm\nBánh Tét", "Làm\nBánh Chưng", getMenuLamBanh(player, 0),
                                        getMenuLamBanh(player, 1), "Đổi Hộp\nQuà Tết");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.BASE_MENU:
                                        switch (select) {
                                            case 0:
                                                Item thitBaChi = InventoryService.gI().findItem(player,
                                                        ConstItem.THIT_BA_CHI, 99);
                                                Item gaoNep = InventoryService.gI().findItem(player, ConstItem.GAO_NEP,
                                                        99);
                                                Item doXanh = InventoryService.gI().findItem(player, ConstItem.DO_XANH,
                                                        99);
                                                Item laChuoi = InventoryService.gI().findItem(player,
                                                        ConstItem.LA_CHUOI, 99);
                                                if (thitBaChi != null && gaoNep != null && doXanh != null
                                                        && laChuoi != null) {
                                                    InventoryService.gI().subQuantityItemsBag(player, thitBaChi, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, gaoNep, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, doXanh, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, laChuoi, 99);
                                                    Item banhtet = ItemService.gI()
                                                            .createNewItem((short) ConstItem.BANH_TET_2023);
                                                    banhtet.itemOptions.add(new ItemOption(74, 0));
                                                    InventoryService.gI().addItemBag(player, banhtet, 0);
                                                    InventoryService.gI().sendItemBags(player);
                                                    Service.getInstance().sendThongBao(player,
                                                            "Bạn nhận được Bánh Tét");
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Không đủ nguyên liệu");
                                                }
                                                break;
                                            case 1:
                                                Item thitHeo1 = InventoryService.gI().findItem(player,
                                                        ConstItem.THIT_HEO_2023, 99);
                                                Item gaoNep1 = InventoryService.gI().findItem(player, ConstItem.GAO_NEP,
                                                        99);
                                                Item doXanh1 = InventoryService.gI().findItem(player, ConstItem.DO_XANH,
                                                        99);
                                                Item laDong1 = InventoryService.gI().findItem(player,
                                                        ConstItem.LA_DONG_2023, 99);
                                                if (thitHeo1 != null && gaoNep1 != null && doXanh1 != null
                                                        && laDong1 != null) {
                                                    InventoryService.gI().subQuantityItemsBag(player, thitHeo1, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, gaoNep1, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, doXanh1, 99);
                                                    InventoryService.gI().subQuantityItemsBag(player, laDong1, 99);
                                                    Item banhChung = ItemService.gI()
                                                            .createNewItem((short) ConstItem.BANH_CHUNG_2023);
                                                    banhChung.itemOptions.add(new ItemOption(74, 0));
                                                    InventoryService.gI().addItemBag(player, banhChung, 0);
                                                    InventoryService.gI().sendItemBags(player);
                                                    Service.getInstance().sendThongBao(player,
                                                            "Bạn nhận được Bánh Chưng");
                                                } else {
                                                    Service.getInstance().sendThongBao(player, "Không đủ nguyên liệu");
                                                }
                                                break;
                                            case 2:
                                                if (!player.event.isCookingTetCake()) {
                                                    Item banhTet2 = InventoryService.gI().findItem(player,
                                                            ConstItem.BANH_TET_2023, 1);
                                                    Item phuGiaTaoMau2 = InventoryService.gI().findItem(player,
                                                            ConstItem.PHU_GIA_TAO_MAU, 1);
                                                    Item giaVi2 = InventoryService.gI().findItem(player,
                                                            ConstItem.GIA_VI_TONG_HOP, 1);

                                                    if (banhTet2 != null && phuGiaTaoMau2 != null && giaVi2 != null) {
                                                        InventoryService.gI().subQuantityItemsBag(player, banhTet2, 1);
                                                        InventoryService.gI().subQuantityItemsBag(player, phuGiaTaoMau2,
                                                                1);
                                                        InventoryService.gI().subQuantityItemsBag(player, giaVi2, 1);
                                                        InventoryService.gI().sendItemBags(player);
                                                        player.event.setTimeCookTetCake(300);
                                                        player.event.setCookingTetCake(true);
                                                        Service.getInstance().sendThongBao(player,
                                                                "Bắt đầu nấu bánh,thời gian nấu bánh là 5 phút");
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Không đủ nguyên liệu");
                                                    }
                                                } else if (player.event.isCookingTetCake()
                                                        && player.event.getTimeCookTetCake() == 0) {
                                                    Item cake = ItemService.gI()
                                                            .createNewItem((short) ConstItem.BANH_TET_CHIN, 1);
                                                    cake.itemOptions.add(new ItemOption(77, 20));
                                                    cake.itemOptions.add(new ItemOption(103, 20));
                                                    cake.itemOptions.add(new ItemOption(74, 0));
                                                    InventoryService.gI().addItemBag(player, cake, 0);
                                                    InventoryService.gI().sendItemBags(player);
                                                    player.event.setCookingTetCake(false);
                                                    player.event.addEventPoint(1);
                                                    Service.getInstance().sendThongBao(player,
                                                            "Bạn nhận được Bánh Tét (đã chính) và 1 điểm sự kiện");
                                                }
                                                break;
                                            case 3:
                                                if (!player.event.isCookingChungCake()) {
                                                    Item banhChung3 = InventoryService.gI().findItem(player,
                                                            ConstItem.BANH_CHUNG_2023, 1);
                                                    Item phuGiaTaoMau3 = InventoryService.gI().findItem(player,
                                                            ConstItem.PHU_GIA_TAO_MAU, 1);
                                                    Item giaVi3 = InventoryService.gI().findItem(player,
                                                            ConstItem.GIA_VI_TONG_HOP, 1);

                                                    if (banhChung3 != null && phuGiaTaoMau3 != null && giaVi3 != null) {
                                                        InventoryService.gI().subQuantityItemsBag(player, banhChung3,
                                                                1);
                                                        InventoryService.gI().subQuantityItemsBag(player, phuGiaTaoMau3,
                                                                1);
                                                        InventoryService.gI().subQuantityItemsBag(player, giaVi3, 1);
                                                        InventoryService.gI().sendItemBags(player);
                                                        player.event.setTimeCookChungCake(300);
                                                        player.event.setCookingChungCake(true);
                                                        Service.getInstance().sendThongBao(player,
                                                                "Bắt đầu nấu bánh,thời gian nấu bánh là 5 phút");
                                                    } else {
                                                        Service.getInstance().sendThongBao(player,
                                                                "Không đủ nguyên liệu");
                                                    }
                                                } else if (player.event.isCookingChungCake()
                                                        && player.event.getTimeCookChungCake() == 0) {
                                                    Item cake = ItemService.gI()
                                                            .createNewItem((short) ConstItem.BANH_CHUNG_CHIN, 1);
                                                    cake.itemOptions.add(new ItemOption(50, 20));
                                                    cake.itemOptions.add(new ItemOption(5, 15));
                                                    cake.itemOptions.add(new ItemOption(74, 0));
                                                    InventoryService.gI().addItemBag(player, cake, 0);
                                                    InventoryService.gI().sendItemBags(player);
                                                    player.event.setCookingChungCake(false);
                                                    player.event.addEventPoint(1);
                                                    Service.getInstance().sendThongBao(player,
                                                            "Bạn nhận được Bánh Chưng (đã chín) và 1 điểm sự kiện");
                                                }
                                                break;
                                            case 4:
                                                Item tetCake = InventoryService.gI().findItem(player,
                                                        ConstItem.BANH_TET_CHIN, 5);
                                                Item chungCake = InventoryService.gI().findItem(player,
                                                        ConstItem.BANH_CHUNG_CHIN, 5);
                                                if (chungCake != null && tetCake != null) {
                                                    Item hopQua = ItemService.gI()
                                                            .createNewItem((short) ConstItem.HOP_QUA_TET_2023, 1);
                                                    hopQua.itemOptions.add(new ItemOption(30, 0));
                                                    hopQua.itemOptions.add(new ItemOption(74, 0));

                                                    InventoryService.gI().subQuantityItemsBag(player, tetCake, 5);
                                                    InventoryService.gI().subQuantityItemsBag(player, chungCake, 5);
                                                    InventoryService.gI().addItemBag(player, hopQua, 0);
                                                    InventoryService.gI().sendItemBags(player);
                                                    Service.getInstance().sendThongBao(player,
                                                            "Bạn nhận được Hộp quà tết");
                                                } else {
                                                    Service.getInstance().sendThongBao(player,
                                                            "Không đủ nguyên liệu để đổi");
                                                }
                                                break;
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.KING_FURY:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                        "Cửa hàng của chúng tôi chuyên mua bán hàng hiệu, hàng độc\n"
                                        + "Cám ơn bạn đã ghé thăm.", "Hướng\ndẫn\nthêm", "Mua bán\nKý gửi\nSự kiện", "Từ chối");
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                switch (player.iDMark.getIndexMenu()) {
                                    case ConstNpc.BASE_MENU:
                                        switch (select) {
                                            case 0:
                                                NpcService.gI().createTutorial(player, avartar, "Cửa hàng chuyên nhận ký gửi mua bán vật phẩm\b"
                                                        + "Chỉ với 1 ngọc và 5% phí ký gửi\b"
                                                        + "Giá trị ký gửi 100k-1 Tỉ vàng hoặc 2-2k ngọc\b"
                                                        + "Một người bán, vạn người mua, mại dô, mại dô");
                                                break;
                                            case 1:
                                                this.npcChat(player, "Sức mạnh của con phải ít nhất phải đạt "
                                                        + Util.numberToMoney(BanDoKhoBau.POWER_CAN_GO_TO_DBKB));
                                                // ConsignmentShop.getInstance().show(player);
                                                return;
                                        }
                                        break;
                                }
                            }
                        }
                    };
                    break;
                case ConstNpc.CUA_HANG_KY_GUI: {
                    npc = new CuaHangKyGui(mapId, status, cx, cy, tempId, avartar);
                    break;
                }

                default:
                    npc = new Npc(mapId, status, cx, cy, tempId, avartar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                super.openBaseMenu(player);
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                                //   ShopService.gI().openShopNormal(player, this, ConstNpc.SHOP_BUNMA_TL_0, 0,
                                //   player.gender);
                            }
                        }
                    };
            }
        } catch (Exception e) {
            Log.error(NpcFactory.class,
                    e, "Lỗi load npc");
        }
        return npc;
    }
// girlkun75-mark

    public static void createNpcRongThieng() {
        Npc npc = new Npc(-1, -1, -1, -1, ConstNpc.RONG_THIENG, -1) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.IGNORE_MENU:

                        break;
                    case ConstNpc.SHENRON_CONFIRM:
                        if (select == 0) {
                            SummonDragon.gI().confirmWish();
                        } else if (select == 1) {
                            SummonDragon.gI().reOpenShenronWishes(player);
                        }
                        break;
                    case ConstNpc.SHENRON_1_1:
                        if (player.iDMark.getIndexMenu() == ConstNpc.SHENRON_1_1
                                && select == SHENRON_1_STAR_WISHES_1.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_2, SHENRON_SAY,
                                    SHENRON_1_STAR_WISHES_2);
                            break;
                        }
                    case ConstNpc.SHENRON_1_2:
                        if (player.iDMark.getIndexMenu() == ConstNpc.SHENRON_1_2
                                && select == SHENRON_1_STAR_WISHES_2.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_1, SHENRON_SAY,
                                    SHENRON_1_STAR_WISHES_1);
                            break;
                        }
                    case ConstNpc.BLACK_SHENRON:
                        if (player.iDMark.getIndexMenu() == ConstNpc.BLACK_SHENRON
                                && select == BLACK_SHENRON_WISHES.length) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.BLACK_SHENRON, BLACK_SHENRON_SAY,
                                    BLACK_SHENRON_WISHES);
                            break;
                        }
                    case ConstNpc.ICE_SHENRON:
                        if (player.iDMark.getIndexMenu() == ConstNpc.ICE_SHENRON
                                && select == ICE_SHENRON_WISHES.length) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.ICE_SHENRON, ICE_SHENRON_SAY,
                                    ICE_SHENRON_WISHES);
                            break;
                        }
                    default:
                        SummonDragon.gI().showConfirmShenron(player, player.iDMark.getIndexMenu(), (byte) select);
                        break;
                }
            }
        };
    }

    public static void createNpcConMeo() {
        Npc npc = new Npc(-1, -1, -1, -1, ConstNpc.CON_MEO, 351) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.iDMark.getIndexMenu()) {

                    case ConstNpc.CONFIRM_DIALOG:
                        ConfirmDialog confirmDialog = player.getConfirmDialog();
                        if (confirmDialog != null) {
                            if (confirmDialog instanceof MenuDialog menu) {
                                menu.getRunable().setIndexSelected(select);
                                menu.run();
                                return;
                            }
                            if (select == 0) {
                                confirmDialog.run();
                            } else {
                                confirmDialog.cancel();
                            }
                            player.setConfirmDialog(null);
                        }
                        break;
                    case 25100303:
                        switch (select) {
                            case 0:
                                PlayerDAO.saveMaBaoVe(player, player.MaBaoVe_TamThoi);
                                PlayerDAO.Bat_Tat_MaBaoVe(player, select);
                                player.MaBaoVe = player.MaBaoVe_TamThoi;
                                player.isUseMaBaoVe = true;
                                Service.getInstance().sendThongBao(player, "Kích hoạt thành công, tài khoản đang được bảo vệ");
                                PlayerService.gI().savePlayer(player);
                                break;
                            case 1:
                                break;
                        }
                        break;
                    case 25100304:
                        switch (select) {
                            case 0:
                                PlayerDAO.Bat_Tat_MaBaoVe(player, 1);
                                player.isUseMaBaoVe = false;
                                PlayerService.gI().savePlayer(player);
                                Service.getInstance().sendThongBao(player, "Chức năng bảo vệ tài khoản đang tắt");
                                break;
                            case 1:
                                break;
                        }
                        break;
                    case 25100305:
                        switch (select) {
                            case 0:
                                PlayerDAO.Bat_Tat_MaBaoVe(player, 0);
                                player.isUseMaBaoVe = true;
                                PlayerService.gI().savePlayer(player);
                                Service.getInstance().sendThongBao(player, "Tài khoản đang được bảo vệ");
                                break;
                            case 1:
                                break;
                        }
                        break;
                    case ConstNpc.HOP_QUA_THAN_LINH:

                        Item aotl_td = ItemService.gI().createNewItem((short) 555);
                        Item aotl_nm = ItemService.gI().createNewItem((short) 557);
                        Item aotl_xd = ItemService.gI().createNewItem((short) 559);

                        aotl_td.itemOptions.add(new ItemOption(47, 800 + new Random().nextInt(200)));

                        aotl_nm.itemOptions.add(new ItemOption(47, 900 + new Random().nextInt(100)));

                        aotl_xd.itemOptions.add(new ItemOption(47, 950 + new Random().nextInt(200)));

                        aotl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        aotl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        aotl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        aotl_td.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        aotl_nm.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        aotl_xd.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ

                        Item quantl_td = ItemService.gI().createNewItem((short) 556);
                        Item quantl_nm = ItemService.gI().createNewItem((short) 558);
                        Item quantl_xd = ItemService.gI().createNewItem((short) 560);

                        quantl_td.itemOptions.add(new ItemOption(22, 47 + new Random().nextInt(5)));
                        quantl_td.itemOptions.add(new ItemOption(27, (47 + new Random().nextInt(5)) * 1000 * 15 / 100));

                        quantl_nm.itemOptions.add(new ItemOption(22, 45 + new Random().nextInt(5)));
                        quantl_nm.itemOptions.add(new ItemOption(27, (45 + new Random().nextInt(5)) * 1000 * 15 / 100));

                        quantl_xd.itemOptions.add(new ItemOption(22, 42 + new Random().nextInt(8)));
                        quantl_xd.itemOptions.add(new ItemOption(27, (42 + new Random().nextInt(8)) * 1000 * 15 / 100));

                        quantl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        quantl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        quantl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        quantl_td.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        quantl_nm.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        quantl_xd.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ

                        Item gangtl_td = ItemService.gI().createNewItem((short) 562);
                        Item gangtl_nm = ItemService.gI().createNewItem((short) 564);
                        Item gangtl_xd = ItemService.gI().createNewItem((short) 566);

                        gangtl_td.itemOptions.add(new ItemOption(0, 3500 + new Random().nextInt(1200)));
                        gangtl_nm.itemOptions.add(new ItemOption(0, 3300 + new Random().nextInt(1100)));
                        gangtl_xd.itemOptions.add(new ItemOption(0, 3500 + new Random().nextInt(1400)));

                        gangtl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        gangtl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        gangtl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        gangtl_td.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        gangtl_nm.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        gangtl_xd.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ

                        Item giaytl_td = ItemService.gI().createNewItem((short) 563);
                        Item giaytl_nm = ItemService.gI().createNewItem((short) 565);
                        Item giaytl_xd = ItemService.gI().createNewItem((short) 567);

                        giaytl_td.itemOptions.add(new ItemOption(23, 42 + new Random().nextInt(5)));
                        giaytl_nm.itemOptions.add(new ItemOption(23, 47 + new Random().nextInt(5)));
                        giaytl_xd.itemOptions.add(new ItemOption(23, 45 + new Random().nextInt(4)));

                        giaytl_td.itemOptions.add(new ItemOption(28, (42 + new Random().nextInt(5)) * 1000 * 15 / 100));
                        giaytl_nm.itemOptions.add(new ItemOption(28, (47 + new Random().nextInt(5)) * 1000 * 15 / 100));
                        giaytl_xd.itemOptions.add(new ItemOption(28, (45 + new Random().nextInt(4)) * 1000 * 15 / 100));

                        giaytl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        giaytl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        giaytl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        giaytl_td.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        giaytl_nm.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ
                        giaytl_xd.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ

                        Item nhan = ItemService.gI().createNewItem((short) 561);

                        nhan.itemOptions.add(new ItemOption(14, 14 + new Random().nextInt(4)));
                        nhan.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        nhan.itemOptions.add(new ItemOption(30, 1)); // ycsm 18 tỉ

                        Item HopQuaThanLinh = InventoryService.gI().findItemBagByTemp(player, 1280);

                        switch (select) {

                            case 0:
                                if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                                    Service.getInstance().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                                    return;
                                }
                                InventoryService.gI().addItemBag(player, aotl_td, 1);
                                InventoryService.gI().addItemBag(player, quantl_td, 1);
                                InventoryService.gI().addItemBag(player, gangtl_td, 1);
                                InventoryService.gI().addItemBag(player, giaytl_td, 1);
                                InventoryService.gI().addItemBag(player, nhan, 1);
                                InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 set thần linh trái đất");
                                return;
                            case 1:
                                if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                                    Service.getInstance().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                                    return;
                                }

                                InventoryService.gI().addItemBag(player, aotl_nm, 1);
                                InventoryService.gI().addItemBag(player, quantl_nm, 1);
                                InventoryService.gI().addItemBag(player, gangtl_nm, 1);
                                InventoryService.gI().addItemBag(player, giaytl_nm, 1);
                                InventoryService.gI().addItemBag(player, nhan, 1);
                                InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 set thần linh namek");
                                InventoryService.gI().sendItemBags(player);
                                return;
                            case 2:
                                if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                                    Service.getInstance().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                                    return;
                                }

                                InventoryService.gI().addItemBag(player, aotl_xd, 1);
                                InventoryService.gI().addItemBag(player, quantl_xd, 1);
                                InventoryService.gI().addItemBag(player, gangtl_xd, 1);
                                InventoryService.gI().addItemBag(player, giaytl_xd, 1);
                                InventoryService.gI().addItemBag(player, nhan, 1);
                                InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                                InventoryService.gI().sendItemBags(player);

                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 set thần linh xayda");
                                return;
                        }
                        return;
                    case ConstNpc.UP_TOP_ITEM:

                        break;
                    case ConstNpc.RUONG_GO:
                        int size = player.textRuongGo.size();
                        if (size > 0) {
                            String menuselect = "OK [" + (size - 1) + "]";
                            if (size == 1) {
                                menuselect = "OK";
                            }
                            NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1,
                                    player.textRuongGo.get(size - 1), menuselect);
                            player.textRuongGo.remove(size - 1);
                        }
                        break;
                    case ConstNpc.MENU_MABU_WAR:
                        if (select == 0) {
                            if (player.zone.finishMabuWar) {
                                ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                            } else if (player.zone.map.mapId == 119) {
                                Zone zone = MabuWar.gI().getMapLastFloor(120);
                                if (zone != null) {
                                    ChangeMapService.gI().changeMap(player, zone, 354, 240);
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Trận đại chiến đã kết thúc, tàu vận chuyển sẽ đưa bạn về nhà");
                                    ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                                }
                            } else {
                                int idMapNextFloor = player.zone.map.mapId == 115 ? player.zone.map.mapId + 2
                                        : player.zone.map.mapId + 1;
                                ChangeMapService.gI().changeMap(player, idMapNextFloor, -1, 354, 240);
                            }
                            player.resetPowerPoint();
                            player.sendMenuGotoNextFloorMabuWar = false;
                            Service.getInstance().sendPowerInfo(player, "TL", player.getPowerPoint());
                            if (Util.isTrue(1, 30)) {
                                player.inventory.addRuby(1); // FIX
                                PlayerService.gI().sendInfoHpMpMoney(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 Hồng Ngọc");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Bạn đen vô cùng luôn nên không nhận được gì cả");
                            }
                        }
                        break;
                    case ConstNpc.IGNORE_MENU:

                        break;
                    case ConstNpc.MAKE_MATCH_PVP:
                        // PVP_old.gI().sendInvitePVP(player, (byte) select);
                        PVPServcice.gI().sendInvitePVP(player, (byte) select);
                        break;
                    case ConstNpc.MAKE_FRIEND:
                        if (select == 0) {
                            Object playerId = PLAYERID_OBJECT.get(player.id);
                            if (playerId != null) {
                                FriendAndEnemyService.gI().acceptMakeFriend(player,
                                        Integer.parseInt(String.valueOf(playerId)));
                            }
                        }
                        break;
                    case ConstNpc.REVENGE:
                        if (select == 0) {
                            PVPServcice.gI().acceptRevenge(player);
                        }
                        break;
                    case ConstNpc.TUTORIAL_SUMMON_DRAGON:
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        }
                        break;
                    case ConstNpc.SUMMON_SHENRON:
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        } else if (select == 1) {
                            SummonDragon.gI().summonShenron(player);
                        }
                        break;
                    case ConstNpc.SUMMON_BLACK_SHENRON:
                        if (select == 0) {
                            SummonDragon.gI().summonBlackShenron(player);
                        }
                        break;
                    case ConstNpc.SUMMON_ICE_SHENRON:
                        if (select == 0) {
                            SummonDragon.gI().summonIceShenron(player);
                        }
                        break;
                    case ConstNpc.INTRINSIC:
                        if (select == 0) {
                            IntrinsicService.gI().showAllIntrinsic(player);
                        } else if (select == 1) {
                            IntrinsicService.gI().showConfirmOpen(player);
                        } else if (select == 2) {
                            IntrinsicService.gI().showConfirmOpenVip(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC:
                        if (select == 0) {
                            IntrinsicService.gI().open(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP:
                        if (select == 0) {
                            IntrinsicService.gI().openVip(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_LEAVE_CLAN:
                        if (select == 0) {
                            ClanService.gI().leaveClan(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_NHUONG_PC:
                        if (select == 0) {
                            ClanService.gI().phongPc(player, (int) PLAYERID_OBJECT.get(player.id));
                        }
                        break;
                    case ConstNpc.BAN_PLAYER:
                        if (select == 0) {
                            PlayerService.gI().banPlayer((Player) PLAYERID_OBJECT.get(player.id));
                            Service.getInstance().sendThongBao(player,
                                    "Ban người chơi " + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                        }
                        break;
                    case ConstNpc.BUFF_PET:
                        if (select == 0) {
                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                            if (pl.pet == null) {
                                PetService.gI().createNormalPet(pl);
                                Service.getInstance().sendThongBao(player, "Phát đệ tử cho "
                                        + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                            }
                        }
                        break;
                    case ConstNpc.TAIXIU:
                        String time = ((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) + " giây";
                        if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) > 0 && player.goldTai == 0 && player.goldXiu == 0 && TaiXiu.gI().baotri == false) {
                            switch (select) {
                                case 0:
                                    int ketqua = TaiXiu.gI().z + TaiXiu.gI().y + TaiXiu.gI().x;
                                    NpcService.gI().createMenuConMeo(player, ConstNpc.TAIXIU, 11039, "\n|7|---Trò chơi may mắn---\n"
                                            + "\n|3|Kết quả kì trước:  " + TaiXiu.gI().x + " : " + TaiXiu.gI().y + " : " + TaiXiu.gI().z + " " + (ketqua >= 10 ? "Tài" : "Xỉu")
                                            + "\n|1|Kết quả kì trước" + "\n"
                                            + "|3| " + TaiXiu.gI().tongHistoryString
                                            + "\n\n|1|Tổng Cược TÀI: " + Util.format(TaiXiu.gI().goldTai) + " Hồng ngọc"
                                            + "\n\n|1|Tổng Cược XỈU: " + Util.format(TaiXiu.gI().goldXiu) + " Hồng ngọc\n"
                                            + "\n|5|Đếm ngược: " + time, "Cập nhập", "Cược\n'Tài'", "Cược\n'Xỉu' ", "Đóng");
                                    break;
                                case 1:
                                    if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_18_0) {
                                        Input.gI().TAI_taixiu(player);
                                    } else {
                                        Service.getInstance().sendThongBao(player, "Bạn chưa đủ điều kiện để chơi");
                                    }
                                    break;
                                case 2:
                                    if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_18_0) {
                                        Input.gI().XIU_taixiu(player);
                                    } else {
                                        Service.getInstance().sendThongBao(player, "Bạn chưa đủ điều kiện để chơi");
                                    }
                                    break;
                            }
                        } else if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) > 0 && player.goldTai > 0 && TaiXiu.gI().baotri == false) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.TAIXIU, "\n|7|---Trò chơi may mắn---\n"
                                            + "\n|3|Kết quả kì trước:  " + TaiXiu.gI().x + " : " + TaiXiu.gI().y + " : " + TaiXiu.gI().z
                                            + "\n\n|1|Tổng nhà 'Tài'=> " + Util.format(TaiXiu.gI().goldTai) + " Hồng ngọc"
                                            + "\n\n|1|Tổng nhà 'Xỉu'=> " + Util.format(TaiXiu.gI().goldXiu) + " Hồng ngọc\n"
                                            + "\n|5|Thời gian còn lại: " + time, "Cập nhập", "Cược\n'Tài'", "Cược\n'Xỉu' ", "Đóng");
                                    break;
                            }
                        } else if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) > 0 && player.goldXiu > 0 && TaiXiu.gI().baotri == false) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.TAIXIU, "\n|7|---Trò chơi may mắn---\n"
                                            + "\n|3|Kết quả kì trước:  " + TaiXiu.gI().x + " : " + TaiXiu.gI().y + " : " + TaiXiu.gI().z
                                            + "\n\n|1|Tổng nhà 'Tài'=> " + Util.format(TaiXiu.gI().goldTai) + " Hồng ngọc"
                                            + "\n\n|1|Tổng nhà 'Xỉu'=> " + Util.format(TaiXiu.gI().goldXiu) + " Hồng ngọc\n"
                                            + "\n|5|Thời gian còn lại: " + time, "Cập nhập", "Cược\n'Tài'", "Cược\n'Xỉu' ", "Đóng");
                                    break;
                            }
                        } else if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) > 0 && player.goldTai > 0 && TaiXiu.gI().baotri == true) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.TAIXIU, "\n|7|---Trò chơi may mắn---\n"
                                            + "\n|3|Kết quả kì trước:  " + TaiXiu.gI().x + " : " + TaiXiu.gI().y + " : " + TaiXiu.gI().z
                                            + "\n\n|1|Tổng nhà 'Tài'=> " + Util.format(TaiXiu.gI().goldTai) + " Hồng ngọc"
                                            + "\n\n|1|Tổng nhà 'Xỉu'=> " + Util.format(TaiXiu.gI().goldXiu) + " Hồng ngọc\n"
                                            + "\n|5|Thời gian còn lại: " + time, "Cập nhập", "Cược\n'Tài'", "Cược\n'Xỉu' ", "Đóng");
                                    break;
                            }
                        } else if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) > 0 && player.goldXiu > 0 && TaiXiu.gI().baotri == true) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.TAIXIU, "\n|7|---Trò chơi may mắn---\n"
                                            + "\n|3|Kết quả kì trước:  " + TaiXiu.gI().x + " : " + TaiXiu.gI().y + " : " + TaiXiu.gI().z
                                            + "\n\n|1|Tổng nhà 'Tài'=> " + Util.format(TaiXiu.gI().goldTai) + " Hồng ngọc"
                                            + "\n\n|1|Tổng nhà 'Xỉu'=> " + Util.format(TaiXiu.gI().goldXiu) + " Hồng ngọc\n"
                                            + "\n|5|Thời gian còn lại: " + time, "Cập nhập", "Cược\n'Tài'", "Cược\n'Xỉu' ", "Đóng");
                                    break;
                            }
                        } else if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) > 0 && player.goldXiu == 0 && player.goldTai == 0 && TaiXiu.gI().baotri == true) {
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, ConstNpc.TAIXIU, "\n|7|---Trò chơi may mắn---\n"
                                            + "\n|3|Kết quả kì trước:  " + TaiXiu.gI().x + " : " + TaiXiu.gI().y + " : " + TaiXiu.gI().z
                                            + "\n\n|1|Tổng nhà 'Tài'=> " + Util.format(TaiXiu.gI().goldTai) + " Hồng ngọc"
                                            + "\n\n|1|Tổng nhà 'Xỉu'=> " + Util.format(TaiXiu.gI().goldXiu) + " Hồng ngọc\n"
                                            + "\n|5|Thời gian còn lại: " + time, "Cập nhập", "Cược\n'Tài'", "Cược\n'Xỉu' ", "Đóng");
                                    break;
                            }
                        }
                        break;

                    case ConstNpc.MENU_ADMIN:
                        switch (select) {
                            case 0:
                                for (int i = 14; i <= 20; i++) {
                                    Item item = ItemService.gI().createNewItem((short) i);
                                    InventoryService.gI().addItemBag(player, item, 0);
                                }
                                InventoryService.gI().sendItemBags(player);
                                break;
                            case 1:
                                if (player.pet == null) {
                                    PetService.gI().createNormalPet(player);
                                } else {
                                    if (player.pet.isMabu) {
                                        PetService.gI().changeNormalPet(player);
                                    } else {
                                        PetService.gI().changeMabuPet(player);
                                    }
                                    PetService.gI().changeSuperPet(player, player.gender, 1);
                                }
                                break;
                            case 2:
                                Maintenance.gI().start(60);
                                break;
                            case 3:
                                Input.gI().createFormFindPlayer(player);
                                break;
                            case 4:
                                NotiManager.getInstance().load();
                                NotiManager.getInstance().sendAlert(player);
                                NotiManager.getInstance().sendNoti(player);
                                Service.getInstance().chat(player, "Cập nhật thông báo thành công");
                                break;
                            case 5:
                                //    NotiManager.getInstance().load();
                                //    NotiManager.getInstance().sendAlert(player);
                                //    NotiManager.getInstance().sendNoti(player);
                                //   Service.getInstance().chat(player, "Cập nhật thông báo thành công");
                                this.createOtherMenu(player, ConstNpc.CALL_BOSS,
                                        "Mời Bạn Chọn?", "Android", "ppkk", "broly");
                                break;
                        }
                        break;
                    case ConstNpc.CALL_BOSS:
                        switch (select) {
                            case 0:
                                BossFactory.createBoss(BossFactory.ANDROID_13);
                                BossFactory.createBoss(BossFactory.ANDROID_14);
                                BossFactory.createBoss(BossFactory.ANDROID_15);
                                BossFactory.createBoss(BossFactory.ANDROID_19);
                                BossFactory.createBoss(BossFactory.ANDROID_20);
                                break;
                            case 1:
                                BossFactory.createBoss(BossFactory.KINGKONG);
                                BossFactory.createBoss(BossFactory.PIC);
                                BossFactory.createBoss(BossFactory.POC);
                                break;
                            case 2:
                                BossFactory.createBoss(BossFactory.BROLY);

                                BossFactory.createBoss(BossFactory.BLACKGOKU);

                                BossFactory.createBoss(BossFactory.XEN_BO_HUNG_1);
                                BossFactory.createBoss(BossFactory.FIDE_DAI_CA_1);
                                BossFactory.createBoss(BossFactory.SO1);
                                BossFactory.createBoss(BossFactory.KUKU);
                                BossFactory.createBoss(BossFactory.MAP_DAU_DINH);
                                BossFactory.createBoss(BossFactory.RAMBO);
                                BossFactory.createBoss(BossFactory.CUMBER);
                                BossFactory.createBoss(BossFactory.MABU_MAP);
                                BossFactory.createBoss(BossFactory.SUPER_BU);
                                BossFactory.createBoss(BossFactory.NGO_KHONG);
                                BossFactory.createBoss(BossFactory.BAT_GIOI);
                                BossFactory.createBoss(BossFactory.KID_BU);
                                BossFactory.createBoss(BossFactory.BU_HAN);
                                BossFactory.createBoss(BossFactory.MABU_MAP2);
                                break;
                        }
                        break;
                    case ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND:
                        if (select == 0) {
                            for (int i = 0; i < player.inventory.itemsBoxCrackBall.size(); i++) {
                                player.inventory.itemsBoxCrackBall.set(i, ItemService.gI().createItemNull());
                            }
                            Service.getInstance().sendThongBao(player, "Đã xóa hết vật phẩm trong rương");
                        }
                        break;
                    case ConstNpc.MENU_FIND_PLAYER:
                        Player p = (Player) PLAYERID_OBJECT.get(player.id);
                        if (p != null) {
                            switch (select) {
                                case 0:
                                    if (p.zone != null) {
                                        ChangeMapService.gI().changeMapYardrat(player, p.zone, p.location.x,
                                                p.location.y);
                                    }
                                    break;
                                case 1:
                                    if (p.zone != null) {
                                        ChangeMapService.gI().changeMap(p, player.zone, player.location.x,
                                                player.location.y);
                                    }
                                    break;
                                case 2:
                                    if (p != null) {
                                        Input.gI().createFormChangeName(player, p);
                                    }
                                    break;
                                case 3:
                                    if (p != null) {
                                        String[] selects = new String[]{"Đồng ý", "Hủy"};
                                        NpcService.gI().createMenuConMeo(player, ConstNpc.BAN_PLAYER, -1,
                                                "Bạn có chắc chắn muốn ban " + p.name, selects, p);
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
        };
    }

    public static void processGemPurchase(Player player, int requiredVndBar, int gemAmount) {
        if (player.soDuVND >= requiredVndBar) {
            player.inventory.addGem(gemAmount); // FIX
            player.soDuVND -= requiredVndBar;
            PlayerDAO.subVndBar(player, requiredVndBar);
            Service.getInstance().sendMoney(player);
            Service.getInstance().sendThongBao(player, "Bạn có thêm " + Util.mumberToLouis(gemAmount) + " ngọc xanh");
        } else {
            Service.getInstance().sendThongBao(player, "Bạn không đủ số dư");
        }
    }

    public static void processThoiVangPurchase(Player player, int requiredVndBar, int gemAmount) {
        if (player.soDuVND >= requiredVndBar) {
            player.soDuVND -= requiredVndBar;
            player.soThoiVang += gemAmount;
            PlayerDAO.subVndBar(player, requiredVndBar);
            PlayerDAO.addGoldBar(player, gemAmount);
            Service.getInstance().sendThongBao(player, "Bạn có thêm " + Util.mumberToLouis(gemAmount) + " thỏi vàng");
            int soHop = 0;
            switch (requiredVndBar) {
                case 20000:
                    soHop = 1;
                    break;
                case 30000:
                    soHop = 1;
                    break;
                case 50000:
                    soHop = 3;
                    break;
                case 100000:
                    soHop = 6;
                    break;
                case 200000:
                    soHop = 12;
                    break;
                case 500000:
                    soHop = 30;
                case 1000000:
                    soHop = 60;
            }
            Item hopThoiKhong = ItemService.gI().createNewItem((short) 1318, soHop);
            InventoryService.gI().addItemBag(player, hopThoiKhong, 9999);
            InventoryService.gI().sendItemBags(player);
            Service.getInstance().sendThongBao(player, "Bạn nhận được " + soHop + " " + hopThoiKhong.getName());
            return;

        } else {
            Service.getInstance().sendThongBao(player, "Bạn không đủ số dư");
        }
    }

    public static void openMenuSuKien(Player player, Npc npc, int tempId, int select) {
        switch (Manager.EVENT_SEVER) {
            case 0:
                break;
            case 1:// hlw
                switch (select) {
                    case 0:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item keo = InventoryService.gI().finditemnguyenlieuKeo(player);
                            Item banh = InventoryService.gI().finditemnguyenlieuBanh(player);
                            Item bingo = InventoryService.gI().finditemnguyenlieuBingo(player);

                            if (keo != null && banh != null && bingo != null) {
                                Item GioBingo = ItemService.gI().createNewItem((short) 2016, 1);

                                // - Số item sự kiện có trong rương
                                InventoryService.gI().subQuantityItemsBag(player, keo, 10);
                                InventoryService.gI().subQuantityItemsBag(player, banh, 10);
                                InventoryService.gI().subQuantityItemsBag(player, bingo, 10);

                                GioBingo.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, GioBingo, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Đổi quà sự kiện thành công");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Vui lòng chuẩn bị x10 Nguyên Liệu Kẹo, Bánh Quy, Bí Ngô để đổi vật phẩm sự kiện");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    case 1:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item ve = InventoryService.gI().finditemnguyenlieuVe(player);
                            Item giokeo = InventoryService.gI().finditemnguyenlieuGiokeo(player);

                            if (ve != null && giokeo != null) {
                                Item Hopmaquy = ItemService.gI().createNewItem((short) 2017, 1);
                                // - Số item sự kiện có trong rương
                                InventoryService.gI().subQuantityItemsBag(player, ve, 3);
                                InventoryService.gI().subQuantityItemsBag(player, giokeo, 3);

                                Hopmaquy.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, Hopmaquy, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Đổi quà sự kiện thành công");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Vui lòng chuẩn bị x3 Vé đổi Kẹo và x3 Giỏ kẹo để đổi vật phẩm sự kiện");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    case 2:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item ve = InventoryService.gI().finditemnguyenlieuVe(player);
                            Item giokeo = InventoryService.gI().finditemnguyenlieuGiokeo(player);
                            Item hopmaquy = InventoryService.gI().finditemnguyenlieuHopmaquy(player);

                            if (ve != null && giokeo != null && hopmaquy != null) {
                                Item HopQuaHLW = ItemService.gI().createNewItem((short) 2012, 1);
                                // - Số item sự kiện có trong rương
                                InventoryService.gI().subQuantityItemsBag(player, ve, 3);
                                InventoryService.gI().subQuantityItemsBag(player, giokeo, 3);
                                InventoryService.gI().subQuantityItemsBag(player, hopmaquy, 3);

                                HopQuaHLW.itemOptions.add(new ItemOption(74, 0));
                                HopQuaHLW.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, HopQuaHLW, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player,
                                        "Đổi quà hộp quà sự kiện Halloween thành công");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Vui lòng chuẩn bị x3 Hộp Ma Quỷ, x3 Vé đổi Kẹo và x3 Giỏ kẹo để đổi vật phẩm sự kiện");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                }
                break;
            case 2:// 20/11
                switch (select) {
                    case 3:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            int evPoint = player.event.getEventPoint();
                            if (evPoint >= 999) {
                                Item HopQua = ItemService.gI().createNewItem((short) 2021, 1);
                                player.event.setEventPoint(evPoint - 999);

                                HopQua.itemOptions.add(new ItemOption(74, 0));
                                HopQua.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, HopQua, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được Hộp Quà Teacher Day");
                            } else {
                                Service.getInstance().sendThongBao(player, "Cần 999 điểm tích lũy để đổi");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    // case 4:
                    // ShopService.gI().openShopSpecial(player, npc, ConstNpc.SHOP_HONG_NGOC, 0,
                    // -1);
                    // break;
                    default:
                        int n = 0;
                        switch (select) {
                            case 0:
                                n = 1;
                                break;
                            case 1:
                                n = 10;
                                break;
                            case 2:
                                n = 99;
                                break;
                        }

                        if (n > 0) {
                            Item bonghoa = InventoryService.gI().finditemBongHoa(player, n);
                            if (bonghoa != null) {
                                int evPoint = player.event.getEventPoint();
                                player.event.setEventPoint(evPoint + n);
                                ;
                                InventoryService.gI().subQuantityItemsBag(player, bonghoa, n);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được " + n + " điểm sự kiện");
                                int pre;
                                int next;
                                String text = null;
                                AttributeManager am = ServerManager.gI().getAttributeManager();
                                switch (tempId) {
                                    case ConstNpc.THAN_MEO_KARIN:
                                        pre = EVENT_COUNT_THAN_MEO / 999;
                                        EVENT_COUNT_THAN_MEO += n;
                                        next = EVENT_COUNT_THAN_MEO / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.TNSM, 3600);
                                            text = "Toàn bộ máy chủ tăng được 20% TNSM cho đệ tử khi đánh quái trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.QUY_LAO_KAME:
                                        pre = EVENT_COUNT_QUY_LAO_KAME / 999;
                                        EVENT_COUNT_QUY_LAO_KAME += n;
                                        next = EVENT_COUNT_QUY_LAO_KAME / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.VANG, 3600);
                                            text = "Toàn bộ máy chủ được tăng 100% vàng từ quái trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.THUONG_DE:
                                        pre = EVENT_COUNT_THUONG_DE / 999;
                                        EVENT_COUNT_THUONG_DE += n;
                                        next = EVENT_COUNT_THUONG_DE / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.KI, 3600);
                                            text = "Toàn bộ máy chủ được tăng 20% KI trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.THAN_VU_TRU:
                                        pre = EVENT_COUNT_THAN_VU_TRU / 999;
                                        EVENT_COUNT_THAN_VU_TRU += n;
                                        next = EVENT_COUNT_THAN_VU_TRU / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.HP, 3600);
                                            text = "Toàn bộ máy chủ được tăng 20% HP trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.BILL:
                                        pre = EVENT_COUNT_THAN_HUY_DIET / 999;
                                        EVENT_COUNT_THAN_HUY_DIET += n;
                                        next = EVENT_COUNT_THAN_HUY_DIET / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.SUC_DANH, 3600);
                                            text = "Toàn bộ máy chủ được tăng 20% Sức đánh trong 60 phút.";
                                        }
                                        break;
                                }
                                if (text != null) {
                                    Service.getInstance().sendThongBaoAllPlayer(text);
                                }

                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Cần ít nhất " + n + " bông hoa để có thể tặng");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Cần ít nhất " + n + " bông hoa để có thể tặng");
                        }
                }
                break;
            case 3:
                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                    Item keogiangsinh = InventoryService.gI().finditemKeoGiangSinh(player);

                    if (keogiangsinh != null && keogiangsinh.quantity >= 99) {
                        Item tatgiangsinh = ItemService.gI().createNewItem((short) 649, 1);
                        // - Số item sự kiện có trong rương
                        InventoryService.gI().subQuantityItemsBag(player, keogiangsinh, 99);

                        tatgiangsinh.itemOptions.add(new ItemOption(74, 0));
                        tatgiangsinh.itemOptions.add(new ItemOption(30, 0));
                        InventoryService.gI().addItemBag(player, tatgiangsinh, 0);
                        InventoryService.gI().sendItemBags(player);
                        Service.getInstance().sendThongBao(player, "Bạn nhận được Tất,vớ giáng sinh");
                    } else {
                        Service.getInstance().sendThongBao(player,
                                "Vui lòng chuẩn bị x99 kẹo giáng sinh để đổi vớ tất giáng sinh");
                    }
                } else {
                    Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                }
                break;
            case 4:
                switch (select) {
                    case 0:
                        if (!player.event.isReceivedLuckyMoney()) {
                            Calendar cal = Calendar.getInstance();
                            int day = cal.get(Calendar.DAY_OF_MONTH);
                            if (day >= 22 && day <= 24) {
                                Item goldBar = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG,
                                        Util.nextInt(1, 3));
                                player.inventory.addRuby(Util.nextInt(10, 30)); // FIX
                                goldBar.quantity = Util.nextInt(1, 3);
                                InventoryService.gI().addItemBag(player, goldBar, 99999);
                                InventoryService.gI().sendItemBags(player);
                                PlayerService.gI().sendInfoHpMpMoney(player);
                                player.event.setReceivedLuckyMoney(true);
                                Service.getInstance().sendThongBao(player,
                                        "Nhận lì xì thành công,chúc bạn năm mới dui dẻ");
                            } else if (day > 24) {
                                Service.getInstance().sendThongBao(player, "Hết tết rồi còn đòi lì xì");
                            } else {
                                Service.getInstance().sendThongBao(player, "Đã tết đâu mà đòi lì xì");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Bạn đã nhận lì xì rồi");
                        }
                        break;
                    case 1:
                        ShopService.gI().openShopNormal(player, npc, ConstNpc.SHOP_SU_KIEN_TET, 1, -1);
                        break;
                }
                break;
            case ConstEvent.SU_KIEN_8_3:
                switch (select) {
                    case 3:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            int evPoint = player.event.getEventPoint();
                            if (evPoint >= 999) {
                                Item capsule = ItemService.gI().createNewItem((short) 2052, 1);
                                player.event.setEventPoint(evPoint - 999);

                                capsule.itemOptions.add(new ItemOption(74, 0));
                                capsule.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, capsule, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được Capsule Hồng");
                            } else {
                                Service.getInstance().sendThongBao(player, "Cần 999 điểm tích lũy để đổi");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    default:
                        int n = 0;
                        switch (select) {
                            case 0:
                                n = 1;
                                break;
                            case 1:
                                n = 10;
                                break;
                            case 2:
                                n = 99;
                                break;
                        }

                        if (n > 0) {
                            Item bonghoa = InventoryService.gI().finditemBongHoa(player, n);
                            if (bonghoa != null) {
                                int evPoint = player.event.getEventPoint();
                                player.event.setEventPoint(evPoint + n);
                                InventoryService.gI().subQuantityItemsBag(player, bonghoa, n);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được " + n + " điểm sự kiện");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Cần ít nhất " + n + " bông hoa để có thể tặng");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Cần ít nhất " + n + " bông hoa để có thể tặng");
                        }
                }
                break;
        }
    }

    public static String getMenuSuKien(int id) {
        switch (id) {
            case ConstEvent.KHONG_CO_SU_KIEN:
                return "Chưa có\n Sự Kiện";
            case ConstEvent.SU_KIEN_HALLOWEEN:
                return "Sự Kiện\nHaloween";
            case ConstEvent.SU_KIEN_20_11:
                return "Sự Kiện\n 20/11";
            case ConstEvent.SU_KIEN_NOEL:
                return "Sự Kiện\n Giáng Sinh";
            case ConstEvent.SU_KIEN_TET:
                return "Sự Kiện\n Tết Nguyên\nĐán 2023";
            case ConstEvent.SU_KIEN_8_3:
                return "Sự Kiện\n 8/3";
        }
        return "Chưa có\n Sự Kiện";
    }

    public static String getMenuLamBanh(Player player, int type) {
        switch (type) {
            case 0:// bánh tét
                if (player.event.isCookingTetCake()) {
                    int timeCookTetCake = player.event.getTimeCookTetCake();
                    if (timeCookTetCake == 0) {
                        return "Lấy bánh";
                    } else if (timeCookTetCake > 0) {
                        return "Đang nấu\nBánh Tét\nCòn " + TimeUtil.secToTime(timeCookTetCake);
                    }
                } else {
                    return "Nấu\nBánh Tét";
                }
                break;
            case 1:// bánh chưng
                if (player.event.isCookingChungCake()) {
                    int timeCookChungCake = player.event.getTimeCookChungCake();
                    if (timeCookChungCake == 0) {
                        return "Lấy bánh";
                    } else if (timeCookChungCake > 0) {
                        return "Đang nấu\nBánh Chưng\nCòn " + TimeUtil.secToTime(timeCookChungCake);
                    }
                } else {
                    return "Nấu\nBánh Chưng";
                }
                break;
        }
        return "";
    }

}
