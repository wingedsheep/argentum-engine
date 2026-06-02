# Teenage Mutant Ninja Turtles (TMT) — Implementation Plan

> **Every card must be implemented perfectly — exactly as stated in the rules.** No
> approximations, no "close enough", no silently dropped clauses. Each card's behavior must
> match its oracle text (from `tmt_full.json` in the repo root) and the Comprehensive Rules
> (`MagicCompRules_20260417.pdf`) in full, including edge cases, timing, and interactions.
> A card is not done until its scenario test proves the rules-correct behavior.

Verify status anytime with: `scripts/card-status --set TMT` (and `--list --set TMT`).

## Status

27 / 190 implemented (basics excluded — handled by `basicLandsFallback`).

Implemented so far (alphabetical):
Agent Bishop · Man in Black, Anchovy & Banana Pizza, April · Reporter of the Weird,
Armaggon · Future Shark, Bespoke Bō, Bot Bashing Time, Buzz Bots, Casey Jones ·
Jury-Rig Justiciar, Cowabunga!, Death in the Family, Dimension X, Donatello · Turtle
Techie, Donatello · Way with Machines, Dream Beavers, Featherbrained Filcher, Foot
Headquarters, Frog Butler, Guac & Marshmallow Pizza, Hamato Guardian Stance,
Hard-Won Jitte, Henchbots, High-Flying Ace, Ice Cream Kitty, Illegitimate Business,
Mutant Town, Raph & Mikey · Troublemakers, TCRI Building.

New-mechanic work blocks the bulk of the set: 26 cards wait on Sneak, 9 on Disappear,
10 on Alliance. See "Engine gaps blocking the remaining cards" below.

## Data sources — do NOT hit the network

- **Card data** (name, mana cost, type line, oracle text, P/T, rarity, collector number,
  artist, flavor, image URI): read from `/workspace/tmt_full.json`, **not** Scryfall. It is
  a full Scryfall dump of all 195 cards (`.data[]`, keyed by the usual field names). When
  running `add-card`, feed it the matching entry from this file instead of doing a Scryfall
  lookup.
- **Rules**: cite and verify against `MagicCompRules_20260417.pdf` (repo root), **not**
  yawgatog or any web source. Read pages with the `Read` tool's `pages` parameter.

## Workflow

Each card is implemented with the **`add-card` skill** (oracle errata, set registration,
scenario test) — but source its card data from `tmt_full.json` and its rule references from
`MagicCompRules_20260417.pdf` per the section above. Run one card per Claude invocation:

```
/add-card <Card Name>   # from set TMT; use tmt_full.json for data, the CompRules PDF for rules
```

The skill is the source of truth on whether a card needs an engine change.

### Git strategy

**One PR per engine-change feature**, each off `main`. When several cards share one new
engine feature, that feature's PR can land all of them together — note it in the PR.
Composable cards (no engine change needed) can land directly on `main`, one commit per card.

### Per-card procedure

1. `/add-card <name>` — implement via the DSL, no class inheritance.
2. If it composes from existing primitives → commit directly on `main` (`Add <Card>`).
3. If `add-card` finds it needs a new `Effect`/keyword/replacement/SDK change →
   stop, branch off `main`, build the engine feature + the card + tests, open its own PR.
   Update `docs/card-sdk-language-reference.md` in the same PR (required for any SDK change).
4. Check the box in `cards.md` and update the `Implemented:` count.

## Notes

- Implement every card faithfully, reproducing oracle text as printed (use the Scryfall
  oracle text in `tmt_full.json`).
- Verify any MTG rule number against `MagicCompRules_20260417.pdf` before citing it.
- Battlefield filtering must use projected state (`matchesWithProjection`).
- Basic lands are covered by `basicLandsFallback`; add TMT-art variants only if you want
  the distinct printings.

---

## Engine gaps blocking the remaining cards

Each gap is its own PR. A card with more than one gap is listed under its dominant one
with the secondary noted inline. Quotes are from the card's oracle text in `tmt_full.json`.

### Gap A — Sneak (alternative cost) — 26 cards
**Engine change:** new keyword `SNEAK` + `KeywordAbility.Sneak(cost)` + alternative-cost
plumbing that branches on a non-mana, mid-combat condition.

