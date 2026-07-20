package nro.models.player;

import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class Inventory {

    public static final long LIMIT_GOLD = 200000000000000000L;

    private Player player;

    public Item trainArmor;

    public List<Item> itemsBody;
    public List<Item> itemsBag;
    public List<Item> itemsBox;

    public List<Item> itemsBoxCrackBall;
//    public List<Item> itemsReward;

    public long gold, goldLimit;
    public int gem;
    public int ruby;

//    public int goldBar;
    public Inventory(Player player) {
        this.player = player;
        itemsBody = new ArrayList<>();
        itemsBag = new ArrayList<>();
        itemsBox = new ArrayList<>();
        itemsBoxCrackBall = new ArrayList<>();
//        itemsReward = new ArrayList<>();
    }

    public int getGem() {
        return this.gem;
    }

    public long getGold() {
        return this.gold;
    }

    public long getGoldLimit() {
        return goldLimit + LIMIT_GOLD;
    }

    public long getGoldDisplay() {
        long amount = gold;
        if (amount > Integer.MAX_VALUE && !player.isVersionAbove(214)) {
            return Integer.MAX_VALUE;
        }
        return amount;
    }

    public long getRuby() {
        return this.ruby;
    }

    public void subGem(int num) {
        if (this.ruby > num) {
            this.subRuby(num);
        } else {
            this.gem -= num;
            if (this.gem < 0) this.gem = 0; // FIX: không để gem âm
        }
    }

    public void subGold(int num) {
        this.gold -= num;
        if (this.gold < 0) this.gold = 0; // FIX: không để gold âm
    }

    public void subRuby(int num) {
        this.ruby -= num;
        if (this.ruby < 0) this.ruby = 0; // FIX: không để ruby âm
    }

    public void addGold(int gold) {
        this.gold += (long) gold; // FIX: cast để tránh int overflow
        long goldLimit = getGoldLimit();
        if (this.gold > goldLimit) { this.gold = goldLimit; }
        if (this.gold < 0) this.gold = 0; // FIX: phòng overflow âm
    }
    public void addRuby(int ruby) {
        this.ruby += ruby;
        if (this.ruby < 0) this.ruby = 0; // FIX: không để âm
    }

    public void addGem(int gem) { // FIX: thêm method an toàn
        this.gem += gem;
        if (this.gem < 0) this.gem = 0;
    }
    public void dispose() {
        this.player = null;
        if (this.trainArmor != null) {
            this.trainArmor.dispose();
        }
        this.trainArmor = null;
        if (this.itemsBody != null) {
            for (Item it : this.itemsBody) {
                it.dispose();
            }
            this.itemsBody.clear();
        }
        if (this.itemsBag != null) {
            for (Item it : this.itemsBag) {
                it.dispose();
            }
            this.itemsBag.clear();
        }
        if (this.itemsBox != null) {
            for (Item it : this.itemsBox) {
                it.dispose();
            }
            this.itemsBox.clear();
        }
        if (this.itemsBoxCrackBall != null) {
            for (Item it : this.itemsBoxCrackBall) {
                it.dispose();
            }
            this.itemsBoxCrackBall.clear();
        }
        this.itemsBody = null;
        this.itemsBag = null;
        this.itemsBox = null;
        this.itemsBoxCrackBall = null;
    }

    public void Ruby(int i) {
        addRuby(i); // FIX: stub → delegate addRuby()
    }
}
