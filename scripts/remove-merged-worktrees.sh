#!/usr/bin/env bash
#
# remove-merged-worktrees.sh — remove git worktrees whose work has already landed.
#
# Why: every agent loop and fix leaves a worktree behind (~1 GB each, several GB for a loop that
# built the whole corpus). They outlive their PRs; on one box 44 worktrees of merged PRs held
# ~49 GB. `prune-worktree-builds.sh` only clears their build output — this removes the worktree.
#
# A worktree is removed only when nothing in it can be lost:
#   * its HEAD is already contained in origin/main, or
#   * its branch has a merged PR and its HEAD is contained in that PR's head commit
#     (squash merges leave no ancestry in main, so the PR is the proof).
# And it is skipped when:
#   * it is the main worktree or the current one, or it is locked;
#   * it was active within --hours (default 24): any file in it changed (build output included)
#     or its HEAD moved (newest reflog entry — not the reflog file's mtime, which `git gc`
#     rewrites; not the index's, which any `git status` refreshes). Agents start from a fresh
#     worktree at origin/main, which would otherwise read as "landed" the moment it exists;
#   * it has any uncommitted or untracked change;
#   * a running java/gradle process references its path (someone is building there);
#   * any running process has its working directory inside it (an agent session, a dev server).
# The checks never write: `git status` runs with --no-optional-locks so it can't refresh the index.
# Removal is plain `git worktree remove` (never --force), so git itself refuses a dirty tree.
# Local branches are left in place. It is a DRY RUN unless --apply is passed.
#
# Usage:
#   scripts/remove-merged-worktrees.sh            # list what would go and why the rest stay
#   scripts/remove-merged-worktrees.sh --hours 72 --apply

set -euo pipefail

APPLY=0
HOURS=24
while [ $# -gt 0 ]; do
    case "$1" in
        --apply) APPLY=1; shift ;;
        --hours) HOURS="$2"; shift 2 ;;
        --dry-run) APPLY=0; shift ;;
        -h|--help) sed -n '2,28p' "$0"; exit 0 ;;
        *) echo "remove-merged-worktrees: unknown argument '$1'" >&2; exit 2 ;;
    esac
done

if ! [[ "$HOURS" =~ ^[0-9]+$ ]] || [ "$HOURS" -lt 1 ]; then
    echo "remove-merged-worktrees: --hours must be a positive integer (got '$HOURS')." >&2
    exit 2
fi
MINUTES=$((HOURS * 60))

git fetch -q origin main 2>/dev/null || echo "remove-merged-worktrees: could not fetch origin/main; using the local copy." >&2
MAIN_REF=origin/main

# Merged PRs as "branch<TAB>head-oid<TAB>number". Without gh only the origin/main check applies.
MERGED_PRS=""
if command -v gh >/dev/null 2>&1; then
    MERGED_PRS="$(gh pr list --state merged --limit 1000 --json headRefName,headRefOid,number \
        --jq '.[] | "\(.headRefName)\t\(.headRefOid)\t\(.number)"' 2>/dev/null)" ||
        echo "remove-merged-worktrees: gh pr list failed; only worktrees already in $MAIN_REF qualify." >&2
else
    echo "remove-merged-worktrees: gh not found; only worktrees already in $MAIN_REF qualify." >&2
fi

CURRENT="$(git rev-parse --show-toplevel)"
BUSY_PROCS="$(ps -Ao command= | grep -E 'java|gradle' | grep -v grep || true)"
PROC_CWDS="$(lsof -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | sort -u || true)"

# Succeeds when [wt]'s HEAD moved or any file in it changed within the last $HOURS hours.
recently_active() {
    local wt="$1" moved
    moved="$(git -C "$wt" log -g -1 --format=%ct HEAD 2>/dev/null || echo 0)"
    [ "${moved:-0}" -ge "$(( $(date +%s) - MINUTES * 60 ))" ] && return 0
    [ -n "$(find "$wt" -path "$wt/.git" -prune -o -mmin "-$MINUTES" -print -quit 2>/dev/null)" ]
}

