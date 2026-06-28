# Teenage Mutant Ninja Turtles (TMT) - Mechanics

**Set:** Teenage Mutant Ninja Turtles (`tmt`) · Released 2026-03-06
**Card pool:** 190 non-basic cards.

Counts below are cards in the set that use the mechanic. A card may appear under
multiple entries (e.g. a creature with Flying + Sneak).

**Implementation progress (119 / 190).** Mechanics now exercised end-to-end:

Evergreen keywords — Flying, Vigilance, Trample, Haste, Flash, Deathtouch,
Menace, Reach (granted and printed), Indestructible, Ward, Equip, Double strike,
First strike, Hexproof (granted UEOT).

Returning / set-specific keywords — **Affinity** (`Krang, Master Mind`),
**Landfall** (`Weather Maker`), **Kicker** (`Stomped by the Foot`), basic-land-
cycling (`Jennika` Plainscycling, `Stockman, Mad Fly-entist` Islandcycling,
`Bebop, Warthog Warrior` Swampcycling, `Zog, Triceraton Castaway`
Mountaincycling, `Rocksteady, Crash Courser` Forestcycling).

Triggers — ETB, LTB, begin-of-combat, attack, deals-combat-damage, you-gain-
life, land-you-control-enters (Landfall), counter-placed-on-creature-you-control
(with `oncePerTurn`), spell-cast filtered by card type, "permanent leaves the
battlefield" (`Super Shredder`, ANY/OTHER binding via `ZoneChangeEvent`),
"another creature you control dies" (with EntityReference.Triggering toughness
read), filtered ETB ("a Ninja you control enters" auto-attach trigger), and
intervening-if conditions ("if you control an artifact").

Static abilities — Anthems via `GrantKeyword` over a `GroupFilter`
(four-keyword Krang Utrom Warlord anthem; Rhino/Boar/Turtle/Squirrel-style
tribal grants), `ConditionalStaticAbility` (`IsYourTurn` first-strike on Null
Group), `CantAttackUnless` with a `Compare` condition, `CantBeBlockedByMoreThan`
(self + tribal), `GrantTriggeredAbility` for granted equipped-creature triggers
(`Quintessential Katana`), `GrantTriggeredAbilityEffect` UEOT (`Pain 101`,
`Perigee Beckoner` pattern), `GrantDynamicStatsEffect` reading
`AggregateBattlefield` (`Improvised Arsenal`, `Krang, Master Mind`).

Costs and cost modification — Optional sacrifice as an additional cost
(`Stomped by the Foot` kicker), `Costs.SacrificeAnother` over an Or-predicate
filter (`Ice Cream Kitty`, `Metalhead`), Sacrifice-self for activated abilities,
`Costs.RemoveCounterFromSelf` (`Weather Maker`), `ActivationRestriction
.OncePerTurn` on activated abilities (`Shredder's Armor`), `ModifySpellCost`
with `CostReductionSource.FixedIfControlFilter` (`Saved by the Shell`).

