-- ============================================================
-- Fix skill icon IDs cho template 27 (Biến Hình) và 28 (Phân Thân)
-- 
-- Nguồn đúng: nro_srcteam.sql + nro_upgrade_data.sql
--   Skill 27 nclass 0 (Trái Đất) = 26247
--   Skill 27 nclass 1 (Namếc)    = 26253
--   Skill 27 nclass 2 (Saijan)   = 26241
--   Skill 28 Phân Thân (all)     = 31142
--
-- LƯU Ý: KHÔNG đổi sang 3783/3784 — trùng với item Sách Dịch Chuyển
--        và Khiên năng lượng, gây lỗi icon lây sang trang phục
-- ============================================================

UPDATE skill_template SET icon_id = 26247 WHERE nclass_id = 0 AND id = 27;
UPDATE skill_template SET icon_id = 26253 WHERE nclass_id = 1 AND id = 27;
UPDATE skill_template SET icon_id = 26241 WHERE nclass_id = 2 AND id = 27;
UPDATE skill_template SET icon_id = 31142 WHERE id = 28;

-- Verify
SELECT nclass_id, id, name, icon_id FROM skill_template WHERE id IN (27,28) ORDER BY id, nclass_id;