> Sneak {cost} (You may cast this spell for {cost} if you also return an unblocked
> attacker you control to hand during the declare blockers step.
> [He/She/It enters tapped and attacking.])

Required pieces:
1. **Alt-cost gate at declare-blockers.** Today's alt-cost pipeline (`Flashback`,
   `Harmonize`, `Impending`) all resolve at cast time. Sneak's payment piece — returning
   an unblocked attacker you control to hand — happens during the declare-blockers step
   *of the same turn the spell is cast*. The action enumerator must surface "cast for
   Sneak" only when (a) it's the declare-blockers step of your combat, (b) you control
   an unblocked attacker, and (c) the spell is castable at the current speed. Casting
   for Sneak also queues the bounce-an-unblocked-attacker as an additional payment.
2. **Permanent spells enter tapped and attacking.** When a creature/artifact-creature
   spell resolves having paid its sneak cost, the permanent enters tapped and joins the
   attack. This already exists for cascade/mobilize tokens (CreateTokenEffect with
   `tappedAndAttacking`) — reuse the same `attackingState` plumbing for cast permanents.
3. **Per-spell "sneak-was-paid" flag.** 4 cards (`Leonardo, Leader in Blue`,
   `Turncoat Kunoichi`, `Karai, Future of the Foot`, `The Last Ronin's Technique`) read
   "if [its] sneak cost was paid" later — at resolution, at ETB, or as a turn-long
   rider on a damage trigger. Stash the flag on the spell-cast event (cast-time) and
   on the resulting permanent's component (post-resolution), and expose it as a
   `Condition.SneakCostWasPaid(source)` predicate.
4. **DSL helper.** `sneak("{1}{W}") { ... }` on `CardBuilder`, mirroring `harmonize`
   and `impending`.

Cards: `Action News Crew`? — _no, Channel_. Sneak cards (26):
`The Last Ronin's Technique`, `Leonardo, Leader in Blue`, `Leonardo, Big Brother`,
`Leonardo, Cutting Edge`, `Leonardo, Sewer Samurai`, `Leonardo's Technique`,
`Turncoat Kunoichi`, `Karai, Future of the Foot`, plus the remaining 18 — full list
is the cards with `"Sneak"` in their `keywords` array in `tmt_full.json`.

### Gap B — Disappear (ability-word trigger with "a permanent left the battlefield under your control this turn" condition) — 9 cards
**Engine change:** generalize the existing `nonlandPermanentLeftBattlefieldThisTurn`
flag and add a Condition usable inside any triggered ability.

