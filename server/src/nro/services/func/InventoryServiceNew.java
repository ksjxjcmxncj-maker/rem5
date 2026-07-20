package nro.services.func;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.services.InventoryService;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class InventoryServiceNew {

    private static volatile InventoryServiceNew i; // FIX: volatile cho thread safety

    public static synchronized InventoryServiceNew gI() { // FIX: synchronized
        if (i == null) {
            i = new InventoryServiceNew();
        }
        return i;
    }

    // FIX: implement thực sự thay vì throw UnsupportedOperationException
    void sendItemBags(Player player) {
        InventoryService.gI().sendItemBags(player);
    }

    void subQuantityItemsBag(Player player, Item item, int quantity) {
        InventoryService.gI().subQuantityItemsBag(player, item, quantity);
    }
}
