package nro.models.player;

import nro.models.skill.PlayerSkill;
import nro.models.skill.Skill;
import nro.services.InventoryService;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.func.ChangeMapService;
import nro.utils.Util;

public class PlayerClone extends Player {
    public Player master;
    private long lastSpawnTime = 0;
    private long lastTimeDie = 0;

    public PlayerClone(Player master) {
        super();
        this.master = master;
        this.isClone = true;
        this.id = master.id - 10_000;
        this.gender = master.gender;
        this.name = "[Clone] " + master.name;
        this.nPoint.hpg = master.nPoint.hpg;
        this.nPoint.mpg = master.nPoint.mpg;
        this.nPoint.dameg = master.nPoint.dameg;
        this.nPoint.defg = master.nPoint.defg;
        this.nPoint.critg = master.nPoint.critg;
        this.nPoint.power = master.nPoint.power;
        this.nPoint.tiemNang = master.nPoint.tiemNang;
        this.nPoint.stamina = master.nPoint.stamina;
        this.nPoint.maxStamina = master.nPoint.maxStamina;
        this.inventory = new Inventory(this);
        this.inventory.itemsBody = InventoryService.gI().copyItemsBody(master);
        this.playerSkill = new PlayerSkill(this);
        this.cloneSkill();
        this.nPoint.calPoint();
        this.nPoint.setFullHpMp();
        this.lastSpawnTime = System.currentTimeMillis();
    }

    private void cloneSkill() {
        for (Skill skill : master.playerSkill.skills) {
            Skill cloneSkill = new Skill(skill);
            this.playerSkill.skills.add(cloneSkill);
        }
    }

    @Override
    public void update() {
        super.update();
        if (isDie() && canRespawn()) {
            Service.getInstance().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
        if (master != null && (this.zone == null || this.zone != master.zone)) {
            joinMapMaster();
        }
        if (Util.canDoWithTime(lastSpawnTime, 180000) || (lastTimeDie != 0 && Util.canDoWithTime(lastTimeDie, 3_000))) {
            dispose();
        }
    }

    @Override
    public void setDie(Player plAtt) {
        super.setDie(plAtt);
        lastTimeDie = System.currentTimeMillis();
    }

    private boolean canRespawn() {
        return lastTimeDie == 0;
    }

    public void joinMapMaster() {
        this.location.x = master.location.x + Util.nextInt(-10, 10);
        this.location.y = master.location.y;
        MapService.gI().goToMap(this, master.zone);
        this.zone.load_Me_To_Another(this);
    }

    public void followMaster() {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= 40) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, 40);
            } else {
                this.location.x = mX + Util.nextInt(0, 40);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    @Override
    public short getHead() {
        return master.getHead();
    }

    @Override
    public short getBody() {
        return master.getBody();
    }

    @Override
    public short getLeg() {
        return master.getLeg();
    }

    @Override
    public byte getAura() {
        return master.getAura();
    }

    @Override
    public short getMount() {
        return master.getMount();
    }

    @Override
    public void dispose() {
       MapService.gI().exitMap(this);
        if (this.master != null) {
            this.master.clone = null;
        }
        this.master = null;
        super.dispose();
    }
}
