#!/bin/bash
# Script tạm để đọc SkillService.java từ Codespace
GH_BIN="/tmp/gh_2.52.0_linux_amd64/bin/gh"
OUT="/tmp/nro_skill_dump.txt"

echo "[$(date)] Cài gh CLI nếu cần..."
if [ ! -f "$GH_BIN" ]; then
  curl -sL https://github.com/cli/cli/releases/download/v2.52.0/gh_2.52.0_linux_amd64.tar.gz | tar -xz -C /tmp/
fi

TOKEN=$(printenv GITHUB_PERSONAL_ACCESS_TOKEN)
echo "$TOKEN" | $GH_BIN auth login --with-token 2>/dev/null
echo "[$(date)] Auth OK"

CS="cautious-space-halibut-p7rwgqwxrg5gfrrqg"

echo "[$(date)] SSH vào Codespace..."
$GH_BIN codespace ssh -c "$CS" -- bash -s << 'REMOTE' > "$OUT" 2>&1
echo "=== FILE: SkillService.java ==="
cat ~/nro/SRC/src/nro/services/SkillService.java

echo ""
echo "=== FILE: Mob.java (injured method) ==="
grep -n -A 30 "void injured" ~/nro/SRC/src/nro/models/mob/Mob.java | head -60

echo ""
echo "=== SkillService grep delay/sleep ==="
grep -n "sleep\|schedule\|Thread\|delay\|hitTime\|castTime\|Timer" ~/nro/SRC/src/nro/services/SkillService.java
REMOTE

echo "[$(date)] Done. Output: $OUT ($(wc -l < $OUT) lines)"
cat "$OUT" | head -20
