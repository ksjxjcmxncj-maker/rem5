package nro.consts;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class ConstPlayer {

    public static final int[] HEADMONKEY = {198, 198, 198, 198, 198, 198, 198};

    
    // AURA BIẾN HÌNH Ở ĐÂY
    public static final byte[][] AURABIENHINH = {
        // LẦN LƯỢT TỪ LB 1-5
        {7, 7, 13, 6, 73},   //td
        {4, 5, 13, 6, 74},  //nm
        {8, 8, 13, 5, 69}   //xd
    };
    // SỬA NGOẠI HÌNH TỪ LV 1-5 Ở ĐÂY
    public static final short[][] HEADBIENHINH = {
        {1463, 1443, 1444, 1445, 1446}, // 5 head TD 
        {1449, 1450, 1451, 1452, 1453},// 5 haed NM
         {1456, 1457, 1458, 1459, 1460}, // 5 head XD
    };
    
    // THÂN NGOẠI HÌNH LV 1-5
    public static final short[] BODYBIENHINH = {1461, 1447, 1454}; // TD /NM/ XD
    public static final short[] LEGBIENHINH = {1462, 1448, 1455}; // TD /NM/ XD

    public static final byte TRAI_DAT = 0;
    public static final byte NAMEC = 1;
    public static final byte XAYDA = 2;

    //type pk
    public static final byte NON_PK = 0;
    public static final byte PK_PVP = 3;
    public static final byte PK_ALL = 5;

    //type fushion
    public static final byte NON_FUSION = 0;
    public static final byte LUONG_LONG_NHAT_THE = 4;
    public static final byte HOP_THE_PORATA = 6;
    public static final byte HOP_THE_PORATA2 = 8;
    
    public static final byte QTY_MAX_ITEM_BODY_PET = 7;
    
}
