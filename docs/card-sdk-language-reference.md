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
- `basicLand("Plains" | "Island" | "Swamp" | "Mountain" | "Forest")` — shortcut for basic lands (sets type line,
  intrinsic mana ability, supertype). Supports `collectorNumber`, `artist`, `flavorText`, `imageUri`, `rarity`, and
  `inBooster` (set `false` to keep an art variant defined but exclude it from the draft/sealed deck-building basic pool).

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
- `auraTarget: TargetRequirement?` — what this Aura enchants.
- `morph: String?` — morph mana cost (cast face-down).
- `morphCost: PayCost?` — non-mana morph cost.
- `morphFaceUpEffect: Effect?` — effect that fires when this morph turns face up.
- `warp: String?` — Warp alt-cost; exiles at end of turn.
- `evoke: String?` — Evoke alt-cost; sacrifices on ETB.
- `selfAlternativeCost: SelfAlternativeCost?` — generic alternative-cost slot.
- `castTimeCreatureTypeChoice: CastTimeCreatureTypeSource?` — forces a creature-type choice at cast time.
- `cantBeCountered: Boolean` — spell is uncounterable.
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
- `ADVENTURE` — primary face is a creature, `cardFaces[0]` is an instant/sorcery Adventure (CR 715). Resolving the
  Adventure exiles the card and grants permission to cast the creature from exile.
- `OMEN` — primary face is a permanent (creature), `cardFaces[0]` is an instant/sorcery Omen (Tarkir: Dragonstorm).
  Casts exactly like an Adventure (creature face, or Omen via `CastSpell.faceIndex = 0`), but resolving the Omen
  **shuffles the card into its owner's library** instead of exiling it — no cast-from-exile linkage. DSL:
  `card { omen("Name") { spell { … } } }`.
- `MODAL_DFC` — primary characteristics are the front face, `cardFaces[0]` is the back face (CR 712). Cast **one**
  face from hand (front via primary characteristics, back via `CastSpell.faceIndex = 0`), never both. Unlike
  ADVENTURE there is no exile-then-recast linkage — a spell back resolves as an ordinary spell (graveyard, or exile
  when its script sets `selfExileOnResolve` via `spell { selfExile() }`). DSL: `card { modalBack("Name") { spell { … } } }`.

**`CardFace` (SPLIT / ADVENTURE / OMEN / MODAL_DFC)**

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
- `Costs.Craft(filter, minCount = 1)` — Craft material cost (CR 702.167a): exile this permanent
  **and** exile at least `minCount` cards matching `filter` selected from the combined pool of
  permanents you control and cards in your graveyard. Atomic because CR 702.167a pairs the
  self-exile with the materials-exile in one clause. Records the chosen materials on the source's
  `CraftedFromExiledComponent` so the back face's CDA can read them after the source returns
  transformed. Always combined with `Mana(...)` and used with the
  `Effects.ReturnSelfFromExileTransformed` resolution effect (the `card { craft(filter, cost) }`
  helper wires the whole pattern).
- `Costs.Composite(c1, c2, ...)` — multiple costs paid together.

**Spell-level alternatives**

- `selfAlternativeCost` — generic "cast instead for" alt-cost.
- `evoke` — pay evoke cost; creature is sacrificed at ETB.
- `morph` — cast face-down for `{3}`-ish.
- `warp` — cast from anywhere; exiled at end of turn.
- `conditionalFlash` — flash while condition holds.
- `cantBeCountered` — spell is uncounterable.
- `xManaRestriction = setOf(Color.BLACK, Color.RED)` — "spend only [colors] on X." Restricts which
  mana may pay the `{X}` portion of the cost (the fixed colored/generic portion is unaffected).
  Available in both `spell { }` and `activatedAbility { }` blocks; honored by the mana solver and the
  payment path. Per-color amount spent on X is then readable via `DynamicAmount.ManaSpentOnX(color)`.
  Soul Burn (`spell { xManaRestriction = setOf(Color.BLACK, Color.RED) }`) and Atalya, Samite Master
  (`activatedAbility { xManaRestriction = setOf(Color.WHITE) }`) are the first users.

**`Costs.additional.*`** (wraps `AdditionalCost`) — extra costs paid alongside the mana cost. Card
definitions construct these through the facade, e.g. `Costs.additional.SacrificePermanent(Filters.Creature)`.

- `Costs.additional.BlightVariable` — "as you cast, you may pay X life" (Blight X); X exposed via
  `DynamicAmount.AdditionalCostBlightAmount`.
- `Costs.additional.PayLifePerTarget(amountPerTarget)` — "this spell costs N life more to cast for
  each target." Pair with an unbounded `TargetCreature(unlimited = true)` etc.; the engine
  auto-pays `amountPerTarget × action.targets.size` at cast resolution (Phyrexian Purge).