# Prints why [wt] at [head] on [branch] is safe to remove, or nothing.
landed_reason() {
    local head="$1" branch="$2"
    if git merge-base --is-ancestor "$head" "$MAIN_REF" 2>/dev/null; then
        echo "in $MAIN_REF"
        return
    fi
    [ -n "$branch" ] && [ -n "$MERGED_PRS" ] || return 0
    local name oid number
    while IFS=$'\t' read -r name oid number; do
        [ "$name" = "$branch" ] || continue
        git cat-file -e "$oid^{commit}" 2>/dev/null || git fetch -q origin "$oid" 2>/dev/null || continue
        if git merge-base --is-ancestor "$head" "$oid" 2>/dev/null; then
            echo "merged in #$number"
            return
        fi
    done <<< "$MERGED_PRS"
}

removed_kb=0
removed=0
kept=0

report() { # verb size path reason
    printf '%-6s %8s  %s  (%s)\n' "$1" "$2" "$3" "$4"
}

process() {
    local wt="$1" head="$2" branch="$3" locked="$4"
    [ -n "$wt" ] && [ -d "$wt" ] || return 0
    local size_kb size_h reason
    size_kb=$(du -sk "$wt" 2>/dev/null | awk '{print $1+0}')
    size_h=$(awk -v k="$size_kb" 'BEGIN{printf "%.1f GB", k/1024/1024}')
    local label="${branch:-detached}"

    if [ "$wt" = "$CURRENT" ]; then kept=$((kept + 1)); report keep "$size_h" "$wt" "current worktree"; return; fi
    if [ "$locked" = 1 ]; then kept=$((kept + 1)); report keep "$size_h" "$wt" "locked"; return; fi
    if recently_active "$wt"; then
        kept=$((kept + 1)); report keep "$size_h" "$wt" "$label: active in the last ${HOURS}h"; return
    fi
    if [ -n "$(git -C "$wt" --no-optional-locks status --porcelain 2>/dev/null)" ]; then
        kept=$((kept + 1)); report keep "$size_h" "$wt" "$label: uncommitted changes"; return
    fi
    if printf '%s\n' "$BUSY_PROCS" | grep -qF -- "$wt"; then
        kept=$((kept + 1)); report keep "$size_h" "$wt" "$label: a running java/gradle process references it"; return
    fi
    if printf '%s\n' "$PROC_CWDS" | grep -qE -- "^$(printf '%s' "$wt" | sed 's/[][\.*^$/]/\\&/g')(/|$)"; then
        kept=$((kept + 1)); report keep "$size_h" "$wt" "$label: a running process works inside it"; return
    fi
    reason="$(landed_reason "$head" "$branch")"
    if [ -z "$reason" ]; then
        kept=$((kept + 1)); report keep "$size_h" "$wt" "$label: not merged"; return
    fi

    if [ "$APPLY" -eq 1 ]; then
        if git worktree remove "$wt"; then
            removed=$((removed + 1)); removed_kb=$((removed_kb + size_kb))
            report remove "$size_h" "$wt" "$label: $reason"
        else
            kept=$((kept + 1)); report keep "$size_h" "$wt" "$label: git worktree remove refused"
        fi
    else
        removed=$((removed + 1)); removed_kb=$((removed_kb + size_kb))
        report would "$size_h" "$wt" "$label: $reason"
    fi
}

# `git worktree list --porcelain` gives one blank-line-separated record per worktree; the
# first record is the main worktree, which is never removed.
first=1
wt="" head="" branch="" locked=0
while IFS= read -r line; do
    case "$line" in
        "worktree "*) wt="${line#worktree }" ;;
        "HEAD "*) head="${line#HEAD }" ;;
        "branch "*) branch="${line#branch refs/heads/}" ;;
        locked*) locked=1 ;;
        "")
            if [ -n "$wt" ]; then
                if [ "$first" = 1 ]; then first=0; else process "$wt" "$head" "$branch" "$locked"; fi
            fi
            wt="" head="" branch="" locked=0
            ;;
    esac
done < <(git worktree list --porcelain; echo)

[ "$APPLY" -eq 1 ] && git worktree prune

echo
awk -v n="$removed" -v k="$removed_kb" -v kept="$kept" -v a="$APPLY" 'BEGIN{
    printf "%s %d worktree(s), %.1f GB; kept %d\n", (a ? "removed" : "would remove (re-run with --apply)"), n, k/1024/1024, kept
}'
