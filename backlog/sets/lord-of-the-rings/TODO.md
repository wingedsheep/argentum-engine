# The Lord of the Rings: Tales of Middle-earth (LTR) — Implementation Plan

> **Every card must be implemented perfectly — exactly as stated in the rules.** No
> approximations, no "close enough", no silently dropped clauses. Each card's behavior must
> match its oracle text (from `ltr_set.json`) and the Comprehensive Rules
> (`MagicCompRules_20260417.pdf`) in full, including edge cases, timing, and interactions.
> A card is not done until its scenario test proves the rules-correct behavior.

Verify status anytime with: `scripts/card-status --set LTR` (and `--list --set LTR`).

## Status

Draft cards at **163/261**. Every card still unchecked in `cards.md` (excluding the five
basic lands, which `basicLandsFallback` covers) needs at least one new engine primitive —
see the "Engine gaps blocking the remaining cards" section below. Each card is listed under
the primitive it is waiting on, with the exact blocking clause. Stop and open a dedicated
PR per gap rather than approximating.

## Data sources — do NOT hit the network

- **Card data** (name, mana cost, type line, oracle text, P/T, rarity, collector number,
  artist, flavor, image URI): read from `ltr_set.json`, **not** Scryfall. It is a full
  Scryfall dump of all 291 cards (`.data[]`, keyed by the usual field names). When running
  `add-card`, feed it the matching entry from this file instead of doing a Scryfall lookup.
- **Rules**: cite and verify against `MagicCompRules_20260417.pdf` (in the repo root),
  **not** yawgatog or any web source. Read the relevant pages with the `Read` tool's
  `pages` parameter. Quote rule numbers from that document.

## Workflow

Each card is implemented with the **`add-card` skill** (oracle errata, set registration,
scenario test) — but source its card data from `ltr_set.json` and its rule references from
`MagicCompRules_20260417.pdf` per the section above. Run one card per Claude invocation:

```
/add-card <Card Name>   # from set LTR; use ltr_set.json for data, the CompRules PDF for rules
```

The skill is the source of truth on whether a card needs an engine change.

### Git strategy

**One PR per engine-change feature**, each off `main`. When several cards share one new
engine feature, that feature's PR can land all of them together — note it in the PR.
Composable cards (no engine change needed) can land directly on `main`, one commit per card.

### Per-card procedure

For each unchecked card in `cards.md`:

1. `/add-card <name>` — implement via the DSL, no class inheritance.
2. If it composes from existing primitives → commit directly on `main` (`Add <Card>`).
3. If `add-card` finds it needs a new `Effect`/keyword/replacement/SDK change →
   stop, branch off `main`, build the engine feature + the card + tests, open its own PR.
   Update `docs/card-sdk-language-reference.md` in the same PR (required for any SDK change).
4. Check the box in `cards.md` and update the `Implemented:` count.

## Notes

- **Implement every card faithfully**, reproducing oracle text as printed (use the Scryfall
  oracle/errata text in `ltr_set.json`).
- Verify any MTG rule number against `MagicCompRules_20260417.pdf` (repo root) before
  citing it — read the relevant pages with `Read(pages=...)`. Do not use web sources.
- Battlefield filtering must use projected state (`matchesWithProjection`).
- Basic lands (Plains/Island/Swamp/Mountain/Forest) are covered by `basicLandsFallback`;
  add LTR-art basic-land variants only if you want the distinct printings.

---

## Engine gaps blocking the remaining cards

Every card still unchecked in `cards.md` (excluding the five basic lands, which the fallback
covers) needs at least one new engine primitive. They are grouped below by the gap they wait
on; each bullet quotes the **specific clause** that cannot be composed today. A card with more
than one gap is listed under its dominant gap with the secondary one noted inline. Clear a gap
in its own PR, then attach all of its cards.

