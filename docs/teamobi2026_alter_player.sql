-- ============================================================
-- ALTER TABLE player: Thêm cột mới từ Teamobi2026
-- FIXED: Tất cả NOT NULL đã có DEFAULT an toàn
-- FIXED: Charset thống nhất utf8mb4
-- BACKUP TRƯỚC: mysqldump -u root nro1 player > /backup/player_backup.sql
-- Lưu ý: Dùng ADD COLUMN IF NOT EXISTS (MariaDB 10.x+)
-- ============================================================: mysqldump -u root nro1 player > /backup/player_backup.sql
-- Lưu ý: Dùng ADD COLUMN IF NOT EXISTS (MariaDB 10.x+)
-- ============================================================
SET NAMES utf8mb4;

-- Tổng: 40 cột mới cần thêm

ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `BoughtSkill` text DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `LearnSkill` text DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `bandokhobau` varchar(250) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `baovetaikhoan` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `captcha` varchar(1000) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `clan_id` int(11) NOT NULL DEFAULT -1;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `conduongrandoc` varchar(255) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `dailyGift` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `dataBadges` text DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `dataTaskBadges` text DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_achievement` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_card` varchar(10000) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_clan_task` varchar(255) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_duahau_egg` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_event` text DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_item_event` varchar(1000) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_luyentap` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `data_vip` text DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `doanhtrai` bigint(20) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `giftcode` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `items_daban` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `lasttimepkcommeson` bigint(20) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `masterDoesNotAttack` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `nhanthoivang` varchar(200) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `nhiem_vu_kol` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `notify` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `pet` text NOT NULL DEFAULT '';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `point_maydam` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `point_sukien` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `point_sukien1` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `point_sukien2` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `rank` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `rongxuong` bigint(20) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `ruonggo` varchar(255) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `sieuthanthuy` varchar(255) NOT NULL DEFAULT '[]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `thachdauwhis` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `thanhTichCDRD` varchar(255) NOT NULL DEFAULT '[0,0,0,0]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `thanhTichKhiGas` varchar(255) NOT NULL DEFAULT '[0,0,0,0]';
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `total_damage_maydam` bigint(20) NOT NULL DEFAULT 0;
ALTER TABLE `player` ADD COLUMN IF NOT EXISTS `vodaisinhtu` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]';