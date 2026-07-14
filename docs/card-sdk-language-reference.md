# Card SDK Language Reference

A complete catalog of every building block available to card authors in the Argentum
Engine `mtg-sdk`, with a one-line description for each. Designed to be scanned and
searched. For step-by-step authoring workflow see [`api-guide.md`](api-guide.md) (and use the
`add-card` skill); for hard cases see
[`managing-complex-and-rare-abilities.md`](managing-complex-and-rare-abilities.md).

**Maintenance rule:** this document is the canonical SDK catalog. **Every change to the
SDK — new effect, trigger, condition, filter, cost, keyword, dynamic amount, modal
shape, replacement effect, etc. — must update the matching section here in the same
change.** If the entry doesn't fit cleanly in an existing section, add or rename a
section; do not let SDK additions land without a corresponding doc update.

---

## 1. Top-level card DSL

**Entry points**

- `card("Name") { ... }` — open the builder for a standard card.
- `basicLand("Plains" | "Island" | "Swamp" | "Mountain" | "Forest" | "Wastes")` — shortcut for basic lands (sets
  type line, intrinsic mana ability, supertype). The five colored types get a subtype after the dash and tap for
  their color; `"Wastes"` is the colorless basic — type line `Basic Land` with no subtype, intrinsic `{T}: Add {C}`.
  Supports `collectorNumber`, `artist`, `flavorText`, `imageUri`, `rarity`, and `inBooster` (set `false` to keep an
  art variant defined but exclude it from the draft/sealed deck-building basic pool).

**Card builder properties**

- `manaCost: String` — mana cost in `{X}{R}{U}` syntax. Supported pip forms: generic (`{2}`),
  colored (`{R}`), colorless (`{C}`), variable (`{X}`), hybrid (`{W/U}` — either colour),
  Phyrexian (`{W/P}` — colour or 2 life), and monocolored hybrid / "twobrid" (`{2/B}` — two
  generic **or** one mana of the colour; mana value counts the generic side per CR 202.3f).
  Gurmag Nightwatch's `{2/B}{2/G}{2/U}` is the canonical twobrid example.
- `typeLine: String` — full type line including supertypes and subtypes. A `Legendary Instant` /
  `Legendary Sorcery` automatically gets the CR 205.4e casting restriction (can be cast only while
  its controller controls a legendary creature or legendary planeswalker) — the engine enforces this
  from the type line in both legal-action enumeration and the cast handler; no per-card opt-in needed.
- `oracleText: String` — rules text; auto-generated from abilities if omitted.
- `power: Int?`, `toughness: Int?` — base P/T for creatures.
- `dynamicPower`, `dynamicToughness` — characteristic-defining P/T (e.g. `*/*` Tarmogoyf).
- `dynamicStats(source, powerOffset?, toughnessOffset?)` — sets both with optional `±` deltas.
- `startingLoyalty: Int?` — starting loyalty for planeswalkers.
- `colorIdentity: String?` — override (normally auto-detected). Treated as authoritative in this repo.
- `colorIndicator: String?` — explicit color indicator (CR 204), e.g. `"B"`. `null` (default) = no
  indicator; the card's color is its mana-cost colors alone. Set it on a face printed with a color
  indicator instead of colored mana symbols — most often a transforming DFC back face with an empty
  mana cost (e.g. The Grim Captain's black back face reads as black despite `manaCost = ""`). The
  indicated colors combine with any mana-cost colors (CR 202.2) and fold into color identity (CR 903.4).
  Prefer this over the older `colorIdentity`-only approximation, which left such faces colourless.
- `auraTarget: TargetRequirement?` — what this Aura enchants. Usually a permanent (`Targets.Creature`),
  but `Targets.Player` makes it an **"enchant player"** Aura: it attaches to a player via
  `AttachedToComponent` (players are entities too), survives state-based actions while that player is in
  the game, and exposes the player through `Player.EnchantedPlayer` / `EventPattern.LifeGainEvent(EnchantedPlayer)`
  and a `takesDamage(binding = ATTACHED)` "whenever enchanted player is dealt damage" trigger. (Grievous Wound.)
- `morph: String?` — morph mana cost (cast face-down).
- `morphCost: PayCost?` — non-mana morph cost.
- `morphFaceUpEffect: Effect?` — effect that fires when this morph turns face up.
- `warp: String?` — Warp alt-cost; exiles at end of turn.
- `evoke: String?` — Evoke alt-cost; sacrifices on ETB.
- `selfAlternativeCost: SelfAlternativeCost?` — generic alternative-cost slot.
- `castTimeCreatureTypeChoice: CastTimeCreatureTypeSource?` — forces a creature-type choice at cast time.
- `cantBeCountered: Boolean` — spell is uncounterable.
- `cantBeCopied: Boolean` — spell can't be copied (CR 707.10); copy effects that name it create no copy (Display of Power).
- `conditionalFlash: Condition?` — gains flash while condition holds.
- `layout: CardLayout` — physical layout shape (see §2).

**Ability blocks inside `card { ... }`**

- `triggeredAbility { ... }` — "when/whenever/at" abilities.
- `staticAbility { ... }` — continuous effects.
- `activatedAbility { ... }` — `cost: effect` abilities.
- `loyaltyAbility(±N) { ... }` — planeswalker loyalty abilities.
- `replacementEffect { ... }` — "instead/if … would" replacement.
- `keywords(...)` / `keywordAbility(...)` / `keywordAbilities(...)` — add keyword abilities.
- `spell { ... }` — define the spell payload for instants/sorceries and Adventure / Omen faces.
- `leyline()` — Leyline mechanic ("If this card is in your opening hand, you may begin the game with it on the
  battlefield"). Sets `CardScript.mayStartOnBattlefield = true`. After all mulligans and bottoming resolve, the
  engine walks each player in turn order from the active player and presents a yes/no decision per leyline card
  in their opening hand; a "yes" routes the card to the battlefield through the standard zone-change pipeline
  before the first turn begins, a "no" leaves it in hand.

---

## 2. Card faces, layouts, printings, set metadata

**`CardLayout`**

- `NORMAL` — standard single face (default).
- `SPLIT` — two or more halves on one card; combined characteristics apply off-battlefield (CR 709.4c). Used for Rooms,
  Fuse, Aftermath, and the classic Invasion split cards (Pain // Suffering, Stand // Deliver, Wax // Wane). Each half is
  cast independently via `CastSpell.faceIndex`; only the chosen half goes on the stack (CR 709.4). A non-permanent half
  carries its effect in a `face("Name") { spell { … } }` block (with its own `target(...)` requirements); a permanent
  half (Room) carries triggered/activated/static abilities instead.
  - **Room face abilities are door-gated (CR 709.5).** A Room face's abilities function only while that door is
    unlocked. Triggered abilities are scoped to the unlocked face in `TriggerDetector`; **static** abilities —
    continuous effects, `GrantActivatedAbility` (incl. granted mana abilities), and `NoMaximumHandSize` — are folded
    in by the engine helper `RoomFaceStatics.activeStaticAbilities(container, cardDef)`, the single source of truth
    every battlefield static-ability scan reads (continuous-effect projection, clickable granted abilities, the mana
    auto-payer, no-maximum-hand-size). So a static printed on a Room face (e.g. Greenhouse's "Lands you control have
    '{T}: Add one mana of any color.'") works exactly like the same static on a normal permanent, but only once its
    door is unlocked. The baked continuous-effect component is refreshed when a door unlocks (via `RoomDoorUnlocker`),
    the same way a transform re-bakes it. (Replacement effects on a Room face are not yet door-gated — no current card
    needs one.)
- `ADVENTURE` — primary face is a permanent (usually a creature; **may also be a land** — FIN Towns), `cardFaces[0]`
  is an instant/sorcery Adventure (CR 715). Resolving the Adventure exiles the card and grants permission to play
  the primary face from exile — *cast* the creature, or *play* the land for a **land // spell** Adventure
  (`Land — Town // Sorcery — Adventure`, e.g. Ishgard, the Holy See // Faith & Grief). The generic may-play
  permission covers both; from hand a land-primary Adventure offers *play the land* (PlayLandEnumerator) **and**
  *cast the Adventure spell* (`CastSpell.faceIndex = 0`).
- `OMEN` — primary face is a permanent (creature), `cardFaces[0]` is an instant/sorcery Omen (Tarkir: Dragonstorm).
  Casts exactly like an Adventure (creature face, or Omen via `CastSpell.faceIndex = 0`), but resolving the Omen
  **shuffles the card into its owner's library** instead of exiling it — no cast-from-exile linkage. DSL:
  `card { omen("Name") { spell { … } } }`.
- `MODAL_DFC` — primary characteristics are the front face, `cardFaces[0]` is the back face (CR 712). Cast **one**
  face from hand (front via primary characteristics, back via `CastSpell.faceIndex = 0`), never both. Unlike
  ADVENTURE there is no exile-then-recast linkage — a spell back resolves as an ordinary spell (graveyard, or exile
  when its script sets `selfExileOnResolve` via `spell { selfExile() }`). DSL: `card { modalBack("Name") { spell { … } } }`.
- `PREPARE` — primary characteristics are the creature face, `cardFaces[0]` is the **prepare spell** (an
  instant/sorcery) (Secrets of Strixhaven). The card is only ever cast as the creature; the prepare spell is never
  cast from hand. A creature that carries `Keyword.PREPARED` ("This creature enters prepared") becomes prepared on
  enter; one without the keyword (e.g. Leech Collector) only becomes prepared via an effect — `Effects.BecomePrepared(target)`.
  When it becomes prepared, the engine creates a **copy of the prepare spell in exile** that the controller may cast for
  the face's cost — surfaced by the cast-from-exile enumerator as `CastSpell(..., faceIndex = 0)` from `EXILE`.
  Casting the copy unprepares the creature; the copy ceases to exist on resolution. The exile copy persists in
  exile (exempt from the 707.10a phantom-copy SBA) until the source leaves the battlefield or stops being prepared,
  at which point it is cleaned up. A creature already prepared does not re-prepare. DSL: `card { prepare("Name") { spell { … } } }`.

**`CardFace` (SPLIT / ADVENTURE / OMEN / MODAL_DFC / PREPARE)**

- `name` — face name.
- `manaCost` — face mana cost.
- `typeLine` — face type line.
- `script { ... }` — that face's abilities; for instant/sorcery SPLIT halves, Adventures, and modal DFC spell
  faces this includes a `spell { effect = …; target(...) }` block holding the face's effect and target
  requirements (plus `selfExile()` for faces that exile themselves on resolution).
- `keywords` — face-local keywords.
- `imageUri` — face art when it differs from the front (MODAL_DFC backs have their own Scryfall image).

**`metadata { ... }`**

- `rarity: Rarity` — `COMMON | UNCOMMON | RARE | MYTHIC | SPECIAL | BONUS`.
- `collectorNumber: String` — Scryfall collector number.
- `artist: String` — illustrator credit.
- `flavorText: String` — italicized flavor.
- `imageUri: String?` — art URL; auto-fetched from Scryfall if omitted.
- `imageRotation: Int` — clockwise degrees to rotate the art when rendered (default `0`). Set `180` for
  flip-layout tokens whose only Scryfall image shows the other face upright — the WOE Role tokens are printed
  two-to-a-card (`Wicked // Cursed`, `Monster // Sorcerer`), so the bottom face (`Cursed`, `Sorcerer`) reads
  upside-down on the single image. Purely cosmetic: flows SDK → `ClientCard.imageRotation` → client CSS transform;
  the engine never reads it.
- `scryfallId: String?` — Scryfall UUID.
- `releaseDate: String?` — `YYYY-MM-DD`.
- `inBooster: Boolean` — part of the draft/sealed product (default `true`; `false` for Special Guests / starter
  exclusives). Gates both the booster pool and the basic-land variants offered during limited deck building.
- `oracleTextOverride: String?` — bypass auto-generated oracle text.

**Reprints** — add a `Printing` row in the new set's `Reprints.kt` and wire it into `MtgSet.printings`. Never duplicate
the `CardDefinition`.

**`Printing`** — a presentation-only row for one printing of a card (oracle identity stays on the `CardDefinition`).
Carries `setCode`, `collectorNumber`, `scryfallId`, `artist`, `imageUri`, `backFaceImageUri`, `releaseDate`, `rarity`,
plus the frame fields:

- `isFullArt: Boolean` — Scryfall full-art treatment.
- `frameEffects: List<String>` — Scryfall `frame_effects` (e.g. `["showcase"]`, `["inverted"]`).
- `borderColor: String?` — Scryfall `border_color` (`"black" | "white" | "borderless"`).
- `isAlternateFrame: Boolean` (derived) — true when the printing is a **showcase** frame
  (`"showcase" in frameEffects`) or **borderless** (`borderColor == "borderless"`). This is the predicate the booster
  variant slot selects on; plain full-art / promo treatments are not counted.

`CardDefinition.withPrinting(printing)` returns a copy presenting that printing — it overlays only presentation
metadata (set code, collector number, art, artist, Scryfall id, and the back-face art for genuine DFCs) and leaves the
card's oracle identity untouched.

**Showcase / borderless in boosters** — a set advertises a per-card variant rate via `MtgSet.boosterVariantChance`
(default `0.0`). When non-zero, `BoosterGenerator` rolls each generated card independently and, on a hit, re-skins it
with one of its `isAlternateFrame` `printings` of the same name (via `applyVariantPrintings` →
`CardDefinition.withPrinting`). The swap is presentation-only: it changes the art shown in the draft/sealed pool, not
the card's rules or its in-game (name-resolved) art. Lorwyn Eclipsed sets `boosterVariantChance = 0.15` and contributes
its showcase/borderless rows via `LorwynEclipsedVariantPrintings` — the play-booster treatments only, with the
collector-only ones (reversible shocklands, Japanese Showcase, Fracture Foil, serialized/headliner chase cards)
excluded.

---

## 3. Costs (`Costs.*`)

> **One cost vocabulary (`CostAtom`).** The payable things shared across cost *contexts* — mana, life,
> sacrifice, discard, exile-from-zone, tap, return-to-hand, reveal — are defined **once** in the
> `CostAtom` sealed hierarchy (`scripting/costs/CostAtom.kt`). All three context wrappers carry them via
> an `Atom(atom)` member: `PayCost.Atom`, `AdditionalCost.Atom`, and `AbilityCost.Atom` each hold one
> `CostAtom`, leaving only their genuinely context-specific members on the wrapper
> (`PayCost.OwnManaCost` / `PayCost.Choice`; `AdditionalCost`'s Behold / Blight / Forage / ChooseEntity
> / per-target life / variable exile; `AbilityCost`'s `Free`, `Tap`/`Untap`, the X-variable costs
> (`PayXLife`, `ExileXFromGraveyard`, `TapXPermanents`), the self-referential `SacrificeSelf` /
> `ExileSelf` / `ExileGrantingPermanent`, counter-removal, `Loyalty`, `Composite`, and named mechanics
> `Forage` / `Blight` / `Craft`). The `Costs.*` facades below are unchanged — they construct the right
> `…Atom(CostAtom.X(…))` for you, so card authoring is identical. A *new* payable thing is one
> `CostAtom` variant + one engine payment branch, available in every context.

- `Costs.Free` — costs nothing (`{0}`).
- `Costs.Tap` — `{T}`; tap this permanent.
- `Costs.Untap` — `{Q}`; untap this permanent.
- `Costs.Mana("{2}{U}")` — pay the given mana cost (string or `ManaCost`).
- `Costs.PayLife(amount)` — pay N life.
- `Costs.PayXLife` — pay X life, where X is the value chosen for the ability's `{X}` mana cost
  (e.g. "{X}{B}, {T}, Pay X life: …" on Krumar Initiate). The X-linked counterpart to
  `Costs.PayLife`; `calculateMaxAffordableX` caps X at the controller's life total — X may go as
  high as their current life, paying down to exactly 0 (legal per CR 119.4; they then lose to a
  state-based action).
- `Costs.Sacrifice(filter)` — sacrifice a permanent matching the filter (may include self).
- `Costs.SacrificeAnother(filter)` — sacrifice a *different* permanent matching the filter.
- `Costs.SacrificeMultiple(count, filter = Any, distinctNames = false)` — sacrifice `count` matching permanents. With `distinctNames = true` the chosen permanents must all have **different names** ("sacrifice three artifact tokens with different names" — Transmutation Font); the cost is only payable when ≥ `count` distinctly-named candidates exist, and the activation always pauses for the selection (it's a real choice even when candidates == count).
- `Costs.DiscardCard` — discard a card you choose (any card).
- `Costs.Discard(filter, count = 1, atRandom = false)` — discard `count` cards matching the filter.
  When `atRandom` is true the engine picks the cards (no player selection); otherwise the player
  chooses which cards to discard.
- `Costs.DiscardAtRandom(count, filter)` — discard `count` cards chosen at random (Meteor Storm:
  "Discard two cards at random").
- `Costs.DiscardHand` — discard your entire hand.
- `Costs.DiscardSelf` — discard this card (cycling-style).
- `Costs.DiscardLastDrawnThisTurn` — discard the specific card you drew most recently this turn
  (Jandor's Ring: "{2}, {T}, Discard the last card you drew this turn: Draw a card."). The engine
  tracks the per-player most-recently-drawn entity on `GameState.lastCardDrawnThisTurnByPlayer`
  (updated at every `CardsDrawnEvent` emit site during a turn; the last id of a multi-card draw
  wins; cleared at every turn boundary) and discards it automatically — no player selection. The
  cost is unpayable when the controller has not drawn a card this turn or the tracked card has
  since left their hand (matches the Scryfall ruling: "If you do not have the card still in your
  hand, you can't pay the cost").
- `Costs.ExileSelf` — exile this permanent (or graveyard card, for graveyard-activated abilities).
- `Costs.ExileFromGraveyard(count, filter)` — exile N matching cards from your graveyard.
- `Costs.ExileXFromGraveyard(filter)` — exile X cards from your graveyard (X = the ability's
  chosen X value).
- `Costs.Forage()` (ability cost) / `Costs.additional.Forage` (additional cost) — Forage (CR
  701.61): "exile three cards from your graveyard **or** sacrifice a Food." A *choice* between two
  sub-costs that belongs to the player. All cost-shaped forage payment is unified in the engine's
  `ForageCostResolver`: the enumerators surface the available modes as separate legal actions (the
  same multi-action pattern the "OrPay" costs use — `ExileFromGraveyard` and `SacrificePermanent`
  cost-info, so the client's existing pickers let the player choose the mode *and* which cards/Food),
  and payment honors that choice, only auto-paying a legal mode when none was supplied (AI /
  engine-direct). Used as an activated/mana-ability cost (Camellia, Thornvault Forager), a modal
  additional cost (Feed the Cycle), and the graveyard-cast permission (Osteomancer Adept, where the
  card being cast is excluded from the exile pool). For a "you may forage" *effect* (not a cost) use
  `Patterns.Mechanic.forage(afterEffect?)` instead.
- `Costs.Craft(filter, minCount = 1, maxCount = null)` — Craft material cost (CR 702.167a): exile
  this permanent **and** exile at least `minCount` (and, when `maxCount` is set, at most `maxCount`)
  cards matching `filter` selected from the combined pool of
  permanents you control and cards in your graveyard. Exact-count crafts ("Craft with artifact" =
  exactly one, "Craft with two creatures" = exactly two) set `maxCount == minCount`; "... or more"
  wordings leave `maxCount = null`. Atomic because CR 702.167a pairs the
  self-exile with the materials-exile in one clause. Records the chosen materials on the source's
  `CraftedFromExiledComponent` so the back face's CDA can read them after the source returns
  transformed. Always combined with `Mana(...)` and used with the
  `Effects.ReturnSelfFromExileTransformed` resolution effect (the `card { craft(filter, cost) }`
  helper wires the whole pattern).
  - **Heterogeneous per-slot craft** — `card { craft(slots = listOf(f1, f2, ...), cost, materialDescription?) }`
    for crafts that name one material of *each* of several kinds ("Craft with a Dinosaur, a Merfolk, a
    Pirate, and a Vampire" — Throne of the Grim Captain). Each slot is filled by exactly **one distinct**
    material, so validating a chosen set is a bipartite perfect-matching problem, not a per-subtype count
    (a single Merfolk Pirate fills only one slot; four Vampires cannot cover four different subtypes). The
    built `AbilityCost.Craft` carries the per-slot filters in `slots` plus a union `filter` (`anyOf` of the
    slots) with `minCount == maxCount == slots.size`, so the flat BF+GY candidate gathering, `canPay`, the
    legal-action enumerator, and the client material overlay work unchanged; the engine layers the
    matching check (`CraftSlotMatching`, Kuhn's augmenting-path — same routine as `BlockPhaseManager`) on
    top in `canPay`, enumeration, and payment. The legal action still ships one flat material list
    (min = max = slot count); an illegal set that can't fill every slot is rejected at payment time
    (no per-slot selection UI).
- `Costs.Composite(c1, c2, ...)` — multiple costs paid together.

**Spell-level alternatives**

- `selfAlternativeCost` — generic "cast instead for" alt-cost.
- `evoke` — pay evoke cost; creature is sacrificed at ETB.
- `morph` — cast face-down for `{3}`-ish.
- `warp` — cast from anywhere; exiled at end of turn.
- `conditionalFlash` — flash while condition holds.
- `cantBeCountered` — spell is uncounterable.
- `cantBeCopied` — spell can't be copied (CR 707.10).
- `xManaRestriction = setOf(Color.BLACK, Color.RED)` — "spend only [colors] on X." Restricts which
  mana may pay the `{X}` portion of the cost (the fixed colored/generic portion is unaffected).
  Available in both `spell { }` and `activatedAbility { }` blocks; honored by the mana solver and the
  payment path. Per-color amount spent on X is then readable via `DynamicAmount.ManaSpentOnX(color)`.
  Soul Burn (`spell { xManaRestriction = setOf(Color.BLACK, Color.RED) }`) and Atalya, Samite Master
  (`activatedAbility { xManaRestriction = setOf(Color.WHITE) }`) are the first users.

**`Costs.additional.*`** (wraps `AdditionalCost`) — extra costs paid alongside the mana cost. Card
definitions construct these through the facade, e.g. `Costs.additional.SacrificePermanent(Filters.Creature)`.

- `Costs.additional.ReturnToHand(filter = Filters.Any, count = 1)` — "as an additional cost to cast
  this spell, return [count] permanent(s) you control to its owner's hand" (Fear of Isolation). Paid
  as the spell is cast (CR 601.2f) via `additionalCostPayment.bouncedPermanents`; the enumerator
  surfaces the returnable permanents (a `costType = "ReturnToHand"` cost) and the client picks them
  on the battlefield. The bounce goes through `ZoneTransitionService.moveToZone(…, Zone.HAND)`, so
  attached Auras fall off and tokens cease to exist. Mirrors the sacrifice/tap additional-cost path.
- `Costs.additional.BlightVariable` — "as you cast, you may pay X life" (Blight X); X exposed via
  `DynamicAmount.AdditionalCostBlightAmount`.
- `Costs.additional.PayXLife(minCount = 0)` — "as an additional cost to cast this spell, pay X life."
  The caster declares X at cast time (capped at their current life total) and X is fed to the spell's
  effects through the resolution **X value** — i.e. read it with `DynamicAmount.XValue` and filter with
  `CardPredicate.ManaValueAtMostX` / `manaValueAtMostX()` (Vicious Rivalry: "pay X life; destroy all
  artifacts and creatures with mana value X or less"). A card using this cost must **not** also have an
  `{X}` in its mana cost — both write the same X slot. The client shows a numeric X picker (no target
  step); the AI declares X = 0 by default.
- `Costs.additional.PayLifePerTarget(amountPerTarget)` — "this spell costs N life more to cast for
  each target." Pair with an unbounded `TargetCreature(unlimited = true)` etc.; the engine
  auto-pays `amountPerTarget × action.targets.size` at cast resolution (Phyrexian Purge).
- `Costs.additional.PayLifeEqualToManaValueOfSpell` — auto-pays life equal to the cast spell's own
  mana value. The substitute cost for "pay life equal to its mana value rather than pay its mana cost"
  (Valgavoth, Terror Eater; Bolas's Citadel-style effects). Pair it with a play-from-exile grant whose
  mana cost is waived — `GrantMayCastFromLinkedExile(withoutPayingManaCost = true, additionalCost =
  Costs.additional.PayLifeEqualToManaValueOfSpell)` — so the only cost paid is the life. The amount is
  read from the cast card's mana value, checked at cast time (CR 119.4 — must have at least that much life).
- `Costs.additional.ExileFromGraveyardOrPay(exileCount, alternativeManaCost, filter = Filters.Any)`
  — "as an additional cost to cast this spell, exile N cards from your graveyard or pay {mana}"
  (Soaring Stoneglider: "exile two cards from your graveyard or pay {1}{W}"). The sibling of the
  `BlightOrPay` / `BeholdOrPay` "do X or pay mana" shapes. The enumerator offers up to two cast
  paths: the **exile path** (base cost + a graveyard-card selection of exactly `exileCount` cards
  matching `filter`, surfaced as a `costType = "ExileFromGraveyard"` cost — the same client picker
  used by a mandatory graveyard-exile cost) and the **pay path** (base cost + `alternativeManaCost`
  folded in). The chosen path is recovered at payment time from whether the cast action's
  `additionalCostPayment.exiledCards` is non-empty; the exile path is only offered when the
  graveyard holds at least `exileCount` matching cards.
- `Costs.additional.SacrificeOrPay(filter = Filters.Any, alternativeManaCost, count = 1)` — "as an
  additional cost to cast this spell, sacrifice a [filter] or pay {mana}" (Louisoix's Sacrifice:
  "sacrifice a legendary creature or pay {2}"). The sibling of `ExileFromGraveyardOrPay` /
  `BlightOrPay` / `BeholdOrPay` for the "sacrifice a permanent or pay mana" shape. The enumerator
  offers up to two cast paths: the **sacrifice path** (base cost + a battlefield selection of
  exactly `count` permanents you control matching `filter`, surfaced as a `costType =
  "SacrificePermanent"` cost — the same on-battlefield picker used by a plain sacrifice cost like
  Natural Order) and the **pay path** (base cost + `alternativeManaCost` folded in). The chosen path
  is recovered at payment time from whether the cast action's `additionalCostPayment.sacrificedPermanents`
  is non-empty; the sacrifice path is only offered when you control at least `count` matching
  permanents, so with nothing to sacrifice only the pay path is castable.

**`Costs.pay.*`** (wraps `PayCost`) — payable costs used by [`PayOrSufferEffect`](#15-replacement-effects) ("do X
unless you Y") and by `morphCost` (non-mana face-up cost). Distinct from `AbilityCost` / `Costs.*`
which model an ability's activation cost; `PayCost` models a single cost the engine prompts the
player to pay against an alternative consequence.

`PayOrSufferEffect(cost, suffer, player = EffectTarget.Controller)` defaults to charging the ability's
controller, but **`player` may route the decision *and* the payment to any other player** — most
usefully `EffectTarget.PlayerRef(Player.TriggeringPlayer)` on a death trigger, which resolves to the
dying permanent's last-known controller. The `suffer` consequence still resolves under the **ability's
controller** (not the payer), so `EffectTarget.Controller` inside it means *you*: Meathook Massacre II's
"whenever a creature an opponent controls dies, *they* may pay 3 life. If they don't, return that card
under *your* control" is `PayOrSufferEffect(player = PlayerRef(TriggeringPlayer), cost = Costs.pay.PayLife(3),
suffer = Composite(Move(TriggeringEntity → battlefield, controllerOverride = Controller), AddCounters(finality)))`.

Non-mana `morphCost` payment is routed through the shared engine `CostPaymentService`, so **every
`Costs.pay` variant below works as a morph cost** (including `Tap` / `Choice` / `OwnManaCost`): turning
the creature face up pauses for the cost-specific decision and only flips once the cost is paid.
(Mana morph costs keep their own up-front payment — explicit mana-source selection, X, auto-tap
preview — in the turn-face-up handler.)

- `Costs.pay.Mana(ManaCost)` — pay mana (auto-taps lands via the solver). "...unless you pay {U}{U}"
  (Vaporous Djinn).
- `Costs.pay.OwnManaCost` — pay the mana cost of the permanent the cost applies to (its *own* mana
  cost, read from `CardComponent.manaCost` at payment time). Use for granted abilities like
  Essence Leak ("...sacrifice this permanent unless you pay its mana cost"), where the affected
  permanent — not a fixed cost — owns the mana cost. The engine resolves it into a concrete
  `Costs.pay.Mana` against that permanent before prompting.
- `Costs.pay.PayLife(amount)` — pay N life; offered only when the player's life total is at least N
  (CR 119.4). "...unless you pay 3 life."
- `Costs.pay.Discard(filter = Any, count = 1, random = false)` — discard cards matching `filter`.
  Random variant prompts a yes/no and the engine picks the discards (Pillaging Horde).
- `Costs.pay.Sacrifice(filter = Any, count = 1)` — sacrifice permanents you control matching
  `filter`. Source is auto-excluded. "...unless you sacrifice three Forests" (Primeval Force).
- `Costs.pay.Exile(filter = Any, zone = HAND, count = 1)` — exile cards from `zone` matching
  `filter`. "...unless you exile a blue card from your hand."
- `Costs.pay.Tap(filter = Any, count = 1)` — tap untapped permanents you control matching `filter`.
  Source is auto-excluded. Tapping each emits a `TappedEvent` so "becomes tapped" triggers fire.
  "...unless you tap an untapped permanent you control" (Command Bridge).
- `Costs.pay.Choice(options)` — present several `PayCost`s; player picks one (or the suffer effect).
  Unaffordable options are hidden. "...unless they sacrifice a nonland permanent or discard a card."
- `Costs.pay.ReturnToHand(filter, count = 1)` — return permanents you control to their owner's hand.
  Currently only consumed by `morphCost`; not yet wired into `PayOrSufferEffect`.
- `Costs.pay.RevealCard(filter, count = 1)` — reveal a card from hand matching `filter`. Currently
  only consumed by `morphCost`; not yet wired into `PayOrSufferEffect`.

---

## 4. Effects (`Effects.*`)

Atomic effect factories. For library/zone manipulation, prefer the pipelines in §5.

### Damage

- `DealDamage(amount, target)` — deal fixed/dynamic damage.
- `DealDamageExcessToController(amount, target)` — deal damage to a creature; any amount beyond
  lethal (CR 120.4a) is dealt to that creature's controller instead (the creature is marked only with
  the lethal portion). Backed by `DealDamageEffect.excessToController`. Used by Gandalf's Sanction.
- `DealXDamage(target)` — deal X damage (spell's X).
- `AmplifyNoncombatDamageThisTurn(bonus)` — install an until-end-of-turn replacement (CR 616): every
  source you control deals `bonus` *additional* noncombat damage to any permanent or player this turn.
  Combat damage is unaffected; no opponent restriction. `bonus` (a `DynamicAmount`) is resolved once at
  resolution and baked in (typically `DynamicAmount.XValue` from an `{X}` cost); multiple installs stack
  additively. Read at damage time by the engine's static-amplification path, then cleaned up at end of
  turn. Distinct from the opponent-only, permanent-tied `NoncombatDamageBonus` static. Taii Wakeen,
  Perfect Shot: `{X}, {T}: … it deals that much damage plus X instead.`
- `DoubleDamageToPlayer(target, duration = UntilYourNextTurn)` — install a duration-bounded replacement
  (CR 616) that *doubles* all damage — any source, combat or noncombat — dealt to `target` (a player,
  e.g. `EffectTarget.PlayerRef(Player.TriggeringPlayer)`) and to any permanent that player controls. The
  player is resolved once at resolution and baked into a floating effect scoped to that player, so the
  doubling outlives the source that created it (CR 611.2) and lasts the whole `duration`. Read at damage
  time by the engine's static-amplification path — combat damage is doubled per already-assigned recipient
  (assignment/division happens before doubling) and stays attributed to the original source. Two installs
  on the same player each double once (⇒ ×4). Backs the "Stagger" ability word — Lightning, Army of One:
  "Whenever Lightning deals combat damage to a player, until your next turn, if a source would deal damage
  to that player or a permanent that player controls, it deals double that damage instead." Distinct from
  the permanent-hosted, "you"/"opponent"-relative `DoubleDamage` replacement (Furnace of Rath).
- `Fight(target1, target2, excessDamageVariable?)` — two creatures each deal damage equal to their power
  to each other (CR 701.14). When `excessDamageVariable` is set, the excess damage (CR 120.4a, deathtouch-
  and marked-damage-aware) that `target1` deals **to `target2`** is stored into that pipeline number
  variable for a following effect to read via `DynamicAmount.VariableReference` — e.g. The Last Agni Kai:
  `Fight(yours, theirs, "excess") then AddMana(RED, VariableReference("excess"))`.
- `DividedDamageEffect(totalDamage, minTargets, maxTargets, dynamicTotal?)` — "N damage divided as you
  choose among target ..." The targets come from the ability's target requirement; pair with
  `TargetCreature(count, minCount)` (Forked Lightning, Skirk Volcanist) or, for "any number of target"
  + a dynamic total, a `TargetObject(optional = true, dynamicMaxCount = ..., filter = ...)`. Set
  `dynamicTotal` (a `DynamicAmount`) for totals computed when the ability resolves/goes on the stack —
  Ureni, the Song Unending: `dynamicTotal = DynamicAmounts.landsYouControl()`. Works for creatures and
  planeswalkers (`GameObjectFilter.CreatureOrPlaneswalker`); zero chosen targets ⇒ no-op.
- `DamageCantBePreventedThisTurn()` — "Damage can't be prevented this turn." Turn-scoped one-shot that
  sets a `GameState` flag (cleared at the next turn boundary), shutting off all damage prevention for
  the rest of the turn — prevention shields, prevention/replacement-of-damage effects, and protection's
  prevention clause are ignored (CR 615.6). The static, permanent-hosted equivalent is the
  `DamageCantBePrevented` replacement effect (Sunspine Lynx); use this effect when a spell/ability needs
  the shutoff without a permanent on the battlefield (Fear, Fire, Foes!).

### Life

- `GainLife(amount, target?)` — target gains life (default: controller).
- `PayDynamicLife(amount: DynamicAmount, payer?)` — pay life equal to a `DynamicAmount` (e.g.
  "pay life equal to its power" via `EntityProperty(Triggering, Power)`), evaluated at resolution.
  The dynamic, payer-parametric twin of the fixed `PayLifeEffect`; use it as the `cost` of an
  `OptionalCostEffect` (`Gate.MayPay`) so the same amount can also feed the `ifPaid` effect. A
  non-positive evaluated amount pays nothing and still counts as paid (CR 119.4).
- `LoseLife(amount, target)` — target loses life.
- `DrainLife(amount, from = EachOpponent, to = Controller)` — each player in `from` loses `amount`
  life, then `to` gains life equal to the total *actually* lost (each loss honors `ModifyLifeLoss`
  replacements) as a single life-gain event — "Each opponent loses X life. You gain life equal to
  the life lost this way." (Exsanguinate). Prefer this over `LoseLife + GainLife` whenever the gain
  is worded "equal to the life lost this way".
- `SetLifeTotal(amount, target)` — set target's life total to N.
- `ExchangeLifeAndPower(target)` — swap target's power with controller's life total.
- `LoseHalfLife(roundUp, target, lifePlayer?)` — lose half of life total (round up/down).
- `LockLifeGain(target?, duration?)` — "target player can't gain life" for `duration` (default
  `Duration.Permanent` = rest of the game; `EndOfTurn` / `UntilYourNextTurn` also honored). A one-shot
  effect that tags the player with `CantGainLifeComponent`, so the lock is independent of any source —
  unlike the `PreventLifeGain` *replacement* (§11), which ends when its permanent leaves play.
  Non-player targets are a no-op, so it composes after a "deal damage to any target" rider (Screaming
  Nemesis). Checked by `DamageUtils.isLifeGainPrevented`.
- `LoseGame(target, message?)` — target loses the game.
- `RemoveMaximumHandSize(target?)` — "target has no maximum hand size for the rest of the game"
  (default target: controller). One-shot resolution effect that confers a permanent, player-scoped
  property via `PlayerNoMaximumHandSizeComponent` — unlike the battlefield-only `NoMaximumHandSize`
  *static ability* (§9, Reliquary Tower / Thought Vessel), it survives the source leaving any zone
  (e.g. Wisdom of Ages exiles itself on resolution). Idempotent. `CleanupPhaseManager` checks both
  this component and the static ability when discarding to hand size.
- `WinGame(target, message?)` — target wins the game.
- `TakeExtraTurn(target, loseAtEndStep?)` — target takes an extra turn after this one (Time Walk, Lost Isle Calling).
  Set `loseAtEndStep = true` for "...you lose the game at the beginning of that turn's end step" (Last Chance, Final
  Fortune). Prevented by the `PreventExtraTurns` replacement (Ugin's Nexus).
- `EndTheTurn` — end the current turn (CR 720): Ultima ("Destroy all artifacts and creatures. End the turn."),
  Time Stop, Sundial of the Infinite, Discontinuity. When it resolves the whole stack is exiled (including the
  source and any triggered abilities the resolution queued — even ones that can't be countered — so those never
  reach the stack, CR 720.1c), creatures are removed from combat, and the game skips straight to the cleanup step
  (discard to maximum hand size, marked damage wears off, "this turn" / "until end of turn" effects end) before the
  next turn begins. Takes no target — it always ends the active player's turn. Modeled as a two-step effect: the
  executor records an `EndTheTurnRequestedComponent` on the active player, and `PassPriorityHandler` runs the
  sequence via `TurnManager.performEndTheTurn` once the current resolution finishes (so it can exile the *rest* of
  the stack and drop the pending triggers). Compose after a board wipe with
  `Effects.Composite(listOf(Effects.DestroyAll(...), Effects.EndTheTurn))`.
- `ForceExileMultiZone(count, target)` — exile from hand/battlefield/graveyard combined (Lich's Mastery shape).

### Cards (draw / discard)

- `DrawCards(count, target?)` — draw N (default: controller).
- `DrawUpTo(max, target)` — draw up to N (player picks 0–N).
- "Draw a card and reveal it; if it isn't a [type], discard it" (Sindbad) is a pipeline composition, not
  an effect type: `GatherCards(TopOfLibrary(1), "toDraw")` → `DrawCards(1)` →
  `FilterCollection("toDraw", InZone(HAND), "drawn")` (skips the branch when the draw was replaced or the
  library was empty) → `RevealCollection("drawn", revealToSelf = false)` →
  `FilterCollection(MatchesFilter(Land), storeNonMatching = "notLand")` →
  `MoveCollection("notLand" → graveyard, moveType = Discard)`.
- `Discard(count, target)` — controller-of-target chooses; mandatory. `count` is an `Int` or a
  `DynamicAmount` ("discard X cards, where X is …" — e.g. Converge's Arcane Omens with
  `DynamicAmounts.colorsOfManaSpent()`).
- `EachOpponentDiscards(count)` — each opponent discards N.
- `EachPlayerReturnPermanentToHand()` — each player bounces a permanent.
- `EachPlayerDrawsForDamageDealtToSource()` — each player draws equal to damage source took this turn.
- `ReadTheRunes()` — draw N, then discard N (or sacrifice permanents).
- `ReplaceNextDraw(effect)` — replaces controller's next draw this turn with the given effect (a one-shot
  floating shield, consumed before the replacement runs so an inner `DrawCards` doesn't re-trigger it). The
  activation-time `{X}` is captured onto the shield, so the replacement effect can read `DynamicAmount.XValue`
  when it fires at draw time (Aladdin's Lamp: "look at the top X cards … then draw a card").

### Destruction & exile

- `Destroy(target, noRegenerate?)` — destroy target (respects indestructible). `noRegenerate = true`
  marks the target so it "can't be regenerated" (composes `CantBeRegeneratedEffect` before the move) —
  the single-target analogue of `DestroyAll(noRegenerate = …)`, for Terror / Smother / Tunnel.
- `RegenerateEffect(target)` (raw — no facade) — drop a regeneration shield on `target`, lasting until end
  of turn. The next time `target` would be destroyed this turn, instead tap it, remove all damage marked on
  it, and remove it from combat. Consumed by the first destruction it intercepts.
- `RemoveDamageShieldEffect(target)` (raw — no facade) — Pyramids' second mode. Same shape as regeneration:
  a one-shot destruction shield lasting until end of turn that replaces "destroyed" with "remove all damage
  marked on it". Differs from regeneration in *not* tapping the target and *not* removing it from combat —
  only the marked damage is cleared. The shield isn't a regeneration ability, so a "can't be regenerated"
  marker on the target doesn't disable it. Consumed by the first destruction it intercepts; expires at end
  of turn.
- `DestroyAll(filter, noRegenerate?, storeDestroyedAs?, excludeTriggering?)` — destroy all matching; optionally
  save the ID list for follow-up. `excludeTriggering = true` spares the triggering entity, for "destroy all
  *other* … with it" triggers (Spreading Plague).
- `DestroyAllAndAttached(filter, noRegenerate?)` — also destroys auras/equipment on the matching permanents.
- `SacrificeAll(filter, excludeTriggering?)` — each matching permanent is *sacrificed by its controller*
  (CR 701.21): emits `PermanentsSacrificedEvent` (sacrifice triggers fire), routes each card to its owner's
  graveyard, and ignores regeneration/indestructibility. The "is sacrificed" sibling of `DestroyAll`.
  `excludeTriggering = true` spares the source ("except for ~", Golgothian Sylex).
- `DestroyLeastPowerCreature(noRegenerate?)` — destroy the creature with the least power among **all**
  creatures on the battlefield (global, both players). On a tie for least power the controller chooses which
  one dies (Drop of Honey). Backed by the `GameObjectFilter.Creature.hasLeastPowerAmongAllCreatures()` filter
  (`StatePredicate.HasLeastPowerAmongAllCreatures`) gathered, then a `ChooseExactly(1)` selection that
  auto-resolves when the minimum is unique.
- `DestroyCreaturesBlockingOrBlockedBySource(noRegenerate?)` — destroy the creatures blocking, or blocked by,
  the effect's source (CR 509 combat pairing), using the pairing **last known when the source left the
  battlefield**. For "when ~ dies, destroy all creatures blocking or blocked by it" (Abu Ja'far): the live
  combat cross-references are already torn down by the time a dies trigger resolves, so the pairing is read
  from the leaves-battlefield snapshot (`ZoneChangeEvent.lastKnownBlockingOrBlockedByIds` →
  `EffectContext.triggerLastKnownBlockingOrBlockedByIds`) via `CardSource.LastKnownCombatPairedWithSource`,
  restricted to creatures still on the battlefield.
- `DestroyAllEquipmentOnTarget(target)` — wreck the gear attached to a creature.
- `Exile(target)` — exile target.
- `ExileAndGrantOwnerPlayPermission(target, until?)` — exile + owner may play it (Garth-style).
- `ExileOpponentsGraveyards()` — exile every card in each opponent's graveyard.
- `ExileUntilLeaves(target)` — linked exile; returns when source leaves the battlefield.
- `ExileWithAurasNotingCounters(target = ContextTarget(0))` / `ReturnNotedExileTappedWithAuras()` — the state-preserving "blink that remembers counters and Auras" pair (Tawnos's Coffin). The exile half exiles the target creature **and all Auras attached to it** (all linked to the source via `LinkedExileComponent`) and records the creature's identity + its `kind→count` counter snapshot on the source via `NotedExileComponent` (captures the Auras *before* exiling the creature, so the unattached-Aura SBA can't pre-empt them). The return half (a no-op when nothing is noted, so it's safe to fire from **both** a `LeavesBattlefield` and a `BecomesUntapped` trigger — whichever fires first returns the cards) returns the noted creature **tapped under its owner's control** with the noted counters restored, then returns the linked Auras attached to it; Auras that can't legally re-attach go to their owners' graveyards via the CR 704.5m unattached-Aura SBA (the "If you don't …" fallback). `NotedExileComponent` is preserved across the source's own zone change (like `LinkedExileComponent`) so the leaves-the-battlefield return still reads it, and stripped on battlefield re-entry (Rule 400.7).
- `ExileLinkedToSource(target)` — exile a target **permanently** and record it in the source's linked-exile pile (`LinkedExileComponent`). Unlike `ExileUntilLeaves` there's no automatic return — the link just lets later abilities reference the exiled card (Territory Forge's "this permanent has all activated abilities of the exiled card").
- `RecordChosenLinkedExile(from)` — stamp the source's `ChosenLinkedExileComponent` with the first card in the pipeline collection `from` (its "last chosen card"). Pair after a `SelectFromCollection` over `CardSource.FromLinkedExile()` so a `HasAbilitiesOfChosenLinkedExiledCard` static ability grants the source that card's activated and triggered abilities (Koh, the Face Stealer's "Pay 1 life: Choose a creature card exiled with Koh").
- `ExileGroupAndLink(filter, storeAs?)` — exile all matching permanents into source's linked exile pile.
- `ExileFromTopRepeating(count, repeatCondition)` — keep exiling top cards while a condition holds.
- `ExileLibraryUntilManaValue(manaValue)` — exile from library until mana value ≤ N.

### Return / placement

- `ReturnToHand(target)` — bounce to hand.
- `PutOnTopOfLibrary(target)` — place target on top of its owner's library.
- `PutOnBottomOfLibrary(target)` — place target on the bottom of its owner's library (forced, no choice).
- `PutOnTopOrBottomOfLibrary(target)` — player chooses top or bottom.
- `PutSecondFromTopOrBottomOfLibrary(target)` — second-from-top or bottom.
- `ShuffleIntoLibrary(target)` — shuffle target into owner's library.
- `PutIntoLibraryNthFromTop(target, positionFromTop)` — place N from the top.
- `PutOntoBattlefield(target, tapped?)` — put target on the battlefield.
- `PutOntoBattlefieldUnderYourControl(target)` — under controller's control.
- `PutOntoBattlefieldFaceDown(count, target?)` — enter face-down (2/2 morph shape).
- `RevealFaceDownPermanent(target?)` — reveal a face-down permanent (make its hidden card public,
  CR 708.2). Informational only — does **not** turn it face up. Pair with
  `Conditions.TargetIsCreatureCard` + `TurnFaceUpEffect` for "Reveal target face-down permanent. If
  it's a creature card, you may turn it face up." (Hauntwoods Shrieker).
- `PutOntoBattlefieldAttachedToChosen(target, hostFilter?)` — put a targeted Aura or Equipment onto the
  battlefield attached to a permanent the controller chooses at resolution (default host filter: a creature
  you control). Works for both Auras and Equipment; the host is chosen, not targeted. If no legal host exists,
  an Equipment enters unattached while an Aura can't enter (Rule 303.4g). (One Last Job.)
- `ReturnSelfToBattlefieldAttached(target)` — return source attached to target (Aura recursion).
- `ReturnSelfFromExileTransformed` — Craft resolution (CR 702.167a). Returns the source from exile to the
  battlefield as its back face, under its owner's control, and re-attaches the source's
  `CraftedFromExiledComponent` recording the exiled materials. Pair with `AbilityCost.Craft`; see the `Craft`
  keyword helper in the keyword catalog.
- `ExileAndReturnTransformed(target = Self, returnAs = ReturnFace.TRANSFORMED)` — "Exile [this], then return it
  to the battlefield transformed under its owner's control" (FIN Dominant / eikon transform). Exiles a
  double-faced permanent and re-enters it as a **new object** on the chosen face — unlike `Transform`, which
  flips a permanent in place. Because it is a new object: counters/damage drop, attachments fall off, leaves-
  and enters-the-battlefield triggers fire (not transform triggers), and a Saga face re-enters with one lore
  counter (CR 714.2b). The exile and return are atomic (no priority/SBAs between). `returnAs`: `TRANSFORMED`
  (the opposite face — front→back), `FRONT` ("return it front face up" — the eikon Saga's final chapter flips
  back to the legend), or `BACK`. The front face's activated ability is sorcery-speed
  (`timing = TimingRule.SorcerySpeed`); Jecht uses it from a "may" combat-damage trigger instead.
- `ReturnSelfFromGraveyardTransformed()` — "Return this card from your graveyard to the battlefield
  transformed" (Garland, Knight of Cornelia). Returns the *source card* from the graveyard to the
  battlefield with its back face up; pair with `activateFromZone = Zone.GRAVEYARD` on the owning
  activated ability. No transform triggers fire (the card was never turned over on the battlefield);
  the back face's ETB triggers fire normally. No-ops if the source left the graveyard before
  resolution, or if it isn't a double-faced card (a single-faced card instructed to enter transformed
  doesn't move at all). Raw type `ReturnSelfFromZoneTransformedEffect(fromZone)` generalizes to other
  source zones.
- `ReturnCreaturesPutInGraveyardThisTurn(player)` — Patriarch's Bidding shape.
- `ReturnSameNamedFromGraveyard(target = ContextTarget(0))` — return the target graveyard card and
  **every other card with the same name** in the controller's graveyard to the battlefield **tapped**
  under the controller's control (each moved through `ZoneTransitionService`, so enters-with-counters
  still apply). Rat King, Verminister: "Return target creature card and all other cards with the same
  name as that card from your graveyard to the battlefield tapped."

### Hand reveal

- `Effects.MayRevealCardFromHand(filter, otherwise?)` — atomic "you may reveal a `filter`
  card from your hand" choice. Computes eligible hand cards; if none, runs `otherwise`
  silently; otherwise prompts the controller with a `SelectCardsDecision` (min=0, max=1).
  Revealing emits a `CardsRevealedEvent` and stops; declining (or empty selection) runs
  `otherwise`. Compose with `Effects.Tap`/`Effects.Sacrifice`/etc. via `otherwise` to
  express "if you don't, X" riders — e.g. SOI shadow lands wrap this in
  `OnEnterRunEffect(...)` with `otherwise = Effects.Tap(EffectTarget.Self)` for the
  "this land enters tapped" branch.
- `Effects.Behold(filter, ifBeheld?)` — resolution-time **behold** (`BeholdEffect`): "you may
  behold a `filter`. If you do, `ifBeheld`." The behold itself is optional — the controller may
  choose a matching permanent they control **or** reveal a matching card from hand (revealing emits
  `CardsRevealedEvent`; battlefield permanents are merely chosen). If they decline, or control no
  matching permanent and hold no matching card, `ifBeheld` does not run. Distinct from the cast-time
  `AdditionalCost.Behold` / `AdditionalCost.BeholdOrPay` (which behold as a casting cost). Sarkhan,
  Dragon Ascendant ETB: `Effects.Behold(GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
  ifBeheld = Effects.CreateTreasure())`.

### Library reveal & free cast

- `Effects.Cascade` — CR 702.85a (`CascadeEffect`). Exile from the top of the controller's library
  until a nonland card with mana value **strictly less than** the triggering spell's is exiled,
  offer to cast it for free, bottom-randomize every exiled card that isn't cast.
- `Effects.Discover(amount, storeDiscoveredAs?, thenEffect?)` — Discover N, CR 701.57 (`DiscoverEffect`).
  Exile from the top of the controller's library until a nonland card with mana value **≤ N** is exiled
  (the "discovered card"), then present a two-option prompt: **cast it for free** or **put it into your
  hand** (if the cast can't initiate, it falls back to hand); bottom-randomize the rest. `amount` is an
  `Int` (fixed, "Discover 4/5/10") or a `DynamicAmount` ("Discover X, where X is that spell's mana value"
  — **Hurl into History**, pass `EntityProperty(Target(0), ManaValue)`). Differs from `Cascade` on three
  axes: explicit threshold (not the triggering spell's MV), ≤ vs strict <, and the non-cast branch keeps
  the card (hand) rather than bottoming it — hence a distinct primitive. Set `storeDiscoveredAs` to publish
  the discovered card's id to a pipeline collection and `thenEffect` to resolve a follow-up **only when a
  card was discovered** (CR 701.57c); the follow-up runs after the cast/hand step and can read the
  discovered card — e.g. **Hit the Mother Lode** (`Effects.Discover(10, storeDiscoveredAs = "discovered",
  thenEffect = Effects.CreateTreasure(count = IfPositive(Subtract(Fixed(10), StoredCardManaValue("discovered"))), tapped = true))`).
- `RevealAndMayCastFromLibraryEffect(count, maxManaValue, player?)` — Sunbird's Invocation
  shape. Reveal top `count` cards of `player`'s library, present a `SELECT_CARDS` prompt over
  the revealed nonland cards with mana value ≤ `maxManaValue` (player picks 0 or 1), free-cast
  the chosen card if any, bottom-randomize the rest. Pair with `DynamicAmounts.triggeringManaValue()`
  (= `EntityProperty(Triggering, ManaValue)`) when both bounds come from the triggering spell.

### Linked exile & play-from-exile permissions

- `ReturnLinkedExile()` — return all from source's linked exile, under controller.
- `ReturnLinkedExileUnderOwnersControl()` — return under each card's owner.
- `ReturnLinkedExileToHand()` — return all from linked exile to hand.
- `ReturnOneFromLinkedExile()` — return one chosen card.
- `GrantMayPlayFromExile(from, expiry?, withAnyManaType?, asThoughFlash?, condition?, landEntersTapped?, onPlayRider?, ownerControls?, exileAfterResolve?, fixedAlternativeManaCost?, fixedAlternativeCostIsManaValue?, waterbend?)` — controller may play matching cards from exile. `asThoughFlash=true` lets the granted cards be cast at **instant speed** — "as though they had flash" (CR 702.8) — even when they are sorceries/creatures; the timing rider rides on the `MayPlayPermission` and is honored by both `CastFromZoneEnumerator` (offered actions) and `CastSpellHandler` (authoritative timing check), and waives no cost. Combine with `condition = IsYourTurn` + `withAnyManaType = true` + `expiry = MayPlayExpiry.Permanent` for "During your turn, you may cast cards exiled with this … as though they had flash. Mana of any type can be spent to cast those spells." (**Azula, Cunning Usurper**, whose ETB exiles the two chosen cards *with* it via `MoveCollection(linkToSource = true)`, then grants over `CardSource.FromLinkedExile`). `fixedAlternativeManaCost` (a `ManaCost`, e.g. `{2}`) makes each granted card castable for that *fixed* cost **instead of** its printed mana cost while exiled — it *replaces* the cost, unlike `GrantPlayWithCostIncrease` which adds on top. Stamps `PlayWithFixedAlternativeManaCostComponent(controllerId, fixedCost, waterbend)`, honored by `CastFromZoneEnumerator` + `CastSpellHandler` and stripped on leaving exile by `StackResolver`. `fixedAlternativeCostIsManaValue = true` computes that fixed cost **per card** as `{its mana value}` generic at grant time (a 6-drop → `{6}`, mutually exclusive with a literal `fixedAlternativeManaCost`), and `waterbend = true` marks it a **waterbend** cost (CR 701.67): its whole generic may be paid by tapping untapped artifacts/creatures (each `{1}`) in addition to mana — `CastSpellHandler` reduces the fixed cost by the tapped `AlternativePaymentChoice.waterbendPermanents` (cap = the fixed cost's generic) in both validation and payment, and `CastFromZoneEnumerator` surfaces `hasWaterbend`/`waterbendPermanents` + folds the tap help into affordability. Reached through the `Effects.WaterbendCastFromExile(from, condition?)` facade — backs **Hama, the Bloodbender** ("you may cast the exiled card during your turn by waterbending {X} … where X is its mana value"), whose grant is gated by `AllConditions(IsYourTurn, YouControlSource)` so the exiled card is castable only on your turn and only while you control the granting source (once it leaves the battlefield `YouControlSource` fails and the grant ends). Backs the **Airbend** keyword (`Effects.Airbend`); pair with `ownerControls = true` for "its owner may cast it for {2}". `exileAfterResolve=true` stamps `ExileAfterResolveComponent` on each granted card so a spell cast from the permission is exiled instead of going to a graveyard (on resolution, when countered, or when it fizzles) — the "If that spell would be put into a graveyard, exile it instead" rider on borrow-a-spell-you-don't-own cards (Nita, Forum Conciliator); the same mechanism as `GrantFreeCastTargetFromExile.exileAfterResolve` but for a *paid* cast. `withAnyManaType=true` relaxes the colored pips so mana of any type can pay them (Laughing Jasper Flint, Cruelclaw's Heist); the grant works whether the card stays in exile *or* in a graveyard (Tinybones, the Pickpocket grants over a card the trigger gathered straight from a graveyard via `CardSource.ChosenTargets`), and the relaxation is applied both in the legal-action enumerator and in the cast handler's payment. `landEntersTapped=true` forces a played land tapped regardless of its own ETB script (Lightstall Inquisitor); PlayLandHandler reads the flag off the active `MayPlayPermission` at play time and stamps `TappedComponent` before the card's intrinsic `EntersTapped` branch runs. `onPlayRider` is a "When you play a card this way, …" payoff: the engine registers a linked event-based delayed triggered ability alongside the permission, and casting/playing a granted card emits a `CardPlayedFromPermissionEvent` (link-id-scoped, like `DamagePreventedEvent`) that fires the rider on the stack as a triggered ability of the granting source. Expires with the grant (end of turn). Used by Fires of Mount Doom ("…When you play a card this way, Fires of Mount Doom deals 2 damage to each player."). `ownerControls=true` grants the permission to each exiled card's *owner* instead of the effect controller — the collection is grouped by owner into one permission per owner, and any turn-keyed `expiry` (e.g. `MayPlayExpiry.UntilEndOfNextTurn`) is measured against each owner's own turns. Use for "for each of those cards, its owner may play it until the end of their next turn" wording where the exiled cards may belong to different players (Suspend Aggression: exile a target nonland permanent + your top library card, each owner may replay the one they own). Mirrors `MakePlottedEffect.ownerControls`; prefer it (composed in a gather → exile → grant pipeline) over the monolithic `ExileAndGrantOwnerPlayPermission` when the expiry is turn-bounded or more than one card is exiled.
- `GrantPlayWithoutPayingCost(from)` — same, without paying mana costs.
- `GrantPlayWithCostIncrease(from, amount)` — stamp `PlayWithCostIncreaseComponent(controllerId, amount)` on every card in the collection, so the next cast pays `{amount}` extra generic. Pair with `GrantMayPlayFromExile` for "each spell cast this way costs {N} more" clauses (Lightstall Inquisitor); for target-based "exile this permanent, owner may play it, opponents tax" effects use `Effects.ExileAndGrantOwnerPlayPermission` instead.
- `GrantFreeCastTargetFromExile(target)` — cast specific exiled card for free.
- `MakePlotted(from, ownerControls = false)` — make every card in the named collection *plotted* (CR 718). The cards must already be in exile (chain after a `MoveCollection` to `Zone.EXILE`). Each card gets the plotted designation (`PlottedComponent`) + a permanent free-cast-as-a-sorcery-on-a-later-turn permission (`PlayWithoutPayingCostComponent` + `MayPlayPermission` gated by `SourcePlottedOnPriorTurn`) — the Plot keyword's state without a plot cost. Emits a `CardPlottedEvent` per card so "when this card becomes plotted" triggers fire. No-ops on an empty collection (so an optional "you may exile … it becomes plotted" fork is safe). With `ownerControls = true` the free-cast permission goes to each card's **owner** rather than the effect's controller (CR 718.2 — for "exile target spell, it becomes plotted" the spell's owner casts it later, not the plotter); default is controller-controls (you plot a card you own, like Make Your Own Luck).

### Stats & keywords

- `ModifyStats(power, toughness, target?, duration?)` — `±P/±T` for `duration` (default: until end of
  turn). Pass `Duration.WhileSourceTapped("…")` for the Antiquities "tap-locked" buffs (Ashnod's Battle
  Gear `+2/-2`, Tawnos's Weaponry `+1/+1`): the bonus persists for as long as the source artifact remains
  tapped and the one-way latch drops it when it untaps.
- `SetBasePower(target, power: DynamicAmount, duration)` — set base power to a dynamic value (Layer 7b),
  leaving toughness unchanged.
- `SetBaseToughness(target, toughness: DynamicAmount, duration)` — toughness-only sibling of `SetBasePower`.
- `SetBasePowerAndToughness(power, toughness, target?, duration)` — set base power AND toughness (Layer 7b,
  set values), e.g. "Target creature has base power and toughness 5/5 until end of turn" (Dreadful as the
  Storm). Two overloads: fixed `Int`s or `DynamicAmount`s.
  - All three facades lower onto the one **`SetBaseStatsEffect(target, power: DynamicAmount?, toughness:
    DynamicAmount?, duration)`** atom — `null` power or toughness leaves that stat unchanged, so the same
    type covers power-only, toughness-only, and both. (Distinct from `ModifyStatsEffect`, a +N/+N
    *modifier* in layer 7c, and from the `SetBasePowerToughness*Static` CDAs, which apply for as long as a
    static ability is active rather than as a one-shot floating effect.)
- `GrantKeyword(keyword, target, duration)` — grant a keyword for a duration. The target may be a battlefield permanent **or a permanent spell still on the stack**: a permanent spell keeps its entity id as it resolves, so a keyword granted to `EffectTarget.TriggeringEntity` inside a "when you next cast a creature spell this turn" delayed trigger carries onto the creature the moment it enters (Summon: Brynhildr's Gestalt Mode = "it gains haste until end of turn"). On a non-permanent spell the floating effect simply never has a permanent to apply to.
- `GrantStaticAbility(ability, target, duration)` — grant a printed-shape `StaticAbility` (e.g. `CantBeBlockedByMoreThan(1)`) to a permanent for a duration. The runtime sibling of a printed static ability: unlike keyword grants (which flow through projected keywords) it is recorded as a `GrantedStaticAbility` keyed to the entity in `GameState.grantedStaticAbilities` and read **at the point of use** — combat blocker validation (`BlockPhaseManager`, CR 509.1b) consults granted `CantBeBlockedByMoreThan` alongside the creature's printed static abilities; the grant expires in the cleanup step (EndOfTurn). Compose inside `ForEachInGroup` with `EffectTarget.Self` for "each creature you control gains ..." (Full Steam Ahead = `ModifyStats(2,2)` + `GrantKeyword(TRAMPLE)` + `GrantStaticAbility(CantBeBlockedByMoreThan(1))`). `CantBeBlockedByMoreThan` (combat), `MayCastFromGraveyard` (graveyard-cast enumerator + `CastZoneResolver`, e.g. Forgotten Cellar's "cast spells from your graveyard this turn"), and `PreventActivatedAbilities` (activation legality: `CastPermissionUtils.isActivationPrevented`, consulted by the ability handler and both ability enumerators) are wired into read sites today; granting another `StaticAbility` kind compiles and stores but needs its own point-of-use read to take effect. A granted `PreventActivatedAbilities` behaves exactly like the printed form anchored to the grant's holder: its filter is evaluated with the holder as source, so the self-scoped `PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself())` locks the *holder's own* activated abilities — mana abilities included unless `nonManaAbilitiesOnly = true`. Pair it with `Duration.WhileAffectedTapped` ("for as long as it remains tapped", keyed to the granted-to permanent) for the Braided Net shape — "Tap another target nonland permanent. Its activated abilities can't be activated for as long as it remains tapped." = `Effects.Tap(target)` + `Effects.GrantStaticAbility(PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()), target, Duration.WhileAffectedTapped)`. Like every "for as long as …" duration it is one-way (CR 611.2b): the read site gates per-frame, and `EndedDurationExpiryCheck` physically removes the grant the moment the permanent untaps (or leaves the battlefield), so a later re-tap does not re-lock it.
- `GrantReplacementEffect(replacement, target, duration)` — grant a printed-shape `ReplacementEffect` (e.g. `RedirectZoneChange`) to a permanent for a duration. The runtime sibling of a printed replacement effect, modelled exactly like `GrantStaticAbility`: recorded as a `GrantedReplacementEffect` (carrying the granting `controllerId`) in `GameState.grantedReplacementEffects` and read **at the point of use** — the zone-change redirect path (`ZoneMovementUtils.checkZoneChangeRedirect`) consults granted `RedirectZoneChange` alongside permanents' printed replacement effects; the grant expires in the cleanup step (EndOfTurn). Used for durational "this turn" riders such as Forgotten Cellar's "if a card would be put into your graveyard from anywhere this turn, exile it instead" (`GrantReplacementEffect(RedirectZoneChange(newDestination = Zone.EXILE, appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter(controllerPredicate = ControllerPredicate.OwnedByYou), to = Zone.GRAVEYARD)))`). Only `RedirectZoneChange` is wired into a read site today; granting another `ReplacementEffect` kind compiles and stores but needs its own point-of-use read to take effect. Note: `MayCastFromGraveyard` granted via `GrantStaticAbility` is now also read at the graveyard-cast enumerator/`CastZoneResolver`, so "you may cast spells from your graveyard this turn" is `GrantStaticAbility(MayCastFromGraveyard(filter), EffectTarget.Self, Duration.EndOfTurn)`.
- `GrantHarmonize(target, cost?, duration)` — grant **Harmonize** (CR 702.180) to a target instant/sorcery card in a graveyard. `cost` defaults to `null` = "equal to the card's mana cost" (Songcrafter Mage); pass a `ManaCost` for a fixed harmonize cost. Records a runtime `GrantedKeywordAbility` keyed to the card entity; the cast-from-graveyard enumerator, the cast handler, the alternative-payment handler (tap-for-power reduction), and the stack resolver (exile on resolution) all read printed-or-granted harmonize through the shared `HarmonizeGrants` resolver, so a granted harmonize behaves identically to a printed one. The grant expires in the cleanup step (EndOfTurn) and surfaces a "Granted Ability" badge on the card.
- `GrantFlashback(target, cost?, duration)` — grant **Flashback** (CR 702.34) to a target instant/sorcery card in a graveyard. The runtime sibling of printed `KeywordAbility.Flashback`, modelled exactly like `GrantHarmonize`. `cost` defaults to `null` = "equal to the card's mana cost" (Archmage's Newt); pass a `ManaCost` for a fixed flashback cost (e.g. `{0}` on a saddled-Mount branch). Records a runtime `GrantedKeywordAbility` keyed to the card entity; the cast-from-graveyard enumerator, the cast handler / `CastZoneResolver`, and the stack resolver (exile on resolution) read printed-**or**-granted flashback through the shared `FlashbackGrants.effectiveFlashback` resolver, so a granted flashback is castable and exiled exactly like a printed one. The grant survives the graveyard → stack move and expires in the cleanup step (EndOfTurn). Pair with `ConditionalEffect(Conditions.SourceIsSaddled, GrantFlashback(t, {0}), elseEffect = GrantFlashback(t))` for the saddled-or-not cost swap.
- `RemoveKeyword(keyword, target, duration)` — strip a keyword.
- `RemoveAllAbilities(target, duration)` — wipe all abilities (including granted keywords).
- `LoseAllCreatureTypes(target, duration)` — remove all creature subtypes.
- `SetCreatureSubtypes(subtypes, target, duration)` — replace subtypes outright.
- `AddCreatureType(subtype, target, duration)` — additive subtype.
- `AddColor(color | colors, target, duration?)` — add color(s) in addition to existing ones
  (Layer 5; default duration Permanent). Ability-applied counterpart of the `GrantColor` static.
  Pair with `AddCreatureType`/`AddCardType` for "becomes a [color] [type] in addition to its other
  colors and types" (Possessed Goat).
- `GrantHexproof(target, duration)` / `GrantShroud(target, duration)` — temporary hexproof / shroud.
  Both are facades lowering onto the player-aware `GrantEvasionKeywordEffect(keyword, target, duration)`:
  for player targets it attaches the matching player protection component; for permanents it grants the
  keyword via a Layer-6 floating effect (like `GrantKeyword`).
- `GrantExileOnLeave(target)` — "if it would leave, exile instead".
- `GrantKeywordToAttackersBlockedBy(keyword, target)` — grant keyword to creatures this blocks.

### Counters

- `AddCounters(type, count, target)` — add N counters of `type`.
- `AddDynamicCounters(type, amount, target)` — count is computed at resolution.
- `AddCountersUpTo(type, max, target)` — the effect's controller **chooses** how many (0 up to `max`, a
  `DynamicAmount` so "up to X" works) counters of `type` to put on the target, via one `ChooseNumberDecision`
  at resolution. The additive, single-kind mirror of `RemoveAnyNumberOfCounters` / `RemoveCountersUpTo`;
  placement goes through the normal `AddCounters` chokepoint, so counter-placement replacements (Hardened
  Scales) and downstream triggers (Saga chapter abilities off lore counters) fire. No-op when the target
  can't receive counters or `max` ≤ 0; choosing 0 places none. Esper Terra's "if it's a Saga, put up to three
  lore counters on it" = `ConditionalEffect(CollectionContainsMatch(CREATED_TOKENS, Enchantment.withSubtype(SAGA)),
  AddCountersUpTo(Counters.LORE, 3, PipelineTarget(CREATED_TOKENS)))`.
- **Stat counters and the layer system.** Counters whose `type` is a P/T stat counter modify power/toughness in
  layer 7c (CR 613.4c) via `EffectApplicator.applyCounters`. The symmetric pair is `Counters.PLUS_ONE_PLUS_ONE` /
  `Counters.MINUS_ONE_MINUS_ONE`; the asymmetric counters `Counters.PLUS_ONE_PLUS_ZERO` (`+1/+0`),
  `Counters.PLUS_ZERO_PLUS_ONE` (`+0/+1`), `Counters.MINUS_ONE_MINUS_ZERO` (`-1/-0`) and
  `Counters.MINUS_ZERO_MINUS_ONE` (`-0/-1`) modify only the indicated stat (Clockwork Avian's four `+1/+0`
  counters). Each has a matching `CounterTypeFilter` (`PlusOnePlusZero`, etc.) for `EntersWithCounters`,
  counter-count dynamic amounts, and counter triggers. Only `+1/+1` and `-1/-1` annihilate each other as a
  state-based action (CR 122.3); the asymmetric counters never cancel.
- `DoubleCounters(type?, target?)` — one-shot doubling of the `type` counters (default `+1/+1`) already on the
  target: reads the current count and places that many more (so the total doubles). Distinct from the
  `DoubleCounterPlacement` replacement (which doubles *future* placements); the added counters still trigger
  placement replacements like Hardened Scales. No-op with zero counters. Sage of the Fang.
- `GrantCounterPlacementModifier(modifier?, duration?, counterType?, recipient?)` — install a **temporary,
  duration-scoped, controller-scoped** counter-placement modifier: the activated/spell-granted analogue of the
  static `ModifyCounterPlacement` replacement (Hardened Scales). While active, if the *controller* of the effect
  would put `counterType` counters (default `+1/+1`) on a recipient matching `recipient` (default
  `RecipientFilter.CreatureYouControl`, resolved relative to that controller), `modifier` additional counters
  (default `+1`) are placed instead. Recorded in a turn-scoped store on the game state and consulted from the
  single counter-placement chokepoint (so every AddCounters-style effect honors it); expires per `duration`
  (default `Duration.EndOfTurn`) via end-of-turn cleanup. Negative `modifier` reduces (floored at 0). Prairie Dog
  (OTJ): "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature you control, put
  that many plus one +1/+1 counters on it instead." → `GrantCounterPlacementModifier()` with all defaults.
- `RemoveCounters(type, count, target)` — remove N counters.
- `RemoveAnyNumberOfCounters(target)` — player removes 0 or more (one prompt per counter kind, no total cap).
- `RemoveCountersUpTo(maxCount, target)` — player removes **up to `maxCount` counters total across all
  kinds**. The budget-capped form of `RemoveAnyNumberOfCounters` — the *same* `RemoveAnyNumberOfCountersEffect`
  with `maxTotal` set, not a separate effect: one `ChooseNumber` prompt per kind, each capped at
  `min(kind's count, remaining budget)`; prompting stops once the budget is spent. Used by Heartless Act's
  "Remove up to three counters from target creature."
- `ConvertCountersToTokensEffect(counterType = +1/+1, tokenFactory)` — "remove any number of `counterType`
  counters from this permanent; for each removed, create one token." Prompts for `0..(count on source)`,
  removes that many, then mints exactly that many tokens from `tokenFactory` (its own `count` is ignored).
  Set `tokenFactory.stampCreator = true` to make the minted tokens recognizable later. The
  counters→tokens half of Tetravus; the reverse (exile any number of those tokens, add that many counters
  back) **composes** from a pipeline — `gather(CardSource.BattlefieldMatching(filter = ….createdBySource(),
  player = You))` → `chooseAnyNumber` → `exile` → `run(AddDynamicCounters("+1/+1",
  VariableReference("<slot>_count"), Self))` — so no dedicated token→counters effect exists.
- `RemoveAllCounters(target)` — wipe every counter.
- `RemoveAllCountersOfType(type, target)` — wipe one kind.
- `MoveAllLastKnownCounters(target)` — Hooded Hydra / Essence Channeler — move every counter kind from source's
  last-known state. Reads the dies/leaves trigger map (`triggerLastKnownCounters`) first, falling back to the
  cost-sacrifice map (`lastKnownSourceCounters`) so it also works from an activated ability whose source was
  sacrificed/exiled as a cost (Zack Fair).
- `MoveCountersEachKindMissing(source, destination)` — Goldberry, River-Daughter (ability A) — for each counter
  kind on `source` that `destination` does not already have, move one of that kind from `source` onto
  `destination`. Deterministic, no player choice; kinds the destination already has are left untouched.
- `MoveChosenCountersToTarget(source, destination, drawCardOnMove?)` — Goldberry, River-Daughter (ability B) —
  player chooses how many of each kind to move from `source` onto `destination` (one `ChooseNumberDecision` per
  kind). When `drawCardOnMove` is true, the controller draws a card if any counter was moved ("if you do, draw").
- `MoveCounters(counterType, amount, source, destination)` — Tester of the Tangential — deterministic,
  count-carrying move of a single counter kind: moves `amount` (a `DynamicAmount`, e.g. `DynamicAmount.XValue`
  from a may-pay-{X} reflexive) of `counterType` from `source` onto `destination`. The count is capped at the
  number actually on `source`, and adding to `destination` honors counter-placement replacement effects
  (Hardened Scales). No-op when source/destination missing, they're the same permanent, amount ≤ 0, or source has
  none of that kind. The count-fixed counterpart to the interactive `MoveChosenCountersToTarget`.
- `Counters.ANY` — wildcard counter-type string for "counters of any type" triggers/events (e.g.
  `Triggers.countersPlacedOn`); not a real placeable counter, only a matcher sentinel.
- `DistributeCountersFromSelf(type?, count?)` — split source's counters among creatures you control.
- `DistributeCountersAmongTargets(total, type?, minPerTarget?)` — divvy N counters among chosen targets.
- `DistributeCountersAmongFiltered(total, type?, filter, minPerTarget?)` — distribute N **new** counters among permanents matching `filter`, chosen at resolution (not the spell's targets); `minPerTarget = 0` models "among any number of". Unlike `DistributeCountersFromSelf` nothing is removed from a source. Crashing Wave: `DistributeCountersAmongFiltered(3, Counters.STUN, Filters.Creature.tapped().opponentControls())` — "distribute three stun counters among any number of tapped creatures your opponents control."
- `Proliferate()` — add one counter of each kind already present on chosen permanents/players (CR 701.27).
- `AddCountersToCollection(name, type, count)` — add counters to cards held in a pipeline collection.
  An overload takes a `DynamicAmount` instead of an `Int` count, evaluated once at resolution — "create
  a token, then put X +1/+1 counters on it, where X is …" over the `CREATED_TOKENS` collection (Emil,
  Vastlands Roamer).

### Color & type

- `AddCardType(type, target, duration)` — add a card type (e.g. become an artifact).
- `AddSubtype(subtype, target, duration)` — add a subtype temporarily.
- `SetLandType(landType, target, duration, fromChosenValueKey)` — target land *becomes* the basic land type, **replacing** its existing land subtypes (Rule 305.7); pass `fromChosenValueKey` to read the type from a preceding `ChooseOption(OptionType.BASIC_LAND_TYPE)`. One-shot counterpart to the `SetEnchantedLandType` aura static ability. (Dream Thrush)
- `ChooseColorForTarget(target)` — target picks a color; stored in context.
- `BecomeChosenManaColor(target)` — adopt the previously chosen color.
- `ChangeColor(colors, target, duration)` — replace colors with the given set.
- `BecomeAllColors(target, duration)` — five-color until end of turn.
- `ChangeColorToChosen(target, duration)` — replace the target's colors with the single color picked
  by a preceding `ChooseColorThen` (read from `EffectContext.chosenColor`). The target may be a
  **spell on the stack** or a permanent — the color projection reads the recolored entry in both
  zones, so a recolored spell's new color drives color-matching checks (e.g. protection) during
  resolution. Compose as `ChooseColorThen(then = ChangeColorToChosen(target))` for "target ...
  becomes the color of your choice" (Blind Seer).
- `ChangeWordInText(target, duration)` — Layer-3 text change: the player picks one **color word**
  or **basic land type** on the target and a replacement of the same category, recorded as a
  `TextReplacement` on the target. A basic-land-type swap flows through the projected type line, so
  the land's mana (via `IntrinsicManaAbilities`), landwalk relevance, and type checks all follow
  automatically (Forest→Island taps for `{U}`); a color-word swap rewrites protection-from-color and
  `HasColor`/`NotColor` filters. `duration = EndOfTurn` is stripped at cleanup; `Permanent` is the
  Artificial-Evolution-style indefinite change. The player picks the FROM and TO words on **one
  screen** (a `ChooseReplacementDecision`), with words **present on the target** surfaced first
  (labeled "On <card>") so a no-op pick is discouraged, and a live `from → to` preview. (Crystal Spray)

### Mana

- `AddMana(color, amount, restriction?, expiry?)` — add N of one color. `expiry` is a `ManaExpiry`
  (default `END_OF_TURN`); set `END_OF_COMBAT` for firebending-style combat-duration mana that the
  pool keeps through combat and discards when combat ends. Combat-duration mana is stored as an
  `AnySpend` restricted entry (so it spends like any other mana) and cleared by
  `CombatManager.endCombat`. See [ManaExpiry](#manaexpiry).
- `AddColorlessMana(amount, restriction?)` — add colorless.
- `RetainUnspentMana(vararg colors)` — "Until end of turn, you don't lose unspent mana of these colours
  as steps and phases end." The colour-filtered, single-player, turn-scoped one-shot cousin of the
  permanent-static `PreventManaPoolEmptying` (Upwelling, which stops *all* emptying for *everyone*).
  Confers a `RetainUnspentManaComponent` on the resolving controller; at every step/phase-end mana
  emptying (CR 500.5, `CleanupPhaseManager.emptyManaPools` → `ManaPoolComponent.emptyAtBoundary(...)`)
  the kept colours survive while everything else empties, until the marker clears at end-of-turn
  cleanup. The Last Agni Kai (`RetainUnspentMana(Color.RED)`).
- `AddManaOfChoice(colorSet, amount?, restriction?, riders?)` — **unified primitive.** Add N mana of one color the controller picks from a resolved [ManaColorSet](#manacolorset). All "any-color from a constrained pool" cards (any color, commander identity, among permanents, lands could produce, source-chosen color) are expressed as this effect plus a different `ManaColorSet`. `riders` is a `Set<ManaSpellRider>` consumed when the mana pays for a spell (e.g. Path of Ancestry tags its mana with `ScryOnSharedTypeWithCommander`); when riders are set without a `restriction`, the engine stores the entries under `ManaRestriction.AnySpend` to preserve the rider through the pool.
- `AddAnyColorMana(amount?, restriction?)` — sugar for `AddManaOfChoice(ManaColorSet.AnyColor, amount)`. "Add N mana of any **one** color" (Gilded Lotus): one chosen color, N of it. For "any **combination** of colors" use `AddManaInAnyCombination`.
- `AddManaOfChosenColor(amount?)` — sugar for `AddManaOfChoice(ManaColorSet.SourceChosenColor, amount)`.
- `AddManaOfColorAmong(filter)` — sugar for `AddManaOfChoice(ManaColorSet.AmongPermanents(filter))`.
- `AddManaOfColorAmongGraveyard(filter)` — one mana of any color among cards in your graveyard matching
  `filter` (reads each card's base colors; sugar for `ManaColorSet.AmongCardsInGraveyard(filter)`). The
  Grey Havens ("any color among legendary creature cards in your graveyard").
- `AddManaOfColorLandsCouldProduce(scope)` — sugar for `AddManaOfChoice(ManaColorSet.LandsCouldProduce(scope))`. Fellwar Stone / Exotic Orchard / Reflecting Pool shape.
- `AddManaOfColorInCommanderColorIdentity()` — sugar for `AddManaOfChoice(ManaColorSet.CommanderIdentity)`. Arcane Signet / Command Tower shape.
- `AddAnyColorManaSpendOnChosenType(typeName)` — mana that can only pay for a specific card type (kept separate because it derives a runtime [ManaRestriction] from the source's chosen subtype).
- `AddDynamicMana(amount, allowedColors, restriction?)` — split X across a fixed color set, distinct from `AddManaOfChoice` because it distributes the full X total across multiple colors rather than producing X copies of one chosen color.
- `AddManaInAnyCombination(amount, allowedColors?, restriction?)` — "Add N mana in any combination of colors" (Wizard's Rockets, Thornvault Forager, Interdimensional Web Watch). Sugar for `AddDynamicMana`; `allowedColors` defaults to all five. The controller colors **each** pip independently at resolution (3+ colors → pip-by-pip color choice; 2 colors → one "how much of the first" prompt; ≤0 → no mana, no prompt), so the result can mix colors — distinct from `AddAnyColorMana`, where all N share one color.
- `AddOneManaOfEachColorAmong(filter)` — one mana of *each* color found among matching permanents (Bloom Tender shape).
- `AddOneManaOfEachCraftedMaterialColor()` — one mana of *each* printed color among the exiled cards used to craft the source (`AddOneManaOfEachColorAmongEffect(colorSource = ManaColorSource.CraftedMaterials)`; Sunbird Effigy).
- `PayDynamicMana(amount, payer?, color?)` — pay a dynamically-computed amount of mana at resolution; the
  dynamic, payer-parametric twin of the flat `PayManaCostEffect`. `amount` is a [DynamicAmount](#dynamicamount)
  evaluated at resolution (0 pays nothing and succeeds); `payer` is a `Player` reference defaulting to the
  controller (`Player.You`). `color` defaults to null (pay `amount` **generic** mana); set it to a `Color` to pay
  `amount` copies of that **colored** symbol instead — `color = Color.GREEN` → `{G}{G}…`, for "pay {G} for each
  wind counter" (Cyclone). Affordability and the prompt label honor the color. This is the building block for **"pay {N} for each X"** templating — pair it with a
  pipeline selection and read the selection size via `DynamicAmount.Multiply(DynamicAmount.VariableReference("<collection>_count"), N)`
  — and for **"that player pays"** on each-player triggers (`payer = Player.TriggeringPlayer`, the only effect that
  charges a player other than the ability's controller). Affordability is recognized by `Gate.MayPay`, so wrapping
  it in a may-pay gate skips the prompt when the payer can't afford the computed cost, and the gate's "yes" button
  renders the *computed* total ("Pay {8}"), not the formula. When the amount scales with an upstream selection,
  put `SelectionRestriction.MaxAffordablePayment(manaPerSelected, payer)` on the selection so the player can't pick
  a set they can't pay for. Magnetic Mountain ("pay {4} for each tapped blue creature chosen, untap them") composes
  all three: the capped selection, then the dynamic cost in a `Gate.MayPay` whose `decisionMaker` is the same
  `Player.TriggeringPlayer`.

### Tokens & emblems

- `CreateToken(p, t, colors?, creatureTypes, keywords?, count?, controller?, imageUri?, legendary?, tapped?, artifactToken?, enchantmentToken?, staticAbilities?)` — make N creature tokens.
  `artifactToken = true` makes them **artifact** creatures and `enchantmentToken = true` makes them **enchantment**
  creatures (both may be set at once); the extra card type is unioned onto the token's `Creature` type line (e.g.
  Duskmourn's Glimmer cards create a "1/1 white Glimmer enchantment creature token" via `enchantmentToken = true`).
  `count` accepts an `Int` or a `DynamicAmount` (the latter for "create X tokens" wording — e.g. Verdeloth the
  Ancient passes `count = DynamicAmount.XValue` to make X Saprolings when kicked); both count overloads accept
  `tapped` (The Final Days creates a `DynamicAmount.Conditional` number of **tapped** Horror tokens). Publishes the created token
  entity IDs to the `CREATED_TOKENS` pipeline collection, so a sibling effect in a `CompositeEffect` can address
  each token via `EffectTarget.PipelineTarget(CREATED_TOKENS, index)` — e.g. Mardu Monument grants menace and haste
  until end of turn to each of its three freshly-created Warriors with one `GrantKeyword` per token. For a *named*
  token (creature or otherwise) with its own abilities — Treasure, Munitions, Cragflame — add a `CardDefinition`
  to `PredefinedTokens.kt` and expose an `Effects.Create<Name>Token()` facade that wraps
  `CreatePredefinedTokenEffect("<Name>", count)`. The predefined-token registry already supports noncreature type
  lines (e.g. Munitions' `typeLine = "Artifact"`) and embedded triggered abilities. For a count computed at
  resolution rather than a fixed integer, pass `dynamicCount = <DynamicAmount>` instead of `count` — the executor
  evaluates it (coerced to ≥ 0) and creates that many tokens (Lobelia Sackville-Baggins, LTR: "create X Treasure
  tokens, where X is the exiled card's power", via `DynamicAmount.EntityProperty(Target(0), Power)`).
  **Colored** predefined tokens (a token has no mana cost, so its printed color is a color indicator, CR 204):
  set `colorIdentity = "<symbols>"` on the token's `CardDefinition` — both predefined-token executors read
  `colorIdentityOverride ?: colors` for the token's color (a bare `colors` is mana-cost-derived and would be
  colorless). Example: the `Frog` token (`PredefinedTokens.Frog`, a 1/1 green Frog created by Quina, Qu Gourmet).
  For an *inline* token (not a registered `CardDefinition`) that has its own abilities, the facade's
  `staticAbilities?` parameter covers the common static-only case (e.g. a token with "This token can't
  block" — Broodrage Mycoid's `CantBlock(GroupFilter.source())`). For `triggeredAbilities` **and**
  `activatedAbilities`, drop to the raw `CreateTokenEffect` constructor, which exposes all three —
  each list is granted to every created token at resolution (permanent
  duration) via `GameState.granted{Static,Triggered,Activated}Abilities`, so the legal-action
  enumerator and `ActivateAbilityHandler` pick them up like any other granted ability. Example:
  Mourner's Surprise's "1/1 red Mercenary creature token with \"{T}: Target creature you control
  gets +1/+0 until end of turn. Activate only as a sorcery.\"" passes a single
  `ActivatedAbility(cost = AbilityCost.Tap, effect = Effects.ModifyStats(1, 0), targetRequirements =
  listOf(Targets.CreatureYouControl), timing = TimingRule.SorcerySpeed)`. (Remember a token is a
  creature, so a `{T}` ability is summoning-sick the turn the token enters.) The raw constructor also
  exposes `stampCreator: Boolean` — when true each minted token records the creating permanent (the
  effect's source) so later abilities can recognize "tokens created with this permanent" via the
  `StatePredicate.CreatedBySource` filter (`.createdBySource()`). Tetravus uses it to reabsorb only the
  Tetravite tokens it minted; off by default.
- `CreateDynamicToken(dynamicPower, dynamicToughness, colors?, creatureTypes, keywords?, count?, controller?, imageUri?)` —
  tokens whose P/T is computed at resolution (e.g. Pure Reflection's X/X Reflection where X = the cast spell's mana
  value, via `DynamicAmounts.triggeringManaValue()`). `controller` directs who gets the token (e.g.
  `EffectTarget.PlayerRef(Player.TriggeringPlayer)` for "that player creates …"); `imageUri` sets custom token art.
- `CreateTokenOfChosenColorAndType(dynamicPower, dynamicToughness, count?)` — a token whose **color and
  creature type are the ones the source locked into its cast-choice slots** (`ChoiceSlot.COLOR` /
  `ChoiceSlot.CREATURE_TYPE`), read off the source's `CastChoicesComponent` at resolution. Riptide
  Replicator: "create an X/X creature token of the chosen color and type." (Replaces the old one-off
  `CreateChosenTokenEffect`; under the hood it sets `CreateTokenEffect.colorsFromChoice` /
  `creatureTypesFromChoice`.)
- `CreateTokenCopyOfSelf(count?, overridePower?, overrideToughness?, removeLegendary?)` — token copies
  of the source. `removeLegendary = true` applies the "except it's not legendary" copy clause (Ran and
  Shaw), mirroring `CreateTokenCopyOfEquippedCreature`.
- `CreateTokenCopyOfTarget(target, count?, overridePower?, overrideToughness?, tapped?, attacking?, triggeredAbilities?, addedKeywords?, addedSupertypes?, removedSupertypes?, overrideColors?, addedColors?, overrideSubtypes?, addedSubtypes?, overrideCardTypes?, activatedAbilities?, addedStaticAbilities?, sacrificeAtStep?, sacrificeOnlyOnControllersTurn?, addCardTypes?, exileAtStep?, exileUnlessSourceIsRingBearer?, controller?)` —
  token copy of another permanent (or a card in any zone — the executor copies the target's `CardComponent`,
  so a graveyard/exile card works; pass `EffectTarget.PipelineTarget("name")` to copy a card a prior pipeline
  step exiled/stored, as Nexus of Becoming and Mardu Siegebreaker do).
  `overrideColors`/`overrideSubtypes` replace the copy's colors/subtypes
  outright for "a token that's a copy … except it's a 5/5 black Demon" wording (Ardyn, the Usurper).
  `addedColors` *unions* extra colors onto the copy (vs `overrideColors` which replaces; ignored when
  `overrideColors` is set) — e.g. The Jolly Balloon Man's "a 1/1 red Balloon creature in addition to its
  other colors and types".
  `addedSubtypes` *unions* extra subtypes onto the copy (vs `overrideSubtypes` which replaces) — e.g.
  Nexus of Becoming's "a 3/3 Golem … in addition to its other types".
  `addCardTypes` (e.g. `setOf("ARTIFACT")`) *unions* extra card types onto the copy's type line for the
  "except it's a [type] in addition to its other types" clause (the targeted sibling of
  `CreateTokenCopyOfSource`'s `addCardTypes`; Molten Duplication, Nexus of Becoming).
  `overrideCardTypes` replaces the copy's card types outright — "it's a Food artifact … and it loses all
  other card types" → `setOf(CardType.ARTIFACT)`; `activatedAbilities` grants extra activated abilities to
  the copy (the Food "{2}, {T}, Sacrifice this token: You gain 3 life") — together model Shelob, Child of
  Ungoliant's death-trigger Food token.
  `addedStaticAbilities` grants extra static abilities to the copy — the "except it has \"[static ability]\""
  copy clause — via `GameState.grantedStaticAbilities` (the same channel as `activatedAbilities`, since
  tokens have no `CardDefinition`). Firion, Wild Rose Warrior's token copy of an entering Equipment adds
  `ReduceEquipCost(amount = 2, onlyOwnEquip = true)` ("This Equipment's equip abilities cost {2} less to
  activate"). Any static reader that a granted ability must reach has to union `grantedStaticAbilities` with
  printed statics (as the equip-cost reducer and combat managers already do).
  `attacking` only applies to copies whose printed type line is a creature (a copy of a non-creature card
  still enters tapped but never attacking). `sacrificeAtStep` schedules one delayed `SacrificeTargetEffect`
  per created copy at that step (the sacrifice sibling of `CreateTokenEffect.sacrificeAtStep`);
  `sacrificeOnlyOnControllersTurn = true` restricts it to "at the beginning of *your* next end step"
  (Mardu Siegebreaker: a tapped+attacking copy of the linked-exiled card, sacrificed at your next end step).
  `exileAtStep` is the *exile* sibling — it schedules one delayed `MoveToZoneEffect(token, EXILE)` per copy
  at that step (the next matching step of any player's turn, "the next end step"). When
  `exileUnlessSourceIsRingBearer = true` that exile is wrapped in `Gate.WhenCondition(SourceIsRingBearer)`
  so it is skipped while the source is the controller's Ring-bearer at fire time (CR 701.54e) — "create a
  tapped and attacking token that's a copy of that card … at the beginning of the next end step, exile that
  token unless ~ is your Ring-bearer" (Sauron, the Necromancer).
  `controller` (an `EffectTarget` player ref) overrides who creates — and so controls and owns — the token;
  `null` defaults to the effect's controller. Set it for "**Target player** creates a token that's a copy of
  target creature you control" (Echocasting Symposium): the chosen creature is copied but the token enters
  under the named player's control. Mirrors `CreateTokenEffect.controller`.
  Like `CreateToken`, both `CreateTokenCopyOfTarget` and `CreateTokenCopyOfSource` publish their created token
  entity IDs to the `CREATED_TOKENS` pipeline collection, so a sibling effect in a `CompositeEffect` can address
  the new copy — e.g. Applied Geometry's "Create a token that's a copy … Put six +1/+1 counters on it" composes
  `CreateTokenCopyOfTarget(...)` then `AddCountersToCollection(CREATED_TOKENS, "+1/+1", 6)`.
- `CreateTokenCopyOfEquippedCreature(count?, tapped?)` — equipment-specific copy.
- `CreateRandomCreatureTokenWithManaValue(manaValue)` — create a token that's a copy of a *randomly
  chosen* creature card whose mana value equals `manaValue` (the Momir Basic Vanguard avatar's payoff —
  "Momir Vig, Simic Visionary": `{X}, Discard a card`). The candidate pool is the active
  `Format.MomirBasic.eligibleCreatureNames` (every creature across all sets, stored pre-sorted for
  replay-stable RNG); the executor filters it to `cmc == manaValue`, picks
  one with the game's seeded `GameRng`, and mints a token copy via `TokenFromDefinition` (the minting
  path for a bare `CardDefinition`, sibling to the in-zone token-copy path). If no creature has that
  mana value, nothing is created (the cost was still paid). The minted token's own `{X}` reads 0 — it
  never went on the stack. Pass `DynamicAmount.XValue` for "mana value X".
- `CreateTreasure(count?, tapped?, controller?)` — Treasure tokens. `count` accepts an `Int` or a `DynamicAmount`
  (the latter evaluated at resolution, e.g. `CreateTreasure(DynamicAmounts.sourcePower(), tapped = true)`
  for Goldvein Hydra's "create a number of tapped Treasure tokens equal to its power"). `controller`
  (Int overload) redirects the tokens, e.g. `EffectTarget.TargetController` for "its controller creates
  two Treasure tokens" (An Offer You Can't Refuse).
- `CreateFood(count?, controller?)` — Food tokens.
- `CreateBlood(count?, controller?)` — Blood tokens (artifact with "{1}, {T}, Discard a card, Sacrifice this artifact: Draw a card.").
- `CreateClue(count?, controller?)` / `Investigate(count?, controller?)` — Clue tokens (artifact with
  "{2}, Sacrifice this token: Draw a card."). `Investigate` is the keyword-action spelling (CR 701.36) so
  card text "investigate" maps directly; both create the same predefined `Clue` token — Malcolm, the Eyes.
- `CreateShard(count?, controller?)` — Shard tokens (the Clue token's enchantment cousin: colorless
  "Enchantment — Shard" with "{2}, Sacrifice this enchantment: Scry 1, then draw a card."). Niko, Light of Hope.
- `CreateLander(count?, controller?)` — Lander land tokens.
- `CreateMeteorite(count?, tapped?, controller?)` — Meteorite tokens (Roxanne, Starfall Savant): a
  colorless artifact with "When this token enters, it deals 2 damage to any target." and "{T}: Add
  one mana of any color." Roxanne creates them `tapped = true`.
- `CreateMutavault(count?, tapped?, controller?)` — Mutavault tokens.
- `CreateEverywhere(count?, tapped?, controller?)` — Everywhere land tokens (Overlord of the Hauntwoods):
  a colorless land token with all five basic land subtypes (Plains/Island/Swamp/Mountain/Forest) that
  taps for any color — i.e. the mana ability of each basic land type, without the basic supertype. The
  Overlord of the Hauntwoods trigger creates them `tapped = true`.
- `CreateRoleToken(roleName, target)` — attach a Role aura token.
- `CreateMapToken(count?)` — Map artifact tokens.
- `CreateDroneToken(count?)` — Drone tokens.
- `CreateMunitionsToken(count?)` — Munitions noncreature artifact tokens (Weapons Manufacturing); the LTB damage
  trigger lives on the predefined `Munitions` `CardDefinition` and is picked up automatically by the engine's
  `TriggerAbilityResolver`.
- `CreateMutagenToken(count?)` / `CreateMutagenToken(amount: DynamicAmount)` — Mutagen noncreature artifact tokens
  (Teenage Mutant Ninja Turtles). The token is `"Artifact — Mutagen"` with the sorcery-speed activated ability
  `"{1}, {T}, Sacrifice this token: Put a +1/+1 counter on target creature."`, defined on the predefined `Mutagen`
  `CardDefinition` (so the ability is resolved automatically). The `DynamicAmount` overload serves X-count makers
  (Mutagen Man, Living Ooze — "create X Mutagen tokens").
- `CreatePermanentEmblem(name, abilities)` — planeswalker emblem with static abilities.

### Ability granting

- `GrantTriggeredAbilityEffect(ability)` — permanently grant a triggered ability.
- `CreateGlobalTriggeredAbility(ability, duration = Duration.Permanent, descriptionOverride? = null)` — engine-wide triggered ability with no source permanent. `duration` is a plain parameter, so the one method covers every lifetime: `Duration.EndOfTurn` (False Cure, Death Frenzy), `Duration.UntilYourNextTurn` (Season of the Bold), `Duration.EndOfCombat`, `Duration.Permanent` (Dimensional Breach, planeswalker emblems), etc. `descriptionOverride` sets emblem display text.
- `GrantSpellKeywordEffect` — grant a keyword to a spell on the stack.
- `GrantSpellsCantBeCountered(target, filter, duration)` — target's matching spells become uncounterable (Domri shape).
- `GrantFlashToSpells(target, spellFilter, duration)` — target may cast matching spells as though they had flash (CR 702.8a) for `duration` (default `EndOfTurn`). Resolution-time one-shot that records the grant on the player and survives the source spell leaving the stack. Used by **Borne Upon a Wind** ("You may cast spells this turn as though they had flash."); narrower filters like `GameObjectFilter.Sorcery` cover "you may cast sorcery spells as though they had flash" variants. Sibling of the permanent-static [`GrantFlashToSpellType`](#9-static-abilities) — use the static for "as long as this is on the battlefield" wording (the two Gandalfs); use this Effect for a turn-scoped or duration-bounded grant.

### Control & combat

- `GainControlEffect(target, duration)` — gain control until end of turn (default). Pair with
  `Duration.WhileSourceTapped` (Callous Oppressor) or
  `Duration.WhileSourceTappedAndAffectedPowerAtMostSource` (Old Man of the Sea) for the classic
  "for as long as this creature remains tapped [and the stolen creature's power stays ≤ source's
  power]" steal pattern, or `Duration.WhileYouControlSource("<source name>")` for the
  "for as long as you control this [permanent]" pattern (Aladdin, Scroll of Isildur Chapter I,
  Rangers of Ithilien), or `Duration.WhileSourceAttachedToAffected` for "gain control … for as
  long as that Aura is attached to it" (Eriette, the Beguiler — the effect is sourced from the
  *Aura*, so the executor swaps the floating effect's source to the triggering attachment, and the
  control ends the instant the Aura leaves, detaches, or re-attaches elsewhere). `StateProjector`
  gates these per-frame for the instantaneous view; the
  one-way half of CR 611.2b ("for as long as" durations don't restart) is enforced by the
  `EndedDurationExpiryCheck` state-based action, which physically removes the effect the moment
  the condition fails — so a pump that wears off, a re-tap, or a re-acquired source never
  re-grabs the creature.
- `ExchangeControlEffect(target1, target2)` — swap control of two permanents.
- `GainControlByMostEffect(metric, target?)` — the player with strictly the most of a `PlayerRankMetric` takes it (tie = no change). Metrics: `PlayerRankMetric.LifeTotal` (Ghazbán Ogre), `PlayerRankMetric.CreaturesOfSubtype(subtype)` (Thoughtbound Primoc). Facades: `Effects.GainControlByMostLife()`, `Effects.GainControlByMostOfSubtype(subtype)`.
- `GiftGivenEffect(target)` — "gift" temporary control.
- `CantAttackEffect(target, unless?)` — target can't attack.
- `CantBlockEffect(target, unless?)` — target can't block.
- `CantAttackGroupEffect(filter, condition?)` — group-scoped can't-attack.
- `CantBlockGroupEffect(filter, condition?)` — group-scoped can't-block.
- `Effects.Suspect(target)` — target becomes Suspected (MKM keyword). Composite: `SetSuspectedEffect` (named status, CR 701.60d dedup) + `GrantKeywordEffect(MENACE)` + `CantBlockEffect`.
- `RemoveFromCombatEffect(target, unblockSoleBlockedAttackers = false)` — yank target out of combat.
  Set `unblockSoleBlockedAttackers = true` for the old-rules behavior (Ydwen Efreet): attackers the
  target was sole blocker of become unblocked (CR 509.1h normally keeps them blocked).
- `Effects.OpponentGuessesTopCardKind(onGuessedRight, onGuessedWrong, chooser = Controller, guesser = Opponent)`
  (`OpponentGuessesTopCardKindEffect`) — "Choose land or nonland. An opponent guesses whether the top
  card of your library is the chosen kind. Reveal that card. If they guessed right, [onGuessedRight];
  otherwise, [onGuessedWrong]." (Gollum, Scheming Guide.) A reusable opponent-guess primitive that
  sequences two `ChooseOptionDecision`s: the `chooser` picks the framing land/nonland kind, then the
  `guesser` guesses the *actual* kind of the top card of the chooser's library; the card is revealed
  and the guess compared to reality (a correct guess = the guesser's call matches the actual top card).
  Both branch effects resolve in the source's original context, so `EffectTarget.Self` inside them
  refers to the ability's source. Empty library → no top card → guess can never be right, so the
  "wrong" branch runs. `chooser`/`guesser` reuse the shared `Chooser` enum (see `ChoosePileEffect`).
- `Effects.CanAttackDespiteDefenderThisTurn(target = Self)` (`CanAttackDespiteDefenderThisTurnEffect`) — target can attack this
  turn as though it didn't have defender. Adds a transient `CanAttackDespiteDefenderThisTurnComponent`
  honored by the defender attack-restriction rule and cleaned up at end of turn. The
  activated/temporary counterpart to the static `CanAttackDespiteDefender` ability (Krotiq Nestguard).
- `Effects.Goad(target = ContextTarget(0))` (`GoadEffect`) — goad target creature (CR 701.15).
  Tags the creature with `GoadedComponent(goaderIds: Set<EntityId>)`; the effect's controller at
  resolution is recorded as the goader. While goaded the creature (a) must attack each combat if able
  and (b) can't attack any player in `goaderIds` if a non-goader player is available to attack (per
  CR 701.15b the alternative is a *player*, not a planeswalker) — both checks
  live inline in `AttackPhaseManager.declareAttackers` alongside the must-attack-this-turn pass. The
  goader set deduplicates, so the same player re-goading is a no-op (CR 701.15d); multiple distinct
  goaders stack (CR 701.15c). After the untap step of each player's turn,
  `CleanupPhaseManager.expireGoadedDesignationFor` drops that player from every goader set and
  removes the component when the set is empty — same hook as the `Duration.UntilYourNextTurn`
  floating-effect path, implementing the "until your next turn" duration (CR 701.15a). Surfaced to
  the client as the `Goaded` badge on the
  card (listing goader names) — there is no separate game-log event. Used by **Glóin, Dwarf
  Emissary**: `Costs.Composite(Costs.Tap, Costs.Sacrifice(Artifact.withSubtype("Treasure"))):
  Goad(target creature)`.
- `Effects.SkipNextTurn(target = Controller, count = Fixed(1))` (`SkipNextTurnEffect`) — target skips their next `count` turns. `count` is a `DynamicAmount`, so it can read a pipeline value (e.g. a coin-flip tally via `DynamicAmount.VariableReference`). Skips accumulate on a `SkipNextTurnComponent(turns)`, decremented one turn per the player's turn-start; a resolved count of 0 is a no-op. Used by Lethal Vapors (one turn) and **Ral Zarek, Guest Lecturer** (skip N turns where N = heads).
- `Effects.FlipCoins(count, storeHeadsAs = "heads")` (`FlipCoinsEffect`) — flip `count` coins and store the number of heads under `storeHeadsAs` in the pipeline (`storedNumbers`) so a later sub-effect in the same composite can scale off it via `DynamicAmount.VariableReference`. The general "flip N coins, count heads" primitive (CR 705); unlike `FlipCoinEffect` (branch on win/lose) and `FlipTwoCoinsEffect` (branch on combined outcome) it only tallies. Each flip emits a `CoinFlipEvent`. **Ral Zarek, Guest Lecturer**'s ultimate composes `FlipCoins(5, "heads")` then `SkipNextTurn(target, count = VariableReference("heads"))`.
- `Effects.SkipNextDrawStep(target = Controller)` (`SkipNextDrawStepEffect`) — target skips their next draw step. Adds a one-shot `SkipDrawStepComponent` marker consumed by `DrawPhaseManager.performDrawStep` (Elfhame Sanctuary's "you skip your draw step this turn").
- `Effects.HijackNextTurn(target)` / `Effects.HijackNextCombatPhase(target)` (`HijackNextTurnEffect(target, scope)`, `scope` = `HijackScope.NextTurn` | `NextCombatPhase`) — Mindslaver-style: you make all decisions for the target player during their next whole turn, or during their next combat phase only. Moves *input authority* only (resource/permanent/spell ownership stays with the affected player); reuses `PlayerTurnHijackedComponent` + `GameState.actorFor`, so hand visibility and legal-action routing follow automatically. A scheduled hijack waits through skipped turns/combat phases and engages on the next one the player actually takes. Turn scope engages at turn start and clears at end-of-turn cleanup (**The Dominion Bracelet**); combat scope engages at beginning of combat and clears when that one combat phase ends — extra combat phases are not controlled (**Secret of Bloodbending**, whose optional waterbend upgrades combat→turn via `ConditionalEffect(Conditions.WaterbendWasPaid, HijackNextTurn, elseEffect = HijackNextCombatPhase)`).
- `GrantCantBeBlockedByChosenColorEffect(target, duration)` — unblockable except by chosen color.
- `Effects.GrantCantBeBlockedExceptBy(target, blockerFilter, duration = EndOfTurn)` (`GrantCantBeBlockedExceptByEffect`) —
  the floating, one-shot grant of "can't be blocked except by creatures matching `blockerFilter`". The dynamic
  counterpart to the static `CantBeBlockedExceptBy` ability (and the filter-based sibling of the color-only
  `GrantCantBeBlockedExceptByColorEffect`). Routes through the same projected `cantBeBlockedExceptByFilters` channel
  the static ability uses, so the existing `CantBeBlockedExceptByRule` enforces it. Used by **Resilient Roadrunner**:
  `{3}: This creature can't be blocked this turn except by creatures with haste` —
  `Effects.GrantCantBeBlockedExceptBy(EffectTarget.Self, GameObjectFilter.Creature.withKeyword(Keyword.HASTE))`.
- `CantBeBlockedByFewerThan(minBlockers, filter = source())` (static ability) — "can't be blocked
  except by N or more creatures," a generalization of menace (the N = 2 case). May be left unblocked;
  the restriction only applies once at least one creature blocks it. Enforced at block declaration in
  `BlockPhaseManager.validateMinBlockersRequirements`, mirroring the menace check. Used by Troll of
  Khazad-dûm (`CantBeBlockedByFewerThan(3)`).
- `CantCastSpellsEffect(target, until?)` — target can't cast spells. Facade: `Effects.CantCastSpells(target, duration)`.
- `CantPlayCardsFromHandEffect(target = Controller, duration = UntilYourNextTurn)` — target can't play cards (cast
  spells **or** play lands) from their **hand** zone for the duration. Hand-scoped: cards in exile/graveyard with a
  may-play permission stay playable. Facade: `Effects.CantPlayCardsFromHand(target, duration)`. Pairs with an impulse
  grant (`ExilePatterns.impulse`) so a player swaps their hand for the top cards of their library for a turn
  (Memory Vessel). Distinct from `CantCastSpells` (every zone, spells only).
- `CantCastSpellsFromNonHandZonesEffect(target, duration = UntilYourNextTurn)` — target can't cast spells from any zone
  **other than their hand** for the duration; ordinary hand casts still resolve, but graveyard (flashback/escape),
  exile (foretell/plot/a may-play permission), library-top, and command-zone casts all become illegal. The **inverse**
  of `CantPlayCardsFromHand` (which restricts *to* the hand): this restricts *away from* every zone except the hand.
  Facade: `Effects.CantCastSpellsFromNonHandZones(target, duration)`. Stamps `CantCastFromNonHandZonesComponent` on the
  player (with an `expiresForPlayerId` keyed to the *casting* player for the `UntilYourNextTurn` window, like
  `PlayerCantPlayFromHandComponent`); enforced authoritatively in `CastSpellHandler` and suppressed at enumeration by the
  non-hand `CastFromZoneEnumerator`. The "your opponents can't cast spells from anywhere other than their hands" clause of
  Avatar's Wrath (`target = EffectTarget.PlayerRef(Player.EachOpponent)`).
- `Effects.CantPlayLandsThisTurn(target = Controller)` (`PreventLandPlaysThisTurnEffect`) — the target player can't
  play lands for the rest of this turn (sets remaining land drops to 0). Defaults to the controller (Rock Jockey);
  pass `EffectTarget.ContextTarget(n)` for "target player can't play lands this turn" cards like Turf Wound.
- `CantActivateLoyaltyAbilitiesEffect(target, duration)` — target can't activate planeswalkers' loyalty abilities.
  Facade: `Effects.CantActivateLoyaltyAbilities(target, duration)`. Sibling of `CantCastSpells`; compose the two for
  cards that forbid both (e.g. Revel in Silence).

### Forced sacrifice / discard

- `SacrificeTargetEffect(target, sacrificedByItsController = false)` — sacrifice a specific permanent. By
  default only fires if the resolving player controls it; set `sacrificedByItsController = true` for
  "[that creature]'s controller sacrifices it" (e.g. The Ring's Ring-bearer ability).
- `Effects.Sacrifice(filter, count, target)` / `ForceSacrificeEffect(target, count)` — edict; target
  sacrifices N permanents matching the filter (target chooses). `count` accepts an `Int` or a
  `DynamicAmount` — pass a `DynamicAmount` (via the `Effects.Sacrifice(filter, count: DynamicAmount, …)`
  overload / `ForceSacrificeEffect.dynamicCount`) for "sacrifices half the creatures they control,
  rounded up" (Rush of Dread): `Divide(AggregateBattlefield(Player.ContextPlayer(0), Creature), Fixed(2),
  roundUp = true)`. The amount is evaluated at resolution against the resolving context, so a per-target
  player reference counts the chosen player's permanents.
- `Effects.SacrificeAnyNumber(filter)` (= `SacrificeEffect(filter, any = true)`) — the *resolving*
  player chooses 0+ of their own permanents matching `filter` to sacrifice. Distinct from
  `ForceSacrifice` (edict on a target) and from `Costs.pay.Sacrifice` (a cost): this is a
  resolution effect. The sacrificed permanents are recorded in the effect context, so a later
  composite step can read the count via `DynamicAmounts.permanentsSacrificedThisWay()` — e.g.
  "Sacrifice any number of lands. Reveal the top X cards … where X is the number of lands
  sacrificed this way" (Hew the Entwood; same shape as Scapeshift).
- "Return a permanent you control [to its owner's hand]" is a pipeline composition, not an effect type:
  `GatherCards(BattlefieldMatching(filter, Player.You, excludeSelf?))` →
  `SelectFromCollection(ChooseExactly(1), useTargetingUI = true)` → `MoveCollection(→ HAND)` (the
  battlefield→hand move routes each card to its owner's hand). See Mistbreath Elder.

### Stack manipulation

- `CounterEffect(target, condition?, destination?)` — counter a spell/ability; optionally send elsewhere. `CounterDestination.Exile(grantFreeCast?)`: `grantFreeCast` lets the counter's *controller* recast the exiled card for free (Kheru Spellsnatcher). (For "exile it; its owner may recast it" wording that is **not** a counter — e.g. airbending a spell — use `ExileTargetSpell(fixedAlternativeManaCost = …)` below, which bypasses can't-be-countered.)
  - `target = CounterTarget.Spell` / `Ability` / `SpellOrAbility` — `SpellOrAbility` dispatches at resolution by inspecting whether the stack entity has a `SpellOnStackComponent`. Used by Teferi's Response.
  - `condition = CounterCondition.UnlessPaysMana(cost, onPaid?)` / `UnlessPaysDynamic(amount, onPaid?)` — "unless its controller pays …" with an optional `onPaid: Effect` rider that fires **only** when the spell's controller pays (Divert Disaster's "If they do, you create a Lander token"). The rider executes with the counter's controller as `controllerId`, so "you" in the rider resolves to the caster of the counter. The rider does not fire when the spell is countered. Facade: `Effects.CounterUnlessPays(cost, onPaid)` / `Effects.CounterUnlessDynamicPays(amount, exileOnCounter, onPaid)`.
- `CounterAllOnStackEffect(filter?, destination?)` — counter everything matching.
- `ExileTargetSpellEffect(makePlotted = false, fixedAlternativeManaCost = null)` (facade `Effects.ExileTargetSpell(makePlotted, fixedAlternativeManaCost)`) — exile target spell (CR 718 "exile target spell"). **Not a counter:** it removes the spell from the stack and exiles the card even if the spell *can't be countered* (so it works where `CounterEffect(destination = Exile())` no-ops), and it fires no "whenever a spell is countered" trigger — but the spell still fails to resolve because it left the stack. With `makePlotted = true` the exiled card becomes *plotted* for its **owner** (gains `PlottedComponent` + a permanent free-cast-on-a-later-turn `MayPlayPermission` gated by `SourcePlottedOnPriorTurn`, granted to the owner per CR 718.2), emitting a `CardPlottedEvent`. With `fixedAlternativeManaCost = {2}` the exiled card's **owner** instead gets a permanent `MayPlayPermission` + `PlayWithFixedAlternativeManaCostComponent`, letting them recast it for that fixed cost rather than its printed cost — the spell-on-stack form of the **Airbend** keyword (Aang, Swift Savior). `makePlotted` and `fixedAlternativeManaCost` are mutually exclusive. Pair with `Targets.Spell`. Used by **Aven Interrupter** ("…exile target spell. It becomes plotted.") and the airbend stack branch.
- `MarkSpellExileWithCountersEffect(target = TriggeringEntity, counterType, count = 1)` (facade `Effects.MarkSpellExileWithCounters(target, counterType, count)`) — mark a spell on the stack so that, **as it resolves**, it is exiled with `count` counters of `counterType` on it instead of being put into its owner's graveyard. Lets the spell resolve fully, then re-routes only its post-resolution destination via `ExileAfterResolveComponent(onlyIfResolved = true)` — so if the spell is countered or fizzles it goes to the graveyard normally. Used by **Goliath Daydreamer** ("exile that card with a dream counter on it instead of putting it into your graveyard as it resolves").
- `MarkSpellPlotOnResolveEffect(target = TriggeringEntity)` (facade `Effects.MarkSpellPlotOnResolve(target)`) — the plot sibling of `MarkSpellExileWithCounters`: as the spell resolves it is exiled instead of going to the graveyard and **becomes plotted** for its owner (`PlottedComponent` + permanent free-cast-on-a-later-turn `MayPlayPermission` gated by `SourcePlottedOnPriorTurn`, emitting `CardPlottedEvent`). Also `onlyIfResolved` — a countered/fizzled spell is not exiled and doesn't become plotted. Distinct from `ExileTargetSpell(makePlotted = true)`, which removes a *targeted* spell from the stack now (it never resolves); this one only changes a self-cast spell's destination after it resolves. Used by **Lilah, Undefeated Slickshot** ("Whenever you cast a multicolored instant or sorcery spell from your hand, exile that spell instead of putting it into your graveyard as it resolves. If you do, it becomes plotted.").
- `spell { returnTransformedFromGraveyard(vararg counters: CounterType) }` — **not** an effect but a resolution-destination flag on the spell's `CardScript` (`returnTransformedFromGraveyardOnResolve: ReturnTransformedFromGraveyard?`). Marks a double-faced card so that, when it resolves **after being cast from a graveyard**, it is exiled and then put onto the battlefield **transformed** (its back face up) under its owner's control, entering with the given `counters`, instead of going to its owner's graveyard. Models **Esper Origins** ("If this spell was cast from a graveyard, exile it, then put it onto the battlefield transformed under its owner's control with a finality counter on it"): `spell { effect = …; returnTransformedFromGraveyard(CounterType.FINALITY) }` on the sorcery front, joined to a Saga-creature back via `frontFace.copy(backFace = …)`. Like flashback's own graveyard-cast exile, the destination is derived from the spell's `castFromZone` at resolution time (in `StackResolver`), **not** from an effect run during resolution — so it survives a mid-resolution pause (e.g. an earlier Surveil in the same resolution) and is correctly inert when the spell is countered or fizzles. It **takes precedence over the flashback exile**: a graveyard-cast card that both has flashback and this flag returns transformed rather than exiling. Requires a permanent back face; a non-DFC or non-permanent back is a no-op (the card falls through to its normal graveyard/exile destination, per the official ruling on putting a non-double-faced card onto the battlefield transformed). The back-face flip + battlefield entry reuse the shared `returnDfcFaceFromExile` helper (a Saga back enters with a fresh lore counter, CR 714.2b; leaves/enters triggers fire, not transform triggers).
- `OpenLifeBid(onWin, participant = Player.AnOpponent)` — open life-bidding auction between you and `participant` (resolved against the effect context). You open at a bid of 1; the two bidders alternate topping the high bid (yes/no to top, then a number for the amount, capped at the bidder's life) until one passes. The high bidder loses that much life; `onWin` runs **only if you win**, with the original targets in context. If `participant` resolves to you (or to nobody), you're the sole bidder and win at the opening bid. For Mages' Contest, bid against the targeted spell's controller and counter it: `Effects.OpenLifeBid(Effects.CounterSpell(), Player.ControllerOf("target spell"))` — pair with a `TargetSpell` requirement.
- `DestroySourceOfTargetedAbilityEffect` — when the targeted stack object is a permanent's activated/triggered ability, destroy that source permanent. Compose *before* the counter step so the ability component is still readable (Teferi's Response).
- `CopyTargetSpellEffect(target, keywordsForCopy, removeLegendary, addedTokenKeywords, sacrificeTokenAtStep, sacrificeTokenOnlyOnControllersTurn)` (facade `Effects.CopyTargetSpell(...)`) — copy a spell on the stack. `keywordsForCopy` grants keywords to the copy **while it remains a spell** (wither/lifelink). When the copied spell is a **permanent spell** it becomes a token as it resolves (CR 707.10f); the *token-side* riders bake onto that token for its life on the battlefield: `addedTokenKeywords` (e.g. `HASTE`) are unioned into the token's base keywords, and `sacrificeTokenAtStep: Step?` registers a delayed "sacrifice this token" trigger at the next matching step (`sacrificeTokenOnlyOnControllersTurn` gates it to "your next" step). The spell-copy mirror of `CreateTokenCopyOfTargetEffect.addedKeywords` / `sacrificeAtStep`. Used by **Choreographed Sparks** ("Copy target creature spell you control. The copy gains haste and 'At the beginning of the end step, sacrifice this token.'"). Pair with `Targets.CreatureSpellYouControl`.
- `CopyEachTargetSpellEffect()` (facade `Effects.CopyEachTargetSpell(keywordsForCopy, removeLegendary)`) — copy **every** spell targeted by this effect (one copy per `ChosenTarget.Spell` in context), pausing per copy that has targets so the controller may choose new targets (CR 707.10). Pair with an unlimited spell target requirement — `Targets.AnyNumberOfInstantOrSorcerySpells`. Used by Display of Power ("Copy any number of target instant and/or sorcery spells."). Spells flagged `cantBeCopied` are skipped.
- `CopyTargetTriggeredAbilityEffect(target)` — copy a triggered ability on the stack.
- `CopyTargetSpellOrAbilityEffect(target, copies = DynamicAmount.Fixed(1))` (facade `Effects.CopyTargetSpellOrAbility(target, copies)`) — copy whichever kind of stack object the single target resolved to, dispatching at resolution by inspecting the stack entity's component: an instant/sorcery **spell** copies via the spell-copy path, a **triggered ability** via `CopyTargetTriggeredAbilityEffect`'s logic, an **activated ability** by cloning its `ActivatedAbilityOnStackComponent`. You may choose new targets for the copy (CR 707.10c). Pair with `Targets.InstantSorcerySpellOrAbility` (one requirement admitting all four kinds). Generalizes the two single-kind copy effects into the "copy target instant/sorcery spell, activated ability, or triggered ability" clause — **Return the Favor**. `copies` (a `DynamicAmount`, default 1) makes *N independent copies* of an **ability** — pass `DynamicAmount.XValue` for "copy target activated or triggered ability you control X times" (**Gogo, Master of Mimicry**); the executor pauses per copy that has targets so each is retargeted independently, and a no-target ability is copied all the same. `copies` > 1 applies only to the ability branches; the spell branch always makes a single copy. An ability instance tagged "can't be copied" (`ActivatedAbility.cantBeCopied`, see §9/§11) yields no copies (CR 707.10e).
- `CopyNextSpellCastEffect(copies = 1, spellFilter = InstantOrSorcery)` (facade `Effects.CopyNextSpellCast(copies, spellFilter)`) — when its controller next casts a spell matching `spellFilter` this turn, create `copies` copies of it. `spellFilter` is a `GameObjectFilter` matched against the spell as it's cast, so the default "instant or sorcery" (Howl of the Horde) can be widened — e.g. `GameObjectFilter.Creature` for "copy the next creature spell." Consumed after one matching cast. Non-matching casts leave the entry waiting.
- `CopyEachSpellCastEffect(copies = 1, spellFilter = InstantOrSorcery)` (facade `Effects.CopyEachSpellCast(copies, spellFilter)`) — the persistent sibling: copies **every** spell matching `spellFilter` the controller casts for the rest of the turn (The Mirari Conjecture Ch. III). Same `spellFilter` parameterization as above.
- `MakeNextSpellUncounterableEffect(spellFilter = Any)` (facade `Effects.MakeNextSpellUncounterable(spellFilter)`) — one-shot rider: the controller's **next** spell matching `spellFilter` cast this turn can't be countered, then the entry is consumed. Stamps `CantBeCounteredComponent` on that spell as it's cast (so it stays uncounterable for as long as it's on the stack); non-matching casts leave the entry waiting, and an unused entry clears at the start of the controller's next turn. Same pending-rider shape as `CopyNextSpellCastEffect`. Contrast with the duration-based `GrantSpellsCantBeCountered` (Domri), which protects **every** matching spell cast for a whole duration rather than just the next one. Used by **Mistrise Village** ("{U}, {T}: The next spell you cast this turn can't be countered.").
- `GrantNextSpellAffinityEffect(spellFilter = Noncreature, forType = ARTIFACT)` (facade `Effects.GrantNextSpellAffinity(spellFilter, forType)`) — one-shot rider mirroring `MakeNextSpellUncounterable`, but the controller's **next** matching spell this turn gains **affinity for `forType`**: the cost calculator reduces it by the caster's count of that card type *at cast time* (dynamic), then `CastSpellHandler` consumes the entry. Used by **Don & Raph, Hard Science** ("the next noncreature spell you cast this turn has affinity for artifacts").
- `CopyCardIntoCollectionEffect(source, storeAs)` (facade `Effects.CopyCardIntoCollection(source, storeAs)`) — copy a **card in a zone** (not a spell on the stack), publishing the copy's entity id to pipeline collection `storeAs`. Per Rule 707.12 the copy is created in the card's current zone under the effect's controller and tagged as a stack-style copy, so once cast it becomes a token if it's a permanent spell and ceases to exist if it's an instant/sorcery (Rule 707.10). Pair with `CastFromCollectionWithoutPayingCostEffect(from)` (facade `Effects.CastFromCollectionWithoutPayingCost(from)`, wrap in `MayEffect` for "you may cast") to express "copy a card, then cast the copy" — e.g. **Shiko, Paragon of the Way**: `Composite(MoveToZoneEffect(target, Zone.EXILE), Effects.CopyCardIntoCollection(target, "copy"), MayEffect(Effects.CastFromCollectionWithoutPayingCost("copy")))`. A copy that is never cast is swept up by the Rule 707.10a state-based action (`PhantomCardCopiesCheck`), so no explicit cleanup step is needed. For the "you may cast it" wording that **doesn't** say "without paying its mana cost", use `Effects.CastFromCollection(from, storeCastTo?)` (`CastFromCollectionWithoutPayingCostEffect(from, payManaCost = true, storeCastTo)`): the controller pays the spell's normal cost (an {X} spell prompts for X) instead of casting for free. Pass `storeCastTo` to publish the cast card's id to that pipeline collection on a successful cast, then gate a follow-up with `IfYouDoEffect(this, then, SuccessCriterion.CollectionNonEmpty(storeCastTo))` — e.g. **Kaervek, the Punisher**: `Composite(Move(target, EXILE), CopyCardIntoCollection(target, "copy"), MayEffect(IfYouDoEffect(CastFromCollection("copy", storeCastTo = "cast"), LoseLife(2, Controller), SuccessCriterion.CollectionNonEmpty("cast"))))` — declining (or being unable to pay) leaves the collection empty, so no life is lost. (`storeCastTo` is reliably published for synchronous casts and target-selection casts; an {X}-cost spell cast with no targets is the one sub-case where the publish doesn't survive the X pause.) **Free-casting still pays the copied spell's non-mana additional costs** (CR 601.2f / 118.9 waive only the mana cost) — when the copy carries a printed sacrifice / discard / exile / tap additional cost, the engine resolves it during the synthesized cast: a forced single option is auto-paid, and a real choice pauses for an on-battlefield (sacrifice/tap) or overlay (discard/exile) selection; if the cost can't be paid the cast doesn't happen (e.g. Roving Actuator copying **Embrace Oblivion**'s "sacrifice an artifact or creature" still makes you sacrifice).
- `CopyCollectionIntoCollectionEffect(from, storeAs)` (facade `Effects.CopyCollectionIntoCollection(from, storeAs)`) — the collection-wide sibling of `CopyCardIntoCollectionEffect`: copy **every** card in pipeline collection `from`, publishing all the copies' entity ids (in `from` order) to `storeAs`. For "copy them" over a set of cards rather than one (`CopyCardIntoCollection` overwrites its collection, so it can't accumulate across a `ForEach`). Each copy is created in its original's current zone (Rule 707.12) and tagged as a stack-style copy, so gather/exile the originals first, then copy. Pair with `Effects.CastAnyNumberFromCollection(storeAs)` for "copy them. You may cast any number of the copies" — e.g. **The Tale of Tamiyo** IV: `Composite(ForEachTargetEffect(Move(ContextTarget(0), EXILE)), GatherCards(ChosenTargets, "exiled"), CopyCollectionIntoCollection("exiled", "copies"), CastAnyNumberFromCollection("copies"))`. Copies never cast are swept by the Rule 707.10a state-based action.
- `CastAnyNumberFromCollectionWithoutPayingCostEffect(from, payManaCost = false)` (facades `Effects.CastAnyNumberFromCollectionWithoutPayingCost(from)` for free / `Effects.CastAnyNumberFromCollection(from)` for paid) — the multi-cast sibling of `CastFromCollectionWithoutPayingCostEffect`. **During this effect's resolution**, the controller is offered the cards in pipeline collection `from` (filtered to those still in exile) one at a time and may cast each until they decline; each cast's targets / X / modes flow through the normal cast machinery. With the default `payManaCost = false` each is cast for free; set `payManaCost = true` (facade `Effects.CastAnyNumberFromCollection`) for the "you may cast any number of [them]" wording **without** "without paying their mana costs" — each chosen card is then cast paying its normal cost (an {X} card prompts for X). Because the casts go through the synthesized-cast path (like Cascade), card-type **timing restrictions are ignored** and no lingering "you may play it later" permission is granted — cards left uncast just stay where they are (the controller can't wait until later in the turn). Hand it the eligible set: filter the collection upstream (e.g. nonland + `FilterCollection(ManaValueAtMost(...))`). The free form models "you may cast any number of spells with mana value X or less from among them without paying their mana costs" — e.g. **Kotis, the Fangkeeper**: `GatherCards(TopOfLibrary(damage, TriggeringPlayer)) → MoveCollection(→ exile) → FilterCollection(Nonland) → FilterCollection(ManaValueAtMost(damage)) → CastAnyNumberFromCollectionWithoutPayingCostEffect("castable")` (also **Villainous Wealth**, **Etali, Primal Storm**). The paid form models **The Tale of Tamiyo** IV (cast the copies paying their costs).
- `FilterCollection(from, CollectionFilter.InZone(zone), storeMatching)` — keep only the cards in pipeline collection `from` that are **currently** in `zone`. Pipeline collections track entity refs, not live location, so a card can leave its zone mid-resolution (e.g. an exiled card cast for free moves to the stack). Use this to act on "the ones still there." Models the "you may cast it … if you don't, put that card into your hand" fallback of the **Tarkir: Dragonstorm "…storm" enchantments** (Breaching Dragonstorm): `GatherUntilMatch(Nonland) → MoveCollection(→ exile) → FilterCollection(ManaValueAtMost(8), "castable") → ConditionalOnCollection("castable", ifNotEmpty = MayEffect(CastFromCollectionWithoutPayingCost("castable"))) → FilterCollection("nonland", InZone(EXILE), "uncast") → MoveCollection("uncast" → hand)` — only the nonland still in exile (not the one just cast) goes to hand; the lands stay exiled. The `ConditionalOnCollection` wrapper suppresses the empty "you may cast" prompt when the nonland's mana value is > 8.
- `MoveCollectionEffect(from, destination, filter = null, …)` — move a pipeline collection to a zone.
  `destination = ToZone(zone, player, placement)`; `ZonePlacement.Tapped` enters the battlefield
  tapped, and `player` sets the controller for a battlefield destination (so a card can enter under
  your control even when owned by an opponent — Sméagol). The optional `filter` moves only the cards
  in `from` matching it (the rest stay), letting one revealed pile be split by type in successive
  steps — e.g. revealed lands → battlefield tapped, the rest → graveyard/library (Sméagol, Galadriel
  of Lothlórien, The Ring Goes South). (Equivalent to a `FilterCollection` partition; the inline
  filter just avoids naming an intermediate collection.) `storeMovedAs = "<key>"` captures the
  resulting entity ids under a pipeline collection; `markEnteredViaSourceAbility = true` stamps each
  card that lands on the battlefield with `EnteredViaAbilityComponent(this source)` so a later
  `GatherCards(CardSource.EnteredViaThisResolution)` can re-collect them from live battlefield state.
- `ChangeTargetEffect(spell, newTarget)` — change a spell's target.
- `ChangeSpellTargetEffect(spell, filter)` — same, filtered.
- `ReselectTargetRandomlyEffect(spell)` — re-choose targets at random.
- `Effects.ChangeTriggeringObjectTargets(chooser = RetargetChooser.Controller)` — the player named by `chooser` may change the target or targets of the triggering spell/ability (`context.triggeringEntityId`); the player-chosen, multi-target counterpart of `ReselectTargetRandomly`. `RetargetChooser.Controller` = the effect's controller; `RetargetChooser.OwnerOfStored(name)` = the owner of the single card in pipeline collection `name` (≠1 card → no chooser → no-op). Reselection is offered slot-by-slot among the original object's legal targets (legality judged from *its* controller, current target kept as a "keep" option, no target chosen twice). **Psychic Battle** composes from atoms: `Composite(GatherCards(TopOfLibrary(1, Player.Each), revealed=true, storeAs="revealed"), FilterCollection("revealed", GreatestManaValue, storeMatching="w"), ChangeTriggeringObjectTargets(RetargetChooser.OwnerOfStored("w")))` — a tie keeps several greatest cards so `OwnerOfStored` finds no unique owner and the targets stay put.
- `ReturnSpellToOwnersHandEffect` / `Effects.ReturnSpellToOwnersHand()` — return the targeted spell (`ContextTarget`) from the stack to its owner's hand. Not a counter (CR 701.27 / 701.5b), so "can't be countered" doesn't block it. Pair with `Targets.Spell` (Reprieve, Hullbreaker Horror).
- `Effects.ReturnSpellOrPermanentToOwnersHand(target = ContextTarget(0))` (`ReturnSpellOrPermanentToOwnersHandEffect`) — bounce one target to its owner's hand, dispatching on what it resolves to: a **spell on the stack** is removed from the stack to hand (does not resolve; not a counter), a **permanent** is bounced normally (delegates to the standard `MoveToZone(HAND)` path with its full leave-the-battlefield cleanup). The bounce counterpart of `PutOnLibraryPositionOfChoiceEffect`. Pair with `TargetSpellOrPermanent` for "return target spell or nonland permanent…" cards (Press the Enemy). To cap a follow-up free-cast at the bounced object's mana value, capture it first with `Effects.StoreNumber("mv", DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.ManaValue))` before bouncing, then read it via `DynamicAmount.VariableReference("mv")`.

### Combat-shape & misc

- `PreventDamageEffect(target, recipientGroup, amount, direction, scope, sourceFilter, onPrevented, gainLifeFromColors, duration, nextInstanceOnly)` — prevention shield. `amount = null` prevents all. **`recipientGroup: GroupFilter?`** is the recipient-side analogue of `sourceFilter = FromGroup(...)` (which filters the *source* of damage): when set, the shield protects **every** permanent matching the group instead of a single `target` — "prevent all damage that would be dealt to creatures you control this turn" (Summon: Alexander = `Effects.PreventAllDamageToGroup(GroupFilter.AllCreaturesYouControl)`). The group is re-evaluated against projected state at the moment each damage instance would be dealt, with the shield's controller as the "you" reference, so permanents that come under your control later in the turn are protected too; it covers both combat and noncombat damage, and honours `scope` (pass `PreventionScope.CombatOnly` for "prevent all combat damage to creatures you control") and `duration`. Players are not creatures, so a "creatures you control" shield never protects the player. Facade: `Effects.PreventAllDamageToGroup(group, scope = AllDamage, duration = EndOfTurn)`. The next "prevent all damage to artifacts / to each opponent's creatures" card needs only a different `GroupFilter`, not a new effect. `sourceFilter` can be `ChosenSource` (player picks any source on resolution), `ChosenColoredSource` (player picks a source on resolution, but only colored sources are offered — "a source of your choice that shares a color with the mana spent"; a colorless source qualifies for nothing, so it's never offered — Protective Sphere), or `ChosenSourceMatching(filter)` (player picks a source, but only sources matching the `GameObjectFilter` are offered — the parameterized "a [quality] source of your choice" form; `ChosenSourceMatching(GameObjectFilter.Artifact)` is Circle of Protection: Artifacts, and a future "an enchantment/red/… source of your choice" Circle reuses the same variant with a different filter). `nextInstanceOnly` (default false) is **orthogonal to the eligibility filter**: with `amount = null`, `true` prevents only the *next whole damage instance* from the chosen source then consumes the shield (the Circle of Protection family), while `false` prevents *all* damage from that source for the `duration` (Samite Ministration). Facade `Effects.PreventNextDamageFromChosenArtifactSource(target)` sets `ChosenSourceMatching(Artifact)` + `nextInstanceOnly = true`. `onPrevented: Effect?` is an **arbitrary follow-up effect** run when a single-instance `ChosenSource` shield prevents an instance of damage (see below). `gainLifeFromColors: Set<Color>` makes the shield's controller gain that much life whenever it prevents damage from a source of one of those colors (Samite Ministration). Facades: `Effects.PreventNextDamage`, `Effects.PreventAllCombatDamageTo(target, duration = EndOfTurn)` ("prevent all combat damage that would be dealt to it this turn" — Fleeting Flight), `Effects.PreventNextDamageFromChosenSource(amount, target)`, `Effects.PreventNextDamageFromChosenSource(onPrevented)`, `Effects.PreventNextDamageFromChosenArtifactSource(target)`, `Effects.PreventAllDamageFromChosenSource(target, gainLifeFromColors)`, `Effects.PreventAllDamageFromChosenColoredSource(target)`, `Effects.DeflectNextDamageFromChosenSource()`, `Effects.ReflectNextDamageFromChosenSourceToController()`. The `preventDamage` flag (default true) — when **false**, the chosen-source shield does NOT prevent the damage (it still hits in full) but still fires its `onPrevented` reaction with the captured amount; this is the "instead it still deals that damage to you AND deals that much to its controller" shape (Eye for an Eye), as opposed to the deflect/prevent shape (Deflecting Palm).
  - **Prevent-and-react (`onPrevented`)** — instead of a bespoke reaction type, the chosen-source shield runs **any composed effect** when it fires, as a real triggered ability on the stack ("When damage is prevented this way, …", CR-faithful — opponents get priority and can respond). Mechanically: on resolution the shield is created **and** a linked event-based delayed triggered ability (`CreateDelayedTriggerEffect`-style) whose `effect` is `onPrevented`; when the shield prevents an instance it emits an internal `DamagePreventedEvent` that fires only that delayed trigger (matched by id). Inside the trigger the prevented amount is `DynamicAmounts.preventedDamage()` ("that much"/"that many") and the prevented source's controller is `EffectTarget.ControllerOfTriggeringEntity` ("that source's controller") — the same pair Tephraderm uses. So Deflecting Palm's `onPrevented` = `DealDamage(ControllerOfTriggeringEntity, preventedDamage())`; New Way Forward's = `Composite(DealDamage(ControllerOfTriggeringEntity, preventedDamage()), DrawCards(preventedDamage()))`. Because the payoff is a normal stack ability, it may be interactive (targets, replacements) like any other.
- `RedirectNextDamageEffect(protectedTargets, redirectTo, amount, scope)` — redirection shield (CR 614.9):
  while active, damage that would be dealt to any of `protectedTargets` this turn is dealt to `redirectTo`
  instead. Installed as a `Duration.EndOfTurn` floating effect and checked during damage resolution.
  `amount` caps the redirected damage (`null` = redirect all). **`scope` decides when an unlimited shield is
  used up** (a capacity shield, `amount != null`, is always used up once its capacity hits 0 — CR 615.7):
  - `RedirectScope.NEXT_INSTANCE` (default) — one source's instance, then gone. "The next time a source of
    your choice would deal damage to you…" (Beacon of Destiny).
  - `RedirectScope.NEXT_BATCH` — every instance dealt in the next *simultaneous moment*, then gone. Because
    all combat damage is dealt simultaneously (CR 510.2), this redirects a whole combat damage step — every
    attacker hitting the protected player/creature — not just the first. "The next time damage would be dealt
    to X and/or you…" (Glarecaster). The shield survives the per-assignment apply loop (`inBatch=true`) and is
    consumed once afterwards by `CombatDamageManager.consumeBatchRedirectShields`.
  - `RedirectScope.CONTINUOUS` — never self-consumed; redirects every instance until end of turn.
    "All damage that would be dealt to it this turn…" (Karona's Zealot).
  Distinct from the static `RedirectDamage` replacement (continuous, source-keyed) listed under Replacement
  effects — this one is a one-shot/duration shield generated by a resolving ability.
- `Effects.BecomeCreature(target, power, toughness, keywords, creatureTypes, removeTypes, addTypes, colors, imageUri, duration, dynamicPower = null, dynamicToughness = null)`
  (`BecomeCreatureEffect`) — animate / "becomes a creature." Adds CREATURE (Layer 4, keeping the permanent's
  existing types — so "it's still a land"), *sets* creature subtypes to `creatureTypes` (replacing all others),
  removes `removeTypes`, grants additional card types via `addTypes` (e.g. `setOf("ARTIFACT")` for **Mishra's
  Factory** — "becomes a 2/2 Assembly-Worker artifact creature. It's still a land"), *sets* `colors` (Layer 5,
  replacing all others; `null` = keep), grants `keywords` (Layer 6), and sets base P/T (Layer 7b set
  values). **`power`/`toughness` are `DynamicAmount`** (evaluated once at resolution, CR 613.4c, then
  stamped as a fixed set-P/T floating effect); an `Int` convenience overload wraps them in
  `DynamicAmount.Fixed`. Optional **`imageUri`** is a *display-only* card-art override shown for the
  animated permanent while the effect lasts (e.g. a token's art for "becomes a Fractal") — it changes
  no characteristic (stored as a `SerializableModification.OverrideImage` floating effect that maps to
  `NoOp`, read directly by `ClientStateTransformer` via `GameState.imageOverrideFor`) and reverts with
  the rest of the animate at cleanup; `null` keeps the permanent's own art. The optional
  `dynamicPower`/`dynamicToughness` (`DynamicAmount`, supplied together) instead make the Layer 7b base-P/T a
  *dynamic* `SetPowerToughnessDynamic` recomputed at projection rather than locked in at resolution — the
  single-target companion to `MassAnimateEffect`. Facade
  `Effects.BecomeCreatureWithManaValueStats(target, addTypes, keywords, creatureTypes, duration)` wires P/T = the
  animated permanent's own mana value (`EntityProperty(AffectedEntity, ManaValue)`) for **Xenic Poltergeist**
  ("Until your next upkeep, target noncreature artifact becomes an artifact creature with power and toughness
  each equal to its mana value"). Sarkhan's "4/4 Dragon" (fixed); Fractalize's "green and blue Fractal with base
  power and toughness each equal to X plus 1, losing all other colors and creature types"
  (`power = toughness = Add(XValue, Fixed(1))`, `creatureTypes = {"Fractal"}`, `colors = {GREEN, BLUE}`,
  `imageUri =` the Fractal token's Scryfall art).
- `BecomeArtifactEffect(target, cardTypes = {"ARTIFACT"}, subtypes, colors = emptySet(), loseAllAbilities = true, grantedAbility?, duration = Permanent)` — the general "becomes a Treasure/Food/Clue/artifact" transform: stacks continuous floating effects on `target` — Layer 4 `SetCardTypes` (replaces *all* card types) + `SetAllSubtypes` (replaces *all* subtypes), Layer 5 color (`emptySet()` = colorless), Layer 6 `RemoveAllAbilities` when `loseAllAbilities` — plus an optional single `grantedAbility` recorded durably in `grantedActivatedAbilities` (so it survives the ability wipe; the enumerators read it after the projected `lostAllAbilities` check). `Duration.Permanent` ends only when the permanent leaves the battlefield. Differs from `BecomeCreatureEffect` (which *adds* CREATURE + sets P/T): this fully replaces types/subtypes so the result is exactly the named artifact. `cardTypes = null` **keeps** the permanent's card types unchanged (only subtypes/color/abilities are touched) — used when a land "loses all land types and abilities" but stays a land and keeps any other card types (Ultima, Origin of Oblivion: `cardTypes = null, subtypes = emptySet(), colors = null, grantedAbility = {T}: Add {C}, duration = Durations.whileAffectedHasCounter(Counters.BLIGHT)`). (Vraska, the Silencer: a dead opponent's creature returns as a bare colorless Treasure with the sac-for-mana ability.)
- `BecomeSaddledEffect(target = Self)` (facade `Effects.BecomeSaddled()`) — target permanent becomes saddled until end of turn (CR 702.171b). The resolving half of a Saddle ability: stamps the transient `SaddledComponent` marker (cleared at end of turn / on leaving the battlefield; not copiable) and emits `BecameSaddledEvent`. No P/T or type change — read the marker with `Conditions.SourceIsSaddled` / `.saddled()`.
- `BecomePreparedEffect(target = Self)` (facade `Effects.BecomePrepared()`) — target permanent becomes prepared (Secrets of Strixhaven). The target must be a `CardLayout.PREPARE` permanent on the battlefield; becoming prepared creates a castable copy of its prepare spell in exile (shared `PreparationLogic.makePrepared`, the same path used when a `Keyword.PREPARED` creature enters prepared). A creature already prepared, not on the battlefield, or not a preparation card does nothing. Used by Leech Collector ("Whenever you gain life for the first time each turn, this creature becomes prepared").
- `UnprepareEffect(target = Self)` (facade `Effects.Unprepare()`) — target permanent **becomes unprepared** (Secrets of Strixhaven), the inverse of `BecomePrepared`. Strips the target's `PreparedComponent` and removes the cast-from-exile permission for its exile prepare-spell copy; the now-orphaned copy is swept by the `PhantomCardCopiesCheck` state-based action (which removes prepare-spell copies whose source is no longer prepared). No-op if the target isn't prepared. Used by Biblioplex Tomekeeper ("Target creature becomes unprepared").
- `EachPermanentBecomesCopyOfTargetEffect(target, filter, duration, excludeTarget, affected, sourceFromAnyZone)` — mass copy (Mirrorform, Naga Fleshcrafter renew). Facade `Effects.EachPermanentBecomesCopyOfTarget(...)`. Copies copiable values only (Rule 707) — counters, tapped state, attached auras/equipment and non-copy modifiers stay put. `duration = Duration.Permanent` (default) bakes the copy into base state for good; `Duration.EndOfTurn` makes a temporary copy reverted by the end-of-turn cleanup; `Duration.UntilNextEndStep` reverts one step earlier — on **entry to the next end step** (`RevertCopyAtNextEndStepComponent`, processed by `CleanupPhaseManager.performNextEndStepExpiry`) — so it lines up with a paired "return it at the beginning of the next end step" delayed trigger (Niko, Light of Hope: "Shards you control become copies of it until the next end step"). Each temporary copy restores its pre-copy `CardComponent` from its `CopyOfComponent` snapshot. `sourceFromAnyZone = true` lets the copy **source** (`target`) live off the battlefield — its copiable characteristics are read wherever it is, e.g. a card in exile (Lazav, Familiar Stranger; Niko reads the just-exiled creature). `excludeTarget = true` keeps the copy **source** out of the affected set, for "each **other** … becomes a copy of that …" wordings where the target keeps its own identity (and any counter just placed on it). `affected` (an `EffectTarget`, e.g. a second `ContextTarget`) switches to the single-permanent "target permanent A becomes a copy of target permanent B" shape (Fleeting Reflection: "target creature you control … becomes a copy of up to one **other** target creature") — only that one resolved permanent becomes a copy of `target`, and `filter`/`excludeTarget` are ignored; if `affected` resolves to nothing (optional target omitted) the effect is a no-op.
- `BecomeCopyOfLinkedExileEffect(affected = AttachedToTriggeringPermanent)` — facade `Effects.BecomeCopyOfLinkedExile(affected)`. The `affected` permanent becomes a copy of the first creature card in the effect's **source's linked exile** (`LinkedExileComponent` — the card the source banished via `Effects.ExileUntilLeaves`), copiable values only (Rule 707.2). Baked into the affected permanent's `CardComponent` like Clone, but tagged with a `CopyWhileAttachedComponent(sourceId)`; the `AttachedCopyExpiryCheck` state-based action reverts the copy (restoring the pre-copy snapshot) the moment the source stops being attached to it — detach, re-attach elsewhere, or source leaving (CR 611.2b, one-way). No-op when the source's linked exile holds no creature card. Used by Assimilation Aegis ("for as long as this Equipment remains attached to it, that creature becomes a copy of a creature card exiled with this Equipment").
- `AnimateLandEffect(target, subtypes, keywords, duration)` — land becomes a creature.
- `MassAnimateEffect(filter, power, toughness, loseAllAbilities = true, duration = EndOfTurn)` — facade `Effects.MassAnimate(filter, power, toughness, loseAllAbilities, duration)`. One-shot: animate **every** permanent matching `filter` into a creature for `duration`, setting each one's base power and toughness to the `power`/`toughness` **`DynamicAmount`s** — resolved per affected permanent (Layer 7b `SetPowerToughnessDynamic`), so `EntityProperty(AffectedEntity, ManaValue)` gives "each equal to its own mana value" — and, when `loseAllAbilities`, stripping all of its abilities (Layer 6 `RemoveAllAbilities`); Layer 4 `AddType("CREATURE")` makes them creatures. The affected set is captured **once** at resolution against the current battlefield (CR 611.2c) and locked in for the duration. This is the fixed-set, one-shot companion to expressing the same effect *continuously* via the `GrantCardType` + `LoseAllAbilities` + `SetBasePowerToughnessDynamicStatic` group statics on a permanent (which take the same `DynamicAmount` P/T) — use the statics for the while-on-battlefield behavior and this effect for the "this effect continues until end of turn" linger when the generating permanent leaves. Used by **Titania's Song** ("Each noncreature artifact loses all abilities and becomes an artifact creature with power and toughness each equal to its mana value. If this enchantment leaves the battlefield, this effect continues until end of turn.") — a `LeavesBattlefield(SELF)` trigger replays the static set as until-EOT floating effects with `power = toughness = EntityProperty(AffectedEntity, ManaValue)`. The dynamic-P/T floating effect resolves its controller from the effect's captured controller (`ContinuousEffect.controllerId`) when the source has already left the battlefield.
- `ExploreEffect(target)` — Explore mechanic (reveal top; land → battlefield, else hand + counter).
- `MakePreparedEffect(target = Self)` — facade `Effects.MakePrepared(target)`. The target permanent **becomes prepared** (Prepare — Secrets of Strixhaven): gains the prepared status and gets a castable copy of its card's prepare spell (`cardFaces[0]`) in its controller's exile (same machinery as entering prepared, shared via `PreparedService.makePrepared`). No-op if already prepared, or the card has no prepare face. Use for cards that *become* prepared mid-game (e.g. Joined Researchers' end-step trigger); cards that "enter prepared" use `Keyword.PREPARED` on a `PREPARE`-layout card instead.
- `AttachEquipmentEffect(equip, target)` — attach an Equipment. Facade `Effects.AttachEquipment(...)`.
  `Effects.AttachTargetEquipmentToCreature(equipmentTarget, creatureTarget)` force-attaches one
  *targeted* Equipment to one *targeted* creature (both are explicit targets, not the source) — used
  by Stolen Uniform's "Attach it to the chosen creature".
- `UnattachEquipmentEffect(target = Self)` — facade `Effects.UnattachEquipment(target)`. The inverse of
  the attach effects: **unattach** an Aura/Equipment from its host *without moving zones* (CR 701.3d) —
  clears the attachment's `AttachedToComponent` and drops it from the host's attachment list, emitting
  `PermanentUnattachedEvent`. A no-op (no event) when `target` isn't currently attached to anything.
  `target` is usually `EffectTarget.TriggeringEntity` ("that Equipment" inside a delayed trigger) or
  `EffectTarget.Self`. Used by Stolen Uniform's "when you lose control of that Equipment … unattach it".
- `TapUntapEffect(target, isTap)` — tap or untap. Facade: `Effects.Tap` / `Effects.Untap`.
- `Effects.TapEachTarget()` — "tap up to N target creatures": taps every object chosen as a target.
  Composes `ForEachTargetEffect` over `Effects.Tap(ContextTarget(0))`, so the count lives only on the
  spell's `TargetCreature`/`TargetPermanent` (`count`, `unlimited`, or `dynamicMaxCount`) — never
  duplicated on the effect. For "tap X target creatures" use `dynamicMaxCount = DynamicAmount.XValue`
  on the target (Icy Blast); for a fixed cap use `count = N` (Tidal Surge, Choking Tethers, Eddymurk
  Crab). Do **not** pass a magic `count = 20` to mean "any number" — use `unlimited`/`dynamicMaxCount`.
- `Effects.UntapEachTarget()` — the untap twin of `TapEachTarget`: untaps every object chosen as a
  target ("untap each of those creatures"). Composes `ForEachTargetEffect` over
  `Effects.Untap(ContextTarget(0))`, with the count owned by the spell's target requirement.
- `UnlockDoorEffect(target = ContextTarget(0))` — facade `Effects.UnlockDoor(target)`. The resolution-time
  **"unlock a door"** instruction (CR 709.5f): gives a locked half of the targeted Room the "unlocked"
  designation. Distinct from the *unlock cost* special action (CR 709.5e, `ModifyUnlockCost` below) — the
  resolving controller pays nothing. Routes through the same shared `RoomDoorUnlocker` the special action uses,
  so it emits the identical `DoorUnlockedEvent`/`RoomFullyUnlockedEvent` and a face's "When you unlock this
  door" trigger (`Triggers.OnDoorUnlocked`, CR 709.5h) fires either way. Pair with an **up-to-one** target
  Room restricted to one with a locked door —
  `TargetObject(optional = true, filter = TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.ROOM).youControl()).hasLockedDoor())`
  — so a fully-unlocked Room is never a legal target and the controller may choose no target. If the Room has
  more than one locked door (it entered without being cast, CR 709.5d), the controller is prompted for which
  door to unlock (CR 709.5f). Used by Ghostly Keybearer.
- `LockDoorEffect(target = ContextTarget(0))` — facade `Effects.LockDoor(target)`. The resolution-time
  **"lock a door"** instruction (CR 709.5g), the twin of `UnlockDoorEffect`: removes a chosen unlocked half's
  "unlocked" designation, turning that half's name/cost/text off via projection. Unlike unlocking, locking is
  **not a trigger source** (there is no "when you lock this door" ability) and can never *fully unlock* a Room —
  so it emits only a `DoorLockedEvent` (game-log/animation), never the `DoorUnlockedEvent`/`RoomFullyUnlockedEvent`
  family. (That asymmetry is why lock and unlock are two separate effects, not one `lock: Boolean` flag.) When the
  targeted Room has more than one unlocked door (a fully-unlocked Room) the controller is prompted for which door
  to lock; with one it is locked directly; with none it is a harmless no-op. Both door effects share
  `RoomDoorResolution`, which raises the door-choice `ChooseOptionDecision` only when more than one door is eligible.
- `Effects.LockOrUnlockDoor(target = ContextTarget(0))` — **"lock or unlock a door of target Room"**
  (Keys to the House). A resolution-time `ModalEffect.chooseOne(LockDoor, UnlockDoor, countsAsModalSpell = false)`
  (modeled exactly like `Effects.Endure` — no new modal machinery, not a printed modal spell): the controller
  chooses lock or unlock as it resolves, then the chosen door effect runs against the same outer `target`. Pair
  with a single **"target Room you control"** `TargetObject` (no locked/unlocked restriction — either choice can
  always do something on a Room you control).
- `PhaseOutEffect(target = Self)` — phase the target permanent out (Rule 702.26); facade `Effects.PhaseOut(target)`. While phased out it's treated as though it doesn't exist (excluded from `getBattlefield`, so from projection, triggers, combat, targeting, and SBAs) and phases back in before its controller's next untap step. Indirect phasing (attached Auras/Equipment) is handled automatically. Used as the `suffer` branch of a pay-or-phase trigger (Vaporous Djinn: "phases out unless you pay {U}{U}" = `PayOrSufferEffect(Costs.pay.Mana(...), Effects.PhaseOut())`), or as the reaction of a "becomes the target of a spell, it phases out" trigger (King of the Oathbreakers = `Triggers.BecomesTargetOfSpell(...)` + `Effects.PhaseOut(EffectTarget.TriggeringEntity)`). The matching phase-in moment is the `Triggers.PhasesIn(filter?)` trigger (see Triggers below).
- `PhaseOutUntilLeavesEffect(target, tapOnPhaseIn)` / `Effects.PhaseOutUntilLeaves(target, tapOnPhaseIn)` — phase the target out **indefinitely, linked to the effect's source** (the phasing analogue of `ExileUntilLeaves`): it skips its untap-step phase-in and stays out until the source leaves the battlefield. Pair with `Effects.PhaseInLinkedToSource()` on the source's `LeavesBattlefield` trigger, which phases everything the source phased out this way back in (tapping those flagged `tapOnPhaseIn`). The link lives on the phased-out permanent (`PhasedOutComponent.phaseInOnSourceLeaves`); indirect phasing carries Auras/Equipment out with the creature. This is Oubliette (ETB phase-out + leaves-trigger phase-in, tapped).
- `MarkExileOnDeathEffect(target)` — replace next "to graveyard" with "to exile".
- `Effects.AddCombatPhase` / `Effects.AddMainPhase` — the two **atomic extra-phase** effects
  (CR 500.8: extra phases are added after the specified phase). `AddCombatPhase` queues *one combat
  phase and nothing else* — "After this phase, there is an additional combat phase" (Aurelia, the
  Warleader / Combat Celebrant / Fear of Missing Out / Great Train Heist / Raph & Leo / Éomer);
  `AddMainPhase` queues one extra postcombat main phase (CR 505.1a: every main phase after the first
  is a postcombat main phase). **Compose them** —
  `Effects.Composite(listOf(Effects.AddCombatPhase, Effects.AddMainPhase))` — to reproduce "an
  additional combat phase followed by an additional main phase" (Aggravated Assault, All-Out
  Assault); the combat atom alone adds *no* trailing main phase. Implemented as an ordered
  `AdditionalPhasesComponent(phases: List<QueuedPhase>)` queue on the active player (each `QueuedPhase`
  is a `COMBAT` / `MAIN` kind plus, for a combat phase, an optional attacker-restriction filter),
  drained one at a time by `TurnManager.advanceStep` after the postcombat main phase and, for an
  inserted combat phase, again at its end-of-combat step (marked by `InAdditionalCombatPhaseComponent`)
  so a combat-only extra phase proceeds straight to the end step instead of granting an unwanted main
  phase. Engine simplification: all queued phases are inserted after the postcombat main phase
  regardless of when the effect resolved.
- `Effects.AddCombatPhaseRestrictedTo(attackerRestriction: GameObjectFilter)` — the same atomic extra
  combat phase, but **only creatures matching `attackerRestriction` may be declared as attackers
  during that inserted phase** (CR 508.1c; Bumi, Unleashed: "there is an additional combat phase. Only
  land creatures can attack during that combat phase" ⇒
  `AddCombatPhaseRestrictedTo(GameObjectFilter.Creature and GameObjectFilter.Land)`). The filter rides
  on the `QueuedPhase` and is copied onto `InAdditionalCombatPhaseComponent` when the phase begins, so
  it is scoped to exactly that phase (the natural combat phase and any unrestricted extra combat impose
  nothing). Enforced by `AdditionalCombatPhaseAttackerRule` in `defaultAttackRestrictionRules()`,
  matched against projected state so animated lands read as the land *creatures* they are. Reused by
  Aang, Destined Savior and Bumi, King of Three Trials' sibling. (A Kotlin property and function can't
  share a name, so the unrestricted atom stays the `Effects.AddCombatPhase` value and the restricted
  variant is this factory; both build the one `AddCombatPhaseEffect(attackerRestriction: GameObjectFilter?)`.)
- `Effects.AddAdditionalUpkeepSteps(amount)` (`amount: DynamicAmount` or `Int`) — give the
  controller `amount` additional upkeep steps after the current phase (Obeka, Splitter of Seconds:
  "you get that many additional upkeep steps after this phase"). Per CR 500.10, each added upkeep
  step creates the *beginning phase* that normally contains it, with the untap and draw steps
  skipped; per CR 500.8 the phases are inserted after the current phase, and after any additional
  combat phases added to the same point (most-recently-created phase first). "At the beginning of
  [your] upkeep" abilities trigger in each inserted step (CR 503.1a). Steps are always added to the
  controller's own turn (CR 500.10a). Implemented by accumulating an `AdditionalUpkeepStepsComponent`
  count on the active player, drained by `TurnManager.advanceStep` after the postcombat main phase
  (after the additional-combat-phase check), each remaining count redirecting into a fresh upkeep
  step in the beginning phase. Read "that many" from the triggering combat damage with
  `DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)`.
- `Effects.AddAdditionalEndSteps(amount)` (`amount: DynamicAmount` or `Int`, default `1`) — insert
  `amount` additional end step(s) directly after the current end step (Y'shtola Rhul: "there is an
  additional end step after this step"). Per CR 500.9 a step is inserted directly after the
  specified step; each added step is a full end step (CR 513) where the active player gets priority
  and every "at the beginning of the end step" ability triggers again. Steps are always added to the
  controller's own turn (CR 500.10a). Implemented by accumulating an `AdditionalEndStepsComponent`
  count on the active player, drained by `TurnManager.advanceStep` when leaving the end step — each
  remaining count redirects back into a fresh end step instead of advancing to the cleanup step. A
  rider that adds an end step *and re-triggers in it* must guard against looping with
  `Conditions.IsFirstEndStepOfTurn` (see Conditions below) — Y'shtola only spawns the extra end step
  during the turn's first (natural) end step.
- `GatedEffect(gate, then, otherwise?, decisionMaker?)` — **the unified resolution frame for the
  optional / gated-effect cluster** (phase-rs Lesson 1). A `Gate` decides whether `then` runs; if it
  fails, `otherwise` runs. One executor + one continuation/resumer own the canonical unwind order, so
  targets on `then`/`otherwise` lock at trigger time (CR 603.3d) and the gate is resolved at
  resolution time (CR 117.3a) by `decisionMaker` (defaults to the controller) — the may-vs-target
  timing is correct by construction rather than re-encoded per wrapper. Gates:
  - `Gate.MayDecide(prompt?, hint?, sourceRequiredZone?, inlineOnTrigger?, feasibility?)` — pure yes/no
    ("You may [then]."). Replaces `MayEffect` (see the `MayEffect` facade below). `sourceRequiredZone`
    skips the gate silently when the source has left that zone by resolution; `inlineOnTrigger`
    renders the yes/no on the triggering permanent rather than as a modal. `feasibility` (a
    `FeasibilityCheck`) — when set and **unmet** at resolution, the may-action is impossible, so the
    player "doesn't": the prompt is skipped and `otherwise` runs directly. Lets "you may sacrifice an
    artifact. If you don't, …" apply its else automatically when the controller has no artifact (the
    no-target analogue of a targeted "may" with no legal targets falling to its else branch).
  - `Gate.MayPay(cost)` — "You may [cost]. If you do, [then]." `cost` is a cost **effect**
    (`PayManaCostEffect`, `PayDynamicManaCostEffect`, `PayLifeEffect`, `SacrificeEffect`, or a
    `CompositeEffect` of them). An unaffordable cost (fixed mana, dynamic mana, and life are
    recognized — the dynamic-mana amount is checked against its own `payer`; other shapes are
    assumed payable) skips the prompt straight to `otherwise`. On "yes", the cost is paid then `then`
    runs (`stopOnError`: an unpayable cost aborts the payoff). For a recognized mana cost the "yes"
    button is labeled with the concrete amount — a dynamic cost shows its computed total ("Pay {8}"),
    not the formula. When the cost is a
    `PayDynamicManaCostEffect` with a non-default `payer` (e.g. the "each player's upkeep, that player
    may pay …" shape — Magnetic Mountain), set `decisionMaker` to that same player so the one who is
    charged is the one prompted; affordability is already gauged against the `payer` regardless.
  - `Gate.WhenCondition(condition)` — **not a decision, a state test.** Succeeds iff `condition`
    holds at resolution; no prompt, no pause — `then`/`otherwise` run synchronously in the executor.
    The condition evaluates through the shared `ConditionEvaluationContext` (identical at resolution
    and projection). Replaces `ConditionalEffect` (see "Sequencing & conditional" below).
  - `Gate.DoAction(action, successCriterion?)` — **not a decision, an *action-outcome* test.**
    `action` is performed (it may itself pause for sub-decisions); once it has fully drained,
    `successCriterion` scores it against a pre-action snapshot to decide whether it "happened" —
    success → `then`, failure → `otherwise`. This is "[action]. If you do, [then]" (a declined or
    no-op action runs `otherwise`, not `then`), distinct from `MayDecide` (gates on the yes/no) and
    `MayPay` (gates on paying a cost). `successCriterion` defaults to `SuccessCriterion.Auto`, which
    infers success from the action's terminal zone-move (a pipeline `MoveCollection` to a zone, or a
    single `MoveToZone` of the source itself) growing its destination zone. **Auto is only legal on
    shapes it can infer** (`SuccessCriterion.Auto.canInfer`): card-load validation (`CardValidator`,
    enforced corpus-wide by `SuccessCriterionValidationTest`) rejects an Auto criterion on any other
    action — non-zone-move actions (deal damage, gain life, …) must state `SuccessCriterion.Always`,
    `CollectionNonEmpty(name, min)`, or `DamageDealt(recipient)` explicitly instead of silently
    inheriting a fail-open "it happened". Auto's zone-grew probe is also **wrong for zone-move
    actions that succeed vacuously**: "discard your hand" discards zero cards from an empty hand yet
    still counts as performed (Narset, Jeskai Waymaster ruling 2025-04-04 — "You may choose to
    discard your hand even if your hand contains zero cards"), so gate it with
    `SuccessCriterion.Always`, never Auto (Vaultguard Trooper, Narset, Sauron the Dark Lord). `SuccessCriterion.DamageDealt(recipient = Controller|Any)`
    gates on whether the action **actually dealt damage** from the effect's source (a positive-amount
    `DamageDealtEvent` sourced from it) — damage that was fully prevented or replaced (a Circle of
    Protection, a prevention shield, redirection) emits no such event and counts as "didn't happen".
    `DamageRecipient.Controller` (the default) requires the damage to have reached *you*; `.Any`
    accepts any recipient. Mishra's War Machine: "deals 3 damage to you unless you discard a card. If
    it deals damage to you this way, tap it" → `IfYouDoEffect(PayOrSuffer(discard, DealDamage(3,
    Controller, source=Self)), Tap(Self), successCriterion = DamageDealt(Controller))`.
    `SuccessCriterion.ControlChanged` gates on whether the action **actually changed control** of a
    permanent (a `ControlChangedEvent` whose old and new controllers differ) — a control change that
    never happened (the targeted permanent left the battlefield / is no longer controlled by the donor
    at resolution) emits no such event and counts as "didn't happen". Stiltzkin, Moogle Merchant:
    "{2}, {T}: Target opponent gains control of another target permanent you control. If they do, you
    draw a card" → `IfYouDoEffect(GiveControlToTargetPlayer(permanent, opponent), DrawCards(1),
    successCriterion = ControlChanged)`.
    `CollectionNonEmpty` gates on the action's actual pipeline collection
    (`storedCollections[name].size >= min` after the action runs) — the collections propagate onto
    the gate frame via `exposeCollectionsToNextFrame`, in both the synchronous and the
    paused/continuation-drain paths. The executor pre-pushes a `GatedActionContinuation` so a paused
    action auto-resumes and evaluates after its own continuations drain. Replaces `IfYouDoEffect`
    (see "Sequencing & conditional" below).
  - `Gate.MayPayX` — **not a yes/no, a number chooser.** "You may pay {X}. If you do, [then]." The
    decision-maker is prompted for a number 0..(most generic mana they can produce); paying X > 0
    succeeds → `then` runs with the chosen X bound into the context (read via `DynamicAmount.XValue`),
    X = 0 declines → `otherwise`. An unaffordable gate (no mana) is skipped silently. A parameterless
    `data object` (the {X} cost is implicit). The executor builds a `ChooseNumberDecision` and reuses
    the existing `MayPayXContinuation`/`resumeMayPayX` to auto-tap and bind X. Replaces
    `MayPayXForEffect` (see "Optional & gated" below).
  - The multi-player APNAP `AnyPlayerMayPayEffect` stays a **standalone effect**, not a gate — a
    single `decisionMaker` can't express its turn-order loop (see below).
- `MayEffect(effect, descriptionOverride?, sourceRequiredZone?, inlineOnTrigger?, hint?, decisionMaker?, otherwise?)`
  — "You may [effect]." Facade preserved for existing cards; it now **lowers to
  `GatedEffect(Gate.MayDecide(...), then = effect, otherwise = otherwise, decisionMaker = decisionMaker)`**
  (compiled form is `Gated`, no distinct `May` type or executor). The may-vs-target trigger reorder —
  for a "may" ability that *also* targets, the yes/no is asked *before* target selection (Invigorating
  Boon) — recognizes the lowered shape via the `Effect.asMayDecide()` matcher (a bare `Gate.MayDecide`
  with no `otherwise`).
  - **`decisionMaker` routes the yes/no to a non-controller** — pass any player `EffectTarget`
    (`EffectTarget.TargetController` for "that creature's controller may …", or a bound target such
    as `target("target opponent", Targets.Opponent)` for "**target opponent may …**"). Only the
    yes/no prompt is delegated; the `then`/`otherwise` effects still resolve from the controller's
    perspective unless they themselves target a specific player.
  - **`otherwise` is the "if that player doesn't" branch** — runs iff the chooser declines. Combine
    with a delegated `decisionMaker` for "target opponent may [then]; if that player doesn't,
    [otherwise]" (Palantír of Orthanc: "target opponent may have you draw a card; if that player
    doesn't, you mill X cards and that player loses life equal to their total mana value").
- `OptionalCostEffect(cost, ifPaid, ifNotPaid?)` — "You may [cost]. If you do, [ifPaid]." Facade
  preserved for existing cards; it now **lowers to `GatedEffect` with a `Gate.MayPay`** gate (compiled
  form is `Gated`, not a distinct `OptionalCost` type).
- `MayPayManaEffect(cost, effect)` — "You may pay [cost]. If you do, [effect]." Facade preserved for
  existing cards; it now **lowers to `GatedEffect(Gate.MayPay(PayManaCostEffect(cost)), then = effect)`**
  (compiled form is `Gated`, no distinct `MayPayMana` type or executor). The engine recognizes this
  exact shape — a flat mana `Gate.MayPay` with no `otherwise` and the default decision-maker, whether
  authored via `MayPayManaEffect` or `OptionalCostEffect(PayManaCostEffect(...), …)` — and gives it the
  bespoke optional-mana-payment UX rather than the generic gated yes/no: **manual mana-source
  selection** at resolution (a `SelectManaSourcesDecision`, so sources that sacrifice or carry a tap
  sub-cost aren't auto-tapped), and, for a **triggered ability that also requires a target** (the
  Onslaught "Words of …" cycle, Lightning Rift), the deliberate **pay → select-mana → choose-target**
  order so the player isn't asked to pick a target before deciding to pay. Composite-cost, life-gated,
  or `otherwise`-bearing MayPay gates keep the generic auto-tapping path.
- `Effects.UnlessYouWaterbend(amount, otherwise)` — "[otherwise] unless you waterbend {amount}." An
  **in-resolution waterbend payment gate** (Avatar: The Last Airbender). Lowers to
  `GatedEffect(Gate.MayPay(PayManaCostEffect("{amount}", waterbend = true)), then = Composite(), otherwise = otherwise)`.
  The `PayManaCostEffect.waterbend` flag makes the gated executor recognize a waterbend MayPay and,
  instead of the plain "pay?" yes/no + auto-tap, surface a `SelectManaSourcesDecision` that **also lists
  the untapped artifacts/creatures the player may tap to help** (each paying {1}), reusing the shared
  waterbend machinery (`CostEnumerationUtils.findWaterbendPermanents`/`canAffordWithWaterbend`,
  `AlternativePaymentHandler.applyWaterbendForAbility`, `SelectManaSourcesDecision.waterbendPermanents`
  — the same plumbing as Ward—Waterbend). Paying (mana and/or taps) runs `then` (empty — paying is its
  own reward); declining, or being unable to pay, runs `otherwise`. The payment resumes through the
  shared `MayPayManaSelectionContinuation` (now carrying `waterbend`/`otherwise`). Waterbend is
  generic-only, so `amount` carries no colored pips. **Waterbending Lesson**: `Composite(DrawCards(3),
  UnlessYouWaterbend(2, Discard(1)))` — "Draw three cards. Then discard a card unless you waterbend {2}."
- `MayPayXForEffect(effect)` — "You may pay {X}. If you do, [effect]." Facade preserved for existing
  cards; it now **lowers to `GatedEffect(Gate.MayPayX, then = effect)`** (compiled form is `Gated`, no
  distinct `MayPayX` type or executor). Prompts a 0..max-affordable number chooser; paying X auto-taps
  X generic mana and binds the chosen X into `effect`'s context (read via `DynamicAmount.XValue`).
  Decree of Justice's cycling trigger, Hollow Specter's combat-damage trigger.
- `Effects.AnyPlayerMayPay(cost, consequence)` / `Effects.UnlessAnyPlayerPays(cost, effect)` —
  back the single `AnyPlayerMayPayEffect(cost, consequence?, consequenceIfNonePaid?)`, which asks
  each player in APNAP order whether to pay `cost`. The first to pay runs `consequence` and stops
  the loop; if no one pays, `consequenceIfNonePaid` runs. `AnyPlayerMayPay` reads the
  "if a player does, X" direction (Prowling Pangolin); `UnlessAnyPlayerPays` reads the inverse
  "X unless any player pays" direction (Aether Rift: "return it… unless any player pays 5 life").
  Supported costs: `Costs.pay.Sacrifice` (card selection) and `Costs.pay.PayLife` (yes/no). The
  surrounding pipeline's stored collections are carried into whichever consequence fires, so the
  consequence can reference cards gathered earlier in the same resolution (e.g. the discarded card,
  via `MoveCollection(from = "discarded", …)`). The optional `eligiblePlayers: Player` field scopes
  *which* players are offered the choice, relative to the source's controller: `Player.Each`
  (default) asks everyone; `Player.EachOpponent` asks only the controller's opponents — "any
  opponent may sacrifice a creature… if a player does, tap this and put a +1/+1 counter on it"
  (Desecration Demon). APNAP order is preserved within the scoped subset.
- `RepeatWhileEffect(body, repeatCondition)` (facade `Effects.RepeatWhile(body, repeatCondition)`) — do-while loop: run `body` once, then repeat while `repeatCondition` holds. `repeatCondition` is `RepeatCondition.PlayerChooses(decider, prompt)` (a yes/no each iteration) or `RepeatCondition.WhileCondition(condition)` (a game-state `Condition`). A `WhileCondition` is evaluated against the **body's own pipeline outputs from that iteration** (the collections/values it just stored), then the next iteration's body runs from the pristine pre-loop context — so a loop can branch on what it just produced without stale state leaking forward. Models "do X. Repeat this process [if …]" — e.g. **The Tale of Tamiyo** I–III: `RepeatWhile(body = Composite(Patterns.Library.mill(2), ConditionalEffect(CollectionSharesCardType("milled"), DrawCards(1))), repeatCondition = WhileCondition(CollectionSharesCardType("milled")))` mills two, and while the two milled cards share a card type both draws and repeats.

### Sequencing & conditional

- `CompositeEffect(effects)` — run effects in order. Card definitions use the facade
  `Effects.Composite(e1, e2, ...)` (vararg) or `Effects.Composite(effects, stopOnError?,
  descriptionOverride?, descriptionAmounts?)` (list + render options).
- `ForEachEffect(space, body)` — the **single compiled iteration effect**: run `body` once per item
  of a sealed `IterationSpace`. Five lowering facades keep the pre-unification authoring names
  (same precedent as `IfYouDoEffect` → `GatedEffect`); use the one matching the iteration source:
  - `ForEachTargetEffect(effects)` → `IterationSpace.Targets` — per chosen target; the body sees
    only the current target as `ContextTarget(0)`, fresh `storedCollections` (Kaboom!).
  - `ForEachPlayerEffect(players, effects)` → `IterationSpace.Players(players)` — per matching
    player; `controllerId` rebound so `Player.You` is the current player, `opponentId` recomputed,
    fresh `storedCollections` (Winds of Change, Bend or Break, One Ring to Rule Them All).
  - `ForEachInCollectionEffect(collection, effect)` → `IterationSpace.Collection(name)` — per
    entity of a named pipeline collection; `pipeline.iterationTarget` bound so `EffectTarget.Self`
    is the current entity; outer collections preserved (Fight or Flight).
  - `ForEachInGroupEffect(filter, effect, noRegenerate?)` / facade
    `Effects.ForEachInGroup(...)` → `IterationSpace.Group(filter, noRegenerate)` — per battlefield
    permanent matching a group filter; same `iterationTarget` binding as Collection.
  - `ForEachColorOfEffect(source, effect)` / facade `Effects.ForEachColorOf(...)` →
    `IterationSpace.ColorsOf(source)` — per color of an entity, WUBRG order, bound via the
    chosen-color channel (see the choice section).

  Every space snapshots its items before the first iteration (entities destroyed mid-loop stay in
  the list) and every space is **pause-safe**: a body that pauses for a decision resumes the
  remaining iterations via the shared `ForEachContinuation`. Multi-effect bodies lower to a
  `CompositeEffect`.
- `ConditionalEffect(condition, ifTrue, ifFalse?)` / `Branch(...)` — conditional branch. Facade
  preserved for existing cards; it now **lowers to `GatedEffect(Gate.WhenCondition(condition), then =
  ifTrue, otherwise = ifFalse)`** (compiled form is `Gated`, not a distinct `Conditional` type). It is
  a synchronous state test — no decision, no pause. Engine paths that recognize a conditional branch
  (stack-time branch resolution for opponent views, repeat-activation analysis, limited rating) key
  off the lowered `Gate.WhenCondition` shape via the `Effect.asConditional()` matcher.
- `IfYouDoEffect(action, ifYouDo, ifYouDont?, successCriterion?)` — "[action]. If you do, [ifYouDo].
  Otherwise, [ifYouDont]." Gates the payoff on whether `action` actually accomplished its work (a
  declined or no-op action runs `ifYouDont`, not `ifYouDo`) — not on a yes/no decision. Facade
  preserved for existing cards; it now **lowers to `GatedEffect(Gate.DoAction(action,
  successCriterion), then = ifYouDo, otherwise = ifYouDont)`** (compiled form is `Gated`, not a
  distinct `IfYouDo` type or executor). `successCriterion` defaults to `SuccessCriterion.Auto` (infer
  from the action's terminal zone-move); an action shape Auto *can't* infer from is a card-load
  validation error — state `SuccessCriterion.Always` / `CollectionNonEmpty(name, min)` explicitly
  there (and use them whenever the inference is wrong). Wrap with `MayEffect` for the optional
  "You may [action]. If you do, [effect]" shape.
- `ReflexiveTriggerEffect(action, reflexive, optional)` — same shape but the reflexive effect goes on the stack.
  Targets in `reflexiveTargetRequirements` are chosen *after* `action` resolves, against the resolving
  ability's live pipeline. A `TargetObject.dynamicMaxCount` on a reflexive requirement is therefore
  resolved against that pipeline — so "up to **that many** target …" can read a count a preceding action
  stored, e.g. `dynamicMaxCount = DynamicAmount.VariableReference("discarded_count")` paired with
  `Patterns.Hand.discardAnyNumber()` (Miasma Demon: "you may discard any number of cards. When you do, up
  to that many target creatures each get -2/-2"). This differs from the cast-time path
  (`TargetValidator.effectiveMaxCount`), where the pipeline isn't live yet — pipeline-linked caps only
  work on reflexive/resolution-time targets.
- **Branching on gathered properties** — "reveal/look, if it's a [type] do X, otherwise Y" needs no
  bespoke effect type; it is the partition + collection-gate composition:
  1. **Partition:** `FilterCollection(from, CollectionFilter.MatchesFilter(filter), storeMatching,
     storeNonMatching)` splits a gathered collection by any `GameObjectFilter` — deterministic, no
     player decision, no continuation.
  2. **Branch:** gate follow-up effects on the partition with
     `GatedEffect(Gate.WhenCondition(CollectionContainsMatch(name, filter?)))` (or `Not(...)` /
     `ConditionalOnCollectionEffect(name, ifNotEmpty, ifEmpty?, minSize?)`). Gate conditions are
     evaluated against the live `EffectContext`, so they see pipeline collections — **including
     collections written before an earlier pause** (a `SelectFromCollection` decision); the resumed
     pipeline context carries them.
  3. Effects that consume an empty collection (`MoveCollection`, `SelectFromCollection`,
     `AddCountersToCollection`) are silent no-ops, so the "nothing matched" leg often needs no gate
     at all — just move the (possibly empty) partition.
  Worked examples: `Patterns.Mechanic.explore()` (CR 701.44), Sindbad ("draw and reveal; if it isn't
  a land, discard it"), Cache Grab (Food gated on `CollectionContainsMatch("selected", Squirrel)`
  after a selection pause).
  - **Collection conditions** (read a pipeline collection from the live `EffectContext`):
    `Conditions.CollectionContainsMatch(name, filter?)` — true if any card in `name` matches `filter`;
    `Conditions.CollectionSharesCardType(name)` — true if two cards in `name` share a card type (CR
    205.2a; false for fewer than two cards). The latter models "if two cards that share a card type
    were milled this way" (The Tale of Tamiyo I–III), typically inside both a `ConditionalEffect` and
    a `RepeatCondition.WhileCondition` over the mill's `"milled"` collection.

### Modal & choice

- `ModalEffect.chooseOne { mode(...) }` / `ModalEffect.chooseN(n) { ... }` — modal effect block.
- `ModalEffect.chooseOneNotYetChosen(*modes)` — "choose one that hasn't been chosen"; source remembers used modes across the game (Gandalf the Grey).
- `ChooseActionEffect(choices, player = Controller)` — `player` picks from a list of labeled effects; infeasible options (per each `EffectChoice.feasibilityCheck`) are filtered out, and if one remains it auto-runs. `player` may be any `EffectTarget`, including the state-relational `EffectTarget.TargetController` — routing the choice to the controller of the ability's chosen permanent (a "[do X to target permanent] unless its controller [accepts an avoidance]" choice). Combustion Man: "destroy target permanent unless its controller has Combustion Man deal damage to them equal to his power" — `player = TargetController`, with choices `DealDamage(sourcePower(), target = TargetController, damageSource = Self)` and `Destroy(<the permanent>)`.
- `GrantProtectionFromColor(color, target, duration)` — grant protection from a **fixed** color to a target (no player choice); a thin recipe over `GrantKeyword("PROTECTION_FROM_<COLOR>")`. "{W}: Target creature gains protection from red until end of turn." (Crimson Acolyte).
- `GrantPlayerProtection(scope = ProtectionScope.Everything, duration = Duration.UntilYourNextTurn, target = Controller)` — grant a **player** protection from a `ProtectionScope` (CR 702.16); the player-level counterpart of the creature protection statics. For a player only the **D**amage and **T**argeting parts of DEBT apply: a protected player can't be the target of, nor be dealt damage by, a source matching the scope. Adds/merges a `PlayerProtectionComponent` (multiple grants stack their scopes); the targeting validator, target enumerator, and `DamageUtils` all consult the shared `PlayerProtectionRules`. `Duration.UntilYourNextTurn` clears it after the untap step of the player's next turn. "You gain protection from everything until your next turn." (The One Ring).
- `ChooseColorThenEffect(whenChosen)` — pick a color, then run a function of that color.
- `Effects.ChooseNumberThen(then, minValue=0, maxValue=16, prompt)` — pick a number in `[minValue, maxValue]`,
  then run `then` once with the chosen number exposed via the effect context as **X**. Atomic effects and filters
  under `then` read it through `ManaValueEqualsX` (`.manaValueEqualsX()`). Compose with `CompositeEffect` for
  multi-step cards (Void: destroy all artifacts/creatures with that mana value, then a target player reveals their
  hand and discards all nonland cards with that mana value).
- `Effects.ChooseNumberForSource(minValue=0, maxValue=7, slot=ChoiceSlot.CHOSEN_NUMBER, prompt)` — pick a number in
  `[minValue, maxValue]` and **store it durably on the source permanent** under `slot` (a `ChoiceValue.NumberChoice`
  in its `CastChoicesComponent`, replacing any prior value). Unlike `ChooseNumberThen` (transient X for one inner
  effect), the number persists so a characteristic-defining ability can read it for the permanent's whole life via
  `DynamicAmount.CastChoice(slot)`. Re-callable — the **last** chosen value wins. This is the *on-resolution* form
  (run from a triggered/activated ability, e.g. an upkeep re-choice); for the *as-enters* "As ~ enters, choose a
  number" choice use the replacement `EntersWithChoice(ChoiceType.NUMBER, minValue, maxValue)` (§ replacement effects),
  which writes the same `CHOSEN_NUMBER` slot before the permanent is on the battlefield. For a "you **may** choose"
  clause mark the running triggered ability `optional = true` (declining keeps the prior value). Powers Shapeshifter
  (replacement at entry + this effect each upkeep), whose P/T is a
  `SetBasePowerToughnessDynamicStatic(power = CastChoice(CHOSEN_NUMBER), toughness = Subtract(Fixed(7),
  CastChoice(CHOSEN_NUMBER)))` CDA — power = last chosen number, toughness = 7 − it.
- `Effects.ChooseOpponent(prompt)` — the controller picks an opponent, **stored durably on the
  source entity** under `ChoiceSlot.OPPONENT` (a `ChoiceValue.EntityChoice` in its
  `CastChoicesComponent`) and read back through `Player.ChosenOpponent`. Forced (promptless) with a
  single opponent, so 2-player games see no extra decision. The source may be a spell on the stack
  (the choice lives on the spell entity for its resolution) or a permanent (recorded durably).
  `Patterns.Mechanic.giftSpell` prefixes its gift mode with this automatically — gift recipients
  address `Player.ChosenOpponent`. The *as-enters* analogue is `EntersWithChoice(ChoiceType.OPPONENT)`,
  which writes the same slot (Jihad, The Rack).
- `GrantHexproofFromChosenColorEffect(target)` — hexproof from chosen color.
- `GrantProtectionFromChosenColorEffect(target)` — protection from chosen color. Must run inside `ChooseColorThen`; wrap in `ForEachInGroup` for the group case (Akroma's Blessing: "Creatures you control gain protection from the chosen color").
- `Effects.GrantProtectionFromChosenCardType(target, duration)` — "gains protection from the card type of your choice" (Pippin, Guard of the Citadel). The card-type analogue of `GrantProtectionFromChosenColor`, but **self-contained**: its executor owns the choice — it presents a `ChooseOptionDecision` over the fixed protectable card-type set (Artifact, Creature, Enchantment, Instant, Land, Planeswalker, Sorcery, Battle) and, on response, grants a floating `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword for `duration`. The targeting validator, `StackResolver` spell-targeting, `DamageUtils`, the combat-damage pipeline/manager, and a `ProtectionFromCardTypeRule` block-evasion rule all match the protected keyword against the source's projected card types. (The "can't be enchanted/equipped by that type" clause is reminder text and unenforced at attach time, mirroring color/subtype protection.)
- `ChooseCreatureTypeEffect(...)` — pause for creature-type pick.
- `SelectTargetEffect(...)` — have a player pick from a valid set.

> **Authoring rule:** prefer composing primitives over adding parameters to an existing effect. Use `CompositeEffect`
> and the gather/select/move pipeline before writing a new executor.

---

## 5. Effect patterns (`Patterns.Library.*` / `Patterns.Hand.*` / `Patterns.Group.*` / `Patterns.Exile.*` / `Patterns.Sideboard.*` / `Patterns.CreatureType.*` / `Patterns.Mechanic.*`)

Composed pipelines (`GatherCards → SelectFromCollection → MoveCollection` shapes and similar).
Named entries here are for named MTG mechanics and shapes with a demonstrated second user — a
one-off pipeline belongs inline in the card file via `Effects.Pipeline { }` (§5.5) instead.

**Library search & reveal**

- `searchLibrary(filter, destination?, tapped?, shuffle?)` — search library, pick matching, move, shuffle.
- `searchMultipleZones(zones, filter, count?, destination?, tapped?, reveal?)` — search several zones (e.g. library and/or graveyard) in one effect; shuffles automatically if `LIBRARY` is among the zones. Pass `reveal = true` for "reveal it" tutors (Delivery Moogle).

**Sideboard / wish (`Patterns.Sideboard.*`)**

- `wish(filter = Any, count = 1, destination = HAND, revealed = true)` — the **wish** mechanic (Burning Wish,
  Living Wish, Cunning Wish, Death Wish, Glittering Wish, Wish, …): "you may [reveal] a [type] card
  you own from outside the game and put it into your hand." A player's sideboard is modelled as the
  private per-player `Zone.SIDEBOARD` ("outside the game", CR 100.4 / 400.11a; strictly not a zone
  per CR 400.11, but a pseudo-zone lets the wish reuse the ordinary pipeline). The recipe composes
  `GatherCards(FromZone(SIDEBOARD, You, filter)) → SelectFromCollection(ChooseUpTo(count)) →
  MoveCollection(→ destination, revealed = revealed)` — **no shuffle** (the sideboard is unordered).
  `revealed` defaults on per the cycle's "reveal that card" clause (CR 701.20, Reveal); pass
  `revealed = false` for cards that merely "put a card you own from outside the game into your hand"
  with no reveal (**North Wind Avatar**: `Patterns.Sideboard.wish(GameObjectFilter.Any, revealed = false)`).
  The "may" and "a card" are both the `ChooseUpTo(count)`: declining or having no
  legal choice simply moves nothing. The varying axis across the cycle is `filter`
  (`Filters.Sorcery` for Burning Wish, `Filters.Instant` for Cunning Wish, creature-or-land for
  Living Wish, `Any` for Death Wish/Wish); `destination` is `HAND` for every printed wish but is
  parameterized for the rare future "from outside the game onto the battlefield"/Karn-style case.
  Pair with `spell { selfExile() }` for the cards (Burning/Cunning/Living Wish) that exile
  themselves on resolution instead of going to the graveyard (CR 608.2g). The sideboard is private
  to its owner — masked from opponents and spectators by `ClientStateTransformer` (like the library
  and hand) — and, consistent with CR 400.11c, no effect other than a wish should ever gather from
  it. Sideboards are populated at deck-build: an explicit ≤15-card list in constructed (CR 100.4a),
  `pool − maindeck` in Limited (CR 100.4b). Example — **Burning Wish**:
  `spell { selfExile(); effect = Patterns.Sideboard.wish(Filters.Sorcery) }`.

**Top-deck manipulation**

- `scry(count)` — look at top N, bottom any, rest on top. Also `Effects.Scry(count)`.
- `scry(count, target)` — **"Target player scries N"** (`Effects.Scry(count, target)`). Player-scoped
  twin of `scry(count)`: when `target` is the controller it is identical (returns the compact
  `ScryEffect` macro); otherwise it expands to a `scryPipeline` whose gather + library moves read the
  **target** player's library and whose top/bottom decision is made by that player
  (`Chooser.TargetPlayer`) — the scry analogue of `mill(count, target)`. Used by modal "• Target
  player scries N" modes (Bumi, King of Three Trials), where `target` is the chosen mode's local
  `EffectTarget.ContextTarget(0)`.
- `surveil(count)` — look at top N, any to graveyard, rest on top. Also `Effects.Surveil(count)`.
  - **Compact macro effect.** `scry`/`surveil` return a single `ScryEffect`/`SurveilEffect` *marker*
    node (`{"type":"Scry","count":N}`), not the unrolled pipeline. The engine's `ScryExecutor` /
    `SurveilExecutor` expand it to the shared `LibraryPatterns.scryPipeline(N)` /
    `surveilPipeline(N)` (Gather → Select → Move → Move → emit `ScriedEvent`/`SurveiledEvent`) at
    resolution and delegate to `CompositeEffectExecutor` — so the SelectCardsDecision pause and the
    "Whenever you scry/surveil" triggers behave exactly as the expanded pipeline. Collapsing to one
    node keeps the per-card snapshot goldens one line and stops them churning when the shared
    pipeline internals change. Any effect-tree walker that needs the inner nodes expands through the
    single `LibraryPatterns.expandMacro(effect)` helper.
- `mill(count)` — top N cards into graveyard.
- `exileTop(count, target = Controller)` — top N cards of a player's library into exile (Malboro's
  "exiles the top three cards of their library"). Same Gather → Move pipeline as `mill`, destination
  exile. `count` is an `Int` or `DynamicAmount`. Pass a `target` (e.g. a `Player.You` rebind under
  `Effects.ForEachPlayer(Player.EachOpponent, …)`) to exile another player's library top.
- `lookAtTopAndKeep(count, keepCount)` — Ancestral Memories — keep exactly K to hand.
- `lookAtTopRevealMatchingToHand(count, filter, prompt, restDestination?, restOrder?)` — Radagast the
  Brown / Star Charter shape: look at top `count`, **optionally** reveal one card matching `filter` to
  hand, rest to `restDestination` (default bottom of library) in `restOrder` (default
  `CardOrder.Random`). `count` is a `DynamicAmount` (e.g. `DynamicAmounts.triggeringManaValue()`).
- `revealTopPutAllMatchingToHand(count, filter, restDestination?, restOrder?)` — Marina Vendrell shape:
  **mandatorily** reveal the top `count`, auto-route *every* card matching `filter` to hand (a
  choice-free `FilterCollection` partition, not a "keep up to one" choice), rest to `restDestination`
  (default bottom of library) in `restOrder` (default `CardOrder.Random`). Use this for "reveal the top
  N, put all [type] cards into your hand and the rest on the bottom" wording.
- `lookAtTopAndReorder(count)` — reorder top N.
- `manifest(count = 1)` — manifest the top N cards (CR 701.40): each is put onto the battlefield
  face down as a 2/2 creature (one at a time). A manifested creature card can be turned face up for
  its mana cost; a manifested non-creature can't.
- `manifestDread(markEntered = false)` — "Manifest dread" (CR 701.60, Duskmourn): look at the top
  two cards of your library, manifest one of them (your choice), and put the other into your
  graveyard. Composes gather → select → move-face-down(MANIFEST) → move-to-graveyard →
  emit-`ManifestedDreadEvent`. Pass `markEntered = true` to stamp each manifested permanent with
  `EnteredViaAbilityComponent(this source)`, so wrapping it in
  `RepeatDynamicTimes(X, manifestDread(markEntered = true))` lets a later
  `GatherCards(CardSource.EnteredViaThisResolution)` re-collect every creature this spell manifested
  (across all X iterations and the per-iteration manifest-dread pick pauses) and reference "each of
  those creatures" — e.g. **Valgavoth's Onslaught**: `RepeatDynamicTimes(XValue, manifestDread(true))`
  → `GatherCards(EnteredViaThisResolution, "manifested")` → `AddCountersToCollection("manifested",
  +1/+1, XValue)`. The trailing `EmitManifestedDreadEventEffect` tail (internal — not for card
  authors) fires `Triggers.WheneverYouManifestDread` (see Triggers) once per manifest-dread,
  carrying the card put into the graveyard this way so a "this way" payoff can pull it back out
  (**Paranormal Analyst**).

**Reveal patterns**

- `revealUntilNonlandDealDamage(target)` — Bonecrusher Giant shape.
- `revealUntilMatchToHand(filter, restDestination?, restOrder?)` — Spinner of Souls / Wirewood Herald
  shape: reveal from the top of your library until you reveal a card matching `filter`; that card goes to
  hand and the cards revealed before it go to `restDestination` (default: bottom of library) in `restOrder`
  (default: random). If the library empties before a match, nothing goes to hand and every revealed card
  goes to the rest destination.
- `wheelEffect(players)` — each player shuffles hand into library, draws that many.
- `factOrFiction(count = 5, keepZone, otherZone, ...)` — reveal/look at the top `count`, an
  opponent splits them into two piles, then you choose which pile goes to `keepZone` (hand) and
  which to `otherZone` (graveyard). The shared CR 700.3 "divvy" pile-split primitive — also drives
  Sauron's Ransom (`count = 4`, chained `.then(Effects.TheRingTemptsYou())`).

**Hand manipulation**

- `discardCards(count, target)` — controller-of-target chooses (mandatory).
- `discardCardsUnlessMatching(count, unlessFilter, target?, reducedCount?, requiredMatches?)` /
  `Effects.DiscardUnlessMatching(...)` — one-step "discard N cards unless you discard a matching card" selection;
  a lower-count selection is valid only when it includes enough cards matching `unlessFilter`.
- `discardAnyNumber(target?, filter?, storeAs?, prompt?)` — "discard any number of cards": the
  controller chooses any subset of their hand (including none) to discard, via
  `SelectionMode.ChooseAnyNumber`. The selected set is stored under `storeAs` (default `"discarded"`),
  so the count is readable downstream as `DynamicAmount.VariableReference("${storeAs}_count")` — e.g.
  Miasma Demon wires this as the `ReflexiveTriggerEffect` action and reads `discarded_count` as the
  reflexive targets' `dynamicMaxCount` ("up to that many target creatures").
- `discardRandom(count, target)` — random discards.
- `discardHand(target)` — discard entire hand.
- `eachOpponentDiscards(count, controllerDrawsPerDiscard?)` — Mind Twist-style.
- `eachPlayerDiscardsDraws(controllerBonusDraw?)` — Windfall / Wheel of Fortune.
- `eachPlayerDrawsX(includeController?, includeOpponents?)` — Howling Mine shape.
- `eachPlayerMayDraw(maxCards, lifePerCardNotDrawn?)` — optional group draw with a tax.
- `exileFromHand(count?, target)` — exile N from hand.

**Sacrifice / destroy**

- `sacrifice(filter, count, then)` — sacrifice N, then run effect.
- `sacrificeFor(filter, countName, thenEffect)` — sacrifice variable count, store, then effect.
- `destroyAllPipeline(filter, noRegenerate?, storeDestroyedAs?)` — wrath pipeline with storage.
- `destroyAllAndAttachedPipeline(filter, noRegenerate?)` — wrath + attached.
- `destroyAllSharingTypeWithSacrificed(noRegenerate?)` — destroy all creatures sharing type with a sacrificed creature.

**Creature-type choice**

- `chooseCreatureTypeRevealTop()` — pick a type, reveal until matching.
- `chooseCreatureTypeReturnFromGraveyard(count)` — pick a type, return N from graveyard.
- `chooseCreatureTypeModifyStats(...)` — pick a type, buff matching.
- `chooseCreatureTypeUntap()` — pick a type, untap your matching.
- `chooseCreatureTypeGainControl(duration?)` — pick a type, control matching.
- `becomeChosenTypeAllCreatures(...)` — all creatures become the chosen type.

**Misc mechanic shapes**

- `mayPay(cost, effect)` — optionally pay cost to trigger an effect.
- `mayPayOrElse(cost, ifPaid, ifNotPaid)` — pay-or-else fork.
- `blight(amount, player?)` — Blight X additional cost glue.
- `bolster(amount)` — Bolster N (CR 701.36): controller chooses a creature with the least toughness among
  creatures they control and puts N +1/+1 counters on it. Non-targeting; no-op with no creatures. Composes
  Gather → `FilterCollection(CollectionFilter.LeastToughness)` → `SelectFromCollection(ChooseExactly 1)` →
  `AddCountersToCollection(+1/+1)`. Toughness is read from projected state for the least-toughness comparison.
- `explore(explorer?)` — Explore (CR 701.44): reveal the top card of your library; a land goes to your
  hand, otherwise the exploring permanent (default `EffectTarget.Self`) gets a +1/+1 counter and you may
  put the revealed card into your graveyard. Pure pipeline composition: Gather (revealed) →
  `FilterCollection(MatchesFilter(Land))` partition → `MoveCollection(land → hand)` →
  `GatedEffect(WhenCondition(Not(CollectionContainsMatch("explored", Land))))` over counter + optional
  graveyard move. The gate is "no land revealed" (not "a nonland was revealed") so an empty library still
  yields the counter, per CR 701.44a/b.
- `forage(afterEffect?)` — Forage as an *effect* ("you may forage"): a `ChooseActionEffect` letting
  the player choose to exile three cards from their graveyard or sacrifice a Food (each gated by a
  feasibility check), with `afterEffect` appended to whichever mode is taken (Bushy Bodyguard, Curious
  Forager). For forage as a *cost*, use `Costs.Forage()` / `Costs.additional.Forage` (§3).
- `loot(draw?, discard?)` — "draw N, discard M" loop.
- `rummage(count?)` — discard then draw.
- `connive(target?)` — draw 1, discard 1, then put a +1/+1 counter on `target` (default Self) if the discard was a nonland (CR 702.166). Also exposed as `Effects.Connive(target)`.
- `conniveTargeting(requirement, storeAs?)` — connive whose +1/+1 counter lands on a *reflexively chosen* target: "draw a card, then discard a card. When you discard a nonland card this way, put a +1/+1 counter on target creature you control" (Teo, Spirited Glider). The recipient is selected at resolution via `SelectTargetEffect` *inside* the nonland gate — so the player never chooses up front or when the discard is a land. Pass the recipient's `TargetRequirement` (e.g. `Targets.CreatureYouControl`); do **not** also declare it as a cast-time `target(...)`. Exposed as `Effects.ConniveTargeting(requirement)`.
- `readTheRunes()` — "draw X cards; for each, discard a card unless you sacrifice a permanent." Composes `RepeatDynamicTimesEffect(XValue, ChooseActionEffect(...))` with feasibility guards. Exposed as `Effects.ReadTheRunes()`.
- `eachOpponentMayPutFromHand(filter?)` — each opponent may dump a matching card.
- `putFromHand(filter?, count?, entersTapped?, entersAttacking?)` — you may put N from hand onto
  battlefield. `entersAttacking = true` puts them in **tapped and attacking**
  (`ZonePlacement.TappedAndAttacking`; the engine adds an `AttackingComponent` against the defending
  player), e.g. Shadowfax, Lord of Horses ("put a creature card with lesser power from your hand onto
  the battlefield tapped and attacking").
- `incubate(n)` — make an Incubator token with N counters.
- `impulse(count?, expiry?)` — impulse draw: exile the top N of your library, may play those cards until `expiry` (default end of turn); played cards still pay their mana. For the play-free variant compose with `GrantPlayWithoutPayingCostEffect` (cf. `shuffleAndExileTopPlayFree`). Irascible Wolverine (1), Annie Flash, the Veteran (2).
- `returnLinkedExile(underOwnersControl?)` — bring back linked exile pile.
- `takeFromLinkedExile()` — pull one card from linked exile.
- `shuffleGraveyardIntoLibrary(target?)` — Elixir of Immortality shape.
- `reflexiveTrigger(action, whenYouDo, optional?)` — optional action; if taken, queue a reflexive trigger.

**Group bulk operations** (one effect applied to every permanent matching a `GroupFilter`)

- `modifyStatsForAll(power, toughness, filter, duration?)` — give every match +X/+Y (`Int` or `DynamicAmount`).
- `doublePowerAndToughnessForAll(filter, duration?)` — double each match's power and toughness. Resolves to a fixed +P/+T modification read per-entity from projected state via `DynamicAmount.EntityProperty(EntityReference.IterationEntity, …)`, so the bonus locks in at resolution (no re-doubling) and negative power doubles correctly. Roar of Endless Song, Unnatural Growth.
- `grantKeywordToAll(keyword, filter, duration?)` / `removeKeywordFromAll(...)`; `tapAll(filter)` / `untapGroup(filter?)`; `dealDamageToAll(amount, filter)`; `destroyAll(filter, noRegenerate?)`; `gainControlOfGroup(filter?, duration?)`.
- `GroupFilter` exclusion flags: `excludeSelf` (`.other()`) drops the resolving **source** from the group; `excludeTarget` (`.otherThanTarget()`) drops the spell/ability's **first chosen target**. Combine `GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)` with `.otherThanTarget()` for "each other creature with the same controller [as the target]" — Fear, Fire, Foes!: `dealDamageToAll(1, GroupFilter(Creature.targetPlayerControls(TargetController)).otherThanTarget())`.

---

## 5.5 Inline pipelines (`Effects.Pipeline { }`)

The facade-respecting way to compose a **one-off** Gather → Select → Move pipeline inside a card
file (see `backlog/inline-pipeline-dsl.md`). Named `Patterns.*` entries are for named MTG mechanics
and shapes with a demonstrated second user; one-off pipelines go inline via the builder instead of
hand-threading string slot keys between raw step constructors.

Each builder verb serializes to the existing pipeline step `Effect` — the result is the exact same
`CompositeEffect` tree the raw constructors produce (zero engine change, zero JSON-contract change).
Steps return **typed slot handles**; the only way to obtain a handle is from a step that produced
it, so a read-without-write (the `CardLinter` dangling-slot error class) cannot be expressed.

```kotlin
effect = Effects.Pipeline {
    val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(7)))
    val (kept, rest) = chooseExactlySplit(2, from = looked)
    toHand(kept)
    toGraveyard(rest)
}
```

**Slot handles** (one per `EffectContext` namespace, mirroring `CardLinter.Space`):

| Handle | Backing store | Produced by | Consumed by |
|---|---|---|---|
| `CollectionSlot` | `storedCollections` | `gather`, `chooseExactly`, `filter`, `captureControllers`, `moveTracked`, … | `move`, `reveal`, the select/filter verbs, `forEachCaptured` |
| `NumberSlot` | `storedNumbers` | `storeNumber`, `forEachCaptured`'s block param | `.amount` → `DynamicAmount.VariableReference` |
| `ChosenSlot` | `chosenValues` | `storeCardName`, `chooseOption`, `noteCreatureType` | `GameObjectFilter.namedFromVariable(slot)` |
| `SubtypeGroupsSlot` | `storedSubtypeGroups` | `gatherSubtypes` | subtype-matching filters |

**Keys are auto-generated deterministically** — `"<verb><stepIndex>"` per builder instance
(`gathered0`, `selected1`, `matching3`), so renaming a Kotlin `val` never churns the serialized
JSON, while reordering steps changes keys (the tree changed anyway). Every producing step takes an
optional `name = "..."` override: use it for readable goldens on gnarly cards and for **churn-free
migration** of existing inline cards (keep the old hand-written keys → byte-identical JSON,
untouched snapshot goldens; `inv/cards/Lobotomy.kt` is the worked example). Duplicate explicit
names, empty pipelines, and empty branch blocks fail at card-load with `require`.

**Step vocabulary** (one verb per existing step type — the vocabulary grows with step types, never
with cards):

| Builder verb | Serializes to |
|---|---|
| `gather(source)` / `gather(filter, player?, …)` (battlefield shorthand) | `GatherCardsEffect` |
| `gatherUntilMatch(filter, …)` → `(match, revealed)` | `GatherUntilMatchEffect` |
| `chooseExactly(n, from)` / `chooseUpTo` / `chooseAnyNumber` / `chooseRandom` / `selectAll` (+ `…Split` variants returning `(selected, remainder)`) | `SelectFromCollectionEffect` |
| `filter(from, filter)` / `filterSplit(…)` → `(matching, rest)` | `FilterCollectionEffect` |
| `move(from, destination, …)` / `moveTracked(…)` / sugar `destroy`, `sacrifice`, `exile`, `toHand`, `toGraveyard`, `toLibraryTop`, `toLibraryBottom` | `MoveCollectionEffect` |
| `reveal(from, …)` | `RevealCollectionEffect` |
| `captureControllers(from)` | `CaptureControllersEffect` |
| `forEachCaptured(collection, original, controllers) { count -> … }` | `ForEachCapturedControllerEffect` |
| `gatherSubtypes(from)` | `GatherSubtypesEffect` |
| `storeCardName(from)` | `StoreCardNameEffect` |
| `storeNumber(amount)` | `StoreNumberEffect` |
| `chooseOption(optionType, …)` / `noteCreatureType(…)` | `ChooseOptionEffect` / `NoteCreatureTypeEffect` |
| `choosePile(a, b, chooser?, …)` → `(chosen, other)` | `ChoosePileEffect` |
| `selectTarget(requirement)` (resolution-time choice — never printed "target") | `SelectTargetEffect` |
| `ifNotEmpty(slot, filter?, minSize?) { … } orElse { … }` | `ConditionalOnCollectionEffect` |
| `whenMatches(slot, filter)` (returns a `Condition`, adds no step) | `CollectionContainsMatch` |
| `run(effect)` | any other `Effect`, verbatim |

`run(...)` keeps the builder open: non-pipeline effects (a `ShuffleLibraryEffect`, a damage effect)
interleave without the builder needing a verb for everything. Optional secondary outputs
(`storeRemainder`, `storeNonMatching`, `storeMovedAs`) are only serialized when the card actually
requests the handle (`chooseExactlySplit`, `filterSplit`, `moveTracked`), keeping emitted JSON free
of never-read writes.

**Branch scoping** matches the engine's `EffectContext`: handles from the outer scope are visible
inside `ifNotEmpty` / `forEachCaptured` blocks by plain lexical capture (branches don't start fresh
scopes; nested *abilities* do). Branch bodies with one step stay bare; multiple steps wrap in a
nested `CompositeEffect`. Nested scopes share the key counter, so auto-keys never collide.

```kotlin
// Branch-on-gathered (the PR #618 idiom):
effect = Effects.Pipeline {
    val drawn = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)))
    reveal(drawn)
    ifNotEmpty(drawn, filter = GameObjectFilter.Creature) {
        toHand(drawn)
    } orElse {
        toGraveyard(drawn)
    }
}
```

**Special `gather` sources** (component-backed, no zone scan):

- `CardSource.Self` — the ability's own source card, in whatever zone it currently sits.
- `CardSource.TriggeringEntity` — the entity that fired the trigger (`EffectContext.triggeringEntityId`),
  the gatherable counterpart of `EffectTarget.TriggeringEntity`. Yields a single-element collection while
  that entity still exists (empty once it has left), so a non-targeted "it" reference can feed a
  gather → move → grant pipeline. Backs "whenever a creature you control becomes blocked, you may exile it.
  You may play that card from exile this turn" (Norin, Swift Survivalist): `gather(TriggeringEntity)` →
  `exile(...)` → `GrantMayPlayFromExile(EndOfTurn)`.
- `CardSource.TappedAsCost` — the permanents tapped to pay the activation cost.
- `CardSource.AttachedTo(host, filter?)` — the permanents attached to the `host` entity (any
  `EffectTarget` that resolves to a permanent — a spell's `ContextTarget`, `Self`, `TriggeringEntity`, …)
  that match `filter`, read off the host's `AttachmentsComponent` and intersected with the projected
  filter matches. The non-targeted counterpart of "the Equipment/Aura attached to that creature": yields
  nothing when the host has left play or has no matching attachments. Backs "destroy up to one Equipment
  attached to that creature" (Light of Judgment): `gather(AttachedTo(targetCreature, Equipment))` →
  `chooseUpTo(1)` → `destroy(...)`.
- `CardSource.ChosenTargets` — the spell/ability's already-resolved targets.
- `CardSource.FromLinkedExile(count?)` — the cards in the source's linked-exile pile.
- `CardSource.CraftedMaterials` — the cards exiled to Craft the source (its
  `CraftedFromExiledComponent`), restricted to those still in exile. The gather-pipeline twin of
  `ExiledCardsSource.CRAFTED` (which feeds back-face CDAs/ability grants). Backs The Grim Captain's
  "put an exiled creature card used to craft it onto the battlefield tapped and attacking":
  `GatherCards(CraftedMaterials) → SelectFromCollection(ChooseUpTo 1, filter = Creature) →
  MoveCollection(BATTLEFIELD, ZonePlacement.TappedAndAttacking)`.
- `CardSource.LastKnownCombatPairedWithSource` — creatures blocking/blocked by the source at the
  moment it last left the battlefield (Abu Ja'far).
- `CardSource.CreaturesThatSaddledSource` — the creatures that saddled the source Mount this turn
  (CR 702.171c), read off its crew/saddle-contributors record and restricted to creatures still on
  the battlefield. Backs "exile it and up to one creature that saddled it this turn, then return
  those cards" (Fortune, Loyal Steed): `gather(CreaturesThatSaddledSource)` → `chooseUpTo(1)` →
  exile linked to the Mount alongside `CardSource.Self` → `gather(FromLinkedExile())` → return
  under owners' control.
- `CardSource.EnteredViaThisResolution` — every permanent this resolving spell/ability put onto the
  battlefield, found by the `EnteredViaAbilityComponent(this source)` stamp that a
  `MoveCollection(markEnteredViaSourceAbility = true)` leaves (restricted to permanents still on the
  battlefield). Reads live battlefield state rather than a pipeline collection, so it survives the
  pauses of a multi-step resolution and the per-iteration contexts of a `RepeatDynamicTimes` body.
  Backs "manifest dread X times, then put X +1/+1 counters on each of those creatures" (Valgavoth's
  Onslaught): `RepeatDynamicTimes(XValue, manifestDread(markEntered = true))` →
  `gather(EnteredViaThisResolution)` → `AddCountersToCollection(+1/+1, XValue)`.
- `CardSource.LastKnownEquipmentAttachedToSource` — the Equipment that was attached to the source the
  moment a self-sacrifice / self-exile cost moved it off the battlefield (CR 112.7a), read off
  `EffectContext.lastKnownSourceAttachments` (captured before the cost wiped the source's attachment
  list) and restricted to permanents still on the battlefield that are still Equipment. The host has
  gone by resolution, so the live attachment index is empty — only the captured snapshot identifies
  them. Backs "attach an Equipment that was attached to it to that creature" (Zack Fair):
  `gather(LastKnownEquipmentAttachedToSource)` → `chooseExactly(1)` (auto-resolves with one, no-op
  with none) → `AttachTargetEquipmentToCreature(PipelineTarget(chosen), target)`.

A card needing a genuinely **new step semantic** (a new capture kind, a new decision shape) still
adds the `Effect` + executor first (`add-feature`); the builder only composes the existing
vocabulary. The JSON/custom-card authoring path is unchanged — raw step types stay `@Serializable`
with string keys, and `CardLinter` remains the backstop for that path and for anything the builder
can't statically prevent (cross-trigger flows, `Self`-vs-`ContextTarget` inside `ForEach`).

---

## 6. Targets

### Resolution-time (`EffectTarget`)

- `EffectTarget.ContextTarget(i)` — i-th cast-time target.
- `EffectTarget.Controller` — controller of the source ability.
- `EffectTarget.Self` — the source permanent. In a *granted* ability (Equipment/Aura "equipped
  creature has …"), `Self` is the **host** that received the ability — its `{T}` taps the host —
  not the granting object (CR 113.7).
- `EffectTarget.GrantingSource` — the permanent whose static ability granted the currently-resolving
  ability: the Equipment/Aura/permanent bearing the `GrantActivatedAbility` static, as the counterpart
  to `Self` (the host). Use when a granted ability names the *granting object* — e.g. Trusty
  Boomerang's "equipped creature has '{1}, {T}: Tap target creature. **Return Trusty Boomerang** to
  its owner's hand'" (`Effects.ReturnToHand(EffectTarget.GrantingSource)`), or Cranial Plating's
  "Attach Cranial Plating to target creature". The granter is captured when the ability is put on the
  stack (threaded `ActivatedAbilityOnStackComponent.granterId` → `EffectContext.granterId`), so it
  survives the granter leaving play — the referencing effect no-ops if it's gone (CR 113.7a). For an
  ability whose source already *is* the granter (Territory Forge / Sharkey-style gains), it resolves
  to the same entity as `Self`.
- `EffectTarget.TriggeringEntity` — the entity that caused the trigger to fire.
- `EffectTarget.TargetController` — the controller of the spell/ability's first chosen target
  ("its controller creates two Map tokens", "its controller gains 4 life"). Control-change effects
  are honored (projected controller first), and a target that has already left the battlefield —
  typically destroyed/exiled by an earlier step of the same effect (Get Lost, Beast Within) —
  resolves to the controller it last had on the battlefield (CR 608.2h last-known information,
  carried by the engine's `LastKnownPermanentComponent`), falling back to the card's owner.
- `EffectTarget.DiscardedAsCost(index = 0)` — a card discarded to pay this spell's additional discard
  cost (`Costs.additional.DiscardCards(...)`). The discarded card is in its owner's graveyard by
  resolution (CR 608.2), so this resolves to that card's id; pair it with an `EntityMatches` (facade
  `Conditions.DiscardedCardMatches(filter)`) to test the discarded card's graveyard characteristics —
  the cost-referencing sibling of `EntityReference.Sacrificed` / `TappedAsCost`. Resolution-only. Used
  by Grab the Prize ("if the discarded card wasn't a land card, …").
- `EffectTarget.PlayerRef(...)` — a player slot; see the `Player` reference list below.

**`Player` references** (multiplayer-safe vocabulary — there is deliberately no bare
`Player.Opponent`; every reference says *which* player it means):

- `Player.You` — the controller of the ability/effect.
- `Player.Each` — all players (active players only; lost players are skipped).
- `Player.EachOpponent` — all of your opponents. Also the *matching* form for "an opponent"
  in event filters (`SpellCastEvent(player = …)`), exists-conditions, and battlefield
  aggregations ("creatures your opponents control").
- `Player.ActivePlayerFirst` — all players in APNAP order.
- `Player.TargetPlayer` / `Player.TargetOpponent` — the bound player target (resolved from the
  chosen targets, never from turn order).
- `Player.DefendingPlayer` — CR 802.2a: the player the ability's source is attacking, read from
  the source's attack assignment (a creature attacking a planeswalker defends against its
  controller); falls back to the trigger's damaged player as last-known information for "deals
  combat damage to a player" triggers. Use for attack/combat-damage triggers ("defending player
  mills four cards", "that player sacrifices a creature").
- `Player.TriggeringPlayer` — the player bound by the trigger (the caster for `SpellCastEvent`,
  the active player for per-player step triggers — "at the beginning of each opponent's upkeep,
  *that player* …", **the player dealt the damage** for a `DealsDamageEvent` trigger whose
  recipient is a player, **and the dying/leaving permanent's last-known controller** for a
  death / leaves-the-battlefield trigger — so "whenever a creature an opponent controls dies,
  *they* may pay 3 life" binds *that creature's controller*, the opponent, not the ability's
  controller (Meathook Massacre II). This is populated from `ZoneChangeEvent.lastKnownController`
  (CR 603.10/608.2h last-known information), falling back to the owner). It resolves inside a target's controller filter, so "deals noncombat
  damage to an opponent, … to target creature **that player** controls" is
  `TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.TriggeringPlayer))))`
  on the damage trigger — the legality check and resolution both bind it to the damaged player
  (Fear of Burning Alive).
- `Player.AnOpponent` — a genuinely non-targeted "an opponent" (a chooser: "an opponent chooses a
  creature type"). Currently resolves to the first opponent in turn order; the proper multiplayer
  choice flow is tracked in `backlog/multiplayer.md`. Do **not** use it where the text means
  `TargetOpponent`, `EachOpponent`, `DefendingPlayer`, or `TriggeringPlayer`.
- `Player.ChosenOpponent` — the opponent locked into the source's `ChoiceSlot.OPPONENT` slot.
- `Player.EnchantedPlayer` — the player the source Aura is attached to ("enchant player", CR 303),
  read from the source's `AttachedToComponent` when its target is a player. Use it both for the payoff
  (`DynamicAmount.LifeTotal(EnchantedPlayer)`, `EffectTarget.PlayerRef(EnchantedPlayer)`) and to scope a
  `PreventLifeGain` (`LifeGainEvent(player = EnchantedPlayer)`). Resolves to nothing if the source isn't
  an Aura attached to a player. (Grievous Wound.)
- `Player.ControllerOf(desc)` / `Player.OwnerOf(desc)` — controller/owner of the first chosen target.
- `Player.OwnersOfLinkedExile` — the **distinct owners** of the cards still in the effect source's
  linked-exile pile (`LinkedExileComponent`, populated by `Effects.ExileUntilLeaves`). A *list*
  reference: reach it only through `Effects.ForEachPlayer(Player.OwnersOfLinkedExile, …)`, typically
  on a `LeavesBattlefield` trigger, to make "the exiled card's owner does X" act on the right
  player(s). One iteration per distinct owner (a player owning several exiled cards acts once);
  resolves to nothing — never "all players" — when the pile is empty. Unidentified Hovership ("the
  exiled card's owner manifests dread", CR 701.62) = `ForEachPlayer(Player.OwnersOfLinkedExile,
  Patterns.Library.manifestDread().effects)`.
- `Player.ContextPlayer(i)` / `Player.Candidate` / `Player.Any` — positional target, CR 115
  candidate during target-restriction evaluation, and "a player" matching.
- `EffectTarget.ContextProperty(key)` — value plumbed into `EffectContext` (damage amount, life gained, blight
  amount, …).
- `EnchantedCreature` / `EquippedCreature` — resolve via `AttachedToComponent`; requires the state-aware
  `resolveTarget(state, target)` overload.
- `EnchantedPermanent` — same `AttachedToComponent` resolution as `EnchantedCreature`, but type-agnostic; use for
  Auras that enchant non-creature permanents (e.g. Wellspring enchants a land: "gain control of enchanted land").
- `AttachedToTriggeringPermanent` — inside a `Triggers.becomesAttached` trigger, the permanent the
  triggering attachment (Aura/Equipment) became attached to. Resolved live from the triggering
  object's `AttachedToComponent` (so a "for as long as attached" payoff does nothing if the
  attachment already left — CR 611.2b). Used by Eriette ("gain control of that permanent") and
  Assimilation Aegis ("that creature becomes a copy …").

### Cast-time (`Targets.*` / `TargetRequirement`)

- `Targets.Any` — any creature, player, or planeswalker.
- `Targets.AnyChosenByOpponent` — "any target **of an opponent's choice**" (Cuombajj Witches). A real
  target of *your* spell/ability that an **opponent** selects: announced at the same time as your own
  targets, equally respondable, and with legality (hexproof/protection/shroud) measured relative to
  **you, the controller** — so an opponent can't pick a hexproof creature they control. Desugars to
  `AnyTarget(chooser = TargetChooser.Opponent)`; any `TargetRequirement` can carry `chooser` (see
  below). List the opponent-chosen requirement *after* the controller-chosen ones in a script. The
  engine routes the opponent's selection at announcement for **activated abilities** (the only printed
  use today); the controller picks which opponent in multiplayer (currently defaults to the sole
  opponent — see `TargetChooser`).
- `Targets.AnyOtherThanEnchantedCreature` — any target except the creature the source Aura/Equipment
  is attached to. Desugars to `TargetOther(AnyTarget(), excludeAttachedCreature = true)`; for Aura/Equipment
  abilities worded "enchanted/equipped creature deals damage … to **any other target**" (e.g. Pain for All),
  where the dealer is the attached creature, not the ability's source permanent.
- `Targets.Creature` — any creature.
- `Targets.CreatureYouControl` / `CreatureOpponentControls` — controller-restricted.
- `Targets.OtherCreatureYouControl` — "another target creature you control"; excludes the source.
- `Targets.Player` — any player.
- `Targets.Planeswalker` — any planeswalker.
- `Targets.Permanent` — any permanent.
- `Targets.NonlandPermanent` — any nonland permanent.
- `Targets.OtherNonlandPermanent` — "another target nonland permanent"; excludes the source (Braided Net).
- `Targets.Artifact` — any artifact.
- `Targets.Enchantment` — any enchantment.
- `Targets.Land` — any land.
- `Targets.ArtifactOrLand` — any artifact or land (Territory Forge).
- `Targets.BasicLand` — any basic land.
- `Targets.Spell` — any spell on the stack.
- `Targets.InstantOrSorcerySpellYouControl` — an instant or sorcery spell on the stack you control (copy-your-spell modes).
- `Targets.CreatureSpellYouControl` — a creature spell on the stack you control (Choreographed Sparks' "Copy target creature spell you control").
- `Targets.ActivatedOrTriggeredAbility` / `Targets.ActivatedAbility` — an ability on the stack (Stifle).
- `Targets.ActivatedOrTriggeredAbilityYouControl` — an activated or triggered ability **you control** on the stack (`TargetFilter.ActivatedOrTriggeredAbilityOnStack.youControl()`). Mana abilities never use the stack, so they're excluded automatically. Pair with `Effects.CopyTargetSpellOrAbility` — **Gogo, Master of Mimicry**.
- `Targets.SpellOrAbilityWithSingleTarget` — a spell or ability whose single target is changed (Willbender; pair with `Effects.ChangeTarget()`).
- `Targets.InstantSorcerySpellOrAbility` — one requirement admitting an instant spell, sorcery spell, activated ability, or triggered ability on the stack (`TargetFilter.InstantSorcerySpellOrAbilityOnStack`, a single `CardPredicate.Or`). Pair with `Effects.CopyTargetSpellOrAbility()` — Return the Favor. **Note:** the STACK targeting enumeration (`TargetFinder.findSpellTargets`) offers an ability as a legal target only when the requirement's filter *explicitly names an ability predicate* (anywhere inside `Or`/`And`/`Not`); plain "target spell" filters stay spell-only (CR 112.1 vs 113.3b/c).
- `Targets.Card` — any card in any zone (e.g. graveyard).
- `Targets.CreatureOrPlaneswalker` — combined.
- `Targets.TappedCreature` / `UntappedCreature` — state-restricted.
- `Targets.InstantOrSorcery` — instant-or-sorcery card.

**Chained predicates** — `.youControl()`, `.controlledByOpponent()`, `.opponent()`, `.withSubtype(...)`,
`.withKeyword(...)`, `.ofColor(...)`, `.tapped()`, `.untapped()`, `.power(n)`, `.minPower(n)`, `.maxPower(n)`,
`.targetsMatching(subfilter)` (a spell/ability on the stack that targets at least one object matching
`subfilter` — `CardPredicate.TargetsMatching`; e.g. `GameObjectFilter.InstantOrSorcery.targetsMatching(GameObjectFilter.Creature)`
for "an instant or sorcery spell that targets a creature" — Forum Necroscribe, Lecturing Scornmage);
`.castFromZone(zone)` / `.notCastFromZone(zone)` (a spell on the stack that was / wasn't cast from a
specific zone — `StatePredicate.WasCastFromZone`, reading `SpellOnStackComponent.castFromZone`; e.g.
`TargetFilter.SpellOnStack.notCastFromZone(Zone.HAND)` for "target spell that wasn't cast from its
owner's hand" — Wash Away, since a card in a hand is owned by that hand's player, CR 108.3);
plus `TargetFilter.excludeSelf` to exclude the source.

### Cross-zone union targets (`TargetFilter.or` / `TargetFilter.anyOf`)

A **single** target whose legal objects can come from more than one zone, each with its own predicate —
the cross-zone "or" wording. `TargetFilter` carries `alternatives: List<TargetFilter>`, additional
zone-scoped clauses unioned with the primary one; build it with `clauseA.or(clauseB)` or
`TargetFilter.anyOf(clauseA, clauseB, …)`. The legal-target set is the union over every clause and a
chosen target is legal iff it satisfies *any* clause. This is **not** a multi-target requirement (still
one target), and it is distinct from the player/permanent unions (`TargetSpellOrPermanent`,
`TargetCreatureOrPlayer`). `GameObjectFilter.anyOf` is the same idea *within one zone*; this lifts it
across zones, which the flat `baseFilter` can't express because each zone needs its own predicate and
the engine dispatches target-finding/validation per `TargetFilter.zone`.

```kotlin
// Sorceress's Schemes — "instant or sorcery card from your graveyard or exiled card with flashback you own"
val union = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou()
    .or(TargetFilter(GameObjectFilter.Any).withKeyword(Keyword.FLASHBACK).ownedByYou().inZone(Zone.EXILE))
val t = target("…", TargetObject(filter = union))
effect = Effects.Composite(Effects.ReturnToHand(t), Effects.AddMana(Color.RED))
```

"Card with flashback" is just `withKeyword(Keyword.FLASHBACK)`: a flashback ability adds
`Keyword.FLASHBACK` to the card's base keywords, so the predicate matches even on a card sitting in
exile (no projection needed). The client routes a union to its cross-zone card picker, grouping the
valid targets into per-(owner, zone) tabs — "Your Graveyard", "Your Exile", etc.

### Named multi-target binding

```kotlin
spell {
    val creature = target("creature", Targets.Creature)
    val player = target("player", Targets.Player)
    effect = Effects.Composite(
        Effects.Destroy(creature),
        Effects.DealDamage(3, player),
    )
}
```

For modal spells, prefer the explicit `targetPlayerControls(target)` DSL form; per-mode targets route via
`modeTargetsOrdered`.

### Target count

Every `TargetRequirement` carries count semantics (defaults shown):

- `count = 1` — maximum number of targets.
- `minCount = count` — minimum; set below `count` for "one or two target creatures".
- `optional = false` — when `true`, minimum becomes 0 ("up to N target ..."). An activated ability
  whose controller-chosen requirements are **all** optional (e.g. Boom Box's "Destroy up to one target
  artifact, up to one target creature, and up to one target land") is legal to activate with an *empty*
  target list — choosing no targets for every slot — and still resolves; the engine only requires a
  target when at least one controller requirement has a non-zero effective minimum.
- `unlimited = false` — when `true`, **"any number of target ..."** — no upper cap. The practical
  maximum is the number of legal targets, which the engine sends to the client; validation imposes
  no limit and the minimum is 0. Use this instead of a large placeholder `count` (Phyrexian Purge,
  Kaboom, Weaver of Lies). Available on `TargetObject` / `TargetCreature(...)` / `TargetPlayer` and
  on `TargetOpponent` — `TargetOpponent(unlimited = true)` is "any number of target opponents"
  (Hollow Marauder); pair with `ForEachTargetEffect` to apply a per-opponent body. Works on
  **triggered abilities** as well as spells — `TargetPlayer(unlimited = true)` on a triggered
  ability sizes the decision's `maxTargets` to the legal-target count (Tinybones Joins Up's
  "any number of target players each discard a card"). For "**X** target
  creatures" use `dynamicMaxCount = DynamicAmount.XValue` instead — that clamps the count to the chosen X.
- `dynamicMaxCount: DynamicAmount?` — evaluated when the spell/ability hits the stack; the resolved
  value becomes the max ("up to X target creatures", X = board state or chosen X).
- **Distinctness (CR 601.2c) is automatic and needs no flag.** A single requirement that picks more
  than one target ("two / up to two / X target creatures") is one instance of the word "target", so the
  chosen objects/players must all be **different** — enforced both at cast time (`TargetValidator`) and on
  interactive target decisions (`DecisionValidators`). Cross-requirement duplicates are a *different*
  "target" instance and stay **legal by default** (the same object may be chosen once per instance); when
  a later requirement must differ from an earlier one ("… and up to one **other** target …"), wrap it in
  `TargetOther` — `.other()`/`excludeSelf` on a filter only excludes the *source*, not another chosen target.
- `sameController = false` — on `TargetObject` / `TargetCreature(...)`; when `true` and the requirement
  picks more than one target, every chosen target must share a controller ("**two target creatures
  controlled by the same player**"). Enforced cross-target by `TargetValidator` at cast time using
  projected control; a no-op for single-target requirements. E.g.
  `TargetCreature(count = 2, sameController = true)` (Barrin's Spite).
- `sameOwner = false` — on `TargetObject`; when `true` and the requirement picks more than one target,
  every chosen **card** target must share an owner ("**exile up to two target cards from a single
  graveyard**"). Enforced cross-target both at cast time (`TargetValidator`) and on triggered-ability
  target decisions (`DecisionValidators` reads each card's `OwnerComponent`); a no-op for single-target
  requirements and for non-card targets. E.g.
  `TargetObject(count = 2, optional = true, filter = TargetFilter.CardInGraveyard, sameOwner = true)`
  (Arashin Sunshield).
- `sameCreatureType = false` — on `TargetObject` / `TargetCreature(...)`; when `true` and the requirement
  picks more than one target, the chosen **permanent** targets must all share at least one creature type
  ("**two target creatures you control that share a creature type**"). Enforced cross-target by
  `TargetValidator` at cast/activation time as the **intersection** of every target's *projected* creature
  subtypes being non-empty — i.e. a single creature type common to the *whole* set, which for 3+ targets is
  stricter than pairwise sharing (granted/changed types count). A target with no creature types — or one off
  the battlefield — can never share, so the set is rejected. A no-op for single-target requirements and for
  non-permanent targets. E.g. `TargetCreature(count = 2, filter = TargetFilter.CreatureYouControl,
  sameCreatureType = true)` (Secret Tunnel).
- `totalManaValueAtMost = null` — on `TargetObject`; when set to a `DynamicAmount`, the **combined
  mana value** of the chosen **card** targets may not exceed the resolved amount ("**any number of
  target creature cards with total mana value X or less**"). The amount is resolved once the ability
  is put on the stack — for a reflexive after a pay-`{X}` (`DynamicAmount.XValue` reads the X just
  paid) — and enforced cross-target against the summed `manaValue` by both `TargetValidator`
  (authoritative, resolving the `DynamicAmount` against `xValue`) and, on the interactive target
  decision, `DecisionValidators` (which sees the cap already baked to a concrete int in
  `TargetRequirementInfo`). Pair with `unlimited = true` for the "any number … with total mana value
  N or less" shape. Distinct from `dynamicMaxCount`, which caps the target *count*, not their summed
  mana value. The web-client's graveyard target picker enforces the same cap eagerly (blocks a pick
  that would exceed it). E.g. `TargetObject(unlimited = true, filter =
  TargetFilter(GameObjectFilter.Creature.ownedByTriggeringPlayer(), zone = Zone.GRAVEYARD),
  totalManaValueAtMost = DynamicAmount.XValue)` — **Fire Lord Sozin** (back face of The Rise of Sozin),
  reanimating post-payment via a `MayPayXForEffect(ReflexiveTriggerEffect(...))`.
- `chooser = TargetChooser.Controller` — **who selects this requirement's target(s)**. Set to
  `TargetChooser.Opponent` for "**… of an opponent's choice**" wording (Cuombajj Witches). The chosen
  target is still a real target of *your* spell/ability — announced together with your own targets,
  equally respondable, legality measured relative to **you** — but an opponent picks which legal
  object/player it is (the controller chooses *which* opponent in multiplayer per CR 601.6a/602.3a, and
  that pick follows the controller's own choices per CR 601.6b/602.3b). Orthogonal to legality: target-finding and
  validation ignore `chooser` (always relative to the controller); only the announcement layer reads it
  to route the selection decision. Honored for **activated abilities** today; list the opponent-chosen
  requirement after the controller-chosen ones. `Targets.AnyChosenByOpponent` is the ready-made
  "any target of an opponent's choice". `CardLinter` (§21) fails any card that puts a
  `TargetChooser.Opponent` target outside an activated ability — a spell or triggered ability would
  silently let the *controller* choose it instead.

### Player-target restrictions (`TargetPlayer.restriction` / `TargetOpponent.restriction`)

A `TargetPlayer` / `TargetOpponent` can carry `restriction: Condition?` — a gate each candidate
player must satisfy to be a legal target ("target player who lost life this turn", "target player
with 10 or less life"). The restriction is evaluated against each player with
`Player.Candidate` bound to that player, in all three target paths: legal-target enumeration,
cast/activation validation, and the CR 608.2b re-check at resolution (a target whose restriction
stopped holding — e.g. gained life above the threshold — is removed, fizzling a single-target spell).

Author the restriction through the `Conditions.candidate*` facade (never hand-write `Player.Candidate`):

- `Conditions.candidateLostLifeThisTurn()` — the targeted player lost life this turn (Rix Maadi Guildmage).
- `Conditions.candidateLifeAtMost(n)` — the targeted player has `n` or less life.

Because `Condition` descriptions don't read as English relative clauses, pass `descriptionOverride`
whenever `restriction` is set:

```kotlin
target(
    "target player who lost life this turn",
    TargetPlayer(
        restriction = Conditions.candidateLostLifeThisTurn(),
        descriptionOverride = "target player who lost life this turn",
    ),
)
```

This is the player-arm prerequisite for the planned composable mixed `TargetUnion` (see
`backlog/target-union-with-arms.md`).

---

## 7. Filters & predicates

### `GameObjectFilter` — for searches, sacrifice, group effects

- `Filters.AnyCard` — any card.
- `Filters.Creature` — any creature card.
- `Filters.Land` — any land card.
- `Filters.BasicLand` — any basic land.
- `Filters.PlainsCard` / `IslandCard` / `SwampCard` / `MountainCard` / `ForestCard` — specific basics.
- `Filters.Instant` — instant card.
- `Filters.Sorcery` — sorcery card.
- `Filters.Permanent` — permanent card.
- `Filters.NonlandPermanent` — nonland permanent.
- `Filters.WithSubtype(subtype)` — card of a given subtype.
- `GameObjectFilter.Multicolored` — multicolored card (two or more colors; `CardPredicate.IsMulticolored`).
- `CardPredicate.IsColored` — one or more colors (the complement of `IsColorless`). Used for "a
  permanent that's one or more colors" (Ugin, Eye of the Storms). Pair with `IsPermanent` for the
  colored-permanent target filter; pair `IsColorless` with `IsNonland` for "colorless nonland card".
- **Type-negation predicates** `CardPredicate.IsNonland` / `IsNoncreature` / `IsNonenchantment` /
  `IsNonartifact` — "is not a \<type\>". Named filter constants `GameObjectFilter.Nonland` /
  `Noncreature` / `Nonenchantment` / `Nonartifact` wrap each. `IsNonartifact` is the "nonartifact
  creature" leg of the Terror template ("destroy target nonartifact, nonblack creature") — pair with
  `IsCreature` + `.notColor(...)`. FQL keys: `nonland` / `noncreature` / `nonartifact`.
- **Combined-type filters** — `GameObjectFilter.InstantOrSorcery` / `CreatureOrPlaneswalker` /
  `CreatureOrEnchantment` / `ArtifactOrEnchantment` / `CreatureOrArtifact` / `CreatureOrVehicle` /
  `ArtifactOrLand` / `ArtifactCreatureOrEnchantment` each wrap a `CardPredicate.Or` of the named
  types (`CreatureOrVehicle` matches a creature or any object with the Vehicle subtype).
  `ArtifactCreatureOrEnchantment` (the three-type O-Ring restriction) has matching target constants
  `TargetFilter.ArtifactCreatureOrEnchantment` and
  `TargetFilter.ArtifactCreatureOrEnchantmentOpponentControls` — the latter for "exile target
  artifact, creature, or enchantment an opponent controls" (Trapped in the Screen).

**Chained predicates**

- `.youControl()` / `.controlledByOpponent()` — control predicate.
- `.controlledByActivePlayer()` — controlled by the player whose turn it is (`ControllerPredicate.ControlledByActivePlayer`).
  Pairs with `Triggers.EachUpkeep` for "at the beginning of each player's upkeep, do X to permanents that player
  controls" (the upkeep player is the active player — Temporal Distortion).
- `.targetPlayerControls(target)` — controlled by a referenced player. Resolves `EffectTarget`
  bindings/context targets, plus `EffectTarget.ControllerOfTriggeringEntity` (controller of the
  entity that fired the trigger — e.g. Tectonic Instability "tap all lands its controller controls").
- `.ownedByYou()` / `.ownedByOpponent()` — owner predicates (for graveyard/exile cards without a
  controller, and "you own" battlefield wordings).
- `.ownedByTargetPlayer()` (`ControllerPredicate.OwnedByTargetPlayer`, FQL `own:target-player`) —
  owned by the spell/ability's **target player**, the owner-axis sibling of `.targetPlayerControls()`.
  Matches the card's immutable owner against `context.targetPlayerId`, so it captures battlefield
  permanents the target owns even when another player controls them (after a control change) and
  cards in other zones with no controller. Use for "all artifacts **target player owns**" wordings
  (Hurkyl's Recall) where the spell may target either player, so a fixed owned-by-you/opponent
  predicate can't express it. Gather with `Patterns.Group.returnAllToHand(GroupFilter(
  GameObjectFilter.Artifact.ownedByTargetPlayer()))` — its `BattlefieldMatching(player = Player.Each)`
  adds no `youControl` constraint, so the filter matches purely on ownership across the battlefield.
  Also works **at target-validation time** in a *graveyard* zone for a separately-targeted player:
  `TargetValidator` threads the chosen player target (target index 0) into graveyard-card validation,
  so a `TargetObject(unlimited = true, filter = TargetFilter(GameObjectFilter.Artifact.ownedByTargetPlayer(),
  zone = Zone.GRAVEYARD))` legally selects "any number of target artifact cards from **target player's**
  graveyard" — **Drafna's Restoration**. (An `unlimited` requirement combined with a bounded one is now
  overflow-safe in the validator's per-cast target-count cap.) Pair with `GatherCardsEffect(CardSource.
  ChosenTargets)` → `MoveCollectionEffect(destination = ToZone(LIBRARY, player = Player.TargetPlayer,
  placement = Top), order = CardOrder.ControllerChooses)` to put the chosen cards on top of *their*
  (the target player's) library in a player-chosen order.
- `.ownedByTriggeringPlayer()` (`ControllerPredicate.OwnedByTriggeringPlayer`, FQL
  `own:triggering-player`) — owned by the trigger's associated player: the damaged player for a
  combat/damage trigger, the event's player otherwise. The *non-targeted* "that player" sibling of
  `.ownedByTargetPlayer()`, matching the card's immutable owner against `context.triggeringPlayerId`.
  Use for "…from **that player's** graveyard" where the ability doesn't target the player — Fire Lord
  Sozin's "put any number of target creature cards … from that player's graveyard" reads "that player"
  as whoever Sozin just damaged. Works at target-validation/finding time in a graveyard zone:
  `TargetObject(unlimited = true, filter = TargetFilter(GameObjectFilter.Creature.ownedByTriggeringPlayer(),
  zone = Zone.GRAVEYARD), totalManaValueAtMost = DynamicAmount.XValue)`.
- `.withControllerPredicate(p)` — set any `ControllerPredicate` directly; the entry point for the
  **composed** predicates `ControllerPredicate.And(list)` / `Or(list)` / `Not(p)`, which express
  heterogeneous controller/owner relationships in one filter — e.g. "creatures you own but don't
  control" = `withControllerPredicate(And(listOf(OwnedByYou, Not(ControlledByYou))))`. Every engine
  evaluation site (live projection, zone-change last-known-controller, grant fast paths) shares the
  combinator recursion via the `evaluateWith` fold next to `ControllerPredicate`. Note that
  `GameObjectFilter.and` **rejects** two sides carrying *different* controller predicates (it used
  to silently keep only one) — state the intent with a composed predicate instead.
- `.withSubtype(s)` / `.withKeyword(k)` — type/ability predicate.
- `.ofColor(c)` / `.ofColors(set)` — color predicate.
- `.withColor(c)` / `.withAnyColor(c…)` / `.notColor(c)` — fixed-color predicates (`CardPredicate.HasColor`/`NotColor`).
- `.nonartifact()` — appends `CardPredicate.IsNonartifact` ("nonartifact creature", the Terror template);
  the type-negation analogue of `.notColor(c)` / `.notSubtype(s)`.
- `.notCreature()` — appends `CardPredicate.Not(CardPredicate.IsCreature)` ("noncreature artifact",
  e.g. `GameObjectFilter.Artifact.notCreature().youControl()` — Guardian Beast).
- `.withCardPredicate(p)` — appends any `CardPredicate` for which there's no dedicated combinator
  (general-purpose escape hatch), e.g.
  `GameObjectFilter.Nonland.withCardPredicate(CardPredicate.HasActivatedAbility)` — The Enigma Jewel.
- `.nonbasic()` — appends `CardPredicate.Not(CardPredicate.IsBasicLand)` ("nonbasic land"); compose on
  the land base (`GameObjectFilter.Land.nonbasic()`), or use the named constant `GameObjectFilter.NonbasicLand`
  / `TargetFilter.NonbasicLand` (Rocket Volley, Shivan Harvest, Encroaching Wastes). `TargetFilter.Land.nonbasic()`
  is the target-side passthrough.
- `.withChosenColor()` — `CardPredicate.HasChosenColor`: matches the color chosen during the current
  effect's resolution (read from `EffectContext.chosenColor`, set by `Effects.ChooseColorThen`). Use with
  `AggregateBattlefield(Player.Each, …)` for "for each permanent of that color" (Coalition Dragon cycle).
  In a **static ability** (a continuous lord), the chosen color is read from the source permanent's
  `CastChoicesComponent` (set by `EntersWithChoice(ChoiceType.COLOR)`), so `ModifyStats(filter =
  GroupFilter(GameObjectFilter.Creature.youControl().withChosenColor()))` expresses "creatures you
  control of the chosen color get +X/+Y" (Heraldic Banner). Fails closed until a color is chosen.
- `.sharingCreatureTypeWith(entity)` — `CardPredicate.SharesCreatureTypeWith(entity)`: shares ≥1 (projected)
  creature subtype with a referenced entity. `entity` may be `EntityReference.AffectedEntity`, which resolves
  to the creature a continuous effect is being applied to during projection — combine with
  `AggregateBattlefield(Player.Each, GameObjectFilter.Creature.sharingCreatureTypeWith(EntityReference.AffectedEntity), excludeSelf = true)`
  for "+X/+X for each OTHER creature that shares a creature type with it" (Alpha Status). In a granted
  context `excludeSelf` excludes the affected (enchanted) creature, not the granting source.
- `.sharingColorWith(entity)` — `CardPredicate.SharesColorWith(entity)`: shares ≥1 (projected) color with
  a referenced entity (e.g. `EntityReference.Triggering`). Mirror of `.sharingCreatureTypeWith(entity)`.
  Colorless entities share no color (never match). Used by Spreading Plague ("destroy all other creatures
  that share a color with it") — pair with `Effects.DestroyAll(filter, excludeTriggering = true)` so the
  triggering creature itself is spared.
- `.sharingColorWithPermanentYouControl(filter)` — `CardPredicate.SharesColorWithPermanentYouControl`:
  shares ≥1 (projected) color with at least one permanent the evaluating player controls matching
  `filter`. Used by Ringsight ("search your library for a card that shares a color with a legendary
  creature you control") with `filter = GameObjectFilter.Creature.legendary()`. Colorless candidates
  never match. Evaluated for real in targeting/search/count contexts; inert (false) in
  static-projection / trigger-gating, permissive (true) in cost-calculation.
- `.notSharingCreatureTypeWithPermanentYouControl(filter)` —
  `CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl`: shares **no** (projected) creature
  type with any permanent the evaluating player controls matching `filter`. Negative analogue of
  `.sharingColorWithPermanentYouControl`. Used by Radagast the Brown ("a creature card that doesn't share
  a creature type with a creature you control") with `filter = GameObjectFilter.Creature`. A candidate
  with no creature types of its own shares none, so it matches.
- `.named(name)` — `CardPredicate.NameEquals`: matches a fixed card name.
- `.notNamed(name)` — `CardPredicate.Not(NameEquals)`: matches cards whose name is **not** `name`. Use for
  "… that don't have the same name as this creature" wording (Marvin, Murderous Mimic — creatures you control
  not named "Marvin, Murderous Mimic").
- `.namedFromVariable(variableName)` — `CardPredicate.NameEqualsChosen`: matches the card name stored in
  `chosenValues[variableName]` (case-insensitive). Set the name with `Effects.ChooseCardName` (player names it)
  or `Effects.StoreCardName` (captured from a chosen card). Fails closed in static/projection contexts. Used by
  the "name a card … cards with that name" family (Desperate Research, Lobotomy).
- `.namedFromChosenComponent(slot = ChoiceSlot.CARD_NAME)` — `CardPredicate.NameEqualsChosenComponent`: matches
  the card name **durably chosen by the source permanent as it entered** — read from that permanent's
  `CastChoicesComponent` under `slot` (case-insensitive). Unlike `.namedFromVariable` (transient pipeline variable,
  fails closed in projection), this is **static-projection / activation-legality safe**: it keys off the granting
  permanent's id, which the predicate context supplies as the source wherever a static ability's filter is
  evaluated. Pair with `replacementEffect(EntersWithChoice(ChoiceType.CARD_NAME))`. Fails closed (no match) before
  a name is chosen. Used by name-keyed static abilities (Petrified Hamlet — "sources with the chosen name … /
  Lands with the chosen name …").
- `.originallyPrintedInSet(setCode)` — `CardPredicate.OriginallyPrintedInSet`: matches a card whose
  *canonical* set code equals `setCode` (case-insensitive), i.e. the set it was *originally printed* in —
  reprints still match their original set, regardless of the printing in play. Reads the entity's
  `CardComponent.originalSetCode` (populated from the canonical `CardDefinition.setCode`); tokens never
  match. Used by Golgothian Sylex (`"ATQ"`) and ARN City in a Bottle (`"ARN"`).
- `.nameNotSharedWithControlledRoom()` — `CardPredicate.NameNotSharedWithControlledRoom`: matches a Room card
  whose name isn't shared with any Room the evaluating player controls (CR 709). Per the Central Elevator
  ruling, only the names of a controlled Room's **unlocked** doors count (a Room with no unlocked doors
  contributes neither name), and a split Room card is excluded if **either** of its door names matches. Keyed
  off the predicate context's `controllerId`; fails **open** (matches) with no controller in scope and matches
  every Room card when the controller has no unlocked doors. Pair with `.withSubtype(Subtype.ROOM)` at a search
  site. Used by Central Elevator ("search your library for a Room card that doesn't have the same name as a
  Room you control").
- `.power(n)` / `.minPower(n)` / `.maxPower(n)` — P/T comparator.
- `.manaValue(n)` / `.manaValueAtMost(n)` / `.manaValueAtLeast(n)` — mana-value comparator.
- `.manaValueAtMostX()` — mana value ≤ the X chosen for the source spell/ability.
- `.manaValueEqualsX()` — mana value **exactly equal** to the X chosen for the source spell/ability (the chosen
  number, or the X paid in an `{X}…` mana cost; resolution-time only — matches nothing without a chosen number).
  Available on both the object-filter builders and on `TargetFilter` (mirrors `.manaValueAtMostX()`). Used by Void
  (`Effects.ChooseNumberThen`) and Repeal (`{X}{U}` — return target nonland permanent with mana value X).
- `.manaValueAtMostEntity(ref)` — mana value ≤ a referenced entity's mana value (e.g. Kodama of the East Tree).
- `.powerEqualsX()` — **projected power exactly equal** to the X chosen for the source spell/ability — the power
  analogue of `.manaValueEqualsX()`. Available on both the object-filter builders and on `TargetFilter`. Used by an
  X-cost activated ability that targets "a creature with power X" (Ent-Draught Basin: `{X}, {T}: Put a +1/+1
  counter on target creature with power X`). Legal-action enumeration runs before X is bound, so it matches
  permissively then (the client re-filters by the chosen X via the `xConstrainsTargetPower` /
  `LegalActionTargetInfo.xConstrainsPower` flags); activation-time validation re-checks with X bound and rejects
  any creature whose power isn't exactly X.
- `.powerGreaterThanEntity(ref)` — power strictly greater than a referenced entity's projected power. Used by
  Éowyn, Fearless Knight ("exile target creature an opponent controls with greater power") — combine
  with `EntityReference.Source` to express "greater power than the ability's source".
- `.powerAtMostEntity(ref)` / `.powerLessThanEntity(ref)` — power ≤ (resp. **strictly** <) a referenced
  entity's projected power; inverses of `.powerGreaterThanEntity`. `powerAtMostEntity` backs Old Man of
  the Sea ("power less than or equal to this creature's power"); `powerLessThanEntity` backs "a creature
  with lesser power" (Rangers of Ithilien). Pair with `EntityReference.Source` for "than the source", or
  with `EntityReference.AmassedArmy` for a **resolution-time pipeline** bound — Grishnákh, Brash Instigator
  ("power ≤ the amassed Army's power"). The pipeline reference is threaded into target enumeration via
  `findLegalTargets(..., pipelineContext = …)`; see §13 "Pipeline values inside target filters".
- `.manaValueAtMostEntityManaSpent(ref)` — mana value ≤ the mana **actually spent** to cast a referenced
  entity. Reads the live `SpellOnStackComponent` buckets while the entity is still a spell, or the
  `CastRecordComponent` snapshot once it has resolved onto the battlefield (0 if it was never cast).
  Used by Edge of Eternities warp payoffs like Astelli Reclaimer ("…mana value X or less…, where X is the
  amount of mana spent to cast this creature") — X is 5 for `{3}{W}{W}`, 3 for warp `{2}{W}`, 0 for free.
- `.manaValueAtMostColorsSpent(ref)` — mana value ≤ the number of distinct **colors** of mana spent to
  cast a referenced entity (0–5). Sibling of `.manaValueAtMostEntityManaSpent`, but compares against the
  color *count* rather than total mana (colorless is not a color). The **Converge** exile-by-color-count
  gate — Sundering Archaic ("exile target nonland permanent an opponent controls with mana value less than
  or equal to the number of colors of mana spent to cast this creature"), with `EntityReference.Source`.
- `.manaValueAtMostDynamic(amount)` — mana value ≤ a resolved `DynamicAmount`. The open-ended
  "mana value X or less, where X is <some game value>" cap, for sources no fixed/entity-derived sibling
  covers: feed it any `DynamicAmount` (a `TurnTracking` total, a count over a filter, a life total, an
  arithmetic composition, …) and the engine evaluates it against the controller/source at evaluation
  time. **Moseo, Vein's New Dean** — "return … a creature card with mana value X or less from your
  graveyard …, where X is the amount of life you gained this turn" — uses
  `.manaValueAtMostDynamic(DynamicAmount.TurnTracking(Player.You, TurnTracker.LIFE_GAINED))`. The cap
  fails closed (matches nothing) when there is no controller context to resolve a player-scoped amount,
  and is `false` in the layer-projection / cost-calculation / cast-record paths (no resolution context),
  matching the other entity-relative caps.
- `.manaValueIsOdd()` / `.manaValueIsEven()` — mana-value parity (zero is even). Pair with modal
  spells whose modes ask the caster to choose a parity (e.g. *Mutinous Massacre*).
- `.hasXInManaCost()` — the card's **printed** mana cost contains an `{X}` symbol (inspects
  `manaCost.hasX`, not the computed mana value), so a spell cast with X=0 still matches by its
  printed cost. Face-down objects (no mana cost) never match; the cast-record path returns `false`
  (a record stores the resolved mana value, not the printed cost). Used by *Paradox Surveyor*
  ("a card with {X} in its mana cost"). Underlying predicate: `CardPredicate.HasXInManaCost`.
- `.toughnessAtMost(n)` / `.toughnessAtLeast(n)` — toughness comparator.
- `.powerOrToughnessAtLeast(n)` / `.powerOrToughnessAtMost(n)` — **OR** caps over power and
  toughness: matches when *either* power or toughness is ≥ (resp. ≤) `n`. `powerOrToughnessAtMost`
  backs Arnyn, Deathbloom Botanist ("a creature you control with power or toughness 1 or less
  dies"). Honored in all four evaluation sites (resolution predicate, trigger matcher with
  last-known stats, layer projection, cost calculation). Underlying predicates:
  `CardPredicate.PowerOrToughnessAtLeast` / `CardPredicate.PowerOrToughnessAtMost`.
- `.toughnessAtMostX()` — toughness ≤ the X chosen for the source spell/ability. Resolves
  against `PredicateContext.xValue` at evaluation time, so it works at the spell's resolution
  filter pass (e.g. Zero Point Ballad's mass destruction). Layer projection / trigger matching
  / cost calculation report `false` (no X context).
- `.tapped()` / `.untapped()` — tap state.
- `.withCounter(type)` / `.withAnyCounter()` / `.withoutCounters()` — counter presence: a specific kind,
  any kind, or none at all. `.withoutCounters()` is `StatePredicate.Not(HasAnyCounter)` for "with no
  counters on it" (Heartless Act). All three are also on `TargetFilter`
  (`TargetFilter.Creature.withoutCounters()`).
- `.dealtDamageThisTurn()` — was dealt damage this turn (marked-damage *history*, not current marked
  damage); backed by `StatePredicate.WasDealtDamageThisTurn`. Survives damage removal / leaving combat;
  cleared at end-of-turn cleanup. For "...that was dealt damage this turn" (Rooftop Assassin, Unsparing
  Boltcaster). Also available on `TargetFilter` (`TargetFilter.Creature.dealtDamageThisTurn()`).
- `.dealtCombatDamageToSourceControllerThisTurn()` — source-relative: creature dealt combat damage
  *this turn* to the player who controls the effect's source; backed by
  `StatePredicate.DealtCombatDamageToSourceControllerThisTurn`. Resolves `context.sourceId`'s
  controller, so as an edict filter it means "...a creature that dealt combat damage to *you* this
  turn" (Witch-king of Angmar). Per-turn marker, cleared at end-of-turn cleanup; inert with no source
  context (group-static projection returns false).
- `.saddled()` — permanent is saddled (CR 702.171b); backed by `StatePredicate.IsSaddled`.
- `.crewedOrSaddledSourceThisTurn()` — source-relative: creature crewed (CR 702.122) or saddled
  (CR 702.171) the effect's source permanent this turn; backed by
  `StatePredicate.CrewedOrSaddledSourceThisTurn` (see Object-state predicates). For
  "target/choose/return a creature that crewed/saddled it this turn".
- `.crewedOrSaddledBySourceThisTurn()` — source-relative **mirror** of the above: the candidate is a
  Vehicle/Mount that the effect's source *creature* crewed/saddled this turn (source is the crewer,
  candidate is the Vehicle); backed by `StatePredicate.CrewedOrSaddledBySourceThisTurn`. For
  "whenever a Vehicle crewed by this creature this turn attacks" (Balthier and Fran).
- `.nontoken()` / `.token()` — token vs printed.
- `.monocolored()` — restrict to monocolored objects (exactly one color, CR 105.2); colorless objects don't match. ("for each color among monocolored permanents you control" — Tarnation Vista.)
- `.faceDown()` — face-down state.
- `.card(filter)` — defer to a card-shape filter for off-battlefield checks.

**Explicit constructor**:
`GameObjectFilter(cardPredicates, controllerPredicate, colorPredicate, keywordPredicate, powerToughnessPredicate, subtypePredicate)`.

### `GroupFilter` — static-ability scope

- `GroupFilter.CreaturesYouControl` — your creatures.
- `GroupFilter.CreaturesOpponentControls` — their creatures.
- `GroupFilter.AllCreatures` — every creature on the battlefield.
- `GroupFilter.All(filter)` — custom group.
- Chained: `.withColor`, `.withoutColor`, `.withKeyword`, `.withoutKeyword`, `.withSubtype`, `.withoutSubtype`,
  `.minPower`, `.maxPower`, `.power`.

### Stack-object predicates

These `CardPredicate`s evaluate against entities in the `Zone.STACK` (spells and activated/triggered
abilities on the stack). They are handled in the evaluator before the `CardComponent` check, so they
work for abilities-on-stack (which carry no `CardComponent`).

- `CardPredicate.IsActivatedOrTriggeredAbility` — true for activated/triggered abilities on the stack
  (Stifle).
- `CardPredicate.IsTriggeredAbility` — triggered abilities only (excludes activated abilities and
  spells).
- `CardPredicate.IsActivatedAbility` — activated abilities only (excludes triggered abilities and
  spells). Mana abilities never use the stack, so they're never matched. Exposed as the
  `Targets.ActivatedAbility` target requirement (Bind: "Counter target activated ability").
- `CardPredicate.TargetsMatching(subfilter)` — true when the stack object's `TargetsComponent`
  includes at least one chosen target matching `subfilter`. Player targets are skipped. The
  subfilter inherits the outer `PredicateContext`, so `Land.youControl()` inside the subfilter
  resolves against the outer chooser. Used by Teferi's Response.
- `CardPredicate.HasNonManaActivatedAbility` — matches a permanent whose printed activated abilities
  include at least one that isn't a mana ability and isn't a loyalty ability (battlefield-activatable).
  Backed by the precomputed `CardComponent.hasNonManaActivatedAbility` flag (set at entity creation from
  `CardDefinition.hasNonManaActivatedAbility`), so abilities granted by other continuous effects are not
  counted. Used by Tsabo's Web ("each land with an activated ability that isn't a mana ability …").
- `CardPredicate.HasActivatedAbility` — matches a permanent/graveyard card whose printed activated
  abilities include at least one that functions from the **battlefield or the graveyard**, **of any kind,
  mana abilities included** (the difference from `HasNonManaActivatedAbility`). Backed by the precomputed
  `CardComponent.hasActivatedAbility` flag (from `CardDefinition.hasActivatedAbility`); granted abilities
  aren't counted. The battlefield-or-graveyard scope matches the zones a craft material comes from, so a
  mana rock/dork qualifies and so does a graveyard card whose only ability is graveyard-activated (a
  hand-only cycling ability does not). Used by The Enigma Jewel's craft material clause ("four or more
  nonlands with activated abilities"). Compose onto any filter with `.withCardPredicate(...)`, e.g.
  `GameObjectFilter.Nonland.withCardPredicate(CardPredicate.HasActivatedAbility)`.

### `StatePredicate` — battlefield state checks

- `IsTapped` — currently tapped.
- `IsUntapped` — currently untapped.
- `IsAttacking` — declared as attacker this combat.
- `IsBlocking` — declared as blocker this combat.
- `HasLockedDoor` (filter builder `hasLockedDoor()`) — a Room permanent (CR 709.5) with at least one locked
  door, i.e. a half lacking its "unlocked" designation (CR 709.5c). Reads the engine's
  `RoomComponent.lockedFaces`; false for non-Rooms and fully-unlocked Rooms. The targeting restriction for
  "unlock a locked door of target Room you control" (Ghostly Keybearer) — a Room with nothing to unlock isn't a
  legal target. Pairs with `UnlockDoorEffect`/`Effects.UnlockDoor`.
- `InSameBandAsSource` (filter builder `inSameBandAsSource()`) — source-relative (CR 702.22):
  matches the effect's source creature itself and any creature sharing its combat band id.
  Resolves against `PredicateContext.sourceId`, so it only matches while that source is attacking
  (band membership exists only during combat). Used as the recipient filter of Camel's
  "prevent all damage Deserts would deal to this creature and to creatures banded with this
  creature". Note: it's only evaluated where the context carries a source entity — currently the
  recipient filter of a `PreventDamage` replacement (see §15); it's inert in group/projection,
  untap, and trigger-gating contexts.
- `IsBlockingSource` (filter builder `blockingSource()`) — source-relative (CR 509): matches a
  creature whose blocked-attacker set contains the effect's `PredicateContext.sourceId`, i.e. the
  source's own blockers. Used with `Patterns.Group.dealDamageToAll(n, GroupFilter(Creature.blockingSource()))`
  for "Whenever this becomes blocked, it deals N damage to each creature blocking it" (Battle-Scarred
  Goblin). Resolves in resolution-time effect contexts (where the source is carried); inert in
  group/projection, untap, and trigger-gating contexts.
- `CreatedBySource` (filter builder `createdBySource()`) — source-relative (CR 111 provenance): matches a
  token whose stamped `CreatedByComponent.creatorId` equals the effect's `PredicateContext.sourceId` — a
  token *created by the source permanent*. Stamped at creation by a `CreateTokenEffect` with
  `stampCreator = true`. Backs "tokens created with this creature" (Tetravus reabsorbing its own Tetravite
  tokens), which `"{filter} tokens you control"` can't express when several sources mint the same token.
  Yields false for non-tokens / unstamped tokens / no source context.
- `NotTargetedByAbilityFromSameNamedSource` (filter builder
  `notTargetedByAbilityFromSameNamedSource()`) — source-relative + stack-aware: the candidate object
  (a spell or permanent) is **not** currently the target of an *ability* on the stack whose source is
  *another* battlefield permanent sharing the effect source's (`PredicateContext.sourceId`) name.
  Backs Goblin Artisans' self-referential targeting restriction ("counter target artifact spell you
  control that isn't the target of an ability from another creature named Goblin Artisans") so two
  Goblin Artisans can't both lock onto the same spell. Evaluated in target validation/choice via
  `PredicateEvaluator`; the spell-target validation path now passes the activating ability's
  `sourceId` so it can resolve. Inert (true) with no source context, and in group-static projection
  (no source / no candidate-on-stack).
- `CrewedOrSaddledSourceThisTurn` (filter builder `crewedOrSaddledSourceThisTurn()`) —
  source-relative (CR 702.122 / 702.171): matches a creature that crewed or saddled the effect's
  source permanent this turn (i.e. one tapped to pay that permanent's Crew/Saddle cost). Resolves
  against `PredicateContext.sourceId` by reading the source's `CrewSaddleContributorsComponent`;
  inert with no source context (group/projection, untap, trigger-gating). Used for Mount/Vehicle
  payoffs that target/choose/sacrifice/return "a creature that crewed/saddled it this turn" (Giant
  Beaver, Rambling Possum, The Gitrog, Calamity). For the *count* of those creatures use
  `DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn` instead.
- `CrewedOrSaddledBySourceThisTurn` (filter builder `crewedOrSaddledBySourceThisTurn()`) — the
  source-relative **mirror** of the above (CR 702.122 / 702.171): matches a Vehicle/Mount that the
  effect's source *creature* crewed or saddled this turn (source is the crewer, candidate is the
  Vehicle). Resolves by reading the *candidate's* `CrewSaddleContributorsComponent` and asking
  whether `PredicateContext.sourceId` is among the recorded crewers; inert with no source context.
  Used as a per-attacker attack-trigger filter for "whenever a Vehicle crewed by this creature this
  turn attacks" (Balthier and Fran).
- `IsAttachedToBySource` (positive filter builder `attachedToBySource()`; negated builder
  `notAttachedToBySource()`) —
  source-relative: matches the permanent the effect's source is attached to, read from the source's
  `AttachedToComponent.targetId`. Used negated for "other than enchanted/equipped creature"
  exclusions on Aura/Equipment edicts — Sporogenic Infection: "target player sacrifices a creature of
  their choice other than enchanted creature" via
  `Effects.Sacrifice(GameObjectFilter.Creature.notAttachedToBySource(), 1, targetPlayer)`. Used
  positively to scope a static ability on an Aura/Equipment to just its host — Stuck in Summoner's
  Sanctum: "enchanted permanent's activated abilities can't be activated" via
  `PreventActivatedAbilities(GameObjectFilter.Permanent.attachedToBySource())`. Resolves
  against `PredicateContext.sourceId`; inert with no source / unattached source, and never matches in
  group-static projection or trigger-gating contexts (no source there).
- `IsSource` (filter builder `sourceItself()`) — source-relative: matches only the effect's source
  permanent itself (`PredicateContext.sourceId == candidate`). The `GameObjectFilter` counterpart of
  `GroupFilter`'s `Scope.Self` — use it to scope a filter-carrying static ability to the very
  permanent that carries it. Backs the granted form of `PreventActivatedAbilities`: a permanent
  granted `PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself())` has *its own*
  activated abilities locked, because the activation-legality check evaluates the filter with the
  grant's holder as source (Braided Net's "Its activated abilities can't be activated for as long
  as it remains tapped"). Inert with no source context, and never matches in group-static
  projection (use `GroupFilter.source()` there) or trigger-gating contexts.
- `IsAttachedToSource` (filter builder `attachedToSource()`) — the *mirror* of `IsAttachedToBySource`:
  matches an Aura/Equipment currently attached **to** the effect's source, read from the candidate's
  `AttachedToComponent.targetId == sourceId`. Use it to scope a static ability on the *host* to its own
  attachments — Cloud, Midgar Mercenary's "an Equipment attached to it" via
  `GameObjectFilter.Artifact.withSubtype("Equipment").attachedToSource()`. Source-relative; inert with no
  source context.
- `HasGreatestPower` (filter builder `hasGreatestPower()`) / `HasLeastPower` (filter builder
  `hasLeastPower()`) — has the greatest / least projected power among creatures *its controller*
  controls (ties all qualify). Used for "creature with the greatest/least power" target and edict
  filters, e.g. Witch-king, Bringer of Ruin: `Effects.Sacrifice(Creature.hasLeastPower(), 1, EachOpponent)`.
- `IsRingBearer` (filter builder `ringBearer()`) — the creature is its controller's Ring-bearer
  (CR 701.54: has `RingBearerComponent` and is controlled by its designating owner). Used for
  player-level Ring-bearer conditions via `Conditions.YouControl(Creature.ringBearer(), negate = …)`
  (Dúnedain Rangers: "if you don't control a Ring-bearer"). For the source-relative "this creature is
  your Ring-bearer" use the existing `Conditions.SourceIsRingBearer`.
- `IsFaceDown` — currently face-down.
- `HasCounter(type)` — has at least one counter of `type`.
- `AttachedToCardType(cardType)` — Aura/Equipment whose `AttachedToComponent` points to a
  permanent that currently has the given top-level [`CardType`] in its **projected** type
  set. Used by filters like "Aura attached to a land" (Pyramids) or "Equipment attached
  to a creature". Reads the attached-to permanent's projected types, so a land animated
  into a creature still matches `LAND` and additionally matches `CREATURE`. False for
  entities with no `AttachedToComponent`.
- `AttachedTo(filter)` (filter builder `attachedTo(hostFilter)`) — the **general** form of
  `AttachedToCardType`: Aura/Equipment whose `AttachedToComponent` host matches an arbitrary nested
  `GameObjectFilter`, evaluated against **projected** battlefield state, so the host's control (`a
  creature you control`), card type, keywords, P/T, etc. all compose. The "you" of any controller
  predicate in the nested filter resolves to the controller supplied in the evaluation context (the
  ability's controller). False if not attached to anything; in group-static projection (which has no
  ability controller) it never matches — it is only meaningful in target/condition contexts. Used by
  Stolen Uniform's "if it's attached to a creature you control" guard
  (`Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Any.attachedTo(GameObjectFilter.Creature.youControl()))`).
- `ExiledWithSource` (filter builder `exiledWithSource()`) — source-relative: the candidate card is
  one the effect's source permanent exiled, i.e. its id is recorded in the source's
  `LinkedExileComponent` (the same linkage set by `RedirectZoneChange(linkToSource = true)`,
  `RedirectZoneChangeWithEffect(linkToSource = true)`, `MoveToZoneEffect(linkToSource = true)`, and the
  `FromLinkedExile` pipeline source). Backs "target creature card exiled with ~" reanimation — The
  Darkness Crystal: `TargetObject(filter = TargetFilter(GameObjectFilter.Creature.exiledWithSource(),
  zone = Zone.EXILE))`. Resolves against `PredicateContext.sourceId`; inert with no source, and never
  matches in group-static projection or trigger-gating contexts.
- `IsWarpExiled` (filter builder `warpExiled()`) — card in exile via warp's
  end-of-turn delayed trigger (CR 702.185b).
- `WasCastForWarp` (filter builder `castForWarp()`) — battlefield permanent that
  was cast for its warp cost (CR 702.185). Pair with
  `Conditions.TargetMatchesFilter(GameObjectFilter.Creature.castForWarp(), …)` to
  branch on whether a target was warp-cast (e.g., Full Bore).
- `PutIntoGraveyardFromBattlefieldThisTurn` (filter builder
  `putIntoGraveyardFromBattlefieldThisTurn()`) — card currently in a graveyard whose most
  recent arrival there was from the battlefield during the current turn. Backed by the
  per-entity `PutIntoGraveyardFromBattlefieldThisTurnMarker` data-object component, set by
  `ZoneTransitionService` on every battlefield→graveyard move and stripped when the card
  leaves the graveyard (so a later mill or exile→graveyard arrival doesn't falsely match).
  The marker carries no turn number — `BeginningPhaseManager` wipes it from every entity
  at each turn's untap step, which is what gives the predicate MTG-correct per-turn
  semantics (the engine's `state.turnNumber` increments per round, not per active player,
  so a turn-number comparison would be wrong in multiplayer). Used by Samwise the
  Stouthearted and Lobelia Sackville-Baggins (LTR) — pair with `GameObjectFilter.Permanent`
  or `Creature` on a graveyard-zone `TargetFilter`. False in battlefield-projection / untap /
  trigger-gating contexts (the marker only lives on graveyard cards).
- `BlockedOrWasBlockedByLegendaryThisTurn` (filter builder
  `blockedOrWasBlockedByLegendaryThisTurn()`) — creature that, at some point during the current
  turn, blocked or was blocked by a legendary creature. Backed by the per-creature
  `BlockedOrWasBlockedByLegendaryThisTurnComponent` marker stamped in `BlockPhaseManager` at
  block declaration: the legendary partner's status is captured *at pairing time*, so the
  predicate keeps matching even after that legendary creature leaves the battlefield or loses
  legendary-ness (per the card's ruling). Cleared at end-of-turn cleanup. Distinct from the
  combat-only `IsBlocking`/`IsBlocked`, which only hold during the combat phase. Used by
  You Cannot Pass! (LTR) on a `TargetCreature` filter.

### `AffectsFilter` — static-ability target shapes

- `OtherCreaturesWithSubtype` — lord scope (other creatures of subtype).
- `CreaturesWithCounter` — creatures with at least one counter (Aurification).

> **Load-bearing rule:** filtering battlefield permanents by type/subtype/color/keyword/P-T MUST use
`predicateEvaluator.matchesWithProjection(state, projected, ...)`. Use `projected.isCreature(entityId)` rather than
`cardComponent.typeLine.isCreature`. Non-battlefield zones may read base state.

---

## 8. Triggered abilities (`Triggers.*`)

`triggeredAbility { trigger; effect; target?; triggerCondition?; optional?; elseEffect?; checkOnNextState?; dealsDamageBeforeResolve?; controlledByTriggeringEntityController?; oncePerTurn?; triggersOnce? }`.

**`oncePerTurn` vs `triggersOnce` — two firing caps.** `oncePerTurn = true` caps the ability to one
fire per turn ("This ability triggers only once each turn", e.g. Scavenger's Talent), tracked by a
per-turn component cleared in cleanup. `triggersOnce = true` is the lifetime cap ("This ability
triggers only once", e.g. Acrobatic Cheerleader): once the source has fired it, it never fires again
while that permanent stays on the battlefield — tracked by a `TriggeredAbilityFiredEverComponent`
that is **not** cleared at end of turn (it lives on the entity, so re-entering the battlefield as a
new object — a distinct game object — triggers afresh). Both caps share one detection-time filter and
collapse simultaneous fires of the same `(source, ability)` to a single instance.

**`optional` = "you may [effect]"; `elseEffect` adds "If you don't, [elseEffect]."** For a
**targeted** trigger, `optional` lets the player choose 0 targets to decline, and `elseEffect` runs
on decline or when no legal targets exist (Entrails Feaster: "you may exile a creature card from a
graveyard … if you don't, tap this"). A **no-target** optional trigger lowers to a
`GatedEffect(Gate.MayDecide, then = effect, otherwise = elseEffect)` resolved by the unified gated
executor — a resolution-time yes/no whose "no" runs `elseEffect` (or nothing when there is none,
e.g. Song of Stupefaction's "you may mill two cards"); the wrap is skipped when `effect` already
carries its own consent gate (a `May*`-gated `GatedEffect`), so an authored `Effects.May` never
double-prompts. The may-action's feasibility is
derived from `effect` (a `SacrificeEffect` needs the controller to control a matching permanent), so
an impossible "may" skips the prompt and runs `elseEffect` directly — the no-target analogue of "no
legal targets → else" (Yawgmoth Demon: "you may sacrifice an artifact. If you don't, tap this
creature and it deals 2 damage to you" — with no artifact the tax just applies). Always-feasible
bodies (gain life, draw, add a counter) always prompt.

### Zone change

Named sugar for the common cases; reach for `entersBattlefield(...)` / `leavesBattlefield(...)`
for any other (filter, binding, to/excludeTo) combination.

**Enters the battlefield**

- `EntersBattlefield` — SELF, no filter. ("When this permanent enters.")
- `OtherCreatureEnters` — OTHER binding, filter = `Creature.youControl()`.
- `LandYouControlEnters` — landfall: OTHER binding, filter = `Land.youControl()`.
- `entersBattlefield(filter, binding)` — factory. Covers face-down filters,
  ANY-binding tribal scopes, permanent-you-control scopes, enchantment-enters scopes (Eerie), etc.

**Leaves / dies**

- `LeavesBattlefield` — SELF, any destination.
- `Dies` — SELF, battlefield → graveyard.
- `AnyCreatureDies` — ANY binding, filter = `Creature`.
- `YourCreatureDies` — ANY binding, filter = `Creature.youControl()`. **Per-creature**: fires
  once for *each* matching death, so a board wipe fires it once per creature. Use this for
  "whenever another creature you control dies, …" (Unruly Mob, Rot Shambler, Pitiless Plunderer).
- `OneOrMoreCreaturesYouControlDie(filter = Creature, excludeSelf = false)` — **batched** death
  trigger: fires **at most once per event batch** regardless of how many matching creatures died
  simultaneously. This is the correct shape for "whenever one or more [other] creatures you control
  die, …" (Vengeful Townsfolk) — a per-creature `YourCreatureDies` would over-count on mass removal.
  Set `excludeSelf = true` for the "*other* creatures" wording (the source's own death is excluded).
  Detected specially by `TriggerDetector` (grouped by each dying creature's last-known controller).
  Rule 603.10 "look back in time": if the source itself dies in the *same* batch as another
  qualifying creature, it still sees that death and fires (recovered from its last-known card
  definition, since it has already left the battlefield). For `excludeSelf = true` payoffs that
  target the source (Vengeful Townsfolk's own +1/+1) this is a harmless no-op, but a *non-self*
  payoff — draw a card, make a token, gain life — correctly still resolves on a board wipe that
  also kills the source. The `filter`'s controller predicate scopes which players' deaths count
  (mirrors the enter-batch trigger): no predicate means "you control"; `.opponentControls()` scopes
  to your opponents.
- `OneOrMoreCreaturesAnOpponentControlsDie(filter = Creature)` — the opponent-scoped variant of the
  above (sugar for `OneOrMoreCreaturesYouControlDie(filter.opponentControls())`): batched, fires at
  most once per death batch, so it pairs with `oncePerTurn` without over-firing on mass removal —
  "Whenever one or more creatures your opponents control die, …" (Spiteful Banditry). A per-creature
  `leavesBattlefield(binding = ANY)` would create a separate trigger per death, and simultaneous
  deaths each fire before the once-per-turn marker is set, so the batched form is required here.
- `OneOrMoreCreaturesDie(filter = Creature)` — the unscoped variant: "Whenever one or more creatures
  die, …" (any player's creatures, regardless of controller — Chainsaw). Sugar for
  `OneOrMoreCreaturesYouControlDie(filter.anyController())`; same batched once-per-death-batch
  semantics. The filter carries `ControllerPredicate.ControlledByAny`, which `TriggerDetector`
  treats as "every controller's deaths count". Use `.anyController()` on a `GameObjectFilter` to
  widen any controller-scoped filter the same way.
- `PutIntoGraveyardFromBattlefield` — SELF, same event shape as `Dies`; rename
  clarifies non-creature intent (artifact / enchantment going to yard).
- `leavesBattlefield(filter, to?, excludeTo?, binding, excludeSacrifice = false)` — factory.
  `to = GRAVEYARD` gives a "dies" variant scoped beyond the named constants (other tribal deaths,
  any-controller deaths); `excludeTo = GRAVEYARD` gives "leaves without dying"
  (Three Tree Scribe shape); leaving both null gives "leaves to any zone."
  `excludeSacrifice = true` adds the intervening-if "if it wasn't sacrificed" (Urza's Miter,
  CR 701.21): the trigger fires for any battlefield exit **except** a sacrifice. The matcher reads
  the triggering `ZoneChangeEvent.wasSacrificed` flag, which the central sacrifice hook
  (`ZoneTransitionService.trackPermanentSacrifice` → `pendingSacrificeIds`) stamps on every
  sacrifice — cost payment and the sacrifice effect executors alike — so ordinary destruction /
  lethal-damage / SBA deaths leave it `false`.

**Token creation**

- `EventPattern.TokenCreationEvent(controller = ControllerFilter.You, tokenFilter? = null)` — used as
  a trigger, "Whenever you create a token" (Mirkwood Bats). **Per-token**: fires once for *each* token
  created, so an effect that creates three tokens at once fires it three times. Matched against each
  token-creation `ZoneChangeEvent` (`fromZone == null`); a token that's a copy of a permanent spell
  enters from the stack and is **not** "created" (CR 608.3f / 111.13), so it doesn't fire this. The
  same `EventPattern` also serves as a replacement-effect filter (token doublers); the two uses don't
  conflict.

### Combat

Named sugar for the common cases; reach for `attacks(...)` / `blocks(...)` /
`becomesBlocked(...)` for any other combination, and use the [AttackPredicate]
sealed set for attack-time facts beyond the basics.

**Attacks (per-attacker `AttackEvent`)**

- `Attacks` — SELF, no filter. ("When this creature attacks.")
- `AttacksFirstTimeEachTurn` — SELF sugar for
  `attacks(requires = setOf(AttackPredicate.FirstTimeEachTurn))`. ("Whenever this
  creature attacks for the first time each turn.")
- `attacks(filter?, requires?, binding?)` — factory. Covers ANY-binding scopes,
  type-filtered scopes (creature-you-control, nontoken-creature-you-control),
  and attack-time predicates (alone, Battalion-style count gates, first-attack-each-turn).

**Attacks (player-level)**

- `YouAttack` — when you declare attackers (player-level, ANY binding).
- `YouAttackWithFilter(filter)` — when you attack with ≥1 matching attacker.
- `CreaturesAttackYou` — defender side; fires once per `AttackersDeclaredEvent`,
  not per attacker. Excludes creatures attacking a planeswalker you control
  (CR 509.1b). Pair with `DynamicAmounts.creaturesAttackingYou()` for
  attacker-count payoffs (e.g., Orim's Prayer).
- `CreaturesAttackYourOpponent` — the "your opponents are attacked" counterpart of
  `CreaturesAttackYou`; fires once per `AttackersDeclaredEvent` when one or more declared
  attackers have one of the controller's opponents (a player, via `state.getOpponents`) as
  their defender. Like the "you" side, attacks against an opponent's planeswalker don't count.
  Party Dude level 3.

**Blocks**

- `Blocks` — SELF, no filter.
- `BecomesBlocked` — SELF, no filter.
- `blocks(filter?, binding?, attackerFilter?)` — factory. `filter` constrains the
  blocker (ANY binding). `attackerFilter` constrains the blocked attacker — requires
  SELF binding for "whenever this creature blocks a [filter]" (Skystinger);
  combining it with ANY is rejected (the ANY detector branch ignores `attackerFilter`).
  `triggeringEntityId` is set to the blocked attacker in that case.
- `becomesBlocked(filter?, binding?)` — factory. Replaces the old
  `CreatureYouControlBecomesBlocked` and `FilteredBecomesBlocked(filter)`.
- `BlocksOrBecomesBlockedBy(filter, binding = SELF)` — either direction, partner-filtered;
  sole consumer of `BlocksOrBecomesBlockedByEvent`. Prefer `blocks(attackerFilter=...)`
  when only the blocking direction should fire. `binding = ATTACHED` fires off the
  equipped/enchanted creature's combat (Barrow-Blade — "Whenever equipped creature blocks
  or becomes blocked by a creature, …"); the partner is the `TriggeringEntity`.
- `AttacksAndIsntBlocked` — SELF. Fires once per attacker that reaches end of
  Declare Blockers with no creatures declared as blockers (CR 509.3g). Backed by
  `BecomesUnblockedEvent` matched against `BlockersDeclaredEvent`. Used for
  Merchant Ship: "Whenever this creature attacks and isn't blocked, you gain 2 life."
  (SELF only — an ANY-binding filtered variant isn't wired in `TriggerMatcher` yet.)

**`AttackPredicate`** — extensible "facts about an attack declaration."
Adding a new attack-time mechanic is one new sealed-case + one matcher branch
— `AttackEvent` does not grow a new field per axis.

- `AttackPredicate.Alone` — the attacker is the only declared attacker this
  combat (`attacker count == 1`). Replaces the old `alone: Boolean` axis.
- `AttackPredicate.AttackerCountAtLeast(n)` — at least N creatures total were
  declared as attackers (counting the trigger's attacker). Battalion shape:
  `attacks(requires = setOf(AttackerCountAtLeast(3)))` on a `SELF` binding.
- `AttackPredicate.FirstTimeEachTurn` — the trigger's own attacker is attacking for
  the **first time this turn** (it had not been declared as an attacker in an earlier
  combat phase this turn). Per-attacker, so use it on a `SELF` binding. Fires once on
  the first attack and not again if the creature attacks in a later combat phase the
  same turn (extra-combat effects like Fear of Missing Out); the window resets each
  turn. The "first time" fact is captured on `AttackersDeclaredEvent.firstTimeAttackers`
  at declaration — post-declaration state can't tell, since the per-turn attacker set
  already includes the just-declared attacker. Prefer the `AttacksFirstTimeEachTurn`
  sugar.
- `AttackPredicate.DefenderIsPlayer` — the trigger's own attacker was declared as
  attacking a **player**, not a planeswalker or a battle (CR 508.1). A creature only
  ever attacks a player who is its controller's opponent, so on a `SELF` binding this is
  exactly the "attacks an opponent" wording (Kaalia of the Vast — whose 2024 ruling
  clarifies the ability "doesn't trigger if it attacks a planeswalker or battle").
  Per-attacker, so use it on a `SELF` binding (or an ANY-binding attacker filter that
  already scopes to one creature). The defender kind is fixed at declaration, so it's
  captured on `AttackersDeclaredEvent.attackersAgainstPlayer` rather than re-derived
  from post-declaration state. Prefer the `AttacksAnOpponent` sugar.

Examples:

```kotlin
// "Whenever this creature attacks alone"
Triggers.attacks(requires = setOf(AttackPredicate.Alone))

// "Whenever this creature attacks for the first time each turn" (prefer the sugar)
Triggers.AttacksFirstTimeEachTurn

// "Whenever this creature attacks a player / an opponent" — does NOT fire on attacking a
// planeswalker or battle (Kaalia of the Vast). Prefer the sugar:
Triggers.AttacksAnOpponent
// equivalent to: Triggers.attacks(requires = setOf(AttackPredicate.DefenderIsPlayer))

// "Whenever a nontoken creature you control attacks"
Triggers.attacks(
    filter = GameObjectFilter.Creature.youControl().nontoken(),
    binding = TriggerBinding.ANY,
)

// "Whenever a Beast becomes blocked"
Triggers.becomesBlocked(
    filter = GameObjectFilter.Creature.withSubtype("Beast"),
    binding = TriggerBinding.ANY,
)

// "Whenever this creature blocks a creature with flying" (Skystinger)
Triggers.blocks(attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
```

`Triggers.BecomesBlocked` (SELF, **unfiltered**) fires **once** when the creature becomes
blocked, regardless of how many creatures block it, with `triggeringEntityId` = the source —
so `DynamicAmounts.numberOfBlockers()` reads this creature's blocker count (Rampage). The
**filtered** SELF form `becomesBlocked(filter = …)` instead fires once per matching blocker,
with `triggeringEntityId` = that blocker (Flanking gives each blocker -1/-1).

### Damage

Named sugar for the common cases; reach for the factories for any other combination of axes.

- `DealsDamage` — source deals any damage (SELF binding).
- `DealsCombatDamageToPlayer` — source deals combat damage to a player (SELF binding).
- `DealsCombatDamageToCreature` — source deals combat damage to a creature (SELF binding).
- `OneOrMoreDealCombatDamageToPlayerEvent(sourceFilter = Creature)` — **offensive combat-damage batch trigger** (ANY binding, via `TriggerSpec(OneOrMoreDealCombatDamageToPlayerEvent(sourceFilter = …), TriggerBinding.ANY)`): "whenever one or more [matching creatures] you control deal combat damage to a player" (Kastral, the Windcrested: `Creature.withSubtype("Bird")`; Vaan, Street Thief: `Creature.withAnySubtype("Scout", "Pirate", "Rogue")`). The `sourceFilter`'s "you control" is implied by the observer — don't add `youControl()`. Fires **once per damaged player** (a batch per recipient): multiple matching creatures hitting the same player still fire a single trigger, but two players each dealt damage fire it twice. `Player.TriggeringPlayer` resolves to the damaged player, so effects can reference "that player" (e.g. exile the top card of *that player's* library); `triggeringEntityId` is an arbitrary matching source for that player (batch triggers don't dispatch per source).
- `OneOrMoreCreaturesDealCombatDamageToYou(filter = Creature)` — **defensive combat-damage batch trigger** (ANY binding): "whenever one or more creatures deal combat damage to *you*" (Witch-king of Angmar). Fires at most once per combat-damage batch regardless of how many creatures connected with the trigger's controller (the damaged player), unlike per-source `dealsDamage(recipient = You, …)` which fires once per connecting creature. The triggering entity is an arbitrary matching damager. Pair with the `dealtCombatDamageToSourceControllerThisTurn()` filter for "...each opponent sacrifices a creature that dealt combat damage to you this turn".
- `TakesDamage` — source is dealt damage by any source (SELF binding).
- `CreatureDealtDamageByThisDies` — Etali / Sengir / Soul Collector shape (SELF binding): "whenever a creature dealt damage by *this* permanent this turn dies". Uses `CreatureDealtDamageBySourceDiesEvent(sourceFilter = null)`.
- `creatureDealtDamageBySourceDies(sourceFilter)` — observer variant (ANY binding): "whenever another creature dealt damage this turn by [a source matching the filter] dies" (Shelob, Child of Ungoliant: `GameObjectFilter.Creature.youControl().withSubtype("Spider")`). The damaging source is matched against the filter using last-known info from when it dealt the damage (a `DamagedBySourcesThisTurnComponent` snapshot of the source's controller + subtypes), so a source that died in the same combat still qualifies (CR 608.2h). Only the filter's controller predicate, required subtype, and creature requirement are evaluated against the snapshot.

**Factories** (axes: `damageType` × `recipient` × `sourceFilter` × `binding` for outgoing; `source` × `binding` for incoming):

- `dealsDamage(damageType?, recipient?, sourceFilter?, binding?, requireExcess?, batch?)` — outgoing-damage trigger. Pick `DamageType.{Any,Combat,NonCombat}`, `RecipientFilter.{Any,AnyPlayer,AnyPlayerOrPlaneswalker,AnyCreature,…}`, an optional source `GameObjectFilter`, and `TriggerBinding.{SELF,ANY,ATTACHED}`. Covers "deals combat damage to a player or planeswalker", "creature you control deals combat damage to a player" (`binding = ANY` + `sourceFilter = Creature.youControl()`), "nontoken creature you control deals…" (`.nontoken()`), and "enchanted creature deals damage" (`binding = ATTACHED`). Pass `requireExcess = true` to fire only when the recipient was dealt damage past lethal (CR 120.4a) — Fall of Cair Andros' "is dealt excess noncombat damage". Pass `batch = true` for recipient-side **"one or more" batch wording** (CR 603.2c) — "whenever one or more creatures your opponents control are dealt excess noncombat damage" (Magmatic Galleon): simultaneous damage to several matching recipients (a sweeper, combat damage to multiple blockers) fires the trigger once per event batch instead of once per damaged recipient. Batch is only honored on the `binding = ANY` observer path; SELF/ATTACHED damage triggers are inherently per-source-event. Read the excess via `DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT)`. For a creature recipient, read its toughness *as it last existed at damage time* (CR 603.10 LKI — survives a lethal hit) via `DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS)`; pair it with a `triggerCondition` such as `Conditions.CompareAmounts(ContextProperty(TRIGGER_DAMAGE_AMOUNT), ComparisonOperator.EQ, ContextProperty(TRIGGER_RECIPIENT_TOUGHNESS))` for "deals noncombat damage to a creature equal to that creature's toughness" (Taii Wakeen, Perfect Shot). On the observer path (`binding = ANY` + `sourceFilter`), `EntityReference.Triggering` is the damage SOURCE, but the recipient toughness is still carried in this context key. **Combat caveat:** combat-damage state-based actions run *before* trigger detection, so a non-indestructible recipient that dies to the same combat-damage event has already left the battlefield when a `RecipientFilter.CreatureOpponentControls`-style filter reads its `ControllerComponent` — the filter silently fails (no last-known-info path yet). A `requireExcess = true` + `DamageType.Combat` trigger therefore only fires reliably on recipients that survive (indestructible / high toughness). Fall of Cair Andros is unaffected because it gates on `DamageType.NonCombat`, where the trigger is detected from the damage event before the kill SBA.
- `takesDamage(source?, binding?)` — incoming-damage trigger. Pick `SourceFilter.{Any,Creature,Spell,Combat,NonCombat,HasColor(c),…}` and `TriggerBinding.{SELF,ATTACHED}`. Covers "damaged by a creature/spell" and "enchanted creature is dealt damage" (`binding = ATTACHED`, Aurification / Frozen Solid shape).
- `becomesTapped(binding?, filter?)` — "becomes tapped" trigger. `BecomesTapped` is the SELF constant; pass `binding = TriggerBinding.ANY` with an optional `filter: GameObjectFilter` for "whenever a [filter] becomes tapped" (e.g. `GameObjectFilter.CreatureOrLand` — Temporal Distortion). The filter is matched against the tapped permanent via projected state.

### Phase & turn

Named sugar for the common `(step, player)` cases; reach for `phase(step, player?, binding?)`
for anything else (the ATTACHED-binding aura shapes, custom step/player combinations).

- `YourUpkeep` — start of your upkeep.
- `YourDrawStep` — start of your draw step.
- `EachUpkeep` — every upkeep.
- `EachOpponentUpkeep` — at each opponent's upkeep.
- `ChosenOpponentUpkeep` — at the upkeep of the opponent chosen as the source entered (The Rack).
  Pairs with `replacementEffect(EntersWithChoice(ChoiceType.OPPONENT))`; the step trigger
  (`StepEvent(UPKEEP, Player.ChosenOpponent)`) fires only on that stored player's upkeep — the
  matcher resolves `Player.ChosenOpponent` against the source's `ChoiceSlot.OPPONENT` and doesn't
  fire until a choice is recorded. Effects/dynamic amounts in the ability can reference
  `Player.ChosenOpponent` / `EffectTarget.PlayerRef(Player.ChosenOpponent)` for "that player".
- `YourEndStep` — beginning of your end step.
- `EachEndStep` — beginning of each end step.
- `BeginCombat` — start of combat on your turn.
- `EachCombat` — beginning of each combat (any player's turn).
- `EachEndOfCombat` — at end of combat (CR 511.1), on any player's turn. `YourEndOfCombat` for your
  turn only. Pair with `triggerCondition = Conditions.SourceAttackedOrBlockedThisCombat` for "at end of
  combat, if this creature attacked or blocked this combat, …" (Clockwork Avian).
- `FirstMainPhase` — start of pre-combat main.
- `YourPostcombatMain` — start of post-combat main.

**Factory** — `phase(step, player = Player.You, binding = TriggerBinding.ANY)`.

### Aura / equipment

No named constants for the "enchanted/equipped creature does X" shapes — they all collapse to
the existing event factories with `binding = TriggerBinding.ATTACHED`. Examples (all card uses
in the repo today):

- *Enchanted creature dies* (Demonic Vigor):
  `Triggers.leavesBattlefield(to = Zone.GRAVEYARD, binding = TriggerBinding.ATTACHED)`
- *Enchanted/equipped creature leaves the battlefield* (Curator's Ward):
  `Triggers.leavesBattlefield(binding = TriggerBinding.ATTACHED)`
- *Enchanted/equipped creature attacks* (Extra Arms, Heart-Piercer Bow, Ordeal of Nylea,
  Chorale of the Void, Atomic Microsizer, Sorcerer Role token):
  `Triggers.attacks(binding = TriggerBinding.ATTACHED)`
- *Enchanted permanent becomes tapped* (Uncontrolled Infestation, Cryoshatter):
  `Triggers.becomesTapped(binding = TriggerBinding.ATTACHED)`
- *Enchanted creature is turned face up* (Fatal Mutation):
  `Triggers.turnedFaceUp(binding = TriggerBinding.ATTACHED)`
- *At the beginning of enchanted creature's controller's `<step>`* (Custody Battle,
  Lingering Death): `Triggers.phase(step, binding = TriggerBinding.ATTACHED)`
- *Enchanted-creature damage triggers* — damage factories already support binding:
  `Triggers.dealsDamage(binding = TriggerBinding.ATTACHED)` (any damage),
  `Triggers.dealsDamage(damageType = Combat, recipient = AnyPlayer, binding = TriggerBinding.ATTACHED)`,
  `Triggers.takesDamage(binding = TriggerBinding.ATTACHED)` (Aurification / Frozen Solid).

### Cards & draws

- `YouDraw` — when you draw a card. Fires once per individual card drawn (CR 121.2), so a
  single "draw N" effect triggers it N times.
- `OpponentDraws` — when an opponent draws a card (once per card; the `Player.EachOpponent` analogue
  of `YouDraw`).
- `OpponentDrawsExceptFirstEachDrawStep` — whenever an opponent draws a card **except** the first
  card they draw in each of their own draw steps (CR 504.1's turn-based draw is exempt; every
  other draw — additional draw-step draws and all draws outside the draw step — fires once per
  card). Backed by `DrawEvent(exceptFirstInDrawStep = true)` plus a per-player draw-step-start
  snapshot (`GameState.drawStepStartDrawCountByPlayer`) that identifies the one exempt card. Used
  by Orcish Bowmasters / A-Orcish Bowmasters.
- `NthCardDrawn(n, player?)` — fires when the drawing player draws their Nth card each turn
  (CR 121.2). Draw analogue of `NthSpellCast`; backed by `CardsDrawnThisTurnComponent` (reset
  per turn). Fires exactly once per crossing — a single multi-card draw that spans the
  threshold triggers it once, not N times. Putting cards into hand without "draw" (CR 121.5)
  does not advance the count. Used by Knights of Dol Amroth, Prince Imrahil the Fair,
  Stalwarts of Osgiliath ("Whenever you draw your second card each turn, …").
- `RevealCreatureFromDraw` — Hatching Plans-style top-card reveal.
- `RevealCardFromDraw` — generic reveal-from-draw trigger.
- `CardsPutIntoYourGraveyard(filter?)` — when matching cards enter your yard.
- `PermanentCardsPutIntoYourGraveyard` — only permanent cards.
- `CreaturesPutIntoGraveyardFromLibrary` — mill-trigger shape.
- `LandsPutIntoGraveyardFromLibrary` — batching mill-trigger filtered to land cards. The matching
  land cards are captured into the resolving ability's pipeline under
  `IterationSpace.TRIGGER_CAPTURED_COLLECTION`, so a `MoveCollectionEffect(from = …, destination =
  ToZone(BATTLEFIELD, placement = Tapped))` payoff can put exactly those lands onto the battlefield
  (Hedge Shredder). Like `CreaturesPutIntoGraveyardFromLibrary`, both wrap
  `CardsPutIntoGraveyardFromLibraryEvent(filter)`; the batch detector matches `IsCreature` /
  `IsLand` / `IsNonland` / `HasSubtype` predicates and captures the matching cards.
- `CardsLeaveYourGraveyard(filter?)` — batching trigger; fires once per event batch when one
  or more matching cards **leave** your graveyard (cast/exiled/reanimated/returned to hand,
  etc.), regardless of how many or where they went. For the common "leave your graveyard
  **during your turn**" wording, add `triggerCondition = Conditions.IsYourTurn`; for "this
  ability triggers only once each turn", add `oncePerTurn = true`. (Attuned Hunter, Kishla
  Skimmer, Kheru Goldkeeper.)

### Discard

Fires once per card discarded — a single resolution that discards N cards fires the
trigger N times (mirrors how `YouDraw` handles multi-card draws). The engine emits
one aggregate `CardsDiscardedEvent` per resolution and fans it out in the detector.
`Player.TriggeringPlayer` resolves to the discarding player inside the effect.

- `AnyOpponentDiscards` — whenever an opponent discards a card. (Entropic Battlecruiser.)
- `YouDiscard` — whenever you discard a card.
- `YouDiscardOneOrMore` — **batch wording** "whenever you discard one or more cards"
  (CR 603.2c): fires once per discard event no matter how many cards it contained
  (Inti, Seneschal of the Sun). Sequential discards in the same resolution ("discard a
  card, then discard a card") are separate `CardsDiscardedEvent`s and fire separately.

**Factory** — `discards(player?, cardFilter?, batch?)` — generic shape. `player = Player.Each`
matches any player; `cardFilter` narrows the fan-out to matching cards, so a batch that
discards a creature and two lands fires a `cardFilter = Creature` trigger once, not three
times. The cardFilter is evaluated against the **post-discard zone** (the cards are already
in the graveyard when the trigger matches) — safe for type/subtype/color predicates,
but a filter that depends on hand-specific state would read the wrong zone. `batch = true`
selects the "one or more" wording: at most one firing per discard event even when several
cards match the filter.

### Spell casting

Named sugar for the common type-primitive cases; reach for `youCastSpell(...)` plus a
`SpellCastPredicate` set for anything from-zone / kicked / mana-source-tagged.

- `YouCastSpell` — any spell you cast.
- `YouCastCreature` — any creature spell you cast.
- `YouCastNoncreature` — non-creature spells you cast.
- `YouCastInstantOrSorcery` — instant/sorcery you cast.
- `YouCastEnchantment` — any enchantment you cast.
- `YouCastHistoric` — artifact / legendary / Saga.
- `YouCastSubtype(subtype)` — tribal helper: spell with matching subtype.
- `AnySpellOrAbilityOnStack` — any object hits the stack.
- `OpponentActivatesAbility` — an opponent activates an ability that **isn't a mana ability** (CR 605/606). Mana
  abilities don't use the stack, so they never fire this; loyalty abilities (which are activated abilities) do. Pair
  with `Effects.DealDamage(n, EffectTarget.PlayerRef(Player.TriggeringPlayer))` to punish the activator (Flamescroll
  Celebrant). Backed by `EventPattern.AbilityActivatedEvent(player)`.
- `YouActivateAbility` — you activate an ability that isn't a mana ability (the `Player.You` form of the above).
- `youActivateAbilityTargeting(targetMatch)` — you activate an ability whose **chosen targets** satisfy
  `targetMatch`. Backed by `EventPattern.AbilityActivatedEvent(player, targetMatch)`: when `targetMatch != null`, the
  activated ability on the stack must have at least one chosen target matching it, so a non-targeting ability (e.g.
  tap-for-mana) never fires. `targetMatch` is an `AbilityTargetMatch` (in `scripting.events`): `ObjectMatching(filter)`
  matches an object target against a `GameObjectFilter`, `AnyPlayer` matches a player target, and `AnyOf(list)` is a
  heterogeneous OR (the match space is wider than `GameObjectFilter` because an ability can target a player too).
  `AbilityTargetMatch.CreatureOrPlayer` is the prebuilt "targets a creature or player" used by Ertha Jo, Frontier
  Mentor, whose payoff is `Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity)` (for an
  `AbilityActivatedEvent` the triggering entity is the activated ability on the stack; the copy executor reprompts for
  new targets, CR 707.10/707.10c).
- `activatesAbilityWithoutTap(player?, sourceFilter?, binding?)` — the Antiquities "tap / activate an artifact"
  punisher half: a permanent matching `sourceFilter` has an activated ability used **without `{T}` in its activation
  cost** (Haunting Wind, Powerleech, Artifact Possession). Backed by
  `EventPattern.AbilityActivatedEvent(player, sourceFilter, requireNoTapInCost = true)`. This differs from
  `OpponentActivatesAbility` / `YouActivateAbility` in two ways: it keys on the literal `{T}`-in-cost wording rather
  than "isn't a mana ability", so **a non-`{T}` mana ability also fires it** (the engine emits an
  `AbilityActivatedEvent` for every activated ability whose cost lacks `{T}`, mana or not, and `costsTap`/`isManaAbility`
  on the event let the matcher pick the right wording); and `sourceFilter` restricts which permanent's ability counts
  (`GameObjectFilter.Artifact`, `Artifact.opponentControls()`, or null with `TriggerBinding.ATTACHED` for "enchanted
  artifact"). Pair with `becomesTapped(...)` to cover the full "becomes tapped or has a non-`{T}` ability activated"
  clause. For "that artifact's controller": the **global** form uses
  `EffectTarget.PlayerRef(Player.TriggeringPlayer)` (the activator); the **ATTACHED** form uses
  `EffectTarget.ControllerOfTriggeringEntity` (the enchanted artifact's controller, exposed by
  `AttachmentTriggerDetector`).
- `AttackCausesYourCreaturesTriggeredAbility` — **a creature you control attacking causes a triggered ability of that
  creature to trigger** (Firebender Ascension). Backed by `EventPattern.AbilityTriggeredEvent(player, requireAttackCause,
  sourceFilter?)`, which matches the engine's `AbilityTriggeredEvent` when a triggered ability is put on the stack.
  With `requireAttackCause = true` it fires only for a creature's **own** "whenever this creature attacks" ability — a
  SELF-bound `AttackEvent`. The engine stamps `causedByAttack` on the event in `StackResolver.putTriggeredAbility` (from
  `TriggerProcessor.isAttackCausedTrigger`) so unrelated in-combat triggers (deals damage, dies, ETB) never match; the
  SELF binding is what ties the ability to "that [attacking] creature", excluding anthem-style ANY-bound
  "whenever a creature you control attacks" abilities on other permanents. The triggering ability is exposed as
  `EffectTarget.TriggeringEntity` (`AbilityTriggeredEvent` sets the triggering entity to the ability on the stack), so a
  `Effects.CopyTargetTriggeredAbility(EffectTarget.TriggeringEntity)` can copy it and reprompt for new targets (CR
  707.10c). `sourceFilter` optionally restricts which permanent's ability counts; `player` scopes whose ability it is.

**Other casters.** The same shape, scoped to a different caster via the runtime
`Player.Each` / `Player.EachOpponent` matching on `SpellCastEvent`. Bind the payoff to the
caster with `EffectTarget.PlayerRef(Player.TriggeringPlayer)`.

- `AnyPlayerCastsSpell` — any player (including you) casts a spell.
- `OpponentCastsSpell` — an opponent casts a spell.
- `AnyPlayerChoosesTargets` — any player casts a spell, activates an ability, or puts a triggered ability on the stack with ≥1 target (fires once per object via `EventPattern.TargetsChosenEvent`). The triggering entity is that spell/ability, so the payoff can read/change its targets (Psychic Battle).
- `anyPlayerCasts(spellFilter?, requires?)` — factory; e.g. `anyPlayerCasts(GameObjectFilter.Creature)`
  for "whenever a player casts a creature spell" (Pure Reflection).
- `opponentCasts(spellFilter?, requires?)` — factory; e.g. `opponentCasts(GameObjectFilter.Multicolored)`
  for "whenever an opponent casts a multicolored spell" (Rewards of Diversity).
- `youCastSpellTargetingSource()` — "whenever you cast a spell that targets [this permanent]"
  (Legolas, Master Archer). Sugar for `youCastSpell(requires = setOf(SpellCastPredicate.TargetsSource))`.
- `youCastSpellTargeting(filter)` — "whenever you cast a spell that targets a [filter]" (Legolas,
  Master Archer's `Creature.opponentControls()`). Sugar for
  `youCastSpell(requires = setOf(SpellCastPredicate.TargetsMatching(filter)))`.

**Factory** — `youCastSpell(spellFilter?, requires: Set<SpellCastPredicate>)`. The
`requires` set is conjunctive — every predicate must hold for the trigger to fire.

**`SpellCastPredicate`** — extensible "facts about a cast." Adding a new cast-time mechanic
(was-copied, was-overloaded, paid-additional-life-cost, …) is one new sealed-case plus one
matcher branch — `SpellCastEvent` does not grow a new field per axis.

- `SpellCastPredicate.CastFromZone(zone)` — spell was cast from this zone. Used for Sunbird's
  Invocation (`Zone.HAND`), Goliath Daydreamer's instant/sorcery-from-hand trigger,
  Wildsear's enchantment-from-hand cascade.
- `SpellCastPredicate.CastFromZoneOtherThan(zone)` — the negation: spell was cast from a known
  zone that is *not* [zone]. Fires only on an actual cast from a different recorded zone (a spell
  with no recorded cast zone does not satisfy it). Used by Kellan, the Kid — "Whenever you cast a
  spell from anywhere other than your hand" (`CastFromZoneOtherThan(Zone.HAND)`): casts from
  exile (Adventure / plotted), graveyard (flashback), or the top of library all match; hand casts
  don't.
- `SpellCastPredicate.WasKicked` — spell was cast with kicker (CR 702.32). Used for
  Hallar / Bloodstone Goblin.
- `SpellCastPredicate.PaidWithManaFromSubtype(subtype)` — mana from a permanent of this
  subtype was spent on the cast. Resolves Treasure today (Rain of Riches, Alchemist's
  Talent); engine matcher accepts other token subtypes as the shape, but only Treasure
  actually fires until the mana-pool tracker generalizes beyond its current Treasure-only
  boolean.
- `SpellCastPredicate.IsModal` — spell was cast with at least one chosen mode (rules
  700.2). Matches `SpellCastEvent.chosenModesCount > 0`, where the count is the size of
  `SpellOnStackComponent.chosenModes` (so Spree picking the same mode twice counts as
  two). Used by Riku of Many Paths: "Whenever you cast a modal spell, …".
- `SpellCastPredicate.HasXInCost` — spell has `{X}` in its printed mana cost (CR 107.3).
  A property of the cost, not the value chosen, so a spell cast with X=0 still satisfies
  it. Matches `CardComponent.manaCost.hasX`. Read the announced X in the payoff via
  `DynamicAmounts.xValueOfTriggeringSpell()` (→ `ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL`).
  Used by Geometer's Arthropod: "Whenever you cast a spell with {X} in its mana cost, look
  at the top X cards of your library, …".
- `SpellCastPredicate.TargetsSource` — the cast spell targets the trigger's own source
  permanent. Used by Legolas, Master Archer ("a spell that targets Legolas").
- `SpellCastPredicate.TargetsMatching(filter)` — the cast spell has ≥1 chosen target matching
  `filter` (evaluated relative to the trigger controller). Used by Legolas, Master Archer
  ("a spell that targets a creature you don't control").
- `SpellCastPredicate.NotOwnedByController` — the cast spell is owned by someone other than its
  controller (the trigger's controller, i.e. "you"). True precisely for "a spell you don't own":
  a card you cast out of exile / a graveyard that belongs to another player (Nita, Forum
  Conciliator; Gonti's-style borrow effects). Matches when the spell entity's `OwnerComponent`
  (fixed at game start, CR 108.3) differs from the controller; a spell cast from your own zones
  (owner == controller) never satisfies it.

Examples:

```kotlin
// "Whenever you cast a spell from your hand"
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)))

// "Whenever you cast an instant or sorcery from your hand"
Triggers.youCastSpell(
    spellFilter = GameObjectFilter.InstantOrSorcery,
    requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
)

// "Whenever you cast a kicked spell"
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.WasKicked))

// "Whenever you cast a spell using mana from a Treasure"
Triggers.youCastSpell(
    requires = setOf(SpellCastPredicate.PaidWithManaFromSubtype(Subtype.TREASURE)),
)

// "Whenever you cast a modal spell" (Riku of Many Paths)
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.IsModal))

// "Whenever you cast a spell with {X} in its mana cost" (Geometer's Arthropod) —
// then dig X cards: keep one, bottom the rest in a random order.
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.HasXInCost))
// payoff count: DynamicAmounts.xValueOfTriggeringSpell()

// "Whenever you cast a noncreature or Otter spell"
Triggers.youCastSpell(
    spellFilter = GameObjectFilter.Noncreature or
                  GameObjectFilter.Any.withSubtype(Subtype("Otter")),
)
```

### State change & misc

- `TurnedFaceUp` — source turns face up. Use `turnedFaceUp(binding)` for the ATTACHED-binding aura variant (Fatal Mutation).
- `CreatureTurnedFaceUp(player?)` — when a creature you control turns face up.
- `GainControlOfSelf` — you gain control of source. Built on `ControlChangeEvent(ControlChangeDirection.GAINED)`
  + `TriggerBinding.SELF` — the resident "when you gain control of this" self-trigger (Risky Move).
- `LoseControlOfWatched` — `ControlChangeEvent(ControlChangeDirection.LOST)` + `TriggerBinding.SELF`,
  used as the `trigger` of an **event-based delayed trigger** scoped to a watched permanent
  (`CreateDelayedTriggerEffect(trigger = Triggers.LoseControlOfWatched, watchedTarget = …)`): "when you
  lose control of [that permanent] this turn …". It fires on any mid-turn control change of the watched
  permanent *away from* the trigger's controller (the old controller was you). Stolen Uniform pairs it
  with `DelayedTriggerExpiry.EndOfTurn` + `fireOnce`. `EventPattern.ControlChangeEvent(direction,
  requireOpponent)` is the underlying primitive: `direction` (`ControlChangeDirection.GAINED` default /
  `LOST`) selects which side of the control change — relative to the ability's controller — the ability
  watches.
- `OpponentGainsControlOfYourPermanent` — `ControlChangeEvent(ControlChangeDirection.LOST,
  requireOpponent = true)` + `TriggerBinding.ANY`: "whenever an opponent gains control of a permanent from
  you …". A resident, battlefield-wide watcher (not entity-scoped like `LoseControlOfWatched`): it fires
  once for each permanent the ability's controller loses to an opponent (team-aware, CR 810). The trigger
  belongs to the *old* controller via look-back-in-time (CR 603.10), so it still fires for you even when the
  permanent being stolen is the ability's own source (Zidane, Tantalus Thief — a stolen Zidane still makes a
  Treasure for its old controller). `requireOpponent` (LOST only) adds the "new controller is an opponent"
  gate on top of the plain LOST direction.
- `BecomesTarget(filter?)` — source becomes target of spell/ability. The engine emits the
  underlying `BecomesTargetEvent` for both permanent targets and spell targets on the stack, but the
  trigger matches **permanent targets only** by default — "a creature you control" is a battlefield
  creature, not a creature spell. Set `includeSpellTargets = true` on the event for the "... or a
  creature spell you control" wording (Surrak, Elusive Hunter); the `filter` is then also matched
  against the spell's card data, so a `Creature` filter matches a creature spell on the stack. Ward
  never sees spell targets because it is generated only from battlefield permanents.
- `CreatureYouControlBecomesTargetByOpponent(filter?, includeSpellTargets = false)` — your creature
  gets targeted by an opponent's spell or ability. Permanent-only unless `includeSpellTargets = true`
  (Surrak), which also fires when an opponent targets a matching creature spell you control.
- `BecomesTargetByOpponent` — the self-bound counterpart of the above: source becomes the target of a
  spell or ability **an opponent controls** (Cactarantula's "Whenever this creature becomes the target
  of a spell or ability an opponent controls, you may draw a card").
- `BecomesTargetOfSpell(filter)` — a permanent matching `filter` becomes the target of a **spell**
  only (not an ability). Sets `BecomesTargetEvent(spellsOnly = true)`, which matches only when the
  targeting source is a spell on the stack (`sourceIsSpell`). ANY-bound; a filter like "a Spirit you
  control" covers both halves of "King of the Oathbreakers or another Spirit you control becomes the
  target of a spell" because the source itself matches the filter. Pair with
  `Effects.PhaseOut(EffectTarget.TriggeringEntity)` to phase out the targeted permanent.
- `PhasesIn(filter?)` — a permanent matching `filter` phases in (Rule 702.26). Matches the engine's
  `PhasedInEvent`, which `BeginningPhaseManager.performUntapStep` emits when a phased-out permanent
  returns during its controller's untap step. ANY-bound (use the filter, e.g. "a Spirit you control",
  for "King of the Oathbreakers or another Spirit you control phases in"); `TriggeringEntity` resolves
  to the phased-in permanent. King of the Oathbreakers makes a tapped token on each phase-in.
- `Transforms` — source transforms (either direction).
- `TransformsToFront` — to front face.
- `TransformsToBack` — to back face.
- `YouCycleThis` — you cycle source.
- `AnyPlayerCycles` — anyone cycles.
- `AnyPlayerTapsLandForMana` — whenever any player taps a land for mana. Use
  `landTappedForMana(player, landFilter, binding)` for "an opponent"/"you" variants or a land-type
  restriction. Fires on the manual mana-ability path only (auto-pay adds mana via the solver without
  emitting the event). Backs the "whenever a player taps a land for mana" family (Mana Flare, Heartbeat
  of Spring); the inline-static cards (Overabundance, Pulse) use the mana statics in §9 instead.
- `YouCommitCrime` — MKM crime mechanic.
- `YouGiveAGift` — Gift mechanic.
- `BecomesPlotted` — OTJ Plot (CR 718) — "when this card becomes plotted". SELF binding; fires for the
  very card that was plotted while it sits face up in exile (Aloe Alchemist). Detected by
  `TriggerDetector.detectPlottedCardTriggers` off the plot special action's `CardPlottedEvent`, since
  the card is never on the battlefield for the index loop to see.
- `becomesSaddled(filter = Any, firstTimeEachTurn = false, binding = SELF)` — OTJ Saddle (CR 702.171b)
  — "whenever this creature becomes saddled". Fires when a Saddle ability resolves on the permanent
  (`BecameSaddledEvent`); the Mount stays on the battlefield while saddled, so it matches in the regular
  battlefield index loop (`TriggerCategory.BECAME_SADDLED`). Pass `firstTimeEachTurn = true` for the
  "for the first time each turn" intervening-if (Stubborn Burrowfiend) — backed by the event's
  `firstThisTurn` flag, which is true only when the permanent wasn't already saddled when the ability
  resolved (re-saddling in the same turn reports false, since `SaddledComponent` persists until
  cleanup). Use an `ANY` binding + `filter` for "whenever a [filter] becomes saddled".
- `becomesAttached(attachmentFilter = Any, attachmentController = Any, attachedToFilter = Any, binding = SELF)`
  — "whenever an Aura/Equipment becomes attached to a permanent" (CR 603.2e). Fires from
  `PermanentAttachedEvent`, emitted at every attach site (aura ETB onto its enchant target, equip
  resolution, an aura moved onto the battlefield attached by an effect) only when newly attached —
  not on a persisting attachment, and not on phasing in/out (CR 702.26j). The triggering entity is
  the **attachment**; the host it attached to is reachable via
  `EffectTarget.AttachedToTriggeringPermanent`. `SELF` = "whenever **this** Equipment/Aura becomes
  attached" (Assimilation Aegis). `ANY` + `attachmentController = Player.You` + `attachmentFilter`
  = "whenever a [filter] you control becomes attached to …" (Eriette, the Beguiler); the
  `attachedToFilter` is matched against the host with the attaching object exposed as
  `EntityReference.Triggering`, so relative predicates like
  `manaValueAtMostEntity(EntityReference.Triggering)` ("MV ≤ that Aura's MV") resolve against it.
  Indexed under `TriggerCategory.BECOMES_ATTACHED`.
- `Valiant` — Bloomburrow Valiant trigger.
- `RoomFullyUnlocked` — Rooms — both doors unlocked.
- `OnDoorUnlocked` — single Room door unlocked.

### Life

- `YouGainLife` — you gain any life.
- `YouGainLifeFirstTimeEachTurn` — you gain life for the first time each turn (Leech Collector). Backed by `LifeGainEvent(firstTimeEachTurn = true)`, matched against `LifeChangedEvent.firstThisTurn` (computed in `DamageUtils.gainLife` before the per-turn life-gained marker is set).
- `AnyPlayerGainsLife` — anyone gains life.
- `YouLoseLife` — you lose any life.
- `AnyPlayerLosesLife` — anyone loses life.
- `AnOpponentLosesLife` — an opponent loses life (fires per opponent life-loss event; read the amount via `ContextPropertyKey.TRIGGER_LIFE_LOST`). Bloodthirsty Conqueror; Kefka, Ruler of Ruin (pair with `triggerCondition = Conditions.IsYourTurn` for "during your turn").
- `YouGainOrLoseLife` — combined life-change.
- `AnyPlayerLosesGame` — a player loses the game (CR 104.3; backed by `EventPattern.PlayerLostGameEvent`, matched against the engine's `PlayerLostEvent`). Fires for every player's loss; `Player.TriggeringPlayer` inside the effect is the loser. Narrow to one player with a `triggerCondition` — Shinryu, Transcendent Rival's "When the chosen player loses the game, you win the game" uses `triggerCondition = Conditions.TriggeringPlayerIs(Player.ChosenOpponent)` + `Effects.WinGame()`.

### The Ring

- `RingTemptsYou` — whenever the Ring tempts you (CR 701.54d). Paired with `Effects.TheRingTemptsYou()`.
- `WheneverYouChooseRingBearer` — whenever you choose a creature as your Ring-bearer (CR 701.54a–b).
  A `RingTemptedEvent` pattern with `requireBearerChosen = true`, so it fires only when the temptation
  actually designates a creature (the event's `bearerId` is non-null) — not when you control none to
  choose. Used by Call of the Ring.

### Scry / Surveil

- `WheneverYouScry` — fires once per scry resolution (CR 701.18), after the cards have
  been placed on top/bottom. Pair with `DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_SCRY_COUNT)`
  for "for each card looked at" payoffs (Celeborn the Wise, Elrond Master of Healing).
  Automatically emitted by `Patterns.Library.scry(N)`; no card has to opt in.
- `WheneverYouSurveil` — the surveil twin (CR 701.42), fired once per surveil resolution.
  Automatically emitted by `Patterns.Library.surveil(N)`. Reads the same `TRIGGER_SCRY_COUNT`
  ("cards looked at"). Used by Golbez.
- `WheneverYouScryOrSurveil` — the combined look-at-top trigger; fires once per scry **and**
  once per surveil (Matoya, Archon Elder).

### Explore (CR 701.44)

- `Triggers.creatureExplores(filter, revealedType)` — "Whenever a permanent matching `filter`
  explores." The exploring permanent is the event subject, so the binding is `TriggerBinding.ANY`
  and `filter.youControl()` resolves "you" to the observing ability's controller. `revealedType`
  (`ExploreReveal.ANY | LAND | NONLAND`) gates on the reveal outcome (CR 701.44a). Backed by
  `EventPattern.ExploredEvent`; emitted by `ExploreEffectExecutor` as `PermanentExploredEvent`
  once per explore.
  - `WheneverCreatureYouControlExplores` — ANY reveal (Merfolk Cave-Diver).
  - `WheneverCreatureYouControlExploresLand` — only when a **land** card was revealed (Nicanzil,
    Current Conductor's first ability).
  - `WheneverCreatureYouControlExploresNonland` — only when a **nonland** card was revealed
    (Nicanzil's second ability).
  - Fires even on an **empty library** (CR 701.44b — the permanent still explored): `ANY` matches,
    `LAND`/`NONLAND` do not (`revealedCardWasLand == null`).

### Library search (CR 701.23)

- `WheneverYouSearchYourLibrary` / `WheneverAnOpponentSearchesTheirLibrary` — fire once per
  library search (CR 701.23), after the found cards have moved and the library has shuffled.
  Backed by the `SearchLibraryEvent(player)` pattern + the engine `LibrarySearchedEvent`, emitted
  automatically by every search primitive (`Patterns.Library.searchLibrary` / `searchMultipleZones`
  / `eachPlayerSearchesLibrary`) via the internal `EmitLibrarySearchedEventEffect` tail — so every
  tutor, fetch, and basic-land search drives it; no card has to opt in. Under a `ForEachPlayer`
  search the tail's controller is rebound to each iterated player, so the event names the correct
  searcher. Since searching is the act of looking through the zone (CR 701.23a) and finding a card is
  not required (CR 701.23b), the trigger fires even when no card was found. The opponent-scoped
  variant is used by **Wan Shi Tong, Librarian** ("Whenever an opponent searches their library, put a
  +1/+1 counter on him and draw a card").

### Manifest Dread

- `WheneverYouManifestDread` — fires once per manifest-dread resolution (CR 701.60), after the
  chosen card is manifested face down and the other put into your graveyard. Automatically emitted
  by `Patterns.Library.manifestDread()`; no card has to opt in. Per CR 701.60b it fires even when
  the library held fewer than two cards. The card(s) put into the graveyard this way are seeded into
  the payoff's pipeline under `IterationSpace.TRIGGER_CAPTURED_COLLECTION` (the same engine-seeded
  slot batch ETB payoffs read), so a payoff that references "a card you put into your graveyard this
  way" can move it out of the graveyard — e.g. **Paranormal Analyst**:
  `MoveCollection(from = TRIGGER_CAPTURED_COLLECTION, destination = ToZone(HAND))`. The collection is
  empty when nothing was binned, making the payoff a safe no-op.
- A literal "scry 0" / "surveil 0" produces no event and fires no trigger (CR 701.18b / 701.42c);
  the trigger still fires when the library was empty and zero cards were looked at (CR 701.18d /
  701.42d).

### Saga creatures — "Enchantment Creature — Saga" (CR 714.1a)

A Summon Saga (Final Fantasy) is a permanent that is **simultaneously a creature and a Saga**: it
has power/toughness, keywords, and fights in combat while progressing through lore chapters. No
special builder is needed — just author a normal `card { }` with both a creature type line and
`sagaChapter` blocks:

```kotlin
val SummonTitan = card("Summon: Titan") {
    typeLine = "Enchantment Creature — Saga Giant"   // both CREATURE and ENCHANTMENT + Saga subtype
    power = 7; toughness = 7
    keywords(Keyword.REACH, Keyword.TRAMPLE)         // body keywords are independent of chapters
    sagaChapter(1) { effect = Patterns.Library.mill(5) }
    sagaChapter(3) {                                  // chapters may reference the saga-creature
        effect = Effects.DealDamage(                  //   itself via EffectTarget.Self
            DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power),
            EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
    }
}
```

The saga machinery is **type-line driven, not enchantment-gated**: `TypeLine.isSaga` is
`isEnchantment && hasSubtype(SAGA)`, which an Enchantment Creature satisfies. Lore accrual at the
controller's precombat main (CR 714.3c), chapter triggers (CR 714.2b), self-referential
`EffectTarget.Self` resolution in a chapter effect, and the final-chapter sacrifice SBA (CR 714.4)
all apply unchanged while the permanent is a live creature. A chapter effect's `Self` resolves to
the saga permanent (e.g. "This creature deals damage equal to its power …"). Covered by
`CreatureSagaTest` and `FinSummonSagaScenarioTest`. *(The standalone Summon Sagas all read
"Sacrifice after N", so the default CR 714.4 sacrifice is correct; no opt-out flag exists. Eikon /
Dominant back faces that "stay" instead self-exile on their final chapter, dodging 714.4 via its
"not the source of a chapter ability on the stack" clause.)*

### Saga chapter resolution (CR 714)

- `WheneverFinalChapterOfYourSagaResolves` — fires when the *final* chapter ability of a Saga you
  control finishes resolving (Tom Bombadil). The engine detects Saga chapter abilities from
  lore-counter additions, marks them, and emits `SagaChapterResolvedEvent` on resolution; this
  trigger matches the ones flagged as the final chapter. Pair with `oncePerTurn = true` for "This
  ability triggers only once each turn."
- `WheneverChapterOfYourSagaResolves` — same, but matches *any* chapter ability's resolution
  (`finalChapterOnly = false`).

### Sacrifice & counters

- `YouSacrificeOneOrMore(filter?)` — you sac ≥1 matching. Built with `binding = ANY`, which (CR-faithfully)
  **includes the source sacrificing itself**: an ANY-binding sacrifice-batch trigger fires both when *another*
  matching permanent is sacrificed and when the source permanent is itself sacrificed. The triggering entity is
  bound to the just-sacrificed permanent, so a payoff reading "that <permanent>" (its mana value / a token copy
  of it) resolves against its last-known information in the graveyard. This is exactly the wording "whenever you
  sacrifice this permanent or another <filter>" (Esoteric Duplicator) as well as the plain "whenever you sacrifice
  a <filter>" (Mayhem Devil). For the "another" exclusion use an `OTHER`-binding trigger instead.
  By default the ANY-binding form watches only the source *controller's* sacrifices ("whenever **you**
  sacrifice…"). For the "whenever **a player** sacrifices…" scope set
  `EventPattern.PermanentsSacrificedEvent(filter, byAnyPlayer = true)` (Zodiark, Umbral God — "Whenever a
  player sacrifices another creature, put a +1/+1 counter on Zodiark"): the detector then fires the trigger
  once per sacrificing player in the batch, regardless of who controls the source, binding
  `triggeringPlayerId` to that player.
- `YouSacrificeAnother(filter?)` — the **per-permanent** template "whenever you sacrifice **another**
  permanent" (Mazirek, Kraul Death Priest; Savra; Zhao, Ruthless Admiral). Built with `binding = OTHER`
  and `EventPattern.PermanentsSacrificedEvent(filter, perPermanent = true)`. Distinct from
  `YouSacrificeOneOrMore` on **two** axes: (1) *multiplicity* — it fires once for **each** matching
  permanent sacrificed, even when several are sacrificed simultaneously (CR 603.2c), so sacrificing three
  permanents fires it three times; `YouSacrificeOneOrMore` (batch) fires once per event. (2) *exclusion* —
  `OTHER` excludes the source sacrificing itself; a source sacrificed *alongside* other permanents still
  reacts to those others (fires once per other), but not to itself. The `perPermanent` flag is the general
  multiplicity switch on `PermanentsSacrificedEvent` — combine it with `binding = ANY` for the "whenever you
  sacrifice **a** permanent" wording that also counts the source itself.
- `Sacrificed` — source is sacrificed.
- `EventPattern.ExploitedEvent(player = Player.You, requireNontokenExploited = false)` — "whenever a creature you control
  exploits a creature" (CR 702.110b; the sacrifice half of the Exploit keyword). Fires once per exploited creature; the
  `exploit()` helper appends `EmitExploitedEventEffect` after the exploit sacrifice, so declining the optional sacrifice
  emits nothing. The **exploiter** identity is selected by the ability's `TriggerBinding` against the event's exploiter:
  `SELF` = "when **this** creature exploits" (rarely needed — cards bake their self-payoff into the exploit reflexive so
  it survives self-sacrifice), `ANY` = "whenever **a creature you control** exploits" (Skull Skaab — includes the source's
  own exploit), `OTHER` = "whenever **another** creature you control exploits". `player` scopes the exploiter's
  *controller*. `requireNontokenExploited = true` gates on the sacrificed creature being a **nontoken** (Skull Skaab's
  "exploits a nontoken creature") — a boolean rather than a full `GameObjectFilter` because the exploited creature is
  gone by the time the event is observed, so only its last-known token-ness (`GameEvent.ExploitedEvent.sacrificedWasToken`,
  snapshotted before the zone change) is available. See the `Exploit` keyword entry for full wiring.
- `PlusOneCountersPlacedOnYourCreature` — Hardened Scales shape (+1/+1 only).
- `countersPlacedOn(filter = Creature.youControl(), counterType = Counters.ANY, firstTimeEachTurn = true, binding = ANY, placedBy = null)`
  — fires when counters of any type (`Counters.ANY` wildcard) land on a matching permanent;
  `firstTimeEachTurn` gates it to the first counter placement on *that* permanent this turn
  (engine-tracked via `ReceivedCountersThisTurnComponent`). `binding = SELF` restricts it to the
  source permanent (the `TriggerMatcher.CountersPlacedEvent` branch honors `SELF`/`OTHER`).
  `placedBy = Player.You` restricts to counters *you* put — the `filter` constrains the permanent
  *receiving* the counters, `placedBy` constrains the *placer* (CR 122.6a). Use it for
  "Whenever **you** put one or more +1/+1 counters on **a creature**" (recipient unrestricted, so
  the "you put" scope can't come from a `.youControl()` recipient filter) — Earth Kingdom General.
  The placer is the controller of the placing effect, the entering permanent's controller (for a
  permanent entering with counters, CR 122.6a), the mover's controller (CR 122.5 — *moving* a counter
  "puts" it on the destination), or the damage source's controller (wither, CR 702.80). A few
  low-value paths carry no placer (saga lore counters, poison counters on players) and never match a
  non-null `placedBy`. Default `null` matches any placer. Triggering permanent is
  `EffectTarget.TriggeringEntity`. Stalwart Successor shape.
- `CountersPlacedOnThis` — "whenever you put one or more counters on ~" (any kind, SELF-bound).
  Aragorn, Company Leader.
- `OneOrMorePermanentsEnter(filter?, excludeSource?)` — batched ETB trigger; fires at most once per
  event batch (CR 603.3b). The `filter`'s controller predicate scopes which players' permanents
  count: no predicate means "you control" (default), `.opponentControls()` scopes to your opponents.
  `excludeSource = true` models "one or more **other** … you control enter" — the source's own entry
  never counts toward the batch (Valley Questcaller); leave it false for wordings that include the
  source ("Satoru and/or one or more other creatures…"). The
  matching members of the batch are exposed to the payoff as the pipeline collection
  `PipelineState.TRIGGER_CAPTURED_COLLECTION` — iterate them with
  `ForEachInCollectionEffect(PipelineState.TRIGGER_CAPTURED_COLLECTION, body)` where the body uses
  `EffectTarget.Self` for the current entered permanent ("for each of them, create a tapped copy of
  it" — Kambal, Profiteering Mayor).
- `OneOrMoreOpponentPermanentsEnter(filter?)` — same batched ETB trigger with the controller scope
  fixed to your opponents (sugar for `OneOrMorePermanentsEnter(filter.opponentControls())`).
- `OneOrMoreLeaveWithoutDying(...)` — batched LTB-without-dying.

### Conditional

- `NthSpellCast(n, player?)` — fires on the Nth spell cast.
- `WhenYouCastThisSpell()` — a "cast trigger" that fires on the spell's **own** cast while it is on
  the stack (`EventPattern.CastThisSpellEvent`, `binding = SELF`). Distinct from a battlefield
  `SpellCast`/`NthSpellCast` trigger that observes *other* spells: this one travels with the spell
  onto the stack and is detected only by `TriggerDetector`'s self-cast path (it is deliberately
  **not** indexed against battlefield permanents, so it never fires after the spell resolves).
  Pair with a `triggerCondition` for an intervening "if" (CR 603.4). Sage of the Skies — "When you
  cast this spell, if you've cast another spell this turn, copy this spell" — uses
  `triggerCondition = Conditions.YouCastSpellsThisTurn(atLeast = 2)` (the spell itself is already
  counted, so "two or more" = "another spell") and `Effects.CopyTargetSpell(TriggeringEntity)` to
  copy itself; copying a permanent spell yields a token (CR 707.10f), and the copy isn't cast so it
  doesn't re-trigger (CR 707.10).
- `Expend(threshold)` — Expend N (CLB mechanic).

### Delayed & granted triggers

- `DelayedTriggeredAbility` — registered now, fires at a specific future step (Astral Slide).
- `Effects.GrantTriggeredAbilityEffect` — grant a triggered ability for a duration; `GrantTriggeredAbilityExecutor` uses
  projected state and supports leaves-battlefield-to-zone triggers.
- `CreateDelayedTriggerEffect(step, effect, fireOnPlayer, timing, …)` —
  the data-side facade. Two orthogonal axes control *whose / which* turn fires the trigger:
  - `fireOnPlayer: EffectTarget?` — the single "whose turn" gate. Resolved to a concrete player
    at scheduling time; only matches when that player is active. Defaults to `null` (no player
    gate — fires on the next matching step of *any* turn). Two common shapes:
    - `EffectTarget.PlayerRef(Player.You)` — only the controller's turn ("at the beginning of
      *your* next end step"; Dragonhawk, Kav Landseeker, Meandering Towershell).
    - `EffectTarget.PlayerRef(Player.TriggeringPlayer)` — the triggering/damaged player's turn
      ("at the beginning of *their* next [step]"; Nafs Asp's "that player loses 1 life at the
      beginning of their next draw step unless they pay {1}").
    The resolved player id is also re-exposed to the inner `effect` as `triggeringPlayerId` /
    `triggeringEntityId` when the trigger fires, so `Player.TriggeringPlayer` inside the inner
    effect resolves to the same player.
  - `timing: DelayedTriggerTiming` — gates *which* turn is the earliest eligible one:
    - `CURRENT_TURN_OR_LATER` (default) — no turn floor; the next upcoming occurrence of `step`,
      which may be the current turn. (Astral Slide exile-until-end-step.)
    - `NEXT_END_STEP` — "at the beginning of your next end step": defers to next turn only if the
      controller's current-turn end step has already begun (END/CLEANUP); otherwise the current
      turn's end step qualifies. (Dragonhawk, Fate's Tempest.)
    - `NEXT_TURN` — stricter "on your next turn"-style timing: the current turn never qualifies
      regardless of step. Pair with `fireOnPlayer = PlayerRef(Player.You)` to land on the
      controller's upcoming own turn rather than an intervening opponent turn. (Kav Landseeker.)
- **Event-based delayed triggers** — pass `trigger = <TriggerSpec>` (instead of `step`) and the
  delayed ability fires whenever a matching *event* occurs, staying resident until `expiry`
  (`DelayedTriggerExpiry.EndOfTurn`) removes it. Supported events include `DealsDamageEvent`,
  `ZoneChangeEvent`, the internal `DamagePreventedEvent`, and the attack-declaration events
  `YouAttackEvent` / `AttackEvent`. There are two ways to scope which events match:
  - **Entity-scoped** — set `watchedTarget` to bind the trigger to one concrete entity (resolved at
    creation time): "when **that** creature deals combat damage / dies this turn" (Long River Lurker,
    Deflecting Palm). Only `DealsDamageEvent` (scoped on the damage source) and `ZoneChangeEvent`
    (scoped on the moving entity) use this; the spec's `GameObjectFilter` is *not* applied — the
    watched entity is the whole scope.
  - **Recipient-scoped** — set `watchedRecipient` to bind a `DealsDamageEvent` trigger to the damaged
    *recipient* (rather than the source): "whenever a creature you control deals combat damage to
    **that player** this turn, …" (Great Train Heist's Treasure-on-hit mode). The target is resolved at
    creation time (e.g. `EffectTarget.ContextTarget(0)` for a just-chosen target opponent), so the
    trigger fires only for hits to that specific player. The `TriggerSpec`'s `recipient` / `sourceFilter`
    still apply on top (use `RecipientFilter.AnyPlayer` + `sourceFilter = Creature.youControl()`).
    Orthogonal to `watchedTarget` (source scope) — set at most one.
  - **Filter-scoped** — leave `watchedTarget` null and let the `TriggerSpec`'s `GameObjectFilter` +
    `TriggerBinding` describe the group, exactly like a battlefield-resident trigger. Use this for
    "whenever a creature you control enters this turn, …" (Thunder of Unity chapters II/III):
    `trigger = Triggers.entersBattlefield(GameObjectFilter.Creature.youControl(), binding = ANY)`.
    Matching delegates to the same `TriggerMatcher` the battlefield triggers use, so the filter's
    type **and** controller predicates are honored — it fires only for *your* creatures, not every
    permanent that enters. (`YouAttackEvent` / `AttackEvent` are always filter-scoped this way.)
  - `fireOnce = true` makes it a **one-shot**: it's consumed the first time it fires, then gone —
    "when you **next** [event] this turn". Combine with `trigger = Triggers.YouAttack` for the
    common "when you next attack this turn, …" template (All-Out Assault: untap each creature you
    control on your next attack). With `fireOnce = false` (default) it fires on every matching event
    until expiry (double-strike combat damage). One-shot consumption happens when the trigger goes
    on the stack (`TriggerProcessor`), so a second matching event the same turn won't re-fire it.
  - `expiry` — when the resident delayed trigger is removed. `DelayedTriggerExpiry.EndOfTurn`
    (default) drops it in the end-of-turn cleanup ("this turn" riders). `DelayedTriggerExpiry.Never`
    keeps it across turns until it fires (pair with `fireOnce = true`) or the game ends — for
    watch-a-permanent-until-it-leaves reflexive triggers that aren't turn-scoped, e.g. Zenos yae
    Galvus's "When the chosen creature leaves the battlefield, transform Zenos yae Galvus"
    (`trigger = Triggers.LeavesBattlefield, watchedTarget = <chosen>, fireOnce = true,
    expiry = DelayedTriggerExpiry.Never`). `EffectTarget.Self` inside the effect resolves to the
    delayed trigger's source (the permanent that created it).
  - `targetRequirement = <TargetRequirement>` — a target chosen **each time** the delayed trigger
    fires, exposed to `effect` as `EffectTarget.ContextTarget`. Use for delayed triggers whose payoff
    targets: Rediscover the Way chapter III installs
    `CreateDelayedTriggerEffect(trigger = Triggers.YouCastNoncreature, fireOnce = false,
    expiry = EndOfTurn, targetRequirement = Targets.CreatureYouControl,
    effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE))` — "whenever you cast a noncreature spell
    this turn, target creature you control gains double strike". Works on both event-based and
    step-based delayed triggers; null (default) for non-targeting delayed triggers.
  - **Amount snapshot at creation time.** When `effect` is an `AddManaEffect` / `AddColorlessManaEffect`
    carrying a non-`Fixed` `DynamicAmount` that reads from the *current* resolution context — e.g.
    `DynamicAmounts.targetManaSpent(0)` (`EntityProperty(Target(0), ManaSpent)`) — the executor
    evaluates that amount **now** and bakes it into a `DynamicAmount.Fixed` literal, exactly as it
    bakes `ContextTarget(n)` target references into `SpecificEntity`. This is required for "at the
    beginning of your next main phase, add an amount of {C} equal to the amount of mana spent to cast
    that spell" (**Mana Sculpt**): the referenced spell is gone by the time the delayed trigger fires,
    so a lazy read would yield 0 — author the gated delayed-trigger creation *before* the counter so
    the target spell is still on the stack when the snapshot is taken. Already-`Fixed` amounts pass
    through untouched.
  - **Context-target baking covers `CreateTokenCopyOfTargetEffect`.** A delayed "at the beginning of the
    next end step, create a token that's a copy of that <permanent>" whose `target` is `TriggeringEntity` /
    `ContextTarget(n)` / `Self` is baked into a `SpecificEntity` at creation time (same mechanism that bakes
    `MoveToZoneEffect` / `SacrificeTargetEffect` / `AddCountersEffect` targets). The copied permanent is
    typically gone (sacrificed → graveyard) by the time the trigger fires; the token-copy executor then reads
    its printed characteristics from the captured entity's `CardComponent` via last-known information. Used by
    **Esoteric Duplicator** ("whenever you sacrifice this artifact or another artifact … create a token that's a
    copy of that artifact").

---

## 8.5 State-triggered abilities (CR 603.8)

A **state-triggered ability** fires whenever a game-state condition becomes true, rather
than in response to a `GameEvent`. The engine polls the condition at every priority pass
and emits the trigger on each false → true transition. Once it has fired, a per-permanent
`StateTriggerLatchesComponent` latch suppresses re-firing until the condition next
evaluates false again (CR 603.8).

> **Latch note.** The printed CR 603.8 resets after the ability *leaves the stack* and
> re-triggers if the condition is still true. This engine resets on the condition next
> being *false* instead — equivalent for "sacrifice this creature" cards (source leaves,
> condition clears) but divergent for a state trigger that leaves source and condition
> intact. No such card exists yet; reset-on-leaves-the-stack should be wired before one is
> authored.

```kotlin
stateTriggeredAbility {
    condition = Conditions.YouControl(
        GameObjectFilter.Land.withSubtype("Island"),
        negate = true,
    )
    effect = Effects.SacrificeTarget(EffectTarget.Self)
    description = "When you control no Islands, sacrifice this creature"
}
```

- `condition` — any `Condition`. Evaluated with the source permanent as
  `EffectContext.sourceId`; `Player.You` references resolve to the source's controller.
- `effect` — fires when the condition transitions false → true. Resolves on the stack
  like an ordinary triggered ability.
- `description` (optional) — overrides the auto-generated text.

Used for Dandân, Island Fish Jasconius, Merchant Ship ("When you control no Islands,
sacrifice this creature"), Serendib Djinn ("When you control no lands, sacrifice this
creature"), and similar "static cleanup" wording in early sets. Differs from an
intervening-if triggered ability — there is no event to gate on; the engine watches the
condition itself.

`stateTriggeredAbility { }` is also available **inside a `face { }` block** of a split/Room
card. The poller folds in the state triggers of every currently-**unlocked** Room face
(`RoomFaceStatics.activeStateTriggeredAbilities`), so a locked door's state trigger stays inert
until its door is unlocked — Promising Stairs (Central Elevator // Promising Stairs): "You win the
game if there are eight or more different names among unlocked doors of Rooms you control"
(`condition = Compare(DynamicAmount.UnlockedDoors(Player.You, distinctNames = true),
ComparisonOperator.GTE, DynamicAmount.Fixed(8))`, `effect = Effects.WinGame(...)`).

---

## 9. Static abilities

```kotlin
staticAbility {
    // The whole continuous modification is the `ability`; the affected objects (filter),
    // layer, and duration all live on the StaticAbility itself, not on the block.
    ability = GrantKeyword(
        Keyword.FLYING,
        GroupFilter.CreaturesYouControl.withSubtype("Soldier")
    )
    condition = Conditions.YouControl(Filters.Swamp)   // optional intervening condition
}
```

> A static ability is a continuous modification, so `ability = <StaticAbility>` is the only
> path — there is no `effect =` shorthand. For a permanent that grants several modifications,
> use one `staticAbility { }` block per `StaticAbility` (e.g. an Equipment that gives +2/+1 and
> trample is two blocks).

**`Modification` options**

- `AddSubtype(subtype)` — add a subtype to matching creatures.
- `RemoveSubtype(subtype)` — strip a subtype.
- `ReplaceSubtypes(subtypes)` — set the subtype list outright.
- `ModifyStats(p, t)` — `±P/±T`.
- `SetPower(p)` — overwrite power.
- `SetToughness(t)` — overwrite toughness.
- `SetStats(p, t)` — overwrite both.
- `GrantKeyword(keyword)` — grant a keyword.
- `RemoveKeyword(keyword)` — remove a keyword.
- `GrantProtection(color)` — grant protection from a color.
- `Custom(...)` — escape hatch for one-off modifications.

**Composite static abilities**

- `ModifyStatsForCreatureGroup` — lord-style P/T booster targeting a group.
- `SetBasePowerToughnessDynamicStatic(power, toughness, filter = GroupFilter.source())` —
  characteristic-defining ability (CDA): *sets* base power and toughness each to a `DynamicAmount`,
  recomputed continuously at projection (Layer 7b SET_VALUES). Distinct from
  `GrantDynamicStatsEffect`, which is a Layer 7c additive *bonus*: use this when the dynamic value
  *is* the printed base P/T (a later base-setting effect overwrites it rather than stacking). For
  `*/*` creatures and token CDAs — e.g. Beau ("power and toughness each equal to the number of lands
  you control"), Tarmogoyf-style stats. Pair with `DynamicAmounts.landsYouControl()` /
  `AggregateBattlefield(...)`. (Bonny Pall, Clearcutter's Beau token)
- `GrantKeywordByCounter` — Aurification — keyword based on counters present.
- `AddCreatureTypeByCounter` — subtype based on counters present.
- `SetEnchantedLandType(landType)` — "Enchanted land is an Island" — replaces the enchanted
  land's basic land types with a fixed type (Rule 305.7). (Sea's Claim)
- `SetLandTypesForGroup(filter, landTypes)` — the **group** counterpart of `SetEnchantedLandType`
  (and the "set/replace" counterpart of `GrantAdditionalTypesToGroup`, which *adds* and keeps
  abilities): "[filter] are [types]. (They lose all other land types and abilities and have the new
  type's mana ability.)" Realizes CR 305.7 over a whole group of lands as two continuous effects —
  Layer 4 `SetBasicLandTypes` (replace the basic land subtypes) + Layer 6 `RemoveAllAbilities`
  (strip printed abilities). The new type's **intrinsic** mana ability (Mountain → "{T}: Add {R}")
  is derived from the projected subtype by `IntrinsicManaAbilities` and survives the ability
  suppression, so the lands still tap for the appropriate color. Gate behind a
  `ConditionalStaticAbility` for conditional variants. Blood Moon / Magus of the Moon
  (`filter = GroupFilter(GameObjectFilter.NonbasicLand)`, `landTypes = setOf("Mountain")`); Zhao,
  the Moon Slayer gates the same on `Conditions.SourceCounterCountAtLeast(Counters.CONQUEROR, 1)`.
- `SetEnchantedLandTypeFromChosen` — "Enchanted land is the chosen type" — same, but reads the
  type from the source's `ChosenLandTypeComponent` (paired with
  `EntersWithChoice(ChoiceType.BASIC_LAND_TYPE)`). Chosen-value counterpart to
  `SetEnchantedLandType`, mirroring `GrantChosenColor`/`GrantColor`. (Phantasmal Terrain)
- `GrantLandwalkOfChosenType(filter = attachedCreature())` — "Enchanted creature has landwalk of
  the chosen type" — grants the landwalk keyword matching the source's `ChosenLandTypeComponent`
  (Plains→Plainswalk, Island→Islandwalk, …) at projection time. Chosen-value counterpart to
  `GrantKeyword`; pair with `EntersWithChoice(ChoiceType.BASIC_LAND_TYPE)`. (Traveler's Cloak)
- `GrantProtectionFromControlledColors(filter = attachedCreature())` — "[filter] have protection from
  the colors of permanents you control" — grants the affected creature(s) protection from every color
  among the permanents the source's controller controls, recomputed at projection (Layer 6, after
  Layer 5 colors) so it tracks the board in real time. Colorless permanents add no color. (Pledge of
  Loyalty)
- `GrantProtectionFromCardType(cardType, filter = attachedCreature())` — "[filter] have protection from
  [card type]s" (e.g. *protection from instants*, *protection from sorceries*). Projects the keyword
  `PROTECTION_FROM_CARDTYPE_<TYPE>`; the engine enforces the *targeting* leg (a spell/permanent of that
  card type can't target the creature), which is the only DEBT leg that matters for instants/sorceries.
  Pair two of these for the two-type wording. (Sword of Wealth and Power)
- `GrantHexproofFromMonocoloredToGroup(filter = attachedCreature())` — "[filter] have hexproof from
  monocolored" — adds the projected keyword `HEXPROOF_FROM_MONOCOLORED`, which blocks targeting by
  monocolored (exactly one color, CR 105.2) spells and abilities opponents control. Colorless and
  multicolored sources are unaffected; the controller can still target their own creatures. (Dragonfire
  Blade)
- `CantBeTargetedBySourceTypeAbilities(sourceType, filter = attachedCreature())` — "[filter] can't be the
  target of abilities from [sourceType] sources" — hexproof keyed to a source *card type* (e.g.
  `CardType.ARTIFACT`) rather than a controller or color. Projects the keyword
  `CANT_BE_TARGETED_BY_CARDTYPE_<TYPE>_SOURCE_ABILITIES` (mirroring the `PROTECTION_FROM_CARDTYPE_<TYPE>`
  idiom); the engine (`TargetFinder`/`StackResolver`, via `SourceTypeTargeting`) blocks an *ability* (not a
  spell) whose source has that card type, **regardless of who controls the source**. Deliberately not
  protection-from-[type] (which would also stop equipping/enchanting/blocking/damage). (Artifact Ward, with
  `CardType.ARTIFACT`)
- `GrantCardType(cardType, filter)` / `RemoveCardType(cardType, filter)` — Layer 4 type-changing statics that add or
  remove a card type (e.g. `"CREATURE"`). `RemoveCardType` backs Impending's "isn't a creature while it has a time
  counter" (wrapped in a `ConditionalStaticAbility`); reuse it for any "it's no longer a [type]" effect.
- `GrantSubtype(subtype, filter)` — Layer 4 type-changing static that adds a **fixed** creature subtype to the group,
  in addition to their other types ("is a Knight in addition to its other types"). (Dub)
- `GrantChosenSubtype(filter, includeControlledSpells = false, includeOwnedCardsOutsideBattlefield = false)` — Layer 4
  type-changing static that adds the creature type **chosen as the source entered** (read from the source's
  `CastChoicesComponent`) to the group, in addition to their other types. Chosen-value counterpart to `GrantSubtype`,
  mirroring `GrantChosenColor`/`GrantColor`; pair with `EntersWithChoice(ChoiceType.CREATURE_TYPE)`. This is the
  Conspiracy / Xenograft mechanic. The `filter` half is normal Layer 4 battlefield projection ("Creatures you control
  are the chosen type"). The two cross-zone flags extend the grant beyond the battlefield (the Conspiracy / Leyline-of-
  Transformation clause "the same is true for creature spells you control and creature cards you own that aren't on the
  battlefield"): `includeControlledSpells` reaches creature spells the controller controls on the stack, and
  `includeOwnedCardsOutsideBattlefield` reaches creature cards the controller owns in hand/library/graveyard/exile/
  command. Because Layer 4 projection only touches the battlefield, those flags are honored by a separate overlay —
  `ProjectedState.crossZoneGrantedSubtypes`, consulted by every non-battlefield subtype read-site in `PredicateEvaluator`
  and `SelectFromCollectionExecutor` — so a granted type drives type-matters checks everywhere ("target Zombie spell",
  "return target Zombie card from your graveyard", search filters). Leave both flags `false` for battlefield-only effects
  like Xenograft. (Leyline of Transformation)
- `TransformPermanent(setCardTypes, setSubtypes, setColors?, setName?, clearSubtypes, filter)` — Layer 3/4/5 "becomes a
  whole new identity" (Sugar Coat, Darksteel Mutation, Witness Protection, Imprisoned in the Moon). A non-empty `setSubtypes` replaces all
  subtypes; an empty `setSubtypes` leaves subtypes alone **unless** `clearSubtypes = true`, which replaces them with
  none ("has no subtypes" — the Enduring return strips Sheep/Glimmer). `setColors = null` keeps colors. `setName`
  (default `null` = don't change) overwrites the object's name at Layer 3 (CR 612.8 — "loses any names it had and has
  only the specified name"), lowering to `Modification.SetName`; only supertypes (e.g. Legendary) survive a rename,
  so two same-controller permanents renamed to the same name can trigger the legend rule (Witness Protection: "named
  Legitimate Businessperson"). `ClientCard.name` and `LegendRuleCheck` both prefer the projected name over the base
  `CardComponent.name` when one is active.
- `SetName(name, filter)` — a standalone static that overrides the matching permanents' **name** with a fixed string
  (Layer 3 / TEXT; CR 612 / 613.1c — setting a name is a text-changing effect). Lowers to the same `Modification.SetName`
  as `TransformPermanent.setName`, exposed by `ProjectedState.getName` and surfaced to the client by
  `ClientStateTransformer`. The fixed-name half of the "becomes a 1/1 X **named Y**" composite — pair with
  `TransformPermanent` + `SetBasePowerToughnessStatic` + `LoseAllAbilities` + `GrantActivatedAbility`. Honest Work:
  "Enchanted creature ... is a Citizen with base power and toughness 1/1 and '{T}: Add {C}' named Humble Merchant."
- `ConditionalStaticAbility` — static gated by a runtime `Condition`. A conditional wrapping a *multi-effect* ability
  (e.g. `TransformPermanent`) lowers through the plural converter and gates every resulting effect on the condition.
- `CantBeTurnedFaceUp(filter)` — matching permanents can't be turned face up (Layer 6; projects a
  `cantBeTurnedFaceUp` flag read by the turn-face-up handler/enumerator). Only meaningful while the
  permanent is face down (a face-up permanent can't be "turned face up"), so it's applied
  unconditionally. Unable to Scream: "As long as enchanted creature is face down, it can't be turned
  face up."
- `CantReceiveCounters(filter)` — matching permanents can't have counters put on them (projects the
  `AbilityFlag.CANT_RECEIVE_COUNTERS` flag).
- `CantBeSacrificed(filter)` — matching permanents can't be sacrificed (projects the
  `AbilityFlag.CANT_BE_SACRIFICED` flag, honored by the sacrifice executor — a sacrifice that can't
  happen simply no-ops). Wrap in `ConditionalStaticAbility` for time-restricted forms, e.g. Zurgo,
  Thunder's Decree: `ConditionalStaticAbility(CantBeSacrificed(GroupFilter(Token.youControl().withSubtype("Warrior"))), IsInStep(listOf(Step.END)))`.
- `GrantKeyword(AbilityFlag.CANT_BE_ENCHANTED.name, filter)` — matching permanents can't be enchanted
  (CR 303.4): an Aura can't legally target them. Honored in `TargetValidator` at Aura-cast/activation
  target legality (only the targeting step — effects that move/attach an Aura without targeting are not
  covered). (Guardian Beast)
- `GrantKeyword(AbilityFlag.CANT_GAIN_CONTROL.name, filter)` — other players can't gain control of
  matching permanents. Honored in the control-change executors (`GainControl`, `GainControlByMost`,
  `ExchangeControl` — an exchange where either side can't be gained control of fails entirely); the
  controller keeping their own permanent is an unaffected no-op. (Guardian Beast)
- `AssignDamageEqualToToughness(filter, onlyWhenToughnessGreaterThanPower)` — static: matching creatures
  assign combat damage equal to their toughness rather than their power (Doran the Siege Tower, Bark of
  Doran). `CombatDamageUtils.getAssignedCombatDamage` consults it. For the **turn-scoped, granted** form
  (Bill the Pony: "Until end of turn, target creature you control assigns combat damage equal to its
  toughness rather than its power"), grant the `AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS` flag via
  `Effects.GrantKeyword(AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS, target, duration)`; the same combat
  util reads it from projected keywords (unconditional — no toughness > power gate).
- **Untap-step restriction flags** — granted via `GrantKeyword(AbilityFlag.X.name)` and read by the untap
  step (`BeginningPhaseManager`) off projected keywords, so they vanish when the granting source leaves play:
  - `AbilityFlag.DOESNT_UNTAP` — "doesn't untap during its controller's untap step" (Charmed Sleep,
    Temporal Distortion's hourglass-counter form). `Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, target,
    duration)` works on **any** battlefield permanent, not only creatures (the grant executor no longer
    requires a creature target), so it covers noncreature-artifact targets. For **imposed untap
    suppression that lasts only while the source stays tapped** — Phyrexian Gremlins, "{T}: Tap target
    artifact. It doesn't untap during its controller's untap step for as long as Phyrexian Gremlins
    remains tapped" — compose `Effects.Tap(target) then Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP,
    target, Duration.WhileSourceTapped("…"))`: the one-way `WhileSourceTapped` latch drops the grant the
    instant the source untaps.
  - `AbilityFlag.MAY_NOT_UNTAP` — controller may choose not to untap it (Everglove Courier, and the
    Antiquities self-optional untap-skip on Phyrexian Gremlins / Ashnod's Battle Gear / Tawnos's Weaponry).
  - `AbilityFlag.REMOVE_COUNTER_TO_UNTAP` — "If this would untap during your untap step, remove a +1/+1
    counter from it instead. If you do, untap it." (Bewitching Leechcraft). During the controller's untap
    step the engine tries to remove a +1/+1 counter; the permanent untaps **only if** one was removed,
    otherwise it stays tapped (and a `CountersRemovedEvent` + `UntappedEvent` are emitted when it does).
    Engine-wired through `untapOrConsumeStun` (`rules-engine/core/UntapHelpers.kt`), which applies it only on
    the natural untap step (callers pass projected state); explicit "untap target permanent" effects and
    other players' untap steps (Seedborn Muse) pass `projected = null` and never apply it — matching the
    "during **your** untap step" wording. Stacks after the stun-counter replacement (CR 122.1d, checked first).
- Untap-during-other-players'-untap-steps statics (read by `BeginningPhaseManager.performUntapStep`,
  which untaps the chosen permanents for each non-active player after the active player's normal untap):
  - `UntapDuringOtherUntapSteps` — untap **all** permanents you control (Seedborn Muse).
  - `UntapFilteredDuringOtherUntapSteps(filter)` — untap each permanent you control matching `filter`
    (Ivorytusk Fortress).
  - `UntapSelfDuringOtherUntapSteps` — untap **only the source permanent itself** ("Untap this artifact
    during each other player's untap step" — Bender's Waterskin). Guarded on the source still being tapped,
    so it never double-untaps / double-consumes a stun counter alongside the broad/filtered variants.
- `UntapLimitPerStep(filter, max)` — global untap-count cap, "Players can't untap more than `max` `filter`
  during their untap steps" (Damping Field — `filter = GameObjectFilter.Artifact`, `max = 1`). Read by
  `BeginningPhaseManager` for **every** player's untap step regardless of who controls the source: when a
  player has more matching permanents that would untap than the cap allows, the engine raises the same
  keep-tapped decision used by `MAY_NOT_UNTAP` with `minSelections = (matching − max)`, so the player keeps
  the excess tapped and chooses which one untaps. Multiple copies do not stack to a stricter cap unless one
  names a smaller `max` (most restrictive per filter wins). Inert when the player has `≤ max` matching
  permanents tapped.
- `MustBlock(filter = source())` — matching creatures must block each combat if able (Grand Melee).
- `MustBeBlocked(allCreatures = false, filter = null)` — static: a creature must be blocked while
  active — "if able" (≥1 blocker, default) or by **all** able blockers (`allCreatures = true`,
  Lure-style). Static counterpart of `MustBeBlockedEffect`; `BlockPhaseManager` honors it alongside
  the floating must-be-blocked modifications. With `filter = null` (default) the requirement applies
  to the ability's own **source**; wrap in `ConditionalStaticAbility(_, condition)` for the gated
  form (Frodo Baggins: `ConditionalStaticAbility(MustBeBlocked(), Conditions.SourceIsRingBearer)`).
  Set `filter` to project the requirement onto a **different** creature, resolved relative to the
  static's source — e.g. an Equipment: "equipped creature … must be blocked if able" (The Masamune)
  uses `MustBeBlocked(filter = GroupFilter.attachedCreature())`. Source-relative scopes
  (`attachedCreature()`, `source()`) resolve correctly; a `Battlefield`-scope filter matches every
  attacker. No separate "while attacking" gate is needed — must-be-blocked only bites while the
  creature attacks.
- `CantBeBlockedByMoreThan(maxBlockers)` — static cap on how many creatures may block the source (CR
  509.1b). For the **turn-scoped, granted** form (Glorfindel, Dauntless Rescuer: "can't be blocked by
  more than one creature each combat this turn"), grant `AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE`
  via `Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE, target, duration)`;
  `BlockPhaseManager.validateMaxBlockersRequirements` reads the projected flag (cap = 1) alongside the
  printed `CantBeBlockedByMoreThan` static, taking the smaller cap.
- `CantBlockCreaturesWithGreaterPower(filter = source())` — blocker-side evasion (Spitfire Handler): this
  creature can't block creatures whose projected power exceeds its own.
- `CantBeBlockedByCreaturesWithLessPower(filter = source())` — attacker-side dual (Formation Breaker): this
  creature can't be blocked by creatures whose projected power is less than its own. Resolved by
  `CantBeBlockedByCreaturesWithLessPowerRule`; both sides use projected power, so a P/T buff raises the
  threshold.
- `Effects.CreatePermanentEmblem(...)` — emblem with static abilities (planeswalker ultimates).
- `AttackTax(amountPerAttacker: DynamicAmount, condition: Condition? = null)` — Propaganda / Ghostly
  Prison / Windborn Muse / Collective Restraint. Per-attacker generic-mana tax for attacking the
  source's controller (and their planeswalkers); the amount is a `DynamicAmount` so it can scale with
  state (e.g., `DynamicAmounts.domain()` for "{X} where X is your domain"). Evaluated with the source
  permanent's controller as "you". The optional `condition` gates the whole tax on the source's own
  state, evaluated with the source as "you"/source — e.g. Archangel of Tithes
  (`condition = Conditions.SourceIsUntapped`, "As long as this creature is untapped, …"); when null
  the tax is always active. When `totalTax > 0`, the engine pauses `DeclareAttackers` for a
  `SelectManaSourcesDecision` *before* tapping any mana — declining is a clean no-op that leaves the
  player in `DECLARE_ATTACKERS` to re-declare.
- `BlockTax(amountPerBlocker: DynamicAmount, condition: Condition? = null)` — Archangel of Tithes'
  second ability ("creatures can't block unless their controller pays {1} for each of those
  creatures"). A *global* per-blocker generic-mana tax: while any permanent with this ability — whose
  optional `condition` holds — is on the battlefield, every declared blocker is taxed `amountPerBlocker`
  (multiple sources stack). The optional `condition` gates the tax on the source's own state — e.g.
  Archangel uses `Conditions.SourceIsAttacking` ("As long as this creature is attacking, …"). Block
  declaration pauses for the same mana-source confirmation as the attack tax. The pre-existing
  per-creature-type block tax (Whipgrass Entangler) uses `AttackBlockTaxPerCreatureType` floating
  effects instead.
- `CantBeAttackedWithout(keyword, attackerFilter = null)` — Form of the Dragon-style "Creatures
  without flying can't attack you." defender-side restriction. Optional `attackerFilter` narrows
  which attackers are restricted (evaluated with the source permanent as predicate source, so
  chosen-color/subtype predicates resolve against it) — e.g. Teferi's Moat:
  `CantBeAttackedWithout(Keyword.FLYING, GameObjectFilter.Creature.sharingChosenColorWithSource())`.
- `CantAttackUnlessCoAttacker(coAttackerFilter, filter = source)` — "This creature can't attack
  unless [a creature matching coAttackerFilter] also attacks" (Scarred Puma). Unlike
  `CantAttackUnless` (which is defender-relative), this depends on the whole proposed attacker
  group, so it's validated against the other declared attackers at declaration time (CR 508.1c,
  projected state; self never counts as its own co-attacker).
- `CantBlockUnlessCoBlocker(coBlockerFilter, filter = source)` — the blocking sibling: "This
  creature can't block unless [a creature matching coBlockerFilter] also blocks." Validated against
  the whole proposed blocker group at declaration time (CR 509.1b, `BlockPhaseManager`); the
  co-blocker need not block the same attacker, and self never counts as its own co-blocker. Pass
  `GameObjectFilter.Creature` for the bare "can't block alone" form. Combined with
  `CantAttackUnlessCoAttacker` it models "can't attack or block alone" (Toby, Beastie Befriender's
  Beast token). Both restriction families are read from the card definition *and* from
  `grantedStaticAbilities`, so they work on tokens (which have no `CardDefinition`) —
  `CreateTokenEffect.staticAbilities` are granted per-token for exactly this reason.
- `AttackerCountLimit(maxAttackers)` / `BlockerCountLimit(maxBlockers)` — global combat caps
  (Dueling Grounds — "No more than one creature can attack/block each combat"). Constrain the
  *total* declared attacker/blocker set across all players, not a single creature, so they are
  enforced as a whole-declaration check in `AttackPhaseManager`/`BlockPhaseManager` rather than a
  per-creature rule. While any permanent with the ability is on the battlefield, declaring more
  than the smallest cap is rejected. (`BlockerCountLimit` counts distinct blocking creatures.)
- `AdditionalETBOrLTBTriggers(filter, mustBeYouControl = true, directions = setOf(ENTERING))` —
  the Panharmonicon family (CR 603.2d "triggers additional times"). When a permanent matching
  `filter` crosses the battlefield boundary in one of `directions`, triggered abilities of
  permanents controlled by this ability's controller that fired from that event trigger an
  additional time per copy. Default `{ENTERING}` covers Panharmonicon / Naban / Traveling Chocobo;
  `mustBeYouControl = false` drops the "X you control" restriction on the cause (Starfield
  Vocalist); adding `BattlefieldDirection.LEAVING` covers Gandalf the White's "entering or leaving
  the battlefield" wording. `TriggerDetector.duplicateETBOrLTBTriggers`; additive across copies.
- `AdditionalSourceTriggers(sourceFilter, excludeSelf = true, alsoSource = false, condition = null)` —
  Twinflame Travelers: all triggered abilities of permanents matching `sourceFilter` you control trigger
  an additional time (not just ETB). Set `excludeSelf = false` for "a permanent you control" wording that
  includes the source itself (Fractured Realm). `alsoSource = true` *also* doubles the doubler's own
  triggers regardless of the filter — the "a triggered ability of ~ **or** …" wording where the source is
  one of the doubled objects. `condition` gates the whole ability against the doubler source ("As long as
  ~ is equipped, …"); a `null` condition always applies. Cloud, Midgar Mercenary combines all three:
  `AdditionalSourceTriggers(sourceFilter = Artifact.withSubtype("Equipment").attachedToSource(),
  alsoSource = true, condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped()))`.
  `TriggerDetector.duplicateSourceTriggers` (and `ActivateAbilityHandler` for triggered mana abilities).
- `AdditionalAttackTriggers(attackerFilter = GameObjectFilter.Any)` — Windcrag Siege (Mardu): the
  attack-cause analogue of `AdditionalETBOrLTBTriggers`. If a creature matching `attackerFilter`
  being declared as an attacker causes an attack-related triggered ability ("whenever a creature
  attacks" / "whenever you attack") of a permanent you control to trigger, that ability triggers an
  additional time. `TriggerDetector.duplicateAttackTriggers`; additive across copies.
- `AdditionalDeathTriggers(attachedCreature = false, permanentsYouControl = null, includeEmblems = false)`
  — the **death-cause** analogue: if a creature dying (put into a graveyard from the battlefield,
  continuous effects included) causes a **death/leave-the-battlefield** triggered ability within
  scope to trigger, that ability triggers an additional time. Scope: `permanentsYouControl` (a
  filter) for "a permanent you control" (Teysa Karlov = `AdditionalDeathTriggers(permanentsYouControl
  = GameObjectFilter.Any)`); `attachedCreature = true` for "this creature" on an Equipment/Aura (The
  Masamune, modelled as an equipment-level doubler scoped to the equipped creature); `includeEmblems
  = true` for "an emblem you own". Only death/leave triggers are doubled — abilities responding to the
  event that *caused* the death (e.g. "whenever you sacrifice a creature") are not (their EventPattern
  isn't a battlefield-exit `ZoneChangeEvent`). `TriggerDetector.duplicateDeathTriggers`; additive
  across copies (N doublers → N+1 firings). Limitation: a scoped source's own "when this creature
  dies" trigger fired by *itself* dying isn't doubled (post-death trigger detection no longer exposes
  the doubler's attachment) — the same constraint `AdditionalSourceTriggers` has.

**Spell cost statics — `ModifySpellCost`**

Replaces the per-shape cost classes. Use directly as the `ability` of a `staticAbility { }` block.

```kotlin
staticAbility {
    ability = ModifySpellCost(
        target = SpellCostTarget.YouCast(GameObjectFilter.Any),
        modification = CostModification.ReduceGeneric(2),
        gating = CostGating.NthOfTypePerTurn(2),
    )
}
```

- `target: SpellCostTarget` — `SelfCast`, `YouCast(filter)`, `AnyCaster(filter)`,
  `OpponentsCastTargeting(GroupFilter)`, `OpponentsCastFromZones(zones, filter?)`, `YouCastFromZones(zones, filter?)`, `FaceDownYouCast`, `MorphActivation`.
  - `OpponentsCastFromZones(zones, filter = Any)` — spells the source-controller's opponents cast **from one of `zones`** (matched against the spell's actual cast zone, threaded as `fromZone`), matching `filter`. Pair with `CostModification.IncreaseGeneric(n)` for the Aven Interrupter shape: `OpponentsCastFromZones(setOf(Zone.GRAVEYARD, Zone.EXILE))` + `IncreaseGeneric(2)` = "Spells your opponents cast from graveyards or from exile cost {2} more to cast."
  - `YouCastFromZones(zones, filter = Any)` — the you-cast analogue: spells the **source's controller** casts **from one of `zones`**, matching `filter`. Pair with `CostModification.ReduceGeneric(n)` for Doc Aurlock, Grizzled Genius: `YouCastFromZones(setOf(Zone.GRAVEYARD, Zone.EXILE))` + `ReduceGeneric(2)` = "Spells you cast from your graveyard or from exile cost {2} less to cast." (Only the normal-cast path threads `fromZone`; alternative-cost casts such as flashback compute their own base cost and are unaffected.)
- `modification: CostModification` — `ReduceGeneric(amount)`, `ReduceGenericBy(source)`,
  `ReduceColored(symbols)`, `ReduceColoredPerUnit(symbols, source)`,
  `ReduceColoredIfAnyTargetMatches(symbols, filter)` (target-gated **colored** reduction — the
  colored analogue of the `FixedIfAnyTargetMatches` reduction source, which only reduces generic;
  removes the given colored pips if the spell targets a matching object. Brush Off's "costs
  {1}{U} less if it targets an instant or sorcery spell" pairs `ReduceColoredIfAnyTargetMatches("{U}",
  InstantOrSorcery)` for the `{U}` with `ReduceGenericBy(FixedIfAnyTargetMatches(1, InstantOrSorcery))`
  for the `{1}`, both gated on the same filter so they apply together. Like the generic gated
  reduction, affordability enumeration only optimistically discounts when a matching *battlefield*
  permanent exists; a stack-spell target reduction shows at full cost during enumeration and locks
  in once targets are announced), `IncreaseGeneric(amount)`,
  `IncreaseColored(symbols)` (colored tax — adds colored pips, e.g. the Invasion Leeches'
  "White spells you cast cost {W} more"), `IncreaseGenericPerOtherSpellThisTurn(amountPerSpell)`,
  `IncreaseGenericIfAnyTargetMatches(amount, filter)` (target-gated tax — "{N} more if it targets
  a Dragon", Dragon's Prey; the increase analogue of the `FixedIfAnyTargetMatches` reduction;
  applies only once a matching target is chosen, so affordability enumeration treats it as not
  applying), `IncreaseLife(amount)`.
  Reduction `source: CostReductionSource` covers fixed amounts, counts of permanents/cards in
  zones, target gates, and a few mechanic-specific shapes — e.g. `Fixed`, `CreaturesYouControl`,
  `ArtifactsYouControl`, `PermanentsYouControlMatching(filter)` (the filtered "you control" count —
  Temur Battlecrier's "creature you control with power 4 or greater" via
  `GameObjectFilter.Creature.powerAtLeast(4)`), `PermanentsOnBattlefieldMatching(filter)` (the
  same, all players), `PermanentsSacrificedThisTurn(amountPerPermanent = 1)` (the count of
  permanents sacrificed this turn by *any* player — not controller-scoped — reading the
  turn-scoped `GameState.permanentsSacrificedThisTurn` counter; The Balrog, Durin's Bane),
  `CardsInGraveyardMatchingFilter`, `FixedIfAnyTargetMatches`, … — see
  `CostStaticAbilities.kt` for the full list.
- `gating: CostGating` — gates whether/how often the modifier fires:
  - `None` (default) — applies to every matching cast.
  - `NthOfTypePerTurn(n)` — only when this is the Nth matching spell each turn (1-indexed; counts the
    spell currently being cast). Use `n = 1` for "the first ... each turn" (Eluge); use
    `NthOfTypePerTurn(2)` with `target = YouCast(GameObjectFilter.Any)` for Uthros Psionicist's "the
    second spell you cast each turn costs {2} less". Requires a filter-bearing target
    (`YouCast` / `AnyCaster`) — it needs a notion of "type" to count.
  - `OnlyIf(condition)` — applies only while `condition` holds at cast time (evaluated with the
    caster as controller). Gates the *whole* modification, so it composes with the dynamic per-unit
    reductions that a fixed-amount source can't express: Temur Battlecrier's "During your turn, …"
    is `OnlyIf(Conditions.IsYourTurn)` over `ReduceGenericBy(PermanentsYouControlMatching(…))`. For a
    fixed conditional reduction pair it with `ReduceGeneric` (Mental Modulation:
    `ReduceGeneric(1)` gated by `OnlyIf(IsYourTurn)`; Lashwhip Predator / Arwen's Gift gate on a
    `Compare(...)`).

**Plot cost statics — `ModifyPlotCost`**

Plot (CR 718) is a *special action*, not a spell, so `ModifySpellCost` never touches it.
`ModifyPlotCost(target, modification)` is its dedicated cost modifier, evaluated by the engine's
`PlotCostReducer` (consulted by both the plot legal-action enumerator and the plot handler so
affordability and payment stay in lockstep).

```kotlin
staticAbility {
    ability = ModifyPlotCost(
        target = PlotCostTarget.YouPlotFromHand,
        modification = CostModification.ReduceGeneric(2),
    )
}
```

- `target: PlotCostTarget` — currently `YouPlotFromHand` (cards the source's controller plots from
  hand; a future "plot from top of library" / Fblthp variant slots in as a new `PlotCostTarget`
  without changing call sites).
- `modification: CostModification` — reuses the spell-cost vocabulary, but only the flat generic
  shapes (`ReduceGeneric` / `IncreaseGeneric`) are meaningful since a printed plot cost is a flat
  mana cost; generic is floored at {0} and colored pips are never reduced. Doc Aurlock, Grizzled
  Genius: "Plotting cards from your hand costs {2} less" = `ReduceGeneric(2)`.

**Door-unlock cost statics — `ModifyUnlockCost`**

Unlocking a Room door (CR 709.5e) is a *special action*, so neither `ModifySpellCost` nor
`ModifyPlotCost` touches it. `ModifyUnlockCost(target, modification)` is its dedicated cost modifier,
evaluated by the engine's `UnlockCostReducer` (consulted by both the unlock legal-action enumerator
and `UnlockRoomDoorHandler` so affordability and payment stay in lockstep — the same lockstep pattern
as `ModifyPlotCost`).

```kotlin
staticAbility {
    ability = ModifyUnlockCost(
        target = UnlockCostTarget.YouUnlock,
        modification = CostModification.ReduceGeneric(1),
    )
}
```

- `target: UnlockCostTarget` — currently `YouUnlock` (unlock actions performed by the source's
  controller; a future "opponents' unlock costs" tax slots in as a new variant without changing call
  sites).
- `modification: CostModification` — like plot, only the flat generic shapes are meaningful (printed
  unlock cost is a flat mana cost); generic is floored at {0}, colored pips untouched. Inquisitive
  Glimmer: "Unlock costs you pay cost {1} less" = `ReduceGeneric(1)` (its "Enchantment spells you cast
  cost {1} less" half is a plain `ModifySpellCost(YouCast(GameObjectFilter.Enchantment), ...)`).

**Global denial statics** (no `filter`/`duration` block — they're singleton-style)

- `PreventCycling` — "Players can't cycle cards." (Stabilizer)
- `PreventActivatedAbilities(filter, nonManaAbilitiesOnly = false)` — activated abilities of
  matching permanents can't be activated; loyalty abilities and animation costs that haven't yet
  produced a creature are unaffected. By default both mana and non-mana abilities are blocked
  (Cursed Totem → `GameObjectFilter.Creature`). With `nonManaAbilitiesOnly = true`, mana abilities
  stay usable and only non-mana abilities are blocked — the "… can't be activated unless they're
  mana abilities" wording (Sharkey, Tyrant of the Shire → `GameObjectFilter.Land.opponentControls()`).
  Also grantable at runtime via `Effects.GrantStaticAbility` (read from `GameState.grantedStaticAbilities`
  by the same activation-legality check, anchored to the holder) — see the `GrantStaticAbility`
  entry in §3 for the durational, targeted form (Braided Net:
  `PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself())` +
  `Duration.WhileAffectedTapped`).
- `HasAllActivatedAbilitiesOfExiledCards(source = ExiledCardsSource.LINKED, filter = GroupFilter.source(), creatureCardsOnly = false, oncePerTurnEach = false)`
  — the permanents matching `filter` gain **all activated abilities of the cards in this source's exile
  pile**. `source` selects the pile: `LINKED` reads the `LinkedExileComponent`; `CRAFTED` reads the
  `CraftedFromExiledComponent` recorded by a `craft(...)` cost (CR 702.167c). Resolved dynamically at
  activation-legality time: the engine pulls each exiled card's `activatedAbilities` and surfaces them on
  every matching permanent, with **that permanent** as granter (so `{T}` taps it and "this card"
  self-references bind to it — CR 113.7). Grants *activated* abilities only, not triggered/static/replacement.
    - `filter = GroupFilter.source()` (the default) → "This permanent has all activated abilities of
      the exiled cards" — the source grants to *itself* (Territory Forge with `LINKED`; Locus of
      Enlightenment with `CRAFTED`).
    - any battlefield filter → the source grants to *other* matching permanents ("Creatures you control
      with +1/+1 counters on them have all activated abilities of all creature cards exiled with this" —
      Agatha's Soul Cauldron →
      `HasAllActivatedAbilitiesOfExiledCards(filter = GroupFilter.AllCreaturesYouControl.withCounter(Counters.PLUS_ONE_PLUS_ONE), creatureCardsOnly = true)`).
    - `creatureCardsOnly = true` restricts the pile to *creature* cards (the exiled card's printed type),
      for the "all **creature** cards exiled with" wording.
    - `oncePerTurnEach = true` (Locus of Enlightenment's "only once each turn") gives each granted ability
      an `ActivationRestriction.OncePerTurn` **tracked per exiled card**: each granted ability is re-stamped
      with an exiled-card-derived `AbilityId` (`exiled_<entity>_<printedId>`), so two exiled copies of one
      card get independent budgets rather than sharing one, and duplicate materials aren't collapsed by the
      granter-dedup. Left `false`, abilities are granted unmodified (Territory Forge, Agatha). Locus →
      `HasAllActivatedAbilitiesOfExiledCards(source = ExiledCardsSource.CRAFTED, oncePerTurnEach = true)`.
    - Fill a `LINKED` pile with `Effects.ExileLinkedToSource(target)`; a `CRAFTED` pile is filled by the
      `craft(...)` cost.
- `HasAbilitiesOfChosenLinkedExiledCard(grantActivated = true, grantTriggered = true)` — the source
  permanent has all **activated and/or triggered abilities of the single card it most recently *chose***
  from its linked-exile pile (its "last chosen card", stamped by
  `Effects.RecordChosenLinkedExile(from)`). The self-scoped, one-card, activated-**and**-triggered
  sibling of `HasAllActivatedAbilitiesOfExiledCards`: it reads the source's
  `ChosenLinkedExileComponent` and re-reads it live, so re-choosing a different exiled card swaps which
  abilities the source has. Granted abilities use the source as their own source (`{T}`/self-references
  bind to it). Use the two flags to grant activated abilities, triggered abilities, or both; it never
  grants static, keyword, or replacement abilities. Pair with `Effects.ExileLinkedToSource(target)` to
  fill the pile, then a `SelectFromCollection` over `CardSource.FromLinkedExile()` +
  `Effects.RecordChosenLinkedExile(...)` to choose. (Koh, the Face Stealer — "Pay 1 life: Choose a
  creature card exiled with Koh. Koh has all activated and triggered abilities of the last chosen card"
  → `HasAbilitiesOfChosenLinkedExiledCard()`.)
- `SuppressEntersTriggers(filter = GameObjectFilter.Creature)` — permanents matching `filter`
  entering the battlefield don't cause abilities to trigger (CR 603.6 enters-the-battlefield
  triggers). Suppresses both the entering permanent's *own* ETB triggers and any other permanent's
  "whenever a [...] enters" trigger whose triggering object is that permanent — the gate is whether
  the *entering object* matches `filter` in projected state (continuous effects apply), not what the
  watching trigger names. Replacement effects (enters with counters/tapped) and `EntersWithChoice`
  "as it enters" choices are unaffected (they aren't triggered abilities). Torpor Orb / Hushwing Gryff
  → `SuppressEntersTriggers()`; Tocatli Honor Guard → `SuppressEntersTriggers(GameObjectFilter.Creature.youControl())`.
- `GainActivatedAbilitiesOfPermanents(grantedTo, sourceFilter, includeManaAbilities = false)` —
  permanents matching `grantedTo` (a `GroupFilter`; use `GroupFilter.source()` for "this permanent")
  gain copies of the activated abilities of every permanent matching `sourceFilter`. The copy uses
  the gaining permanent as its source (CR 113.2), so a copied `SacrificeSelf`/`{T}` refers to the
  gainer. The dynamic, copy-from-other-permanents sibling of `GrantActivatedAbility`. Mana abilities
  are excluded unless `includeManaAbilities = true`. (Sharkey, Tyrant of the Shire — "Sharkey has all
  activated abilities of lands your opponents control except mana abilities")
- `SpendAnyManaTypeForActivatedAbilities(filter)` — mana of any type can be spent to pay the mana
  portion of the activated-ability costs of permanents matching `filter` (a `GroupFilter`; use
  `GroupFilter.source()` for "this permanent's abilities"). Relaxes colored/hybrid/Phyrexian/colorless
  pips to generic per CR 118.14 / 609.4b; non-mana cost components are untouched. Honored by both
  affordability checks and the mana solver. `filter` is matched against the permanent whose ability
  is being activated, so a battlefield filter scopes the permission to a class of permanents —
  `GroupFilter.AllCreaturesYouControl` for "spend mana as though it were mana of any color to activate
  abilities of creatures you control" (Agatha's Soul Cauldron). (Sharkey, Tyrant of the Shire — "Mana
  of any type can be spent to activate Sharkey's abilities" → `GroupFilter.source()`.)
- `PreventManaPoolEmptying` — mana pools don't empty between steps/phases. (Upwelling)
- `ConvertEmptyingManaToRed` — "If you would lose unspent mana, that mana becomes red instead."
  The colour-converting cousin of `PreventManaPoolEmptying`: at every step/phase-end mana emptying
  (`CleanupPhaseManager.emptyManaPools`, and for firebending mana at `CombatManager.endCombat`) the
  *controller's* would-be-lost mana becomes that many red mana instead of emptying (CR 500.5 / 703.4q
  emptying replaced per CR 614). Scoped to the controller of the bearing permanent, unlike Upwelling's
  all-players prevention. (Ozai, the Phoenix King)
- `NoMaximumHandSize` — controller has no hand-size limit *while this permanent is on the
  battlefield*. (Thought Vessel, Reliquary Tower) For a one-shot resolution effect that confers a
  *permanent, player-scoped* "no maximum hand size for the rest of the game" (survives the source
  leaving play), use the effect `Effects.RemoveMaximumHandSize(target)` instead — see §4. (Wisdom of Ages)
- `DamagePersistsThroughCleanup` — marked damage isn't removed from this permanent during cleanup
  steps, an exception to the CR 514.2 turn-based damage removal, so damage accumulates turn over turn
  until it becomes lethal (Ancient Adamantoise). A turn-based read consulted directly by
  `CleanupPhaseManager` (via `RoomFaceStatics` for the printed case, plus granted instances), not a
  Rule 613 projection. Only the cleanup removal is suppressed — regeneration and "remove all damage"
  effects still clear the damage. Add with `staticAbility { ability = DamagePersistsThroughCleanup }`.
- `GrantCantLoseGame` — controller "can't lose the game" while this permanent is on the battlefield
  (Lich's Mastery, Platinum Angel). Suppresses *all* loss conditions for that player (0-or-less life,
  poison, empty-library draw, effect losses); opponents can still win via "you win the game" effects.
  Projected to `GrantsCantLoseGameComponent`, read by every player-loss SBA via `playerCantLoseGame`.
- `GrantCantLoseGameFromLife` — the narrow sibling: controller "doesn't lose the game for having 0 or
  less life" (CR 704.5a) only. Poison, empty-library, and effect-based losses still apply — including a
  card's own `Effects.LoseGame`. Marina Vendrell's Grimoire ("…and don't lose the game for having 0 or
  less life" alongside its "if you have no cards in hand, you lose the game" clause, which still fires).
  Projected to `GrantsCantLoseGameFromLifeComponent`, read only by `PlayerLifeLossCheck` (controller-
  scoped, not a team-wide grant).
- `GrantProtectionToController(scope = ProtectionScope.EachOpponent)` — controller "has protection from
  [scope]" while this permanent is on the battlefield (Absolute Virtue: "You have protection from each
  of your opponents."). The continuous, static counterpart of the one-shot `GrantPlayerProtection`
  (The One Ring) — for a player only the **D**amage and **T**argeting legs of DEBT apply. Projected to
  `GrantsControllerProtectionComponent(scopes)`; `PlayerProtectionRules.isProtectedFromSource` unions
  these battlefield-sourced scopes with any one-shot `PlayerProtectionComponent` on the player, so the
  protection appears/disappears with the permanent (no cleanup). General over any `ProtectionScope`
  (single color, `Everything`, `EachOpponent`, …), mirroring the controller-grant statics
  `GrantHexproofToController` / `GrantShroudToController`.
- `SetMaximumHandSize(player, amount)` — sets the maximum hand size of a `player` scope (`You` /
  `EachOpponent` / `Each`, resolved relative to the source's controller) to a `DynamicAmount`, read at
  cleanup. Most restrictive (smallest) value wins when several apply; a `NoMaximumHandSize` controlled
  by that player still removes the cap entirely. Gate it behind a `ConditionalStaticAbility` for an "as
  long as …" form — the cleanup read unwraps the conditional and evaluates its condition against the
  source's controller. (Winter, Misanthropic Guide — `ConditionalStaticAbility(SetMaximumHandSize(
  EachOpponent, Subtract(Fixed(7), AggregateZone(You, GRAVEYARD, Any, DISTINCT_TYPES))), Delirium())`.)
- `DampLandManaProduction` — a land tapped for 2+ mana produces `{C}` instead. (Damping Sphere)
- `WinCoinFlips(firstFlipEachTurn = false)` — a coin-flip result replacement (CR 705.3): the
  controller's coin flips come up heads / are won. `firstFlipEachTurn = true` restricts it to the
  controller's *first* coin-flip event each turn (Edgar, King of Figaro — "the first time you flip
  one or more coins each turn, those coins come up heads and you win those flips"); `false` forces
  every coin flip the controller makes. Heads is modeled as a won flip, so this forces the win. Not a
  Rule 613 continuous effect — the coin-flip executors query it via `CoinFlipModifiers`, and a
  per-player `FlippedCoinsThisTurnComponent` (cleared at cleanup) tracks the "first each turn" gate.
- `RestrictSpellsCastPerTurn(maxPerTurn, eachPlayer = false)` — a per-turn cap on spells cast.
  `eachPlayer = false` (default) limits only the source's controller (Yawgmoth's Agenda: "You can't
  cast more than one spell each turn."); `eachPlayer = true` is a *global* restriction binding every
  player (High Noon: "Each player can't cast more than one spell each turn."). The most restrictive
  `maxPerTurn` applies when several are in play. Already-cast spells count, even those cast before this
  permanent entered.
- `CantCastSpellsSharingColorWithLastCast` — *global* (all players): can't cast a spell that shares a
  color with the spell most recently cast this turn. Backed by `GameState.lastCastSpellColors` (the
  colors of the last spell cast, cleared each turn). Never blocks the first spell of the turn; a
  colorless spell shares no color, so it is always castable and casting one lifts the restriction
  until the next colored spell. (Mana Maze)
- `PlayersCantCastSpells(affected = Player.EachOpponent, spellFilter = GameObjectFilter.Any, condition = null)`
  — continuous cast *prohibition* parameterized along three independent axes, each a reused
  primitive: **who** (`affected`, a `Player` reference *relative to the source's controller* —
  `EachOpponent`/`Opponent`, `You`, `Each`), **which** (`spellFilter`, matched against the card being
  cast), and **when** (`condition`, evaluated in the controller's context, so `IsYourTurn` = "during
  your turn", `IsNotYourTurn` = "during an opponent's turn"; `null` = always). Read at cast-legality
  time through the single `CastPermissionUtils.reasonCannotCast` chokepoint, so it covers every
  casting zone (hand, flashback/harmonize, exile, top of library) uniformly; control is read from
  projected state. Examples: Voice of Victory = `PlayersCantCastSpells(Player.EachOpponent, condition
  = IsYourTurn)`; Grand Abolisher's cast clause = `PlayersCantCastSpells(Player.EachOpponent, condition
  = IsYourTurn)`; Void Winnower = `PlayersCantCastSpells(Player.EachOpponent, spellFilter =
  GameObjectFilter(cardPredicates = listOf(CardPredicate.ManaValueIsEven)))`.
- `PlayersCantActivateAbilities(affected = Player.EachOpponent, permanentFilter = GameObjectFilter.Any, condition = null)`
  — continuous *activation* prohibition, the activated-ability twin of `PlayersCantCastSpells`,
  parameterized along the same three axes: **who** (`affected`, relative to the source's controller),
  **which** (`permanentFilter`, matched in projected state against the *permanent whose ability is being
  activated* — not the ability itself), and **when** (`condition`, evaluated in the controller's
  context). Read at ability-activation-legality time on every battlefield permanent
  (`CastPermissionUtils.isActivationPreventedForPlayer`, consulted by the activate handler + the
  activated/mana-ability enumerators), so it blocks mana and non-mana abilities alike. Unlike the
  who/when-blind `PreventActivatedAbilities` (Cursed Totem), it additionally scopes by who is activating
  and when. Grand Abolisher's activate clause = `PlayersCantActivateAbilities(Player.EachOpponent,
  permanentFilter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Enchantment,
  condition = IsYourTurn)` ("During your turn, your opponents can't activate abilities of artifacts,
  creatures, or enchantments."); loyalty abilities and land mana abilities are unaffected because the
  filter only matches those three permanent types.

**Tapped-for-mana mana statics** (extra mana / replaced mana when a land is tapped for mana — resolve
inline as triggered mana abilities, off the stack per CR 605). These fire on the *manual* mana-ability
path; automatic cost payment adds the extra/replacement *mana* via the solver but skips non-mana
riders, matching how the engine already treats e.g. City of Brass's damage during auto-pay.

- `AdditionalManaOnTap(color, amount, anyColor = false)` — aura: "Whenever enchanted land is tapped
  for mana, its controller adds additional mana." `color = null` reads the aura's `ChosenColorComponent`;
  `anyColor = true` makes it one mana of **any color the controller chooses** each tap (prompts on a
  manual tap; flexible for the solver). (Elvish Guidance = fixed `{G}`; **Fertile Ground** = `anyColor`)
- `AdditionalManaOnSourceTap(sourceFilter, color = null, amount = 1, rider = null, whenProducing = TappedForManaType.ANY)` — global: "Whenever
  a `<sourceFilter>` is tapped for mana, that player adds …". `color = null` mirrors the produced color.
  `rider` is an optional non-mana `Effect` resolved inline, controlled by the tapping player
  (`EffectTarget.Controller` = tapper, `EffectTarget.Self` = the static's source). (Lavaleaper = basic-land
  mirror; Badgermole Cub = `+{G}`; **Overabundance** = `GameObjectFilter.Land` mirror + `DealDamage(1,
  Controller)` rider; **Roxanne, Starfall Savant** = `GameObjectFilter.Artifact.token().youControl()`
  mirror). The mirror fires for both fixed-color producers (handled synchronously) and **any-color**
  producers whose color is chosen at tap time (e.g. Roxanne's Meteorite "{T}: Add one mana of any
  color" — the mirror is applied after the color choice resolves, in the color-choice continuation).
  `whenProducing` (`TappedForManaType.{ANY, COLORLESS, COLORED}`, default `ANY`) restricts which
  produced-mana type fires the bonus: `COLORLESS` models "tap a land **for {C}**" (a Forest tapped for
  {G} doesn't fire it) and, paired with `color = null`, mirrors the {C} back — **Ultima, Origin of
  Oblivion** = `GameObjectFilter.Land.youControl()`, `color = null`, `whenProducing = COLORLESS`. Gated
  on all three mana paths (manual tap, color-choice-resume mirror, and the auto-pay `ManaSolver`, whose
  colorless bonus floats as a colorless `BonusManaEntry`).
- `ReplaceLandManaColor(filter)` — global: lands matching `filter` produce one mana of a color of their
  controller's choice instead of their normal mana. Implemented by swapping the land's base mana effect
  for "add one mana of any color", so the choice flows through the normal any-color machinery (manual tap
  prompts; solver treats a matched basic as a five-color source). (**Pulse of Llanowar** =
  `GameObjectFilter.BasicLand.youControl()`)
- `OverrideEnchantedLandManaColor(color)` — aura: replaces the enchanted land's *own* produced color with
  a fixed/aura-chosen `color` (vs. `ReplaceLandManaColor`'s filter-based, free-choice form). (Shimmerwilds Growth)

**Alternative play / cast permissions** (let a player play or cast cards from non-hand zones)

- `MayPlayLandsFromGraveyard` — play lands from your graveyard (no per-turn cap). (Icetill Explorer)
- `MayPlayPermanentsFromGraveyard` — Muldrotha: play a land + cast one permanent spell of each
  permanent type from your graveyard each turn (per-type-per-turn cap).
- `EquipAbilitiesAtInstantSpeed` — the controller may activate equip abilities any time they could
  cast an instant (CR 702.6e timing lifted). Wrap in a `ConditionalStaticAbility` (the
  `staticAbility { condition = …; ability = EquipAbilitiesAtInstantSpeed }` DSL form) for a gated
  grant — Forge Anew uses `condition = Conditions.IsYourTurn` for its "During your turn …" clause;
  a bare grant (Leonin Shikari) applies unconditionally. Consulted by `CastPermissionUtils
  .canEquipAtInstantSpeed` (enumerator) and `ActivateAbilityHandler.validate` (submit path), both
  keyed on `ActivatedAbility.isEquipAbility`.
- `FreeFirstEquipEachTurn` — the controller may pay {0} rather than the equip cost of the **first**
  equip ability they activate during each of their turns (Forge Anew). The engine zeroes the whole
  cost (colored pips included) of the turn's first equip while the per-player
  `EquipActivationsThisTurnComponent.count == 0`, and increments that counter on every equip
  activation (reset at turn start by `TurnManager`).
- `ReduceEquipCost(amount, onlyIfTargetIsSource = false, onlyOwnEquip = false)` — the controller's equip abilities cost
  `{amount}` generic mana less to activate (Éowyn, Lady of Rohan: "Equip abilities you activate cost
  {1} less to activate"). The engine reduces only the generic portion of the equip cost (floored at
  {0}); colored pips are untouched, and multiple sources stack additively. Controller-scoped — it
  applies to every equip ability the controller activates, regardless of which permanent bears the
  equip ability. Consulted by `CastPermissionUtils.applyEquipCostReduction` from both the enumerator
  (displayed cost) and `ActivateAbilityHandler` (paid cost), keyed on
  `ActivatedAbility.isEquipAbility` and applied before the `FreeFirstEquipEachTurn` discount. Wrap in
  a `ConditionalStaticAbility` for a "during your turn"-style gate. Set `onlyIfTargetIsSource = true`
  for the target-restricted form — "Equip abilities you activate **that target ~** cost `{amount}`
  less to activate" (Cloud, Planet's Champion): the reduction applies only when the equip's chosen
  target is the permanent bearing this static. The exact per-target cost is enforced at payment
  (the chosen target is threaded into `applyEquipCostReduction`); at enumeration, before a target is
  chosen, the discount is offered optimistically whenever the source is currently a creature, so the
  ability is never withheld for want of the discount.
  Set `onlyOwnEquip = true` for the self-restricted form — "**This permanent's** equip abilities cost
  `{amount}` less to activate" (Firion, Wild Rose Warrior's token copy): the reduction applies only to
  the equip abilities of the permanent bearing this static, matched at the reduction site against the
  equip ability's source. Because it is typically granted to a token (via
  `CreateTokenCopyOfTargetEffect.addedStaticAbilities`), the equip-cost reader unions
  `grantedStaticAbilities` with printed statics.
- `ReduceActivatedAbilityCost(filter, amount, manaFloor = 0)` — the activated abilities of permanents
  matching `filter` cost `{amount}` generic mana less to activate, with the mana in each cost floored
  at `manaFloor` *total* mana (generic + colored). The activated-ability sibling of `ReduceEquipCost`,
  generalized over a `GroupFilter`: `GroupFilter.attachedCreature()` keys it to an Aura's enchanted
  permanent (Power Artifact: "Enchanted artifact's activated abilities cost {2} less to activate. This
  effect can't reduce the mana in that cost to less than one mana." → `ReduceActivatedAbilityCost(
  GroupFilter.attachedCreature(), amount = 2, manaFloor = 1)`); `GroupFilter.source()` for "this
  permanent's abilities"; a battlefield filter for a group lord. Generic-only (colored/hybrid pips
  untouched, CR 118.7); with `manaFloor = 1` a `{1}` ability stays `{1}`, `{3}`→`{1}`, `{2}{U}`→`{U}`.
  `manaFloor = 0` (default) floors at `{0}`. Reductions stack additively; the most restrictive
  (largest) `manaFloor` wins. Consulted by `CastPermissionUtils.applyActivatedAbilityCostReduction`
  from both the enumerator (displayed cost) and `ActivateAbilityHandler` (paid cost), keyed on the
  ability's source permanent; non-mana costs (`{T}`, sacrifice) and abilities with no mana cost are
  unaffected. Backed by `ManaCost.reduceGenericWithManaFloor(amount, minTotalMana)`.
- `MayCastFromGraveyard(filter, lifeCost = 0, duringYourTurnOnly = false)` — cast spells matching
  `filter` from your graveyard following normal timing, optionally paying `lifeCost` life. Free for
  Yawgmoth's Agenda (`MayCastFromGraveyard(Nonland)`); `lifeCost = 1, duringYourTurnOnly = true` for
  Festival of Embers. Pair with `MayPlayLandsFromGraveyard` for "play lands and cast spells from
  your graveyard". Lands are *played*, not cast, so they need the lands permission separately. This
  grants permission over *other* cards in your graveyard from a battlefield permanent — for a card
  that grants permission to cast *itself* from a zone, use `MayCastSelfFromZones`.
- `GraveyardCardsHaveFlashback(filter, cost = null, duringYourTurnOnly = false)` — a **whole-graveyard
  flashback grant** (CR 702.34): a continuous static that grants flashback to *every* card in the
  controller's graveyard matching `filter` (not a single-card grant like `Effects.GrantFlashback` /
  Archmage's Newt). `cost = null` means "flashback cost equal to that card's mana cost"; pass a
  `ManaCost` for a fixed cost. `duringYourTurnOnly = true` gates the grant to the controller's turn.
  A matching card is castable from the graveyard for the flashback cost and is exiled on resolution,
  exactly like printed flashback — all four read sites (enumeration, cast cost, permission, and the
  stack resolver's exile-on-resolution clause) route through the shared `FlashbackGrants.effectiveFlashback`
  resolver, which now also scans the battlefield for this static (matching on the card's
  zone-independent characteristics, so it resolves the same whether the card is still in the
  graveyard or already on the stack). Used by Iroh, Grand Lotus: one grant for
  `InstantOrSorcery.notSubtype(Lesson)` with `cost = null` ("each non-Lesson instant and sorcery card
  in your graveyard has flashback … equal to that card's mana cost") and one for
  `InstantOrSorcery.withSubtype(Lesson)` with `cost = {1}` ("each Lesson card in your graveyard has
  flashback {1}"), both `duringYourTurnOnly = true`.
- `GrantMayCastFromLinkedExile(filter = Nonland, duringYourTurnOnly = false, additionalCost = null, ownedByYou = false, withoutPayingManaCost = false, oncePerTurn = false, maxManaValue = null, exiledThisTurnOnly = false)`
  — "you may cast cards exiled with this permanent" — reads the source's `LinkedExileComponent` (Rona,
  Disciple of Gix; Maralen, Fae Ascendant; Dawnhand Dissident). Casting spells from linked exile is
  enumerated by `CastFromZoneEnumerator.enumerateLinkedExile`; the cast path deliberately skips lands.
  For the "pay life equal to its mana value rather than pay its mana cost" shape (Valgavoth, Terror
  Eater), set `withoutPayingManaCost = true` (waives the mana) and `additionalCost =
  Costs.additional.PayLifeEqualToManaValueOfSpell` (substitutes the life). When `filter` admits lands
  (e.g. `GameObjectFilter.Any`, no `IsNonland` predicate), the permission also covers *playing* land
  cards from the linked exile — surfaced by `PlayLandEnumerator` and authorized by `PlayLandHandler`
  (lands cost no life). Pair with a `RedirectZoneChange(linkToSource = true)` to fill the exile pile.
- `GraveyardCreaturesHaveSneak(cost)` — "Creature cards in your graveyard have sneak `cost`. You may
  cast creature spells from your graveyard using their sneak abilities." (Ninja Teen level 3.) While
  the controller has this static active, their graveyard creature cards become castable via the
  Sneak alt-cost (CR 702.190) from the graveyard — pay `cost` plus return an unblocked attacker you
  control during your declare-blockers step; the creature enters tapped and attacking. Implemented
  additively (the printed-Sneak hand path is untouched): `SneakWindow.graveyardSneakGrantCost` /
  `effectiveSneakCost` are read by `SneakCastEnumerator` (a graveyard loop) and four sites in
  `CastSpellHandler` (zone gate, both cost paths, the enters-tapped/bounce resolution). No
  exile-on-resolution (unlike flashback) — the creature simply moves graveyard → stack → battlefield.
- `MayCastSelfFromZones(zones, condition = null)` — intrinsic *self* permission: this card may be
  cast from any of `zones` (graveyard/exile) following normal timing and for its normal mana cost.
  Squee, the Immortal = `MayCastSelfFromZones(listOf(GRAVEYARD, EXILE))`. When `condition` is
  non-null the permission is **gated**: it is available only while the condition holds, evaluated in
  the casting player's context at cast-legality time *and* re-checked when the cast is authorized
  (so it can't outlive the permission). Undead Sprinter (DSK) = `MayCastSelfFromZones(listOf(GRAVEYARD),
  condition = Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.ZOMBIE))` for "You may cast this card
  from your graveyard if a non-Zombie creature died this turn." Pair with an
  `EntersWithCounters(selfOnly = true, condition = Conditions.WasCastFromGraveyard)` rider to model
  "if you do, this creature enters with a +1/+1 counter on it" — the counter is tied to the
  graveyard cast (`CastFromGraveyardComponent` stamped on resolution), not to the gate condition.
- `GrantWarpToCardsInHand(filter, cost)` — cards in the controller's hand matching `filter` gain
  warp (CR 702.185) with mana cost `cost`. Behaves identically to a printed warp keyword: surfaces a
  "Cast (Warp)" legal action, marks `wasWarped` on resolution, and the post-resolution permanent is
  exiled at the next end step and can be cast again from exile for its regular mana cost. Hand-only
  by CR 702.185a and the granters' "in your hand" wording — the grant doesn't extend warp to other
  zones. Routed through `WarpGrants.effectiveWarp` alongside printed warp; the granter's controller
  is the only beneficiary. (Tannuk, Steadfast Second = `GrantWarpToCardsInHand(filter = artifact OR
  red creature, cost = {2}{R})`.) When the granted warp lands on a card that *also* has another
  alternative cost (e.g. a red evoke creature), both casts are offered and disambiguated by
  `CastSpell.alternativeCostType` (see `engine-server-interface.md`) — picking "Evoke" charges the
  evoke cost, not warp, even though warp would win a naive priority order.
- `GrantKeywordToOwnSpells(keyword, spellFilter = Creature)` — while this permanent is on the battlefield,
  spells its controller casts matching `spellFilter` effectively have `keyword` ("you cast" semantics). Read by
  the cast machinery via `GrantedKeywordResolver`:
  - **Cost-modifying keywords** (CONVOKE, DELVE) surface in the cast enumerator / alternative-payment handler —
    e.g. **Eirdu, Carrier of Dawn** (`GrantKeywordToOwnSpells(CONVOKE, Creature)`), **Teval, Arbiter of Virtue**
    (`DELVE`).
  - **STORM** (and other "each instance triggers separately" cast keywords) are counted by
    `GrantedKeywordResolver.countGrants` when `CastSpellHandler` builds the spell's storm triggers — one storm
    instance per matching grant (CR 702.40b), added to the printed-keyword count. **Prismari, the Inspiration** =
    `GrantKeywordToOwnSpells(STORM, InstantOrSorcery)`. Removing the granter before the spell is cast revokes the
    grant. (Cascade-style granted keywords are instead modelled as a `youCastSpell(...)`-triggered `Effects.Cascade`
    on the granter — see **Quandrix, the Proof** / Wildsear, Scouring Maw — since cascade is a cast trigger, not a
    cost keyword.)
  - **Damage keywords on the spell object** (LIFELINK) — the noncombat-damage path (`DamageUtils.dealDamageToTarget`)
    now also consults `GrantedKeywordResolver` for the spell *source's* current controller, so
    "`<type>` spells you control have lifelink" is honored when a matching spell deals damage → its controller
    gains that much life (**Lo and Li, Twin Tutors** → `GrantKeywordToOwnSpells(LIFELINK, Any.withSubtype("Lesson"))`,
    validated with a burn Lesson like Ozai's Cruelty). Static keyword projection only reaches battlefield permanents,
    so a *spell* granted lifelink is invisible to `projected.hasKeyword`; the damage site reads the grant directly
    (the same shape as the existing wither-on-spell check, which reads `SpellGrantedKeywordsComponent`). One-shot
    per-spell grants (`GrantKeywordToSpellEffect` → `SpellGrantedKeywordsComponent`, e.g. a copy that gains lifelink)
    feed the same check.
- `MayCastWithoutPayingManaCost(controllerOnly = false, firstSpellOfTurnOnly = false, spellFilter = Any, oncePerTurn = false, fromExileOnly = false)` — a
  battlefield permission to cast a spell without paying its mana cost (CR 118.9). Composable
  gates: `controllerOnly = true` restricts the benefit to the source's controller ("you" wording);
  `firstSpellOfTurnOnly = true` requires the caster to be the active player and to have cast
  zero spells this turn; `oncePerTurn = true` allows one free cast during each of the caster's own
  turns (active player only) regardless of how many spells were cast first — unlike
  `firstSpellOfTurnOnly`, the caster may cast other spells before using it; each source tracks its
  own use via `MayCastWithoutPayingCostUsedThisTurnComponent`, cleared at end of turn; `spellFilter`
  restricts *which* spells may be cast for free (card predicates, matched in any zone — default
  `GameObjectFilter.Any` = every spell). `fromExileOnly = true` restricts the permission to spells
  cast **from exile** (offered on the cast-from-exile path, withheld from hand/graveyard casts) —
  Warped Space (Charred Foyer // Warped Space): "Once each turn, you may pay {0} rather than pay the
  mana cost for a spell you cast from exile" → `MayCastWithoutPayingManaCost(controllerOnly = true,
  oncePerTurn = true, fromExileOnly = true)`. The free-cast permission is offered for cards in hand
  (default) and, when `fromExileOnly = true`, for cards being cast from exile. Weftwalking is
  `MayCastWithoutPayingManaCost(firstSpellOfTurnOnly = true)`; Zaffai and the
  Tempests is `MayCastWithoutPayingManaCost(controllerOnly = true, oncePerTurn = true, spellFilter =
  GameObjectFilter.InstantOrSorcery)` ("Once during each of your turns, you may cast an instant or
  sorcery spell from your hand without paying its mana cost"); Dracogenesis is
  `MayCastWithoutPayingManaCost(controllerOnly = true, spellFilter = GameObjectFilter.Any.withSubtype("Dragon"))`
  ("You may cast Dragon spells without paying their mana costs"); a future "you may cast the first
  spell you cast each turn …" composes via both gates true. The filter and zone gate are enforced
  per-spell in `CostCalculator.hasFreeCastPermission(state, casterId, spellCardDef, castFromZone)`
  (the enumerator threads the card being cast and its zone through
  `EnumerationContext.freeCastPermissionFor(cardId, castFromZone)` — `Zone.HAND` on the hand path,
  `Zone.EXILE` on the cast-from-exile path).
  Cast-legality is checked by `CostCalculator.hasFreeCastPermission`. Surfaced as a dedicated
  `CastWithoutPayingManaCost` `LegalAction` variant routed through
  `CastSpell.useWithoutPayingManaCost = true` — emitted **alongside** Jodah-style
  `GrantAlternativeCastingCost`, flashback, harmonize, warp, evoke, impending, and
  `selfAlternativeCost` variants so the player explicitly picks one (CR 118.9a — only one
  alternative cost may apply to a cast, and which one is the player's choice, not handler
  priority). `CastSpellHandler.validate` rejects combining the flag with `useAlternativeCost`.
  When chosen, the cast is treated as `playForFree` (cost zeroed, X = 0 per CR 107.3b, kicker
  / blight / behold / runtime tax skipped; mandatory additional costs like Embrace Oblivion's
  sacrifice are still enforced), matching the existing `PlayWithoutPayingCostComponent` flow
  used by Cascade and Omniscience.

**Top-of-library reveal & play** (reveal the top card of a library, optionally with permission to
play it from there). Visibility (public reveal to all players) and play permission are separate
concerns — the `ClientStateTransformer` reveals the top card for `PlayFromTopOfLibrary` *or*
`RevealTopOfLibrary`, while the cast/play-from-top paths key only on the play-granting variants.

- `RevealTopOfLibrary` — *public reveal only*, no play permission: the controller's top card is
  shown to all players, but can only be played once drawn. (**Goblin Spy**)
- `PlayFromTopOfLibrary` — public reveal **and** "play lands and cast spells from the top of your
  library" (all card types). (Future Sight)
- `PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter)` — like `PlayFromTopOfLibrary` but only
  spells matching `spellFilter` are castable (lands always playable), and **no public reveal** (pair
  with `LookAtTopOfLibrary` to let just the controller see). `spellFilter = GameObjectFilter.Any`
  means "play the top card" of any type non-revealingly. (Glarb, Calamity's Augur =
  `GameObjectFilter.Any.manaValueAtLeast(4)`; The Lunar Whale = `GameObjectFilter.Any`.) Honors a
  `ConditionalStaticAbility` wrapper — the play/cast-from-top readers unwrap the conditional and
  evaluate its gate against the granting permanent, so the permission can be time-restricted (The
  Lunar Whale: `condition = Conditions.SourceAttackedThisTurn` — only after the Whale attacked).
- `CastSpellTypesFromTopOfLibrary(filter)` — cast only matching spell types from the top; no land
  play, no full public reveal. (Precognition Field = instants/sorceries)
- `LookAtTopOfLibrary` — *private*: the controller may look at their own top card any time (revealed
  only to them, not opponents). (Lens of Clarity, Vizier of the Menagerie)
- `PlotFromTopOfLibrary(filter = Nonland)` — the controller may **plot** (CR 718) the top card of
  their library if it matches `filter`, paying a plot cost equal to the card's mana cost. The plot
  legal-action enumerator offers the top card as a plot action and `PlotCardHandler` moves it from
  library → exile and plots it (sorcery-speed special action; can't be cast the turn it's plotted).
  Grants an *additional* way to plot — a card with its own printed plot cost may still use that. Pair
  with `LookAtTopOfLibrary` so the controller can see the card. (Fblthp, Lost on the Range =
  nonland.)
- `OpponentsPlayWithHandsRevealed` — visibility-only, the opponent-facing sibling of
  `RevealTopOfLibrary`: each opponent of the controller plays with their hand publicly visible to
  that controller (no other game effect). Handled entirely by the client state transformer's
  hand-masking seam. (**Seer's Vision**)

> Multiple lord effects on one card → multiple `staticAbility { }` blocks.

---

## 10. Activated abilities

```kotlin
activatedAbility {
    cost = Costs.Tap
    effect = Effects.DrawCards(1)
    target = Targets.Creature
    optional = false
    timing = TimingRule.Normal
    isManaAbility = false
    restriction = ActivationRestriction.MaxPerTurn(1)
}
```

**`genericCostReduction` — "this ability costs {N} less to activate for each …".** The
`activatedAbility { }` builder exposes `genericCostReduction: DynamicAmount?`. When set, the engine
reduces the generic-mana portion of the ability's `cost` by that amount at activation time (floored
at {0}; colored pips are never touched — CR 118.9a), and both the legal-action enumerator and
`ActivateAbilityHandler` apply the same reduction so the displayed/affordable cost matches what's
paid. It accepts **any** `DynamicAmount`, so the reduction can read:
- a **per-source property** — `DynamicAmount.EntityProperty(Source, Power)` for "costs {X} less,
  where X is this creature's power" (The Dominion Bracelet);
- the **chosen target** — `DynamicAmounts.targetColorCount()` for "costs {1} less for each color of
  the creature it targets" (Dragonfire Blade; the enumerator gates affordability on the largest
  reduction over the legal targets, since the target isn't picked until activation);
- a **battlefield count** — `DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Town")).count()`
  for "costs {1} less to activate for each Town you control" (Qiqirn Merchant).

No new vocabulary is needed for a "costs {N} less per «permanents you control matching a filter»"
ability — feed the matching count `DynamicAmount` to `genericCostReduction`.

**`TimingRule`**

- `Normal` — at instant speed (default for most abilities).
- `ManaAbility` — resolves immediately, doesn't use the stack (CR 605).
- `SorcerySpeed` — only during your main phase, empty stack.
- `OnlyIfCondition(c)` — guarded by a runtime condition.

**`ActivationRestriction`**

- `MaxPerTurn(n)` — at most N activations per turn.
- `OncePerTurn` — once each turn (resets at end of turn).
- `Once` — *"Activate only once"* (CR): once per the **lifetime of this object**, tracked on the
  permanent entity via `AbilityActivatedEverComponent`. Per CR 400.7 / 403.4 a permanent that leaves
  and re-enters the battlefield is a *new object*, so its `Once` ability may be activated again — this
  is **not** "once per game". Backs the **Exhaust** keyword (below).
- `OnlyIfCondition(c)` — condition gate.
- `OnlyDuringYourTurn` / `DuringPhase(p)` / `DuringStep(s)` / `BeforeStep(s)` — timing gates (compose
  via `All(...)`, e.g. `All(DuringStep(UPKEEP), OnlyDuringYourTurn)` for "only during your upkeep").

**Exhaust** (Avatar: The Last Airbender, returning from Edge of Eternities; CR 702.177) — *not a
keyword-line keyword*; a marker flag on an activated ability. *"Exhaust — [cost]: [effect]"* means
*"[cost]: [effect]. Activate only once."* Set `isExhaust = true` in the `activatedAbility { }` block.
That (a) renders the *"Exhaust — "* prefix on the ability's `description` (in printed order, so an
"Exhaust — Waterbend {N}" ability reads correctly) and (b) **auto-adds `ActivationRestriction.Once`**
to the ability's restrictions, so the keyword marker and its once-per-object enforcement can't drift
apart. No game-scoped tracker is needed — `Once`'s per-object lifetime (above) is exactly Exhaust's
rules semantics, so a permanent re-entering the battlefield may activate its exhaust ability again.
Compose freely with other restrictions, e.g. `restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)`
alongside `isExhaust = true` for "Exhaust — …: … Activate only during your turn." (Bitter Work).

**`minimumXValue` — "X can't be 0".** Set `minimumXValue = 1` in the `activatedAbility { }` block for
an X-cost ability whose X may not be 0 (**Gogo, Master of Mimicry**: "{X}{X}, {T}: … X can't be 0.").
The X-choice decision clamps its lower bound to this value, the enumerated `LegalAction.minX` surfaces
it to the client's X picker, and the handler rejects an engine-direct activation with a smaller X.
Defaults to 0.

**`cantBeCopied` — "This ability can't be copied".** Set `cantBeCopied = true` in the
`activatedAbility { }` block so the ability instance on the stack is tagged with the shared
`CantBeCopiedComponent` marker; a copy-ability effect (`Effects.CopyTargetSpellOrAbility`) makes no
copy of it (CR 707.10e). The activated-ability analogue of the spell-level `cantBeCopied` flag
(§spell flags). First user: **Gogo, Master of Mimicry**.

**Loyalty abilities**

- `loyaltyAbility(+N) { ... }` — add loyalty + effect.
- `loyaltyAbility(-N) { ... }` — remove loyalty + effect.
- `loyaltyAbility(0) { ... }` — 0-loyalty ability.

---

## 11. Keywords

> **Where set-mechanic helpers live.** The `card { … }` keyword helpers below for *set-specific*
> mechanics — `leyline()`, `flurry { }`, `mobilize(…)`, `firebending(n)`, `sneak(cost)`, `decayed()`,
> `vividEtb { }` / `vividCostReduction()`, `convergeEntersWithCounters(counterType?)`,
> `impending(time, cost)`, `renew(cost) { }`, `enduring()`,
> `craft(filter, cost)`, `station()`, `jobSelect()` — are `CardBuilder` **extension functions** in
> `mtg-sdk/.../dsl/mechanics/` (one file per mechanic), not methods on the core `CardBuilder`. They
> stay in package `com.wingedsheep.sdk.dsl`, so the call syntax is unchanged, but a card file that
> uses one needs the matching import (e.g. `import com.wingedsheep.sdk.dsl.station`). Evergreen /
> multi-set parameterized keywords (`prowess()`, `rampage(n)`, `keywordAbility(…)`) remain on the core
> builder. New set mechanics get an extension file in `dsl/mechanics/`.

> **Enduring** (Duskmourn Glimmer cycle). `card { enduring() }` wires the full mechanic: "When this
> permanent dies, if it was a creature, return it to the battlefield under its owner's control. It's an
> enchantment. (It's not a creature.)" Modeled like **Persist** — a synthesized SELF dies-trigger detected
> in `DeathAndLeaveTriggerDetector.detectEnduringTriggers`, gated on the last-known type being a creature
> (so the returned enchantment never loops on its second death) and suppressed on tokens (CR 111.7). The
> synthesized effect returns the card via `MoveToZoneEffect(Self, BATTLEFIELD, fromZone = GRAVEYARD)` then
> stamps an enduring-return marker (`MarkEnduringReturnEffect` → `EnduringReturnComponent`). The helper also
> adds a `ConditionalStaticAbility(TransformPermanent(setCardTypes = {"ENCHANTMENT"}, clearSubtypes = true),
> SourceReturnedAsEnchantment)` so, while the marker is present, the permanent is an enchantment with no other
> card types or subtypes. Author the printed dies-clause + reminder text into `oracleText`. The
> `Keyword.ENDURING` display keyword carries no combat behavior.

> **Converge** (ability word, CR 207.2c — flavor only, no keyword, like Opus/Vivid). Scales an effect
> by the number of distinct colors of mana spent to cast the spell. Three shapes:
> (1) *enters with counters* — `convergeEntersWithCounters(counterType = PlusOnePlusOne)` (the SOS
> "Archaic" cycle; lowers to `EntersWithDynamicCounters(DynamicAmount.DistinctColorsManaSpent)`);
> (2) *spell whose effect scales* — read `DynamicAmounts.colorsOfManaSpent()` directly (Arcane Omens
> "discard X"); (3) *exile-by-color-count* — the `manaValueAtMostColorsSpent(EntityReference.Source)`
> target predicate (Sundering Archaic). Author the printed "Converge — …" text into `oracleText`.

> **Flanking** (CR 702.25 — bare `Keyword.FLANKING`, no builder). A keyword-derived *triggered*
> ability: "Whenever this creature becomes blocked by a creature without flanking, that blocking
> creature gets -1/-1 until end of turn." Cards just declare `keywords(Keyword.FLANKING)`; the engine
> synthesizes [`Flanking.blockedByNonFlankerTrigger`](../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/Flanking.kt)
> for any creature that has the keyword (intrinsic or granted) via
> `TriggerAbilityResolver.getFlankingTriggeredAbilities`, the same derive-don't-author pattern used
> by ward and suspend. The trigger's `BecomesBlockedEvent(filter = Creature.withoutKeyword(FLANKING))`
> fires once per non-flanking blocker with that blocker as the triggering entity, so each blocker
> independently takes -1/-1; a blocker that also has flanking is excluded (CR 702.25c).

**`Keyword` enum (display-level)**

Flying, Menace, Intimidate, Fear, Shadow, Horsemanship, all basic landwalks (Plainswalk … Forestwalk), Desertwalk
(nonbasic landwalk variant — `Keyword.DESERTWALK`, keyed off `Subtype.DESERT`), Nonbasic landwalk
(`Keyword.NONBASIC_LANDWALK` — unblockable while the defending player controls any non-basic land;
`LandwalkRule` checks `typeLine.isLand && !isBasicLand`; Trailblazer's Boots), First Strike, Double
Strike, Trample, Deathtouch, Lifelink, Vigilance, Reach, Provoke, Defender, Indestructible, Hexproof, Shroud, Haste,
Flash, Prowess, Flurry, Changeling, Convoke, Delve, Affinity, Storm, Flashback, Harmonize, Evoke, Sneak, Ninjutsu, Impending, Conspire, Casualty, Miracle, Hideaway, Cascade, Plot,
Offspring, Persist, Enduring, Ascend, Wither, Toxic, Eerie, Vivid, Fateful Bite, Exploit, … (display-only — engine effect lives in handlers or
composite abilities).

**Parameterized `KeywordAbility.*`**

- `Ward(amount)` — opponent pays a mana cost to target this (CR 702.21). Non-mana costs use
  `KeywordAbility.Ward(WardCost.X)`: `WardCost.Mana`, `WardCost.Life(n)`, `WardCost.DynamicLife(amount)`,
  `WardCost.Discard(n, random, filter)`,
  and `WardCost.Sacrifice(filter, count = 1)` ("Ward—Sacrifice a Food", Ygra; "Ward—Sacrifice three
  nonland permanents" with `count = 3`, Valgavoth, Terror Eater, via `KeywordAbility.wardSacrifice(filter, count)`).
  For sacrifice ward, the opponent chooses which `count` matching permanent(s) they control to sacrifice
  (selecting fewer, or controlling fewer than `count`, counters their spell); valid fodder is matched
  against projected state, so subtypes granted by continuous effects count.
  Composite costs ("Ward—{2}, Pay 2 life", Gisa, the Hellraiser) use
  `KeywordAbility.wardComposite(WardCost.Mana("{2}"), WardCost.Life(2))` → `WardCost.Composite(parts)`.
  The components are charged one at a time in order; the spell/ability resolves only if every
  component is paid, and declining or being unable to pay any one component counters it (CR 702.21a).
  Each component reuses the same per-cost decision flow as a standalone ward, so a composite cost
  shows one prompt per component. `parts` must be flat atomic ward costs (no nested `Composite`).
  `WardCost.Discard` (built via `KeywordAbility.wardDiscard(count, random, filter)`) takes an optional
  `GameObjectFilter`: when set, only matching hand cards count toward the can-pay check and only matching
  cards are offered for discard — e.g. Saruman of Many Colors' "Ward—Discard an enchantment, instant, or
  sorcery card" (`wardDiscard(filter = Enchantment or Instant or Sorcery)`).
  `WardCost.DynamicLife(amount)` (built via `KeywordAbility.wardLife(amount: DynamicAmount)`) is a life
  cost whose amount is a `DynamicAmount` read when the ward trigger *resolves* (CR 702.21b) — e.g.
  Raubahn, Bull of Ala Mhigo's "Ward—Pay life equal to Raubahn's power"
  (`wardLife(DynamicAmounts.sourcePower())`), which reads the source's projected power then, or its
  last-known power if the source has left the battlefield (CR 112.7a, via `EntityReference.Source`'s
  last-known-information policy). It resolves down to a fixed `WardCost.Life` in the executor, so it
  composes inside `WardCost.Composite` and uses the same pay-or-counter prompt.
  **Ward—Waterbend `{N}`** (Avatar: The Last Airbender — The Unagi of Kyoshi Island's
  "Ward—Waterbend {4}") is `KeywordAbility.wardWaterbend("{4}")` → `WardCost.Mana("{4}", waterbend = true)`.
  It is an ordinary mana ward, but while paying the `{N}` the controller may tap their untapped
  artifacts and creatures to help — each tapped permanent pays `{1}` of the generic, reusing the
  same waterbend payment machinery as the activated-ability / spell waterbend cost
  (`AlternativePaymentHandler.applyWaterbendForAbility`, `AlternativePaymentChoice.waterbendPermanents`).
  The ward's `SelectManaSourcesDecision` carries the eligible permanents in
  `waterbendPermanents`, and the player returns the chosen subset in
  `ManaSourcesSelectedResponse.waterbendPermanents`; the resumer taps them (reducing the generic owed)
  before paying any remainder with mana sources. Affordability and eligibility reuse
  `CostEnumerationUtils.findWaterbendPermanents` / `canAffordWithWaterbend`, so it stays single-sourced
  with the other waterbend surfaces.
- `Protection(color)` — protection from a single color.
- `ProtectionFrom(set)` — protection from a set of colors/types.
- `Protection(ProtectionScope.Supertype("Legendary"))` / `KeywordAbility.protectionFromSupertype("Legendary")` — protection from a supertype, e.g. "protection from legendary creatures" (Tsabo Tavoc). Enforced across targeting, blocking, and combat damage via projected `PROTECTION_FROM_SUPERTYPE_<X>` keywords.
- `Affinity(filter)` — cost reduction per matching permanent.
- `Amplify(n)` — ETB reveal-creatures-for-counters.
- `Devour(multiplier, sacrificeFilter, variant)` — "As this enters, you may sacrifice any number of [sacrificeFilter]. It enters with [multiplier] × that many +1/+1 counters." Plain Devour uses `sacrificeFilter = Creature` and `variant = ""`; the Edge of Eternities variant "Devour land N" uses `KeywordAbility.devourLand(n)` (`sacrificeFilter = Land`, `variant = "land"`). The keyword surfaces the rules text; pair with [`EntersWithDevour`](#15-replacement-effects) for the mechanical behavior.
- `Annihilator(n)` — attacker forces sacrifices.
- `Absorb(n)` — prevent N damage each time it would be dealt to this.
- `Bushido(n)` — +N/+N when blocking or blocked.
- `Rampage(n)` — +N/+N for each blocker past the first. Display-only; wire the behavior with the
  `card { rampage(n) }` builder helper, which adds this keyword ability plus a "becomes blocked"
  triggered ability granting `+n/+n × (blockers − 1)` until end of turn (mirrors `prowess()`).
- `Flurry` (Tarkir: Dragonstorm, Jeskai) — "Flurry — Whenever you cast your second spell each turn,
  [effect]." Display-only `Keyword.FLURRY`; wire the behavior with the `card { flurry { … } }` builder
  helper. Author the effect/target/optional inside the block exactly like `triggeredAbility { }` — the
  helper forces the `Triggers.NthSpellCast(2, Player.You)` trigger, adds the FLURRY tag, and prefixes the
  rendered text with "Flurry — Whenever you cast your second spell each turn," (mirrors `prowess()` /
  `rampage()`). The second-spell-cast event is matched by `EventPattern.NthSpellCastEvent`; no new engine
  subsystem is involved. Example: `flurry { effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self) }`.
- `Casualty(threshold)` (`KeywordAbility.casualty(n)`) — Casualty N (CR 702.153): "As an additional
  cost to cast this spell, you may sacrifice a creature with power N or greater. When you do, copy
  this spell and you may choose new targets for the copy." Modeled like Conspire — an optional
  additional cost plus a reflexive triggered copy (reuses `StormCopyEffect` with `copyCount = 1`).
  The cast enumerator surfaces a `CastWithCasualty` legal action whenever the caster controls a
  creature whose projected power meets the threshold; the chosen creature is submitted as
  `CastSpell.casualtyCreature` and sacrificed during payment. Grant it to a player's spells with
  `GrantKeywordToOwnSpells(keyword = Keyword.CASUALTY, spellFilter = …, keywordParameter = N)` —
  the `keywordParameter` carries the threshold for granted casualty (Silverquill, the Disputant:
  "Each instant and sorcery spell you cast has casualty 1"). `GrantedKeywordResolver.casualtyThreshold`
  reads the printed `KeywordAbility.Casualty.threshold` first, then the granting source's
  `keywordParameter`.
- `Miracle(cost)` (`KeywordAbility.miracle(cost)`) — Miracle {cost} (CR 702.94): "You may cast this
  card for its miracle cost when you draw it if it's the first card you drew this turn." Modeled as a
  hand-only alternative cost gated by a one-turn window. When a card with miracle (printed, or granted
  in hand — see below) is the first card its owner draws in a turn, the draw flow stamps it with a
  `MiracleWindowComponent` and reveals it (`CardRevealedFromDrawEvent`); the cast-from-hand enumerator
  then surfaces a "Miracle …" alternative-cost action (`AlternativeCostType.MIRACLE`) paying the
  miracle mana cost instead of the mana cost. The window is cleared at end of turn. Grant it to a
  player's hand cards with `GrantMiracleToCardsInHand(filter, cost)` (Lorehold, the Historian: "Each
  instant and sorcery card in your hand has miracle {2}"); `MiracleGrants.effectiveMiracle` is the
  single source of truth consulted by the draw flow, enumerator, and cast handler (printed wins, else
  the first matching battlefield grant on a permanent the player controls).
- `Cleave(cost)` (`KeywordAbility.cleave("{cost}")`) — Cleave {cost} (CR 702.148, Innistrad: Crimson
  Vow). Two static abilities on a spell while it's on the stack: "You may cast this spell by paying
  [cost] rather than paying its mana cost" **and** "If this spell's cleave cost was paid, change its
  text by removing all text found within square brackets." Modeled as an **alternative casting cost**
  (`AlternativeCostType.CLEAVE`) whose text change is a **structural swap done at cast time**, not
  runtime text mangling. Declare the keyword at card level, then supply the *brackets-removed* variant
  inside the `spell { }` block:
  - base `target(...)` / `effect` = the **brackets-present** (printed, restricted) shape;
  - `cleaveTarget(...)` / `cleaveEffect` = the **brackets-removed** (broadened) shape.

  When the spell is cast for its cleave cost, `CastSpellHandler` swaps `cleaveTargetRequirements` /
  `cleaveSpellEffect` (`CardScript` fields) in for the base ones, so the resolving spell only ever
  carries the cleaved shape — targeting legality, the effect, and the compiled tree all reflect it.
  Mirrors how kicker declares `kickerTarget` / `kickerEffect`. The cleave cost **never changes the
  spell's mana value** — MV is always computed from the printed mana cost (CR 202.3b), so cost
  reductions/increases keyed on MV are unchanged whether or not cleave was paid. Leave `cleaveTarget`
  unset when the removed brackets don't touch the target line (a mass-destroy or a self-effect only
  differs in `cleaveEffect`); leave `cleaveEffect` unset when only the target restriction changes and
  the effect is identical. The five VOW reference cards cover the shapes:
  - **Fierce Retribution** ({1}{W}, Cleave {5}{W}) — target only: base `Targets.AttackingCreature`,
    `cleaveTarget` `Targets.Creature`; both `Effects.Destroy` the chosen target.
  - **Wash Away** ({U}, Cleave {1}{U}{U}) — target only: base
    `TargetSpell(TargetFilter.SpellOnStack.notCastFromZone(Zone.HAND))` (the bracketed "that wasn't
    cast from its owner's hand"), `cleaveTarget` `Targets.Spell`; both `Effects.CounterSpell()`.
  - **Path of Peril** ({1}{B}{B}, Cleave {4}{W}{B}) — effect only (mass-destroy has no target):
    base `Effects.DestroyAll(Creature.manaValueAtMost(2))`, `cleaveEffect` `Effects.DestroyAll(Creature)`.
  - **Dig Up** ({G}, Cleave {1}{B}{B}{G}) — effect only, two bracket spans (`[basic land]` and
    `[reveal it,]`): base tutors a basic land to hand revealed, `cleaveEffect` tutors any card to hand
    without revealing.
  - **Alchemist's Gambit** ({1}{R}{R}, Cleave {4}{U}{U}{R}) — effect only, one bracket span is the
    drawback: base `Effects.Composite(TakeExtraTurnEffect(loseAtEndStep = true), …)`, `cleaveEffect`
    the same with `loseAtEndStep = false` — the shared "extra turn / damage can't be prevented on it /
    self-exile" clauses sit outside the brackets. (The "damage can't be prevented during that turn"
    clause is scoped to the extra turn, so it's scheduled as a `CreateDelayedTriggerEffect` firing
    `Effects.DamageCantBePreventedThisTurn()` at the next turn's upkeep, in both modes.)

  The alternative cast is surfaced like the other alt-cost keywords (see `MayCastWithoutPayingManaCost`
  above and `engine-server-interface.md`): a distinct "Cleave" legal action routed through
  `CastSpell.useAlternativeCost = true` + `alternativeCostType = AlternativeCostType.CLEAVE`, offered
  alongside the normal cast so the player explicitly picks one (CR 118.9a).
- `Afflict(n)` — defender loses N when this becomes blocked.
- `Crew(n)` (`KeywordAbility.crew(n, onceEachTurn = false)` / `Numeric(Keyword.CREW, n, onceEachTurn)`) —
  Crew N (CR 702.122): tap any number of untapped creatures you control with total power N or greater to
  animate a **Vehicle** (artifact subtype, CR 301.7) — it becomes an artifact creature until end of turn at
  its printed P/T and keywords (the engine `CrewVehicleHandler` resolves a `BecomeCreature`; surfaced as the
  `CrewVehicle` legal action). Summoning-sick creatures may crew (CR 702.122c). Pass `onceEachTurn = true` for
  "Crew N. Activate only once each turn." (Luxurious Locomotive): the crew enumerator + handler then refuse a
  second crew activation in the same turn (counted via `CrewSaddleContributorsComponent.crewActivations`, reset
  at end of turn). Vanilla Crew is uncapped. `onceEachTurn` is omitted from compiled JSON when false
  (`encodeDefaults = false`). To make a Vehicle a creature **conditionally** instead of via crew (e.g. Grond,
  the Gatebreaker — "As long as it's your turn and you control an Army, this is an artifact creature"), use a
  `staticAbility { }` with `ability = GrantCardType("CREATURE", GroupFilter.source())` gated by a `condition`
  (Layer 4 type change; printed P/T applies automatically). The same `GrantCardType("CREATURE", …)` pattern
  covers Spacecraft "it's an artifact creature at N+" (see Synthesizer Labship).
- `saddle(n)` (`KeywordAbility.saddle(n)`) — Saddle N (CR 702.171). A sorcery-speed activated
  ability whose cost is tapping any number of *other* untapped creatures you control with total
  power ≥ N; on resolution this permanent **becomes saddled** until end of turn. Reuses the same
  "tap creatures with total power N" selection as Crew (surfaced as a `SaddleMount` legal action),
  but resolves to a marker rather than animating the permanent. Read the saddled state with
  `Conditions.SourceIsSaddled` / the `saddled()` filter (e.g. `triggerCondition =
  Conditions.SourceIsSaddled` for "whenever this attacks while saddled"). The marker (engine
  `SaddledComponent`) is cleared at end of turn or when the permanent leaves the battlefield, and
  is not a copiable value (CR 702.171b). Mounts that gate on the saddled state use this.
- `Modular(n)` — ETB with +1/+1 counters, transfer on death.
- `Fading(n)` — ETB with N fade counters; removes one each upkeep, sacrifice if can't.
- `Vanishing(n)` — same idea with time counters.
- `Renown(n)` — first combat damage to a player grants renown counters.
- `Fabricate(n)` — ETB choose +1/+1 counters or Servo tokens.
- `Tribute(n)` — opponent chooses ETB bonus.
- `Mobilize(n)` — +N tapped-and-attacking 1/1 red Warrior tokens on attack (Tarkir: Dragonstorm, Mardu).
  Display-only; wire the behavior with the `card { mobilize(n) }` builder helper, which adds this keyword
  ability plus a "whenever this attacks" triggered `CreateTokenEffect` (`tapped = true`, `attacking = true`)
  whose `sacrificeAtStep = Step.END` schedules one delayed `SacrificeTargetEffect` per created token at the
  next end step (mirrors `rampage()`). `n` may be any fixed value (Mobilize 1/2/3, …).
  For a dynamic count ("Mobilize X, where X is …"), use the `card { mobilize(amount, amountDescription, label) }`
  overload: it adds a `KeywordAbility.Variable(MOBILIZE, label)` display tag (prints "Mobilize X") plus the same
  attack-triggered `CreateTokenEffect`, but with `count = amount` (any `DynamicAmount`) resolved at attack time.
  Avenger of the Fallen passes `DynamicAmounts.creatureCardsInYourGraveyard()`.
- `Firebending(n)` — "Whenever this creature attacks, add N {R}. Until end of combat, you don't lose this mana
  as steps and phases end." (CR 702.189, Avatar: The Last Airbender). Display-only; wire the behavior with the
  `card { firebending(n) }` builder helper, which adds this keyword ability plus a "whenever this attacks"
  triggered `AddManaEffect(Color.RED, n, expiry = ManaExpiry.END_OF_COMBAT)` (mirrors `mobilize()` / `rampage()`).
  The mana is ordinary red mana spendable anywhere — it is held as an `AnySpend` restricted-pool entry tagged
  with [ManaExpiry](#manaexpiry).`END_OF_COMBAT` and discarded by `CombatManager.endCombat`. It is a normal
  triggered ability (not a mana ability): it uses the stack and can be responded to. `n` may be any fixed value;
  "firebending X (X = its power)" is not yet expressible by this helper (the keyword carries only a fixed Int).
  To **grant** firebending until end of turn ("target creature gains firebending N until end of turn", Fire Nation
  Palace), use `Effects.GrantFirebending(n, target, duration = EndOfTurn)`. Because firebending has no engine
  handler, the grant reuses the *exact* attack trigger the printed keyword installs (`firebendingAttackTrigger(n)`,
  shared with `firebending(n)`) via `GrantTriggeredAbilityEffect`, so the affected creature adds the same N {R}
  combat-duration mana on attack while the grant is live. The grant rides `GameState.grantedTriggeredAbilities`
  and is dropped in the cleanup step (EndOfTurn).
  For a **conditional static** "this creature has firebending N as long as `<condition>`" (Fire Nation Cadets —
  "… as long as there's a Lesson card in your graveyard"), wrap a *self-scoped* `GrantTriggeredAbility` in a
  `ConditionalStaticAbility`: `staticAbility { ability = ConditionalStaticAbility(GrantTriggeredAbility(
  firebendingAttackTrigger(n), filter = GroupFilter.source()), Conditions.GraveyardContainsSubtype(Subtype.LESSON)) }`.
  `GroupFilter.source()` (i.e. `Scope.Self`) grants the firebending attack trigger to the source itself; the
  `TriggerAbilityResolver` re-evaluates the gating condition each time triggers are computed, so the trigger
  toggles live — it fires on attack while the condition holds and not while it's false. (Self-scoped
  `GrantTriggeredAbility`, plain or conditional, is consulted by `TriggerAbilityResolver.getSelfGrantedTriggeredAbilities`
  alongside the existing battlefield-scope/lord and attached-aura grant paths.)
- `Increment` — "Whenever you cast a spell, if the amount of mana you spent is greater than this creature's power
  or toughness, put a +1/+1 counter on this creature." (Secrets of Strixhaven). Display-only; wire the behavior with
  the `card { increment() }` builder helper, which adds the `KeywordAbility.Increment` display marker (surfacing
  `Keyword.INCREMENT`) plus a `Triggers.YouCastSpell` triggered `AddCounters(+1/+1, 1, Self)` gated by an
  intervening-if (CR 603.4) that compares the triggering spell's mana spent (`EntityProperty(Triggering,
  ManaSpent)`) against the source's power *or* toughness — modelled as an `AnyCondition` of two `Compare(GT)`
  arms, so it fires when the mana exceeds the smaller characteristic. No parameter (mirrors `firebending()` /
  `decayed()`).
- `Opus` — "Opus — Whenever you cast an instant or sorcery spell, [base]. If five or more mana was spent to cast
  that spell, [bonus] [instead]." (Secrets of Strixhaven). **Opus is an ability word** (CR 207.2c — flavor only),
  so it adds *no keyword*; the whole mechanic is one `Triggers.YouCastInstantOrSorcery` triggered ability wired by
  the `card { opus { … } }` builder helper. The 5+ mana tier is a `Compare` of
  `ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL) >= 5` (the mana spent on the *triggering* spell, not the
  resolving object's own cast). Author the base effect as `effect = …` and pick exactly one bonus setter:
  `insteadIfFiveOrMore = …` lowers to `ConditionalEffect(5+ → bonus, otherwise → base)` and renders "… [bonus]
  instead" (Deluge Virtuoso, Exhibition Tidecaller, Tackle Artist); `alsoIfFiveOrMore = …` lowers to
  `base then ConditionalEffect(5+ → bonus)` and runs the bonus *in addition* (Expressive Firedancer, Colorstorm
  Stallion). Declare a `target(name, requirement)` inside the block and reference the returned handle from both
  `effect` and the bonus so the single chosen target carries across both tiers (Exhibition Tidecaller's "target
  player mills three … mills ten instead"). The rendered ability text is auto-composed from the base/bonus effect
  descriptions unless `description` overrides the whole string.
- `Decayed` — "This creature can't block, and when it attacks, sacrifice it at end of combat" (CR 702.147,
  Innistrad: Midnight Hunt). Display-only; wire the behavior with the `card { decayed() }` builder helper, which adds
  the keyword plus a `CantBlock(GroupFilter.source())` static ability and a "whenever this attacks" triggered
  `CreateDelayedTriggerEffect(step = Step.END_COMBAT, effect = Effects.SacrificeTarget(EffectTarget.Self))` (mirrors
  Mardu Blazebringer's end-of-combat self-sacrifice). No parameter. The **decayed counter** (`Counters.DECAYED`,
  Tarkir: Dragonstorm) grants the same Decayed ability to *any* creature that bears one (CR 702.147a) — put it with
  `AddCounters(Counters.DECAYED, n, target)` (Rot-Curse Rakshasa's Renew). The engine realizes the behavior off the
  counter directly: `StateProjector` projects the `DECAYED` keyword + `cantBlock = true`, and `TriggerDetector`
  schedules the end-of-combat self-sacrifice when a decayed-countered creature is declared as an attacker — no
  per-card static/trigger needed for the counter form.
- `Exploit` — "Exploit (When this creature enters, you may sacrifice a creature.)" (CR 702.110, Dragons of Tarkir;
  reprinted MH1/MH2/VOW/PIP/MH3). Display-only keyword; wire the behavior with the `card { exploit(onExploit, onExploitTargets) }`
  builder helper. It adds the keyword plus one `EntersBattlefield` triggered ability whose effect is a
  `ReflexiveTriggerEffect(optional = true)`:
  - **action** = `CompositeEffect(SacrificeEffect(GameObjectFilter.Creature, count = 1), EmitExploitedEventEffect)` — an
    optional "you may sacrifice a creature" (any one creature the controller owns, **including this creature itself** —
    CR 701.17a scopes sacrifice to the controller, and there is no self-exclusion), immediately followed by
    `EmitExploitedEventEffect`, which fires an observable `EventPattern.ExploitedEvent` (CR 702.110b). Declining the
    optional sacrifice sacrifices nothing, so no `ExploitedEvent` is emitted and no payoff fires (satisfies CR 702.110a's "may").
  - **reflexiveEffect** = the self-bound "when this creature exploits a creature, …" payoff (`onExploit`), or a no-op
    `CompositeEffect(emptyList())` when `onExploit = null`. Prefer baking a *self-bound* payoff into the reflexive
    (established while the source is on the battlefield, run "when you do") rather than a separate SELF-bound
    `ExploitedEvent` trigger: the reflexive path needs no gone-source detection, whereas a SELF-bound `ExploitedEvent`
    watcher would rely on the sacrifice look-back below.
  - `onExploitTargets` supplies `reflexiveTargetRequirements` for a **targeted** payoff (Fell Stinger's "target player
    draws two cards and loses 2 life"), chosen *after* the sacrifice resolves.
  Examples: Stitched Assistant `exploit(onExploit = scry(1) then draw(1))` (untargeted self-payoff); Fell Stinger
  `exploit(onExploit = <target player draws 2, loses 2>, onExploitTargets = listOf(Targets.Player))`; Skull Skaab
  `exploit()` (no self-payoff) **plus** a hand-written broadcast watcher `triggeredAbility { trigger =
  EventPattern.ExploitedEvent(player = Player.You, requireNontokenExploited = true); effect = <create a 2/2 black Zombie> }`.
  **Self-exploit look-back (CR 603.10a):** sacrifice triggers "look back in time", so an exploiter's own
  `ExploitedEvent` watcher (SELF or ANY binding — e.g. Skull Skaab exploiting itself) still fires even though the
  exploiter is in the graveyard when the event resolves. `TriggerDetector.detectExploitedSelfSacrificeTriggers` supplies
  this pass by resolving the gone exploiter's last-known abilities from `event.exploiterId`; the main battlefield index
  scan handles the exploiter-still-present case, and a battlefield-presence guard keeps the two from double-firing.
  `EmitExploitedEventEffect` is an internal `data object` (no player-facing text) and should not be used directly — it is
  wired into `exploit()`. See `EventPattern.ExploitedEvent` under Sacrifice triggers for the watcher form.
- `Job select` — "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach
  this to it.)" (Final Fantasy). Equipment keyword; display-only. Wire it with the `card { jobSelect() }` builder
  helper, which adds the keyword plus an `EntersBattlefield` triggered ability composing two existing primitives
  through the token pipeline: `Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Hero"))` (no
  colors → colorless) publishes the new token's id to the `createdTokens` slot, then
  `Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))` attaches the source Equipment to that
  freshly-made token. No new effect/executor — it reuses the same create-then-attach-on-ETB chain as Auxiliary
  Boosters. Author the per-card equip cost and equipped-creature bonus alongside the `jobSelect()` call (e.g. Monk's
  Fist: `jobSelect()` + `ModifyStats(1, 0)` + `GrantSubtype("Monk", Filters.EquippedCreature)` + `equipAbility("{2}")`).
- `Toxic(n)` — adds poison counters on combat damage.
- `Cycling(cost)` — pay cost, discard, draw a card.
- `BasicLandcycling(cost)` — cycling that fetches a basic land type.
- `Typecycling(type, cost)` — cycling that fetches a card type.
- `Plot(cost)` — `KeywordAbility.plot(cost)`. Special action available during your main phase while the stack is empty: pay [cost] and exile the card from your hand. It becomes plotted (stamped with a `PlottedComponent`). On a later turn you may cast it from exile without paying its mana cost, as a sorcery (CR 718). Cast permission is granted via the engine's standard `MayPlayPermission` + `PlayWithoutPayingCostComponent`, gated by `Conditions.SourcePlottedOnPriorTurn`. No card-side wiring needed — declare the keyword ability on the card and the engine handles the rest.
- `Foretell(cost)` — `KeywordAbility.foretell(cost)` (Kaldheim, CR 702.143). A sorcery-speed **special action** available while you have priority during your own turn: pay the fixed **{2}** setup cost and exile the card from your hand **face down** (`ForetoldComponent` + `FaceDownComponent` — hidden from opponents in exile, visible to its owner). On a **later** turn (not the turn it was foretold) you may cast it from exile by paying its **foretell cost** `[cost]` rather than its mana cost. Cast permission is a standard permanent `MayPlayPermission` gated by `Conditions.SourceForetoldOnPriorTurn`, plus a `PlayWithFixedAlternativeManaCostComponent` carrying `[cost]` — the same fixed-alternative-cast machinery **Airbend** uses, honored by `CastFromZoneEnumerator` + `CastSpellHandler` and stripped on leaving exile by `StackResolver` (which also strips the `FaceDownComponent` as the card is cast face up). Structurally Plot's paid cousin (`ForetellCardHandler` / `ForetellEnumerator` mirror `PlotCardHandler` / `PlotEnumerator`): plot is free to set up and free to cast later, foretell costs {2} to exile and has a distinct foretell cost to cast. No card-side wiring needed — declare the keyword ability and the engine handles the rest.
- `Hideaway(n)` — `KeywordAbility.hideaway(n)`; display tag rendered "Hideaway N". Mechanic is composed manually via `MoveCollectionEffect(faceDown = FaceDownMode.HIDDEN, linkToSource = true)` + `CardSource.FromLinkedExile()` — the keyword itself carries no engine behavior.
- `Harmonize(cost)` — `KeywordAbility.harmonize(cost)` (Tarkir: Dragonstorm). An alternative cost to cast an instant/sorcery **from your graveyard**, like Flashback, then exile it as it resolves. As you cast it you may tap **a single** untapped creature you control to reduce the **generic** portion of the harmonize cost by that creature's (projected) power — a Convoke-style reduction, but one creature paying generic-equal-to-power instead of one mana per creature. No card-side wiring: declare the keyword ability and the engine handles graveyard-cast enumeration (`CastWithHarmonize`), the per-creature reduction (routed through `AlternativePaymentChoice.harmonizeCreature`), and the exile-on-resolution. The chosen creature and its power are surfaced to the client via `LegalAction.harmonizeCreatures` / `hasHarmonize`; the client offers an on-battlefield single-creature tap step (the `harmonize` pipeline phase + `HarmonizeSelector` HUD, mirroring Convoke). **Harmonize {X}** (e.g. Nature's Rhythm `{X}{G}{G}{G}{G}`): the `CastWithHarmonize` action surfaces `hasXCost`/`maxAffordableX` (max X folds in the best single-creature tap reduction) so the client prompts for X. {X} is generic mana, so the tap reduces the mana paid *for X* — `CastSpellHandler.harmonizePaymentXValue` lowers the X mana once `reduceGeneric` has consumed any printed generic — while the chosen X stamped onto `SpellOnStackComponent.xValue` (and read by the effect, e.g. "mana value X or less") is unchanged. Colored pips are never reduced. **Granting harmonize at runtime:** harmonize can also be granted to a graveyard card that doesn't print it via `Effects.GrantHarmonize(target, cost?, duration)` (Songcrafter Mage). The grant is a `GrantedKeywordAbility` record keyed to the card entity; every harmonize read site consults printed-**or**-granted harmonize through the `HarmonizeGrants.effectiveHarmonize` resolver, so a granted harmonize is castable, reducible, and exiled exactly like a printed one. The grant survives the graveyard → stack move (so exile-on-resolution still fires) and is cleared in the cleanup step.
- **Waterbend** (Avatar: The Last Airbender) — *not a keyword ability*; a cost flag on an activated ability. Set `hasWaterbend = true` in the `activatedAbility { }` block (alongside a `cost = Costs.Mana("{N}")`). It means "Waterbend {N}: pay {N}, but for each generic mana in that cost you may tap an untapped **artifact or creature** you control instead." It is Convoke widened to artifacts and restricted to generic-only payment — a tapped permanent never covers a colored pip, and the number of taps is bounded by the generic mana in the cost (CR; you can tap a permanent that just came under your control, no summoning-sickness gate). Routed through `AlternativePaymentChoice.waterbendPermanents` (a `Set<EntityId>`), mirroring `hasConvoke`: the activated-ability handler applies it via `AlternativePaymentHandler.applyWaterbendForAbility`, the enumerator surfaces `LegalAction.hasWaterbend` / `waterbendPermanents` (via `CostEnumerationUtils.findWaterbendPermanents` + `canAffordWithWaterbend`), and the client offers an on-battlefield tap step (the `waterbend` pipeline phase + `WaterbendSelector` HUD, generic-only). The ability's `description` auto-prefixes "Waterbend " before the cost.
- **Spell-level waterbend additional cost** (Avatar: The Last Airbender) — *"As an additional cost to cast this spell, [you may] waterbend {N}."* Declared in the card builder with `waterbendCost(amount, optional = false, isX = false)`, which sets `CardScript.spellWaterbend: SpellWaterbendCost`. It adds {N} generic to the spell's cost; the same `AlternativePaymentChoice.waterbendPermanents` taps pay it, **bounded by N** so taps never cover the spell's own generic. `optional = true` models "you may waterbend {N}" — the enumerator offers a second, *paid* cast variant, and paying it sets `ChoiceSlot.WATERBEND_PAID` so the effect branches via `Conditions.WaterbendWasPaid` (the waterbend analogue of `BlightWasPaid`, e.g. `ConditionalEffect(Conditions.WaterbendWasPaid, paidEffect, elseEffect = baseEffect)`); a mandatory cost always adds {N}. Wiring: `CastSpellHandler` adds {N} and applies `AlternativePaymentHandler.applyWaterbendForSpell` (capped at N); `CastSpellEnumerator` surfaces `hasWaterbend`/`waterbendPermanents` on the cast action, reusing the same client `waterbend` pipeline phase + `WaterbendSelector`. Cards: Benevolent River Spirit (mandatory {5}), Ruinous Waterbending (optional {4}), Spirit Water Revival (optional {6}). The **`isX` "waterbend {X}" shape** (`waterbendCost(isX = true)`) is fully wired: the enumerator folds a literal `{X}` into the cost so the spell reads as X-carrying (`maxAffordableX` bounded by available mana **plus** tappable permanents), the client prompts for X then runs the waterbend tap step (capped at the chosen X), and the resolver charges X as the waterbend generic — so X also feeds the effect via `DynamicAmount.XValue`. *(The two `isX` cards Crashing Wave and Foggy Swamp Visions each still need a card-specific effect beyond the cost — "distribute N counters among a filtered group chosen at resolution", and "token copy of each exiled card" + delayed sacrifice — before they can ship.)* The **in-resolution "unless you waterbend {N}" shape** (Waterbending Lesson) is wired separately as `Effects.UnlessYouWaterbend(amount, otherwise)` — a `Gate.MayPay` over a waterbend-flagged `PayManaCostEffect`, resolved during the spell's resolution rather than as a cast-time cost (see the gated-effects section).
- `OptionalAdditionalCost(manaCost?, additionalCost?, multi, displayPrefix, branchesEffect, grantsFlashTiming)` — generalised "pay an optional extra cost while casting" primitive. Backs printed Kicker / Multikicker / Offspring **and** the pre-kicker "pay {N} more to cast as though it had flash" pattern (Ghitu Fire). When `branchesEffect = true` (default) paying the cost marks the spell so `WasKicked` fires for the card's own effect/triggers; when `false` the payment is invisible to `WasKicked` (used by `flashKicker`). When `grantsFlashTiming = true` paying the cost unlocks instant-speed casting in addition to whatever else it does — the optional cost may be mana (Ghitu Fire: `KeywordAbility.flashKicker("{2}")`) **or** a non-mana `additionalCost` such as Behold (Molten Exhale: "you may cast this as though it had flash if you behold a Dragon", `KeywordAbility.flashKicker(Costs.additional.Behold(filter = Filters.WithSubtype("Dragon")))`). Prefer the factories: `KeywordAbility.kicker(cost)`, `KeywordAbility.kicker(additionalCost)`, `KeywordAbility.multikicker(cost)`, `KeywordAbility.offspring(cost)`, `KeywordAbility.flashKicker(cost)`, `KeywordAbility.flashKicker(additionalCost)`. Serial name is `Kicker` for wire compatibility. **Kicker {X}** (variable kicker, e.g. `KeywordAbility.kicker("{X}")` on Verdeloth the Ancient): the kicked cast surfaces `hasXCost`/`maxAffordableX` so the client prompts for X exactly like a base-cost X spell; the chosen X is paid as part of the kicker and stamped onto `SpellOnStackComponent.xValue`, so the card's ETB trigger reads it via `DynamicAmount.XValue` ("create X tokens").
- `Impending(time, cost)` — `card { impending(n, cost) }` builder helper (CR 702.176, Duskmourn). A self-alternative
  cost: pay [cost] instead of the mana cost and the permanent enters with N **time counters**, isn't a creature until
  the last is removed, and loses one at the beginning of your end step. The helper wires everything from one call — the
  `KeywordAbility.Impending` alt-cost (display + cast enumeration), a `ConditionalStaticAbility(RemoveCardType("CREATURE"),
  Conditions.SourceHasCounter(TIME))` "isn't a creature while it has a time counter" static, and a `YourEndStep`
  triggered ability (gated by the same intervening-if) that removes a time counter. The engine places the N TIME counters
  when a spell cast for its impending cost resolves; casting for the normal mana cost adds no counters, so neither wiring
  fires (mirrors `prowess()` / `rampage()`).
- `Sneak(cost)` — `card { sneak("{cost}") }` builder helper (CR 702.190, Teenage Mutant Ninja Turtles). An
  alternative cost with a built-in **timing permission**: *"Any time you could cast an instant during your declare
  blockers step, you may cast this spell by paying [cost] and returning an unblocked creature you control to its owner's
  hand rather than paying this spell's mana cost."* A permanent spell whose sneak cost was paid enters **tapped and
  attacking** the same defender the returned creature was attacking (CR 702.190b). The helper just attaches the
  `KeywordAbility.Sneak` display marker; all behavior is in the engine: the dedicated `SneakCastEnumerator` surfaces a
  `CastWithAlternativeCost` (`AlternativeCostType.SNEAK`) only during the active player's declare blockers step while they
  control an unblocked attacker, with a `BouncePermanent` additional cost listing the returnable attackers; `CastSpellHandler`
  charges the sneak mana, returns the chosen attacker to hand, and stamps the sneak-was-paid flag; `StackResolver` enters a
  resolving permanent tapped and attacking. Read "its sneak cost was paid" via `Conditions.SneakCostWasPaid`.
- `Ninjutsu(cost)` — `card { ninjutsu("{cost}") }` builder helper (CR 702.49). *"[cost], Return an unblocked attacker you
  control to hand: Put this card onto the battlefield from your hand tapped and attacking."* **Mechanically identical to
  `Sneak`** — Ninjutsu is the canonical rules keyword, `Sneak` its reflavor in the custom TMNT set — so both share the
  engine's declare-blockers alternative-cost pipeline. The two keyword abilities expose their cost through one property,
  `KeywordAbility.ninjutsuStyleCost`, which `SneakWindow`/`SneakCastEnumerator`/`CastSpellHandler` read; a new reflavor of
  the mechanic only overrides that property. A card put onto the battlefield this way enters **tapped and attacking** the
  same defender the returned creature was attacking (CR 506.3a); a card that isn't a creature as it enters (e.g. an
  un-animated planeswalker) just enters tapped. Used by *Kaito, Bane of Nightmares* (DSK) — a planeswalker with ninjutsu
  whose own static makes it a creature on your turn, so it can enter attacking.
- `Suspend` (CR 702.62) — an **exile-zone** mechanic, unlike Impending/Vanishing which live on the battlefield.
  A suspended card sits in exile with **time counters**; at the beginning of its **owner's** upkeep one is removed,
  and when the last is gone its owner **may play it for free**, with **haste** if it's a creature. The lifecycle is
  **component-driven**, not definition-driven: the engine grants `Suspend.countdownAbility` (a synthesized
  `activeZone = EXILE` upkeep trigger — remove a counter, then a `MayEffect` that gathers the card via
  `CardSource.Self` and casts it with `CastFromCollectionWithoutPayingCostEffect`) to **any** exiled card carrying the
  `SuspendedComponent` marker. So an arbitrary card with no printed suspend can be suspended.
  - **Putting a card into suspend** is a chain you compose; `Effects.Suspend(target, timeCounters)` is the reusable
    two-step tail (`AddCounters(TIME, n)` + `GrantSuspendEffect` — the latter sets the marker **and** arms a dormant
    haste effect on the card with duration `WhileControlledByController`, so the haste ends the moment the player who
    played it loses control of the permanent — CR 702.62g). The caller supplies the exile step first, because it differs by source zone:
    a spell on the stack uses `CounterSpellToExile` / `CounterEffect(counterDestination = Exile())` (it can't be lifted
    off the stack with a zone-move); a printed `suspend N—[cost]` exiles from hand as its cast cost.
  - **Taigam, Master Opportunist** is the first user: `Composite(CopyTargetSpell(TriggeringEntity),
    CounterEffect(TriggeringEntity → Exile), Suspend(TriggeringEntity, 4))`.
- `Paradigm` (Secrets of Strixhaven) — `spell { effect = …; paradigm() }` on a Lesson spell. An **exile-zone
  recurrence** mechanic, modelled exactly like Suspend (a marker the engine reads off an exiled card), differing
  only in that it casts a **copy** rather than the card itself, so the original recurs forever. Oracle: "[effect]
  Then exile this spell. After you first resolve a spell with this name, you may cast a copy of it from exile
  without paying its mana cost at the beginning of each of your first main phases." `paradigm()` implies
  `selfExile()`: the spell exiles itself on resolution (reusing the `selfExileOnResolve` → `StackResolver` exile
  path) and is tagged with the `ParadigmComponent` marker as it lands in exile; the `Keyword.PARADIGM` display
  keyword is added automatically. The engine then grants `Paradigm.recastAbility` — a synthesized
  `activeZone = EXILE`, `StepEvent(PRECOMBAT_MAIN, You)` trigger whose `MayEffect` copies the card via
  `CopyCardIntoCollectionEffect(Self)` and casts it with `CastFromCollectionWithoutPayingCostEffect` — to **any**
  exiled card carrying the marker (the marker is the gate: a Lesson exiled by some other path never recurs). The
  original stays in exile; each cast copy is a phantom that ceases to exist (CR 707.10a / 112.3b), so there is no
  exponential growth. The `Lesson` spell subtype (`Subtype.LESSON`) is a plain, non-functional subtype (no Learn
  mechanic in the set), but the type line must parse it.
- `Craft(filter, cost)` — `card { craft(filter, cost, materialDescription?, minCount = 1, maxCount = null) }`
  builder helper (CR 702.167, The Lost Caverns of
  Ixalan). On the front face of a transforming DFC: "Craft with [filter] [cost] ([cost], Exile this permanent,
  Exile [filter] you control and/or [filter] cards from your graveyard: Return this card to the battlefield
  transformed under its owner's control. Activate only as a sorcery.)" `minCount`/`maxCount` bound the material
  count: exact-count wordings ("Craft with artifact" = exactly one, "Craft with two creatures" = exactly two)
  pass `maxCount = minCount`; "one or more [filter]s" leaves `maxCount = null`. Composes entirely from existing primitives
  — `AbilityCost.Composite(Mana(cost), AbilityCost.Craft(filter, minCount, maxCount))` (the atomic `Craft` sub-cost handles both the
  self-exile and the materials-exile because CR 702.167a defines them as one paired clause), plus
  `Effects.ReturnSelfFromExileTransformed` as the resolution effect, and `timing = TimingRule.SorcerySpeed`.
  Records the exiled materials on the source's `CraftedFromExiledComponent` so the back face's CDA
  ("Mastercraft Raptor's power is equal to the total power of the exiled cards used to craft it", CR 702.167c)
  can read them via `DynamicAmount.CraftedMaterialsTotalPower`. Declares `Keyword.CRAFT` for display.

  Material selection: the engine surfaces the combined BF + GY candidate pool on each Craft activation as
  `AdditionalCostData.validCraftMaterials` / `craftMinCount` / `craftMaxCount`. The web client renders both zones side-by-side
  via the dedicated `CraftMaterialOverlay` (routed by the `Craft` cost-type branch in `pipelinePhases`) and
  submits the picked IDs back as `ActivateAbility.costPayment.exiledCards`. Headless / game-server callers can
  supply the chosen IDs directly. The cost handler validates that every chosen entity is either a permanent
  the activator controls or a card in their graveyard matching `filter`, and rejects activation when no
  choices are supplied (no silent auto-pick).

- `Renew(cost)` — `card { renew(cost) { effect = … } }` builder helper (Tarkir: Dragonstorm, Sultai clan keyword).
  A graveyard-activated ability: "Renew — [cost], Exile this card from your graveyard: [effect]. Activate only as a
  sorcery." The helper composes it entirely from existing primitives — `AbilityCost.Composite(Mana(cost), ExileSelf)`,
  `activateFromZone = Zone.GRAVEYARD`, and `timing = TimingRule.SorcerySpeed` — so no new engine subsystem is involved.
  The `renew { }` lambda configures the effect (and any targets via `target(name, requirement)`) exactly like
  `activatedAbility { }`; its `cost`/`timing`/`activateFromZone` fields are ignored (fixed by Renew). The
  `GraveyardAbilityEnumerator` surfaces the ability while the card is in the graveyard and only at sorcery speed; the
  `ActivateAbilityHandler` pays the mana and exiles the card from the graveyard. Declares `Keyword.RENEW` for display.
- `station()` — `card { station() }` builder helper (CR 702.184, Edge of Eternities; Spacecraft and Planet cards).
  Emits the fixed station keyword ability (CR 702.184a): "Tap another untapped creature you control: Put a number of
  charge counters on this permanent equal to the tapped creature's power. Activate only as a sorcery." The ability is
  fully fixed by the rules, so the helper takes no arguments — it builds
  `AbilityCost.TapPermanents(count = 1, filter = Creature, excludeSelf = true)` →
  `Effects.AddDynamicCounters(Counters.CHARGE, DynamicAmount.StationCharge, Self)` at `TimingRule.SorcerySpeed`. The
  charge amount is the dedicated `DynamicAmount.StationCharge` node (see §13), *not* a plain
  `EntityProperty(TappedAsCost, Power)` read, so the CR 702.184c "station using toughness" substitution
  (`StationUsingToughness`, Tapestry Warden) stays scoped to station abilities. What the card gains at each charge
  threshold (the `{N+}` station symbols, CR 721.2a) is authored separately per card — `staticAbility { }` rows for
  Spacecraft that grant `GrantKeyword(...)` / `GrantCardType("CREATURE", …)`, or threshold-gated activated abilities
  for Planets — each gated on `Conditions.SourceCounterCountAtLeast(Counters.CHARGE, N)` (see §12). No dedicated
  `Keyword.STATION`: the layout/symbols are display-only and the ability is the whole mechanic.
  - **Multi-select activation shortcut.** Because the station ability has no chosen targets and its
    effect stacks, the player may station with several creatures in one gesture: the cost-selection
    UI lets them pick 1..N distinct untapped creatures and the engine queues one activation per
    creature on the stack (each taps exactly its creature and charges by *that* creature's power).
    This is a pure UX convenience over activating station repeatedly — the resulting state is
    identical to doing it one creature at a time — and is wired generically, not just for station:
    any single-creature (`count == 1`) `TapPermanents`-cost activated ability with no target
    requirements, a repeat-stacking effect, and no once-/max-per-turn restriction is offered the
    same batch. It rides the existing `ActivateAbility.repeatCount` batch-activation path; the
    server advertises the cap as `AdditionalCostInfo.tapBatchMaxActivations` (the count of legal tap
    targets) and `ActivateAbilityHandler` slices `costPayment.tappedPermanents` one creature per
    activation. Selecting a single creature is the unchanged single-station behaviour. (Saddle/Mount,
    by contrast, already taps any number of creatures within one activation — a different shape —
    so it is unaffected.)
- `Morph(cost)` — cast face-down for `{3}`, flip for cost.
- `Unmorph(cost, effect)` — turn-face-up cost + bonus effect.
- `Equip(cost)` — Equipment attach cost. The `equipAbility(cost, genericCostReduction = …)` DSL
  form optionally reduces the generic portion of the equip cost by a `DynamicAmount` evaluated at
  activation. Reductions that read the chosen equip target (e.g. `DynamicAmounts.targetColorCount()`
  for "costs {1} less to activate for each color of the creature it targets" — Dragonfire Blade)
  resolve against the picked target. Backed by `ActivatedAbility.genericCostReduction`: the
  `ActivateAbilityHandler` locks the per-target reduction in before paying; the legal-action
  enumerator gates affordability on the cheapest reachable cost (largest reduction over the
  currently-legal targets) since the target isn't chosen until activation. The synthesized ability
  carries `ActivatedAbility.isEquipAbility = true`, which the engine keys off for the equip-timing
  and free-first-equip permissions below. For a **non-mana equip cost** ("Equip—Sacrifice a
  creature" — Dissection Tools) the `equipAbility(cost)` helper doesn't apply (it only parses a mana
  cost); author the ability by hand as `activatedAbility { cost = Costs.Sacrifice(...); isEquipAbility
  = true; timing = TimingRule.SorcerySpeed; … }`. The `activatedAbility { }` builder exposes
  `isEquipAbility` so a hand-rolled equip ability still participates in equip-specific rules
  (sorcery-speed default, equip-cost reductions, instant-speed-equip permissions).
- `Fortify(cost)` — Aura-like attach cost on lands.

```kotlin
keywords(Keyword.FLYING, Keyword.VIGILANCE)
keywordAbility(KeywordAbility.Ward(2))
keywordAbilities(KeywordAbility.Protection(Color.BLUE), KeywordAbility.Annihilator(2))
```

---

## 12. Conditions (`Conditions.*`)

**One "entity matches a filter" primitive.** "Does *some entity* match a `GameObjectFilter`" is a
single condition — `EntityMatches(entity: EffectTarget, filter)` — that names *which* entity via the
shared `EffectTarget` vocabulary. It subsumes the four former near-clones; each is now a facade
helper over it:

| Helper | Desugars to |
|---|---|
| `Conditions.SourceMatches(f)` (and every `SourceIs*` / `SourceHas*`) | `EntityMatches(EffectTarget.Self, f)` |
| `Conditions.EnchantedPermanentMatches(f)` | `EntityMatches(EffectTarget.EnchantedPermanent, f)` |
| `Conditions.TargetMatchesFilter(f, i)` | `EntityMatches(EffectTarget.ContextTarget(i), f)` |
| `Conditions.TriggeringSpellMatches(f)` | `EntityMatches(EffectTarget.TriggeringEntity, f)` |
| `Conditions.DiscardedCardMatches(f, i = 0)` | `EntityMatches(EffectTarget.DiscardedAsCost(i), f)` |

The entity role fixes *when* the condition can be answered: `Self` and the enchanted/equipped
attachment roles evaluate in both resolution and static-ability projection; `ContextTarget`,
`TriggeringEntity`, and `DiscardedAsCost` are resolution-only (false under projection). Use `Conditions.EntityMatches`
directly only for a role the helpers don't name (e.g. the equipped creature). It is deliberately
*not* a player check (`TargetIsPlayer`) nor a numeric/tracker check (`Compare`). Any other
`EffectTarget` role is rejected by the `CardLinter` at card load (§21) — the evaluator can't
answer it and would silently return `false`.

**Two anti-patterns to avoid when adding conditions:**

- **Tracker-shaped conditions route through `Compare` + a tracked amount**, not a new condition
  class. "You gained 3+ life this turn" is `Compare(TurnTracking(You, LIFE_GAINED), GTE, 3)`. When
  the tracker the comparison needs doesn't exist yet, add the *tracker enum value* (data) and reach
  for `Compare` — don't mint a bespoke `You…ThisTurn` condition.
- **Set-mechanic conditions are quarantined** in mechanic-named files (next to the mechanic's other
  SDK surface), never added to the general condition files. The `add-feature` checklist asks this
  placement question explicitly.

### Battlefield state

- `YouControl(filter, negate = false, excludeSelf = false)` — you control ≥1 matching permanent.
  Set `excludeSelf = true` for "another …" wording, which excludes the resolving source from the
  search (e.g. Splitskin Doll's "another creature with power 2 or less").
- `YouControlAtLeast(count, filter)` — you control `count` or more matching permanents (the
  filtered-count generalization of `ControlCreaturesAtLeast`/`ControlLandsAtLeast`; e.g.
  `YouControlAtLeast(3, GameObjectFilter.Creature.attacking())` for Stormbeacon Blade).
- `ControlCreature` — you control any creature.
- `NoCreaturesOnBattlefield` — there are no creatures anywhere on the battlefield (global, either player;
  `Exists(Player.Each, …, negate = true)`). Used by Drop of Honey's "when there are no creatures on the
  battlefield, sacrifice this enchantment" state trigger.
- `ControlMoreCreatures` — you control more creatures than each opponent.
- `OpponentControlsCreature` — at least one opponent has a creature.
- `OpponentControlsMoreCreatures` — an opponent outpaces you.
- `OpponentControlsMoreLands` — an opponent has more lands.
- `OpponentHasMoreCardsInHand` — an opponent has more cards in hand than you (compares opponents' hand size to yours). Used by Beza, the Bounding Spring and Joined Researchers.
- `DefendingPlayerControlsLandType(type)` — the defending player controls a land of a type (CantAttackUnless template; defender-relative, not any-opponent).
- `CompareAmounts(left, operator, right)` — generic numeric comparison of two `DynamicAmount`s with a
  `ComparisonOperator.{LT,LTE,EQ,NEQ,GT,GTE}` (composes the underlying `Compare` condition). The facade
  entry point for any "if amount X (relation) amount Y" intervening-if or static gate. Used by Taii
  Wakeen, Perfect Shot's intervening-if: `CompareAmounts(ContextProperty(TRIGGER_DAMAGE_AMOUNT), EQ,
  ContextProperty(TRIGGER_RECIPIENT_TOUGHNESS))`.
- `AmountIsPrime(amount)` / `AmountIsEven(amount)` / `AmountIsOdd(amount)` /
  `AmountIsMultipleOf(amount, divisor)` — the **unary** numeric-predicate family, the counterpart to
  `CompareAmounts` for properties a two-sided threshold can't express (primality, parity,
  divisibility). Each desugars to `NumberMatches(amount, NumberProperty.{Prime,Even,Odd,MultipleOf})`;
  the arithmetic lives in the engine's `ConditionEvaluator` (the SDK `NumberProperty` is pure data,
  exactly like `ComparisonOperator`). Dual-mode (resolution + projection), so it gates either a
  triggered ability's intervening-if or an "as long as" static. `0` and `1` are not prime; `0` is
  even and a multiple of every nonzero divisor. Used by Zimone, All-Questioning ("if … you control a
  prime number of lands": `AmountIsPrime(AggregateBattlefield(You, Land))`).
- `YouHaveUnspentManaAtLeast(amount)` — true while your mana pool holds at least `amount` unspent
  mana. Desugars to `CompareAmounts(UnspentMana(You), GTE, Fixed(amount))`; dual-mode, so it gates an
  "as long as you have six or more unspent mana" conditional static (Ozai, the Phoenix King).
- `DifferentCounterKindsAtLeast(count, filter = Creature)` — true when `count` or more *different
  kinds* of counters are among permanents you control matching `filter` (default: creatures). A
  +1/+1 and a finality counter is two kinds; the same kind on several permanents counts once.
  Board-derived only, so it gates a `ConditionalStaticAbility` (evaluates identically in resolution
  and projection). Desugars to `Compare(AggregateBattlefield(You, filter, DISTINCT_COUNTER_TYPES),
  GTE, count)`. Used by Hundred-Battle Veteran ("three or more different kinds of counters among
  creatures you control").
- `CounterKindAmongYouControlAtLeast(count, counterType, filter)` — true when the *total* number of
  `counterType` counters (a `CounterTypeFilter`, e.g. `CounterTypeFilter.Named("lore")`, or `.Any`
  to total every kind) among permanents you control matching `filter` is at least `count`. Sums the
  kind across the whole group — three Sagas with one, two, and one lore counter total four.
  Board-derived only (gates a `ConditionalStaticAbility`; evaluates identically in resolution and
  projection). Desugars to `Compare(AggregateBattlefield(You, filter, SUM, counterType = …), GTE,
  count)`. Used by Tom Bombadil ("As long as there are four or more lore counters among Sagas you
  control, … hexproof and indestructible").
- `TriggeringEntityHadCounters` — intervening-if for dies/leaves triggers: true when the triggering
  entity had ≥1 counter of *any* kind on it the moment it left the battlefield (reads the last-known
  total counter count, CR 603.10 / 603.6c). Resolution-only. Pair with `Triggers.YourCreatureDies` +
  `Effects.MoveAllLastKnownCounters` for "whenever this or another creature you control dies, if it
  had counters on it, move its counters" (Host of the Hereafter). Companion to the existing
  `TriggeringEntityHadMinusOneMinusOneCounter` (which checks only -1/-1 counters, e.g. Retched Wretch).
- `TargetControlsCreature(target)` — target player has a creature.
- `TargetControlsLand(target)` — target player has a land.
- `TargetMatchesFilter(filter, targetIndex = 0)` — the context target matches a `GameObjectFilter`.
  Resolution-only; backed by `EntityMatches(EffectTarget.ContextTarget(targetIndex), filter)`.
- `TargetIsPlayer(targetIndex = 0)` — the context target is a player (not a permanent/spell/card).
  `TargetMatchesFilter` matches only game objects and returns false for a player target, so this is
  the dedicated check for "any target" effects with a player-only follow-up. Used by Sonic Shrieker
  ("If a player is dealt damage this way, they discard a card"); pair with
  `EffectTarget.ContextTarget(index)` to make that same player the subject of the follow-up.
- `TriggeringPlayerIs(player)` — the player who triggered this ability equals another resolved
  `Player` reference. Narrows a broad "whenever a player …" trigger to one player without a bespoke
  event filter; both sides resolve through the shared player resolver. Shinryu, Transcendent Rival
  gates "When the chosen player loses the game, you win the game" with
  `triggerCondition = TriggeringPlayerIs(Player.ChosenOpponent)` on `Triggers.AnyPlayerLosesGame`.
- `TargetIsCreatureCard(targetIndex = 0)` — the context target is a creature *card*, tested by the
  underlying card's printed types rather than projected state. Unlike `TargetMatchesFilter(Creature)`
  (which reads projection, where a face-down permanent always projects as a typeless 2/2 Creature),
  this reads the hidden card itself (CR 708.2) — the correct test for "...if it's a creature card"
  over a face-down permanent. Resolution-only. Used by Hauntwoods Shrieker ("Reveal target face-down
  permanent. If it's a creature card, you may turn it face up.").
- `TargetIsSpellOnStack(targetIndex = 0)` — the context target is a **spell on the stack** (a
  `ChosenTarget.Spell`) rather than a permanent. The zone-aware test needed to branch a single "target
  creature **or spell**" target — a creature *spell* on the stack still satisfies a `Creature`
  `GameObjectFilter`, so `TargetMatchesFilter` can't distinguish it. Resolution-only. Used by Aang,
  Swift Savior (the airbend stack branch).
- `TargetIsTapped(targetIndex = 0)` — the context target resolves to a tapped battlefield permanent.
  Non-permanent targets and permanents no longer on the battlefield return false. Branch on a target's
  tapped state at resolution via `ConditionalEffect` — used by Shackle Slinger ("If it's tapped, put a
  stun counter on it. Otherwise, tap it.").
- `TargetIsSource(targetIndex = 0)` — the context target resolves to the ability's own source
  permanent. Wrap in `Conditions.Not(...)` for "another"/"a different permanent" wordings — used by
  Arid Archway ("If another Desert was returned this way, surveil 1": `All(TargetMatchesFilter(Desert
  land), Not(TargetIsSource()))` checks the chosen land *before* the bounce, so returning the Archway
  itself doesn't count).
- `IfTargetTookExcessDamage(targetIndex = 0)` — true post-damage when the target creature's marked
  damage strictly exceeds its (projected) toughness. Chain after `Effects.DealDamage` in a composite
  so the marked-damage update applies before the condition reads it. Used by Orbital Plunge ("If
  excess damage was dealt this way, create a Lander token"). Semantics caveat: the read is
  `marked > toughness` regardless of which preceding step dealt the damage — Composite doesn't
  interleave SBA or fire triggers mid-chain, so for the canonical "deal N, then check" pipeline
  this is equivalent to "did the preceding step deal excess". A chain that deals damage in
  multiple steps within the same composite would see cumulative damage; reach for a different
  condition there. Defensive guards return false for non-creature targets and targets no longer
  on the battlefield (unreachable under `Targets.Creature` + Composite, retained for future
  callers).
- `TargetSharesMostCommonColor(targetIndex = 0)` — the context target shares a color with the
  most common color among all permanents, or a color tied for most common. Tallies each of the
  five colors across every battlefield permanent (multicolored permanents count once per color,
  using projected colors), takes the highest tally, and checks whether the target has any color
  in that (possibly tied) most-common set. A board with no colored permanents is `false`. Used by
  Tsabo's Assassin.
- `ColorIsMostCommon(color)` — the self-gating sibling of the above: true when `color` is the most
  common color among all permanents, or tied for most common (same tally rules). Board-derived
  only — no targets/triggering/kicker — so it evaluates identically in resolution and in
  projection, which lets it gate a `ConditionalStaticAbility`. Used by the Invasion djinn cycle
  ("as long as [color] is the most common color among all permanents…" — Goham/Halam/Ruham/Sulam/Zanam).
- `AnotherPermanentWithSameNameAsTarget(targetIndex = 0)` — true when at least one *other*
  battlefield permanent shares the exact card name of the context target at `targetIndex`. The
  target itself is excluded, so a lone copy never satisfies its own check; tokens compare by name
  like any other permanent. Resolution-only (reads a chosen target). Used by Winnow ("Destroy
  target nonland permanent if another permanent with the same name is on the battlefield").
- `EnchantedPermanentMatches(filter)` — true when the permanent the source Aura is attached to
  matches a `GameObjectFilter` (color, type, etc.), evaluated in projected state via the Aura's
  `AttachedToComponent`. General-purpose counterpart to the narrow `EnchantedCreatureIsLegendary` /
  `EnchantedCreatureHasSubtype` conditions. Backed by
  `EntityMatches(EffectTarget.EnchantedPermanent, filter)`; works as a `ConditionalStaticAbility`
  gate (also in the trigger resolver for conditionally-granted abilities). Used by Essence Leak ("as
  long as enchanted permanent is red or green…", `GameObjectFilter.Permanent.withAnyColor(Color.RED,
  Color.GREEN)`).
- `YouHaveCitysBlessing` — you have City's Blessing (10+ permanents).
- `SourceIsRingBearer` — the source permanent is your Ring-bearer (CR 701.54e).
- `YouChoseOtherCreatureAsRingBearer` — intervening-if for `Triggers.RingTemptsYou` payoffs that fire
  only when the controller chose a Ring-bearer other than the source (CR 701.54a). True iff the
  controller currently has a Ring-bearer designated AND that bearer isn't the source — so it's false
  both when the source itself was chosen and when the controller had no creature to choose. Used by
  Aragorn (Company Leader), Faramir (Field Commander), Gandalf (Friend of the Shire), and Galadriel
  of Lothlórien.
- `RingHasTemptedYouAtLeast(times)` — the Ring has tempted you `times` or more times this game
  (CR 701.54). Reads the cumulative `temptCount` on your The Ring emblem; a player never tempted
  counts as 0. Works at resolution and as an intervening-if. Used by Frodo, Sauron's Bane's granted
  Rogue ability ("that player loses the game if the Ring has tempted you four or more times this
  game"). Backed by `RingHasTemptedPlayerAtLeast(times, Player.You)`.

### Life & damage

- `LifeAtLeast(n, player?)` — player has ≥N life.
- `LifeAtMost(n, player?)` — player has ≤N life.
- `APlayerLifeAtMost(n)` — *some* player in the game has ≤N life (existential over `state.turnOrder`; distinct from `LifeAtMost`, which is `Player.You`). Used by enters-tapped-unless lands like Razortrap Gorge.
- `YouLostLife` — you lost life this turn.
- `OpponentLostLife` — an opponent lost life this turn.
- `PlayerLostLifeThisTurn(player)` — a specific player lost life this turn. Use when the wording
  binds the check to a particular player rather than "an opponent" — Thought-Stalker Warlock's
  "choose target opponent. If THEY lost life this turn, …" is
  `PlayerLostLifeThisTurn(Player.ContextPlayer(0))` (the chosen target, not any opponent).
- `YouGainedLifeThisTurn` — you gained ≥1 life this turn (intervening-if / static gate; backed by the
  `LIFE_GAINED` turn tracker). Used by Ulna Alley Shopkeep's Infusion buff.
- `YouGainedLifeThisTurnAtLeast(n)` — you gained ≥`n` life this turn. The threshold form of
  `YouGainedLifeThisTurn` (`Compare(TurnTracking(You, LIFE_GAINED), GTE, n)`). Used by Scheming
  Silvertongue's "if you gained 2 or more life this turn" prepared trigger.
- `CardsPutIntoExileThisTurn(atLeast = 1)` — `atLeast` or more cards were put into exile this turn,
  game-wide (summed across every player via `Player.Each`, backed by the `CARDS_PUT_INTO_EXILE`
  turn tracker), not just yours. Used by Ennis, Debate Moderator's end-step "if one or more cards
  were put into exile this turn".

### Cast / cost

- `WasCast` — source was cast (not put onto the stack).
- `TriggeringEntityWasCast` — the *triggering* entity (not the ability's source) was cast — i.e. it
  carries a cast-origin marker (`CastFromHandComponent` / `CastFromGraveyardComponent`). The
  cast-subject sibling of `WasCast`, for "whenever a creature you control enters, **if you cast it**,
  …" intervening-if triggers where the source is a separate permanent and the cast subject is the
  entering creature. Tokens, reanimated permanents, and "put onto the battlefield" permanents lack
  the markers and are correctly excluded. Used by **The Sibsig Ceremony** (a plain `WasCast` there
  would test the enchantment, not the entering creature). Resolution-only.
- `WasCastFromHand` — cast specifically from hand.
- `WasCastFromZone(zone)` — cast from a specific zone. For resolving spells it reads the spell's
  cast-origin; for a permanent already on the battlefield it falls back to the cast-origin marker
  stamped as it entered (`HAND` → `CastFromHandComponent`, `GRAVEYARD` → `CastFromGraveyardComponent`,
  `LIBRARY` → `CastFromLibraryComponent`), so it can gate an entering permanent's own replacement
  effect (Hundred-Battle Veteran: "enters with a finality counter if cast from your graveyard") or a
  *non-self* `EntersWithCounters` (`selfOnly = false`), whose condition is evaluated against the
  entering creature — Leonardo, Sewer Samurai ("creatures you cast from your graveyard enter with a
  finality counter") and Mikey & Don ("creatures you cast from the top of your library enter with an
  extra +1/+1 counter", `WasCastFromZone(Zone.LIBRARY)`).
- `WasKicked` — cast with kicker / multikicker / offspring (i.e. an `OptionalAdditionalCost` with `branchesEffect = true` whose extra cost was paid). FlashKicker payments are intentionally invisible to this condition.
- `SneakCostWasPaid` — the source was cast for its `Sneak` cost (CR 702.190 — mana + returning an unblocked attacker). Reads the durable `ChoiceSlot.SNEAK` flag on a resolved permanent, falling back to the resolution context for a non-permanent spell's own effect. Backs riders like Leonardo, Leader in Blue and The Last Ronin's Technique.
- `WaterbendWasPaid` — the spell's optional spell-level **waterbend** additional cost (`waterbendCost(..., optional = true)`, Avatar: The Last Airbender) was paid. Reads the durable `ChoiceSlot.WATERBEND_PAID` flag on a resolved permanent, falling back to the resolution context for a non-permanent spell's own effect. The waterbend analogue of `BlightWasPaid`; backs "if this spell's additional cost was paid" on Ruinous Waterbending and Spirit Water Revival.
- `NoManaSpentToCast` — "it wasn't cast or no mana was spent to cast it": the standard free-cast
  payoff clause (**Freestrider Commando**, **Satoru, the Infiltrator**). True when the *total* mana
  spent to put the source onto the battlefield was zero — it was put onto the battlefield without
  being cast (reanimation, token, "put onto the battlefield"), **or** it was cast for free / for
  `{0}` (e.g. a plotted card cast from exile). False if any mana was spent, including mana paid for
  additional costs or cost increases on an otherwise-free cast (per the Freestrider Commando ruling:
  a plotted spell taxed by Aven Interrupter had mana spent, so it does not qualify). Reads the
  source's cast-mana record (`CastRecordComponent`), which the engine only stamps when mana > 0 was
  spent, so its absence (or a zero total) is exactly "no mana was spent." This single condition
  covers the whole oracle clause; compose `All(WasCast, NoManaSpentToCast)` for the narrower
  "cast, but for free" sense that excludes uncast permanents. Pairs naturally with the conditional
  `EntersWithCounters(..., condition = Conditions.NoManaSpentToCast)`. Resolution-only.
- `NoManaSpentToCastEntered` — the batch-enters variant of `NoManaSpentToCast`: "if none of them were
  cast or no mana was spent to cast them." Evaluated at resolution over the permanents a batch-enters
  trigger captured (the `Triggers.OneOrMorePermanentsEnter` batch, exposed as the `trigger.captured`
  pipeline collection), it's true iff **every** captured permanent had no mana spent to cast it (an empty
  capture is vacuously true). Use as a resolution-time `ConditionalEffect` gate on the payoff — **Satoru,
  the Infiltrator**: `ConditionalEffect(Conditions.NoManaSpentToCastEntered, Effects.DrawCards(1))` under a
  `Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Creature.nontoken())` trigger. Resolution-only.
- `TriggeringSpellCastWithoutPayingMana` — triggering-entity counterpart of `NoManaSpentToCast`: "if no
  mana was spent to cast it" about the *triggering* spell (reads its `CastRecordComponent`). Used as a
  triggered-ability intervening-if (Boromir, Warden of the Tower: "Whenever an opponent casts a spell,
  if no mana was spent to cast it, counter that spell" → pair with `Effects.CounterTriggeringSpell()`).
- `TriggeringSpellManaSpentAtLeast(amount)` — threshold counterpart of
  `TriggeringSpellCastWithoutPayingMana`: "if at least `amount` mana was spent to cast it" about the
  *triggering* spell (sums the mana paid recorded on its `SpellOnStackComponent`). Triggered-ability
  intervening-if (Sahagin: "Whenever you cast a noncreature spell, if at least four mana was spent to
  cast it, …"). Counts actual mana paid, so an {X} spell that paid four or more qualifies.
- `BlightWasPaid(amount)` — the Blight X additional cost was paid.

### Source state

All "source matches X" conditions desugar to `Conditions.SourceMatches(filter)`, the facade over
`EntityMatches(EffectTarget.Self, filter)` — a generic predicate check against the source entity
that works in both resolution and static-ability (projection) contexts.

- `SourceMatches(filter)` — source entity matches a `GameObjectFilter`
  (`EntityMatches(EffectTarget.Self, filter)`).
- `SourceIsAttacking` — source is attacking.
- `SourceIsBlocking` — source is blocking.
- `SourceIsBlockingOrBlockedBySubtype(listOf(subtype, …))` — the source is currently in a combat
  pairing with a creature of one of the given subtypes — i.e. it is blocking, or being blocked by,
  at least one creature whose projected subtypes match any of them. "It" is resolved through the
  source: an Equipment/Aura reads its attached creature (so the condition gates a static ability
  granted to the equipped creature); a creature source uses itself. Checks both combat directions
  (`BlockingComponent` + `BlockedComponent`); partner subtypes use projected state. Works under
  static-ability projection, so a conditionally-granted keyword (e.g. first strike) is honored when
  combat damage is assigned. Used by Sting, the Glinting Dagger (LTR): "Equipped creature has first
  strike as long as it's blocking or blocked by a Goblin or Orc."
- `SourceIsTapped` — source is tapped.
- `SourceIsUntapped` — source is untapped.
- `SourceEnteredThisTurn` — source entered the battlefield this turn.
- `SourceIsSaddled` — source is saddled (CR 702.171b). Gates Mount payoffs on "while saddled" /
  "as long as it's saddled"; evaluates identically at resolution and during projection.
- `SourceAttackedThisTurn` — source was declared as an attacker at least once during the
  current turn (per-creature, derived from the controller's `PlayerAttackersThisTurnComponent`).
  Negate via `Conditions.Not(...)` for Erg Raiders-style "if it didn't attack this turn".
- `SourceAttackedThisCombat` / `SourceBlockedThisCombat` — source was declared as an attacker /
  blocker at least once during the current combat (per-creature `AttackedThisCombatComponent` /
  `BlockedThisCombatComponent`, stamped at declaration time, cleared when the combat phase ends;
  filter helpers `.attackedThisCombat()` / `.blockedThisCombat()`). Unlike the live
  `AttackingComponent`/`BlockingComponent` these survive the creature (or its blocked attacker)
  leaving combat, so they still read true at end of combat; unlike `SourceAttackedThisTurn` they
  reset between multiple combats in one turn.
- `SourceAttackedOrBlockedThisCombat` — `Any(SourceAttackedThisCombat, SourceBlockedThisCombat)`.
  Used as the intervening-if on Clockwork Avian's `EachEndOfCombat` counter-shed trigger.
- `SourceHasDealtDamage` — source has dealt damage since entering the battlefield.
- `SourceHasDealtCombatDamageToPlayer` — saboteur-style payoff gate.
- `SourceIsModified` — has counters, attached Equipment, or controller-owned Aura
  attached (CR 700.4). Kept as a dedicated condition because the controller-of-Aura
  match isn't expressible via the generic `EntityMatches` filter machinery.
- `SourceReceivedCounterThisTurn` — "if you put a counter on this creature this turn." True while the source
  carries the per-turn `ReceivedCountersThisTurnComponent` marker (stamped by the counter-placement path, cleared
  at cleanup). Distinct from `SourceHasCounter` (which checks current counters): this fires even if the counter was
  later removed, and stays false if the source merely entered with counters from a *prior* turn. Used as the
  end-step intervening-if of Secrets of Strixhaven's Fractal Tender.
- `SourceHasSubtype(subtype)` — `SourceMatches(GameObjectFilter.Any.withSubtype(...))`;
  Changeling is honored.
- `SourceHasKeyword(keyword)` — `SourceMatches(GameObjectFilter.Any.withKeyword(...))`.
- `SourceHasCounter(counterType)` — `SourceMatches(GameObjectFilter.Any` with the
  corresponding `StatePredicate.HasCounter` / `HasAnyCounter`).
- `SourceCounterCountAtLeast(counterType, count)` — the threshold form of `SourceHasCounter`: the source has `count`+
  counters of `counterType` (a `Compare` on `EntityProperty(Source, CounterCount(filter))`). This is the gate
  behind a Station card's `{N+}` symbol (CR 721.2a — "As long as this permanent has N or more charge counters on it,
  it has [abilities]"): use it as the `condition` of a `staticAbility { }` or inside
  `ActivationRestriction.OnlyIfCondition(...)`, with `Counters.CHARGE`. Generic over counter type, reads counters live.
  Takes either a counter-type name (`Counters.CHARGE`) or a `CounterTypeFilter`; pass `CounterTypeFilter.Any` for
  "N or more counters **of any kind**" gates (Warden of the Inner Sky), which sums every counter kind on the source.

### Turn / phase

- `IsYourTurn` — it's your turn.
- `IsNotYourTurn` — it's an opponent's turn.
- `IsInPhase(phase)` — currently in `BEGINNING | MAIN | COMBAT | …`.
- `IsInStep(steps, yoursOnly = true)` — current step is one of `steps` (e.g. `Step.END`). Board-derived
  (reads `state.step` + active player), so it evaluates identically at resolution and under projection,
  making it usable as a `ConditionalStaticAbility` gate. `yoursOnly` requires it to be the controller's
  turn ("during your end step"). Used by Zurgo, Thunder's Decree.
- `IsFirstEndStepOfTurn` — it's the turn's first (natural) end step, i.e. *not* an extra end step
  inserted by `Effects.AddAdditionalEndSteps`. Board-derived (reads `state.step` + the active
  player's "in an inserted end step" marker), so it evaluates identically at resolution and under
  projection. The loop guard for "there is an additional end step after this step" riders: gate the
  `Effects.AddAdditionalEndSteps` call on it so the spawned end step doesn't spawn another (Y'shtola
  Rhul).
- `IsFirstCombatPhaseOfTurn` — the combat analog of `IsFirstEndStepOfTurn`: it's the turn's first
  (natural) combat phase, i.e. *not* an extra combat phase inserted by `Effects.AddCombatPhase`.
  Board-derived (reads `state.phase == COMBAT` + the active player's "in an inserted combat phase"
  marker), so it evaluates identically at resolution and under projection. The intervening-if / loop
  guard for "after this phase, there is an additional combat phase" riders: use it as
  `triggerCondition` so the spawned combat phase doesn't spawn another (Balthier and Fran; also the
  faithful replacement for the `oncePerTurn = true` approximation on Genji Glove / Raph & Leo).
- `ControllerTurnsTakenAtMost(n)` — the controller has taken at most N turns so far
  (1-indexed once they're partway through their first turn). Reads
  `PlayerTurnsTakenComponent` set by `TurnManager.startTurn`. Used by Starting Town
  ("your first, second, or third turn of the game" → `n = 3`).

### Per-turn counts

All three are parameterised by a `Player` reference (default `Player.You`), so they
work in both resolution and static-ability (projection) contexts. The DSL helpers
default to "you" so card authors don't need to pass it explicitly.

- `YouAttackedWithCreaturesThisTurn(filter, atLeast)` — Raid/Battalion shape. Backed by
  `PlayerAttackedWithCreaturesThisTurn(Player.You, filter, atLeast)`.
- `PlayerAttackedPlayerThisTurn(attacker, defender = Player.You)` — whether `attacker` "attacked"
  `defender` this turn (CR 508.6): they declared one or more attackers whose defending player was
  `defender` (the player directly, or the controller of a planeswalker / protector of a battle the
  attacker was attacking, CR 508.5). Reads the attacker's per-turn `PlayerAttackedPlayersThisTurnComponent`
  (stamped at declare-attackers, cleared at end of turn). Negate with `Not(...)` for "didn't attack you
  that turn" (Faramir, Prince of Ithilien: at the chosen opponent's next end step, draw if they didn't
  attack you, else make three Human Soldier tokens). `attacker` is typically `Player.TriggeringPlayer`
  (the player a delayed trigger fired on via `CreateDelayedTriggerEffect.fireOnPlayer`).
- `YouCastSpellsThisTurn(atLeast, filter, fromZone?)` — Prowess/Magecraft shape. Backed by
  `PlayerCastSpellsThisTurn(Player.You, filter, atLeast, fromZone)`. `fromZone` (default any) restricts
  the count to spells cast from that zone, matched independently of `filter` (a face-down/morph spell
  cast from hand still counts, CR 708.2). With `fromZone = Zone.HAND`, negating gives the Prairie Dog
  cycle's "you haven't cast a spell from your hand this turn":
  `Not(YouCastSpellsThisTurn(1, fromZone = Zone.HAND))` (Inventive Wingsmith, Prairie Dog, Canyon Crab,
  Emergent Haunting, Wrangler of the Damned). The origin zone is captured on each `CastSpellRecord`
  (`castFromZone`) at cast time, so flashback/forage (GRAVEYARD), plot/foretell (EXILE), and commander
  (COMMAND) casts are all distinguished from hand casts.
- `YouDrewCardsThisTurn(atLeast = 1)` — "as long as you've drawn N or more cards this turn".
  Backed by `PlayerDrewCardsThisTurn(Player.You, atLeast)`, which reads the per-player
  `CardsDrawnThisTurnComponent` (reset for all players at turn start). Works in resolution and
  cost-reduction (projection) contexts. Used by Gwaihir the Windlord ("costs {2} less … as long as
  you've drawn two or more cards this turn") via `ModifySpellCost(..., gating = CostGating.OnlyIf(...))`.
- `TriggeringSpellMatches(filter)` — intervening-if guard: the spell that triggered this ability
  matches `filter`. Reads the triggering entity's static card characteristics (so it stays correct
  after the spell leaves the stack). General "whenever you cast a spell, if it's a/an X ..." gate.
  Backed by `EntityMatches(EffectTarget.TriggeringEntity, filter)`.
- `DiscardedCardMatches(filter, index = 0)` — the card discarded to pay this spell's additional
  discard cost (`Costs.additional.DiscardCards(...)`) matches `filter`. The discarded card is in its
  owner's graveyard by resolution (CR 608.2), so the filter checks the graveyard card's
  characteristics. Resolution-only; backed by `EntityMatches(EffectTarget.DiscardedAsCost(index),
  filter)`. Wrap in `Not` for "wasn't a [type]" — e.g. Grab the Prize: "if the discarded card wasn't
  a land card, ~ deals 2 damage to each opponent" = `Not(DiscardedCardMatches(GameObjectFilter.Land))`.
- `YouCastFirstSpellOfTypeThisTurn(filter)` — true when the triggering spell is the *first* spell
  matching `filter` you've cast this turn. Pure composition, no bespoke counting:
  `All(TriggeringSpellMatches(filter), Not(YouCastSpellsThisTurn(atLeast = 2, filter)))`. The
  `TriggeringSpellMatches` half is load-bearing — it stops a later non-matching cast from satisfying
  the count once one matching spell exists. Used by Alania, Divergent Storm (first instant / first
  sorcery / first Otter).
- `YouCommittedCrimeThisTurn` — Outlaws of Thunder Junction crime gate: true once you've cast a
  spell, activated an ability, or put a triggered ability on the stack this turn that targets an
  opponent, anything an opponent controls, and/or a card in an opponent's graveyard. Backed by
  `PlayerCommittedCrimeThisTurn(Player.You)`, which reads `GameState.playersWhoCommittedCrimeThisTurn`
  — a turn-scoped set populated at every `CommitCrimeEvent` emit site (crime detection is the engine's
  `CrimeDetector`) and cleared at each turn boundary. Stays true for the rest of the turn even if the
  crime-committing spell/ability is countered. Resolves identically in resolution and projection (e.g.
  cost-reduction) contexts. Pairs with `CostGating.OnlyIf(...)` for "costs {N} less if you've committed
  a crime this turn" (Seize the Secrets).
- `PermanentEnteredFaceDownThisTurn` / `YouTurnedPermanentFaceUpThisTurn` — Duskmourn (Oblivious
  Bookworm) gates: true when a permanent entered the battlefield face down under your control this turn,
  or you turned a permanent face up this turn. Backed by per-player `PermanentEnteredFaceDownThisTurnComponent`
  (stamped in `ZoneTransitionService` on any face-down battlefield entry) and `TurnedPermanentFaceUpThisTurnComponent`
  (stamped in the turn-face-up handler), both cleared at the turn boundary. Compose with `Conditions.Not` /
  `Conditions.Any` for the "unless A or B" rider.
- `YouHaveCitysBlessing` — Ascend gate. Backed by `PlayerHasCitysBlessing(Player.You)`.
- `IsFirstSpellPaidWithTreasureManaCastThisTurn` — gates a triggered ability to fire only
  on the first spell each turn that mana from a Treasure was spent to cast (Rain of
  Riches). Reads `CastSpellRecord.paidWithTreasureMana` on the per-player spell history.
- `PermanentTypeEnteredBattlefieldThisTurn(cardType, player = Player.You)` — true if a
  permanent of `cardType` entered the battlefield under `player`'s control at any point
  this turn. Pure ETB tracker: the permanent need not still be on the battlefield, still
  be of that type, or still be under the same controller — only the entry event matters
  (so Mechan Shieldmate's "as long as an artifact entered ... this turn" stays satisfied
  even if the artifact is destroyed before combat). Captured types are read from the
  *projected* state at the moment of entry, so a permanent that's an artifact via a
  continuous effect at ETB (Mycosynth Lattice, etc.) also counts. Backed by the per-player
  `PermanentTypesEnteredBattlefieldThisTurnComponent`, cleared by `CleanupPhaseManager` at
  end of turn. Every battlefield entry must go through `BattlefieldEntry.place` for this
  tracker to stay in sync. Shortcut: `Conditions.ArtifactEnteredBattlefieldThisTurn`.
- `YouDescendedThisTurn(atLeast = 1)` — CR 700.11 gate: at least `atLeast` nontoken
  permanent cards were put into your graveyard from *any* zone this turn (battlefield,
  hand, library, stack, exile). Tokens do not count, even though they briefly enter the
  graveyard before ceasing to exist; instants and sorceries do not count. The cards
  themselves need not still be in the graveyard when the gate evaluates — the count is a
  pure event tracker. Composes through `Compare(DynamicAmount.TurnTracking(Player.You,
  TurnTracker.DESCENDED), GTE, Fixed(atLeast))`, so the same plumbing supports the bare
  descend gate (`atLeast = 1`, Ruin-Lurker Bat: "At the beginning of your end step, if
  you descended this turn, scry 1") and the descend N / fathomless descent ability words
  (`atLeast = 4`, `atLeast = 8`). Backed by the per-player
  `PlayerDescendedThisTurnComponent`, incremented in `ZoneTransitionService` whenever a
  permanent (nontoken) card lands in a player's graveyard, and cleared by
  `CleanupPhaseManager` at end of turn.
- `YouSacrificedPermanentsThisTurn(atLeast = 1)` — at least `atLeast` permanents (any type) were
  sacrificed by you this turn. Composes through `Compare(DynamicAmount.TurnTracking(Player.You,
  TurnTracker.PERMANENTS_SACRIFICED), GTE, Fixed(atLeast))`. Backed by the per-player
  `PermanentsSacrificedThisTurnComponent` (controller-scoped; distinct from the game-wide
  `GameState.permanentsSacrificedThisTurn` cost-reduction counter), incremented in
  `ZoneTransitionService.trackPermanentSacrifice` and cleared by `CleanupPhaseManager`. Pair with the
  `DynamicAmounts.permanentsSacrificedThisTurn()` amount for "that much" damage (Sawblade Skinripper:
  "At the beginning of your end step, if you sacrificed one or more permanents this turn, this creature
  deals that much damage to any target").
- `GraveyardContains(filter)` — "there is at least one card matching `filter` in your graveyard"
  (`Exists(Player.You, Zone.GRAVEYARD, filter)`). Compose with `Conditions.All`/`Any` for multi-type
  checks, e.g. `All(GraveyardContains(Filters.Instant), GraveyardContains(Filters.Sorcery))` =
  "an instant card and a sorcery card in your graveyard" (Flow State). `GraveyardContainsSubtype(subtype)`
  is the subtype-filtered sibling.
- `CardsInGraveyardMatchingAtLeast(count, filter)` — "there are `count` or more cards matching `filter`
  in your graveyard" (`Compare(Count(Player.You, Zone.GRAVEYARD, filter), GTE, count)`). The general
  form behind `CreatureCardsInGraveyardAtLeast(count)`; use for "N or more <kind> cards", e.g. Ran and
  Shaw's "three or more Dragon and/or Lesson cards" with
  `GameObjectFilter.Any.withAnySubtype("Dragon", "Lesson")` (a card matching multiple ways is counted
  once). `CardsInGraveyardAtLeast(count)` is the unfiltered total.
- `Delirium(count = 4)` — the Delirium ability word: "there are `count` or more card types among
  cards in your graveyard." Composes through `Compare(DynamicAmount.AggregateZone(Player.You,
  Zone.GRAVEYARD, GameObjectFilter.Any, Aggregation.DISTINCT_TYPES), GTE, Fixed(count))` — the
  distinct-card-type count over your graveyard (artifact, battle, creature, enchantment, instant,
  land, planeswalker, sorcery); a single card with several types contributes each type once. The
  printed threshold is always four, but `count` is parameterized. Works as a static "as long as …"
  gate (`staticAbility { ability = ModifyStats(...); condition = Conditions.Delirium() }` —
  Spineseeker Centipede) and as an activated-ability `ActivationRestriction.OnlyIfCondition`
  ("Activate only if there are four or more card types …" — Balustrade Wurm).
- `CreatureDiedThisTurn` — intervening-if "if a creature died this turn", **global** (any player's
  control; sums every player's `CreaturesDiedThisTurnComponent`).
- `ControlledCreatureDiedThisTurn` — intervening-if "if a creature died **under your control** this
  turn", scoped to the source's controller (reads only that player's `CreaturesDiedThisTurnComponent`).
  Used by Barrensteppe Siege (Mardu). Dual-mode.
- `SubtypeCreatureDiedThisTurn(subtype)` / `NonSubtypeCreatureDiedThisTurn(subtype)` — **global**,
  subtype-filtered death gates. Backed by `CreatureSubtypesDiedThisTurnComponent`, which records one
  entry per death holding the dying creature's **last-known subtypes** (captured from projected state
  at the moment it left the battlefield, CR 603.10), so a creature whose types change after death is
  still recorded by the subtypes it had as it died. `SubtypeCreatureDiedThisTurn(Subtype.GOBLIN)` is
  true iff some dead creature *had* Goblin; `NonSubtypeCreatureDiedThisTurn(Subtype.ZOMBIE)` is true
  iff some dead creature *lacked* Zombie (note the asymmetry — a turn in which only Zombies died does
  not satisfy the non-Zombie form; a Zombie + a Human both dying satisfies both forms). Used by Undead
  Sprinter (DSK): "if a non-Zombie creature died this turn". Dual-mode. The underlying SDK condition
  is `CreatureWithSubtypeDiedThisTurn(subtype, present)`. Cleared at end of turn by `CleanupPhaseManager`.
- `YouHadPermanentLeaveBattlefieldThisTurn` — intervening-if "if a permanent you controlled left
  the battlefield this turn". Per-player, scoped to the source's controller. Counts every permanent
  type (creatures, lands, artifacts, enchantments, planeswalkers) and includes tokens — broader
  than `ControlledCreatureDiedThisTurn`. Backed by `PermanentLeftBattlefieldThisTurnComponent`,
  incremented by `ZoneTransitionService` whenever a permanent leaves the battlefield, credited to
  its last-known controller. Used by Shortcut to Mushrooms (LTR). Dual-mode.
- `SacrificedHadSubtype(subtype)` — intervening-if "if an X was sacrificed this way". Reads
  `EffectContext.sacrificedPermanents` snapshots captured at cost/effect-time (cost-payment or
  edict-sacrifice both populate the same list). Used by Thallid Omnivore (DOM).
- `SacrificedWasLegendary` — intervening-if "if the sacrificed creature was legendary". Same
  snapshot path as `SacrificedHadSubtype`, but reads `supertypes` instead of subtypes. Used by
  Nasty End and Gríma Wormtongue (LTR).
- `YouSacrificedThisWay` — intervening-if "if you sacrificed a creature this way". Filters
  `EffectContext.sacrificedPermanents` for snapshots whose last-known controller is the source's
  controller — the gate on the personal half of a symmetric edict. Used by Rise of the Witch-king
  (LTR). The companion `CardSource.FromZone(..., excludeSacrificedThisWay = true)` drops those same
  snapshotted entities from a later gather, so "return **another** permanent card …" can't offer the
  permanent you just sacrificed to the same spell.

### Composition

- `All(c1, c2, ...)` — AND.
- `Any(c1, c2, ...)` — OR.
- `Not(c)` — negate.
- `Compare(v1, op, v2)` — numeric comparison between `DynamicAmount`s.
- `NumberMatches(amount, NumberProperty.{Prime,Even,Odd,MultipleOf(n)})` — unary numeric predicate
  over one `DynamicAmount` (primality/parity/divisibility); facades `AmountIsPrime/Even/Odd/MultipleOf`.
- `Exists(player, zone, filter)` — at least one matching object exists.

To gate a spell-cost reduction on a condition, use `CostGating.OnlyIf(condition)` on the
`ModifySpellCost` ability (see **Spell cost statics**) rather than baking the condition into the
reduction amount.

### Static-ability vs resolution-time evaluation

Every `Condition` works in both contexts: at spell/trigger resolution (full
`EffectContext` — targets, kicker, triggering entity, etc.) and during state projection
inside a `ConditionalStaticAbility` (only the source entity and projected values are
known). The engine dispatches via a `ConditionEvaluationContext.Resolution` /
`Projection` sealed type — there is **no** separate `SourceProjectionCondition` arm.

Conditions that need resolution-only facts (e.g. `TargetMatchesFilter`, `TargetSharesMostCommonColor`, `TriggeringEntity*`,
`WasKicked`, `ManaSpentToCastIncludes`, `CollectionContainsMatch`) silently evaluate to
`false` under projection — a static-ability gate is never "in the middle of casting a spell".

Other gates available in both contexts:

- `ColorIsMostCommon(color)` — board-derived, so it gates a `ConditionalStaticAbility` directly
  (the Invasion djinns rely on this).
- `SourceChosenModeIs("id")` — gate on the chosen mode (Sieges / `EntersWithChoice`). Works at both
  resolution and projection.
- `CastChoiceMade(slot)` — generic "was a value locked into this `ChoiceSlot`" guard over the durable
  cast-choices bag (mtgish's `AColorWasChosen`): `CastChoiceMade(ChoiceSlot.COLOR)`,
  `CastChoiceMade(ChoiceSlot.KICKED)`. Works at resolution and projection.
- `CastChoiceIs(slot, "value")` — the slot's value equals `value` (text compare; color compares against
  the enum name): `CastChoiceIs(ChoiceSlot.MODE, "Khans")`, `CastChoiceIs(ChoiceSlot.COLOR, "RED")`. The
  generic slot reader new cards should prefer over per-slot conditions; the §8 emitter target for
  mtgish's `TheChosenColor`/`TheChosenCreatureType` guards.
- `CapturedAtCast("flag")` — the named **"as you cast this spell"** condition capture (CR 601.2i) was
  true the moment the spell was cast. Pairs with the spell DSL `captureAtCast("flag", condition)`: the
  engine evaluates `condition` (caster as controller) as the spell finishes being cast and freezes the
  names whose condition held onto `SpellOnStackComponent.castTimeFlags`; this reads the frozen answer at
  resolution, so a later board change can't flip the branch. Distinct from the player-choice slot guards
  above — those read what the player *chose*, this reads whether a game condition *held*. Used by Steer
  Clear ("deals 4 damage instead if you controlled a Mount as you cast this spell"):

  ```kotlin
  spell {
      captureAtCast("controlledMount", Conditions.ControlCreatureOfType(Subtype("Mount")))
      val creature = target("target", TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature))
      effect = ConditionalEffect(
          condition = Conditions.CapturedAtCast("controlledMount"),
          effect = Effects.DealDamage(4, creature),
          elseEffect = Effects.DealDamage(2, creature),
      )
  }
  ```

**Cast-choice slots (`ChoiceSlot`).** The choices an object locks in *as it is cast / as it enters*
(CR 601.2b) — color, creature type, land type, mode, chosen creature, kicked-ness, blight amount,
`CHOSEN_NUMBER` — all ride one durable `CastChoicesComponent` on the stable entity (the immutable-ECS
analogue of Forge's SVar bag). `{X}` has its own dedicated reader (`DynamicAmount.CastX`); the other
slots are read generically via `DynamicAmount.CastChoice(slot)` (numeric), `CastChoiceMade(slot)` /
`CastChoiceIs(slot, value)` (conditions), or consumed directly by effects (e.g.
`Effects.CreateTokenOfChosenColorAndType`). `ChoiceSlot.CHOSEN_NUMBER` is the general-purpose,
re-settable numeric slot written by `Effects.ChooseNumberForSource` and read by `DynamicAmount.CastChoice`
— distinct from `BLIGHT_AMOUNT` (a one-time cast additional-cost X); it backs a free-standing repeatable
number choice not tied to casting (Shapeshifter's 0–7 P/T choice).

---

## 13. Dynamic amounts (`DynamicAmount.*`)

Numbers computed at resolution time.

### Math

- `Fixed(n)` — literal constant.
- `XValue` — the X chosen for the spell/ability, read from the transient resolution context. Populated
  only while the spell/ability itself is resolving — an ETB trigger or a later activated ability can't
  see it. Use `CastX` for the durable, object-scoped reading.
- `CastX` — the `{X}` this object was cast with, read off the *current object* regardless of zone, so it
  survives onto the permanent. The same X feeds a "when you cast this spell" trigger, an enters-the-
  battlefield trigger, the enters-with-counters replacement, and a later activated ability — the analogue
  of mtgish's `ValueX` / `Trigger_ValueXOfThatSpell`. Backed by a durable `CastChoicesComponent` that
  rides the spell's stable entity onto the battlefield (and `SpellOnStackComponent.xValue` while still on
  the stack); preserved as last-known information for dies/leaves triggers. A copy of a *permanent*
  (Clone) does not inherit it (CR 707.2); a copy of a *spell* on the stack does. Hydroid Krasis reads
  `CastX` for both its cast trigger ("draw half X") and its enters-with-X-counters replacement.
- `CastChoice(slot)` — the *numeric* value locked into a `ChoiceSlot` as this object was cast, read off
  the same durable `CastChoicesComponent` as `CastX` (falling back to the resolution context, so an
  instant/sorcery that never becomes a permanent still resolves it). The only numeric slot today is
  `ChoiceSlot.BLIGHT_AMOUNT` — the X declared for a `blight X` additional cost (Soul Immolation "deals X
  damage…"). Non-numeric slots (color, creature type, mode) are read by the `CastChoiceMade` /
  `CastChoiceIs` conditions or consumed directly by effects, not by `DynamicAmount`. (Replaces the old
  `ContextProperty(ADDITIONAL_COST_BLIGHT_AMOUNT)`.)
- `TotalManaSpent` — total mana paid from the pool to cast the current spell (sum of every per-color
  bucket; for X spells the X portion is included). E.g. Memory Deluge "where X is the mana spent."
- `ManaSpentOnX(color)` — the amount of `{color}` mana spent on the `{X}` portion specifically, broken
  down by color. Used by payoffs that scale with how much of a color went into X — Soul Burn ("you gain
  life equal to the amount of black mana spent on X"). Pair with `xManaRestriction` (see below) so the X
  can only be paid with the relevant colors.
- `DistinctColorsManaSpent` — the number of distinct *colors* of mana spent to cast the source spell
  (0–5), counting how many of the W/U/B/R/G payment buckets are non-zero. Colorless is not a color
  (CR 105.1) and never counts; mana spent on `{X}` and on generic costs still has its color counted.
  Backs the **Converge** ability word and the classic **Sunburst** rule. Resolves off the source
  entity's recorded payment (live `SpellOnStackComponent` while on the stack, the resolved permanent's
  `CastRecordComponent` afterward), so it reads correctly both at resolution and as the permanent enters
  (the common use: feeding `EntersWithDynamicCounters`). Facade: `DynamicAmounts.colorsOfManaSpent()`.
  A permanent put onto the battlefield without being cast spent no mana, so this is 0 for it.
- `DevotionTo(colors, player = You)` — a player's **devotion** to one or more colors (CR 700.5):
  the number of mana symbols of those colors among the mana costs of permanents the player controls.
  One color = "devotion to red"; several = devotion to that combination ("white and black"), where a
  symbol matching more than one listed color is counted once. Every colored symbol contributes —
  plain colored, both halves of a two-color hybrid ({W/U} counts for white *and* blue), monocolored
  twobrid ({2/B} is black), and Phyrexian ({B/P} is black); generic/colorless/{X} never count.
  Face-down permanents have no mana cost and contribute 0. Controller read via projected state.
  Facade: `DynamicAmounts.devotionTo(color, …)`. Used by "draw cards equal to your devotion to red"
  (Clive, Ifrit's Dominant).
- `UnlockedDoors(player = You, distinctNames = false)` — the number of unlocked doors among Rooms
  `player` controls (CR 709.5). Reads per-face door state, so a single Room with **both** doors
  unlocked counts as **two** — an entity-level `AggregateBattlefield`/`Count` cannot see this. With
  `distinctNames = true`, counts the distinct printed names among those unlocked door faces instead
  (two Rooms sharing an unlocked face name count once). Controller read via projected state.
  Facades: `DynamicAmounts.unlockedDoors(player)` and `DynamicAmounts.distinctUnlockedDoorNames(player)`;
  condition facade `Conditions.UnlockedDoorsAtLeast(count, player)`. Feeds the standard `Compare`
  machinery — Rampaging Soulrager ("+3/+0 while two or more unlocked doors"), Misty Salon's X/X token,
  Promising Stairs' "eight or more different names among unlocked doors" alt-win.
- `Add(a, b)` — `a + b`.
- `Subtract(a, b)` — `a − b`.
- `Multiply(a, b)` — `a × b`.
- `Power(base, exponent)` — `base^exponent` with a fixed integer `base` and a dynamic `exponent`; saturates at the global quantity cap. A non-positive exponent yields `1` (`x⁰ = 1`). Used for "draws 2ˣ cards" (Mathemagics: `Power(2, XValue)`).
- `Divide(a, b, roundUp?)` — division with rounding rule.
- `Min(a, b)` — minimum.
- `Max(a, b)` — maximum.
- `Absolute(a)` — `|a|`.

### Battlefield aggregation

- `AggregateBattlefield(player, filter, aggregation?, property?, counterType?)` — aggregate over
  matching permanents. `aggregation` defaults to `COUNT`; other modes: `MAX`/`MIN`/`SUM` over a
  `property` (`POWER`/`TOUGHNESS`/`MANA_VALUE`), and the distinct-set counters
  `DISTINCT_TYPES`, `DISTINCT_COLORS`, `DISTINCT_NAMES`, `DISTINCT_BASIC_LAND_SUBTYPES`
  (Domain), `DISTINCT_COUNTER_TYPES` (the number of different kinds of counters present
  across the group — same kind on several permanents counts once), and `DISTINCT_VALUES`
  (the number of *distinct values* of the configured `property` — Selvala, Eager Trailblazer's
  "the number of different powers among creatures you control" via
  `aggregation = DISTINCT_VALUES, property = POWER`; two permanents sharing a value count once).
  Builder shortcut: `DynamicAmounts.battlefield(player, filter).distinctValues(CardNumericProperty.POWER)`.
  `DISTINCT_NAMES` counts *differently named* matched permanents (two sharing a name count once) —
  "the number of differently named lands you control" (Emil, Vastlands Roamer) via
  `DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).distinctNames()`.
  `excludeSelf = true` drops the aggregate's own source/affected entity ("among *other* …"), e.g.
  Loot, the Key to Everything's "the number of card types among other nonland permanents you control"
  (`filter = GameObjectFilter.NonlandPermanent, aggregation = DISTINCT_TYPES, excludeSelf = true`).
  `DISTINCT_TYPES` counts only true **card types** (CR 205.2a: Artifact/Creature/Enchantment/…), never
  supertypes or subtypes, while still honoring projection-changed types (an animated land that became a
  Creature counts as a Creature).
  When `counterType` (a `CounterTypeFilter`) is set with `SUM`/`MAX`/`MIN`, the per-permanent value
  aggregated is the count of *that kind* of counter on it — i.e. "the total <kind> counters among
  <filter>" (Tom Bombadil's lore-counter total; reach for it via
  `Conditions.CounterKindAmongYouControlAtLeast`). `CounterTypeFilter.Any` totals every kind. Counters
  are read from base state (layer-independent).
- `AggregateZone(player, zone, filter?, aggregation?)` — count cards in a zone.
- `CountPermanentsOfType(player, subtype)` — count by creature type.
- `CountCreaturesYouControl` — shorthand for "your creatures".
- Facades: `DynamicAmounts.equipmentYouControl(player = You)` — Equipment you control (counts
  permanents whose projected subtypes include Equipment), and `equippedCreaturesYouControl(player = You)`
  — creatures with at least one Equipment attached (`GameObjectFilter.Creature.equipped()`). Used by
  Adelbert Steiner (+1/+1 per Equipment), Barret Wallace.
- Facades: `DynamicAmounts.cardsInYourGraveyard()` / `creatureCardsInYourGraveyard()`
  (graveyard counts), and `DynamicAmounts.cardsInYourHand()` — cards in your hand,
  e.g. Stingerback Terror's "-1/-1 for each card in your hand" (multiply by `-1` and feed
  both bonuses of a `GrantDynamicStatsEffect(GroupFilter.source(), …)`). Greatest power among
  creatures you control is `DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()`
  (Tumbleweed Rising's X/X token, paired with `Effects.CreateDynamicToken`).

### Player & game

- `LifeTotal(player)` — current life total.
- `UnspentMana(player)` — total unspent mana in that player's mana pool (all colours + colorless +
  restricted entries, i.e. the pool's `total`). Powers "as long as you have six or more unspent mana"
  (Ozai, the Phoenix King) via `Conditions.YouHaveUnspentManaAtLeast(n)` /
  `CompareAmounts(UnspentMana(You), GTE, Fixed(n))`.
- `HandSize(player)` — cards in hand.
- `TurnCount(player)` — turn number for that player.
- `TurnTracking(player, TurnTracker)` — value of a per-turn counter (see below).
- `SpellsCastThisTurn(player, filter?, excludeSelf?, fromZone?)` — count of spells `player` has cast
  this turn, read from the per-player cast history (`GameState.spellsCastThisTurnByPlayer`). `filter`
  matches a spell characteristic captured at cast time — type/color/mana value (face-down casts
  never match a non-empty filter); defaults to `GameObjectFilter.Any`. `excludeSelf` (default
  `false`) drops the resolving spell's *own* record, matched by its stack entity id, for "the
  number of **other** spells you've cast this turn". `fromZone` (default any) restricts to spells cast
  from that zone (`CastSpellRecord.castFromZone`), matched independently of `filter`. The triggering
  spell is already recorded and counts unless `excludeSelf`. DSL:
  `DynamicAmounts.spellsCastThisTurn(player, filter, excludeSelf, fromZone)`.
  - Thunder Salvo ("2 plus the number of other spells you've cast this turn"):
    `Add(Fixed(2), SpellsCastThisTurn(Player.You, excludeSelf = true))`.
  - Magebane Lizard ("the number of noncreature spells they've cast this turn"):
    `SpellsCastThisTurn(Player.TriggeringPlayer, GameObjectFilter.Noncreature)`.
  - `countDistinctCardTypes` (default `false`) switches the aggregation from *count of spells* to
    *count of distinct card types among them* — April O'Neil, Hacktivist ("draw a card for each card
    type among spells you've cast this turn"): `SpellsCastThisTurn(Player.You, countDistinctCardTypes
    = true)`. An artifact creature spell contributes both Artifact and Creature; types are unioned
    across the matching records (and across every player the ref resolves to).
  - Pairs with the `YouCastSpellsThisTurn` **condition** (§ conditions) — that gates a yes/no
    threshold, this yields the count.
- `CraftedMaterialsTotalPower` — total printed power of the cards exiled to craft the source
  permanent (CR 702.167c). Reads the source's `CraftedFromExiledComponent`. Used for the
  `*`-power CDA on Mastercraft Raptor (Saheeli's Lattice back face). Evaluates to 0 when the
  source has no recorded materials.
- `CraftedMaterialsTotalManaValue` — mana-value sibling of `CraftedMaterialsTotalPower`: total
  printed mana value of the crafted materials. Exact-one crafts read it as the single material's
  mana value (Jadeheart Attendant's "gain life equal to the mana value of the exiled card used to
  craft it"). 0 when not crafted.
- `CraftedMaterialsColorCount` — number of distinct printed colors (0–5) among the crafted
  materials (Sunbird Effigy's `*/*` P/T CDA). Pairs with the
  `Effects.AddOneManaOfEachCraftedMaterialColor()` mana effect (§4 mana effects) —
  `AddOneManaOfEachColorAmongEffect(colorSource = ManaColorSource.CraftedMaterials)` — for
  "for each color among the exiled cards used to craft this creature, add one mana of that
  color". 0 when not crafted.
- `CreaturesThatCrewedOrSaddledThisTurn` (facade `DynamicAmounts.creaturesThatCrewedOrSaddledThisTurn()`)
  — number of distinct creatures that crewed (CR 702.122) or saddled (CR 702.171) the source
  permanent this turn. Source-relative: reads the source's `CrewSaddleContributorsComponent` and
  returns its size. Retains contributors that have since left the battlefield, so the count
  includes creatures no longer present as the ability resolves (Luxurious Locomotive ruling) — a
  plain `Count` over `crewedOrSaddledSourceThisTurn()` can't express that. Evaluates to 0 with no
  source / no component. For *which* creatures (targeting/gathering) use the
  `CrewedOrSaddledSourceThisTurn` state predicate instead.
- `PermanentsSacrificedThisWay` (facade `DynamicAmounts.permanentsSacrificedThisWay()`) — number of
  permanents sacrificed by the current resolving effect ("this way"); reads the effect context's
  `sacrificedPermanents` snapshot list (populated when an edict resolves earlier in the same
  composite — the sibling-rider wiring from the sacrifice-snapshot work). Used by "each opponent
  sacrifices a creature … create a Food token for each creature sacrificed this way" (Voracious Fell
  Beast). Evaluates to 0 when nothing was sacrificed.
- `LargestSharedCreatureTypeCount(player = You)` — the size of the largest creature-type tribe among
  the creatures `player` controls, i.e. "the greatest number of creatures you control that have a
  creature type in common." For every creature type present, tally how many of the player's creatures
  have it, then take the max. A creature with several creature types feeds each of its tribes (a Bird
  Soldier adds to both the Bird and the Soldier tally); a Changeling — projected to all creature types
  — feeds every tribe. Reads projected creature subtypes (type-changing effects and Changeling are
  honored), restricted to actual creature types so artifact/land subtypes never inflate the count.
  Evaluates to 0 when no creature shares a type. Used by White Lotus Tile ("Add X mana of any one
  color, where X is …") — pair with `AddManaOfChoiceEffect(ManaColorSet.AnyColor, amount = …)`.

### Counters

- `CountersOnSource(type)` — counters of `type` on the source permanent.
- `LastKnownCountersOnSource(type)` — counters when source last existed (for dies-triggers).
- `CountersOnTarget(target, type)` — counters on a target permanent.
- `CountersOnContext(path, type)` — counters stored in an `EffectContext` path.

### Last-known source counters (self-exile / self-sacrifice cost)

- `LastKnownSourceCounters(CounterTypeFilter)` — the number of matching counters the *source* had the moment its
  self-exile / self-sacrifice cost wiped them (CR 112.7a / 122.2). When an activated ability's cost exiles or
  sacrifices its own source, the counters are gone by resolution, so `ActivateAbilityHandler` snapshots them into the
  resolution context at cost-payment time and this node reads them back. `CounterTypeFilter.Any` sums all counter
  types; otherwise it reads the named/typed counter. Facade: `DynamicAmounts.lastKnownSourceCounters(filter)`.
  Example — Lost Isle Calling: "{4}{U}{U}, Exile this enchantment: Draw a card for each verse counter on this
  enchantment. If it had seven or more verse counters on it, take an extra turn." Both the draw amount
  (`DrawCards(lastKnownSourceCounters(Named(Counters.VERSE)))`) and the seven-or-more gate
  (`Compare(lastKnownSourceCounters(Named(Counters.VERSE)), GTE, Fixed(7))`) read this node. Contrast
  `EntityProperty(Source, CounterCount(filter))`, which reads counters on the still-present source (zero after a
  self-exile cost).

- **Last-known source P/T (self-exile / self-sacrifice cost)** — the P/T analogue of
  `LastKnownSourceCounters`, applied automatically to `EntityProperty(EntityReference.Source, Power|Toughness)`
  (i.e. `DynamicAmounts.sourcePower()` / `sourceToughness()`). When an activated ability's cost sacrifices or
  exiles its own source, the source is off the battlefield by resolution, so `ActivateAbilityHandler` snapshots
  its projected P/T at cost-payment time (CR 112.7a / 608.2h, mirroring the counter snapshot) and
  `DynamicAmountEvaluator` reads the snapshot back when the source is no longer on the battlefield. This makes
  "{T}, Sacrifice this creature: it deals damage equal to its power" (Ghitu Fire-Eater, Cinder Shade, Blazing
  Bomb's Blow Up) read the pre-sacrifice power including counters/buffs rather than zero. No DSL change: existing
  `sourcePower()` reads simply become correct after a self-sacrifice. Live on-battlefield `Source` reads are
  unaffected (the snapshot is only consulted when the source has left).

### Station

- `StationCharge` — the number of charge counters a Station ability puts on its permanent: the power of the creature
  tapped to pay the station cost (CR 702.184a). Emitted by the `station()` builder (§11); do not hand-author. It is a
  dedicated node rather than `EntityProperty(TappedAsCost(0), Power)` so the CR 702.184c characteristic substitution
  (Tapestry Warden's `StationUsingToughness` → use toughness when toughness > power) is confined to station abilities
  and never rewrites an unrelated "tap a creature: do X equal to its power" read. Resolves with last-known information
  (CR 112.7a) if the tapped creature has left the battlefield before the station ability resolves.

### Card properties

- `TargetPower(target)` — target's current power.
- `TargetToughness(target)` — target's current toughness.
- `TargetManaValue(target)` — target's mana value.
- `DynamicAmounts.targetManaSpent(index)` — sum of all `manaSpent{Color}` buckets on
  the targeted spell's `SpellOnStackComponent` (i.e. what was actually paid, after
  cost reductions/increases). Pair with `targetManaValue()` for "if the amount of
  mana spent to cast that spell was less than its mana value" gates (Unravel).
  Desugars to `EntityProperty(EntityReference.Target(index), EntityNumericProperty.ManaSpent)`.
  Returns 0 if the target isn't a spell on the stack.
- `DynamicAmounts.targetColorCount(index)` / `DynamicAmounts.colorCountOf(entity)` — number of
  distinct colors of the indexed cast-time target / any `EntityReference`. Desugars to
  `EntityProperty(entity, EntityNumericProperty.ColorCount)`. Read from projected state for
  battlefield permanents (honors layer-5 color-changing — a creature turned colorless counts 0).
  Powers "for each color of [it]" amounts, e.g. Dragonfire Blade's equip cost reduction.
- `EntityProperty(entity, EntityNumericProperty.ExcessMarkedDamage)` — the excess damage (CR 120.4a)
  marked on a creature: `max(0, marked − toughness)`, read from post-damage state. Amount-valued twin of
  the `TargetMarkedDamageExceedsToughness` condition. Read it AFTER a deal-damage step in the same
  composite/pipeline resolution so the marked damage in scope is the damage that step just dealt — e.g.
  Hell to Pay: "deals X damage to target creature. Create a number of tapped Treasure tokens equal to the
  amount of excess damage dealt to that creature this way." (`EntityProperty(EntityReference.Target(0),
  ExcessMarkedDamage)`). CompositeEffect resolves sub-effects sequentially with no interleaved SBA pass, so
  the creature is still present mid-composite with its just-marked damage. Returns 0 off the battlefield or
  for a non-creature.
- `CardNumericProperty(card, property)` — generic numeric property accessor.

### Triggering-entity shortcuts (`DynamicAmounts.*` facades)

For triggered abilities whose effect reads a property of the entity that caused the trigger
(rather than the source of the ability):

- `DynamicAmounts.triggeringPower()` — power of the triggering entity (e.g. Warstorm Surge:
  "it deals damage equal to its power").
- `DynamicAmounts.triggeringToughness()` — toughness of the triggering entity.
- `DynamicAmounts.triggeringManaValue()` — mana value of the triggering entity.

All three desugar to `EntityProperty(EntityReference.Triggering, …)`.

### Attached-creature shortcut (`DynamicAmounts.*` facade)

For Aura/Equipment abilities that read a property of the creature the source is attached to (rather
than the source permanent itself — for an Aura, `EntityReference.Source` is the Aura, not the creature):

- `DynamicAmounts.enchantedCreaturePower()` — power of the attached creature (e.g. Pain for All:
  "enchanted creature deals damage equal to its power"). Desugars to
  `EntityProperty(EntityReference.EnchantedCreature, EntityNumericProperty.Power)`. The
  `EnchantedCreature` reference resolves through the source's `AttachedToComponent` (state-aware), so it
  needs an effect context with a `sourceId`; it returns 0 in predicate/filter-only contexts that don't
  thread state. When read in a **triggered ability** and the attached creature has already left the
  battlefield by resolution (e.g. removed in response to the aura's ETB trigger), it falls back to the
  creature's last-known power — captured when the trigger fired — per CR 608.2g, rather than 0.

### Ring-bearer's power (`EntityReference.RingBearer`)

- `EntityProperty(EntityReference.RingBearer(player = Player.You), EntityNumericProperty.Power)` —
  the power of the referenced player's designated **Ring-bearer** (CR 701.54: the creature carrying
  `RingBearerComponent` and controlled by its designating owner), or 0 when that player has no
  Ring-bearer. The reference always reads the *referenced* player's bearer independent of any later
  player-context rebinding — so inside a `ForEachPlayerEffect` / mill-each-player, "each player mills
  cards equal to **your** Ring-bearer's power" (One Ring to Rule Them All) measures the spell
  controller's Ring-bearer for every player. `player` defaults to `Player.You` ("your Ring-bearer");
  opponent variants resolve through the effect context's `opponentId`.

### Attachment-count shortcuts (`DynamicAmounts.*` facades)

For "X = the number of [things] attached to this permanent":

- `DynamicAmounts.attachmentsOnSelf()` — every Aura/Equipment/Fortification attached to the source
  (Champion of the Flame, Valduk). Desugars to `EntityProperty(Source, AttachmentCount())`
  (`AttachmentKind.ANY`).
- `DynamicAmounts.equipmentAttachedToSelf()` — only the Equipment attached to the source (Shagrat,
  Loot Bearer: "amass Orcs X, where X is the number of Equipment attached to Shagrat"). Desugars to
  `EntityProperty(Source, AttachmentCount(AttachmentKind.EQUIPMENT))`.

`AttachmentCount(kind)` takes an `AttachmentKind` (`ANY` / `EQUIPMENT` / `AURA`); the evaluator
counts the source's `attachedIds` whose card type matches the kind.

### Just-amassed Army (`EntityReference.AmassedArmy`)

For composite "Amass [subtype] N. Then [effect using the amassed Army's …]" shapes — Foray of
Orcs, Surrounded by Orcs, Grishnákh Brash Instigator. Compose `Effects.Amass(...)` with a
sibling effect that reads `DynamicAmount.EntityProperty(EntityReference.AmassedArmy, …)`:

- `EntityReference.AmassedArmy` — the Army that received the +1/+1 counters from the most
  recent Amass step in the current resolution pipeline (CR 701.47). Written by `AmassExecutor`
  into `EffectContext.pipeline.storedCollections[AmassedArmy.STORAGE_KEY]` after Amass
  resolves; the slot survives the multi-Army choice continuation, so a follow-up sibling
  reads the chosen Army even when Amass paused for a decision.
- Pair with `EntityNumericProperty.{Power,Toughness}` for "deals damage equal to the amassed
  Army's power" (Foray of Orcs) or "mills X cards, where X is the amassed Army's power"
  (Surrounded by Orcs).
- It also resolves inside **target / affected-entity filters** via the pipeline-threaded
  predicate path (below), so comparison-based targeting like Grishnákh's "with power ≤ the
  amassed Army's power" works — `TargetFilter.…powerAtMostEntity(EntityReference.AmassedArmy)`.

#### Pipeline values inside target filters (`powerAtMostEntity`/`powerLessThanEntity` + `AmassedArmy`)

A target filter can compare each candidate against a **resolution-time pipeline value** — the
Army just amassed by a sibling/action effect, or any cost-chosen entity. The plumbing:

- `PredicateContext` carries `storedCollections` (the pipeline's `storedCollections`, threaded
  by `PredicateContext.fromEffectContext`). `PredicateEvaluator.resolveEntityReference` resolves
  `EntityReference.AmassedArmy` / `FromCostStorage` from it (mirroring
  `TargetResolutionUtils.resolveEntityReference`), instead of returning null.
- `TargetFinder.findLegalTargets(..., pipelineContext = …)` accepts the resolving effect's
  `PredicateContext` and folds it into the per-candidate context, so **target enumeration** sees
  the pipeline. `ReflexiveTriggerEffectExecutor` passes it for deferred ("when you do, … target …")
  triggers, which is how Grishnákh filters its steal target.
- Pair with `.powerAtMostEntity(ref)` / `.powerLessThanEntity(ref)` / `.powerGreaterThanEntity(ref)`
  on the `TargetFilter`. With `ref = EntityReference.AmassedArmy`, after "amass Orcs 2" the legal
  targets exclude any creature with power > 2. This same plumbing unblocks Ent-Draught Basin's
  "target creature with power X"-style references that need a pipeline-known bound.

### Context-plumbed

- `ContextProperty(key)` — value plumbed via `EffectContext`. Keys include:
  - `TRIGGER_DAMAGE_AMOUNT` — damage in the current trigger payload (Tephraderm).
  - `TRIGGER_LIFE_GAINED` / `TRIGGER_LIFE_LOST` — life delta from a `LifeChangedEvent`.
  - `TRIGGER_COUNTERS_PLACED_AMOUNT` — counters placed in the triggering event (Simic Ascendancy).
  - `LAST_KNOWN_PLUS_ONE_COUNTER_COUNT` / `LAST_KNOWN_TOTAL_COUNTER_COUNT` — counters on the
    source as it last existed on the battlefield (Hooded Hydra / Shadow Urchin).
  - `ADDITIONAL_COST_EXILED_COUNT` — cost-step accumulator. (The blight-X amount moved to
    `DynamicAmount.CastChoice(ChoiceSlot.BLIGHT_AMOUNT)`.)
  - `TARGET_COUNT` — still-legal targets in the current effect context.
  - `LINKED_EXILE_CARD_COUNT` / `LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT` — cards / distinct
    types in the source's linked exile pile (Veteran Survivor / Keen-Eyed Curator).
  - `MODES_CHOSEN_ON_TRIGGERING_SPELL` — number of mode picks recorded on the cast that fired
    the trigger (Riku of Many Paths). Counts selections, not distinct modes, so Spree with
    the same mode twice reads as `2`.
  - `MANA_SPENT_ON_TRIGGERING_SPELL` — total mana spent to cast the spell that fired the
    trigger (Aberrant Manawurm's "+X/+0 ... where X is the amount of mana spent to cast that
    spell", Expressive Firedancer's "if five or more mana was spent"). Distinct from
    `DynamicAmount.TotalManaSpent`, which reads the *current resolving object's own* cast — this
    reads the **triggering** spell's cast (the payoff lives on a separate permanent). Populated
    from `SpellCastEvent.totalManaSpent`; `0` for non-cast triggers.
  - `COLORS_SPENT_ON_TRIGGERING_SPELL` — number of distinct *colors* of mana spent to cast the
    spell that fired the trigger (0–5; colorless is not a color, CR 105.1). The triggering-spell
    analogue of `DistinctColorsManaSpent` (which reads the resolving object's own cast, i.e.
    Converge): this reads the **triggering** spell's payment for a payoff on a separate permanent
    (Magmablood Archaic's "creatures you control get +1/+0 ... for each color of mana spent to cast
    that spell"). Populated from `SpellCastEvent.distinctColorsSpent`; `0` for non-cast triggers.
    Facade: `DynamicAmounts.colorsSpentOnTriggeringSpell()`.
  - `TRIGGERING_SPELL_MANA_VALUE` — mana value (CR 202.3) of the spell that fired the trigger
    (Kellan, the Kid — "a permanent spell with equal or lesser mana value"). Distinct from
    `MANA_SPENT_ON_TRIGGERING_SPELL` (mana actually paid): this is the spell's printed mana
    value, unaffected by cost reductions / alternative costs / X. Populated from
    `SpellCastEvent.manaValue`; `0` for non-cast triggers. Pair with
    `CollectionFilter.ManaValueAtMost(ContextProperty(TRIGGERING_SPELL_MANA_VALUE))` to bound a
    gathered collection by the triggering spell's mana value.
  - `X_VALUE_OF_TRIGGERING_SPELL` — value chosen for `{X}` on the spell that fired the trigger
    (CR 601.2b) — Geometer's Arthropod's "look at the top X cards of your library." Distinct from
    `MANA_SPENT_ON_TRIGGERING_SPELL` (total mana paid) and `TRIGGERING_SPELL_MANA_VALUE` (printed
    mana value, where {X} counts as 0). Populated from `SpellCastEvent.xValue`; `0` for non-cast /
    no-{X} triggers. Pair with `SpellCastPredicate.HasXInCost`. Facade:
    `DynamicAmounts.xValueOfTriggeringSpell()`.
  - `TRIGGER_SCRY_COUNT` — cards looked at by the scry **or surveil** that fired the trigger
    (Celeborn the Wise, Elrond Master of Healing). Equals the scry/surveil N parameter unless the
    library held fewer cards.
  - `TRIGGER_EXCESS_DAMAGE_AMOUNT` — damage past lethal in the trigger payload (CR 120.4a).
    Set from `DamageDealtEvent.excessAmount`; non-zero only for `DealsDamageEvent(requireExcess = true)`
    triggers — Fall of Cair Andros' "amass Orcs X, where X is the excess damage."
  - `TRIGGER_RECIPIENT_TOUGHNESS` — the damage recipient creature's toughness at the instant the
    triggering damage was dealt (CR 603.10 last-known information — survives a lethal hit). Set from
    `DamageDealtEvent.targetToughnessAtDamage`; `0` for non-creature recipients. Compare against
    `TRIGGER_DAMAGE_AMOUNT` for "deals damage to a creature equal to that creature's toughness"
    (Taii Wakeen, Perfect Shot).
- `AdditionalCostBlightAmount` — X paid via the Blight additional cost.
- `ChosenNumber` — number a player chose via a Choose action.
- `VariableReference(name)` — named count variable stored earlier in the same resolution (e.g. a pipeline `storeCountAs`).
- `ColorsAmongPermanents(player)` — count of distinct colors among player's permanents.
- `DistinctEntitiesInCollections(collections)` — number of *distinct* entities across the named
  pipeline collections (union, de-duplicated by entity id). Facade: `DynamicAmounts.distinctEntitiesIn(vararg)`.
  For "you affected N *different* objects" payoffs spread over several resolution-time selections —
  e.g. Call the Spirit Dragons puts a +1/+1 counter on a chosen Dragon of each color (one `SelectTarget`
  per color, each stored under its own key) and wins if five *different* Dragons received counters, so a
  multicolored Dragon chosen for two colors counts once.
- `DistinctCardTypesInCollections(collections)` — number of *distinct card types* among the cards in
  the named pipeline collections (union, de-duplicated by card type; an artifact creature counts for
  two). Facade: `DynamicAmounts.distinctCardTypesIn(vararg)`. Cards are read by entity id, so counting
  stays correct after the cards move zones (e.g. after being discarded into a graveyard). For "draw a
  card for each card type among cards discarded this way" (Kefka, Court Mage) — collection-scoped
  sibling of the `LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT` context property and of
  `SpellsCastThisTurn(countDistinctCardTypes = true)`.
- `StoredCardManaValue(collectionName)` — mana value of the **first** card in a named pipeline
  collection (Erratic Explosion-style "that card's mana value").
- `ManaValueSumOfCollection(collectionName)` — **total** mana value of *every* card in a named
  pipeline collection. Facade: `DynamicAmounts.manaValueSumOf(collectionName)`. Cards are read by
  entity id, so the value stays correct after the collection has moved zones (e.g. milled into the
  graveyard). For "you mill X cards … that player loses life equal to the total mana value of those
  cards" — Palantír of Orthanc mills into the default `"milled"` collection
  (`Patterns.Library.mill(X)`), then `Effects.LoseLife(DynamicAmounts.manaValueSumOf("milled"), opponent)`.

### `ManaColorSet`<a id="manacolorset"></a>

Color analogue of `DynamicAmount` — pure data resolved at the moment a mana effect fires.
Used by `AddManaOfChoice(colorSet, amount)`; the engine's `ManaColorSetResolver` materializes
a `Set<Color>` from the source/controller/projected state, the player picks one (or the
solver picks if there's only one), and that color is added to the pool.

- `ManaColorSet.AnyColor` — all five colors. The "any-color" default.
- `ManaColorSet.Specific(colors)` — hand-authored fixed set (e.g., `{R, G}` for a Gruul producer).
- `ManaColorSet.CommanderIdentity` — union of color identities of every commander the controller has registered. Empty (no mana produced) in non-Commander formats.
- `ManaColorSet.AmongPermanents(filter)` — colors of permanents matching `filter`, read via projected state so type/color-changing effects are honored. Mox Amber shape.
- `ManaColorSet.LandsCouldProduce(scope)` — colors any land in `scope` could produce; tapped state and activation costs are ignored (CR 106.7). `scope` is `LandControllerScope.{YOU, OPPONENTS, ANY}`. Fellwar Stone / Exotic Orchard / Reflecting Pool shape.
- `ManaColorSet.SourceChosenColor` — the single color stored on the source's `ChosenColorComponent` (set via `EntersWithChoice(ChoiceType.COLOR)`). Uncharted Haven / Ashling Rekindled shape.

### `ManaRestriction`

Spending restrictions attached to a unit of mana when it is added to the pool. Used by
`AddMana`, `AddColorlessMana`, and `AddManaOfChoice` (via the `restriction` parameter).
When the engine pays a spell's cost, restricted mana is consumed preferentially when its
restriction matches the spell context.

- `ManaRestriction.AnySpend` — no restriction; satisfies any spend. Used internally when
  `AddManaOfChoice(riders = ...)` is provided without an explicit restriction, so the rider
  set survives in the pool without limiting where the mana can be spent (Path of Ancestry).
- `ManaRestriction.InstantOrSorceryOnly` — only instants and sorceries.
- `ManaRestriction.KickedSpellsOnly` — only kicked spells.
- `ManaRestriction.CreatureSpellsOnly` / `CreatureMV4OrXCost` / `SpellsMV4OrGreater` —
  creature- or mana-value-gated.
- `ManaRestriction.LegendarySpellsOnly` — only legendary spells (matches `SpellPaymentContext.isLegendary`,
  populated from the cast card's `typeLine.isLegendary`). Great Hall of the Citadel
  (`AddManaInAnyCombination(2, restriction = LegendarySpellsOnly)`); Delighted Halfling pairs it with
  the `ManaSpellRider.MakesSpellUncounterable` rider on a one-mana any-color ability.
- `ManaRestriction.SubtypeSpellsOrAbilitiesOnly(subtype, creatureOnly?)` — Cavern of Souls /
  Unclaimed Territory: only spells of a baked subtype, optionally creature-only.
- `ManaRestriction.SubtypeSpellsOnly(subtypes)` — multi-subtype spend restriction: only spells
  whose type line carries **any** of the given subtypes (OR-joined). Spell-only (no ability
  variant). Maelstrom of the Spirit Dragon: `SubtypeSpellsOnly(setOf("Dragon", "Omen"))`
  ("a Dragon spell or an Omen spell").
- `ManaRestriction.CastFromExileOnly` — only spells cast from exile.
- `ManaRestriction.CastFromNonHandOnly` — only spells cast from anywhere other than
  hand (exile, graveyard, top of library, command zone, …). Mm'menon, the Right Hand's
  granted artifact mana ability. Generalizes `CastFromExileOnly` by allowing all non-hand
  origins instead of exile alone; rejects ability activations.
- `ManaRestriction.CardTypeSpellsOrAbilitiesOnly(cardType, allowSpells?, allowAbilities?, negated?)` —
  Steelswarm Operator shape. Use `cardType = CardType.ENCHANTMENT, allowSpells = true` for
  "spend only to cast an enchantment spell." `negated = true` flips the type test for the
  "non[type]" wordings — The Emperor of Palamecia's "Spend this mana only to cast a noncreature
  spell" is `CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE, negated = true)`; only the type
  membership is negated, the allowSpells/allowAbilities gating is unchanged.
- `ManaRestriction.AbilityActivationOnly` — only ability activations (any activated ability of
  any source). Satisfied by `SpellPaymentContext.isAbilityActivation`; unlike
  `CardTypeSpellsOrAbilitiesOnly`, the ability's source card type doesn't matter. Compose with
  `AnyOf` for "... or to activate an ability" clauses — Purple Dragon Punks:
  `AnyOf(CardTypeSpellsOrAbilitiesOnly(ARTIFACT, allowSpells = true, allowAbilities = false), AbilityActivationOnly)`
  ("spend only to cast an artifact spell or to activate an ability").
- `ManaRestriction.TurnPermanentsFaceUpOnly` — only the turn-face-up special action (disguise/
  morph face-up). Satisfied by `SpellPaymentContext.isTurnFaceUpAction`; the turn-face-up handler/
  enumerator pass that context so restricted mana in the pool is consumed. Overgrown Zealot,
  Creeping Peeper.
- `ManaRestriction.UnlockDoorOnly` — only the unlock-a-door special action (CR 709.5e).
  Satisfied by `SpellPaymentContext.isUnlockDoorAction`; the unlock-room handler/enumerator pass
  that context. Creeping Peeper (inside `AnyOf`).
- `ManaRestriction.AnyOf(restrictions)` — disjunction; the mana is spendable in any context that
  satisfies *any* listed restriction. Compose atomic restrictions for multi-option mana — e.g.
  Creeping Peeper's "cast an enchantment spell, unlock a door, or turn a permanent face up" is
  `AnyOf(CardTypeSpellsOrAbilitiesOnly(ENCHANTMENT), UnlockDoorOnly, TurnPermanentsFaceUpOnly)`.

### `ManaSpellRider`

Side-effects attached to mana that fire when the mana is spent on a spell. Orthogonal to
`ManaRestriction`: the restriction controls *where* the mana may be spent; the rider
controls *what happens to the spell* when it is spent. The cast pipeline either mutates the
spell directly (e.g. stamps a component) or queues a triggered ability onto the stack above
the spell when the rider needs the stack (typically because it requires a player decision).

- `ManaSpellRider.MakesSpellUncounterable` — Cavern of Souls: stamps `CantBeCounteredComponent`
  on the spell at cast time.
- `ManaSpellRider.ScryOnSharedTypeWithCommander(amount)` — Path of Ancestry: if the spell is
  a creature spell that shares a creature type with any of the controller's commanders,
  queues a `scry amount` triggered ability above the spell.

### `ManaExpiry`<a id="manaexpiry"></a>

The *duration* axis of mana — when it leaves the pool — orthogonal to `ManaRestriction` (where it
may be spent) and `ManaSpellRider` (what happens to the spell). Passed via the `expiry` parameter
of `AddMana`. The engine empties pools at end of turn, so:

- `ManaExpiry.END_OF_TURN` — the default; ordinary mana cleared by the end-of-turn pool emptying.
- `ManaExpiry.END_OF_COMBAT` — firebending-style mana (CR 702.189): kept through combat, discarded
  by `CombatManager.endCombat` when the combat phase ends ("Any of this mana you still have as combat
  ends will be lost"). Stored as an `AnySpend` restricted-pool entry tagged with the expiry, so it
  spends like any other mana and the tag survives partial spends.

### `TurnTracker` keys (used with `TurnTracking`)

- `CREATURES_DIED` — creatures that died this turn.
- `NONTOKEN_CREATURES_DIED` — nontoken creatures that died this turn.
- `OPPONENT_CREATURES_EXILED` — opponent creatures you exiled.
- `OPPONENTS_WHO_LOST_LIFE` — count of opponents who lost life.
- `DAMAGE_RECEIVED` — damage received by player.
- `DAMAGE_RECEIVED_FROM_ARTIFACTS` — damage dealt to the player this turn by artifact sources
  (a source that is an artifact when it deals the damage). Combat and non-combat both count;
  prevented damage does not. Powers Reverse Polarity ("twice the damage dealt to you so far this
  turn by artifacts") via `Multiply(TurnTracking(You, DAMAGE_RECEIVED_FROM_ARTIFACTS), 2)`.
- `LIFE_GAINED` — life gained this turn (Bre of Clan Stoutarm).
- `LIFE_LOST` — life lost this turn.
- `PLAYER_ATTACKED` — whether/how many times you attacked.
- `DEALT_COMBAT_DAMAGE` — combat damage dealt.
- `DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE` — indicator (0/1) that the player was dealt combat
  damage by a legendary creature this turn (recorded in `CombatDamageManager`, cleared at end of
  turn). Powers "an opponent was dealt combat damage by a legendary creature this turn" — Blitzball —
  via `Compare(TurnTracking(EachOpponent, DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE), GTE, 1)`
  (facade `Conditions.AnOpponentWasDealtCombatDamageByLegendaryCreatureThisTurn`).

For a *combat-damage-amount threshold* that is existential over players — "a player was dealt N or
more combat damage this turn" — use the dedicated condition
`Conditions.aPlayerWasDealtCombatDamageThisTurnAtLeast(n)`
(`AnyPlayerDealtCombatDamageThisTurnAtLeast`), NOT a `TurnTracking` tracker. It reads a per-player
running total (`CombatDamageReceivedThisTurnComponent`, accumulated at the two combat-damage-to-a-
player sites in `CombatDamageManager` and cleared at the turn boundary) and returns true when *some
single* player crossed the threshold. The tracker path would instead sum every player's combat
damage together (`TurnTracking(Player.Each, …)` sums), which would falsely satisfy the threshold on
the combined total. Backs Sidequest: Play Blitzball ("if a player was dealt 6 or more combat damage
this turn").
- `COUNTERS_PUT_ON_CREATURE` — counters placed.
- `LANDS_PLAYED` — lands the player explicitly played this turn (from-hand land drops only,
  derived from `LandDropsComponent`).
- `LANDS_ENTERED_UNDER_CONTROL` — lands that entered the battlefield under the player's
  control this turn. Counts *every* land ETB regardless of how it arrived (land drops,
  Lander-token search, Cultivate-style "put a land onto the battlefield" effects,
  opponent-gift effects), so it differs from `LANDS_PLAYED`. Backs
  `DynamicAmounts.landsEnteredUnderControlThisTurn(player)` — e.g. Bioengineered Future's
  "for each land that entered the battlefield under your control this turn."
- `FOOD_SACRIFICED` — Food tokens sacrificed.
- `CARDS_LEFT_GRAVEYARD` — cards leaving your graveyard.
- `DESCENDED` — number of times a player has descended this turn (CR 700.11) — i.e.
  count of nontoken permanent cards put into that player's graveyard from any zone.
  Backs `Conditions.YouDescendedThisTurn(atLeast)` and `DynamicAmounts.descendedThisTurn`
  (descend N / fathomless descent ability words).
- `CARDS_DRAWN` — number of cards a player has drawn this turn (backed by
  `CardsDrawnThisTurnComponent`, reset to 0 for every player at turn start). Powers
  characteristic-defining stats like Duelist of the Mind's "power is equal to the number of
  cards you've drawn this turn" via `dynamicPower = CharacteristicValue.dynamic(TurnTracking(You, CARDS_DRAWN))`.
- `CARDS_PUT_INTO_EXILE` — number of cards put into exile this turn, keyed on each card's owner
  (backed by `CardsPutIntoExileThisTurnComponent`, incremented at the central zone-transition for
  any non-token card entering exile from another zone, reset to 0 for every player at turn start).
  Summed across all players (via `Player.Each`) it gives the game-wide count of cards put into
  exile this turn. Powers Ennis, Debate Moderator's "if one or more cards were put into exile this
  turn" — see the `Conditions.CardsPutIntoExileThisTurn(atLeast)` wrapper.
- `PERMANENTS_SACRIFICED` — number of permanents a player has sacrificed this turn (controller-scoped,
  any permanent type). Backed by the per-player `PermanentsSacrificedThisTurnComponent`, incremented at
  the central sacrifice hook (`ZoneTransitionService.trackPermanentSacrifice`) and reset to 0 for every
  player at turn start. Distinct from the game-wide `GameState.permanentsSacrificedThisTurn` cost-reduction
  counter (which sums every player's sacrifices). Backs `Conditions.YouSacrificedPermanentsThisTurn(atLeast)`
  and `DynamicAmounts.permanentsSacrificedThisTurn(player)` — e.g. Sawblade Skinripper's "if you sacrificed
  one or more permanents this turn, ... deals that much damage".

`SubtypeEnteredUnderControlThisTurn(player, subtype, excludeTriggeringEntity?)` /
`DynamicAmounts.subtypeEnteredUnderControlThisTurn(subtype, player?, excludeTriggeringEntity?)` —
"the number of [other] [subtype]s that entered the battlefield under [player]'s control this turn"
(Geralf, the Fleshwright — "each other Zombie that entered the battlefield under your control this
turn"). It's a **turn-history** count: backed by `PermanentsEnteredUnderControlThisTurnComponent`,
which records each entrant's subtypes (from projected state) at entry, so a permanent that has since
left the battlefield or lost the type still counts. `excludeTriggeringEntity = true` drops the
permanent whose entry triggered the ability (giving "each *other*"); because every simultaneous
entrant is recorded before the triggers resolve, each one sees the others (2024-04-12 ruling).

---

## 14. Modal & choice

### Modal spells

```kotlin
spell {
    modal(chooseCount = 1) {
        mode("Destroy a creature") {
            val c = target("creature", Targets.Creature)
            effect = Effects.Destroy(c)
        }
        mode("Draw a card") {
            effect = Effects.DrawCards(1)
        }
    }
}
```

- `modal(chooseCount = N) { ... }` — N modes picked at cast time (or resolution for Commands).
- `mode(description) { ... }` — one option with its own targets/effect.
- `.requiresTarget(filter)` — mode needs a target matching filter.
- `.optional()` — mode can be skipped.
- `Mode.noTarget(...)` — explicit target-less mode (outer targets are preserved).

`ModalEffect.chooseOne { mode(...) }` and `ModalEffect.chooseN(n) { ... }` for explicit modal effects.

**Dynamic "choose up to X"** — `ModalEffect.chooseUpToDynamic(dynamicMax, *modes, allowRepeat = false)`
caps the pick count by a `DynamicAmount` evaluated at resolution time. `minChooseCount` is
forced to `0` (the player may always decline); `chooseCount` becomes `min(eval, modes.size)`.
If the evaluated cap is `0` the effect resolves as a no-op. Used by Riku of Many Paths,
where the cap is `ContextProperty(MODES_CHOSEN_ON_TRIGGERING_SPELL)`. Equivalent raw shape:
`ModalEffect(modes, chooseCount = modes.size, minChooseCount = 0, dynamicChooseCount = …)`.
The cap is any `DynamicAmount` — e.g. **Bumi, King of Three Trials** uses
`DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(Subtype.LESSON))`
("choose up to X, where X is the number of Lesson cards in your graveyard"). Modes may carry their
own per-mode targets here just as in a fixed modal — each chosen mode's target (referenced as its
mode-local `EffectTarget.ContextTarget(0)`) is only demanded when that mode is picked (Bumi's scry
mode targets a player, its Earthbend mode targets a land).

**Cast-time mode-selection UX (Spree / "choose one or more").** A choose-N modal *spell* cast
by a human is presented as a **single mode-selection panel** (web client), not a sequential
one-mode-at-a-time prompt. The enumerator emits one `CastSpellModal` legal action carrying a
`modalEnumeration` payload (each mode's description, `+ {cost}`, availability, and target
requirements); the client's cast pipeline opens the panel from that payload, lets the player
toggle the mode subset (respecting `minChooseCount`/`chooseCount`, and a count stepper when
`allowRepeat`), shows the live combined additional/total mana cost, and submits a `CastSpell`
with `chosenModes` populated but **targets deferred**. The engine then drives per-mode
on-battlefield target selection (`CastSpellHandler` pauses via the existing
`CastModalTargetSelectionContinuation`) before cost payment — so `validate()`/`execute()` accept
"modes chosen, targets deferred" as a legitimate intermediate cast state. Server-synthesized
free casts (Cascade, Sunbird's Invocation) and the AI still use the sequential server-side
mode-selection pause; choose-1 modal spells remain client-local `CastSpellMode` actions. No SDK
change is needed to author a Spree card — it is a plain `ModalEffect` with per-mode
`additionalManaCost` (see Trash the Town).

**Tiered (CR 702.183) — `spell { tiered { } }`.** *"Tiered (Choose one additional cost.)"* is a
choose-**one** modal spell where each tier carries its own additional mana cost, paid as you cast
the spell (702.183a: *"Choose one. As an additional cost to cast this spell, pay the cost associated
with that mode."*). Exactly one tier is chosen and only that tier's (usually scaled) effect resolves.
Mechanically it is Spree constrained to a single mode: a `ModalEffect` with `chooseCount = 1`,
`minChooseCount = 1`, and per-mode `additionalManaCost` — **no engine change** beyond Spree, because
the choose-1 enumerator path (`CastSpellEnumerator.computeModeEnumeration`) already folds the chosen
tier's cost into each `CastSpellMode` action's effective cost, and `CastSpellHandler` already adds it
on execute. So the cast surface is the standard choose-1 modal flow: one `CastSpellMode` legal action
per *affordable* tier (unpayable tiers aren't offered), each showing its full mana cost. The
`tiered { }` builder is authoring sugar only:

```kotlin
spell {
    tiered {
        tier("Fire", "{0}", "Fire Magic deals 1 damage to each creature.") {
            effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreatures)
        }
        tier("Fira", "{2}", "Fire Magic deals 2 damage to each creature.") {
            effect = Patterns.Group.dealDamageToAll(2, GroupFilter.AllCreatures)
        }
        tier("Firaga", "{5}", "Fire Magic deals 3 damage to each creature.") {
            effect = Patterns.Group.dealDamageToAll(3, GroupFilter.AllCreatures)
        }
    }
}
```

`tier(name, cost, text) { … }` builds one `Mode` with `additionalManaCost = cost` and a button label
of `"<name> — <text>"`; inside the block, set `effect` and (for targeted tiers) `target`, exactly
like `mode { }`. Use `"{0}"` for a free base tier. Tiered grants no behavior of its own beyond the
modal-with-additional-cost shape, so there is **no `Keyword.TIERED`** (mirroring Spree); print the
reminder via the card's `oracleText` (the `TIERED_REMINDER` constant holds the canonical string). See
Fire / Ice / Thunder / Restoration Magic, Tifa's / Vincent's Limit Break (FIN).

**Cast-time conditional choose count** — for modal *spells* the same `dynamicChooseCount`
field is evaluated against the battlefield **at cast time** (in `CastSpellHandler`,
`effectiveModalChooseCounts`), and — unlike `chooseUpToDynamic` — it keeps the `minChooseCount`
floor instead of forcing `0`. The effective upper bound is `eval.coerceIn(minChooseCount,
modes.size)`. This models "Choose one. If [condition] as you cast this spell, you may choose
two instead." (Flame of Anor): pass it via the DSL with
`modal(chooseCount = 2, minChooseCount = 1, dynamicChooseCount = …) { … }`. Combine a
`DynamicAmount.Conditional` with a control condition, e.g.

```kotlin
modal(
    chooseCount = 2,
    minChooseCount = 1,
    dynamicChooseCount = DynamicAmount.Conditional(
        condition = Conditions.YouControlAtLeast(1, GameObjectFilter.Creature.withSubtype("Wizard")),
        ifTrue = DynamicAmount.Fixed(2),
        ifFalse = DynamicAmount.Fixed(1)
    )
) { /* modes */ }
```

**"Choose one that hasn't been chosen"** — `ModalEffect.chooseOneNotYetChosen(*modes)` for a
repeatable modal *ability* whose source remembers which modes it has already chosen across the
game and never offers them again (Gandalf the Grey). Equivalent raw shape:
`ModalEffect(modes, chooseCount = 1, excludePreviouslyChosenModes = true, countsAsModalSpell =
false)`. The engine records each chosen mode index in a per-source
`ChosenModesEverComponent` and excludes it from every later presentation of the effect; once all
modes have been chosen the ability has no legal mode and resolves as a no-op. The memory is keyed
to the source object and persists while it remains the same object on the battlefield (CR 700.4) —
it resets if the permanent leaves and returns as a new object. Intended for triggered/activated
abilities on a persistent source, not one-shot modal spells.

```kotlin
triggeredAbility {
    trigger = Triggers.YouCastInstantOrSorcery
    effect = ModalEffect.chooseOneNotYetChosen(
        Mode.withTarget(/* tap or untap */, Targets.Permanent, "You may tap or untap target permanent"),
        Mode.noTarget(Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)), "…"),
        Mode.withTarget(Effects.CopyTargetSpell(), Targets.InstantOrSorcerySpellYouControl, "…"),
        Mode.noTarget(Effects.PutOnTopOfLibrary(EffectTarget.Self), "…"),
    )
}
```

### Permanent enters-with-choice (Sieges)

```kotlin
EntersWithChoice(
    ChoiceType.MODE,
    modeOptions = listOf(
        ModeOption(id = "khans", label = "Khans", description = "...", iconKey = "khans"),
        ModeOption(id = "dragons", label = "Dragons", description = "...", iconKey = "dragons"),
    ),
)
```

- Writes `ChosenModeComponent(modeId)` on the permanent.
- Downstream triggers/conditions gate via `SourceChosenModeIs("khans")`.
- Icons live in `web-client/src/assets/icons/options/`.

**Other `ChoiceType`s** — `ChoiceType.COLOR` writes `ChosenColorComponent` (read by
`GrantChosenColor`), `ChoiceType.CREATURE_TYPE` writes `ChosenCreatureTypeComponent`,
`ChoiceType.CREATURE_ON_BATTLEFIELD` writes `ChosenCreatureComponent`,
`ChoiceType.BASIC_LAND_TYPE` writes `ChosenLandTypeComponent` (read by
`SetEnchantedLandTypeFromChosen` and `GrantLandwalkOfChosenType`), and
`ChoiceType.OPPONENT` writes an entity-id choice into the `CastChoicesComponent` under
`ChoiceSlot.OPPONENT` — read back via the `Player.ChosenOpponent` reference (e.g. Jihad's
anthem + state-trigger condition: `Exists(Player.ChosenOpponent, Zone.BATTLEFIELD, …)`), and
`ChoiceType.CARD_NAME` writes a chosen **land card name** (every registered land name, presented as
a searchable option list) into the `CastChoicesComponent` under `ChoiceSlot.CARD_NAME` as a
`ChoiceValue.TextChoice` — read back via `chosenCardName()` or, for name-keyed static-ability
filters, `GameObjectFilter.namedFromChosenComponent()` (→ `CardPredicate.NameEqualsChosenComponent`,
see §7). Used by Petrified Hamlet ("When this land enters, choose a land card name", then two
statics — `PreventActivatedAbilities(nonManaAbilitiesOnly = true)` and `GrantActivatedAbility` of a
`{T}: Add {C}` mana ability — both filtered by `namedFromChosenComponent()`), and
`ChoiceType.NUMBER` (set `minValue` / `maxValue`) writes a chosen number into the
`CastChoicesComponent` under `ChoiceSlot.CHOSEN_NUMBER` as a `ChoiceValue.NumberChoice` — read back
by a CDA via `DynamicAmount.CastChoice(CHOSEN_NUMBER)`. This is the *as-enters replacement* (CR
614.1c) form of a number choice — chosen before the permanent is on the battlefield, no priority
window at the default — versus `Effects.ChooseNumberForSource` (on resolution, e.g. from an upkeep
trigger) writing the same slot. Shapeshifter uses the replacement at entry and the effect each
upkeep: `replacementEffect(EntersWithChoice(ChoiceType.NUMBER, minValue = 0, maxValue = 7))` +
`SetBasePowerToughnessDynamicStatic(power = CastChoice(CHOSEN_NUMBER), toughness = Subtract(Fixed(7),
CastChoice(CHOSEN_NUMBER)))`. Example — Phantasmal Terrain
("As this Aura enters, choose a basic land type. Enchanted land is the chosen type."):

```kotlin
auraTarget = Targets.Land
replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))
staticAbility { ability = SetEnchantedLandTypeFromChosen }
```

Traveler's Cloak grants landwalk of the chosen type to the enchanted creature instead:

```kotlin
auraTarget = Targets.Creature
replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))
staticAbility { ability = GrantLandwalkOfChosenType() }
```

### Other choice effects

- `ChooseActionEffect(choices)` — pick one effect from a list.
- `ChooseColorThenEffect(whenChosen)` — pick a color, then apply a function of the color.
- `GrantHexproofFromChosenColorEffect(target)` / `GrantProtectionFromChosenColorEffect(target)` — atoms that run inside `ChooseColorThen` and read the chosen color from context (hexproof / protection from that color). Wrap in `ForEachInGroup` for "creatures you control gain protection from the chosen color" (Akroma's Blessing).
- `Effects.ForEachColorOf(source, effect)` — the **non-interactive sibling of `ChooseColorThen`**:
  runs `effect` once per color of the entity referenced by `source`, with that color set as the
  context's chosen color, so the same per-color atoms (`GrantProtectionFromChosenColor`,
  `GrantHexproofFromChosenColor`, `GrantCantBeBlockedByChosenColor`, …) compose inside it. Source
  colors come from projected state while the source is on the battlefield (Layer-5 / Devoid honored),
  else its base `CardComponent.colors` (LKI); a colorless source runs zero times (CR 105.2). For
  "[group] gain protection from each of `source`'s colors", wrap a group iteration in it —
  `Effects.ForEachColorOf(source, ForEachInGroupEffect(group, GrantProtectionFromChosenColor(Self)))`
  — and, when `source` is the about-to-leave permanent, place it before the exile/destroy step
  (`Composite(ForEachColorOf(…), Exile(…))`) so its colors are still readable (Éowyn, Fearless Knight).
- `ChooseCreatureTypeEffect(...)` — pause for creature-type selection.
- `Effects.NoteCreatureType(storeAs = "notedType", prompt?)` — "note a creature type that hasn't been noted for this <source>" (LTR — Long List of the Ents). Same decision shape as `ChooseOption(OptionType.CREATURE_TYPE)`, but the source's *current* `NotedCreatureTypesComponent.types` are excluded from the option list (so the player can't pick a duplicate), and on resolution the chosen type is appended to that component on the source AND stored in `chosenValues[storeAs]` for any downstream pipeline step. The component lives on the source permanent's container, so it disappears when the source leaves play (CR 400.7 — a permanent that changes zones becomes a new object with no memory of its previous existence). Use this whenever a card's text says "note … for this permanent"; use plain `ChooseOption(OptionType.CREATURE_TYPE)` when the choice is one-shot and doesn't need to accumulate.
- `Effects.ChooseCardName(storeAs, prompt?, excludeBasicLandNames?)` — name a card (`ChooseOptionEffect(OptionType.CARD_NAME)`); the chosen name is stored in `chosenValues[storeAs]`. Options are every registry card name (searchable list, not free text); `excludeBasicLandNames` drops the five basics. Match cards by it with `GameObjectFilter.namedFromVariable(storeAs)`. (Desperate Research)
- `Effects.StoreCardName(from, storeAs)` — capture the name of the first card in collection `from` into `chosenValues[storeAs]`. The "choose a card, then act on cards of that name" counterpart to `ChooseCardName`. (Lobotomy)
- `SelectTargetEffect(...)` — pick from a valid target set.

---

## 15. Replacement effects

```kotlin
replacementEffect {
    condition = Conditions.YouControl(Filters.Swamp)
    effect = ReplacementEffect.PreventDamage(1)
}
```

- `ReplacementEffect.PreventDamage(amount?, restrictions?, appliesTo)` — prevent damage matching the
  `EventPattern.DamageEvent` shape. `amount = null` prevents all; a number prevents up to that much.
  `restrictions: List<Condition>` (default empty) gates the prevention on extra conditions evaluated
  against the source's controller — the same pattern as `ModifyLifeLoss.restrictions`. Use it for
  "as long as …, prevent …" statics (Spirit of Resistance: a five-distinct-colors `Compare` gate).
- `CapDamage(maxAmount, appliesTo)` — clamp matching damage to `maxAmount` (a *replacement* distinct
  from prevent/modify; applied after all amplification). Divine Presence: `CapDamage(3, DamageEvent(recipient = Any))`.
- `DoubleDamage(restrictions?, appliesTo)` — double matching damage (Gratuitous Violence, Furnace of
  Rath). `restrictions: List<Condition>` (default empty) gates the doubling on extra conditions
  evaluated against the source's controller — the same pattern as `PreventDamage.restrictions`. The
  doubling also honours `appliesTo.damageType` (`Combat` / `NonCombat` / `Any`). The Rollercrusher
  Ride: `DoubleDamage(restrictions = listOf(Conditions.Delirium(4)), appliesTo = DamageEvent(source =
  SourceFilter.Matching(GameObjectFilter.Any.youControl()), damageType = DamageType.NonCombat))` — a
  delirium-gated "double all noncombat damage from sources you control". The doubled damage stays
  attributed to the original source (the engine scales the amount in place).
- `ModifyDamageAmount(modifier = 0, dynamicModifier = null, appliesTo)` — add an amount to matching
  damage. Pass a flat `modifier` (Valley Flamecaller: "deals that much damage plus 1") or a
  `dynamicModifier: DynamicAmount?` evaluated at damage time against the replacement's **source**
  permanent (so `DynamicAmount.EntityProperty(Source, …)` / `DynamicAmounts.countersOnSelf(…)` reads
  the source's own characteristics/counters). Fated Firepower: `dynamicModifier =
  DynamicAmounts.countersOnSelf(CounterTypeFilter.Named("fire"))` with `appliesTo = DamageEvent(source =
  SourceFilter.YouControl, recipient = RecipientFilter.OpponentOrPermanentTheyControl)` — "a source you
  control deals that much damage plus the number of fire counters on this enchantment to an opponent or
  a permanent an opponent controls". Applied in `DamageUtils.applyStaticDamageAmplification` (both the
  general and combat damage paths), once per damage event, after `DoubleDamage`.
- `RedirectDamage(redirectTo, appliesTo, condition = null)` — redirect matching damage to another
  recipient. Now wired as a continuous static replacement (each source applies at most once per damage
  event). `redirectTo` supports `EffectTarget.ControllerOfDamageSource` (the controller of the damaging
  source), `Controller`/`Self` (the replacement's owner/controller), and `TargetController`. Harsh
  Judgment: redirect chosen-color instant/sorcery damage dealt to you back to the spell's controller.
  The optional `condition: Condition?` gates the redirect on the *replacement source* at the moment
  damage would be redirected (mirrors `PreventDamage.restrictions`); a `null` condition always applies.
  Martyrs of Korlis uses `Conditions.SourceIsUntapped` for "As long as this creature is untapped, all
  damage that would be dealt to you by artifacts is dealt to this creature instead" (`redirectTo =
  EffectTarget.Self`, `source = SourceFilter.Matching(GameObjectFilter.Artifact)`).
- `ReplaceDamageWithMill(appliesTo = DamageEvent(recipient = Opponent))` — replace matching damage
  (CR 615, neither dealt nor prevented): each opponent of the replacement's controller mills that many
  cards instead. The Mindskinner: `DamageEvent(recipient = RecipientFilter.Opponent, source =
  SourceFilter.Matching(GameObjectFilter.Any.youControl()))` covers both the unblockable creature's
  combat damage and noncombat damage from any source you control. Mirrors `ReplaceDamageWithCounters`;
  wired in both damage paths (`DamageUtils.applyReplaceDamageWithMill` for the general path,
  `CombatDamageManager` for combat). Damage-type filtering is not applied (matches any type).
- **DamageEvent filters (gap #7):** `EventPattern.DamageEvent(recipient, source, damageType, amount)`.
  `amount: AmountFilter` (`Any` / `AtMost(n)` / `AtLeast(n)` / `Exactly(n)`) gates on the would-be
  amount (Callous Giant: `AtMost(3)`). `source = SourceFilter.Matching(filter)` can carry relational
  predicates: `GameObjectFilter.sharingColorWithRecipient()` (`CardPredicate.SharesColorWithRecipient`,
  Well-Laid Plans — "another creature that shares a color") and `sharingChosenColorWithSource()`
  (`CardPredicate.SharesChosenColorWithSource`, reads the replacement source's `ChosenColorComponent`).
  `source = SourceFilter.YouControl` matches any source (permanent, spell, ability) controlled by the
  replacement's controller — "a source you control" (Fated Firepower) — without enumerating a
  `GameObjectFilter`. `recipient = RecipientFilter.OpponentOrPermanentTheyControl` matches an opponent
  player **or** any permanent an opponent controls — "an opponent or a permanent an opponent controls".
  `recipient = RecipientFilter.Self` / `source = SourceFilter.Self` match the permanent that owns the
  replacement — "damage dealt *to* / *by* this permanent" — for source-relative static foggers like
  Fog Bank (`DamageEvent(recipient = RecipientFilter.Self, damageType = Combat)` +
  `DamageEvent(source = SourceFilter.Self, damageType = Combat)` = "prevent all combat damage that would
  be dealt to and dealt by this creature").
- `EntersTapped(unlessCondition?, payLifeCost?)` — "this permanent enters tapped" (`unlessCondition = null`),
  or "enters tapped unless `<condition>`" when an `unlessCondition` is supplied. The "slow land" cycle
  (Deathcap Glade, Dreamroot Cascade, Sundown Pass — "enters tapped unless you control two or more other
  lands") uses `unlessCondition = Conditions.YouControlAtLeast(3, GameObjectFilter.Land)`: the
  `AggregateBattlefield` count includes the entering land itself, so "two or more *other* lands" is
  "three or more lands total". The parallel "fast land" cycle (Blooming Marsh — "two or fewer other lands")
  uses an `LTE`-direction `Compare(AggregateBattlefield(You, Land), LTE, Fixed(3))`. `payLifeCost` renders
  the "you may pay N life; if you don't, it enters tapped" variant.
- `EntersUntapped(appliesTo = ZoneChangeEvent(filter, to = Zone.BATTLEFIELD))` — the inverse of
  `EntersTapped`: "[filter] enter the battlefield untapped" (The Wandering Minstrel — "Lands you
  control enter untapped", `filter = GameObjectFilter.Land.youControl()`). Unlike `EntersTapped`,
  which is a self-replacement consumed once as the source enters, this is a *runtime* replacement
  stamped into the source's `ReplacementEffectSourceComponent` (`StaticAbilityHandler.isRuntimeReplacementEffect`)
  and consulted from the battlefield against OTHER permanents as they enter — so `appliesTo.filter`
  describes the *affected* permanents. The entry-tap paths (`PlayLandHandler`, `ZoneTransitionService`,
  `StackResolver`, and `CreateTokenExecutor` for created tokens)
  ask `EnterUntappedReplacements.entersUntapped(...)` before marking a permanent tapped and skip the
  tap when it matches. Per CR 614 ordering this collapses "would enter tapped via another replacement"
  (controller chooses untapped) and "simply put onto the battlefield tapped" (no replacement → untapped)
  to the same outcome; a shock land's "pay N life or enter tapped" prompt is elided (moot when it enters
  untapped regardless). Edge not covered: a same-event simultaneous mass-entry where the source itself is
  among the entering permanents (the source isn't yet consulted), and a land tapped via a generic
  `OnEnterRunEffect` self-tap (e.g. Game Trail) is not overridden.
- `PermanentsEnterTapped(appliesTo = ZoneChangeEvent(filter, to = Zone.BATTLEFIELD))` — the global/group
  counterpart of the self-only `EntersTapped`: "[filter] enter the battlefield tapped" (Zhao, the Moon
  Slayer — "Nonbasic lands enter tapped", `filter = GameObjectFilter.NonbasicLand`; also expresses
  Imposing Sovereign / Authority of the Consuls "creatures your opponents control enter tapped"). Like
  `EntersUntapped`, it is a *runtime* replacement stamped into the source's `ReplacementEffectSourceComponent`
  and consulted from the battlefield against OTHER permanents as they enter, so `appliesTo.filter` describes
  the *affected* permanents. The entry paths (`PlayLandHandler`, `ZoneTransitionService`, `StackResolver`,
  and `CreateTokenExecutor` for created tokens) call
  `EnterTappedReplacements.entersTapped(...)` and mark the permanent tapped — **after** consulting
  `EnterUntappedReplacements`, so per CR 614 an applicable `EntersUntapped` still wins. Created tokens are
  covered too: e.g. Dauntless Dismantler's "Artifacts your opponents control enter tapped" taps an
  opponent's Map/Treasure/Clue token, and Authority of the Consuls taps opponents' creature tokens (a token
  entering attacking keeps its tapped state and is not overridden).
- `RedirectZoneChange(newDestination, appliesTo, linkToSource = false)` — redirect a zone change to a
  different destination (Rest in Peace / Leyline of the Void: graveyard → exile). `appliesTo` is an
  `EventPattern.ZoneChangeEvent(filter, from?, to?)`; the `filter`'s `controllerPredicate` scopes it
  (e.g. `OwnedByOpponent` for Leyline). When `linkToSource = true` and `newDestination = Zone.EXILE`,
  each redirected card is added to the source permanent's `LinkedExileComponent`, so the source can
  later reference — and grant playing of — the cards it exiled. Valgavoth, Terror Eater pairs it with
  `GrantMayCastFromLinkedExile`: "If a card you didn't control would be put into an opponent's graveyard
  from anywhere, exile it instead" is `RedirectZoneChange(newDestination = Zone.EXILE, linkToSource =
  true, appliesTo = ZoneChangeEvent(to = Zone.GRAVEYARD, filter = GameObjectFilter(cardPredicates =
  listOf(CardPredicate.IsNontoken), controllerPredicate = ControllerPredicate.And(listOf(OwnedByOpponent,
  Not(ControlledByYou))))))`. The redirect (and link) is honored across every graveyard path: SBA deaths,
  mill/discard/destroy (`ZoneTransitionService`), spell resolution, counters, and fizzles (`StackResolver`).
- `RedirectZoneChangeWithEffect(newDestination, additionalEffect, selfOnly = false, linkToSource = false,
  appliesTo)` — like `RedirectZoneChange` but also runs `additionalEffect` when the replacement fires.
  The additional effect is applied through a small executor whitelist (not the full pipeline) —
  `TakeExtraTurnEffect` (Ugin's Nexus), `AddCountersEffect` on the redirected card (Darigaaz
  Reincarnated), and `GainLifeEffect` (fixed amount, gained by the replacement source's controller;
  emits `LifeChangedEvent` so life-gain triggers fire). `selfOnly = true` restricts it to the source
  permanent itself; `linkToSource = true` (exile destination only) adds the redirected card to the
  source's `LinkedExileComponent` exactly like `RedirectZoneChange.linkToSource`. The Darkness Crystal's
  "If a nontoken creature an opponent controls would die, instead exile it and you gain 2 life" is
  `RedirectZoneChangeWithEffect(newDestination = Zone.EXILE, additionalEffect = GainLifeEffect(2),
  linkToSource = true, appliesTo = ZoneChangeEvent(filter = GameObjectFilter.Creature.nontoken().opponentControls(),
  from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD))` — the linked cards are then retrieved by a
  `Creature.exiledWithSource()` target (see §7 state predicates). Honored across the same graveyard
  paths as `RedirectZoneChange`.
- `ReplacementEffect.IfYouDoBranchEffect(...)` — branch on "if you do" replacement.
- `OnEnterRunEffect(effect)` — generic "as ~ enters the battlefield, run [effect]". The wrapped effect
  executes via the normal effect-executor pipeline at entry time (so `EffectTarget.Self` resolves to
  the entering permanent) and may pause for player input. Compose with atomic pausable effects like
  `Effects.MayRevealCardFromHand` to build SOI shadow lands or other "as ~ enters" choices.
  **Scope today:** only wired into the land-play path (`PlayLandHandler`). When the first non-land
  permanent needs this, also wire it into `StackResolver.enterPermanentOnBattlefield`.
- `EntersWithCounters(counterType?, count, selfOnly?, condition?, appliesTo?)` /
  `EntersWithDynamicCounters(counterType?, count, otherOnly?, appliesTo?)` — "[permanent] enters with
  N counters." `EntersWithCounters` takes a fixed `count: Int` (Master Biomancer, Metallic Mimic);
  `EntersWithDynamicCounters` takes a `count: DynamicAmount` (Stag Beetle; the SOS Converge "Archaic"
  cycle via `convergeEntersWithCounters()` → `count = DistinctColorsManaSpent`). `appliesTo` defaults
  to "creatures you control entering the battlefield." Two scopes:
  - **Self** (default) — applies to the permanent that owns the replacement. Reserve a *dynamic* count
    for "this creature enters with a counter for each color of mana spent to cast **it**" / "for each X
    it has".
  - **`otherOnly = true`** — applies to *other* matching creatures entering (Gev, Scaled Scorch:
    "Other creatures you control enter with additional counters"; Wildgrowth Archaic: "whenever you
    cast a creature spell, that creature enters with X additional +1/+1 counters … where X is the
    number of colors of mana spent to cast **it**"). The `count` is always evaluated against the
    **entering object**, not the replacement source — so an entering-object amount like
    `DistinctColorsManaSpent` reads the new creature's own cast (and a token / reanimated creature that
    wasn't cast spent no mana → 0). Player-scoped counts (`TurnTracking(Player.You)`, Gev) still read
    the replacement source's controller.
- `EntersWithKeywords(keywords, condition?, selfOnly?, appliesTo?)` — "[permanent] enters with
  [keywords]" (CR 614.1c), the keyword counterpart of `EntersWithCounters`. The grant happens as the
  permanent enters — no trigger, no stack, no response window — as a permanent, entry-timestamped
  Layer-6 floating effect: a later "loses all abilities" removes it, it does not re-apply if stripped,
  and it is cleaned up when the permanent leaves the battlefield (new object, CR 400.7). Kicker riders
  are the canonical use (Kavu Titan "If this creature was kicked, it enters with three +1/+1 counters
  on it and with trample" = an `EntersWithCounters` **plus** an `EntersWithKeywords`, both
  `selfOnly = true, condition = WasKicked`; also Benalish Lancer / Duskwalker / Faerie Squadron /
  Pouncing Kavu). `condition` is evaluated at the moment of entry against the entering permanent
  (cast-choice conditions like `WasKicked` read the durable cast-choices bag, so a token copy or
  reanimated body — never kicked — correctly gets nothing). Like `EntersWithCounters`, a
  non-`selfOnly` instance stamped on a battlefield permanent applies to *other* permanents matching
  `appliesTo` as they enter.
- `EntersWithDevour(multiplier, sacrificeFilter, counterType, variant)` — Devour (CR 702.82) and its
  printed variants. As the permanent resolves from the stack, the controller is prompted to pick any
  number of their own permanents matching `sacrificeFilter`. Those permanents are sacrificed and the
  entering permanent gains `multiplier × count` counters of `counterType` (default `+1/+1`). Pair
  with `KeywordAbility.Devour(multiplier, sacrificeFilter, variant)` so the rules text renders. The
  `variant` parameter is a textual tag only — `""` for plain Devour, `"land"` for the EOE
  "Devour land N" wording. **Scope today:** only the stack-spell entry path is wired; reanimation and
  token entries skip Devour (which is fine for printed cards — Devour creatures all cost real mana to
  cast).
- `ModifyCounterPlacement(modifier, appliesTo)` / `DoubleCounterPlacement(placedByYou?, appliesTo)` —
  **static** counter-placement modifiers living on a battlefield permanent for as long as it remains
  (Hardened Scales `+1`, Winding Constrictor `+1`, Doubling Season doubles). `appliesTo` is an
  `EventPattern.CounterPlacementEvent(counterType, recipient)`; `recipient = CreatureYouControl` is
  resolved relative to the *source permanent's* controller. For the **activated/spell-granted,
  duration-scoped** version of this (Prairie Dog), use the effect
  `Effects.GrantCounterPlacementModifier(...)` (§4 Counters) instead — it records a controller-scoped
  modifier in a turn-scoped game-state store consulted from the same counter-placement chokepoint,
  and expires at end of turn.
- `ReplaceTokenCreationWithAttachedCopy(optional, oncePerTurn, attachmentVerb, appliesTo)` —
  "the first time you would create one or more tokens each turn, you may instead create that
  many tokens that are copies of [attached] permanent." Works for both Equipment and Auras —
  the engine reads the source's `AttachedToComponent` to find the permanent to copy.
  `optional = true` surfaces a yes/no during resolution; `oncePerTurn = true` adds
  `TokenReplacementOfferedThisTurnComponent` after the first offer (cleared at end of turn).
  `attachmentVerb` is a display-only label ("equipped", "enchanted", "fortified") — the
  attachment-type validation already happens at cast/attach time via `equipmentTarget` /
  `auraTarget`. Token copies are summoning-sick only when the copy is a creature (CR 302.6).
  Mirrormind Crown: `attachmentVerb = "equipped"`; Moonlit Meditation: `attachmentVerb = "enchanted"`.
- `CreateAdditionalToken(additionalTokenType, additionalTokenCount = 1, inheritTapped = false, appliesTo)` —
  token-creation replacement that keeps the original tokens and appends one or more predefined tokens of
  another type. `appliesTo = EventPattern.TokenCreationEvent(controller, tokenFilter)` gates the original
  creation event, and the extra tokens are added once per qualifying event, not once per token. The added
  tokens bypass the same replacement pass so a Map added for an artifact-token event does not recursively
  trigger itself. Used by Worldwalker Helm (`TokenCreationEvent(You, Artifact)`, add `Map`, `inheritTapped = true`)
  and Peregrin Took (`additionalTokenType = "Food"`, "those tokens plus an additional Food token are created instead")
  and Quina, Qu Gourmet (`additionalTokenType = "Frog"`, default `appliesTo` = any token you create, adds a 1/1 green Frog).
- `EntersAsCopy(optional, copyFilter, copyFromZone, filterByTotalManaSpent, additionalSubtypes, additionalKeywords, nameOverride, powerOverride, toughnessOverride, exileCopiedCard)` —
  "enter as a copy of …". As the permanent resolves, the controller picks an object matching
  `copyFilter` and the permanent enters as a copy (Rule 707 copiable values), with any overrides
  applied. `copyFromZone` selects the candidate pool: `Zone.BATTLEFIELD` (default — Clone, Clever
  Impersonator, Mockingbird) copies a permanent in play; `Zone.GRAVEYARD` copies a creature *card*
  from any graveyard (Superior Spider-Man) via the modal card-list overlay. `additionalSubtypes` /
  `additionalKeywords` are added "in addition to its other types"; `nameOverride` keeps a fixed name;
  `powerOverride` / `toughnessOverride` force base P/T; `exileCopiedCard` exiles the copied card after
  the copy ("When you do, exile that card"). `filterByTotalManaSpent` restricts copy targets to mana
  value ≤ total mana spent (Mockingbird). The copy snapshots a `CopyOfComponent` so it reverts to its
  printed identity when it leaves the battlefield (CR 400.7 / 707.2).
- `ModifyDrawAmount(modifier, restrictions, appliesTo)` — modify the number of cards a draw
  instruction announces by a fixed amount, optionally gated by extra `restrictions: List<Condition>`
  evaluated against the drawing player as controller. Applied **once** per draw instruction at the
  announcement site — `DrawCardsExecutor.execute` for spell/ability draws and
  `DrawPhaseManager.performDrawStep` for the draw step (CR 121.2a: "An instruction to draw multiple
  cards can be modified by replacement effects that refer to the number of cards drawn. This
  modification occurs before considering any of the individual card draws.") — so a paused-and-
  resumed per-card loop doesn't double-modify. Note that "you" in restriction text reads as the
  drawing player, not the source's controller; for `DrawEvent(player = Player.You)` they coincide,
  but `DrawEvent(player = Player.EachOpponent)` cards needing "you" = source controller would have to
  use a source-relative condition instead. Use for "if you would draw one or more cards, you draw
  that many cards plus N instead" (Quantum Riddler:
  `ModifyDrawAmount(modifier = 1, restrictions = listOf(Conditions.CardsInHandAtMost(1)), appliesTo = DrawEvent(player = Player.You))`).
- `ModifyMillAmount(modifier, restrictions, appliesTo)` — modify the number of cards a *mill* announces
  by a fixed amount (the mill twin of `ModifyDrawAmount`): a player who would mill N instead mills
  `N + modifier`, clamped to ≥ 0. `appliesTo` is an `EventPattern.MillEvent` whose `player` filter
  (`Player.You` / `Player.EachOpponent` / `Player.Each`) gates which players' mills are affected,
  relative to the source's controller. `restrictions` (a `List<Condition>`, ALL must hold, evaluated
  against the milling player as controller) gates *when* it applies. Applied **once** per mill
  instruction at the announcement site (`GatherCardsExecutor`'s `CardSource.TopOfLibrary(isMill = true)`
  branch, which only the `Patterns.Library.mill(...)` pipeline sets — scry / surveil / exile-top /
  look-at-top gathers leave `isMill = false` and are never affected), so a paused-and-resumed mill
  never double-modifies. A base mill of 0 is left untouched ("would mill one or more cards"). Multiple
  instances sum. Use for "if an opponent would mill one or more cards, they mill that many cards plus
  four instead" (The Water Crystal:
  `ModifyMillAmount(modifier = 4, appliesTo = EventPattern.MillEvent(player = Player.EachOpponent))`).
- `ModifyLifeGain(multiplier, modifier, appliesTo, restrictions)` — modify life gain by a multiplicative *and/or*
  additive factor: `gained = (original * multiplier) + modifier`, clamped to ≥ 0. `appliesTo` is a `LifeGainEvent`
  whose `player` filter (default `Player.Each`) gates which players the replacement applies to. `restrictions`
  (a `List<Condition>`, ALL must hold, evaluated against the gaining player as controller) gates *when* it applies
  — e.g. Phial of Galadriel `restrictions = listOf(Conditions.LifeAtMost(5))` ("while you have 5 or less life").
  Used by Alhammarret's Archive (`multiplier = 2`), Leyline of Hope (`multiplier = 1, modifier = 1, player =
  Player.You`). Multiple instances stack (×s multiply, +s sum) — two Leylines of Hope add 2 to every life-gain event.
- `ModifyLifeLoss(multiplier, modifier, restrictions, appliesTo)` — same shape as `ModifyLifeGain` for life loss
  events (`LifeLossEvent`), plus a `restrictions: List<Condition>` list that further gates the replacement.
- `LifeLossFloor(floor, restrictions, appliesTo)` — cap damage-induced life loss so the resulting life total
  is ≥ `floor`. `appliesTo` is a `LifeLossEvent` whose `player` filter gates who is protected (default
  `Player.Each`); `restrictions: List<Condition>` (evaluated against the source's controller) further
  gates the floor — same shape as `ModifyLifeLoss.restrictions`. **Scope:** damage-as-life-loss only
  (CR 120.3a); `LoseLifeExecutor` deliberately skips this step so pay-life costs and direct life-loss
  effects bypass the floor (matching the Ali from Cairo ruling "does not apply to effects which reduce
  your life without doing damage"). The damage event still fires at the original amount, so lifelink
  and damage-dealt triggers see the full damage. Multiple instances pick the strictest floor. Used by
  Ali from Cairo (`LifeLossFloor(floor = 1, appliesTo = LifeLossEvent(Player.You))`); Worship adds a
  `restrictions = listOf(YouControlACreature)` gate.
- `PreventLifeGain(appliesTo)` — life gain matching the event is fully prevented (Sulfuric Vortex, Erebos).
  The `LifeGainEvent.player` scope can be `You` / `EachOpponent` / `Each` (resolved relative to the
  source's controller) or `EnchantedPlayer` for an "enchant player" Aura whose locked player is its
  attachment target (Grievous Wound). For a *source-independent, rest-of-game* lock on a specific player
  instead, use the one-shot effect `Effects.LockLifeGain` (§4).
- Custom — implement the `ReplacementEffect` interface directly.

Amount-modifying replacements expose **both** `multiplier` (×) and `modifier` (±) on the same type — do not split into
`DoubleX` + `ModifyXAmount`.

---

## 16. Counters

String-keyed counter types — resolve via the central `resolveCounterType` helper rather than per-executor character
substitution.

- `+1/+1`, `-1/-1` — power/toughness counters.
- `loyalty` — planeswalker loyalty.
- `charge`, `time`, `level`, `quest`, `shield`, `fade`, `vanishing`, `experience`, `age`, `velocity`, `awakening`,
  `blood`, `cage`, `doom`, `storage`, `divinity`, `charm`, `music`, `crumble`, `corpse`, `germ`, `ink`, `growth`,
  `hour`, `energy`, `scry`, `aura`, `chapter`, `citation`, `rune`, `scar`, `crux`, `omen`, `secret`, `feather`,
  `hourglass`, `hope`, `verse`, `influence`, `burden`, `loot` — assorted printed counter kinds. (`hourglass`: Temporal Distortion
  — a permanent with one doesn't untap during its controller's untap step; model the restriction with
  `GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter(... .withCounter(Counters.HOURGLASS)))` so it stays
  projection-scoped.) (`hope` / `verse` / `influence` / `burden`: LTR — Dawn of a New Age / Lost Isle Calling /
  Palantír of Orthanc / The One Ring. `loot`: OTJ — Bandit's Haul. `wind`: ARN — Cyclone (accrued one-per-upkeep,
  scales a pay-or-sacrifice cost + damage). `nest` (`Counters.NEST`): DSK — Twitching Doll,
  whose mana ability accumulates one per activation and whose sacrifice ability reads the count to scale a token
  payoff. `page` (`Counters.PAGE`): SOS — Diary of Dreams, whose cast-an-instant-or-sorcery trigger accumulates one
  and whose `{5},{T}: draw` ability reads the count via `genericCostReduction` to cost `{1}` less per counter.
  `doom`: ATQ — Armageddon Clock (accrued one-per-upkeep, scales the damage dealt to each player in the draw step;
  a {4} ability removes one).
  Pure passive counters with no inherent rule; the cards that use them accumulate/spend them via their own
  abilities and read the count via `DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.X))` — or, when a
  self-sacrifice/exile cost wipes them first, `DynamicAmounts.lastKnownSourceCounters(...)` (CR 112.7a; see §13).
  `rev` (`Counters.REV`): DSK — Chainsaw, whose "whenever one or more creatures die" batched trigger accumulates one
  per death batch and whose `+X/+0` static reads the count via `DynamicAmounts.countersOnSelf(...)` applied to the
  equipped creature — another pure passive counter with no inherent rule.
  `possession` (`Counters.POSSESSION`): DSK — Unwilling Vessel, whose Eerie triggers (an enchantment you control
  entering / fully unlocking a Room) each accumulate one and whose dies trigger reads the total counter count via
  `DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT)` to size the X/X Spirit token it
  leaves behind — another pure passive counter with no inherent rule.)
  `fire` (`Counters.FIRE`): TLA — War Balloon (a `{1}` ability accumulates one; a `ConditionalStaticAbility`
  gated on `Conditions.SourceCounterCountAtLeast(Counters.FIRE, 3)` grants `GrantCardType("CREATURE")` so the
  Vehicle is an artifact creature at 3+); reused by later Fated/Fated-Firepower cards — another pure passive
  counter with no inherent rule.
  `conqueror` (`Counters.CONQUEROR`): TLA — Zhao, the Moon Slayer (a `{7}` ability accumulates one; a
  `ConditionalStaticAbility` gated on `Conditions.SourceCounterCountAtLeast(Counters.CONQUEROR, 1)` switches on a
  `SetLandTypesForGroup` making all nonbasic lands Mountains) — another pure passive counter with no inherent rule.
  `net` (`Counters.NET`): LCI — Braided Net (enters with three via an `EntersWithCounters` replacement; its tap
  ability spends them via `Costs.RemoveCounterFromSelf(Counters.NET, 1)`) — another pure passive counter with no
  inherent rule.
- `stun` — CR 122.1d, a built-in replacement: "If a permanent with a stun counter on it would become untapped,
  instead remove a stun counter from it." Engine-wired through `untapOrConsumeStun` (`rules-engine/core/UntapHelpers.kt`),
  which is invoked from the untap step (`BeginningPhaseManager`), from `TapUntapExecutor`'s untap branch, and from the
  sacrifice/pay continuation resumer. Adding stun counters is done by `AddCounters(Counters.STUN, n, target)`.
- **Keyword counters** (Rule 122.1b) — `flying`, `first strike`, `double strike`, `vigilance`, `lifelink`,
  `indestructible`, `deathtouch`, `trample`, `hexproof`, `reach`. `StateProjector` grants the matching `Keyword`
  to any permanent carrying one (mapped in `KEYWORD_COUNTER_MAP`, re-applied after Layer 6 so "loses all abilities"
  can't wipe a counter-granted keyword). Add via `AddCounters(Counters.DEATHTOUCH, ...)` etc.; no static ability needed.
  (`reach`: Sagu Pummeler's renew payoff puts a reach counter on a creature. `vigilance`: Aragorn, Company Leader.
  `double strike`: Mai, Jaded Edge's exhaust ability.)
- **Ability counters beyond single keywords** — `decayed` (`Counters.DECAYED`, CR 702.147a, Tarkir: Dragonstorm) grants
  the whole **Decayed** ability (a "can't block" static **and** an attack-triggered end-of-combat sacrifice) to any
  creature that bears one. `StateProjector` projects the `DECAYED` keyword + `cantBlock = true` (initial pass and the
  post-Layer-6 re-apply), and `TriggerDetector.detectDecayedCounterAttackTriggers` schedules the self-sacrifice when a
  decayed-countered creature attacks. Add via `AddCounters(Counters.DECAYED, n, target)` (Rot-Curse Rakshasa's Renew).

Counter effects live in §4 (`AddCounters`, `RemoveCounters`, `Proliferate`, `MoveAllLastKnownCounters`, etc.).

---

## 17. Zones & movement

**Zones** — `BATTLEFIELD`, `HAND`, `LIBRARY`, `GRAVEYARD`, `EXILE`, `STACK`.

**Primitives**

- `MoveToZoneEffect(target, zone, faceDown?, byDestruction?, linked?)` — single-target move. Card
  definitions construct it via the facade `Effects.Move(target, destination, …)` (or the named
  shortcuts `Effects.Destroy/Exile/ReturnToHand/PutOnTopOfLibrary/ShuffleIntoLibrary/…`).
- `MoveCollectionEffect(collectionName, zone, faceDown?, linkToSource?, asOwner?, likelyPosition?)` — pipeline move of a
  stored collection.
- `faceDown` (on both move effects) is a nullable **`FaceDownMode`** — `null` = enter face up;
  `MORPH` = face-down with the card's morph cost as its turn-up cost; `MANIFEST` = face-down with
  the card's mana cost as its turn-up cost (only if it's a creature card, CR 701.40b); `HIDDEN` =
  face down with no turn-up (e.g. exiled face down for Hideaway). The engine derives the turn-up
  data at entry, so a manifested creature reuses the whole morph turn-up machinery (special action,
  payment, flip).
- `GatherCardsEffect(source, filter, into)` — pipeline gather from a zone into a named collection. `CardSource`
  variants include zones (`FromZone`, `FromMultipleZones`), battlefield queries (`BattlefieldMatching`,
  `ControlledPermanents`), linked exile (`FromLinkedExile`), tapped-as-cost (`TappedAsCost`), and the resolved
  spell/ability targets (`ChosenTargets`). The zone/library sources (`FromZone`, `FromMultipleZones`,
  `TopOfLibrary`) accept a multi-player `player` reference (`Player.Each`, `Player.ActivePlayerFirst`,
  `Player.EachOpponent`) and fan out across every relevant player's copy of the zone in a single gather —
  e.g. "all creature cards in each player's graveyard" (Bringer of the Last Gift). Pair with
  `MoveCollectionEffect(underOwnersControl = true)` to return each card to its owner.
  `revealed = true` makes a public reveal (every player sees the cards while they stay in a hidden
  zone, persisted via `RevealedToComponent` and emitting a reveal event). For a non-public library
  *look* (`revealed = false`), `lookAudience` chooses who privately sees the cards:
  `LookAudience.Controller` (default — Scry / Surveil / look-at-top-N), `LookAudience.Opponent`
  ("an opponent looks at the top N of your library"), or `LookAudience.None` (no one is auto-shown;
  a downstream decision is the only window — used by **Sauron's Ransom**, where the opponent who
  partitions sees the cards through their own `SelectFromCollection` decision but the caster does
  not). `lookAudience` is ignored when `revealed = true` or for non-library sources. To then turn a
  single pile face up for everyone — including the caster, before a `ChoosePileEffect` — re-gather
  that pile via `GatherCards(FromVariable("pile"), revealed = true)`; any pile never revealed
  renders to the caster as opaque card backs (Sauron's Ransom's concealed face-down pile).
- `CaptureControllersEffect(from, storeAs)` — snapshot each entity's current controller into a parallel
  `List<EntityId>` under `storedCollections[storeAs]`. Required when a later step needs "who controlled
  this card before it left the battlefield" — `ControllerComponent` is stripped on move-out.
- `ForEachCapturedControllerEffect(collection, originalCollection, controllerSnapshot, countVariable?, effects)` —
  cross-references a post-move `collection` against an `originalCollection` + parallel `controllerSnapshot` to
  build per-controller tallies, then runs `effects` once per controller (turn order from the active player). Each
  iteration sets `context.controllerId` to the controller (so `Player.You` / `EffectTarget.Controller` resolve to
  them) and writes the tally into `storedNumbers[countVariable]` (default `"iterationCount"`) for
  `DynamicAmount.VariableReference` to read. Outer `storedCollections` are preserved (unlike
  `ForEachPlayerEffect`). Used by Builder's Bane via the
  `GatherCards(ChosenTargets) → CaptureControllers → MoveCollection(Destroy, storeMovedAs) → ForEachCapturedController`
  shape.
- `ForEachInCollectionEffect(collection, effect)` — run `effect` once per entity in a named pipeline collection
  (snapshotted at resolution), with `pipeline.iterationTarget` set to that entity. Lowers to
  `ForEachEffect(IterationSpace.Collection(...))` — see the unified ForEach entry under "Sequencing &
  conditional". Collection-based sibling of
  `ForEachInGroupEffect` (which iterates a battlefield filter): use it to apply a per-entity effect to a *chosen*
  set rather than a re-evaluated filter. Pair with a single-target effect on `EffectTarget.Self` — e.g.
  `ForEachInCollection(nonChosenPile, Effects.CantAttack(EffectTarget.Self))` gives each creature in a chosen pile
  its own snapshot can't-attack floating effect (Fight or Flight / Stand or Fall; creatures entering after the
  split are unaffected).
- `SelectFromCollectionEffect(from, into, selectCount?, allowZero?, alwaysPrompt?, restrictions?)` — let a player pick
  from a collection. `restrictions` (`List<SelectionRestriction>`) cap and trim the picks server-side: `OnePerCardType`,
  `OnePerColor(matchControllerPermanentColors?)`, `OnePerCardName`, `OnePerPower`, `TotalManaValueAtMost(max)` /
  `TotalManaValueAtMost(maxAmount = <DynamicAmount>)` (the dynamic overload caps the sum at a resolved amount — e.g.
  `DynamicAmount.XValue` for "with total mana value X or less"; the executor resolves it to a fixed cap up front so every
  downstream consumer sees an integer — The Rise of Sozin // Fire Lord Sozin),
  `TotalPowerAtMost(max)`, `OnePerBasicLandType`, `ReducedMinimumIfMatches(reducedMinimum, filter, requiredMatches?)`, and
  `MaxAffordablePayment(manaPerSelected, payer?)`. `TotalPowerAtMost(max)` caps the sum of selected creatures'
  **projected** power at `max` (a creature with undefined power contributes 0); it is the power analogue of
  `TotalManaValueAtMost` and surfaces `maxTotalPower` on `SelectCardsDecision` so the UI shows a running "Total power: X / N"
  and disables over-cap picks while the server trims oversubmits in response order — used for "choose any number of
  creatures you control with total power N or less, then sacrifice the rest" (Destined Confrontation). `OnePerPower` keeps at most one card of each *printed* power
  (a card with no fixed power — no printed P/T, or a characteristic-defining `*` — can't be kept and bottoms out,
  like a typeless land under `OnePerBasicLandType`); pair it with the `CreatureOrVehicle` filter for "any number of
  creature and/or Vehicle cards with different powers" (Rip, Spawn Hunter). `OnePerBasicLandType` keeps at most one
  land of each basic land type (a kept land claims
  *every* basic type it has) and — unlike `OnePerColor`, where a colourless card is unconstrained — a land with no
  basic land type can't be kept at all (Global Ruin: "chooses a land of each basic land type, then sacrifices the
  rest"). Each restriction also exposes a boolean flag on `SelectCardsDecision` (`onePerBasicLandType`, …) so the UI
  can disable redundant picks. `ReducedMinimumIfMatches` exposes `conditionalMinimums` on `SelectCardsDecision` so the
  UI and server can accept one matching card for "discard two unless you discard a creature card" while rejecting one
  nonmatching card. `MaxAffordablePayment` caps the selection at
  `floor(payer's available mana / manaPerSelected)` (floating + untapped sources) — pair it with a downstream
  `Gate.MayPay` over `PayDynamicMana` at the same rate so a player can never select a set whose total cost is
  unpayable and silently forfeit the payoff; a cap of zero (under `ChooseAnyNumber`) skips the selection prompt
  entirely (Magnetic Mountain: "choose any number … and pay {4} for each creature chosen this way").
  - `chooser` (`Chooser`, default `Controller`) — who makes the selection: `Controller`, `Opponent`, `TargetPlayer`
    (`context.targets[0]` treated as the player), `TriggeringPlayer`, `SourceController` (the source's controller,
    ignoring per-iteration swaps), `ControllerOfSelection` (the controller of the cards in `from` — resolved from the
    first card's projected controller), or `ControllerOfTarget` (the controller of the targeted *permanent*,
    `context.targets[0]`, falling back to its owner once it has left the battlefield). Use `ControllerOfSelection` for
    "their controller chooses…" where the deciding player is whoever controls the gathered cards and may be you or an
    opponent (Barrin's Spite: gather the two targeted creatures, their controller sacrifices one, the other is returned
    to hand). Use `ControllerOfTarget` for "destroy target permanent. Its controller searches/chooses…" where the
    targeted permanent's controller performs a follow-up (Magmatic Hellkite: destroy target nonbasic land, *its
    controller* searches for a basic). The same `chooser` set is accepted by `ChoosePileEffect`.

**Linked exile**

- `Effects.ExileGroupAndLink(filter, storeAs?)` — exile matching permanents linked to source.
- `Effects.ReturnLinkedExile` — return all to controller.
- `ReturnLinkedExileUnderOwnersControl` — return to owners.
- `ReturnLinkedExileToHand` — return to hand.
- `ReturnOneFromLinkedExile` — return one chosen card.
- `CardSource.FromLinkedExile()` — play permission targeting linked-exile pile.
- `CardSource.FromExile(name)` — play permission for a named exile zone.

**Face-down**

- `PutOntoBattlefieldFaceDown(count, target?)` — enter face-down (morph shape).
- `Triggers.TurnedFaceUp` — fires when source flips face-up.
- UI label: `"Turn Face-Up"` (used by E2E `selectAction("Turn Face-Up")`).

---

## 18. Components (set indirectly by effects)

### Permanent

- `ChosenModeComponent` — chosen entry mode (Sieges, modal permanents).
- `TypeLineOverrideComponent` — temporary type-line edits.
- `CountersComponent` — all counters on the permanent.
- `EnchantedCreatureComponent` — reference to attached creature (Auras).
- `EquippedCreatureComponent` — reference to equipped creature.
- `LinkedExileComponent` — linked exile pile attached to source.
- `ExileOnLeaveComponent` — replace next zone change with exile.
- `MayPlayFromExileComponent` — owner may play this from exile.
- `TappedStateComponent` — tap state.
- `FaceDownComponent` — face-down state.
- `ControllerComponent` — current controller.
- `ProtectionComponent` — protection from colors/types.
- `CantAttackComponent` / `CantBlockComponent` — combat restrictions.

### Player

- `PlayerCitysBlessingComponent` — you have City's Blessing.
- `TheRingComponent` — you have the Ring emblem; `temptCount` gates its four abilities (CR 701.54).
- `RingBearerComponent` — designates a creature as a player's Ring-bearer (on the creature, not the player).
- `SpellsCantBeCounteredComponent` — your matching spells can't be countered.
- `LifeGainedAmountThisTurnComponent` — accumulator for life gained.
- `LifeLostThisTurnComponent` — marker that you've lost life this turn.
- `PlayerAttackedThisTurnComponent` — marker that you've attacked this turn.
- `PlayerAttackersThisTurnComponent` — list of attackers declared this turn.
- `PlayerAttackedPlayersThisTurnComponent` — set of defending players you "attacked" this turn (CR
  508.6); read by `PlayerAttackedPlayerThisTurn`.
- `LandDropsComponent` — lands played this turn.
- `FoodSacrificeThisTurnComponent` — marker that you sacrificed a Food this turn.
- `SpellsCastThisTurnByPlayer` — count of spells you cast this turn.

Card authors rarely reference these directly; they are created/updated by the matching effect or trigger.

---

## 19. Named-mechanic composites

- **Cycling / Typecycling / Basic landcycling** — `KeywordAbility.Cycling(cost)`, `Typecycling(type, cost)`,
  `BasicLandcycling(cost)`; unified via `TypecyclingVariant(cost, searchFilter, description)` in `TypecycleCardHandler`.
- **Plot (CR 718)** — `KeywordAbility.plot(cost)`. Engine wires a sorcery-speed `PlotEnumerator` + `PlotCardHandler`
  that pays the plot cost, exiles the card face-up from hand, stamps `PlottedComponent(controllerId, turnPlotted)` +
  `PlayWithoutPayingCostComponent`, and adds a permanent `MayPlayPermission` gated by `SourcePlottedOnPriorTurn`.
  The cast-from-exile path is the standard `MayPlayPermission` flow in `CastFromZoneEnumerator` — `permanent = true`
  keeps the grant alive across end-of-turn cleanup. Emits `CardPlottedEvent` / `ClientEvent.CardPlotted`.
- **Adventure (CR 715)** — `layout = ADVENTURE` + `cardFaces[0]` Adventure spell; DSL:
  `card { adventure("Name") { spell { … } } }`. The primary face may be a **land** (`Land — Town`) instead of a
  creature — FIN's "Town land // spell" DFCs (Ishgard, the Holy See // Faith & Grief, …). No new layout: resolving
  the Adventure exiles the card with the same generic `MayPlayPermission`, which `CastFromZoneEnumerator` /
  `PlayLandHandler` already honor as a *play-the-land-from-exile* permission. The only seam beyond the creature case
  is `CastSpellEnumerator` — it now enumerates the Adventure spell face for a land-primary card (the land itself is
  played via `PlayLandEnumerator`). First land users: Ishgard, the Holy See; Jidoor, Aristocratic Capital; Lindblum,
  Industrial Regency; Midgar, City of Mako; Zanarkand, Ancient Metropolis.
- **Omen (Tarkir: Dragonstorm)** — `layout = OMEN` + `cardFaces[0]` Omen spell; DSL:
  `card { omen("Name") { spell { … } } }`. Reuses the Adventure cast/enumeration path (`enumerateSecondaryFace`,
  cast via `CastSpell.faceIndex = 0`), but `StackResolver` routes the resolving Omen to `Zone.LIBRARY` and shuffles
  the owner's library (`shuffleOwnerLibrary` + `LibraryShuffledEvent`) instead of exiling with a `MayPlayPermission`.
  No new effect/component — the layout enum drives the resolution fork. First user: Dirgur Island Dragon //
  Skimming Strike.
- **Modal DFC (CR 712)** — `layout = MODAL_DFC` + `cardFaces[0]` back face; DSL:
  `card { modalBack("Name") { imageUri = …; spell { selfExile(); … } } }`. Cast either face from hand (back via
  `CastSpell.faceIndex = 0`); reuses the Adventure cast/enumeration path (`enumerateSecondaryFace`) but with no
  exile-then-recast linkage at resolution. `StackResolver` reads the cast face's `selfExileOnResolve`, and the back
  art rides on `CardFace.imageUri` → `CardComponent.backFaceImageUri`. First user: Flamescroll Celebrant.
- **Prepare / Prepared (Secrets of Strixhaven)** — `layout = PREPARE` + `cardFaces[0]` prepare spell; DSL:
  `card { prepare("Name") { spell { … } } }`. The creature is only cast as itself. A creature that carries
  `Keyword.PREPARED` ("This creature enters prepared") becomes prepared on enter
  (`StackResolver.enterPermanentOnBattlefield`, gated on the keyword). A PREPARE-layout creature *without* the
  keyword (Leech Collector, Joined Researchers) only becomes prepared via `Effects.BecomePrepared(target)`
  (`BecomePreparedExecutor`). Both paths call the shared `PreparationLogic.makePrepared`, which
  creates a stack-style copy of the prepare spell in the controller's exile carrying
  `PreparedSpellCopyComponent(sourceId)`, stamps `PreparedComponent(exileCopyId)` on the creature, and grants a
  permanent `MayPlayPermission` for the copy. `CastFromZoneEnumerator` recognizes the copy and offers it as
  `CastSpell(..., faceIndex = 0)` from `EXILE` using the prepare face's cost/targets. Casting the copy
  (`StackResolver.castSpell`) strips the source's `PreparedComponent` and consumes the permission; the copy resolves
  via the face script and ceases to exist (`CopyOfComponent`). The exiled copy is exempt from the 707.10a
  phantom-copy SBA (`PhantomCardCopiesCheck`) while linked, and that same check removes it once the source leaves
  the battlefield or stops being prepared. First users: Adventurous Eater // Have a Bite, Landscape Painter //
  Vibrant Idea; becomes-prepared-via-trigger: Leech Collector // Bloodletting, Joined Researchers // Secret
  Rendezvous (end-step trigger gated on `Conditions.OpponentHasMoreCardsInHand`).
- **Hideaway N** — `KeywordAbility.hideaway(n)` (display, "Hideaway N") + `MoveCollectionEffect(faceDown = FaceDownMode.HIDDEN,
  linkToSource = true)` + `CardSource.FromLinkedExile()`; no special engine plumbing needed.
- **Ascend / City's Blessing** — `Keyword.ASCEND` + `Effects.GainCitysBlessing()` + `Conditions.YouHaveCitysBlessing` /
  `SourceProjectionCondition.ControllerHasCitysBlessing` + `PlayerCitysBlessingComponent`.
- **Siege (named-mode entry)** — `EntersWithChoice(ChoiceType.MODE, modeOptions = ...)` + `SourceChosenModeIs("id")`.
- **Morph** — `morph = "{2}{U}"` (top-level) + `morphFaceUpEffect` for "as it turns face up".
- **Warp** — `warp = "{1}{R}"`; alt-cost that exiles end of turn. Like morph and cycle, a warp card
  always surfaces *both* cast options — its normal cost and its warp cost — in the action window, even
  when only one (or neither) is payable; the unpayable side appears grayed out (CR 118.9a, the caster
  chooses which cost to use). The warp action is enumerated by `CastFromZoneEnumerator`, which also
  emits the grayed-out normal-cast placeholder when the normal cost is unaffordable (mirroring
  `MorphCastEnumerator`). The end-step exile is a delayed trigger whose `WarpExileEffect` snapshots
  the permanent's battlefield-entry timestamp (`enteredBattlefieldTimestamp`); at resolution it only
  exiles the *same object* — a warped permanent that left the battlefield and returned before the
  end step (blink, e.g. Daydream) is a new object (CR 603.7c / 400.7) and stays permanently.
- **Evoke** — `evoke = "{U}"`; pay alt cost, sacrifice on ETB.
- **Sneak** — `sneak("{1}{U}")`; declare-blockers-step alt cost (pay mana + return an unblocked attacker you control to hand); a resolving permanent enters tapped and attacking the same defender. `Conditions.SneakCostWasPaid` reads the rider flag.
- **Ninjutsu** — `ninjutsu("{1}{U}{B}")`; the canonical CR 702.49 keyword that **Sneak** reflavors. Same declare-blockers alt cost and tapped-and-attacking entry, shared via `KeywordAbility.ninjutsuStyleCost`. *Kaito, Bane of Nightmares* (DSK).
- **Earthbend** — `Effects.Earthbend(amount, target)` composes AnimateLand + GrantKeyword + AddCounters + granted
  self-triggers (no fake keyword). `amount` is an `Int` for "Earthbend N" (Earthbending Lesson) or a `DynamicAmount`
  for "Earthbend X, where X is …" (Rockalanche — X = the number of Forests you control), which counts X at resolution
  via `AddDynamicCounters`.
- **Airbend** (Avatar: The Last Airbender) — `Effects.Airbend(cost = {2})` / `Effects.AirbendAll(filter, excludeSelf, excludeChosenTargets, cost = {2})`.
  *"Airbend target permanent"* = "Exile it. While it's exiled, its owner may cast it for {2} rather than its mana
  cost." Composes a pipeline (no fake keyword): `GatherCards(ChosenTargets)` → `MoveCollection(→ EXILE, storeMovedAs)`
  → `GrantMayPlayFromExile(ownerControls = true, expiry = Permanent, fixedAlternativeManaCost = {2})`. **Target-agnostic
  by design:** the *card* declares the targeting shape via its `TargetRequirement` ("up to one", "any number of",
  "another", "you control", "target nonland permanent"), and `Effects.Airbend()` airbends whatever was chosen — so one
  effect serves every airbend card. `AirbendAll(filter)` swaps the gather to `CardSource.BattlefieldMatching` for "airbend
  all other creatures" — pass `excludeChosenTargets = true` (and `excludeSelf = false` for a sorcery) so the spared "other"
  is the spell's chosen target, backing **Avatar's Wrath** ("Choose up to one target creature, then airbend all other
  creatures."); `CardSource.BattlefieldMatching.excludeChosenTargets` drops `EffectContext.targets` from the gather, the
  chosen-target sibling of `excludeSelf`/`excludeTriggering`. The new piece is **`fixedAlternativeManaCost`** on `GrantMayPlayFromExile`: it
  stamps `PlayWithFixedAlternativeManaCostComponent(controllerId, fixedCost)` on each exiled card, which the legal-action
  enumerator (`CastFromZoneEnumerator`) and the cast handler (`CastSpellHandler`) read to *replace* the printed mana cost
  entirely (a 6-drop and a 2-drop both become {2}) — unlike `GrantPlayWithCostIncrease`, which adds on top. The component
  is stripped when the card leaves exile (`StackResolver`), so a recast Airbended permanent doesn't carry a stale cost.
- **Airbend a spell** (the stack branch — Aang, Swift Savior: "airbend up to one other target creature **or spell**").
  The single target is a cross-zone union — `TargetFilter.anyOf(TargetFilter.Creature, TargetFilter.SpellOnStack)` (the
  same union machinery as Sorceress's Schemes). Branch on whether the chosen target is a spell with
  `Conditions.TargetIsSpellOnStack(0)`: the spell branch is `Effects.AirbendSpell(cost = {2})` — airbend's reminder
  says "**exile it**", not "counter it", so it reuses the Aven Interrupter `exileSpell` primitive: it removes the spell
  from the stack to its *owner's* exile **even if the spell can't be countered**, fires **no** `SpellCounteredEvent`, and
  grants the **owner** the same fixed-{2} may-play (reusing `PlayWithFixedAlternativeManaCostComponent`). The permanent
  branch is the normal `Effects.Airbend()`. Both branches fire the "whenever you airbend" trigger below once an object is
  actually exiled (CR 701.65b). (`Effects.AirbendSpell` is `Effects.ExileTargetSpell` with `emitAirbend = true`; use the
  plain `ExileTargetSpell` — no bend — for a non-airbend exile like Aven Interrupter.)
- **"Whenever you waterbend, earthbend, firebend, or airbend" (the four-bend event) + "all four this turn"** —
  `Triggers.YouBend(types = BendType.ALL)` fires once per bend of any element in `types` the controller performs
  (Avatar Aang uses all four; pass a subset like `setOf(BendType.EARTH)` for a single-element variant). Backed by a
  `BendPerformedEvent(playerId, bendType)` emitted at each of the four keyword actions, per CR 701.65b / 701.66b /
  701.67c / 702.189b:
  - **earthbend** and **airbend** compose `Effects.EmitBend(BendType.EARTH/AIR)` into their pipelines
    (`Effects.Earthbend`, `Effects.Airbend`/`AirbendAll`); airbend emits only when ≥1 object was exiled (gated on the
    `airbendExiled` collection, CR 701.65b). Airbending a **spell** (`Effects.AirbendSpell`, the stack branch) emits the
    same `BendType.AIR` from `ExileTargetSpellExecutor` once the spell is exiled.
  - **firebending** emits `BendType.FIRE` when its attack trigger resolves (folded into `firebendingAttackTrigger`), so
    both printed `firebending(n)` and `Effects.GrantFirebending` fire it.
  - **waterbend** emits `BendType.WATER` engine-side when the waterbend cost is *paid* — in `CastSpellHandler` /
    `ActivateAbilityHandler`, ungated on how it was paid (CR 701.67c), so paying entirely with mana still fires it.
  Each emit also folds the element into the player's `BendsThisTurnComponent` (a `Set<BendType>`, reset for every player
  at the start of each turn). Read the count of *distinct* bends this turn via
  `DynamicAmount.TurnTracking(Player.You, TurnTracker.DISTINCT_BENDS)` (0–4); "if you've done all four this turn" is
  `Conditions.CompareAmounts(TurnTracking(You, DISTINCT_BENDS), ComparisonOperator.GTE, DynamicAmount.Fixed(4))`.
  `Effects.EmitBend(bendType)` is the internal marker effect (executor: `EmitBendEventExecutor`); card authors reach a
  bend through the keyword-action facades above, not this effect. `BendPerformedEvent` is internal (dropped from the
  client log).
- **Endure N** — `Effects.Endure(amount, target = EffectTarget.Self)` composes a `ModalEffect.chooseOne` of
  AddDynamicCounters (N +1/+1 counters on the enduring permanent) and a single N/N white Spirit `CreateTokenEffect`
  (no fake keyword — endure is always the effect of a triggered/activated ability, resolved at resolution time). `amount`
  is `DynamicAmount.Fixed` for "endure 2" or any dynamic value for "endure X" (e.g. Warden of the Grove reads
  `EntityProperty(Source, CounterCount(...))`); `target` defaults to `Self` ("it endures") but takes
  `EffectTarget.TriggeringEntity` when a card endures the creature that triggered it.
- **Forage** — effect form is `Patterns.Mechanic.forage` (`ChooseActionEffect`). All *cost* forms
  (`Costs.Forage()`, `Costs.additional.Forage`, and the cast-from-graveyard permission) route their
  payment, candidate-finding, and per-mode legal-action cost-info through the single
  `ForageCostResolver`, so the player chooses exile-vs-sacrifice and which cards/Food everywhere
  (CR 701.61).
- **Blight X** — `Costs.additional.BlightVariable` + `DynamicAmount.AdditionalCostBlightAmount` +
  `Conditions.BlightWasPaid(n)`.
- **Divvy (Fact-or-Fiction)** — `Patterns.Library.factOrFiction(...)`; `SplitPilesDecision` stays dormant until N > 2.
- **Astral Slide / delayed return** — `ExileUntilEndStepEffect` + `DelayedTriggeredAbility`.
- **Lord effects** — multiple `staticAbility { }` blocks + `ModifyStatsForCreatureGroup` /
  `AffectsFilter.OtherCreaturesWithSubtype`.
- **Player-scoped uncounterable grant** — `Effects.GrantSpellsCantBeCountered(target, filter, duration)` +
  `SpellsCantBeCounteredComponent`.
- **Static emblems** — `Effects.CreatePermanentEmblem(...)` for planeswalker emblems with static abilities.
- **The Ring / the Ring tempts you (CR 701.54)** — `Effects.TheRingTemptsYou(target = Controller)`: the player gets
  the Ring emblem (`TheRingComponent`, tempt-count tracked) and chooses a creature they control to become their
  Ring-bearer (`RingBearerComponent` designation). The emblem's four cumulative abilities are resolved by the engine,
  not card data: the bearer is made legendary in `StateProjector` and can't be blocked by greater power via
  `RingBearerCantBeBlockedByGreaterPowerRule`; the ≥2/≥3/≥4 triggered abilities are appended to the bearer by
  `TriggerAbilityResolver` (see `TheRingAbilities`). For card triggers/checks use `Triggers.RingTemptsYou`
  ("Whenever the Ring tempts you"), `Conditions.SourceIsRingBearer` ("if this is your Ring-bearer"), and
  `Conditions.YouChoseOtherCreatureAsRingBearer` ("if you chose a creature other than this as your
  Ring-bearer" — pairs with `Triggers.RingTemptsYou` for the Aragorn/Faramir/Gandalf/Galadriel cycle).
  CR 701.54a: the designation ends permanently when another player gains control of the bearer —
  every control-change executor strips `RingBearerComponent` via `clearRingBearerOnControlChange`, so a
  temporary steal (Threaten) does not silently restore the designation when control reverts.
- **Amass [subtype] N (CR 701.47)** — `Effects.Amass(count, subtype)` (fixed) or
  `Effects.Amass(amount, subtype)` (a `DynamicAmount`, for "amass Orcs X"). `subtype` is required (no default) —
  the amassed Army's type is printed on each card (Orcs for the LTR cards). If the controller controls no Army
  creature, a 0/0 black `[subtype]` Army token is created first (composing `CreateTokenEffect`); then they put N
  +1/+1 counters on an Army they control (a `SelectCardsDecision` resolved by `AmassContinuation` picks which one
  when they control several) and that Army becomes the subtype if it isn't already. The counter/subtype back half
  lives in `AmassResolution`; counters route through `AddCountersEffect`, so placement replacements still apply.

## 20. Miscellaneous author-facing knobs

- `triggeredAbility { controlledByTriggeringEntityController = true }` — the triggered ability is controlled by the
  triggering entity's controller (not source's). Useful for ETB-on-creature triggers and Death Match-style shapes.
- `metadata.oracleTextOverride` — bypass auto-generated oracle text when needed.
- `metadata.inBooster = false` — Special Guests, starter exclusives, bonus sheets.
- `colorIdentity` override is authoritative — never run `:mtg-sets:syncColorIdentityFromDump`.
- Layer dependencies (CR 613.8) — same-layer effects sort by dependency (trial application) before falling back to
  timestamp.
- Server is authoritative; never compute legal actions in the client. Every state change emits a `GameEvent` so triggers
  and animations can react.

### Ability identity (engine-internal, not authored)

`AbilityIdentity(cardDefinitionId, abilityId)` (`mtg-sdk` `scripting/AbilityIdentity.kt`) is the stable, **definition-scoped**
identity of a *kind* of ability — independent of the stack object or source entity instance. Two permanents printed from the
same card (and every future instance) share one identity for a given ability, because both halves are definition-scoped:
`cardDefinitionId` is the source's `CardComponent.cardDefinitionId`, and `abilityId` is the ability's `AbilityId` (generated
once when the card definition is built). Cards never author it: the engine threads it onto `TriggeredAbilityOnStackComponent`
/ `ActivatedAbilityOnStackComponent` (via `GameState.abilityIdentityOf(sourceId, abilityId)`) and onto `DecisionContext`, so
batch decisions can group structurally identical triggers and persistent yields can remember a per-ability answer across all
copies. Null for synthesized sources with no card definition (e.g. spell copies). See
`backlog/stack-collapse-and-batch-decisions.md` §C.2.

### Batched may-question (engine-internal, not authored)

When a run of structurally identical **optional, targeted** triggers ("Whenever …, you may … *target* …") fires off one
event, the engine asks the controller a single `BatchYesNoDecision` instead of one `YesNoDecision` per trigger — Magic
Online's "auto-stack identical triggers" affordance (`backlog/stack-collapse-and-batch-decisions.md` §B). Cards author
nothing: `TriggerProcessor` groups contiguous `liveTriggers` sharing one (controller, `AbilityIdentity`) key (and that would
actually raise the may-question rather than fizzle for lack of targets) into one decision carrying a `count`. The reply,
`BatchYesNoResponse(choice, applyToAll)`, is fanned back out by `BatchMayTriggerContinuation`:

- `applyToAll = true` resolves the whole run (`no` drops it; `yes` unwraps each may-gate and routes every instance through
  ordinary per-trigger target selection — only the yes/no is shared, never the target).
- `applyToAll = false` peels one instance off (answered with `choice`) and re-raises the batch for the remainder.

Only same-controller, same-identity, targeted-may triggers batch; targetless "may" triggers still decide at resolution, and a
lone trigger uses the plain per-trigger yes/no. The guard guarantees the engine never makes a meaningful target/ordering
choice on the player's behalf.

## 21. Structural lint (`CardLinter`)

Every registered card is structurally validated at build time: `CardValidator.validate` runs
`CardLinter` (mtg-sdk `serialization/CardLinter.kt`), and the corpus-wide gate is
`CardLintTest` in mtg-sets (beside `CardDefinitionSnapshotTest`). The linter walks the card's
serialized JSON tree, so every container — composites, gates, modes, granted abilities, class
levels, saga chapters, faces — is covered automatically. What it checks:

- **Pipeline dataflow** — every read of a named pipeline variable (`MoveCollection.from`,
  `CardSource.FromVariable`, `VariableReference`, `CollectionContainsMatch`, `chosenSubtypeKey`,
  …) must have a writer (`storeAs` / `storeSelected` / `storeMatching` / `StoreNumber` /
  `ChooseOption` / a cast-time additional cost, …) in the same resolution scope. A read written
  *nowhere* on the card is an **error** (typo → silent no-op); read-before-write and
  cross-resolution reads are warnings, as are stores nothing reads. A collection write `x`
  also satisfies the numeric read `x_count`.
- **Target bindings per owning ability** — `ContextTarget(i)` must fit the owning ability's
  flattened target slots (a `count = 2` requirement spans two indices); `BoundVariable(name)`
  must match a requirement `id` (indexed form `id[i]` allowed). Modes inherit the card-level
  requirements unless they declare their own; `ReflexiveTriggerEffect.reflexiveEffect` resolves
  against `reflexiveTargetRequirements`; `CreateDelayedTriggerEffect.effect` against its
  `targetRequirement`; granted/token abilities against their own requirements only.
- **Choice slots** — a `ChoiceSlot` read (`CastChoiceMade`, `DynamicAmount.CastChoice`,
  `HasChosenColor`, `SourceChosenModeIs`, …) needs a declarer on the card (`EntersWithChoice`,
  kicker, blight, sneak, `ChooseColorThen`/`ChooseColorForTarget`, or `ChooseNumberForSource`,
  which declares the slot named in its `slot` field); `SourceChosenModeIs` ids must match a
  declared `modeOptions` id.
- **Registry hygiene** — a string field whose name follows the dataflow conventions (`store*`,
  `from`, `collectionName`, `variableName`, …) on a node type the linter doesn't know is itself
  an error: **when you add an SDK type that reads or writes a named pipeline variable, classify
  it in `CardLinter.dataflowFields` in the same change** (and name the field conventionally so
  the hygiene net sees it).
- **`EntityMatches` entity roles** — the condition's `entity` must be a role the
  `ConditionEvaluator` dispatches (`Self`, `EnchantedPermanent`, `EnchantedCreature`,
  `EquippedCreature`, `ContextTarget`, `TriggeringEntity`); any other `EffectTarget` would be a
  silent constant `false` and is an **error**. Extending the evaluator to a new role must extend
  `CardLinter.supportedEntityMatchesRoles` in the same change.

Intentional exceptions go in `mtg-sets/src/test/resources/lint-allowlist.txt`
(`ErrorType|Card Name`, stale entries fail). Inside `ForEachInGroup` / `ForEachInCollection`,
address the iterated entity with `EffectTarget.Self` — `ContextTarget(0)` reads the cast-time
target list, which is unrelated to the iteration (this exact bug shipped on a real card before
the linter).

---

## Authoritative source files

| Area               | Path                                                            |
|--------------------|-----------------------------------------------------------------|
| Card DSL           | `mtg-sdk/src/main/kotlin/.../dsl/CardBuilder.kt`                |
| Effects            | `mtg-sdk/src/main/kotlin/.../dsl/Effects.kt`                    |
| Effect patterns    | `mtg-sdk/src/main/kotlin/.../dsl/{Library,Hand,Group,Exile,CreatureType,Misc}Patterns.kt` |
| Inline pipelines   | `mtg-sdk/src/main/kotlin/.../dsl/PipelineBuilder.kt`            |
| Triggers           | `mtg-sdk/src/main/kotlin/.../dsl/Triggers.kt`                   |
| Costs              | `mtg-sdk/src/main/kotlin/.../dsl/Costs.kt`                      |
| Conditions         | `mtg-sdk/src/main/kotlin/.../dsl/Conditions.kt`                 |
| Filters            | `mtg-sdk/src/main/kotlin/.../dsl/Filters.kt`                    |
| Targets            | `mtg-sdk/src/main/kotlin/.../dsl/Targets.kt`                    |
| Keywords           | `mtg-sdk/src/main/kotlin/.../core/Keyword.kt`                   |
| Card model         | `mtg-sdk/src/main/kotlin/.../model/CardDefinition.kt`           |
| Dynamic amounts    | `mtg-sdk/src/main/kotlin/.../scripting/values/DynamicAmount.kt` |
| Real card examples | `mtg-sets/src/main/kotlin/.../definitions/blb/cards/`           |

For step-by-step authoring workflow see [`api-guide.md`](api-guide.md) (and use the `add-card` skill);
for hard cases see [`managing-complex-and-rare-abilities.md`](managing-complex-and-rare-abilities.md).
