/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.models.npc.NpcForge;

import nro.consts.ConstNpc;
import nro.models.consignment.ConsignmentShop;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.Service;

/**
 *
 * @author OBITO
 */
public class CuaHangKyGui extends Npc {

    public CuaHangKyGui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Cửa hàng chúng tôi chuyên mua bán hàng hiệu, hàng độc, cảm ơn bạn đã ghé thăm.",
                    "Hướng\ndẫn\nthêm", "Mua bán", "Danh sách\nHết Hạn", "Hủy");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU:
                    switch (select) {
                        case 0:
                            Service.getInstance().sendPopUpMultiLine(player, tempId, avartar, "Cửa hàng chuyên nhận ký gửi mua bán vật phẩm\bGiá trị ký gửi 10k-200Tr vàng hoặc 2-2k ngọc\bMột người bán, vạn người mua, mại dô, mại dô");
                            break;
                        case 1:
                            ConsignmentShop.getInstance().show(player);
                            break;
                        case 2:
                            ConsignmentShop.getInstance().showExpiringItems(player);
                            break;
                    }
                    break;
            }
        }
    }

}