Verified present today and used by the composed batch: `Effects.TheRingTemptsYou`,
`Triggers.RingTemptsYou`, `Conditions.SourceIsRingBearer`, `Conditions.CreatureDiedThisTurn`,
`Effects.Amass` (incl. `DynamicAmount.XValue`), `EffectPatterns.scry`/`mill`/`searchLibrary`,
`ForEachInGroupEffect` + `GroupFilter`, `GrantKeyword` static, `Targets.UpToCreatures` +
`ForEachTargetEffect`.

### Gap 1 — "Whenever you scry" trigger
**Engine change:** emit a `ScriedEvent` carrying the number of cards looked at, add a
`Triggers.WheneverYouScry`, and a `DynamicAmount` for "cards looked at while scrying this way".
- **Arwen Undómiel** — "Whenever you scry, put a +1/+1 counter on target creature."
- **Celeborn the Wise** — "Whenever you scry, Celeborn gets +1/+1 … for each card looked at."
- **Chance-Met Elves** — "Whenever you scry, put a +1/+1 counter on this creature." (once/turn)
- **Council's Deliberation** — "Whenever you scry, … exile this card from your graveyard …"
- **Elrond, Lord of Rivendell** — scry on each creature ETB; needs "second time this ability
  resolved this turn" counter as well.
- **Elrond, Master of Healing** — "Whenever you scry, put a +1/+1 counter on each of up to X
  target creatures, where X is the number of cards looked at."
- **Galadriel of Lothlórien** — "Whenever you scry, you may reveal the top card …" (also Gap 3).
- **Glorfindel, Dauntless Rescuer** — "Whenever you scry, choose one and Glorfindel gets +1/+1."
- **Legolas, Counter of Kills** — "Whenever you scry, if Legolas is tapped, you may untap it."
- **Nimrodel Watcher** — "Whenever you scry, this creature gets +1/+0 and can't be blocked."
- **Lost Isle Calling** — "Whenever you scry, put a verse counter on this." (also Gap 6 counter)
- **Elvish Mariner** (Extra) — "Whenever you scry, tap up to X target nonland permanents …"

### Gap 2 — "draw your second card each turn" trigger
**Engine change:** an Nth-card-drawn-per-turn trigger (per player), the draw analogue of
`Triggers.NthSpellCast`.
- **Knights of Dol Amroth** — "Whenever you draw your second card each turn, put a +1/+1 counter."
- **Prince Imrahil the Fair** — "Whenever you draw your second card each turn, create a token."
- **Stalwarts of Osgiliath** — same trigger (ETB Ring tempts half is already composable).

### Gap 3 — "the Ring tempted you and you chose a creature other than this" condition
**Engine change:** a condition usable with `Triggers.RingTemptsYou` meaning "you chose a
creature other than {source} as your Ring-bearer" (CR 701.52d).
- **Aragorn, Company Leader** — "…if you chose a creature other than Aragorn as your
  Ring-bearer…" (also Gap 7 keyword counters).
- **Faramir, Field Commander** — "…if you chose a creature other than Faramir…, create a token."
- **Gandalf, Friend of the Shire** — "…if you chose a creature other than Gandalf…, draw a card."
  (also Gap 4 cast-as-flash).
- **Galadriel of Lothlórien** — chapter-one half (also Gap 1).

### Gap 4 — "cast [filter] spells as though they had flash" permission
**Engine change:** a static/one-shot permission granting flash-timing to a filtered set of
spells.
- **Borne Upon a Wind** — "You may cast spells this turn as though they had flash."
- **Gandalf, Friend of the Shire** — "You may cast sorcery spells as though they had flash."
- **Gandalf the White** — "You may cast legendary spells and artifact spells as though they
  had flash." (also needs the extra-trigger replacement below).

### Gap 5 — Goad
**Engine change:** Goad effect + goaded combat-forcing state (CR 701.41).
- **Glóin, Dwarf Emissary** — "{T}, Sacrifice a Treasure: Goad target creature."

