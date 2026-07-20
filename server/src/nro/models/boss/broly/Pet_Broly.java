package nro.models.boss.broly;

import nro.models.boss.mapoffline.*;
import nro.models.player.*;
import nro.consts.ConstMap;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.map.DaiHoiVoThuat.DHVT23Service;
import nro.server.Manager;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.func.ChangeMapService;
import nro.utils.Util;

/**
 * @author outcast c-cute hột me 😳
 */
public class Pet_Broly extends Player {

    public void Pet_Broly(Player pl, int x, int y) {
        init(pl, x, y);
    }
    private static Pet_Broly instance;

    public static Pet_Broly getInstance() {
        if (instance == null) {
            instance = new Pet_Broly();
        }
        return instance;
    }
    
    private static final short[][] PET_ID = {{285, 286, 287}, {288, 289, 290}, {282, 283, 284}, {304, 305, 303}};

    private Zone z;
    
    public int genderPet = Util.nextInt(0, 2);

    @Override
    public short getHead() {
        return PET_ID[genderPet][0];
    }

    @Override
    public short getBody() {
        return PET_ID[genderPet][1];
    }

    @Override
    public short getLeg() {
        return PET_ID[genderPet][2];
    }

    public void joinMap(Zone z, Player player) {
        MapService.gI().goToMap(player, z);
        z.load_Me_To_Another(player);
    }

    @Override
    public int version() {
        return 214;
    }

    @Override
    public void update() {
     //   System.out.println("Hiện đệ tử này đang ở: " + this.zone.map.mapId + " vị trí x: " + this.location.x + " vị trí y:" + this.location.y);
    }

    public void init(Player player, int x, int y) {
        long id = -251003;
        Pet_Broly pl = new Pet_Broly();
        pl.name = "Đệ tử";
        pl.gender = 0;
        pl.id = id;
        pl.nPoint.hpMax = 2100;
        pl.nPoint.hpg = 2100;
        pl.nPoint.hp = 2100;
        pl.nPoint.setFullHpMp();
        pl.location.x = x;
        pl.location.y = y;
        joinMap(player.zone, pl);
        player.zone.setReferee(pl);
    }
}
