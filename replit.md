# [Project name]

_Replace the heading above with the project's name, and this line with one sentence describing what this app does for users._

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- Required env: `DATABASE_URL` — Postgres connection string

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

## Where things live

_Populate as you build — short repo map plus pointers to the source-of-truth file for DB schema, API contracts, theme files, etc._

## Architecture decisions

_Populate as you build — non-obvious choices a reader couldn't infer from the code (3-5 bullets)._

## Product

_Describe the high-level user-facing capabilities of this app once they exist._

## User preferences

1. **Lưu tiến trình cuối phiên** — cập nhật memory trước khi kết thúc chat, tránh mất dữ liệu.
2. **Đánh dấu phần đã xong** — không động vào code đã hoàn thành trừ khi có lý do rõ ràng.
3. **Tiết kiệm token** — ưu tiên phương án ít token; khi xuống 20% chuyển chế độ cực tiết kiệm.
4. **Sửa đúng chỗ** — chia file chi tiết, sai ở đâu sửa đó, không rewrite toàn bộ.
5. **Sync GitHub** — cập nhật repo chính trước, sau đó đồng bộ 3 server dự phòng. Token lưu trong Replit Secret `GITHUB_PERSONAL_ACCESS_TOKEN`.

## Gotchas

_Populate as you build — sharp edges, "always run X before Y" rules._

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