### Gap 6 — new counter types (each spans the 5 counter layers)
**Engine change:** add the counter type across `CounterType`, `StateProjector`, and the three
web-client files; some also need "enters with N counters where N = …".
- **Dawn of a New Age** — hope counter; "enters with a hope counter for each creature you control."
- **Lost Isle Calling** — verse counter (also Gap 1).
- **Palantír of Orthanc** — influence counter (also Gap 1 scry + complex opponent choice).
- **The One Ring** — burden counter (also Gap 9 protection-from-everything).
- **Scroll of Isildur** — stun counter (also Saga gain-control-for-duration).

### Gap 7 — keyword counters + "choose a kind of counter"
**Engine change:** first-strike/vigilance/deathtouch/lifelink counters wired through the
keyword-counter map, plus a "choose one of these counter kinds" effect.
- **Aragorn, Company Leader** — "…put your choice of a counter from among first strike,
  vigilance, deathtouch, and lifelink…; …put one of each of those kinds on another creature."

### Gap 8 — protection from a chosen / total quality
**Engine change:** protection-from-everything (CR 702.16e), protection-from-chosen-colors, and
protection-from-chosen-card-type.
- **The One Ring** — "you gain protection from everything until your next turn."
- **Éowyn, Fearless Knight** — "Legendary creatures you control gain protection from each of
  that creature's colors…" (also exile-creature-with-greater-power).
- **Pippin, Guard of the Citadel** — "…gains protection from the card type of your choice…"

### Gap 9 — copy a spell on the stack
**Engine change:** "copy target instant/sorcery spell(s), you may choose new targets."
- **Display of Power** — "Copy any number of target instant and/or sorcery spells."
- **Gandalf the Grey** — "Copy target instant or sorcery spell you control." (also a
  modal-with-memory "choose one that hasn't been chosen").

### Gap 10 — cast a spell without paying its mana cost from hand / return a spell from the stack
**Engine change:** "you may cast a spell from your hand with mana value ≤ X without paying its
mana cost"; and bouncing a spell off the stack.
- **Press the Enemy** — "Return target spell or nonland permanent…; You may cast an instant or
  sorcery spell with equal or lesser mana value from your hand without paying its mana cost."
- **Glamdring** — "…you may cast an instant or sorcery spell from your hand with mana value ≤
  that damage without paying its mana cost." (also dynamic +1/+0 per I/S in graveyard).
- **Flame of Anor** — modal with a conditional extra mode ("if you control a Wizard … choose two").

### Gap 11 — activated abilities that function from the graveyard
**Engine change:** activated abilities usable while the card is in the graveyard, with
"Exile this card from your graveyard" as a cost, sorcery-speed gating.
- **Gollum's Bite** — "{3}{B}, Exile this card from your graveyard: The Ring tempts you."
- **Gollum, Patient Plotter** — "{B}, Sacrifice a creature: Return this card from your
  graveyard to your hand." (LTB Ring tempts half is composable).

### Gap 12 — reflexive reference to "the amassed Army"
**Engine change:** expose the just-amassed Army to a reflexive "when you do" effect (its power),
and an excess-damage trigger.
- **Foray of Orcs** — "…deals X damage…, where X is the amassed Army's power."
- **Grishnákh, Brash Instigator** — "…gain control of target … creature … with power ≤ the
  amassed Army's power."
- **Shagrat, Loot Bearer** — "…amass Orcs X, where X is the number of Equipment attached…"
  (also attach-equipment-on-attack).
- **Fall of Cair Andros** — "Whenever a creature an opponent controls is dealt excess noncombat
  damage, amass Orcs X…" (excess-damage trigger).
- **Surrounded by Orcs** — "…target player mills X cards, where X is the amassed Army's power."
- **The Mouth of Sauron** — "amass Orcs X, where X is the number of instant and sorcery cards in
  **that player's** graveyard" (dynamic count keyed to a targeted player's graveyard).

### Gap 13 — set base power AND toughness
**Engine change:** `SetBaseToughness` / `SetBasePowerAndToughness` (only `SetBasePower` exists).
- **Dreadful as the Storm** — "Target creature has base power and toughness 5/5…" (Ring tempts
  half composable).
