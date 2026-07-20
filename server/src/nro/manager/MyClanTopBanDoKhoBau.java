package nro.manager;

import lombok.Getter;
import nro.jdbc.DBService;
import nro.jdbc.daos.PlayerDAO;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.player.Player;
import nro.services.ItemService;
import nro.utils.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author outcast c-cute hột me 😳
 */
public class MyClanTopBanDoKhoBau {

    @Getter
    private List<Player> list = new ArrayList<>();

    private static final MyClanTopBanDoKhoBau INSTANCE = new MyClanTopBanDoKhoBau();

    public static MyClanTopBanDoKhoBau getInstance() {
        return INSTANCE;
    }

    public void load2(int idLeader) {
        list.clear();

        try ( Connection con = DBService.gI().getConnectionForGetPlayer();  PreparedStatement ps = con.prepareStatement("SELECT *, "
                + "SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang, ',', 1), '[', -1) AS so1, "
                + "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang, ',', 2), ',', -1) AS SIGNED) AS so2, "
                + "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang, ',', 3), ',', -1) AS SIGNED) AS so3, "
                + "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang, ',', 4), ',', -1) AS SIGNED) AS so4 "
                + "FROM player WHERE CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang, ',', 2), ',', -1), ']', 1) AS SIGNED) > 0 "
                + "AND id = ? ORDER BY so2 DESC, so1 ASC LIMIT 100")) {

            // Đặt giá trị thực tế của idLeader vào câu truy vấn
            ps.setInt(1, idLeader);

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Player player = extractPlayerFromResultSet(rs);
                    list.add(player);
                }
            }
        } catch (Exception e) {
            Log.error(PlayerDAO.class, e);
        }
    }

    private Player extractPlayerFromResultSet(ResultSet rs) throws SQLException {
        Player player = new Player();

        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
        player.lastimelogin = rs.getTimestamp("lastimelogin");
        player.tongnap = rs.getInt("tongnap");

        player.nameClan = rs.getString("so1");
        player.levelBDKBDone = rs.getInt("so2");
        player.timeBDKBDone = rs.getLong("so3");

        player.lastTimeUpdateTopBDKB = (System.currentTimeMillis() - rs.getLong("so4")) / 1000;

        extractDataPoint(rs.getString("data_point"), player);
        extractItemsBody(rs.getString("items_body"), player);

        return player;
    }

    private void extractDataPoint(String dataPoint, Player player) {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = (JSONArray) jv.parse(dataPoint);
        player.nPoint.power = Long.parseLong(dataArray.get(11).toString());
        dataArray.clear();
    }

    private void extractItemsBody(String itemsBody, Player player) {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = (JSONArray) jv.parse(itemsBody);

        for (Object itemDataObject : dataArray) {
            Item item = createItemFromDataObject(itemDataObject.toString());
            player.inventory.itemsBody.add(item);
        }

        dataArray.clear();
    }

    private Item createItemFromDataObject(String itemData) {
        JSONValue jv = new JSONValue();
        JSONObject dataObject = (JSONObject) jv.parse(itemData);
        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
        Item item;

        if (tempId != -1) {
            item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
            JSONArray options = (JSONArray) jv.parse(String.valueOf(dataObject.get("option")).replaceAll("\"", ""));

            for (Object option : options) {
                JSONArray opt = (JSONArray) jv.parse(String.valueOf(option));
                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                        Integer.parseInt(String.valueOf(opt.get(1)))));
            }

            item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));

            if (ItemService.gI().isOutOfDateTime(item)) {
                item = ItemService.gI().createItemNull();
            }
        } else {
            item = ItemService.gI().createItemNull();
        }

        return item;
    }
}
