package nro.services;

import nro.consts.ConstPlayer;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.utils.SkillUtil;
import nro.utils.Util;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class PetService {

    private static PetService i;

    public static synchronized PetService gI() {
        if (i == null) {
            i = new PetService();
        }
        return i;
    }

    public void createNormalPet(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    public void createNormalPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    public void createMabuPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, true, false, false, false, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Oa oa oa...");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, true, false, false, false, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Oa oa oa...");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    public void createPetGokuSSJ4(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, true, false, false, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (InterruptedException ex) {
                System.out.println("Lỗi tại đây");
            }
        }).start();
    }

    public void createPetGokuSSJ4(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, true, false, false, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    public void createPetVegetaSJ4(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, true, false, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (InterruptedException ex) {
                System.out.println("Lỗi tại đây");
            }
        }).start();
    }

    public void createPetVegetaSJ4(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, true, false, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    public void createPetSuperPicolo(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, true, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (InterruptedException ex) {
                System.out.println("Lỗi tại đây");
            }
        }).start();
    }

    public void createPetSuperPicolo(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, true, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }
    public void createPetBabyFide(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, true, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (InterruptedException ex) {
                System.out.println("Lỗi tại đây");
            }
        }).start();
    }

    public void createPetBabyFide(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, true, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }
    public void createPetBabyKidbu(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, false, true, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (InterruptedException ex) {
                System.out.println("Lỗi tại đây");
            }
        }).start();
    }

    public void createPetBabyKidbu(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, false, true, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }public void createPetBabyCell(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, true, false, true);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (InterruptedException ex) {
                System.out.println("Lỗi tại đây");
            }
        }).start();
    }

    public void createPetBabyCell(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, false, false, true, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }
    public void changeNormalPet(Player player, int gender) {
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        MapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createNormalPet(player, gender, limitPower);
        
    }

    public void changeNormalPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        MapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createNormalPet(player, limitPower);
    }

    public void changeMabuPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        MapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createMabuPet(player, limitPower);
    }

    public void changeMabuPet(Player player, int gender) {
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        MapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createMabuPet(player, gender, limitPower);
    }

    public void changeSuperPet(Player player, int gender, int typePet) {
    if (player.pet == null) {
        // Handle the case where player.pet is null
        // You might want to log an error or throw an exception,
        // depending on how critical this is for your application.
        return;
    }
    
    byte limitPower = 0; // default value or handle differently if necessary
    if (player.pet.nPoint != null) {
        limitPower = player.pet.nPoint.limitPower;
    }

    if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
        player.pet.unFusion();
    }
    MapService.gI().exitMap(player.pet);
    player.pet.dispose();
    player.pet = null;

    switch (typePet) {
        case 0:
            createPetGokuSSJ4(player, gender, limitPower);
            break;
        case 1:
            createPetVegetaSJ4(player, gender, limitPower);
            break;
        case 2:
            createPetSuperPicolo(player, gender, limitPower);
            break;
        case 3:
            createPetBabyFide(player, gender, limitPower);
            break;
        case 4:
            createPetBabyKidbu(player, gender, limitPower);
            break;
        case 5:
            createPetBabyCell(player, gender, limitPower);
            break;
        default:
            // Handle unexpected typePet value
            break;
    }
}

    public void changeNamePet(Player player, String name) {
        if (!InventoryService.gI().existItemBag(player, 400)) {
            Service.getInstance().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
            return;
        } else if (Util.haveSpecialCharacter(name)) {
            Service.getInstance().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
            return;
        } else if (name.length() > 10) {
            Service.getInstance().sendThongBao(player, "Tên quá dài");
            return;
        }
        MapService.gI().exitMap(player.pet);
        player.pet.name = "$" + name.toLowerCase().trim();
        InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBagByTemp(player, 400), 1);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
            } catch (Exception e) {
            Log.error(PetService.class, e);
        }
        }).start();
    }

    private int[] getDataPetNormal() {
        int[] hpmp = {1700, 1800, 1900, 2000, 2100, 2200};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(20, 45); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetMabu() {
        int[] hpmp = {1700, 1800, 1900, 2000, 2100, 2200};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(50, 120); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetSuper() { // chỉnh chỉ số pet mới
        int[] hpmp = {1700, 1800, 1900, 2000, 2100, 2200};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(50, 120); //dame
        petData[3] = Util.nextInt(9, 200); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private void createNewPet(Player player, boolean isMabu, boolean isGokuSSJ4, boolean isVegetaSSJ4, boolean isSuperPicolo, boolean isBabyFide, boolean isBabyKidbu, boolean isBabyCell, byte... gender) {
        int[] data = isMabu ? getDataPetMabu() : isGokuSSJ4 ? getDataPetSuper() : isVegetaSSJ4 ? getDataPetSuper() : isSuperPicolo ? getDataPetSuper() : isBabyFide ? getDataPetSuper() : isBabyKidbu ? getDataPetSuper() : isBabyCell ? getDataPetSuper() : getDataPetNormal();
        Pet pet = new Pet(player);
        pet.name = "$" + (isMabu ? "Mabư" : isGokuSSJ4 ? "Goku SSJ4" : isVegetaSSJ4 ? "Vegeta SSJ4" : isSuperPicolo ? "Super Picolo" : isBabyFide ? "Baby Fide" : isBabyKidbu ? "Baby Kidbu" : isBabyCell ? "Baby Cell"  : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = -player.id;
        pet.nPoint.power = isMabu ? 1500000 : isGokuSSJ4 ? 1500000 : isVegetaSSJ4 ? 1500000 : isSuperPicolo ? 1500000 : isBabyFide ? 1500000 : isBabyKidbu ? 1500000 : isBabyCell ? 1500000 : 2000;
        pet.isMabu = isMabu;
        pet.isGokuSSJ4 = isGokuSSJ4;
        pet.isVegetaSSJ4 = isVegetaSSJ4;
        pet.isSuperPicolo = isSuperPicolo;
        pet.isBabyFide = isBabyFide;
        pet.isBabyKidbu = isBabyKidbu;
        pet.isBabyCell = isBabyCell;
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        for (int i = 0; i < ConstPlayer.QTY_MAX_ITEM_BODY_PET; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 3; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.calPoint();
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    //--------------------------------------------------------------------------
}