**`Costs.pay.*`** (wraps `PayCost`) — payable costs used by [`PayOrSufferEffect`](#15-replacement-effects) ("do X
unless you Y") and by `morphCost` (non-mana face-up cost). Distinct from `AbilityCost` / `Costs.*`
which model an ability's activation cost; `PayCost` models a single cost the engine prompts the
player to pay against an alternative consequence.

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
- `DealXDamage(target)` — deal X damage (spell's X).
- `Fight(target1, target2)` — two creatures each deal damage equal to their power to each other (CR 701.12).
- `DividedDamageEffect(totalDamage, minTargets, maxTargets, dynamicTotal?)` — "N damage divided as you
  choose among target ..." The targets come from the ability's target requirement; pair with
  `TargetCreature(count, minCount)` (Forked Lightning, Skirk Volcanist) or, for "any number of target"
  + a dynamic total, a `TargetObject(optional = true, dynamicMaxCount = ..., filter = ...)`. Set
  `dynamicTotal` (a `DynamicAmount`) for totals computed when the ability resolves/goes on the stack —
  Ureni, the Song Unending: `dynamicTotal = DynamicAmounts.landsYouControl()`. Works for creatures and
  planeswalkers (`GameObjectFilter.CreatureOrPlaneswalker`); zero chosen targets ⇒ no-op.

### Life

- `GainLife(amount, target?)` — target gains life (default: controller).
- `LoseLife(amount, target)` — target loses life.
- `SetLifeTotal(amount, target)` — set target's life total to N.
- `ExchangeLifeAndPower(target)` — swap target's power with controller's life total.
- `LoseHalfLife(roundUp, target, lifePlayer?)` — lose half of life total (round up/down).
- `LoseGame(target, message?)` — target loses the game.
- `WinGame(target, message?)` — target wins the game.
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
- `Discard(count, target)` — controller-of-target chooses; mandatory.
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
- `ExileGroupAndLink(filter, storeAs?)` — exile all matching permanents into source's linked exile pile.
- `ExileFromTopRepeating(count, repeatCondition)` — keep exiling top cards while a condition holds.
- `ExileLibraryUntilManaValue(manaValue)` — exile from library until mana value ≤ N.

### Return / placement

- `ReturnToHand(target)` — bounce to hand.
- `PutOnTopOfLibrary(target)` — place target on top of its owner's library.
- `PutOnTopOrBottomOfLibrary(target)` — player chooses top or bottom.
- `PutSecondFromTopOrBottomOfLibrary(target)` — second-from-top or bottom.
- `ShuffleIntoLibrary(target)` — shuffle target into owner's library.
- `PutIntoLibraryNthFromTop(target, positionFromTop)` — place N from the top.
- `PutOntoBattlefield(target, tapped?)` — put target on the battlefield.
- `PutOntoBattlefieldUnderYourControl(target)` — under controller's control.
- `PutOntoBattlefieldFaceDown(count, target?)` — enter face-down (2/2 morph shape).
- `ReturnSelfToBattlefieldAttached(target)` — return source attached to target (Aura recursion).
- `ReturnSelfFromExileTransformed` — Craft resolution (CR 702.167a). Returns the source from exile to the
  battlefield as its back face, under its owner's control, and re-attaches the source's
  `CraftedFromExiledComponent` recording the exiled materials. Pair with `AbilityCost.Craft`; see the `Craft`
  keyword helper in the keyword catalog.
- `ReturnCreaturesPutInGraveyardThisTurn(player)` — Patriarch's Bidding shape.

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
- `GrantMayPlayFromExile(from, expiry?, withAnyManaType?, condition?, landEntersTapped?)` — controller may play matching cards from exile. `landEntersTapped=true` forces a played land tapped regardless of its own ETB script (Lightstall Inquisitor); PlayLandHandler reads the flag off the active `MayPlayPermission` at play time and stamps `TappedComponent` before the card's intrinsic `EntersTapped` branch runs.
- `GrantPlayWithoutPayingCost(from)` — same, without paying mana costs.
- `GrantPlayWithCostIncrease(from, amount)` — stamp `PlayWithCostIncreaseComponent(controllerId, amount)` on every card in the collection, so the next cast pays `{amount}` extra generic. Pair with `GrantMayPlayFromExile` for "each spell cast this way costs {N} more" clauses (Lightstall Inquisitor); for target-based "exile this permanent, owner may play it, opponents tax" effects use `Effects.ExileAndGrantOwnerPlayPermission` instead.
- `GrantFreeCastTargetFromExile(target)` — cast specific exiled card for free.

### Stats & keywords

- `ModifyStats(power, toughness, target?)` — `±P/±T` until end of turn (default scope).
- `GrantKeyword(keyword, target, duration)` — grant a keyword for a duration.
- `GrantHarmonize(target, cost?, duration)` — grant **Harmonize** (CR 702.180) to a target instant/sorcery card in a graveyard. `cost` defaults to `null` = "equal to the card's mana cost" (Songcrafter Mage); pass a `ManaCost` for a fixed harmonize cost. Records a runtime `GrantedKeywordAbility` keyed to the card entity; the cast-from-graveyard enumerator, the cast handler, the alternative-payment handler (tap-for-power reduction), and the stack resolver (exile on resolution) all read printed-or-granted harmonize through the shared `HarmonizeGrants` resolver, so a granted harmonize behaves identically to a printed one. The grant expires in the cleanup step (EndOfTurn) and surfaces a "Granted Ability" badge on the card.
- `RemoveKeyword(keyword, target, duration)` — strip a keyword.
- `RemoveAllAbilities(target, duration)` — wipe all abilities (including granted keywords).
- `LoseAllCreatureTypes(target, duration)` — remove all creature subtypes.
- `SetCreatureSubtypes(subtypes, target, duration)` — replace subtypes outright.
- `AddCreatureType(subtype, target, duration)` — additive subtype.
- `GrantHexproof(target, duration)` — temporary hexproof.
- `GrantExileOnLeave(target)` — "if it would leave, exile instead".
- `GrantKeywordToAttackersBlockedBy(keyword, target)` — grant keyword to creatures this blocks.

### Counters

- `AddCounters(type, count, target)` — add N counters of `type`.
- `AddDynamicCounters(type, amount, target)` — count is computed at resolution.
- `DoubleCounters(type?, target?)` — one-shot doubling of the `type` counters (default `+1/+1`) already on the
  target: reads the current count and places that many more (so the total doubles). Distinct from the
  `DoubleCounterPlacement` replacement (which doubles *future* placements); the added counters still trigger
  placement replacements like Hardened Scales. No-op with zero counters. Sage of the Fang.
- `RemoveCounters(type, count, target)` — remove N counters.
- `RemoveAnyNumberOfCounters(target)` — player removes 0 or more.
- `RemoveAllCounters(target)` — wipe every counter.
- `RemoveAllCountersOfType(type, target)` — wipe one kind.
- `MoveAllLastKnownCounters(target)` — Hooded Hydra / Essence Channeler — move every counter kind from source's
  last-known state.
- `Counters.ANY` — wildcard counter-type string for "counters of any type" triggers/events (e.g.
  `Triggers.countersPlacedOn`); not a real placeable counter, only a matcher sentinel.
- `DistributeCountersFromSelf(type?, count?)` — split source's counters among creatures you control.
- `DistributeCountersAmongTargets(total, type?, minPerTarget?)` — divvy N counters among chosen targets.
- `Proliferate()` — add one counter of each kind already present on chosen permanents/players (CR 701.27).
- `AddCountersToCollection(name, type, count)` — add counters to cards held in a pipeline collection.

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
- `AddManaOfChoice(colorSet, amount?, restriction?, riders?)` — **unified primitive.** Add N mana of one color the controller picks from a resolved [ManaColorSet](#manacolorset). All "any-color from a constrained pool" cards (any color, commander identity, among permanents, lands could produce, source-chosen color) are expressed as this effect plus a different `ManaColorSet`. `riders` is a `Set<ManaSpellRider>` consumed when the mana pays for a spell (e.g. Path of Ancestry tags its mana with `ScryOnSharedTypeWithCommander`); when riders are set without a `restriction`, the engine stores the entries under `ManaRestriction.AnySpend` to preserve the rider through the pool.
- `AddAnyColorMana(amount?, restriction?)` — sugar for `AddManaOfChoice(ManaColorSet.AnyColor, amount)`. "Add N mana of any **one** color" (Gilded Lotus): one chosen color, N of it. For "any **combination** of colors" use `AddManaInAnyCombination`.
- `AddManaOfChosenColor(amount?)` — sugar for `AddManaOfChoice(ManaColorSet.SourceChosenColor, amount)`.
- `AddManaOfColorAmong(filter)` — sugar for `AddManaOfChoice(ManaColorSet.AmongPermanents(filter))`.
- `AddManaOfColorLandsCouldProduce(scope)` — sugar for `AddManaOfChoice(ManaColorSet.LandsCouldProduce(scope))`. Fellwar Stone / Exotic Orchard / Reflecting Pool shape.
- `AddManaOfColorInCommanderColorIdentity()` — sugar for `AddManaOfChoice(ManaColorSet.CommanderIdentity)`. Arcane Signet / Command Tower shape.
- `AddAnyColorManaSpendOnChosenType(typeName)` — mana that can only pay for a specific card type (kept separate because it derives a runtime [ManaRestriction] from the source's chosen subtype).
- `AddDynamicMana(amount, allowedColors, restriction?)` — split X across a fixed color set, distinct from `AddManaOfChoice` because it distributes the full X total across multiple colors rather than producing X copies of one chosen color.
- `AddManaInAnyCombination(amount, allowedColors?, restriction?)` — "Add N mana in any combination of colors" (Wizard's Rockets, Thornvault Forager, Interdimensional Web Watch). Sugar for `AddDynamicMana`; `allowedColors` defaults to all five. The controller colors **each** pip independently at resolution (3+ colors → pip-by-pip color choice; 2 colors → one "how much of the first" prompt; ≤0 → no mana, no prompt), so the result can mix colors — distinct from `AddAnyColorMana`, where all N share one color.
- `AddOneManaOfEachColorAmong(filter)` — one mana of *each* color found among matching permanents (Bloom Tender shape).
- `PayDynamicMana(amount, payer?)` — pay a dynamically-computed amount of **generic** mana at resolution; the
  dynamic, payer-parametric twin of the flat `PayManaCostEffect`. `amount` is a [DynamicAmount](#dynamicamount)
  evaluated at resolution (0 pays nothing and succeeds); `payer` is a `Player` reference defaulting to the
  controller (`Player.You`). This is the building block for **"pay {N} for each X"** templating — pair it with a
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

- `CreateToken(name, p, t, colors?, subtypes?, keywords?, count?, tapped?)` — make N creature tokens.
  `count` accepts an `Int` or a `DynamicAmount` (the latter for "create X tokens" wording — e.g. Verdeloth the
  Ancient passes `count = DynamicAmount.XValue` to make X Saprolings when kicked). Publishes the created token
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
  For an *inline* token (not a registered `CardDefinition`) that has its own abilities, the raw
  `CreateTokenEffect` constructor exposes `staticAbilities`, `triggeredAbilities`, **and**
  `activatedAbilities` — each list is granted to every created token at resolution (permanent
  duration) via `GameState.granted{Static,Triggered,Activated}Abilities`, so the legal-action
  enumerator and `ActivateAbilityHandler` pick them up like any other granted ability. Example:
  Mourner's Surprise's "1/1 red Mercenary creature token with \"{T}: Target creature you control
  gets +1/+0 until end of turn. Activate only as a sorcery.\"" passes a single
  `ActivatedAbility(cost = AbilityCost.Tap, effect = Effects.ModifyStats(1, 0), targetRequirements =
  listOf(Targets.CreatureYouControl), timing = TimingRule.SorcerySpeed)`. (Remember a token is a
  creature, so a `{T}` ability is summoning-sick the turn the token enters.)
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
- `CreateTokenCopyOfSelf(count?, tapped?)` — token copies of source.
- `CreateTokenCopyOfTarget(target, count?, overridePower?, overrideToughness?, tapped?, attacking?, triggeredAbilities?, addedKeywords?, addedSupertypes?, removedSupertypes?, overrideColors?, overrideSubtypes?, sacrificeAtStep?, sacrificeOnlyOnControllersTurn?)` —
  token copy of another permanent (or a card in any zone — the executor copies the target's `CardComponent`,
  so a graveyard/exile card works). `overrideColors`/`overrideSubtypes` replace the copy's colors/subtypes
  outright for "a token that's a copy … except it's a 5/5 black Demon" wording (Ardyn, the Usurper).
  `attacking` only applies to copies whose printed type line is a creature (a copy of a non-creature card
  still enters tapped but never attacking). `sacrificeAtStep` schedules one delayed `SacrificeTargetEffect`
  per created copy at that step (the sacrifice sibling of `CreateTokenEffect.sacrificeAtStep`);
  `sacrificeOnlyOnControllersTurn = true` restricts it to "at the beginning of *your* next end step"
  (Mardu Siegebreaker: a tapped+attacking copy of the linked-exiled card, sacrificed at your next end step).
- `CreateTokenCopyOfEquippedCreature(count?, tapped?)` — equipment-specific copy.
- `CreateTreasure(count?, tapped?)` — Treasure tokens.
- `CreateFood(count?, controller?)` — Food tokens.
- `CreateLander(count?, controller?)` — Lander land tokens.
- `CreateMutavault(count?, tapped?, controller?)` — Mutavault tokens.
- `CreateRoleToken(roleName, target)` — attach a Role aura token.
- `CreateMapToken(count?)` — Map artifact tokens.
- `CreateDroneToken(count?)` — Drone tokens.
- `CreateMunitionsToken(count?)` — Munitions noncreature artifact tokens (Weapons Manufacturing); the LTB damage
  trigger lives on the predefined `Munitions` `CardDefinition` and is picked up automatically by the engine's
  `TriggerAbilityResolver`.
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
  Rangers of Ithilien). `StateProjector` gates these per-frame for the instantaneous view; the
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
- `SkipNextTurnEffect(target)` — target skips their next turn.
- `Effects.SkipNextDrawStep(target = Controller)` (`SkipNextDrawStepEffect`) — target skips their next draw step. Adds a one-shot `SkipDrawStepComponent` marker consumed by `DrawPhaseManager.performDrawStep` (Elfhame Sanctuary's "you skip your draw step this turn").
- `HijackNextTurnEffect(target)` — you control target's next turn.
- `GrantCantBeBlockedByChosenColorEffect(target, duration)` — unblockable except by chosen color.
- `Effects.GrantCantBeBlockedExceptBy(target, blockerFilter, duration = EndOfTurn)` (`GrantCantBeBlockedExceptByEffect`) —
  the floating, one-shot grant of "can't be blocked except by creatures matching `blockerFilter`". The dynamic
  counterpart to the static `CantBeBlockedExceptBy` ability (and the filter-based sibling of the color-only
  `GrantCantBeBlockedExceptByColorEffect`). Routes through the same projected `cantBeBlockedExceptByFilters` channel
  the static ability uses, so the existing `CantBeBlockedExceptByRule` enforces it. Used by **Resilient Roadrunner**:
  `{3}: This creature can't be blocked this turn except by creatures with haste` —
  `Effects.GrantCantBeBlockedExceptBy(EffectTarget.Self, GameObjectFilter.Creature.withKeyword(Keyword.HASTE))`.
- `CantCastSpellsEffect(target, until?)` — target can't cast spells. Facade: `Effects.CantCastSpells(target, duration)`.
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
- `ForceSacrificeEffect(target, count)` — edict; target sacrifices N creatures.
- "Return a permanent you control [to its owner's hand]" is a pipeline composition, not an effect type:
  `GatherCards(BattlefieldMatching(filter, Player.You, excludeSelf?))` →
  `SelectFromCollection(ChooseExactly(1), useTargetingUI = true)` → `MoveCollection(→ HAND)` (the
  battlefield→hand move routes each card to its owner's hand). See Mistbreath Elder.

### Stack manipulation

- `CounterEffect(target, condition?, destination?)` — counter a spell/ability; optionally send elsewhere.
  - `target = CounterTarget.Spell` / `Ability` / `SpellOrAbility` — `SpellOrAbility` dispatches at resolution by inspecting whether the stack entity has a `SpellOnStackComponent`. Used by Teferi's Response.
  - `condition = CounterCondition.UnlessPaysMana(cost, onPaid?)` / `UnlessPaysDynamic(amount, onPaid?)` — "unless its controller pays …" with an optional `onPaid: Effect` rider that fires **only** when the spell's controller pays (Divert Disaster's "If they do, you create a Lander token"). The rider executes with the counter's controller as `controllerId`, so "you" in the rider resolves to the caster of the counter. The rider does not fire when the spell is countered. Facade: `Effects.CounterUnlessPays(cost, onPaid)` / `Effects.CounterUnlessDynamicPays(amount, exileOnCounter, onPaid)`.
- `CounterAllOnStackEffect(filter?, destination?)` — counter everything matching.
- `OpenLifeBid(onWin, participant = Player.AnOpponent)` — open life-bidding auction between you and `participant` (resolved against the effect context). You open at a bid of 1; the two bidders alternate topping the high bid (yes/no to top, then a number for the amount, capped at the bidder's life) until one passes. The high bidder loses that much life; `onWin` runs **only if you win**, with the original targets in context. If `participant` resolves to you (or to nobody), you're the sole bidder and win at the opening bid. For Mages' Contest, bid against the targeted spell's controller and counter it: `Effects.OpenLifeBid(Effects.CounterSpell(), Player.ControllerOf("target spell"))` — pair with a `TargetSpell` requirement.
- `DestroySourceOfTargetedAbilityEffect` — when the targeted stack object is a permanent's activated/triggered ability, destroy that source permanent. Compose *before* the counter step so the ability component is still readable (Teferi's Response).
- `CopyTargetSpellEffect(target)` — copy a spell on the stack.
- `CopyTargetTriggeredAbilityEffect(target)` — copy a triggered ability on the stack.
- `CopyNextSpellCastEffect(copies = 1, spellFilter = InstantOrSorcery)` (facade `Effects.CopyNextSpellCast(copies, spellFilter)`) — when its controller next casts a spell matching `spellFilter` this turn, create `copies` copies of it. `spellFilter` is a `GameObjectFilter` matched against the spell as it's cast, so the default "instant or sorcery" (Howl of the Horde) can be widened — e.g. `GameObjectFilter.Creature` for "copy the next creature spell." Consumed after one matching cast. Non-matching casts leave the entry waiting.
- `CopyEachSpellCastEffect(copies = 1, spellFilter = InstantOrSorcery)` (facade `Effects.CopyEachSpellCast(copies, spellFilter)`) — the persistent sibling: copies **every** spell matching `spellFilter` the controller casts for the rest of the turn (The Mirari Conjecture Ch. III). Same `spellFilter` parameterization as above.
- `MakeNextSpellUncounterableEffect(spellFilter = Any)` (facade `Effects.MakeNextSpellUncounterable(spellFilter)`) — one-shot rider: the controller's **next** spell matching `spellFilter` cast this turn can't be countered, then the entry is consumed. Stamps `CantBeCounteredComponent` on that spell as it's cast (so it stays uncounterable for as long as it's on the stack); non-matching casts leave the entry waiting, and an unused entry clears at the start of the controller's next turn. Same pending-rider shape as `CopyNextSpellCastEffect`. Contrast with the duration-based `GrantSpellsCantBeCountered` (Domri), which protects **every** matching spell cast for a whole duration rather than just the next one. Used by **Mistrise Village** ("{U}, {T}: The next spell you cast this turn can't be countered.").
- `CopyCardIntoCollectionEffect(source, storeAs)` (facade `Effects.CopyCardIntoCollection(source, storeAs)`) — copy a **card in a zone** (not a spell on the stack), publishing the copy's entity id to pipeline collection `storeAs`. Per Rule 707.12 the copy is created in the card's current zone under the effect's controller and tagged as a stack-style copy, so once cast it becomes a token if it's a permanent spell and ceases to exist if it's an instant/sorcery (Rule 707.10). Pair with `CastFromCollectionWithoutPayingCostEffect(from)` (facade `Effects.CastFromCollectionWithoutPayingCost(from)`, wrap in `MayEffect` for "you may cast") to express "copy a card, then cast the copy" — e.g. **Shiko, Paragon of the Way**: `Composite(MoveToZoneEffect(target, Zone.EXILE), Effects.CopyCardIntoCollection(target, "copy"), MayEffect(Effects.CastFromCollectionWithoutPayingCost("copy")))`. A copy that is never cast is swept up by the Rule 707.10a state-based action (`PhantomCardCopiesCheck`), so no explicit cleanup step is needed.
- `CastAnyNumberFromCollectionWithoutPayingCostEffect(from)` (facade `Effects.CastAnyNumberFromCollectionWithoutPayingCost(from)`) — the multi-cast sibling of `CastFromCollectionWithoutPayingCostEffect`. **During this effect's resolution**, the controller is offered the cards in pipeline collection `from` (filtered to those still in exile) one at a time and may cast each for free until they decline; each cast's targets / X / modes flow through the normal cast machinery. Because the casts go through the synthesized-cast path (like Cascade), card-type **timing restrictions are ignored** and no lingering "you may play it later" permission is granted — cards left uncast just stay where they are (the controller can't wait until later in the turn). Hand it the eligible set: filter the collection upstream (e.g. nonland + `FilterCollection(ManaValueAtMost(...))`). Models "you may cast any number of spells with mana value X or less from among them without paying their mana costs" — e.g. **Kotis, the Fangkeeper**: `GatherCards(TopOfLibrary(damage, TriggeringPlayer)) → MoveCollection(→ exile) → FilterCollection(Nonland) → FilterCollection(ManaValueAtMost(damage)) → CastAnyNumberFromCollectionWithoutPayingCostEffect("castable")`. Also used by **Villainous Wealth** (the same chain off an {X} sorcery) and **Etali, Primal Storm** (exile the top card of each library, no MV cap).
- `FilterCollection(from, CollectionFilter.InZone(zone), storeMatching)` — keep only the cards in pipeline collection `from` that are **currently** in `zone`. Pipeline collections track entity refs, not live location, so a card can leave its zone mid-resolution (e.g. an exiled card cast for free moves to the stack). Use this to act on "the ones still there." Models the "you may cast it … if you don't, put that card into your hand" fallback of the **Tarkir: Dragonstorm "…storm" enchantments** (Breaching Dragonstorm): `GatherUntilMatch(Nonland) → MoveCollection(→ exile) → FilterCollection(ManaValueAtMost(8), "castable") → ConditionalOnCollection("castable", ifNotEmpty = MayEffect(CastFromCollectionWithoutPayingCost("castable"))) → FilterCollection("nonland", InZone(EXILE), "uncast") → MoveCollection("uncast" → hand)` — only the nonland still in exile (not the one just cast) goes to hand; the lands stay exiled. The `ConditionalOnCollection` wrapper suppresses the empty "you may cast" prompt when the nonland's mana value is > 8.
- `ChangeTargetEffect(spell, newTarget)` — change a spell's target.
- `ChangeSpellTargetEffect(spell, filter)` — same, filtered.
- `ReselectTargetRandomlyEffect(spell)` — re-choose targets at random.
- `Effects.ChangeTriggeringObjectTargets(chooser = RetargetChooser.Controller)` — the player named by `chooser` may change the target or targets of the triggering spell/ability (`context.triggeringEntityId`); the player-chosen, multi-target counterpart of `ReselectTargetRandomly`. `RetargetChooser.Controller` = the effect's controller; `RetargetChooser.OwnerOfStored(name)` = the owner of the single card in pipeline collection `name` (≠1 card → no chooser → no-op). Reselection is offered slot-by-slot among the original object's legal targets (legality judged from *its* controller, current target kept as a "keep" option, no target chosen twice). **Psychic Battle** composes from atoms: `Composite(GatherCards(TopOfLibrary(1, Player.Each), revealed=true, storeAs="revealed"), FilterCollection("revealed", GreatestManaValue, storeMatching="w"), ChangeTriggeringObjectTargets(RetargetChooser.OwnerOfStored("w")))` — a tie keeps several greatest cards so `OwnerOfStored` finds no unique owner and the targets stay put.
- `ReturnSpellToOwnersHandEffect(spell)` — return a spell from the stack to hand.

### Combat-shape & misc

- `PreventDamageEffect(amount, direction, scope, sourceFilter, onPrevented, gainLifeFromColors, duration)` — prevention shield. `amount = null` prevents all. `sourceFilter` can be `ChosenSource` (player picks any source on resolution) or `ChosenColoredSource` (player picks a source on resolution, but only colored sources are offered — "a source of your choice that shares a color with the mana spent"; a colorless source qualifies for nothing, so it's never offered — Protective Sphere). `onPrevented: Effect?` is an **arbitrary follow-up effect** run when a single-instance `ChosenSource` shield prevents an instance of damage (see below). `gainLifeFromColors: Set<Color>` makes the shield's controller gain that much life whenever it prevents damage from a source of one of those colors (Samite Ministration). Facades: `Effects.PreventNextDamage`, `Effects.PreventNextDamageFromChosenSource(amount, target)`, `Effects.PreventNextDamageFromChosenSource(onPrevented)`, `Effects.PreventAllDamageFromChosenSource(target, gainLifeFromColors)`, `Effects.PreventAllDamageFromChosenColoredSource(target)`, `Effects.DeflectNextDamageFromChosenSource()`.
  - **Prevent-and-react (`onPrevented`)** — instead of a bespoke reaction type, the chosen-source shield runs **any composed effect** when it fires, as a real triggered ability on the stack ("When damage is prevented this way, …", CR-faithful — opponents get priority and can respond). Mechanically: on resolution the shield is created **and** a linked event-based delayed triggered ability (`CreateDelayedTriggerEffect`-style) whose `effect` is `onPrevented`; when the shield prevents an instance it emits an internal `DamagePreventedEvent` that fires only that delayed trigger (matched by id). Inside the trigger the prevented amount is `DynamicAmounts.preventedDamage()` ("that much"/"that many") and the prevented source's controller is `EffectTarget.ControllerOfTriggeringEntity` ("that source's controller") — the same pair Tephraderm uses. So Deflecting Palm's `onPrevented` = `DealDamage(ControllerOfTriggeringEntity, preventedDamage())`; New Way Forward's = `Composite(DealDamage(ControllerOfTriggeringEntity, preventedDamage()), DrawCards(preventedDamage()))`. Because the payoff is a normal stack ability, it may be interactive (targets, replacements) like any other.
- `BecomeCreatureEffect(target, p, t, subtypes, keywords, duration)` — animate non-creature (lands, artifacts).
- `BecomeSaddledEffect(target = Self)` (facade `Effects.BecomeSaddled()`) — target permanent becomes saddled until end of turn (CR 702.171b). The resolving half of a Saddle ability: stamps the transient `SaddledComponent` marker (cleared at end of turn / on leaving the battlefield; not copiable) and emits `BecameSaddledEvent`. No P/T or type change — read the marker with `Conditions.SourceIsSaddled` / `.saddled()`.
- `EachPermanentBecomesCopyOfTargetEffect(target, filter, duration, excludeTarget)` — mass copy (Mirrorform, Naga Fleshcrafter renew). Facade `Effects.EachPermanentBecomesCopyOfTarget(...)`. Copies copiable values only (Rule 707) — counters, tapped state, attached auras/equipment and non-copy modifiers stay put. `duration = Duration.Permanent` (default) bakes the copy into base state for good; `Duration.EndOfTurn` makes a temporary copy reverted by the end-of-turn cleanup (each affected permanent restores its pre-copy `CardComponent` from its `CopyOfComponent` snapshot). `excludeTarget = true` keeps the copy **source** out of the affected set, for "each **other** … becomes a copy of that …" wordings where the target keeps its own identity (and any counter just placed on it).
- `AnimateLandEffect(target, subtypes, keywords, duration)` — land becomes a creature.
- `ExploreEffect(target)` — Explore mechanic (reveal top; land → battlefield, else hand + counter).
- `AttachEquipmentEffect(equip, target)` — attach an Equipment.
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
- `PhaseOutEffect(target = Self)` — phase the target permanent out (Rule 702.26); facade `Effects.PhaseOut(target)`. While phased out it's treated as though it doesn't exist (excluded from `getBattlefield`, so from projection, triggers, combat, targeting, and SBAs) and phases back in before its controller's next untap step. Indirect phasing (attached Auras/Equipment) is handled automatically. Used as the `suffer` branch of a pay-or-phase trigger (Vaporous Djinn: "phases out unless you pay {U}{U}" = `PayOrSufferEffect(Costs.pay.Mana(...), Effects.PhaseOut())`).
- `MarkExileOnDeathEffect(target)` — replace next "to graveyard" with "to exile".
- `GatedEffect(gate, then, otherwise?, decisionMaker?)` — **the unified resolution frame for the
  optional / gated-effect cluster** (phase-rs Lesson 1). A `Gate` decides whether `then` runs; if it
  fails, `otherwise` runs. One executor + one continuation/resumer own the canonical unwind order, so
  targets on `then`/`otherwise` lock at trigger time (CR 603.3d) and the gate is resolved at
  resolution time (CR 117.3a) by `decisionMaker` (defaults to the controller) — the may-vs-target
  timing is correct by construction rather than re-encoded per wrapper. Gates:
  - `Gate.MayDecide(prompt?, hint?, sourceRequiredZone?, inlineOnTrigger?)` — pure yes/no
    ("You may [then]."). Replaces `MayEffect` (see the `MayEffect` facade below). `sourceRequiredZone`
    skips the gate silently when the source has left that zone by resolution; `inlineOnTrigger`
    renders the yes/no on the triggering permanent rather than as a modal.
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
    action — non-zone-move actions (deal damage, gain life, …) must state `SuccessCriterion.Always`
    or `CollectionNonEmpty(name, min)` explicitly instead of silently inheriting a fail-open
    "it happened". `CollectionNonEmpty` gates on the action's actual pipeline collection
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
- `MayEffect(effect, descriptionOverride?, sourceRequiredZone?, inlineOnTrigger?, hint?, decisionMaker?)`
  — "You may [effect]." Facade preserved for existing cards; it now **lowers to
  `GatedEffect(Gate.MayDecide(...), then = effect)`** (compiled form is `Gated`, no distinct `May` type
  or executor). The may-vs-target trigger reorder — for a "may" ability that *also* targets, the yes/no
  is asked *before* target selection (Invigorating Boon) — recognizes the lowered shape via the
  `Effect.asMayDecide()` matcher (a bare `Gate.MayDecide` with no `otherwise`).
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
  via `MoveCollection(from = "discarded", …)`).
- `RepeatWhileEffect(condition, effect, maxIterations?)` — run effect repeatedly while condition holds.

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
    fresh `storedCollections` (Winds of Change, Bend or Break).
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

### Modal & choice

- `ModalEffect.chooseOne { mode(...) }` / `ModalEffect.chooseN(n) { ... }` — modal effect block.
- `ChooseActionEffect(choices)` — player picks from a list of effects.
- `GrantProtectionFromColor(color, target, duration)` — grant protection from a **fixed** color to a target (no player choice); a thin recipe over `GrantKeyword("PROTECTION_FROM_<COLOR>")`. "{W}: Target creature gains protection from red until end of turn." (Crimson Acolyte).
- `ChooseColorThenEffect(whenChosen)` — pick a color, then run a function of that color.
- `Effects.ChooseNumberThen(then, minValue=0, maxValue=16, prompt)` — pick a number in `[minValue, maxValue]`,
  then run `then` once with the chosen number exposed via the effect context as **X**. Atomic effects and filters
  under `then` read it through `ManaValueEqualsX` (`.manaValueEqualsX()`). Compose with `CompositeEffect` for
  multi-step cards (Void: destroy all artifacts/creatures with that mana value, then a target player reveals their
  hand and discards all nonland cards with that mana value).
- `GrantHexproofFromChosenColorEffect(target)` — hexproof from chosen color.
- `GrantProtectionFromChosenColorEffect(target)` — protection from chosen color. Must run inside `ChooseColorThen`; wrap in `ForEachInGroup` for the group case (Akroma's Blessing: "Creatures you control gain protection from the chosen color").
- `ChooseCreatureTypeEffect(...)` — pause for creature-type pick.
- `SelectTargetEffect(...)` — have a player pick from a valid set.

> **Authoring rule:** prefer composing primitives over adding parameters to an existing effect. Use `CompositeEffect`
> and the gather/select/move pipeline before writing a new executor.

---

## 5. Effect patterns (`Patterns.Library.*` / `Patterns.Hand.*` / `Patterns.Group.*` / `Patterns.Exile.*` / `Patterns.CreatureType.*` / `Patterns.Mechanic.*`)

Composed pipelines (`GatherCards → SelectFromCollection → MoveCollection` shapes and similar).
Named entries here are for named MTG mechanics and shapes with a demonstrated second user — a
one-off pipeline belongs inline in the card file via `Effects.Pipeline { }` (§5.5) instead.

**Library search & reveal**

- `searchLibrary(filter, destination?, tapped?, shuffle?)` — search library, pick matching, move, shuffle.
- `searchMultipleZones(filters, ...)` — search multiple zones in one effect.

**Top-deck manipulation**

- `scry(count)` — look at top N, bottom any, rest on top.
- `surveil(count)` — look at top N, any to graveyard, rest on top.
- `mill(count)` — top N cards into graveyard.
- `lookAtTopAndKeep(count, keepCount)` — Ancestral Memories — keep exactly K to hand.
- `lookAtTopAndReorder(count)` — reorder top N.

**Reveal patterns**

- `revealUntilNonlandDealDamage(target)` — Bonecrusher Giant shape.
- `wheelEffect(players)` — each player shuffles hand into library, draws that many.
- `factOrFiction(...)` — reveal 5, opponent splits into two piles, you choose one.

**Hand manipulation**

- `discardCards(count, target)` — controller-of-target chooses (mandatory).
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
- `forage(afterEffect?)` — Forage cost; choose card-from-hand to play.
- `loot(draw?, discard?)` — "draw N, discard M" loop.
- `rummage(count?)` — discard then draw.
- `connive(target?)` — draw 1, discard 1, then put a +1/+1 counter on `target` if the discard was a nonland (CR 702.166). Also exposed as `Effects.Connive(target)`.
- `readTheRunes()` — "draw X cards; for each, discard a card unless you sacrifice a permanent." Composes `RepeatDynamicTimesEffect(XValue, ChooseActionEffect(...))` with feasibility guards. Exposed as `Effects.ReadTheRunes()`.
- `eachOpponentMayPutFromHand(filter?)` — each opponent may dump a matching card.
- `putFromHand(filter?, count?, entersTapped?)` — you may put N from hand onto battlefield.
- `incubate(n)` — make an Incubator token with N counters.
- `returnLinkedExile(underOwnersControl?)` — bring back linked exile pile.
- `takeFromLinkedExile()` — pull one card from linked exile.
- `shuffleGraveyardIntoLibrary(target?)` — Elixir of Immortality shape.
- `reflexiveTrigger(action, whenYouDo, optional?)` — optional action; if taken, queue a reflexive trigger.

**Group bulk operations** (one effect applied to every permanent matching a `GroupFilter`)

- `modifyStatsForAll(power, toughness, filter, duration?)` — give every match +X/+Y (`Int` or `DynamicAmount`).
- `doublePowerAndToughnessForAll(filter, duration?)` — double each match's power and toughness. Resolves to a fixed +P/+T modification read per-entity from projected state via `DynamicAmount.EntityProperty(EntityReference.IterationEntity, …)`, so the bonus locks in at resolution (no re-doubling) and negative power doubles correctly. Roar of Endless Song, Unnatural Growth.
- `grantKeywordToAll(keyword, filter, duration?)` / `removeKeywordFromAll(...)`; `tapAll(filter)` / `untapGroup(filter?)`; `dealDamageToAll(amount, filter)`; `destroyAll(filter, noRegenerate?)`; `gainControlOfGroup(filter?, duration?)`.

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
- `EffectTarget.Self` — the source permanent.
- `EffectTarget.TriggeringEntity` — the entity that caused the trigger to fire.
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
  *that player* …").
- `Player.AnOpponent` — a genuinely non-targeted "an opponent" (a chooser: "an opponent chooses a
  creature type"). Currently resolves to the first opponent in turn order; the proper multiplayer
  choice flow is tracked in `backlog/multiplayer.md`. Do **not** use it where the text means
  `TargetOpponent`, `EachOpponent`, `DefendingPlayer`, or `TriggeringPlayer`.
- `Player.ChosenOpponent` — the opponent locked into the source's `ChoiceSlot.OPPONENT` slot.
- `Player.ControllerOf(desc)` / `Player.OwnerOf(desc)` — controller/owner of the first chosen target.
- `Player.ContextPlayer(i)` / `Player.Candidate` / `Player.Any` — positional target, CR 115
  candidate during target-restriction evaluation, and "a player" matching.
- `EffectTarget.ContextProperty(key)` — value plumbed into `EffectContext` (damage amount, life gained, blight
  amount, …).
- `EnchantedCreature` / `EquippedCreature` — resolve via `AttachedToComponent`; requires the state-aware
  `resolveTarget(state, target)` overload.
- `EnchantedPermanent` — same `AttachedToComponent` resolution as `EnchantedCreature`, but type-agnostic; use for
  Auras that enchant non-creature permanents (e.g. Wellspring enchants a land: "gain control of enchanted land").

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
- `Targets.Artifact` — any artifact.
- `Targets.Enchantment` — any enchantment.
- `Targets.Land` — any land.
- `Targets.BasicLand` — any basic land.
- `Targets.Spell` — any spell on the stack.
- `Targets.Card` — any card in any zone (e.g. graveyard).
- `Targets.CreatureOrPlaneswalker` — combined.
- `Targets.TappedCreature` / `UntappedCreature` — state-restricted.
- `Targets.InstantOrSorcery` — instant-or-sorcery card.

**Chained predicates** — `.youControl()`, `.controlledByOpponent()`, `.opponent()`, `.withSubtype(...)`,
`.withKeyword(...)`, `.ofColor(...)`, `.tapped()`, `.untapped()`, `.power(n)`, `.minPower(n)`, `.maxPower(n)`; plus
`TargetFilter.excludeSelf` to exclude the source.

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
- `optional = false` — when `true`, minimum becomes 0 ("up to N target ...").
- `unlimited = false` — when `true`, **"any number of target ..."** — no upper cap. The practical
  maximum is the number of legal targets, which the engine sends to the client; validation imposes
  no limit and the minimum is 0. Use this instead of a large placeholder `count` (Phyrexian Purge,
  Kaboom, Weaver of Lies). For "**X** target creatures" use `dynamicMaxCount = DynamicAmount.XValue`
  instead — that clamps the count to the chosen X.
- `dynamicMaxCount: DynamicAmount?` — evaluated when the spell/ability hits the stack; the resolved
  value becomes the max ("up to X target creatures", X = board state or chosen X).
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
- `.withChosenColor()` — `CardPredicate.HasChosenColor`: matches the color chosen during the current
  effect's resolution (read from `EffectContext.chosenColor`, set by `Effects.ChooseColorThen`). Use with
  `AggregateBattlefield(Player.Each, …)` for "for each permanent of that color" (Coalition Dragon cycle).
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
- `.named(name)` — `CardPredicate.NameEquals`: matches a fixed card name.
- `.namedFromVariable(variableName)` — `CardPredicate.NameEqualsChosen`: matches the card name stored in
  `chosenValues[variableName]` (case-insensitive). Set the name with `Effects.ChooseCardName` (player names it)
  or `Effects.StoreCardName` (captured from a chosen card). Fails closed in static/projection contexts. Used by
  the "name a card … cards with that name" family (Desperate Research, Lobotomy).
- `.power(n)` / `.minPower(n)` / `.maxPower(n)` — P/T comparator.
- `.manaValue(n)` / `.manaValueAtMost(n)` / `.manaValueAtLeast(n)` — mana-value comparator.
- `.manaValueAtMostX()` — mana value ≤ the X chosen for the source spell/ability.
- `.manaValueEqualsX()` — mana value **exactly equal** to the X chosen for the source spell/ability (the chosen
  number, or the X paid in an `{X}…` mana cost; resolution-time only — matches nothing without a chosen number).
  Available on both the object-filter builders and on `TargetFilter` (mirrors `.manaValueAtMostX()`). Used by Void
  (`Effects.ChooseNumberThen`) and Repeal (`{X}{U}` — return target nonland permanent with mana value X).
- `.manaValueAtMostEntity(ref)` — mana value ≤ a referenced entity's mana value (e.g. Kodama of the East Tree).
- `.powerGreaterThanEntity(ref)` — power strictly greater than a referenced entity's projected power. Used by
  Éowyn, Fearless Knight ("exile target creature an opponent controls with greater power") — combine
  with `EntityReference.Source` to express "greater power than the ability's source".
- `.powerAtMostEntity(ref)` — power ≤ a referenced entity's projected power. Inverse of
  `.powerGreaterThanEntity`; used by Old Man of the Sea ("target creature with power less than or equal
  to this creature's power") with `EntityReference.Source`.
- `.manaValueAtMostEntityManaSpent(ref)` — mana value ≤ the mana **actually spent** to cast a referenced
  entity. Reads the live `SpellOnStackComponent` buckets while the entity is still a spell, or the
  `CastRecordComponent` snapshot once it has resolved onto the battlefield (0 if it was never cast).
  Used by Edge of Eternities warp payoffs like Astelli Reclaimer ("…mana value X or less…, where X is the
  amount of mana spent to cast this creature") — X is 5 for `{3}{W}{W}`, 3 for warp `{2}{W}`, 0 for free.
- `.manaValueIsOdd()` / `.manaValueIsEven()` — mana-value parity (zero is even). Pair with modal
  spells whose modes ask the caster to choose a parity (e.g. *Mutinous Massacre*).
- `.toughnessAtMost(n)` / `.toughnessAtLeast(n)` — toughness comparator.
- `.toughnessAtMostX()` — toughness ≤ the X chosen for the source spell/ability. Resolves
  against `PredicateContext.xValue` at evaluation time, so it works at the spell's resolution
  filter pass (e.g. Zero Point Ballad's mass destruction). Layer projection / trigger matching
  / cost calculation report `false` (no X context).
- `.tapped()` / `.untapped()` — tap state.
- `.dealtDamageThisTurn()` — was dealt damage this turn (marked-damage *history*, not current marked
  damage); backed by `StatePredicate.WasDealtDamageThisTurn`. Survives damage removal / leaving combat;
  cleared at end-of-turn cleanup. For "...that was dealt damage this turn" (Rooftop Assassin, Unsparing
  Boltcaster). Also available on `TargetFilter` (`TargetFilter.Creature.dealtDamageThisTurn()`).
- `.saddled()` — permanent is saddled (CR 702.171b); backed by `StatePredicate.IsSaddled`.
- `.crewedOrSaddledSourceThisTurn()` — source-relative: creature crewed (CR 702.122) or saddled
  (CR 702.171) the effect's source permanent this turn; backed by
  `StatePredicate.CrewedOrSaddledSourceThisTurn` (see Object-state predicates). For
  "target/choose/return a creature that crewed/saddled it this turn".
- `.nontoken()` / `.token()` — token vs printed.
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

### `StatePredicate` — battlefield state checks

- `IsTapped` — currently tapped.
- `IsUntapped` — currently untapped.
- `IsAttacking` — declared as attacker this combat.
- `IsBlocking` — declared as blocker this combat.
- `InSameBandAsSource` (filter builder `inSameBandAsSource()`) — source-relative (CR 702.22):
  matches the effect's source creature itself and any creature sharing its combat band id.
  Resolves against `PredicateContext.sourceId`, so it only matches while that source is attacking
  (band membership exists only during combat). Used as the recipient filter of Camel's
  "prevent all damage Deserts would deal to this creature and to creatures banded with this
  creature". Note: it's only evaluated where the context carries a source entity — currently the
  recipient filter of a `PreventDamage` replacement (see §15); it's inert in group/projection,
  untap, and trigger-gating contexts.
- `CrewedOrSaddledSourceThisTurn` (filter builder `crewedOrSaddledSourceThisTurn()`) —
  source-relative (CR 702.122 / 702.171): matches a creature that crewed or saddled the effect's
  source permanent this turn (i.e. one tapped to pay that permanent's Crew/Saddle cost). Resolves
  against `PredicateContext.sourceId` by reading the source's `CrewSaddleContributorsComponent`;
  inert with no source context (group/projection, untap, trigger-gating). Used for Mount/Vehicle
  payoffs that target/choose/sacrifice/return "a creature that crewed/saddled it this turn" (Giant
  Beaver, Rambling Possum, The Gitrog, Calamity). For the *count* of those creatures use
  `DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn` instead.
- `IsFaceDown` — currently face-down.
- `HasCounter(type)` — has at least one counter of `type`.
- `AttachedToCardType(cardType)` — Aura/Equipment whose `AttachedToComponent` points to a
  permanent that currently has the given top-level [`CardType`] in its **projected** type
  set. Used by filters like "Aura attached to a land" (Pyramids) or "Equipment attached
  to a creature". Reads the attached-to permanent's projected types, so a land animated
  into a creature still matches `LAND` and additionally matches `CREATURE`. False for
  entities with no `AttachedToComponent`.
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

### `AffectsFilter` — static-ability target shapes

- `OtherCreaturesWithSubtype` — lord scope (other creatures of subtype).
- `CreaturesWithCounter` — creatures with at least one counter (Aurification).

> **Load-bearing rule:** filtering battlefield permanents by type/subtype/color/keyword/P-T MUST use
`predicateEvaluator.matchesWithProjection(state, projected, ...)`. Use `projected.isCreature(entityId)` rather than
`cardComponent.typeLine.isCreature`. Non-battlefield zones may read base state.

---

## 8. Triggered abilities (`Triggers.*`)

`triggeredAbility { trigger; effect; target?; triggerCondition?; optional?; checkOnNextState?; dealsDamageBeforeResolve?; controlledByTriggeringEntityController? }`.

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
- `PutIntoGraveyardFromBattlefield` — SELF, same event shape as `Dies`; rename
  clarifies non-creature intent (artifact / enchantment going to yard).
- `leavesBattlefield(filter, to?, excludeTo?, binding)` — factory. `to = GRAVEYARD`
  gives a "dies" variant scoped beyond the named constants (other tribal deaths,
  any-controller deaths); `excludeTo = GRAVEYARD` gives "leaves without dying"
  (Three Tree Scribe shape); leaving both null gives "leaves to any zone."

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
- `attacks(filter?, requires?, binding?)` — factory. Covers ANY-binding scopes,
  type-filtered scopes (creature-you-control, nontoken-creature-you-control),
  and attack-time predicates (alone, future Battalion-style count gates).

**Attacks (player-level)**

- `YouAttack` — when you declare attackers (player-level, ANY binding).
- `YouAttackWithFilter(filter)` — when you attack with ≥1 matching attacker.
- `CreaturesAttackYou` — defender side; fires once per `AttackersDeclaredEvent`,
  not per attacker. Excludes creatures attacking a planeswalker you control
  (CR 509.1b). Pair with `DynamicAmounts.creaturesAttackingYou()` for
  attacker-count payoffs (e.g., Orim's Prayer).

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
- `BlocksOrBecomesBlockedBy(filter)` — either direction, partner-filtered;
  sole consumer of `BlocksOrBecomesBlockedByEvent`. Prefer `blocks(attackerFilter=...)`
  when only the blocking direction should fire.
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

Examples:

```kotlin
// "Whenever this creature attacks alone"
Triggers.attacks(requires = setOf(AttackPredicate.Alone))

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
- `TakesDamage` — source is dealt damage by any source (SELF binding).
- `CreatureDealtDamageByThisDies` — Etali / Sengir / Soul Collector shape; only consumer of `CreatureDealtDamageBySourceDiesEvent`.

**Factories** (axes: `damageType` × `recipient` × `sourceFilter` × `binding` for outgoing; `source` × `binding` for incoming):

- `dealsDamage(damageType?, recipient?, sourceFilter?, binding?, requireExcess?)` — outgoing-damage trigger. Pick `DamageType.{Any,Combat,NonCombat}`, `RecipientFilter.{Any,AnyPlayer,AnyPlayerOrPlaneswalker,AnyCreature,…}`, an optional source `GameObjectFilter`, and `TriggerBinding.{SELF,ANY,ATTACHED}`. Covers "deals combat damage to a player or planeswalker", "creature you control deals combat damage to a player" (`binding = ANY` + `sourceFilter = Creature.youControl()`), "nontoken creature you control deals…" (`.nontoken()`), and "enchanted creature deals damage" (`binding = ATTACHED`). Pass `requireExcess = true` to fire only when the recipient was dealt damage past lethal (CR 120.4a) — Fall of Cair Andros' "is dealt excess noncombat damage". Read the excess via `DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT)`. **Combat caveat:** combat-damage state-based actions run *before* trigger detection, so a non-indestructible recipient that dies to the same combat-damage event has already left the battlefield when a `RecipientFilter.CreatureOpponentControls`-style filter reads its `ControllerComponent` — the filter silently fails (no last-known-info path yet). A `requireExcess = true` + `DamageType.Combat` trigger therefore only fires reliably on recipients that survive (indestructible / high toughness). Fall of Cair Andros is unaffected because it gates on `DamageType.NonCombat`, where the trigger is detected from the damage event before the kill SBA.
- `takesDamage(source?, binding?)` — incoming-damage trigger. Pick `SourceFilter.{Any,Creature,Spell,Combat,NonCombat,HasColor(c),…}` and `TriggerBinding.{SELF,ATTACHED}`. Covers "damaged by a creature/spell" and "enchanted creature is dealt damage" (`binding = ATTACHED`, Aurification / Frozen Solid shape).
- `becomesTapped(binding?, filter?)` — "becomes tapped" trigger. `BecomesTapped` is the SELF constant; pass `binding = TriggerBinding.ANY` with an optional `filter: GameObjectFilter` for "whenever a [filter] becomes tapped" (e.g. `GameObjectFilter.CreatureOrLand` — Temporal Distortion). The filter is matched against the tapped permanent via projected state.

### Phase & turn

Named sugar for the common `(step, player)` cases; reach for `phase(step, player?, binding?)`
for anything else (the ATTACHED-binding aura shapes, custom step/player combinations).

- `YourUpkeep` — start of your upkeep.
- `YourDrawStep` — start of your draw step.
- `EachUpkeep` — every upkeep.
- `EachOpponentUpkeep` — at each opponent's upkeep.
- `YourEndStep` — beginning of your end step.
- `EachEndStep` — beginning of each end step.
- `BeginCombat` — start of combat on your turn.
- `EachCombat` — beginning of each combat (any player's turn).
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

**Factory** — `discards(player?, cardFilter?)` — generic shape. `player = Player.Each`
matches any player; `cardFilter` narrows the fan-out to matching cards, so a batch that
discards a creature and two lands fires a `cardFilter = Creature` trigger once, not three
times. The cardFilter is evaluated against the **post-discard zone** (the cards are already
in the graveyard when the trigger matches) — safe for type/subtype/color predicates,
but a filter that depends on hand-specific state would read the wrong zone.

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

**Factory** — `youCastSpell(spellFilter?, requires: Set<SpellCastPredicate>)`. The
`requires` set is conjunctive — every predicate must hold for the trigger to fire.

**`SpellCastPredicate`** — extensible "facts about a cast." Adding a new cast-time mechanic
(was-copied, was-overloaded, paid-additional-life-cost, …) is one new sealed-case plus one
matcher branch — `SpellCastEvent` does not grow a new field per axis.

- `SpellCastPredicate.CastFromZone(zone)` — spell was cast from this zone. Used for Sunbird's
  Invocation (`Zone.HAND`), Goliath Daydreamer's instant/sorcery-from-hand trigger,
  Wildsear's enchantment-from-hand cascade.
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

// "Whenever you cast a noncreature or Otter spell"
Triggers.youCastSpell(
    spellFilter = GameObjectFilter.Noncreature or
                  GameObjectFilter.Any.withSubtype(Subtype("Otter")),
)
```

### State change & misc

- `TurnedFaceUp` — source turns face up. Use `turnedFaceUp(binding)` for the ATTACHED-binding aura variant (Fatal Mutation).
- `CreatureTurnedFaceUp(player?)` — when a creature you control turns face up.
- `GainControlOfSelf` — you gain control of source.
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
- `Valiant` — Bloomburrow Valiant trigger.
- `RoomFullyUnlocked` — Rooms — both doors unlocked.
- `OnDoorUnlocked` — single Room door unlocked.

### Life

- `YouGainLife` — you gain any life.
- `AnyPlayerGainsLife` — anyone gains life.
- `YouLoseLife` — you lose any life.
- `AnyPlayerLosesLife` — anyone loses life.
- `YouGainOrLoseLife` — combined life-change.

### The Ring

- `RingTemptsYou` — whenever the Ring tempts you (CR 701.54d). Paired with `Effects.TheRingTemptsYou()`.

### Scry

- `WheneverYouScry` — fires once per scry resolution (CR 701.18), after the cards have
  been placed on top/bottom. Pair with `DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_SCRY_COUNT)`
  for "for each card looked at" payoffs (Celeborn the Wise, Elrond Master of Healing).
  Automatically emitted by `Patterns.Library.scry(N)`; no card has to opt in.

### Sacrifice & counters

- `YouSacrificeOneOrMore(filter?)` — you sac ≥1 matching.
- `Sacrificed` — source is sacrificed.
- `PlusOneCountersPlacedOnYourCreature` — Hardened Scales shape (+1/+1 only).
- `countersPlacedOn(filter = Creature.youControl(), counterType = Counters.ANY, firstTimeEachTurn = true)`
  — fires when counters of any type (`Counters.ANY` wildcard) land on a matching permanent;
  `firstTimeEachTurn` gates it to the first counter placement on *that* permanent this turn
  (engine-tracked via `ReceivedCountersThisTurnComponent`). Triggering permanent is
  `EffectTarget.TriggeringEntity`. Stalwart Successor shape.
- `OneOrMorePermanentsEnter(filter?)` — batched ETB trigger.
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
  - `targetRequirement = <TargetRequirement>` — a target chosen **each time** the delayed trigger
    fires, exposed to `effect` as `EffectTarget.ContextTarget`. Use for delayed triggers whose payoff
    targets: Rediscover the Way chapter III installs
    `CreateDelayedTriggerEffect(trigger = Triggers.YouCastNoncreature, fireOnce = false,
    expiry = EndOfTurn, targetRequirement = Targets.CreatureYouControl,
    effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE))` — "whenever you cast a noncreature spell
    this turn, target creature you control gains double strike". Works on both event-based and
    step-based delayed triggers; null (default) for non-targeting delayed triggers.

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
- `GrantKeywordByCounter` — Aurification — keyword based on counters present.
- `AddCreatureTypeByCounter` — subtype based on counters present.
- `SetEnchantedLandType(landType)` — "Enchanted land is an Island" — replaces the enchanted
  land's basic land types with a fixed type (Rule 305.7). (Sea's Claim)
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
- `GrantHexproofFromMonocoloredToGroup(filter = attachedCreature())` — "[filter] have hexproof from
  monocolored" — adds the projected keyword `HEXPROOF_FROM_MONOCOLORED`, which blocks targeting by
  monocolored (exactly one color, CR 105.2) spells and abilities opponents control. Colorless and
  multicolored sources are unaffected; the controller can still target their own creatures. (Dragonfire
  Blade)
- `GrantCardType(cardType, filter)` / `RemoveCardType(cardType, filter)` — Layer 4 type-changing statics that add or
  remove a card type (e.g. `"CREATURE"`). `RemoveCardType` backs Impending's "isn't a creature while it has a time
  counter" (wrapped in a `ConditionalStaticAbility`); reuse it for any "it's no longer a [type]" effect.
- `ConditionalStaticAbility` — static gated by a runtime `Condition`.
- `CantReceiveCounters(filter)` — matching permanents can't have counters put on them (projects the
  `AbilityFlag.CANT_RECEIVE_COUNTERS` flag).
- `CantBeSacrificed(filter)` — matching permanents can't be sacrificed (projects the
  `AbilityFlag.CANT_BE_SACRIFICED` flag, honored by the sacrifice executor — a sacrifice that can't
  happen simply no-ops). Wrap in `ConditionalStaticAbility` for time-restricted forms, e.g. Zurgo,
  Thunder's Decree: `ConditionalStaticAbility(CantBeSacrificed(GroupFilter(Token.youControl().withSubtype("Warrior"))), IsInStep(listOf(Step.END)))`.
- `CantBlockCreaturesWithGreaterPower(filter = source())` — blocker-side evasion (Spitfire Handler): this
  creature can't block creatures whose projected power exceeds its own.
- `CantBeBlockedByCreaturesWithLessPower(filter = source())` — attacker-side dual (Formation Breaker): this
  creature can't be blocked by creatures whose projected power is less than its own. Resolved by
  `CantBeBlockedByCreaturesWithLessPowerRule`; both sides use projected power, so a P/T buff raises the
  threshold.
- `Effects.CreatePermanentEmblem(...)` — emblem with static abilities (planeswalker ultimates).
- `AttackTax(amountPerAttacker: DynamicAmount)` — Propaganda / Ghostly Prison / Windborn Muse /
  Collective Restraint. Per-attacker generic-mana tax for attacking the source's controller; the
  amount is a `DynamicAmount` so it can scale with state (e.g., `DynamicAmounts.domain()` for
  "{X} where X is your domain"). Evaluated with the source permanent's controller as "you".
  When `totalTax > 0`, the engine pauses `DeclareAttackers` for a `YesNoDecision` *before* tapping
  any mana — declining is a clean no-op that leaves the player in `DECLARE_ATTACKERS` to re-declare.
  The same prompt/cancel pattern applies to block-tax floating effects (e.g. Whipgrass Entangler)
  via `AttackBlockTaxPerCreatureType`.
- `CantBeAttackedWithout(keyword, attackerFilter = null)` — Form of the Dragon-style "Creatures
  without flying can't attack you." defender-side restriction. Optional `attackerFilter` narrows
  which attackers are restricted (evaluated with the source permanent as predicate source, so
  chosen-color/subtype predicates resolve against it) — e.g. Teferi's Moat:
  `CantBeAttackedWithout(Keyword.FLYING, GameObjectFilter.Creature.sharingChosenColorWithSource())`.
- `CantAttackUnlessCoAttacker(coAttackerFilter, filter = source)` — "This creature can't attack
  unless [a creature matching coAttackerFilter] also attacks" (Scarred Puma). Unlike
  `CantAttackUnless` (which is defender-relative), this depends on the whole proposed attacker
  group, so it's validated against the other declared attackers at declaration time (projected
  state; self never counts as its own co-attacker).
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
- `AdditionalSourceTriggers(sourceFilter, excludeSelf = true)` — Twinflame Travelers: all triggered
  abilities of permanents matching `sourceFilter` you control trigger an additional time (not just ETB).
  `TriggerDetector.duplicateSourceTriggers`.
- `AdditionalAttackTriggers(attackerFilter = GameObjectFilter.Any)` — Windcrag Siege (Mardu): the
  attack-cause analogue of `AdditionalETBOrLTBTriggers`. If a creature matching `attackerFilter`
  being declared as an attacker causes an attack-related triggered ability ("whenever a creature
  attacks" / "whenever you attack") of a permanent you control to trigger, that ability triggers an
  additional time. `TriggerDetector.duplicateAttackTriggers`; additive across copies.

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
  `OpponentsCastTargeting(GroupFilter)`, `FaceDownYouCast`, `MorphActivation`.
- `modification: CostModification` — `ReduceGeneric(amount)`, `ReduceGenericBy(source)`,
  `ReduceColored(symbols)`, `ReduceColoredPerUnit(symbols, source)`, `IncreaseGeneric(amount)`,
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
  same, all players), `CardsInGraveyardMatchingFilter`, `FixedIfAnyTargetMatches`, … — see
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

**Global denial statics** (no `filter`/`duration` block — they're singleton-style)

- `PreventCycling` — "Players can't cycle cards." (Stabilizer)
- `PreventActivatedAbilities(filter)` — activated abilities (mana + non-mana) of matching
  permanents can't be activated; loyalty abilities and animation costs that haven't yet
  produced a creature are unaffected. (Cursed Totem → `GameObjectFilter.Creature`)
- `PreventManaPoolEmptying` — mana pools don't empty between steps/phases. (Upwelling)
- `NoMaximumHandSize` — controller has no hand-size limit. (Thought Vessel)
- `DampLandManaProduction` — a land tapped for 2+ mana produces `{C}` instead. (Damping Sphere)
- `RestrictSpellsCastPerTurn(maxPerTurn)` — the controller can't cast more than `maxPerTurn`
  spell(s) each turn. Per-controller; the most restrictive applies when several are in play.
  Already-cast spells count, even those cast before this permanent entered. (Yawgmoth's Agenda)
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
  = IsYourTurn)`; Grand Abolisher's cast clause = `PlayersCantCastSpells(Player.EachOpponent)`; Void
  Winnower = `PlayersCantCastSpells(Player.EachOpponent, spellFilter = GameObjectFilter(cardPredicates
  = listOf(CardPredicate.ManaValueIsEven)))`.

**Tapped-for-mana mana statics** (extra mana / replaced mana when a land is tapped for mana — resolve
inline as triggered mana abilities, off the stack per CR 605). These fire on the *manual* mana-ability
path; automatic cost payment adds the extra/replacement *mana* via the solver but skips non-mana
riders, matching how the engine already treats e.g. City of Brass's damage during auto-pay.

- `AdditionalManaOnTap(color, amount, anyColor = false)` — aura: "Whenever enchanted land is tapped
  for mana, its controller adds additional mana." `color = null` reads the aura's `ChosenColorComponent`;
  `anyColor = true` makes it one mana of **any color the controller chooses** each tap (prompts on a
  manual tap; flexible for the solver). (Elvish Guidance = fixed `{G}`; **Fertile Ground** = `anyColor`)
- `AdditionalManaOnSourceTap(sourceFilter, color = null, amount = 1, rider = null)` — global: "Whenever
  a `<sourceFilter>` is tapped for mana, that player adds …". `color = null` mirrors the produced color.
  `rider` is an optional non-mana `Effect` resolved inline, controlled by the tapping player
  (`EffectTarget.Controller` = tapper, `EffectTarget.Self` = the static's source). (Lavaleaper = basic-land
  mirror; Badgermole Cub = `+{G}`; **Overabundance** = `GameObjectFilter.Land` mirror + `DealDamage(1,
  Controller)` rider)
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
- `MayCastFromGraveyard(filter, lifeCost = 0, duringYourTurnOnly = false)` — cast spells matching
  `filter` from your graveyard following normal timing, optionally paying `lifeCost` life. Free for
  Yawgmoth's Agenda (`MayCastFromGraveyard(Nonland)`); `lifeCost = 1, duringYourTurnOnly = true` for
  Festival of Embers. Pair with `MayPlayLandsFromGraveyard` for "play lands and cast spells from
  your graveyard". Lands are *played*, not cast, so they need the lands permission separately.
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
- `MayCastWithoutPayingManaCost(controllerOnly = false, firstSpellOfTurnOnly = false, spellFilter = Any)` — a
  battlefield permission to cast a spell without paying its mana cost (CR 118.9). Composable
  gates: `controllerOnly = true` restricts the benefit to the source's controller ("you" wording);
  `firstSpellOfTurnOnly = true` requires the caster to be the active player and to have cast
  zero spells this turn; `spellFilter` restricts *which* spells may be cast for free (card
  predicates, matched in any zone — default `GameObjectFilter.Any` = every spell). Weftwalking is
  `MayCastWithoutPayingManaCost(firstSpellOfTurnOnly = true)`; Dracogenesis is
  `MayCastWithoutPayingManaCost(controllerOnly = true, spellFilter = GameObjectFilter.Any.withSubtype("Dragon"))`
  ("You may cast Dragon spells without paying their mana costs"); a future "you may cast the first
  spell you cast each turn …" composes via both gates true. The filter is enforced per-spell in
  `CostCalculator.hasFreeCastPermission(state, casterId, spellCardDef)` (the enumerator threads the
  card being cast through `EnumerationContext.freeCastPermissionFor(cardId)`).
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
  spells matching `spellFilter` are castable (lands always playable). (Glarb, Calamity's Augur =
  `GameObjectFilter.Any.manaValueAtLeast(4)`)
- `CastSpellTypesFromTopOfLibrary(filter)` — cast only matching spell types from the top; no land
  play, no full public reveal. (Precognition Field = instants/sorceries)
- `LookAtTopOfLibrary` — *private*: the controller may look at their own top card any time (revealed
  only to them, not opponents). (Lens of Clarity, Vizier of the Menagerie)
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

**`TimingRule`**

- `Normal` — at instant speed (default for most abilities).
- `ManaAbility` — resolves immediately, doesn't use the stack (CR 605).
- `SorcerySpeed` — only during your main phase, empty stack.
- `OnlyIfCondition(c)` — guarded by a runtime condition.

**`ActivationRestriction`**

- `MaxPerTurn(n)` — at most N activations per turn.
- `OnlyOnce` — once per game.
- `OnlyIfCondition(c)` — condition gate.

**Loyalty abilities**

- `loyaltyAbility(+N) { ... }` — add loyalty + effect.
- `loyaltyAbility(-N) { ... }` — remove loyalty + effect.
- `loyaltyAbility(0) { ... }` — 0-loyalty ability.

---

## 11. Keywords

> **Where set-mechanic helpers live.** The `card { … }` keyword helpers below for *set-specific*
> mechanics — `leyline()`, `flurry { }`, `mobilize(…)`, `firebending(n)`, `sneak(cost)`, `decayed()`,
> `vividEtb { }` / `vividCostReduction()`, `impending(time, cost)`, `renew(cost) { }`,
> `craft(filter, cost)`, `station()` — are `CardBuilder` **extension functions** in
> `mtg-sdk/.../dsl/mechanics/` (one file per mechanic), not methods on the core `CardBuilder`. They
> stay in package `com.wingedsheep.sdk.dsl`, so the call syntax is unchanged, but a card file that
> uses one needs the matching import (e.g. `import com.wingedsheep.sdk.dsl.station`). Evergreen /
> multi-set parameterized keywords (`prowess()`, `rampage(n)`, `keywordAbility(…)`) remain on the core
> builder. New set mechanics get an extension file in `dsl/mechanics/`.

**`Keyword` enum (display-level)**

Flying, Menace, Intimidate, Fear, Shadow, Horsemanship, all basic landwalks (Plainswalk … Forestwalk), Desertwalk
(nonbasic landwalk variant — `Keyword.DESERTWALK`, keyed off `Subtype.DESERT`), First Strike, Double
Strike, Trample, Deathtouch, Lifelink, Vigilance, Reach, Provoke, Flanking, Defender, Indestructible, Hexproof, Shroud, Haste,
Flash, Prowess, Flurry, Changeling, Convoke, Delve, Affinity, Storm, Flashback, Harmonize, Evoke, Sneak, Impending, Conspire, Hideaway, Cascade, Plot,
Offspring, Persist, Ascend, Wither, Toxic, Eerie, Vivid, Fateful Bite, … (display-only — engine effect lives in handlers or
composite abilities).

**Parameterized `KeywordAbility.*`**

- `Ward(amount)` — opponent pays a mana cost to target this (CR 702.21). Non-mana costs use
  `KeywordAbility.Ward(WardCost.X)`: `WardCost.Mana`, `WardCost.Life(n)`, `WardCost.Discard(n, random)`,
  and `WardCost.Sacrifice(filter)` ("Ward—Sacrifice a Food", Ygra). For sacrifice ward, the opponent
  chooses which matching permanent(s) they control to sacrifice (declining counters their spell); valid
  fodder is matched against projected state, so subtypes granted by continuous effects count.
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
- `Afflict(n)` — defender loses N when this becomes blocked.
- `Crew(n)` — tap N power worth to animate a Vehicle.
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
- `Toxic(n)` — adds poison counters on combat damage.
- `Cycling(cost)` — pay cost, discard, draw a card.
- `BasicLandcycling(cost)` — cycling that fetches a basic land type.
- `Typecycling(type, cost)` — cycling that fetches a card type.
- `Plot(cost)` — `KeywordAbility.plot(cost)`. Special action available during your main phase while the stack is empty: pay [cost] and exile the card from your hand. It becomes plotted (stamped with a `PlottedComponent`). On a later turn you may cast it from exile without paying its mana cost, as a sorcery (CR 718). Cast permission is granted via the engine's standard `MayPlayPermission` + `PlayWithoutPayingCostComponent`, gated by `Conditions.SourcePlottedOnPriorTurn`. No card-side wiring needed — declare the keyword ability on the card and the engine handles the rest.
- `Hideaway(n)` — `KeywordAbility.hideaway(n)`; display tag rendered "Hideaway N". Mechanic is composed manually via `MoveCollectionEffect(faceDown = true, linkToSource = true)` + `CardSource.FromLinkedExile()` — the keyword itself carries no engine behavior.
- `Harmonize(cost)` — `KeywordAbility.harmonize(cost)` (Tarkir: Dragonstorm). An alternative cost to cast an instant/sorcery **from your graveyard**, like Flashback, then exile it as it resolves. As you cast it you may tap **a single** untapped creature you control to reduce the **generic** portion of the harmonize cost by that creature's (projected) power — a Convoke-style reduction, but one creature paying generic-equal-to-power instead of one mana per creature. No card-side wiring: declare the keyword ability and the engine handles graveyard-cast enumeration (`CastWithHarmonize`), the per-creature reduction (routed through `AlternativePaymentChoice.harmonizeCreature`), and the exile-on-resolution. The chosen creature and its power are surfaced to the client via `LegalAction.harmonizeCreatures` / `hasHarmonize`; the client offers an on-battlefield single-creature tap step (the `harmonize` pipeline phase + `HarmonizeSelector` HUD, mirroring Convoke). **Harmonize {X}** (e.g. Nature's Rhythm `{X}{G}{G}{G}{G}`): the `CastWithHarmonize` action surfaces `hasXCost`/`maxAffordableX` (max X folds in the best single-creature tap reduction) so the client prompts for X. {X} is generic mana, so the tap reduces the mana paid *for X* — `CastSpellHandler.harmonizePaymentXValue` lowers the X mana once `reduceGeneric` has consumed any printed generic — while the chosen X stamped onto `SpellOnStackComponent.xValue` (and read by the effect, e.g. "mana value X or less") is unchanged. Colored pips are never reduced. **Granting harmonize at runtime:** harmonize can also be granted to a graveyard card that doesn't print it via `Effects.GrantHarmonize(target, cost?, duration)` (Songcrafter Mage). The grant is a `GrantedKeywordAbility` record keyed to the card entity; every harmonize read site consults printed-**or**-granted harmonize through the `HarmonizeGrants.effectiveHarmonize` resolver, so a granted harmonize is castable, reducible, and exiled exactly like a printed one. The grant survives the graveyard → stack move (so exile-on-resolution still fires) and is cleared in the cleanup step.
- **Waterbend** (Avatar: The Last Airbender) — *not a keyword ability*; a cost flag on an activated ability. Set `hasWaterbend = true` in the `activatedAbility { }` block (alongside a `cost = Costs.Mana("{N}")`). It means "Waterbend {N}: pay {N}, but for each generic mana in that cost you may tap an untapped **artifact or creature** you control instead." It is Convoke widened to artifacts and restricted to generic-only payment — a tapped permanent never covers a colored pip, and the number of taps is bounded by the generic mana in the cost (CR; you can tap a permanent that just came under your control, no summoning-sickness gate). Routed through `AlternativePaymentChoice.waterbendPermanents` (a `Set<EntityId>`), mirroring `hasConvoke`: the activated-ability handler applies it via `AlternativePaymentHandler.applyWaterbendForAbility`, the enumerator surfaces `LegalAction.hasWaterbend` / `waterbendPermanents` (via `CostEnumerationUtils.findWaterbendPermanents` + `canAffordWithWaterbend`), and the client offers an on-battlefield tap step (the `waterbend` pipeline phase + `WaterbendSelector` HUD, generic-only). The ability's `description` auto-prefixes "Waterbend " before the cost. *(Spell-level waterbend additional costs and in-resolution "may pay a waterbend cost" shapes are not yet wired — the `waterbendPermanents` carrier is reusable for them.)*
- `OptionalAdditionalCost(manaCost?, additionalCost?, multi, displayPrefix, branchesEffect, grantsFlashTiming)` — generalised "pay an optional extra cost while casting" primitive. Backs printed Kicker / Multikicker / Offspring **and** the pre-kicker "pay {N} more to cast as though it had flash" pattern (Ghitu Fire). When `branchesEffect = true` (default) paying the cost marks the spell so `WasKicked` fires for the card's own effect/triggers; when `false` the payment is invisible to `WasKicked` (used by `flashKicker`). When `grantsFlashTiming = true` paying the cost unlocks instant-speed casting in addition to whatever else it does — the optional cost may be mana (Ghitu Fire: `KeywordAbility.flashKicker("{2}")`) **or** a non-mana `additionalCost` such as Behold (Molten Exhale: "you may cast this as though it had flash if you behold a Dragon", `KeywordAbility.flashKicker(Costs.additional.Behold(filter = Filters.WithSubtype("Dragon")))`). Prefer the factories: `KeywordAbility.kicker(cost)`, `KeywordAbility.kicker(additionalCost)`, `KeywordAbility.multikicker(cost)`, `KeywordAbility.offspring(cost)`, `KeywordAbility.flashKicker(cost)`, `KeywordAbility.flashKicker(additionalCost)`. Serial name is `Kicker` for wire compatibility. **Kicker {X}** (variable kicker, e.g. `KeywordAbility.kicker("{X}")` on Verdeloth the Ancient): the kicked cast surfaces `hasXCost`/`maxAffordableX` so the client prompts for X exactly like a base-cost X spell; the chosen X is paid as part of the kicker and stamped onto `SpellOnStackComponent.xValue`, so the card's ETB trigger reads it via `DynamicAmount.XValue` ("create X tokens").
- `Impending(time, cost)` — `card { impending(n, cost) }` builder helper (CR 702.175, Duskmourn). A self-alternative
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
- `Craft(filter, cost)` — `card { craft(filter, cost) }` builder helper (CR 702.167, The Lost Caverns of
  Ixalan). On the front face of a transforming DFC: "Craft with [filter] [cost] ([cost], Exile this permanent,
  Exile [filter] you control and/or [filter] cards from your graveyard: Return this card to the battlefield
  transformed under its owner's control. Activate only as a sorcery.)" Composes entirely from existing primitives
  — `AbilityCost.Composite(Mana(cost), AbilityCost.Craft(filter))` (the atomic `Craft` sub-cost handles both the
  self-exile and the materials-exile because CR 702.167a defines them as one paired clause), plus
  `Effects.ReturnSelfFromExileTransformed` as the resolution effect, and `timing = TimingRule.SorcerySpeed`.
  Records the exiled materials on the source's `CraftedFromExiledComponent` so the back face's CDA
  ("Mastercraft Raptor's power is equal to the total power of the exiled cards used to craft it", CR 702.167c)
  can read them via `DynamicAmount.CraftedMaterialsTotalPower`. Declares `Keyword.CRAFT` for display.

  Material selection: the engine surfaces the combined BF + GY candidate pool on each Craft activation as
  `AdditionalCostData.validCraftMaterials` / `craftMinCount`. The web client renders both zones side-by-side
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
  and free-first-equip permissions below.
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

The entity role fixes *when* the condition can be answered: `Self` and the enchanted/equipped
attachment roles evaluate in both resolution and static-ability projection; `ContextTarget` and
`TriggeringEntity` are resolution-only (false under projection). Use `Conditions.EntityMatches`
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

- `YouControl(filter)` — you control ≥1 matching permanent.
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
- `OpponentControlsLandType(type)` — opponent controls land of a type.
- `DifferentCounterKindsAtLeast(count, filter = Creature)` — true when `count` or more *different
  kinds* of counters are among permanents you control matching `filter` (default: creatures). A
  +1/+1 and a finality counter is two kinds; the same kind on several permanents counts once.
  Board-derived only, so it gates a `ConditionalStaticAbility` (evaluates identically in resolution
  and projection). Desugars to `Compare(AggregateBattlefield(You, filter, DISTINCT_COUNTER_TYPES),
  GTE, count)`. Used by Hundred-Battle Veteran ("three or more different kinds of counters among
  creatures you control").
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

### Life & damage

- `LifeAtLeast(n, player?)` — player has ≥N life.
- `LifeAtMost(n, player?)` — player has ≤N life.
- `APlayerLifeAtMost(n)` — *some* player in the game has ≤N life (existential over `state.turnOrder`; distinct from `LifeAtMost`, which is `Player.You`). Used by enters-tapped-unless lands like Razortrap Gorge.
- `YouLostLife` — you lost life this turn.
- `OpponentLostLife` — an opponent lost life this turn.

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
  stamped as it entered (`HAND` → `CastFromHandComponent`, `GRAVEYARD` → `CastFromGraveyardComponent`),
  so it can gate an entering permanent's own replacement effect (Hundred-Battle Veteran: "enters with
  a finality counter if cast from your graveyard").
- `WasKicked` — cast with kicker / multikicker / offspring (i.e. an `OptionalAdditionalCost` with `branchesEffect = true` whose extra cost was paid). FlashKicker payments are intentionally invisible to this condition.
- `SneakCostWasPaid` — the source was cast for its `Sneak` cost (CR 702.190 — mana + returning an unblocked attacker). Reads the durable `ChoiceSlot.SNEAK` flag on a resolved permanent, falling back to the resolution context for a non-permanent spell's own effect. Backs riders like Leonardo, Leader in Blue and The Last Ronin's Technique.
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
- `BlightWasPaid(amount)` — the Blight X additional cost was paid.

### Source state

All "source matches X" conditions desugar to `Conditions.SourceMatches(filter)`, the facade over
`EntityMatches(EffectTarget.Self, filter)` — a generic predicate check against the source entity
that works in both resolution and static-ability (projection) contexts.

- `SourceMatches(filter)` — source entity matches a `GameObjectFilter`
  (`EntityMatches(EffectTarget.Self, filter)`).
- `SourceIsAttacking` — source is attacking.
- `SourceIsBlocking` — source is blocking.
- `SourceIsTapped` — source is tapped.
- `SourceIsUntapped` — source is untapped.
- `SourceEnteredThisTurn` — source entered the battlefield this turn.
- `SourceIsSaddled` — source is saddled (CR 702.171b). Gates Mount payoffs on "while saddled" /
  "as long as it's saddled"; evaluates identically at resolution and during projection.
- `SourceAttackedThisTurn` — source was declared as an attacker at least once during the
  current turn (per-creature, derived from the controller's `PlayerAttackersThisTurnComponent`).
  Negate via `Conditions.Not(...)` for Erg Raiders-style "if it didn't attack this turn".
- `SourceHasDealtDamage` — source has dealt damage since entering the battlefield.
- `SourceHasDealtCombatDamageToPlayer` — saboteur-style payoff gate.
- `SourceIsModified` — has counters, attached Equipment, or controller-owned Aura
  attached (CR 700.4). Kept as a dedicated condition because the controller-of-Aura
  match isn't expressible via the generic `EntityMatches` filter machinery.
- `SourceHasSubtype(subtype)` — `SourceMatches(GameObjectFilter.Any.withSubtype(...))`;
  Changeling is honored.
- `SourceHasKeyword(keyword)` — `SourceMatches(GameObjectFilter.Any.withKeyword(...))`.
- `SourceHasCounter(counterType)` — `SourceMatches(GameObjectFilter.Any` with the
  corresponding `StatePredicate.HasCounter` / `HasAnyCounter`).
- `SourceCounterCountAtLeast(counterType, count)` — the threshold form of `SourceHasCounter`: the source has `count`+
  counters of `counterType` (a `Compare` on `EntityProperty(Source, CounterCount(Named(type)))`). This is the gate
  behind a Station card's `{N+}` symbol (CR 721.2a — "As long as this permanent has N or more charge counters on it,
  it has [abilities]"): use it as the `condition` of a `staticAbility { }` or inside
  `ActivationRestriction.OnlyIfCondition(...)`, with `Counters.CHARGE`. Generic over counter type, reads counters live.

### Turn / phase

- `IsYourTurn` — it's your turn.
- `IsNotYourTurn` — it's an opponent's turn.
- `IsInPhase(phase)` — currently in `BEGINNING | MAIN | COMBAT | …`.
- `IsInStep(steps, yoursOnly = true)` — current step is one of `steps` (e.g. `Step.END`). Board-derived
  (reads `state.step` + active player), so it evaluates identically at resolution and under projection,
  making it usable as a `ConditionalStaticAbility` gate. `yoursOnly` requires it to be the controller's
  turn ("during your end step"). Used by Zurgo, Thunder's Decree.
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
- `YouCastSpellsThisTurn(atLeast, filter, fromZone?)` — Prowess/Magecraft shape. Backed by
  `PlayerCastSpellsThisTurn(Player.You, filter, atLeast, fromZone)`. `fromZone` (default any) restricts
  the count to spells cast from that zone, matched independently of `filter` (a face-down/morph spell
  cast from hand still counts, CR 708.2). With `fromZone = Zone.HAND`, negating gives the Prairie Dog
  cycle's "you haven't cast a spell from your hand this turn":
  `Not(YouCastSpellsThisTurn(1, fromZone = Zone.HAND))` (Inventive Wingsmith, Prairie Dog, Canyon Crab,
  Emergent Haunting, Wrangler of the Damned). The origin zone is captured on each `CastSpellRecord`
  (`castFromZone`) at cast time, so flashback/forage (GRAVEYARD), plot/foretell (EXILE), and commander
  (COMMAND) casts are all distinguished from hand casts.
- `TriggeringSpellMatches(filter)` — intervening-if guard: the spell that triggered this ability
  matches `filter`. Reads the triggering entity's static card characteristics (so it stays correct
  after the spell leaves the stack). General "whenever you cast a spell, if it's a/an X ..." gate.
  Backed by `EntityMatches(EffectTarget.TriggeringEntity, filter)`.
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
- `CreatureDiedThisTurn` — intervening-if "if a creature died this turn", **global** (any player's
  control; sums every player's `CreaturesDiedThisTurnComponent`).
- `ControlledCreatureDiedThisTurn` — intervening-if "if a creature died **under your control** this
  turn", scoped to the source's controller (reads only that player's `CreaturesDiedThisTurnComponent`).
  Used by Barrensteppe Siege (Mardu). Dual-mode.
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
(CR 601.2b) — color, creature type, land type, mode, chosen creature, kicked-ness, blight amount — all
ride one durable `CastChoicesComponent` on the stable entity (the immutable-ECS analogue of Forge's
SVar bag). `{X}` has its own dedicated reader (`DynamicAmount.CastX`); the other slots are read
generically via `DynamicAmount.CastChoice(slot)` (numeric), `CastChoiceMade(slot)` / `CastChoiceIs(slot,
value)` (conditions), or consumed directly by effects (e.g. `Effects.CreateTokenOfChosenColorAndType`).

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
- `Add(a, b)` — `a + b`.
- `Subtract(a, b)` — `a − b`.
- `Multiply(a, b)` — `a × b`.
- `Divide(a, b, roundUp?)` — division with rounding rule.
- `Min(a, b)` — minimum.
- `Max(a, b)` — maximum.
- `Absolute(a)` — `|a|`.

### Battlefield aggregation

- `AggregateBattlefield(player, filter, aggregation?, property?)` — aggregate over matching
  permanents. `aggregation` defaults to `COUNT`; other modes: `MAX`/`MIN`/`SUM` over a
  `property` (`POWER`/`TOUGHNESS`/`MANA_VALUE`), and the distinct-set counters
  `DISTINCT_TYPES`, `DISTINCT_COLORS`, `DISTINCT_NAMES`, `DISTINCT_BASIC_LAND_SUBTYPES`
  (Domain), and `DISTINCT_COUNTER_TYPES` (the number of different kinds of counters present
  across the group — same kind on several permanents counts once).
- `AggregateZone(player, zone, filter?, aggregation?)` — count cards in a zone.
- `CountPermanentsOfType(player, subtype)` — count by creature type.
- `CountCreaturesYouControl` — shorthand for "your creatures".

### Player & game

- `LifeTotal(player)` — current life total.
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
  - Pairs with the `YouCastSpellsThisTurn` **condition** (§ conditions) — that gates a yes/no
    threshold, this yields the count.
- `CraftedMaterialsTotalPower` — total printed power of the cards exiled to craft the source
  permanent (CR 702.167c). Reads the source's `CraftedFromExiledComponent`. Used for the
  `*`-power CDA on Mastercraft Raptor (Saheeli's Lattice back face). Evaluates to 0 when the
  source has no recorded materials.
- `CreaturesThatCrewedOrSaddledThisTurn` (facade `DynamicAmounts.creaturesThatCrewedOrSaddledThisTurn()`)
  — number of distinct creatures that crewed (CR 702.122) or saddled (CR 702.171) the source
  permanent this turn. Source-relative: reads the source's `CrewSaddleContributorsComponent` and
  returns its size. Retains contributors that have since left the battlefield, so the count
  includes creatures no longer present as the ability resolves (Luxurious Locomotive ruling) — a
  plain `Count` over `crewedOrSaddledSourceThisTurn()` can't express that. Evaluates to 0 with no
  source / no component. For *which* creatures (targeting/gathering) use the
  `CrewedOrSaddledSourceThisTurn` state predicate instead.

### Counters

- `CountersOnSource(type)` — counters of `type` on the source permanent.
- `LastKnownCountersOnSource(type)` — counters when source last existed (for dies-triggers).
- `CountersOnTarget(target, type)` — counters on a target permanent.
- `CountersOnContext(path, type)` — counters stored in an `EffectContext` path.

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
  (Surrounded by Orcs). Pipeline state is not threaded into predicate contexts, so the
  reference returns null in target filters — comparison-based targeting like Grishnákh's
  "with power ≤ the amassed Army's power" needs a separate predicate plumbing that the
  primitive doesn't yet provide.

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
  - `TRIGGER_SCRY_COUNT` — cards looked at by the scry that fired the trigger (Celeborn the
    Wise, Elrond Master of Healing). Equals the scry N parameter.
  - `TRIGGER_EXCESS_DAMAGE_AMOUNT` — damage past lethal in the trigger payload (CR 120.4a).
    Set from `DamageDealtEvent.excessAmount`; non-zero only for `DealsDamageEvent(requireExcess = true)`
    triggers — Fall of Cair Andros' "amass Orcs X, where X is the excess damage."
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
- `ManaRestriction.CardTypeSpellsOrAbilitiesOnly(cardType, allowSpells?, allowAbilities?)` —
  Steelswarm Operator shape.

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
- `LIFE_GAINED` — life gained this turn (Bre of Clan Stoutarm).
- `LIFE_LOST` — life lost this turn.
- `PLAYER_ATTACKED` — whether/how many times you attacked.
- `DEALT_COMBAT_DAMAGE` — combat damage dealt.
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
anthem + state-trigger condition: `Exists(Player.ChosenOpponent, Zone.BATTLEFIELD, …)`). Example — Phantasmal Terrain
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
- `RedirectDamage(redirectTo, appliesTo)` — redirect matching damage to another recipient. Now wired
  as a continuous static replacement (each source applies at most once per damage event). `redirectTo`
  supports `EffectTarget.ControllerOfDamageSource` (the controller of the damaging source),
  `Controller`/`Self` (the replacement's owner/controller), and `TargetController`. Harsh Judgment:
  redirect chosen-color instant/sorcery damage dealt to you back to the spell's controller.
- **DamageEvent filters (gap #7):** `EventPattern.DamageEvent(recipient, source, damageType, amount)`.
  `amount: AmountFilter` (`Any` / `AtMost(n)` / `AtLeast(n)` / `Exactly(n)`) gates on the would-be
  amount (Callous Giant: `AtMost(3)`). `source = SourceFilter.Matching(filter)` can carry relational
  predicates: `GameObjectFilter.sharingColorWithRecipient()` (`CardPredicate.SharesColorWithRecipient`,
  Well-Laid Plans — "another creature that shares a color") and `sharingChosenColorWithSource()`
  (`CardPredicate.SharesChosenColorWithSource`, reads the replacement source's `ChosenColorComponent`).
- `ReplacementEffect.EntersBattlefieldTappedUnless(condition)` — ETB tapped unless condition met.
- `ReplacementEffect.IfYouDoBranchEffect(...)` — branch on "if you do" replacement.
- `OnEnterRunEffect(effect)` — generic "as ~ enters the battlefield, run [effect]". The wrapped effect
  executes via the normal effect-executor pipeline at entry time (so `EffectTarget.Self` resolves to
  the entering permanent) and may pause for player input. Compose with atomic pausable effects like
  `Effects.MayRevealCardFromHand` to build SOI shadow lands or other "as ~ enters" choices.
  **Scope today:** only wired into the land-play path (`PlayLandHandler`). When the first non-land
  permanent needs this, also wire it into `StackResolver.enterPermanentOnBattlefield`.
- `EntersWithDevour(multiplier, sacrificeFilter, counterType, variant)` — Devour (CR 702.82) and its
  printed variants. As the permanent resolves from the stack, the controller is prompted to pick any
  number of their own permanents matching `sacrificeFilter`. Those permanents are sacrificed and the
  entering permanent gains `multiplier × count` counters of `counterType` (default `+1/+1`). Pair
  with `KeywordAbility.Devour(multiplier, sacrificeFilter, variant)` so the rules text renders. The
  `variant` parameter is a textual tag only — `""` for plain Devour, `"land"` for the EOE
  "Devour land N" wording. **Scope today:** only the stack-spell entry path is wired; reanimation and
  token entries skip Devour (which is fine for printed cards — Devour creatures all cost real mana to
  cast).
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
- `ModifyLifeGain(multiplier, modifier, appliesTo)` — modify life gain by a multiplicative *and/or* additive
  factor: `gained = (original * multiplier) + modifier`, clamped to ≥ 0. `appliesTo` is a `LifeGainEvent` whose
  `player` filter (default `Player.Each`) gates which players the replacement applies to. Used by Alhammarret's
  Archive (`multiplier = 2`), Leyline of Hope (`multiplier = 1, modifier = 1, player = Player.You`). Multiple
  instances stack (×s multiply, +s sum) — two Leylines of Hope add 2 to every life-gain event.
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
  `hourglass`, `hope`, `verse`, `influence`, `burden` — assorted printed counter kinds. (`hourglass`: Temporal Distortion
  — a permanent with one doesn't untap during its controller's untap step; model the restriction with
  `GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter(... .withCounter(Counters.HOURGLASS)))` so it stays
  projection-scoped.) (`hope` / `verse` / `influence` / `burden`: LTR — Dawn of a New Age / Lost Isle Calling /
  Palantír of Orthanc / The One Ring. Pure passive counters with no inherent rule; the cards that use them read the
  count via `DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.X))`.)
- `stun` — CR 122.1d, a built-in replacement: "If a permanent with a stun counter on it would become untapped,
  instead remove a stun counter from it." Engine-wired through `untapOrConsumeStun` (`rules-engine/core/UntapHelpers.kt`),
  which is invoked from the untap step (`BeginningPhaseManager`), from `TapUntapExecutor`'s untap branch, and from the
  sacrifice/pay continuation resumer. Adding stun counters is done by `AddCounters(Counters.STUN, n, target)`.
- **Keyword counters** (Rule 122.1b) — `flying`, `first strike`, `lifelink`, `indestructible`, `deathtouch`,
  `trample`, `hexproof`, `reach`. `StateProjector` grants the matching `Keyword` to any permanent carrying one (mapped in
  `KEYWORD_COUNTER_MAP`, re-applied after Layer 6 so "loses all abilities" can't wipe a counter-granted keyword).
  Add via `AddCounters(Counters.DEATHTOUCH, ...)` etc.; no static ability needed. (`reach`: Sagu Pummeler's renew
  payoff puts a reach counter on a creature.)
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
- `GatherCardsEffect(source, filter, into)` — pipeline gather from a zone into a named collection. `CardSource`
  variants include zones (`FromZone`, `FromMultipleZones`), battlefield queries (`BattlefieldMatching`,
  `ControlledPermanents`), linked exile (`FromLinkedExile`), tapped-as-cost (`TappedAsCost`), and the resolved
  spell/ability targets (`ChosenTargets`). The zone/library sources (`FromZone`, `FromMultipleZones`,
  `TopOfLibrary`) accept a multi-player `player` reference (`Player.Each`, `Player.ActivePlayerFirst`,
  `Player.EachOpponent`) and fan out across every relevant player's copy of the zone in a single gather —
  e.g. "all creature cards in each player's graveyard" (Bringer of the Last Gift). Pair with
  `MoveCollectionEffect(underOwnersControl = true)` to return each card to its owner.
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
  `OnePerColor(matchControllerPermanentColors?)`, `OnePerCardName`, `TotalManaValueAtMost(max)`,
  `OnePerBasicLandType`, and `MaxAffordablePayment(manaPerSelected, payer?)`. `OnePerBasicLandType` keeps at most one
  land of each basic land type (a kept land claims
  *every* basic type it has) and — unlike `OnePerColor`, where a colourless card is unconstrained — a land with no
  basic land type can't be kept at all (Global Ruin: "chooses a land of each basic land type, then sacrifices the
  rest"). Each restriction also exposes a boolean flag on `SelectCardsDecision` (`onePerBasicLandType`, …) so the UI
  can disable redundant picks. `MaxAffordablePayment` caps the selection at
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
  `card { adventure("Name") { spell { … } } }`.
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
- **Hideaway N** — `KeywordAbility.hideaway(n)` (display, "Hideaway N") + `MoveCollectionEffect(faceDown = true,
  linkToSource = true)` + `CardSource.FromLinkedExile()`; no special engine plumbing needed.
- **Ascend / City's Blessing** — `Keyword.ASCEND` + `Effects.GainCitysBlessing()` + `Conditions.YouHaveCitysBlessing` /
  `SourceProjectionCondition.ControllerHasCitysBlessing` + `PlayerCitysBlessingComponent`.
- **Siege (named-mode entry)** — `EntersWithChoice(ChoiceType.MODE, modeOptions = ...)` + `SourceChosenModeIs("id")`.
- **Morph** — `morph = "{2}{U}"` (top-level) + `morphFaceUpEffect` for "as it turns face up".
- **Warp** — `warp = "{1}{R}"`; alt-cost that exiles end of turn.
- **Evoke** — `evoke = "{U}"`; pay alt cost, sacrifice on ETB.
- **Sneak** — `sneak("{1}{U}")`; declare-blockers-step alt cost (pay mana + return an unblocked attacker you control to hand); a resolving permanent enters tapped and attacking the same defender. `Conditions.SneakCostWasPaid` reads the rider flag.
- **Earthbend** — `Effects.Earthbend` composes AnimateLand + GrantKeyword + AddCounters + granted self-triggers (no fake
  keyword).
- **Endure N** — `Effects.Endure(amount, target = EffectTarget.Self)` composes a `ModalEffect.chooseOne` of
  AddDynamicCounters (N +1/+1 counters on the enduring permanent) and a single N/N white Spirit `CreateTokenEffect`
  (no fake keyword — endure is always the effect of a triggered/activated ability, resolved at resolution time). `amount`
  is `DynamicAmount.Fixed` for "endure 2" or any dynamic value for "endure X" (e.g. Warden of the Grove reads
  `EntityProperty(Source, CounterCount(...))`); `target` defaults to `Self` ("it endures") but takes
  `EffectTarget.TriggeringEntity` when a card endures the creature that triggered it.
- **Forage** — `Patterns.Mechanic.forage`; cast-from-graveyard permissions need a branch in `CastSpellHandler.validate`.
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
  kicker, blight, sneak, `ChooseColorThen`/`ChooseColorForTarget`); `SourceChosenModeIs` ids
  must match a declared `modeOptions` id.
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