Required pieces:
1. **Per-player, all-permanent tracking.** The current flag in `GameState` is global and
   nonland-only (used by EOE's Void / Spellwarp). Disappear needs **any** permanent
   (lands included) that left the battlefield while under that **player's** control,
   per-turn. Suggested rename / addition:
   `permanentLeftBattlefieldThisTurnByController: Map<PlayerId, Boolean>` set in
   `ZoneTransitionService` whenever a permanent moves out of the battlefield (destroy,
   sacrifice, exile, bounce, phase-out). Existing Void uses can keep reading the
   nonland-restricted flag or migrate; do not silently change Void's filter.
2. **`Condition.PermanentLeftBattlefieldUnderYourControlThisTurn`** that reads the
   per-controller map for the triggered ability's controller.
3. The triggers themselves are existing primitives (`Triggers.EtbSelf`,
   `Triggers.AtYourEndStep`, and an enters-with-counters static for `Putrid Pals`).
   Wire them with the new condition and the existing effect library.

Triggers used across the 9 cards:
- End-step (7): `Insectoid Exterminator`, `Lord Dregg, Insect Invader`,
  `Rat King, Verminister`, `Michelangelo, Game Master`, `West Wind Avatar`,
  `Krang & Shredder`, `Pizza Face, Gastromancer`
- ETB (1): `Foot Mystic`
- "Enters with two +1/+1 counters if ..." (1): `Putrid Pals` — needs
  `entersWith(counters, condition)` if not already supported.

### Gap C — Alliance (ability-word trigger) — 10 cards
**Engine change:** none — pure ability-word marker. Add `Keyword.ALLIANCE` (display only)
and an `alliance { ... }` DSL helper that wires
`Triggers.WheneverAnotherCreatureYouControlEnters { effect }`. Mirror the existing
`flurry`, `eerie`, `vivid`, `fatefulBite` ability-word helpers — no new engine code.

Cards: `East Wind Avatar`, `Lita, Little Orphan Amphibian`, `Mighty Mutanimals`,
`Mutant Town Musicians`, `Raphael, Most Attitude`, `Raphael, Tough Turtle`,
`Slash, Reptile Rampager`, `Wingnut, Bat on the Belfry`, `EPF Point Squad`,
`The Neutrinos`. One of them (`Lita`) layers a "modal effect that hasn't been chosen
this turn" rider — see Gap E.

### Gap D — Channel keyword/DSL helper — 1 card
**Engine change:** `Channel` keyword + DSL helper that wires the discard-from-hand
activated ability. The mechanic exists in spirit (Kamigawa), but no `KeywordAbility.Channel`
entry exists in `KeywordAbility.kt`. Compose: activated ability with cost `{N}, Discard
this card.` from hand zone.
- **Action News Crew** — "Channel — {6}, Discard this card: Put a +1/+1 counter on each
  creature you control. Draw a card."

### Gap E — modal "choose one that hasn't been chosen this turn" — 1 card
**Engine change:** per-source memory of which modes have been chosen this turn,
threaded into modal-spell mode selection. (LTR's Gandalf the Grey gap mentions the
same shape — coordinate with that PR.)
- **Lita, Little Orphan Amphibian** — Alliance trigger picks a mode "that hasn't been
  chosen this turn."

### Gap F — Enrage (ability word) — 1 card
**Engine change:** display-only `Keyword.ENRAGE` + `enrage { ... }` DSL helper that
wires the existing "Whenever this creature is dealt damage" trigger (already exists per
Ixalan-era cards in older sets). Verify the trigger primitive is present; if not, add it.
- **Raphael, Ninja Destroyer** — "Enrage — Whenever Raphael is dealt damage, add that
  much {R}. Until end of turn, you don't lose this mana as steps and phases end."
  Also needs the **"don't lose this mana as steps and phases end"** mana modifier
  (CR 106.4b duration override) — see Gap G.

### Gap G — mana that doesn't empty at end of step/phase — 1 card
**Engine change:** mana pool entry tagged "persists across steps/phases for the rest of
the turn" (CR 106.4b exception). Likely a flag on the mana pool entry that the empty-mana
hooks skip.
- **Raphael, Ninja Destroyer** — see Gap F.

### Gap H — Affinity for artifacts (cost reduction) — 1 card
**Engine change:** `AFFINITY` keyword exists; verify "Affinity for artifacts" cost-
reduction wiring counts artifacts you control. If the wiring is parameterised (filter +
displayPrefix), this is just a DSL call; if hard-coded for a specific filter, generalise.
- **Krang, Master Mind** — "Affinity for artifacts."

### Gap I — "double the number of +1/+1 counters" — 1 card
**Engine change:** an effect that doubles the count of a chosen counter kind on a
target permanent (CR 121.3 — places that many additional counters).
- **Turtle Van** — "double the number of +1/+1 counters on it" if the buffed creature
  is Mutant/Ninja/Turtle. Composes with the existing `put +1/+1 counter` effect.

### Gap J — Crew / Vehicle — 2 cards
**Engine change:** Vehicle card type + Crew N (tap any combination of creatures with total
power ≥ N to turn the artifact into an artifact-creature until end of turn). `Keyword.CREW`
exists, but the artifact-becomes-artifact-creature behavior and the Vehicle card type need
to be wired. (Coordinates with LTR Gap 31.)
- **Turtle Blimp**, **Turtle Van**

### Gap K — Sagas — 2 cards
**Engine change:** Sagas with chapter abilities. Earlier sets (Dominaria, LTR) already
have Saga support; verify it's still functional and the chapter DSL is reachable.
- **The Cloning of Shredder**, **The Last Ronin** — chapter abilities only; no new
  Saga primitives expected.

### Gap L — Landfall, Fight, Mill keyword, basic-land-cycling — verify
**Engine change:** likely none — these all exist in older sets. Confirm during
implementation; add the small gap PR if any are missing.
- Landfall: `Weather Maker`
- Fight: `Novel Nunchaku`
- Mill keyword: `Does Machines`, `Kitsune's Technique`, `Paramecia Coloniex`,
  `The Last Ronin`
- Basic-land-cycling family (Plains/Island/Swamp/Mountain/Forest) — `KeywordAbility.Cycling`
  with `searchFilter` already supports these (see `KeywordAbility.kt:217`).

### Gap M — one-off bespoke cards
Each is its own PR; they don't share a clean reusable gap with others.

- **Pizza Face, Gastromancer** — "If it isn't a creature, it becomes a 0/0 Mutant
  creature in addition to its other types" (type-grant + base P/T on a non-creature).
- **Rat King, Verminister** — "Return target creature card and all other cards with the
  same name as that card from your graveyard to the battlefield tapped." (Name-matching
  multi-target reanimate.)
