# Portent of Calamity — incomplete resolution

**File:** `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/bloomburrow/cards/PortentOfCalamity.kt`

**Card text ({X}{U} Sorcery):**

> Reveal the top X cards of your library. For each card type, you may exile a card of that type
> from among them. Put the rest into your graveyard. You may cast a spell from among the exiled
> cards without paying its mana cost if you exiled four or more cards this way. Then put the rest
> of the exiled cards into your hand.

## Current behaviour

The current pipeline is:

1. `GatherCardsEffect` — reveal top X cards into a collection.
2. `SelectFromCollectionEffect` — player chooses **up to 9** cards from the revealed pile (no
   per-card-type constraint enforced, just a free choice, using a text prompt).
3. `MoveCollectionEffect` — remainder → graveyard.
4. `MoveCollectionEffect` — selected cards → **hand**.

There is an explicit `// TODO` in the card file acknowledging the simplification.

## What's broken

Two distinct bugs:

### 1. "One per card type" constraint is not enforced
The player can freely pick any cards (bounded only by the hard-coded 9 = total number of MTG card
types). The intended constraint is that the chosen set must contain **at most one card of each
card type** (artifact, battle, creature, enchantment, instant, kindred, land, planeswalker,
sorcery). A pile with three creatures should only let you exile one of them.

### 2. Conditional free cast is missing entirely
Selected cards go directly from the revealed pile to **hand**; they are never exiled, and no
"cast without paying mana cost" permission is ever offered. The real card:

- Exiles the chosen cards.
- If **4 or more** were exiled, the player may cast **one** of them without paying its mana cost
  (during the resolution of Portent of Calamity, per the 2024-07-26 ruling).
- The rest of the exiled cards then move to hand.

The "4 or more" threshold is what makes the card interesting — it rewards casting for a high X.

## Fix plan

This needs new effect infrastructure, not just a card edit.

1. **New selection mode: `OnePerCardType`** (or a new `SelectFromCollectionEffect` constraint)
   that partitions the collection by card type and presents the selection UI as one slot per type.
   The engine must enforce the constraint server-side when validating the decision.

2. **Rewire the pipeline** to exile the selected cards instead of moving them to hand:
   `Gather → Select(OnePerCardType) → Move(graveyard, remainder) → Move(exile, selected)`.

3. **Conditional free-cast-from-exile mid-resolution.** The engine already supports
   `GrantMayPlayFromExileEffect` with duration, but this card needs:
   - A gate: only offered if `|exiled| >= 4`.
   - A one-shot: the permission is for casting **one** spell, not "you may play them".
   - Mid-resolution scheduling: the cast must happen **as Portent resolves**, before the
     remaining exiled cards go to hand.

   Likely needs a new `CastFromExileDuringResolutionEffect` or an extension of the existing
   "cast without paying mana cost" mid-resolution path (see how flashback / suspend's free cast
   is wired for precedent).

4. **Move remaining exiled cards to hand** after the optional free cast resolves (or after it's
   declined).

## Impact

The card is unplayably weak / incorrect as-is:
- No free cast = the card's upside is completely removed; it's "pay XU, reveal X, put some in hand".
- No card-type constraint = in certain decks the player can hoard more cards than the card allows.

## Related

- `GrantMayPlayFromExileEffect` — used by Season of the Bold for a similar "play from exile"
  mechanic, but that one grants a duration, not a one-shot free cast during resolution.
- `SelectFromCollectionEffect` / `SelectionMode` — the constraint hook would live here.
