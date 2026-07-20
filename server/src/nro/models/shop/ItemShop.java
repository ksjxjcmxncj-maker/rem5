package nro.models.shop;

import nro.models.item.ItemTemplate;
import nro.models.item.ItemOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nro.utils.SkillUtil;

/**
 *
 * OBITO
 *
 */
public class ItemShop {

    public TabShop tabShop;

    public int id;

    public ItemTemplate temp;

    public int gold;

    public int gem;


    public boolean isNew;

    public List<ItemOption> options;

    public int iconSpec;

    public int costSpec;
    public int itemExchange;

    public ItemShop() {
        this.options = new ArrayList<>();
    }

    public ItemShop(ItemShop itemShop) {
        this.tabShop = itemShop.tabShop;
        this.id = itemShop.id;
        this.temp = itemShop.temp;
        this.gold = itemShop.gold;
        this.gem = itemShop.gem;
        this.isNew = itemShop.isNew;
        this.options = new ArrayList<>();
        for (ItemOption io : itemShop.options) {
            this.options.add(new ItemOption(io));
        }
    }

    public byte getLevelSkill() {
        String[] subName = temp.name.split("skill");
        byte level = Byte.parseByte(subName[subName.length - 1]);
        return level;
    }
    
    public long getPowerRequire(){
        return SkillUtil.getSkillByLevel(temp.id, getLevelSkill()).powRequire;
    }
}