- **Krang & Shredder** — "Whenever Krang & Shredder enter or attack, each opponent
  exiles cards from the top of their library until they exile a nonland card."
  (Reveal-until per-opponent + cast-from-exile-without-paying via the Disappear rider.)
- **West Wind Avatar** — "you may sacrifice a token or a land" (sacrifice filter:
  "token or land" — composes existing filters, just verify the disjunction works).
- **Wingnut, Bat on the Belfry** — "Wingnut gains your choice of flying, menace, or
  reach until end of turn." (Choose-one-of-N-keywords grant — generalises
  Aragorn-style "choose a counter kind" from LTR Gap 7.)
- **Lord Dregg, Insect Invader** — "Sacrifice a token: Draw a card." (Token-filtered
  sacrifice cost on an activated ability; verify `AbilityCost.SacrificeFiltered`
  supports the token predicate.)
- **The Last Ronin's Technique** — Sneak rider: "If this spell's sneak cost was paid,
  they enter tapped and attacking." (Gap A) → token-creator already supports
  tapped-and-attacking; condition is the new piece.

(Continue listing as `add-card` discovers more during implementation.)

---

## New engine gaps discovered during implementation

These were found while walking the alphabetical card list. Each is one card today
unless noted; group with Gap M if they stay solo, lift to their own gap if more
cards turn up wanting the same primitive.

### Gap N — distinct-card-types-among-spells-cast-this-turn dynamic amount
**Engine change:** add `DynamicAmount.DistinctCardTypesAmongSpellsCastByYouThisTurn`
(or a parameterized aggregator over the existing spell-cast-this-turn log).
- **April O'Neil, Hacktivist** — end step: "draw a card for each card type among
  spells you've cast this turn."

### Gap O — "can't be blocked by creatures with power N or greater"
**Engine change:** a static / target-filtered evasion gating creatures by power.
The blocking-static plumbing exists (`BlockingStaticAbilities.kt`); generalize to
read the blocker's projected power.
- **April O'Neil, Kunoichi Trainee** — "can't be blocked by creatures with power
  3 or greater."

### Gap P — "unless you discard a card" cost-mitigation rider
**Engine change:** a triggered-ability shape that says "sacrifice X unless you
[pay alternate cost]," where the player chooses on resolution.
- **Bebop & Rocksteady** — "Whenever Bebop & Rocksteady attack or block, sacrifice
  a permanent unless you discard a card."

### Gap Q — graveyard reanimate as a typed-override token
**Engine change:** a return-from-graveyard variant that puts the card on the
battlefield as if it were a token with overridden card types, P/T, color, and
granted keywords. Different from current copy-with-overrides (which targets a
permanent in play) — this overrides a card on its way out of the graveyard.
- **Brilliance Unleashed** — second mode: non-artifact-creature artifact card
  comes back as "a 3/3 Robot artifact creature with flying."

### Gap R — gain control of all matching permanents UEOT + untap + grant haste
**Engine change:** a bulk gain-control effect over a filter (all artifacts an
opponent controls), composed with Untap and GrantKeyword(HASTE, UEOT). Existing
gain-control primitives are single-target (LTR Gap 37 covers duration-based).
- **Broadcast Takeover** — "Gain control of all artifacts your opponents control
  until end of turn. Untap them. They gain haste until end of turn."