- **Frodo, Sauron's Bane** — "…becomes a Halfling Scout with base power and toughness 2/3…"
  (also activated type/ability morphing + a Ring-tempt-count win condition).

### Gap 14 — flicker (exile and return under your control)
**Engine change:** a blink effect that exiles then returns a permanent under your control.
- **Slip On the Ring** — "Exile target creature you own, then return it to the battlefield under
  your control." (Ring tempts half composable).

### Gap 15 — "deal damage to each blocker" / one-sided creature-deals-damage
**Engine change:** "{source} deals N damage to each creature blocking it"; "target creature
deals damage equal to its power to another target creature."
- **Battle-Scarred Goblin** — "…it deals 1 damage to each creature blocking it."
- **Breaking of the Fellowship** — "Target creature an opponent controls deals damage equal to
  its power to another target creature that player controls."
- **Legolas, Master Archer** — "Legolas deals damage equal to its power to up to one target
  creature." (also a "cast a spell that targets X" trigger).

### Gap 16 — count of permanents sacrificed by an effect
**Engine change:** a dynamic amount for "permanents sacrificed this way."
- **Voracious Fell Beast** — "Create a Food token for each creature sacrificed this way."

### Gap 17 — condition keyed to a creature sacrificed as a cost
**Engine change:** a condition on the creature paid as an additional/activation sacrifice cost
(e.g. "was legendary").
- **Nasty End** — "…If the sacrificed creature was legendary, draw three cards instead."
- **Gríma Wormtongue** — "If the sacrificed creature was legendary, amass Orcs 2." (also
  "your opponents can't gain life" static).
- **Rise of the Witch-king** — "If you sacrificed a creature this way, you may return another
  permanent card from your graveyard to the battlefield." (each-player edict + reanimate).

### Gap 18 — Ring-bearer player-state conditions and statics
**Engine change:** a player-level "you control a Ring-bearer" condition and a "must be blocked
if able" static gated on a condition.
- **Dúnedain Rangers** — "…if you don't control a Ring-bearer, the Ring tempts you."
- **Frodo Baggins** — "As long as Frodo Baggins is your Ring-bearer, it must be blocked if able."

### Gap 19 — "permanent you controlled left the battlefield this turn" condition
**Engine change:** a this-turn condition broader than `CreatureDiedThisTurn`.
- **Shortcut to Mushrooms** — "…if a permanent you controlled left the battlefield this turn…"
  (ETB Ring tempts half composable).

### Gap 20 — "a card put into a graveyard from the battlefield this turn" filter
**Engine change:** a zone filter that matches cards that entered a graveyard from the
battlefield during the current turn.
- **Samwise the Stouthearted** — "…target permanent card in your graveyard that was put there
  from the battlefield this turn."
- **Lobelia Sackville-Baggins** — "…creature card from an opponent's graveyard that was put
  there from the battlefield this turn…" (then Treasures = exiled card's power).

### Gap 21 — graveyard-functional triggered ability
**Engine change:** a triggered ability that works while the card is in the graveyard.
- **Ringwraiths** (Extra) — "When the Ring tempts you, return this card from your graveyard to
  your hand." (also ETB -3/-3 with conditional 3 life loss if the target was legendary).

### Gap 22 — restricted / graveyard-derived mana
**Engine change:** mana abilities whose mana is spendable only on a card class, plus a
"can't be countered" rider, and mana of colors derived from graveyard contents.
- **Delighted Halfling** — "Spend this mana only to cast a legendary spell, and that spell
  can't be countered."
- **Great Hall of the Citadel** — "Spend this mana only to cast legendary spells."
- **The Grey Havens** — "Add one mana of any color among legendary creature cards in your
  graveyard." (ETB scry half composable).

### Gap 23 — cycling variants (land/type/basic-land cycling)
**Engine change:** Landcycling / Typecycling / Swampcycling keywords.
- **Troll of Khazad-dûm** — "Swampcycling {1}" (also "can't be blocked except by three or more").

