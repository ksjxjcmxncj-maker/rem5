package nro.models.map;

import lombok.Getter;
import lombok.Setter;
import nro.consts.ConstMap;
import nro.consts.ConstMob;
import nro.consts.ConstTask;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.map.war.NamekBallWar;
import nro.models.mob.Mob;
import nro.models.npc.Npc;
import nro.models.npc.NpcManager;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.power.CaptionManager;
import nro.server.io.Message;
import nro.services.*;
import nro.services.func.ChangeMapService;
import nro.utils.FileIO;
import nro.utils.Log;
import nro.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.FloatControl;
import nro.consts.ConstPlayer;
import nro.models.boss.BossData;
import nro.models.boss.BossManager;
import nro.models.boss.mapoffline.Boss_Tau77;
import nro.models.boss.mapoffline.Boss_Yanjiro;
import nro.models.mob.Octopus;
import nro.models.npc.NpcFactory;
import nro.sendEff.SendEffect;

import static nro.services.func.ChangeMapService.NON_SPACE_SHIP;

/**
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 */
public class Zone {

    public static final byte PLAYERS_TIEU_CHUAN_TRONG_MAP = 7;

    public int countItemAppeaerd = 0;

    public nro.models.map.Map map;
    public int zoneId;
    public int maxPlayer;
    private final List<Player> humanoids; //player, boss, pet
    private final List<Player> notBosses; //player, pet
    private final List<Player> players; //player
    private final List<Player> bosses; //boss
    private final List<Player> pets; //pet
    private final List<Player> minipets; //minpet

    public final List<Mob> mobs;
    private final List<ItemMap> items;
    @Setter
    @Getter
    private Player referee;

    public long lastTimeDropBlackBall;
    public boolean finishBlackBallWar;
    public byte percentMabuEgg;
    public boolean initBossMabu;
    public boolean finishMabuWar;

    public boolean isZoneHaveBigBoss;

    public List<TrapMap> trapMaps;
    public byte effDragon = -1;

    private static final java.util.Map<String, byte[]> fileCache = new HashMap<>();

    public Zone(nro.models.map.Map map, int zoneId, int maxPlayer) {
        this.map = map;
        this.zoneId = zoneId;
        this.maxPlayer = maxPlayer;
        this.humanoids = new ArrayList<>();
        this.notBosses = new ArrayList<>();
        this.players = new ArrayList<>();
        this.bosses = new ArrayList<>();
        this.pets = new ArrayList<>();
        this.minipets = new ArrayList<>();
        this.mobs = new ArrayList<>();
        this.items = new ArrayList<>();
        this.trapMaps = new ArrayList<>();
    }

    public boolean isFullPlayer() {
        synchronized (this.players) {
            return this.players.size() >= this.maxPlayer;
        }
    }

