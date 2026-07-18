---
name: Working Preferences
description: Quy tắc làm việc do người dùng yêu cầu — áp dụng cho mọi phiên
---

# Quy tắc làm việc

**Why:** Người dùng thiết lập để tiết kiệm token, tránh mất tiến trình, và duy trì codebase gọn gàng.

## 1. Lưu tiến trình cuối phiên
Trước khi kết thúc mỗi phiên chat, luôn cập nhật MEMORY.md và các topic file liên quan với tiến trình công việc hiện tại (phần nào xong, phần nào đang dở, bước tiếp theo là gì).

## 2. Đánh dấu phần đã hoàn thành
Khi một module/tính năng đã xong, ghi nhận vào memory. Không động vào code đã hoàn thành trừ khi có lý do rõ ràng (bug, yêu cầu thay đổi từ user).

## 3. Tiết kiệm token — mức ưu tiên cao
- Luôn chọn phương án ít token nhất khi có thể.
- Batch các tool call độc lập vào cùng một response.
- Chỉ đọc file khi thực sự cần; dùng grep/search trước để định vị chính xác.
- Hạn chế đọc quá 10 file/lượt.
- Token xuống 20% → chuyển sang chế độ cực tiết kiệm: câu trả lời ngắn, không giải thích dài, ưu tiên tool call song song.

## 4. Tổ chức code gọn gàng
- Sai ở đâu sửa đúng chỗ đó, không viết lại từ đầu.
- Chia file nhỏ, chi tiết — dễ định vị sau này.
- Giữ cấu trúc monorepo hiện có, không tái cấu trúc khi không cần thiết.

## 5. Đồng bộ GitHub
- Luôn cập nhật vào repo chính trước khi đồng bộ sang 3 server dự phòng.
- Token GitHub được lưu trong Replit Secrets (không lưu plaintext vào memory).

**How to apply:** Kiểm tra file này đầu mỗi phiên. Cập nhật tiến trình vào MEMORY.md trước khi kết thúc phiên.
