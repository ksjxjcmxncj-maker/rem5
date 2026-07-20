/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.services;

import nro.consts.Cmd;
import nro.manager.SieuHangManager;
import nro.models.Part;
import nro.models.PartManager;
import nro.models.player.Player;
import nro.server.io.Message;
import nro.utils.Util;

import java.util.List;
import nro.models.sieu_hang.SieuHangModel;

/**
 *
 * @author OBITO
 */
public class SieuHangService {

    public static void ShowTop(Player player, int can_fight) {
        List<SieuHangModel> list = SieuHangManager.GetTop((int) player.id, can_fight);
        Message msg = new Message(Cmd.TOP);
        try {
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100 Cao Thủ");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                int thuong = 0;
                SieuHangModel top = list.get(i);
                msg.writer().writeInt(top.rank);
                msg.writer().writeInt((int) top.player_id);
                msg.writer().writeShort(top.player.getHead());
                if (player.isVersionAbove(220)) {
                    Part part = PartManager.getInstance().find(top.player.getHead());
                    msg.writer().writeShort(part.getIcon(0));
                }
                msg.writer().writeShort(top.player.getBody());
                msg.writer().writeShort(top.player.getLeg());
                msg.writer().writeUTF(top.player.name);

                if (top.rank == 1) {
                    thuong = 20000;
                } else if (top.rank == 2) {
                    thuong = 15000;
                } else if (top.rank >= 3 && top.rank < 10) {
                    thuong = 10000;
                } else if (top.rank >= 10 && top.rank < 30) {
                    thuong = 7000;
                }  else {
                    thuong = 1;
                }
                if (top.rank <= 30) {
                    msg.writer().writeUTF("+" + thuong + " ngọc/ ngày");
                } else {
                    msg.writer().writeUTF("");
                }
                
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object gI() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