    public boolean isCheckKilledAll(int mapID) {
        List<Mob> snapshot;
        synchronized (mobs) {
            snapshot = new ArrayList<>(mobs);
        }
        for (Mob mob : snapshot) {
            if (mob.zone.map.mapId == mapID) {
                if (!mob.isDie()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void addMob(Mob mob) {
        synchronized (mobs) {
            mob.id = mobs.size();
            mobs.add(mob);
        }
    }

    private void updateMob() {
        List<Mob> snapshot;
        synchronized (this.mobs) {
            snapshot = new ArrayList<>(this.mobs);
        }
        for (Mob mob : snapshot) {
            mob.update();
        }
    }

    public long getTotalHP() {
        long total = 0;
        List<Mob> mobSnapshot;
        synchronized (mobs) {
            mobSnapshot = new ArrayList<>(mobs);
        }
        for (Mob mob : mobSnapshot) {
            if (!mob.isDie()) {
                total += mob.point.hp;
            }
        }
        List<Player> plSnapshot;
        synchronized (players) {
            plSnapshot = new ArrayList<>(players);
        }
        for (Player pl : plSnapshot) {
            if (pl.nPoint != null && !pl.isDie()) {
                total += pl.nPoint.hp;
            }
        }
        List<Player> petSnapshot;
        synchronized (pets) {
            petSnapshot = new ArrayList<>(pets);
        }
        for (Player pl : petSnapshot) {
            if (pl.nPoint != null && !pl.isDie()) {
                total += pl.nPoint.hp;
            }
        }
        return total;
    }

    private void updatePlayer() {
        List<Player> snapshot;
        synchronized (this.notBosses) {
            snapshot = new ArrayList<>(this.notBosses);
        }
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Player pl = snapshot.get(i);
            if (!pl.isPet && !pl.isMiniPet && !pl.isClone) {
                pl.update();
            }
        }
    }

    private void updateReferee() {
        if (referee != null) {
            referee.update();
        }
    }

    private void updateItem() {
        List<ItemMap> snapshot;
        synchronized (items) {
            snapshot = new ArrayList<>(items);
        }
        for (ItemMap item : snapshot) {
            item.update();
        }
    }

    public void update() {
        updateMob();
        updatePlayer();
        updateItem();
        if (map.mapId == ConstMap.DAI_HOI_VO_THUAT) {
            updateReferee();
        }
    }

    public int getNumOfPlayers() {
        synchronized (this.players) {
            return this.players.size();
        }
    }

    public int getNumOfBosses() {
        synchronized (this.bosses) {
            return this.bosses.size();
        }
    }

    public boolean isBossCanJoin(Boss boss) {
        List<Player> snapshot;
        synchronized (this.bosses) {
            snapshot = new ArrayList<>(this.bosses);
        }
        for (Player b : snapshot) {
            if (b.id == boss.id) {
                return false;
            }
        }
        return true;
    }

    public List<Player> getNotBosses() {
        return this.notBosses;
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public List<Player> getHumanoids() {
        return this.humanoids;
    }

    public List<Player> getBosses() {
        return this.bosses;
    }

    public void addPlayer(Player player) {
        if (player != null) {
            synchronized (humanoids) {
                if (!this.humanoids.contains(player)) {
                    this.humanoids.add(player);
                }
            }
            if (!player.isBoss) {
                synchronized (notBosses) {
                    if (!this.notBosses.contains(player)) {
                        this.notBosses.add(player);
                    }
                }
                if (player.isPet) {
                    synchronized (pets) {
                        this.pets.add(player);
                    }
                } else if (player.isMiniPet) {
                    synchronized (minipets) {
                        this.minipets.add(player);
                    }
                } else {
                    synchronized (players) {
                        if (!this.players.contains(player)) {
                            this.players.add(player);
                        }
                    }
                }
            } else {
                synchronized (bosses) {
                    this.bosses.add(player);
                }
            }

        }
    }

    public void removePlayer(Player player) {
        if (player != null) {
            synchronized (humanoids) {
                this.humanoids.remove(player);
            }
            if (!player.isBoss) {
                synchronized (notBosses) {
                    this.notBosses.remove(player);
                }
                if (player.isPet) {
                    synchronized (pets) {
                        this.pets.remove(player);
                    }
                } else if (player.isMiniPet) {
                    synchronized (minipets) {
                        this.minipets.remove(player);
                    }
                } else {
                    synchronized (players) {
                        this.players.remove(player);
                    }
                }
            } else {
                synchronized (bosses) {
                    this.bosses.remove(player);
                }

            }
        }

    }

    public ItemMap getItemMapByItemMapId(int itemId) {
        List<ItemMap> snapshot;
        synchronized (items) {
            snapshot = new ArrayList<>(this.items);
        }
        for (ItemMap item : snapshot) {
            if (item.itemMapId == itemId) {
                return item;
            }
        }
        return null;
    }

    public ItemMap getItemMapByTempId(int tempId) {
        List<ItemMap> snapshot;
        synchronized (items) {
            snapshot = new ArrayList<>(this.items);
        }
        for (ItemMap item : snapshot) {
            if (item.itemTemplate != null && item.itemTemplate.id == tempId) {
                return item;
            }
        }
        return null;
    }

    public List<ItemMap> getItemMapsForPlayer(Player player) {
        List<ItemMap> list = new ArrayList<>();
        List<ItemMap> snapshot;
        synchronized (items) {
            snapshot = new ArrayList<>(items);
        }
        for (ItemMap item : snapshot) {
            if (item instanceof NamekBall ball) {
                if (ball.isHolding()) {
                    continue;
                }
            }
            if (item != null && item.itemTemplate != null) {
                if (item.itemTemplate.id == 78) {
                    if (TaskService.gI().getIdTask(player) != ConstTask.TASK_3_1) {
                        continue;
                    }
                }
                if (item.itemTemplate.id == 74) {
                    if (TaskService.gI().getIdTask(player) < ConstTask.TASK_3_0) {
                        continue;
                    }
                }
                list.add(item);
            }
        }
        return list;
    }

    public List<Player> getPlayersSameClan(int clanID) {
        List<Player> list = new ArrayList<>();
        List<Player> snapshot;
        synchronized (this.players) {
            snapshot = new ArrayList<>(this.players);
        }
        for (Player pl : snapshot) {
            if (pl.clan != null
                    && pl.clan.id == clanID) {
                list.add(pl);
            }
        }
        return list;
    }

    public void pickItem(Player player, int itemMapId) {
        ItemMap itemMap = getItemMapByItemMapId(itemMapId);
        if (itemMap instanceof Satellite) {
            return;
        }
        if (itemMap != null && !itemMap.isPickedUp) {
            synchronized (itemMap) {
                if (!itemMap.isPickedUp) {
                    if (itemMap.playerId == player.id || itemMap.playerId == -1) {
                        if (itemMap.itemTemplate != null && itemMap.itemTemplate.id == 648) {
                            Item item = InventoryService.gI().findItemBagByTemp(player, 649);
                            if (item == null) {
                                Service.getInstance().sendThongBao(player, "Bạn không có Tất,vớ giáng sinh để đựng quà.");
                                return;
                            }
                            itemMap.options.add(new ItemOption(74, 0));
                            InventoryService.gI().subQuantityItemsBag(player, item, 1);
                            InventoryService.gI().sendItemBags(player);
                        }
                        if (itemMap instanceof NamekBall ball) {
                            NamekBallWar.gI().pickBall(player, ball);
                            return;
                        }
                        Item item = ItemService.gI().createItemFromItemMap(itemMap);
                        int maxQuantity = 0;
                        if (item.template != null && ItemService.gI().isItemNoLimitQuantity(item.template.id)) {
                            maxQuantity = 99999;
                        }
                        boolean picked = InventoryService.gI().addItemBag(player, item, maxQuantity);
                        if (picked) {
                            if (itemMap.itemTemplate != null && itemMap.itemTemplate.id != 74) {
                                itemMap.isPickedUp = true;
                            }
                            int itemType = item.template != null ? item.template.type : -1;
                            Message msg;
                            try {
                                msg = new Message(-20);
                                msg.writer().writeShort(itemMapId);
                                switch (itemType) {
                                    case 9:
                                    case 10:
                                    case 34:
                                        msg.writer().writeUTF("");
                                        PlayerService.gI().sendInfoHpMpMoney(player);
                                        break;
                                    default:
                                        if (item.template != null) {
                                            switch (item.template.id) {
                                                case 73:
                                                    msg.writer().writeUTF("");
                                                    msg.writer().writeShort(item.quantity);
                                                    player.sendMessage(msg);
                                                    msg.cleanup();
                                                    break;
                                                case 74:
                                                    msg.writer().writeUTF("Bạn vừa ăn " + item.template.name);
                                                    break;
                                                case 78:
                                                    msg.writer().writeUTF("Wow, một cậu bé dễ thương!");
                                                    msg.writer().writeShort(item.quantity);
                                                    player.sendMessage(msg);
                                                    msg.cleanup();
                                                    break;
                                                case 516:
                                                    player.nPoint.setFullHpMp();
                                                    PlayerService.gI().sendInfoHpMp(player);
                                                    if (itemMap.itemTemplate != null) {
                                                        Service.getInstance().sendThongBao(player, "Bạn vừa ăn " + itemMap.itemTemplate.name);
                                                    }
                                                    break;
                                                default:
                                                    msg.writer().writeUTF("Bạn nhặt được " + item.template.name);
                                                    InventoryService.gI().sendItemBags(player);
                                                    break;
                                            }
                                        }

                                }
                                msg.writer().writeShort(item.quantity);
                                player.sendMessage(msg);
                                msg.cleanup();
                                Service.getInstance().sendToAntherMePickItem(player, itemMapId);
                                int mapID = this.map.mapId;
                                if (itemMap.itemTemplate != null && !(mapID >= 21 && mapID <= 23 && itemMap.itemTemplate.id == 74 || mapID >= 42 && mapID <= 44 && itemMap.itemTemplate.id == 78)) {
                                    removeItemMap(itemMap);
                                }
                            } catch (Exception e) {
                                Log.error(Zone.class, e);
                            }
                        } else {
                            if (item.template != null && !ItemMapService.gI().isBlackBall(item.template.id)) {
                                String text = "Hành trang không còn chỗ trống";
                                Service.getInstance().sendThongBao(player, text);
                            }
                        }
                    } else {
                        Service.getInstance().sendThongBao(player, "Không thể nhặt vật phẩm của người khác");
                    }
                }
            }
        } else {
            Service.getInstance().sendThongBao(player, "Không thể thực hiện");
        }
        TaskService.gI().checkDoneTaskPickItem(player, itemMap);
        TaskService.gI().checkDoneSideTaskPickItem(player, itemMap);
    }

    public void addItem(ItemMap itemMap) {
        synchronized (items) {
            items.add(itemMap);
        }
    }

    public void removeItemMap(ItemMap itemMap) {
        synchronized (items) {
            this.items.remove(itemMap);
        }
    }

    public Player getRandomPlayerInMap() {
        List<Player> snapshot;
        synchronized (this.notBosses) {
            snapshot = new ArrayList<>(this.notBosses);
        }
        if (!snapshot.isEmpty()) {
            return snapshot.get(Util.nextInt(0, snapshot.size() - 1));
        } else {
            return null;
        }
    }

    public void load_Me_To_Another(Player player) { //load thông tin người chơi cho những người chơi khác
        try {
            if (player.zone != null) {
                if (this.map.isMapOffline) {
                    if (player.isPet && this.equals(((Pet) player).master.zone)) {
                        infoPlayer(((Pet) player).master, player);
                    }
                    List<Player> snapshot;
                    synchronized (this.players) {
                        snapshot = new ArrayList<>(this.players);
                    }
                    for (Player pl : snapshot) {
                        if (!player.equals(pl)) {
                            if (pl.idPlayerForNPC == player.idPlayerForNPC) {
                                infoPlayer(pl, player);
                            }
                        }
                    }
                } else {
                    List<Player> snapshot;
                    synchronized (this.players) {
                        snapshot = new ArrayList<>(this.players);
                    }
                    for (Player pl : snapshot) {
                        if (!player.equals(pl)) {
                            infoPlayer(pl, player);
                        }
                    }
                }
            }
            PlayerService.gI().sendPetFollow(player);
        } catch (Exception e) {
            Log.error(MapService.class, e);
        }
    }

    public void loadAnotherToMe(Player player) { //load những player trong map và gửi cho player vào map
        try {
            List<Player> snapshot;
            synchronized (this.humanoids) {
                snapshot = new ArrayList<>(this.humanoids);
            }
            if (this.map.isMapOffline) {
                for (Player pl : snapshot) {
                    if (pl != null) {
                        if (pl.id == -player.id) {
                            infoPlayer(player, pl);
                            break;
                        }
                    }
                }
                for (Player pl : snapshot) {
                    if (pl != null) {
                        if (pl != player) {
                            if (pl.idPlayerForNPC == player.idPlayerForNPC) {
                                infoPlayer(player, pl);
                            }
                        }
                    }
                }
            } else {
                for (Player pl : snapshot) {
                    if (pl != null) {
                        if (player != pl) {
                            infoPlayer(player, pl);
                            PlayerService.gI().sendPetFollow(player, pl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(MapService.class, e);
        }
    }

    private void infoPlayer(Player plReceive, Player plInfo) {
        Message msg;
        try {
            msg = new Message(-5);
            msg.writer().writeInt((int) plInfo.id);
            if (plInfo.clan != null) {
                msg.writer().writeInt(plInfo.clan.id);
            } else {
                msg.writer().writeInt(-1);
            }
            int level = CaptionManager.getInstance().getLevel(plInfo);
            msg.writer().writeByte(level);
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(plInfo.typePk);
            msg.writer().writeByte(plInfo.gender);
            msg.writer().writeByte(plInfo.gender);
            msg.writer().writeShort(plInfo.getHead());
            msg.writer().writeUTF(plInfo.name);
            msg.writer().writeInt(plInfo.nPoint.hp);
            msg.writer().writeInt(plInfo.nPoint.hpMax);
            msg.writer().writeShort(plInfo.getBody());
            msg.writer().writeShort(plInfo.getLeg());
            msg.writer().writeByte(plInfo.getFlagBag()); //bag
            msg.writer().writeByte(-1);
            msg.writer().writeShort(plInfo.location.x);
            msg.writer().writeShort(plInfo.location.y);
            msg.writer().writeShort(0);
            msg.writer().writeShort(0); //

            msg.writer().writeByte(0);

            msg.writer().writeByte(plInfo.getUseSpaceShip());

            msg.writer().writeShort(plInfo.getMount());
            msg.writer().writeByte(plInfo.cFlag);
            msg.writer().writeByte(0);

            msg.writer().writeShort(plInfo.getAura()); //idauraeff
            msg.writer().writeByte(plInfo.getEffFront()); //seteff

            plReceive.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Log.error(MapService.class, e);
        }

        Service.getInstance().sendFlagPlayerToMe(plReceive, plInfo);

       if (plInfo.isPl() && plInfo.inventory.itemsBody.get(13).isNotNullItem()) {
            Service.getInstance().sendFootRv(plInfo, plReceive, (short) plInfo.inventory.itemsBody.get(13).template.id);
        }
        if (plInfo.isPl() && plInfo.inventory.itemsBody.get(12).isNotNullItem()) {
            Service.getInstance().sendTitleRv1(plInfo, plReceive, (short) (plInfo.inventory.itemsBody.get(12).template.part));
        }

        try {
            if (plInfo.isDie()) {
                msg = new Message(-8);
                msg.writer().writeInt((int) plInfo.id);
                msg.writer().writeByte(0);
                msg.writer().writeShort(plInfo.location.x);
                msg.writer().writeShort(plInfo.location.y);
                plReceive.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {
        }
    }

    public void mapInfo(Player pl) {
        Message msg;
        try {
            msg = new Message(-24);
            msg.writer().writeByte(this.map.mapId);
            msg.writer().writeByte(this.map.planetId);
            msg.writer().writeByte(this.map.tileId);
            msg.writer().writeByte(this.map.bgId);
            msg.writer().writeByte(this.map.type);
            msg.writer().writeUTF(this.map.mapName);
            msg.writer().writeByte(this.zoneId);

            msg.writer().writeShort(pl.location.x);
            msg.writer().writeShort(pl.location.y);

            // waypoint
            List<WayPoint> wayPoints = this.map.wayPoints;
            msg.writer().writeByte(wayPoints.size());
            for (WayPoint wp : wayPoints) {
                msg.writer().writeShort(wp.minX);
                msg.writer().writeShort(wp.minY);
                msg.writer().writeShort(wp.maxX);
                msg.writer().writeShort(wp.maxY);
                msg.writer().writeBoolean(wp.isEnter);
                msg.writer().writeBoolean(wp.isOffline);
                msg.writer().writeUTF(wp.name);
            }
            // mob
            List<Mob> snapshotMobs;
            synchronized (this.mobs) {
                snapshotMobs = new ArrayList<>(this.mobs);
            }
            msg.writer().writeByte(snapshotMobs.size());
            for (Mob mob : snapshotMobs) {
                msg.writer().writeBoolean(false); //is disable
                msg.writer().writeBoolean(false); //is dont move
                msg.writer().writeBoolean(false); //is fire
                msg.writer().writeBoolean(false); //is ice
                msg.writer().writeBoolean(false); //is wind
                msg.writer().writeByte(mob.tempId);
                msg.writer().writeByte(mob.getSys());
                msg.writer().writeInt(mob.point.getHP());
                msg.writer().writeByte(mob.level);
                msg.writer().writeInt((mob.point.getHpFull()));
                msg.writer().writeShort(mob.location.x);
                msg.writer().writeShort(mob.location.y);
                if (mob.isDie()) {
                    msg.writer().writeByte(ConstMob.MA_INHELL); //status
                } else {
                    msg.writer().writeByte(ConstMob.MA_WALK); //status
                }
                msg.writer().writeByte(mob.lvMob); //level boss
                msg.writer().writeBoolean(mob.tempId == 77 || mob.tempId == 83 ? true : false);
            }

            msg.writer().writeByte(0);

            // npc
            List<Npc> npcs = NpcManager.getNpcsByMapPlayer(pl);
            msg.writer().writeByte(npcs.size());
            for (Npc npc : npcs) {
                msg.writer().writeByte(npc.status);
                msg.writer().writeShort(npc.cx);
                msg.writer().writeShort(npc.cy);
                msg.writer().writeByte(npc.tempId);
                msg.writer().writeShort(npc.avartar);
            }

            // item
            List<ItemMap> itemsMap = this.getItemMapsForPlayer(pl);

            if (itemsMap != null && msg.writer() != null) {
                msg.writer().writeByte(itemsMap.size());

                for (ItemMap it : itemsMap) {
                    if (it != null && it.itemTemplate != null) {
                        msg.writer().writeShort(it.itemMapId);
                        msg.writer().writeShort(it.itemTemplate.id);
                        msg.writer().writeShort(it.x);
                        msg.writer().writeShort(it.y);
                        msg.writer().writeInt((int) it.playerId);
                        if (it.playerId == -2) {
                            msg.writer().writeShort(it.range);
                        }
                    }
                }
            }

            // bg item
            try {
                String path = "resources/data_game/map/item_bg_map_data/" + this.map.mapId;
                byte[] bgItem = fileCache.get(path);
                if (bgItem == null) {
                    bgItem = FileIO.readFile(path);
                    if (bgItem != null) {
                        fileCache.put(path, bgItem);
                    }
                }
                if (bgItem != null) {
                    msg.writer().write(bgItem);
                } else {
                    msg.writer().writeShort(0);
                }
            } catch (Exception e) {
                msg.writer().writeShort(0);
            }

            // eff item
            try {
                String path = "resources/data_game/map/eff_map/" + this.map.mapId;
                byte[] effItem = fileCache.get(path);
                if (effItem == null) {
                    effItem = FileIO.readFile(path);
                    if (effItem != null) {
                        fileCache.put(path, effItem);
                    }
                }
                if (effItem != null) {
                    msg.writer().write(effItem);
                } else {
                    msg.writer().writeShort(0);
                }
            } catch (Exception e) {
                msg.writer().writeShort(0);
            }

            msg.writer().writeByte(this.map.bgType);
            msg.writer().writeByte(pl.getUseSpaceShip());
            msg.writer().writeByte(this.map.mapId == 148 ? 1 : 0);
            pl.sendMessage(msg);

            msg.cleanup();

        } catch (Exception e) {
            Log.error(Service.class, e);
        }
    }

    public TrapMap isInTrap(Player player) {
        List<TrapMap> snapshot;
        synchronized (this.trapMaps) {
            snapshot = new ArrayList<>(this.trapMaps);
        }
        for (TrapMap trap : snapshot) {
            if (player.location.x >= trap.x && player.location.x <= trap.x + trap.w
                    && player.location.y >= trap.y && player.location.y <= trap.y + trap.h) {
                return trap;
            }
        }
        return null;
    }

    public void changeMapWaypoint(Player player) {
        Zone zoneJoin = null;
        WayPoint wp = null;
        int xGo = player.location.x;
        int yGo = player.location.y;
        if (map.mapId == 45 || map.mapId == 46) {
            int x = player.location.x;
            int y = player.location.y;
            if (x >= 35 && x <= 685 && y >= 550 && y <= 560) {
                xGo = map.mapId == 45 ? 420 : 636;
                yGo = 150;
                zoneJoin = MapService.gI().getMapCanJoin(player, map.mapId + 1);
            }
        }

        if (zoneJoin == null) {
            wp = MapService.gI().getWaypointPlayerIn(player);
            if (wp != null) {
                zoneJoin = MapService.gI().getMapCanJoin(player, wp.goMap);
                if (zoneJoin != null) {
                    xGo = wp.goX;
                    yGo = wp.goY;
                }
            }
        }
        if (MapService.gI().isMapDoanhTrai(player.zone.map.mapId)) {  // khóa map ( phải tiêu diệt hết quái )
            if (!player.zone.isCheckKilledAll(player.zone.map.mapId)) {
                if (player.zone.getNumOfBosses() != 0) {
                    int x = player.location.x;
                    if (player.location.x >= map.mapWidth - 60) {
                        x = map.mapWidth - 60;
                    } else if (player.location.x <= 60) {
                        x = 60;
                    }
                    Service.getInstance().resetPoint(player, x, player.location.y);
                    Service.getInstance().sendThongBao(player, "Chưa hạ hết đối thủ");
                    return;
                }
            }
        }
        if (MapService.gI().isMapBanDoKhoBau(player.zone.map.mapId)) { // khóa map ( phải tiêu diệt hết quái )
            if (!player.zone.isCheckKilledAll(player.zone.map.mapId)) {
                int x = player.location.x;
                if (player.location.x >= map.mapWidth - 60) {
                    x = map.mapWidth - 60;
                } else if (player.location.x <= 60) {
                    x = 60;
                }
                Service.getInstance().resetPoint(player, x, player.location.y);
                Service.getInstance().sendThongBao(player, "Chưa hạ hết đối thủ");
                return;
            }
        }
        if (MapService.gI().isMapKhiGas(player.zone.map.mapId)) { // khóa map ( phải tiêu diệt hết quái )
            wp = MapService.gI().getWaypointPlayerIn(player);
            if (!player.zone.isCheckKilledAll(player.zone.map.mapId)) {
                if (wp != null) {
                    if ((player.zone.map.mapId == 147 && wp.goMap != 149) || (player.zone.map.mapId == 152 && wp.goMap != 147) || (player.zone.map.mapId == 151 && wp.goMap != 152) || (player.zone.map.mapId == 148 && wp.goMap != 151)) {
                        int x = player.location.x;
                        if (player.location.x >= map.mapWidth - 60) {
                            x = map.mapWidth - 60;
                        } else if (player.location.x <= 60) {
                            x = 60;
                        }
                        Service.getInstance().resetPoint(player, x, player.location.y);
                        Service.getInstance().sendThongBao(player, "Chưa hạ hết đối thủ");
                        return;
                    } else {
                        wp = MapService.gI().getWaypointPlayerIn(player);
                        if (wp != null) {
                            zoneJoin = MapService.gI().getMapCanJoin(player, wp.goMap);
                            if (zoneJoin != null) {
                                if (zoneJoin.map.mapId == 148 || map.mapId == 148) {
                                    xGo = wp.goX;
                                    yGo = wp.goY;
                                } else {
                                    player.location.goMap = wp.goMap;
                                    player.location.goX = wp.goX;
                                    player.location.goY = wp.goY;
                                    ChangeMapService.gI().nextmap(player);
                                    return;
                                }
                            }
                        }
                    }
                }
            } else {
                wp = MapService.gI().getWaypointPlayerIn(player);
                if (wp != null) {
                    zoneJoin = MapService.gI().getMapCanJoin(player, wp.goMap);
                    if (zoneJoin != null) {
                        if (zoneJoin.map.mapId == 148 || map.mapId == 148) {
                            xGo = wp.goX;
                            yGo = wp.goY;
                        } else {
                            player.location.goMap = wp.goMap;
                            player.location.goX = wp.goX;
                            player.location.goY = wp.goY;
                            ChangeMapService.gI().nextmap(player);
                            return;
                        }
                    }
                }
            }
        }
        if (zoneJoin != null) {
            ChangeMapService.gI().changeMap(player, zoneJoin, -1, -1, xGo, yGo, NON_SPACE_SHIP);
            if (zoneJoin.map.mapId == 47) {
                Service.getInstance().callTau77(player);
            }
        } else {
            int x = player.location.x;
            if (player.location.x >= map.mapWidth - 60) {
                x = map.mapWidth - 60;
            } else if (player.location.x <= 60) {
                x = 60;
            }
            Service.getInstance().resetPoint(player, x, player.location.y);
            Service.getInstance().sendThongBao(player, "Không thể đến khu vực này");
        }
    }

    public void playerMove(Player player, int x, int y) {
        if (!player.isDie()) {
            if (player.effectSkill.isCharging) {
                EffectSkillService.gI().stopCharge(player);
            }
            if (player.effectSkill.useTroi) {
                EffectSkillService.gI().removeUseTroi(player);
            }
            player.location.x = x;
            player.location.y = y;
            switch (map.mapId) {
                case 85:
                case 86:
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                    if (x < 24 || x > map.mapWidth - 24 || y < 0 || y > map.mapHeight - 24) {
                        if (MapService.gI().getWaypointPlayerIn(player) == null) {
                            ChangeMapService.gI().changeMap(player, 21 + player.gender, 0, 200, 336);
                            return;
                        }
                    }
                    if (!player.isBoss && !player.isPet) {
                        int yTop = map.yPhysicInTop(player.location.x, player.location.y);
                        if (yTop >= map.mapHeight - 24) {
                            ChangeMapService.gI().changeMap(player, 21 + player.gender, 0, 200, 336);
                            return;
                        }
                    }
                    break;
            }
            if (player.pet != null) {
                player.pet.followMaster();
            }
            if (player.minipet != null) {
                player.minipet.followMaster();
            }
             if (player.clone != null) {
                player.clone.followMaster();
            }
            MapService.gI().sendPlayerMove(player);
            TaskService.gI().checkDoneTaskGoToMap(player, player.zone);
        }
    }

    public Mob findMobByID(int id) {
        List<Mob> snapshot;
        synchronized (mobs) {
            snapshot = new ArrayList<>(mobs);
        }
        int low = 0;
        int high = snapshot.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (snapshot.get(mid).id < id) {
                low = mid + 1;
            } else if (snapshot.get(mid).id > id) {
                high = mid - 1;
            } else {
                return snapshot.get(mid);
            }
        }
        return null;
    }

    public Player findPlayerByID(long id) {
        List<Player> snapshot;
        synchronized (this.players) {
            snapshot = new ArrayList<>(this.players);
        }
        for (Player p : snapshot) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    public void sendMessage(Message m) {
        List<Player> snapshot;
        synchronized (players) {
            snapshot = new ArrayList<>(players);
        }
        for (Player player : snapshot) {
            player.sendMessage(m);
        }
    }
}