Counters — `Counters.PLUS_ONE_PLUS_ONE`, `Counters.CHARGE` (`Weather Maker`),
`Counters.STUN` (`Utrom Scientists`), counter-count read via
`EntityNumericProperty.CounterCount` (`Savanti Romero`'s scaling draw).

Dynamic amounts — `DynamicAmount.Count` of permanents/cards-in-hand/cards-in-
graveyard, `DynamicAmount.Subtract`/`IfPositive` (Krang's draw-to-four ETB),
`DynamicAmount.AggregateBattlefield` (Improvised Arsenal),
`DynamicAmount.XValue` for X-cost token-makers (`Triceraton Commander`),
`DynamicAmount.EntityProperty(Triggering, Toughness)` (`South Wind Avatar`),
`ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT` (`April, Reporter of the Weird`).

Effect pipelines — `GatherCardsEffect → SelectFromCollectionEffect →
MoveCollectionEffect` (Cowabunga!, Casey Jones Jury-Rig), modal "Choose one"
(`Mouser Attack!`, `Shredder's Revenge`), `ConditionalEffect` with
`Conditions.TargetSpellManaValueAtMost` (`Tainted Treats`),
`RepeatDynamicTimesEffect` for X-token creation (`Sally Pride`),
`ForEachInGroupEffect` over `StatePredicate.EnteredThisTurn` (`Renet`),
`EffectPatterns.scry`, `EffectPatterns.loot`, `EffectPatterns.rummage`,
`EffectPatterns.putFromHand` for "may put a land from your hand"
(`Lessons from Life`).

Lands and mana — `EntersTapped` replacement, single-color mana abilities,
`AddAnyColorMana(1)` and `AddAnyColorMana(2)` (`Transdimensional Bovine`),
`AddAnyColorMana(DynamicAmount.EntityProperty(Power))` (`Mona Lisa, Science
Geek`), `AddColorlessMana(2)` (`Weather Maker`).

Tokens — Standard Food, 1/1 colorless Robot (used by 5+ cards), 1/1 black
Ninja (`Uneasy Alliance`), 2/2 red Mutant (Jennika, Sally Pride, Mighty
Mutanimals, Slash), X 2/2 white Dinosaur Soldier (`Triceraton Commander`),
`CreateTokenCopyOfSelf` for self-cloning Equipment (`Improvised Arsenal`).

Cross-card combinators landed since the previous progress note:
- **Alliance trigger** — composed via `Triggers.OtherCreatureEnters` for six
  Alliance cards (East Wind Avatar, EPF Point Squad, Mighty Mutanimals,
  Mutant Town Musicians, Raphael Tough Turtle, Slash Reptile Rampager). The
  display-only `Keyword.ALLIANCE` is still absent, so the rendered card text
  doesn't get the italic "Alliance —" prefix, but the trigger semantics are
  faithful.
- **Channel** — composed via `activatedAbility { activateFromZone = Zone.HAND }`
  paired with `Costs.Composite(Mana, DiscardSelf)` (Action News Crew). Same
  caveat: `Keyword.CHANNEL` is still absent from the display marker enum.
- **Delayed "at your next upkeep"** — `CreateDelayedTriggerEffect(step =
  Step.UPKEEP, fireOnlyOnControllersTurn = true)` (Casey Jones, Vigilante).
- **Optional bottom-of-library + conditional draw** — Gather → Select(ChooseUpTo
  1) → MoveCollection-to-library-bottom → ConditionalOnCollection(DrawCards)
  (Manhole Missile).
- **Choose one of N keywords UEOT** — `ModalEffect(countsAsModalSpell = false)`
  with single-keyword `Mode.noTarget` entries (Wingnut, Bat on the Belfry).
- **Reprint plumbing** — converted the duplicated TMT Negate script into a
  `Printing` row pointing at the canonical FDN script; added Escape Tunnel
  as a TMT printing of the canonical MKM script. Both are wired via
  `CardDiscovery.findPrintingsIn` on `TeenageMutantNinjaTurtlesSet.printings`.
- **Trigger-doubler over a subtype filter** — `AdditionalSourceTriggers
  (sourceFilter = Creature.withSubtype("Ninja").youControl(), excludeSelf
  = false)` ships Splinter, Radical Rat. Same shape ECL Twinflame Travelers
  uses; only the subtype and excludeSelf flag differ.
- **Additional combat phase rider** — `AddCombatPhaseEffect` (LTR Éomer,
  Marshal of Rohan) ships Raph & Leo, Sibling Rivals. The printed *"if it's
  the first combat phase of the turn"* intervening-if is approximated by
  `oncePerTurn = true` (no `Conditions.IsFirstCombatPhase` primitive yet);
  approximation documented in the card's docstring.
- **Delayed destroy of a created token** — EOE Systems Override idiom:
  `CreateTokenEffect` publishes the new token into `EffectTarget.ContextTarget
  (0)`, then `CreateDelayedTriggerEffect(step = Step.END, effect =
  MoveToZoneEffect(ContextTarget(0), GRAVEYARD, byDestruction = true))`
  schedules a real *destroy* at the next end step (Old Hob, Alleycat Blues).
  The `byDestruction = true` flag is load-bearing: it routes the cleanup
  through the destroy pipeline so the second ability's UEOT indestructible
  grant legitimately saves the token. (A `sacrificeAtStep` shortcut would
  have silently mis-implemented that interaction.)
- **Fixed-N power evasion** — `CantBeBlockedBy(GameObjectFilter.Creature
  .powerAtLeast(N))` (BLB Azure Beastbinder) ships April O'Neil, Kunoichi
  Trainee with N = 3. Closes Gap O.
- **Graveyard reanimate as a typed-override token** — `MoveToZoneEffect
  (GRAVEYARD → BATTLEFIELD)` followed by `ConditionalEffect(Not(
  TargetMatchesFilter(Creature)), Effects.BecomeCreature(3, 3, keywords =
  {FLYING}, creatureTypes = {"Robot"}, duration = Duration.Permanent))`
  ships Brilliance Unleashed's Mode 2. The rider only fires when the
  original card wasn't already an artifact creature card, faithful to
  the printed wording. Closes Gap Q. Mirrors EOE Xu-Ifit's reanimate-with-
  permanent-rider shape.
- **Vehicle / Crew** — `typeLine = "Artifact — Vehicle"` +
  `keywordAbility(KeywordAbility.Numeric(Keyword.CREW, N))` already gives
  the full Vehicle pipeline (artifact-becomes-artifact-creature-UEOT, the
  Crew activation tapping a combined-power ≥ N pile of creatures you
  control). Ships `Turtle Blimp` — same shape DOM Weatherlight and BLC
  Rolling Hamsphere use. `Turtle Van` still waits on Gap LL ("creature
  that crewed this Vehicle this turn" filter), not on Vehicle itself.
- **Counter-doubling rider** — `Effects.DoubleCounters(counterType, target)`
  (Sage of the Fang shape) is the primitive that closes Gap I; the only
  card needing it in TMT is `Turtle Van`, which currently can't ship
  for a separate Gap LL reason. Gap I is closed at the SDK level even
  though no TMT card exercises it yet.
- **Sagas** — the `sagaChapter(N) { … }` DSL on `CardBuilder` (same shape
  SPM Origin of Spider-Man uses) already wires lore counters on enter and
  after the draw step, per-chapter effect groups, and the after-last-chapter
  sacrifice. Closes Gap K at the SDK level. Neither TMT Saga has shipped
  yet, but for separate reasons: `The Cloning of Shredder` needs
  `addedSubtypes` on `CreateTokenCopyOfTarget` (Gap MM); `The Last Ronin`
  needs the "Mill X. When you do, …" sub-trigger (Gap AA) plus an
  "attacks alone this turn" trigger condition (Gap NN).

**Sneak — RESOLVED.** The full alternative-cost pipeline shipped: SDK
`Keyword.SNEAK` / `KeywordAbility.Sneak(cost)` / `sneak("{cost}")` DSL helper /
`ChoiceSlot.SNEAK` flag / `Conditions.SneakCostWasPaid`, plus the engine pieces
(`SneakWindow` declare-blockers payment, `SneakCastEnumerator`, and wiring in
`CastSpellHandler` / `StackResolver` / `ConditionEvaluator` so a permanent cast
for Sneak enters tapped and attacking and carries the "sneak cost was paid"
fact). Proven by `SneakTest` and per-card scenario tests. 23 of 26 Sneak cards
shipped (incl. all four sneak-was-paid riders); the remaining 3 each carry a
second engine gap beyond Sneak (see `TODO.md` Gap A).

**Still not exercised** — Disappear's per-controller permanent-left tracking,
the display markers for Alliance / Channel / Disappear, Class enchantments, the
Mutagen token, and the remaining bespoke Gap M / N–NN shapes catalogued in
`TODO.md`. (Sagas and Vehicles/Crew are wired at the SDK level; only adjacent
gaps stop the specific TMT cards from shipping.)

---

## New mechanics in TMT

### Sneak — 26 cards — IMPLEMENTED (23/26 cards shipped)
Alternative cost keyword. Fully wired in the SDK and engine (`Keyword.SNEAK`,
`KeywordAbility.Sneak`, the `sneak("{cost}")` DSL helper, `SneakWindow`,
`SneakCastEnumerator`, and `Conditions.SneakCostWasPaid`); the 3 unimplemented
Sneak cards each carry a second engine gap beyond Sneak. Reminder text:

> Sneak {cost} (You may cast this spell for {cost} if you also return an
> unblocked attacker you control to hand during the declare blockers step.
> [He/She/It enters tapped and attacking.])

- Triggers/pays at declare-blockers (timing differs from normal alt costs).
- Permanent spells cast for Sneak enter tapped and attacking.
- Some Sneak cards have an "**If [its] sneak cost was paid**" rider that
  changes the spell's effect or gives an ongoing bonus this turn — needs
  per-spell "sneak-was-paid" state tracked across the resolution and beyond.
- Sneak-rider cards: 4 (`Leonardo, Leader in Blue`, `Turncoat Kunoichi`,
  `Karai, Future of the Foot`, `The Last Ronin's Technique`).

### Alliance — 10 cards (ability word)
> Alliance — Whenever another creature you control enters, [effect].

Always the same trigger condition; the effect varies. This is essentially a
named "ETB matters" trigger word, similar to Magecraft / Constellation.

### Disappear — 9 cards (ability word)
> Disappear — [Trigger], if a permanent left the battlefield under your
> control this turn, [effect].

Triggers come in two shapes:
- ETB ("When this creature enters, …") — 1 card (`Foot Mystic`)
- End step ("At the beginning of your end step, …") — 7 cards
- Static ETB-with-counters ("This creature enters with two +1/+1 counters on
  it if …") — 1 card (`Putrid Pals`)

Underlying condition: "a permanent left the battlefield under your control
this turn" — needs a per-turn flag the engine flips on any permanent leaving
a player's battlefield (sacrifice, destroy, exile, bounce, phased-out…).

---

## Evergreen / returning keyword abilities

| Mechanic        | Cards |
|-----------------|------:|
| Flying          |    17 |
| Trample         |    13 |
| Equip           |     7 |
| Scry            |     6 |
| Flash           |     6 |
| Menace          |     6 |
| Deathtouch      |     6 |
| Vigilance       |     5 |
| Cycling family  |     5 (Cycling, Landcycling, Typecycling, basic-land-cycling — Plains/Island/Swamp/Mountain/Forest) |
| Reach           |     4 |
| Mill (keyword)  |     4 |
| Enchant         |     3 |
| Haste           |     3 |
| Lifelink        |     2 |
| Ward            |     2 |
| Crew            |     2 (`Turtle Blimp`, `Turtle Van`) |
| Double strike   |     1 |
| First strike    |     1 |
| Indestructible  |     1 |
| Affinity        |     1 (`Krang, Master Mind` — Affinity for artifacts) |
| Kicker          |     1 (`Stomped by the Foot`) |
| Channel         |     1 (`Action News Crew`) |
| Enrage          |     1 (`Raphael, Ninja Destroyer`) |
| Landfall        |     1 (`Weather Maker`) |
| Fight           |     1 (`Novel Nunchaku`) |

Cycling breakdown: 5 cards total — 1 plain Cycling, 1 Typecycling, 5 basic-
land-cycling variants (Plainscycling/Islandcycling/Swampcycling/
Mountaincycling/Forestcycling) split across 5 different cards, plus the
generic Landcycling tag on the same cards.

---

## Token themes

| Token                                   | Cards creating it |
|-----------------------------------------|-------------------|
| Food                                    | 6 |
| 1/1 Mutant                              | 6 |
| 1/1 Ninja (black, generic)              | 3 |
| 1/1 white Ninja Turtle Spirit (attacking) | 1 (`The Last Ronin's Technique`) |
| 1/1 black Insect Warrior (flying)       | 1 (`Lord Dregg, Insect Invader`) |
| 1/1 black Rat                           | 1 (`Rat King, Verminister`) |
| 0/0 Mutant via type-change              | 1 (`Pizza Face, Gastromancer`) |
| Pizza (referenced in flavor / type)     | 1 |

---

## Structural / cross-cutting

| Pattern                                          | Cards |
|--------------------------------------------------|------:|
| Cards that reference the type **Mutant**         |    12 |
| Cards that reference the type **Ninja**          |    11 |
| Cards that reference the type **Turtle**         |     7 |
| ETB triggers ("When … enters")                   |    56 |
| Combat-attack triggers                           |    12 |
| Activated `{T}:` abilities                       |    16 |
| +1/+1 counter references                         |    38 |
| Modal "Choose one"                               |     6 |
| Sagas                                            |     2 (`The Cloning of Shredder`, `The Last Ronin`) |
| Vehicles                                         |     2 |
| Auras                                            |     3 |
| Equipment                                        |     7 |
| Non-basic lands                                  |     8 |
| Reanimate-style ("return … from your graveyard to the battlefield") | 3 |
| Mill (verb in text)                              |     4 |

No cards in the set use Proliferate, Adventure, Mutate, Backup, Bargain,
Discover, Plot, Toxic, Convoke, Cipher, Reconfigure, or daybound/nightbound.
