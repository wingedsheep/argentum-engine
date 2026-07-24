---
name: add-random-card
description: Pick a random unimplemented card from a set and implement it. Accepts either a backlog cards.md path or a set name/code.
argument-hint: <set-name-or-code> | <path-to-backlog-cards.md>
---

# Add Random Card

Implement a random unimplemented card from the set referenced by the user's request or explicit skill arguments.

The supplied set reference can be either:
- **A set name or code** (e.g., `tempest`, `tmp`, `scourge`, `scg`) → use the `card-status` script as the source of truth.
- **A path to a backlog `cards.md`** (e.g., `backlog/sets/scourge/cards.md`) → use the hand-curated checklist.

## Step 1: Pick the source of truth

Check whether the supplied set reference looks like a file path (contains `/` or ends in `.md`).

- **Path form** → go to Step 2A (backlog mode).
- **Set name/code** → go to Step 2B (card-status mode). This is the preferred mode for any set where `scripts/card-status --set <CODE>` succeeds; it doesn't require maintaining a hand-curated checklist.

If you're unsure which mode applies, try card-status mode first and fall back to backlog mode if `scripts/card-status` errors with "no set found".

## Step 2A: Read the backlog (backlog mode)

1. Read the backlog markdown file provided as the argument.
2. Parse the checklist: lines starting with `- [ ]` are unimplemented, `- [x]` are implemented.
3. Collect all unimplemented cards into a numbered list (0-indexed). Continue to Step 3.

## Step 2B: Read card-status (card-status mode)

1. Map the argument to a set code if it's a name (e.g., `tempest` → `tmp`, `bloomburrow` → `blb`). Try the argument as-is first; `card-status` accepts both upper- and lower-case codes.
2. Run `scripts/card-status --set <CODE> --list` to get the missing-card report. Output looks like:
   ```
   Set Name (CODE) — X / Y implemented
   Missing draft:
     - Card Name One
     - Card Name Two
     ...
   Missing extras:
     - Card Name Three
     ...
   ```
3. Collect every "Missing" entry (both draft and extras sections) into a single numbered list (0-indexed). Continue to Step 3.
4. **If `scripts/card-status` errors with "no set found with code X"**: the set isn't scaffolded under `mtg-sets/.../definitions/<code>/` yet. Stop and tell the user — they'll need to scaffold the empty set first (an empty `<Set>Set.kt` registered via ServiceLoader) before this mode can work. Do NOT fall back to picking a card from Scryfall directly; the add-card skill assumes the set is registered.

## Step 3: Pick a truly random card

1. With the unimplemented list from Step 2A or 2B, use `jot -r 1 0 <max_index>` to generate a true random 0-based index. LLM "random" picks aren't truly random.
2. **Always implement the selected card.** Do NOT re-roll because it looks complex or needs new effects/mechanics — the point is to make progress on whatever lands.

## Step 4: Determine the Set Code

In card-status mode you already have the code. In backlog mode, infer it from the file path or header:
- `scourge` → `scg`
- `onslaught` → `ons`
- `legions` → `lgn`
- `portal` → `por`
- `alpha` → `lea`
- `khans-of-tarkir` → `ktk`
- `tempest` → `tmp`

If unclear, check the file header for the set name and map it.

## Step 5: Implement the Card

Invoke the `add-card` skill with the card name and set code:

```
/add-card <Card Name> --set <set-code>
```

This handles: Scryfall lookup, card definition, set registration, effect implementation, tests, and building.

## Step 6: Verify

After the add-card skill completes:

- **Backlog mode**: confirm the `cards.md` file has the card checked off, the colour count in the header is updated, and the total implementation count is updated (add-card handles this in its Step 9).
- **Card-status mode**: re-run `scripts/card-status --set <CODE>` and confirm the implemented count went up by 1 and the picked card no longer appears in `--list` output. No backlog file to update.