### Gap 24 — nonbasic landwalk keyword
**Engine change:** a `NONBASIC_LANDWALK` keyword + evasion check.
- **Trailblazer's Boots** (Extra) — "Equipped creature has nonbasic landwalk."

### Gap 25 — "X in an activated-ability cost" amass / doubled-X
**Engine change:** support `DynamicAmount.XValue` inside activated-ability costs (and `{X}{X}`).
- **Barad-dûr** — "{X}{X}{B}, {T}: Amass Orcs X. Activate only if a creature died this turn."
  (`CreatureDiedThisTurn` exists; conditional ETB-tapped is the other half).

### Gap 26 — "lose all abilities until end of turn"
**Engine change:** an effect that strips all abilities from a creature for a duration.
- **Barrow-Blade** — "…that creature loses all abilities until end of turn." (plus the
  blocks/blocked trigger targeting the *other* creature).

### Gap 27 — assign combat damage by toughness
**Engine change:** a continuous effect making a creature assign combat damage equal to its
toughness rather than its power.
- **Bill the Pony** — "…target creature you control assigns combat damage equal to its
  toughness rather than its power." (Food half composable).

### Gap 28 — equip-ability timing / cost modification
**Engine change:** "activate equip abilities at instant speed," equip-cost reduction, and a
"first equip each turn costs {0}" replacement.
- **Forge Anew** — instant-speed equip + free first equip (return-Equipment-from-GY half
  composable).
- **Éowyn, Lady of Rohan** — "Equip abilities you activate cost {1} less to activate." (plus a
  begin-combat modal first strike/vigilance with an equipped-creature branch).

### Gap 29 — conditional keyword from combat opponent's subtype
**Engine change:** a static granting a keyword conditioned on what is blocking/blocked-by.
- **Sting, the Glinting Dagger** — "Equipped creature has first strike as long as it's blocking
  or blocked by a Goblin or Orc." (begin-of-combat untap is composable).

### Gap 30 — cost reduction conditioned on game history
**Engine change:** cost reductions that read per-turn state (cards drawn, permanents sacrificed).
- **Gwaihir the Windlord** — "costs {2} less … as long as you've drawn two or more cards this
  turn." ("Other Birds you control have vigilance" is composable today).
- **The Balrog, Durin's Bane** — "costs {1} less to cast for each permanent sacrificed this
  turn." (plus "can't be blocked except by legendary" + a death trigger).

### Gap 31 — Vehicles / Crew
**Engine change:** the Vehicle card type, crewing, and conditional artifact-creature state.
- **Grond, the Gatebreaker** — "Crew 3" + "As long as it's your turn and you control an Army,
  Grond is an artifact creature."

### Gap 32 — phasing
**Engine change:** phase out/in (CR 702.26) and a phase-in trigger.
- **King of the Oathbreakers** — "…it phases out." / "Whenever … phases in, create a token."

### Gap 33 — token that is a copy of a creature with overrides
**Engine change:** create-token-copy of a card in the graveyard / dying creature with type and
P/T overrides and added abilities, plus delayed conditional exile.
- **Sauron, the Necromancer** — "Create a tapped and attacking token that's a copy of that card,
  except it's a 3/3 black Wraith with menace … exile that token unless Sauron is your Ring-bearer."
- **Shelob, Child of Ungoliant** — "…create a token that's a copy of that creature, except it's
  a Food artifact … and it loses all other card types." (Spider death-tracking; the Spider
  anthem statics are composable).

### Gap 34 — excess damage redirected to controller
**Engine change:** "excess damage is dealt to that creature's controller instead."
- **Gandalf's Sanction** — dynamic X = I/S cards in your graveyard; "Excess damage is dealt to
  that creature's controller instead."

### Gap 35 — "damage can't be prevented" + same-controller spread
**Engine change:** a turn-long prevention shutoff and "N damage to each other creature with the
same controller as the target."
- **Fear, Fire, Foes!** — "Damage can't be prevented this turn. … X damage to target creature
  and 1 damage to each other creature with the same controller."