### Gap S — delayed trigger "at the beginning of your NEXT upkeep"
**Engine change:** delayed-trigger plumbing for "next" timing windows (not the
end-of-turn cleanup the engine already wires up).
- **Casey Jones, Vigilante** — ETB draw 3, then "at the beginning of your next
  upkeep, discard three cards at random."

### Gap T — copy-an-artifact-token with sacrifice at the next end step
**Engine change:** activated "create a token that's a copy of target artifact you
control" + "sacrifice it at the beginning of the next end step." Existing
token-copy effects are creature-typed and don't carry a self-sac timer.
- **Chrome Dome** — "{5}: Create a token that's a copy of another target artifact
  you control. That token gains haste. Sacrifice it at the beginning of the next
  end step."

### Gap U — Class enchantments (level-up subtype) — 2 cards
**Engine change:** the Class card type (Strixhaven CR 716) — sorcery-speed
level-up costs, per-level static/triggered abilities, "becomes level N" trigger.
Likely a sizable build; coordinates with future Strixhaven / Adventures in the
Forgotten Realms work.
- **Cool but Rude** — Class with 3 levels.
- **Does Machines** — Class with 2 levels.

### Gap V — search-or-fail conditional ETB effect
**Engine change:** an "ETB: search library for X; if no X is put into hand this
way, create a Y token" composite. Composes existing primitives but needs the
post-search "did anything land in hand" condition exposed.
- **Courier of Comestibles** — "search your library for a Food card …; if you
  don't put a card into your hand this way, create a Food token."

### Gap W — Mutagen token (artifact token with an activated counter ability)
**Engine change:** generic "create a Mutagen token" facade — an artifact token
with "{1}, {T}, Sacrifice: put a +1/+1 counter on target creature. Activate only
as a sorcery." Several cards reference it; once defined, they're all composable.
- **Crustacean Commando** — "create a Mutagen token."
- **Genghis Frog** — "Whenever … another Mutant you control enters, create a
  Mutagen token."

### Gap X — flicker a pair (artifact + creature) and return together
**Engine change:** an exile-and-return effect that takes two targets and returns
both at the same time (so synchronous ETB triggers see each other).
- **Don & Leo, Problem Solvers** — "exile up to one target artifact you control
  and up to one target creature you control. Then return them to the battlefield
  under their owners' control."

### Gap Y — grant a keyword (Affinity) to the next spell of a kind you cast
**Engine change:** a delayed continuous effect that watches the next noncreature
spell cast by you this turn and grants it Affinity-for-artifacts at cast time.
Different from cost-reduction-this-turn auras because the granted ability fires
at the *next-cast* moment with a specific filter.
- **Don & Raph, Hard Science** — "the next noncreature spell you cast this turn
  has affinity for artifacts."

### Gap Z — "an artifact entered the battlefield under your control this turn"
**Engine change:** per-controller "has X entered this turn" tracking that
includes artifacts (and ideally lands/creatures), and a `Condition` reading it
for static unblockability. Adjacent to the `nonlandPermanentLeftBattlefield`
plumbing but on the *entered* side.
- **Fugitive Droid** — "This creature can't be blocked if an artifact entered
  the battlefield under your control this turn." Second ability ("counter target
  spell that targets an artifact or creature you control") also needs a target-
  spell-targets-filter check that the engine doesn't expose generically yet.

### Gap AA — "when you do" sub-trigger off an optional sacrifice
**Engine change:** the "may [cost]. When you do, [effect]" two-step pattern,
where the inner trigger only fires if the optional cost was paid.
- **General Traag, Heart of Stone** — "you may sacrifice another artifact. When
  you do, General Traag deals 4 damage to target creature."

### Gap BB — "damage equal to the greatest power among creatures you control"
**Engine change:** `DynamicAmount.GreatestPowerAmong(filter)` (and the
single-creature filter for "the creature with the greatest power"). Read via
projected state. Pairs with several existing damage effects via
`Effects.DealDamage(dynamicAmount, target)`.
- **Go Ninja Go** — Mode 2: "deals damage equal to the greatest power among
  creatures you control to target creature an opponent controls."

