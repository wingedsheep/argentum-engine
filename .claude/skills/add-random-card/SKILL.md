---
name: add-random-card
description: Pick a random unimplemented card from a set backlog and implement it. Balances colours automatically.
argument-hint: <path-to-backlog-cards.md>
---

# Add Random Card from Backlog

Implement a random unimplemented card from the backlog file at `$ARGUMENTS`.

## Step 1: Read the Backlog

1. Read the backlog markdown file provided as the argument (e.g., `backlog/sets/scourge/cards.md`).
2. Parse the checklist: lines starting with `- [ ]` are unimplemented, `- [x]` are implemented.
3. Count implemented cards per colour by looking up each implemented card's colour (use the colour counts table in the file header if present).

## Step 2: Pick a Random Card (Colour-Balanced)

1. Identify which colour(s) have the **fewest** implemented cards from the header table.
2. From the unimplemented cards (`- [ ]`), look up a handful of candidates on Scryfall to find their colours. Prefer cards from the under-represented colour(s).
3. Among valid candidates, pick one **randomly** — don't always pick the first one. Prefer simpler cards (creatures, simple spells) over complex ones (Storm, multi-modal, planeswalkers) to keep implementation quick, but don't avoid complexity entirely.

## Step 3: Determine the Set Code

Infer the set code from the backlog file path or header:
- `scourge` → `scg`
- `onslaught` → `ons`
- `legions` → `lgn`
- `portal` → `por`
- `alpha` → `lea`
- `khans-of-tarkir` → `ktk`

If unclear, check the file header for the set name and map it.

## Step 4: Implement the Card

Invoke the `add-card` skill with the card name and set code:

```
/add-card <Card Name> --set <set-code>
```

This handles: Scryfall lookup, card definition, set registration, effect implementation, tests, and building.

**IMPORTANT**: The add-card skill already handles updating the backlog (Step 9 in add-card). Make sure the backlog file is updated:
- Mark the card: `- [ ] Card Name` → `- [x] Card Name`
- Update the implementation count in the header
- Update the colour count in the header table

## Step 5: Verify

After the add-card skill completes, verify:
1. The backlog file has the card checked off
2. The colour count in the header is updated
3. The total implementation count is updated
