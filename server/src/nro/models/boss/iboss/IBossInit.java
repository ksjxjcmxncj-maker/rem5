package nro.models.boss.iboss;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public interface IBossInit extends IBossOutfit {

    void init(); //khởi tạo respawn

    void initTalk(); //khởi tạo hội thoại

    void dropItemReward(int tempId, int playerId, int... quantity); //rớt item thưởng
}