### Gap 36 — combat-history target filters
**Engine change:** filters/edicts keyed to "blocked or was blocked by a legendary creature this
turn," "dealt combat damage to you this turn," and "least power."
- **You Cannot Pass!** — "Destroy target creature that blocked or was blocked by a legendary
  creature this turn."
- **Witch-king of Angmar** — "…each opponent sacrifices a creature … that dealt combat damage to
  you this turn." (plus discard→indestructible and Ring tempts).
- **Witch-king, Bringer of Ruin** (Extra) — "…defending player sacrifices a creature with the
  least power among creatures they control."

### Gap 37 — gain control for a "for as long as you control this" duration
**Engine change:** a control-change duration tied to continued control of the source.
- **Rangers of Ithilien** — "gain control of up to one target creature with lesser power for as
  long as you control this creature." (Ring tempts half composable).
- **Scroll of Isildur** — "Gain control of up to one target artifact for as long as you control
  this Saga." (also stun counter, Gap 6).

### Gap 38 — one-off complex cards (each its own PR)
**Engine change:** bespoke — these don't share a clean reusable gap with others.
- **Goldberry, River-Daughter** — move counters of each kind between target permanents.
- **Bewitching Leechcraft** — grant the enchanted creature a custom untap-replacement ability.
- **Radagast the Brown** — look at top X (= creature's mana value), reveal a creature not
  sharing a type with one you control.
- **Long List of the Ents** — Saga that *notes creature types* and buffs the next cast of that
  type.
- **Peregrin Took** — token-creation replacement ("those tokens plus an additional Food").
- **Phial of Galadriel** — draw-replacement (draw two if hand empty) + life-gain doubling.
- **Tom Bombadil** — "4+ lore counters among Sagas → hexproof and indestructible" + a
  "final chapter ability resolves" trigger.
- **Sharkey, Tyrant of the Shire** — copy/lock opponents' land activated abilities.
- **Saruman of Many Colors** — Ward—discard; second-spell mill/exile/copy/cast chain.
- **Sauron, the Dark Lord** — Ward—sac; "Army deals combat damage → the Ring tempts you" + more.
- **Ent-Draught Basin** — "{X}, {T}: +1/+1 counter on target creature with power X."
- **Mount Doom** — "Choose up to two creatures, then destroy the rest."
- **Faramir, Prince of Ithilien** — delayed end-step "choose an opponent" + did-they-attack-you.
- **Boromir, Warden of the Tower** — "if no mana was spent to cast it, counter that spell."
- **Call of the Ring** — "Whenever you choose a creature as your Ring-bearer …" trigger.
- **Glorious Gale** — counter creature spell + "if it was a legendary spell" conditional.
- **There and Back Again** — Saga producing **Smaug**, a named token with its own death trigger.
- **Hew the Entwood** — sacrifice any number of lands, reveal X, put artifact/land cards in play.
- **Ringsight** — tutor for a card that "shares a color with a legendary creature you control."
- **One Ring to Rule Them All** — Saga; mill = your Ring-bearer's power (Gap 12-style dynamic).
- **The Ring Goes South** — reveal until X lands, X = legendary creatures you control.
- **Orcish Bowmasters** — "whenever an opponent draws a card except the first … in their draw
  step" trigger.
- **Sauron's Ransom** — opponent separates your top four into hidden face-down/face-up piles.
- **Flame of Anor** — see Gap 9/10 (conditional modal count).
- **Sméagol, Helpful Guide** — reveal-until-land put under your control (the end-step Ring-tempt
  half uses `CreatureDiedThisTurn`, already present).
- **Riders of the Mark** (Extra) — Affinity for Humans (composable) + end-step "if it attacked,
  return it to hand and create tokens equal to its toughness."
- **Fires of Mount Doom** (Extra) — impulse-exile-and-play + destroy attached Equipment + damage.
- **Frodo, Determined Hero** (Extra) — attach Equipment of MV 2–3 on enter/attack + "prevent all
  damage to Frodo during your turn."
- **Gollum, Scheming Guide** (Extra) — opponent guesses whether your top card is land/nonland.
