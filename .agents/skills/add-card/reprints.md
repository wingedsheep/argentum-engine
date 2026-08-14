# Reprints: adding another printing of an existing card

Read this when the canonical-placement check (SKILL.md Step 1) routes you here — either the card is
already implemented in another set, or the canonical `CardDefinition` belongs in an earlier set and the
asked-for set needs only a `Printing` row.

The engine treats cards by **name (oracle identity)**, not by printing. The canonical `CardDefinition` —
script, types, P/T — lives in exactly **one** set's package and is registered once. Reprints contribute
only per-printing presentation data: `setCode`, `collectorNumber`, art, artist, Scryfall id. They live in
the *reprinting* set's package, never inside the canonical card's file.

## Reprint or new card?

- **Same oracle text → reprint.** Add a `Printing` row to the new set's package.
- **Different oracle text** (functional reprint, errata, name change) **→ new card.** Go back to the
  normal flow and pick whichever set should hold the canonical `CardDefinition`.

## Workflow

1. **Confirm the canonical exists.** `grep -rn 'name = "<Card Name>"' mtg-sets/src/main/kotlin/` should
   find a `CardDefinition` in another set's package. If it doesn't, this is a new card — go back to
   SKILL.md Step 1.

2. **Fetch printing-only data** for the new set:
   `https://api.scryfall.com/cards/named?exact=<card-name>&set=<new-set-code>`. Take `set`,
   `collector_number`, `artist`, `image_uris.normal`, `rarity`, `released_at`, `id`, and any back face's
   `image_uris.normal`.

3. **Add a top-level `Printing` val** in the reprinting set's `cards/` package —
   `mtg-sets/.../definitions/{set}/cards/<CardName>Reprint.kt`:

   ```kotlin
   package com.wingedsheep.mtg.sets.definitions.{set}.cards

   import com.wingedsheep.sdk.model.Printing
   import com.wingedsheep.sdk.model.Rarity

   /**
    * <Card Name> reprint in <Set Name>. Canonical [com.wingedsheep.sdk.model.CardDefinition]
    * lives in another set's `cards/` package; this file contributes only presentation data.
    */
   val <CardName>Reprint = Printing(
       oracleId = "<Scryfall oracle_id>",
       name = "<Card Name>",
       setCode = "<NEW_SET_CODE>",
       collectorNumber = "<COLLECTOR_NUMBER>",
       scryfallId = "<Scryfall id>",
       artist = "<Artist Name>",
       imageUri = "<image_uris.normal — verify with curl -sI, must be 200>",
       releaseDate = "<YYYY-MM-DD>",
       rarity = Rarity.COMMON,
   )
   ```

   `CardDiscovery` scans the `cards/` package for top-level `Printing` vals, so no other registration is
   needed.

4. **Wire `printings` in the set object** if it isn't already:

   ```kotlin
   object MySet : MtgSet {
       override val code = "MYS"
       override val cards by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
       override val printings by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
       private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mys.cards"
   }
   ```

   `MtgSet.printings` defaults to empty, so a set with no reprint files needs nothing. `GameBeansConfig`
   registers `set.printings` alongside synthesised defaults; explicit reprints win when they share
   `(setCode, collectorNumber)` with a synthesised entry.

## Field notes

- `oracleId` — same value across every printing; informational only, since lookups go through
  `(setCode, collectorNumber)`. A placeholder is acceptable if the API omits it.
- `imageUri` — must match Scryfall exactly, query parameter included. Verify with `curl -sI`.
- `backFaceImageUri` — DFC reprints only, from the back face's `image_uris.normal`.
- `isPromo`, `isFullArt`, `frameEffects` — fill from `promo`, `full_art`, `frame_effects` when relevant;
  safe to omit.

## Wrapping up

- **No new test.** `MultiPrintingGameTest` already covers the seam. Add one only if you also touched the
  card's behavior.
- **Backlog** — if the new set has a `backlog/sets/{set-name}/cards.md`, mark the card if it's listed.
- **Commit** — `Add {Card Name} reprint to {New Set Name}`.

## Worked example: Lightning Bolt, M10 → 2X2

The canonical spell script lives in `mtg-sets/.../definitions/m10/cards/LightningBolt.kt`. The 2X2
printing adds only:

```kotlin
// mtg-sets/.../definitions/2x2/cards/LightningBoltReprint.kt
val LightningBoltReprint = Printing(
    oracleId = "4457ed35-7c10-48c8-9776-456485fdf070",
    name = "Lightning Bolt",
    setCode = "2X2",
    collectorNumber = "117",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/.../bolt-2x2.jpg?...",
    releaseDate = "2022-04-22",
    rarity = Rarity.UNCOMMON,
    scryfallId = "...",
)
```

`CardRegistry` still resolves "Lightning Bolt" to the M10 script. When a deck pins
`PrintingRef("2X2", "117")`, `GameInitializer` resolves the row from `PrintingRegistry` and stamps the
2X2 art onto that entity's `CardComponent.imageUri`, so the client renders the 2X2 print.