### Gap CC — "Whenever you tap a land for mana, add X" trigger
**Engine change:** a triggered ability on mana abilities — the trigger fires
when the player taps a land specifically *to add mana*, not every time it
becomes tapped.
- **Groundchuck & Dirtbag** — "Whenever you tap a land for mana, add {G}."

### Gap DD — cost reduction conditioned on target's state
**Engine change:** spell-cost discount evaluated against the spell's intended
target (a tapped creature here). Different from "if you control X" reductions
because the predicate reads the chosen target during cast.
- **Grounded for Life** — "This spell costs {3} less to cast if it targets a
  tapped creature."

---

## Composable — deferred for time

These cards appear to compose from primitives the engine already has, but were
skipped during the alphabetical first pass to keep pace. Pick them up next.

- **Baxter Stockman** — ETB Robot token + begin-combat pump on target artifact
  creature you control. Needs an inline `TargetFilter(GameObjectFilter(
  cardPredicates = [IsCreature, IsArtifact]).youControl())`.
- **Bebop, Warthog Warrior** — Menace + "Rhinos you control have menace" anthem
  + Swampcycling {2}. All primitives exist; Swampcycling uses
  `KeywordAbility.Cycling(searchFilter = …, displayPrefix = "Swampcycling")`.
- **Dimensional Exile** — Aura "enchant basic land you control" with ETB exile
  target creature until LTB. Need the basic-land-you-control aura target.
- **Foot Elite** — attack-trigger pump "+1/+0 and gains indestructible UEOT" on
  a target creature you control. Composes ModifyStats + GrantKeyword
  (INDESTRUCTIBLE, EndOfTurn).

---

## Skip log — cards inspected and skipped (alphabetical)

Each row is a card encountered during the alphabetical pass that was not
implemented in this session, with the underlying blocker. Gaps reference the
sections above (existing Gap A–M or the new Gap N–DD).

| Card                                  | Blocker / Gap                               |
|---------------------------------------|---------------------------------------------|
| Action News Crew                      | Gap D (Channel)                             |
| April O'Neil, Hacktivist              | Gap N (card-types-cast amount)              |
| April O'Neil, Kunoichi Trainee        | Gap O (can't-be-blocked-by-power)           |
| Baxter Stockman                       | Composable — deferred                       |
| Bebop & Rocksteady                    | Gap P (unless-you-discard rider)            |
| Bebop, Warthog Warrior                | Composable — deferred                       |
| Brilliance Unleashed                  | Gap Q (typed-override reanimate token)      |
| Broadcast Takeover                    | Gap R (mass gain-control UEOT)              |
| Casey Jones, Vigilante                | Gap S (delayed next-upkeep trigger)         |
| Chrome Dome                           | Gap T (copy-artifact + next-end-step sac)   |
| Cool but Rude                         | Gap U (Class)                               |
| Courier of Comestibles                | Gap V (search-or-fail-then-token)           |
| Crustacean Commando                   | Gap W (Mutagen token)                       |
| Dark Leo & Shredder                   | Gap A (Sneak)                               |
| Dimensional Exile                     | Composable — deferred                       |
| Does Machines                         | Gap U (Class)                               |
| Don & Leo, Problem Solvers            | Gap X (paired flicker)                      |
| Don & Raph, Hard Science              | Gap Y (grant Affinity to next spell)        |
| Donatello's Technique                 | Gap A (Sneak)                               |
| Donatello, Gadget Master              | Gap A (Sneak) + token-with-overrides        |
| Donatello, Mutant Mechanic            | Gap M (Pizza-Face-style type-grant)         |
| Foot Elite                            | Composable — deferred                       |
| Foot Ninjas                           | Gap A (Sneak)                               |
| Fugitive Droid                        | Gap Z (artifact-ETB-this-turn + sac-counter)|
| General Traag, Heart of Stone         | Gap AA (when-you-do sub-trigger)            |
| Genghis Frog                          | Gap W (Mutagen token)                       |
| Go Ninja Go                           | Gap BB (greatest-power-among amount)        |
| Groundchuck & Dirtbag                 | Gap CC (tap-land-for-mana trigger)          |
| Grounded for Life                     | Gap DD (cost reduction if target tapped)    |
