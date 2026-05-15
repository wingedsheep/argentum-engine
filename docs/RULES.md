# Supported Engine Rules

Lists rules `argentum-engine` currently implements. Read by hivedrone's
rule-analysis pre-pass to decide whether a card needs new engine code or
can reuse existing rules. Update this file whenever a new rule lands on `main`.

The bullets below are evidence-based: each entry corresponds to a DSL primitive
that is wired through the rules engine and exercised by at least one implemented
card under `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/` or a
passing scenario test under `game-server/src/test/kotlin/.../scenarios/`. When in
doubt, leave it out — the planner will conservatively schedule a rule-plan.

## Keywords

### Evergreen / commonly printed
- Flying
- Trample
- Haste
- Vigilance
- First strike
- Double strike
- Flash
- Lifelink
- Reach
- Deathtouch
- Indestructible
- Menace
- Defender
- Hexproof (plain; "Hexproof from <color>" via `KeywordAbility.hexproofFrom`)
- Shroud
- Prowess
- Changeling

### Ward (parameterized)
- Ward {N} (mana cost) — `KeywordAbility.ward("{N}")`
- Ward — Pay N life — `KeywordAbility.wardLife(n)`
- Ward — Discard a card — `KeywordAbility.wardDiscard()`

### Protection (parameterized)
- Protection from a color — `KeywordAbility.protectionFrom(Color)`
- Protection from multiple colors
- Protection from a creature subtype (e.g. "Protection from Goblins")
- Protection from each opponent

### Evasion / landwalk
- Fear
- Swampwalk, Forestwalk, Islandwalk, Mountainwalk

### Damage-modifying
- Wither
- Toxic N (numeric)

### Cost-modification / alternative-cost keywords
- Convoke
- Delve
- Affinity for <card type> and Affinity for <subtype>
- Cycling {cost} — discards and draws
- Typecycling (e.g. Forestcycling, Slivercycling) — discards and searches library for a card with that subtype
- Basic landcycling — discards and searches library for any basic land
- Flashback {cost} (optionally with an additional non-mana cost; exiles on resolution)
- Kicker {cost} and Multikicker {cost}
- Offspring {cost} (Kicker-shaped; spawns a 1/1 token copy on ETB when paid)
- Warp {cost} — cast from hand for warp cost, exile at next end step, may be recast from exile on a later turn
- Evoke {cost} — alternative cost; ETB triggers fire, then delayed sacrifice trigger
- Conspire — tap two same-colour untapped creatures you control to copy the spell

### Other keyword mechanics
- Storm
- Persist
- Provoke (combat keyword; forces the chosen creature to block if able)
- Amplify N — reveal creature cards from hand on ETB; ETB with that many +1/+1 counters
- Crew N (vehicle activation; tap creatures with total power >= N to make the vehicle a creature until end of turn)
- Equip {cost} (on Equipment) — `equipAbility("{cost}")`
- Morph {cost} (cast face down as a 2/2 for {3}; turn face up any time for morph cost)
- Vivid (ability-word prefix; magnitude scales with number of distinct colours you control)
- Eerie (ability-word prefix wired to enchantment-ETB + room-fully-unlocked triggers)

### Numeric keywords (printed text + payload; engine wires them where used)
- Annihilator N, Bushido N, Rampage N, Absorb N, Afflict N, Modular N,
  Fading N, Vanishing N, Renown N, Fabricate N, Tribute N
  (catalog entries with display text and N; only the ones above with their own
  bullets have full mechanical wiring beyond what `KeywordAbility.Numeric` carries.
  When a card's only behaviour is the keyword itself, prefer to confirm via an
  existing scenario test before assuming the mechanic is fully simulated.)

## Spell effects

### Damage
- Deal N damage to target (creature / player / planeswalker / any target / creature-or-player / creature-or-planeswalker)
- Deal X damage where X is dynamic
- Divided damage (Effects.DealDamage divided among targets)
- Deal N damage to each entity in a zone
- Fight (two creatures deal damage equal to their power to each other)

### Life
- Gain N life (controller or targeted player)
- Lose N life
- Lose half your life
- Set life total to N
- Exchange life and power (self/target)
- Pay-life as an effect-time cost

### Card flow
- Draw N cards
- Draw up to N cards
- Each opponent discards N
- Replace next draw with another effect
- Reveal hand / look at target hand / look at face-down

### Zones / removal
- Destroy target permanent
- Destroy all permanents matching a filter (with optional "and attached")
- Destroy all equipment on target
- Exile target permanent / card-in-graveyard / spell on stack
- Exile until this leaves the battlefield (the classic "Oblivion Ring" pattern)
- Exile opponents' graveyards
- Force exile across multiple zones
- Return target to hand (bounce)
- Return creatures put into your graveyard this turn
- Put target on top / on top-or-bottom / Nth-from-top of library
- Shuffle target into library
- Sacrifice (controller chooses) and sacrifice-target
- Force-return one's own permanent

### Counters
- Add +1/+1 (or any named) counters: fixed or dynamic amount
- Add counters to a stored collection
- Remove counters / remove any number / remove all
- Distribute counters among targets / distribute from self
- Move all last-known counters
- Proliferate

### Stats / types / abilities
- Modify power/toughness (until end of turn or permanent)
- Set base power, set base power/toughness
- Add card type / add creature type / add subtype
- Set creature subtypes; lose all creature types
- Change colour; become all colours; become chosen mana colour
- Grant keyword (until end of turn or permanent)
- Remove keyword / remove all abilities
- Grant hexproof / shroud (player or permanent)
- Grant toxic
- Grant hexproof-from-chosen-colour, grant can't-be-blocked-by-chosen-colour
- Grant attackers-blocked-by-this gain a keyword
- Animate land; become creature; each permanent becomes a copy of target
- Transform; turn face up / turn face down

### Mana
- Add {C}, {coloured}, dynamic-amount, any-colour, one-of-each-among, of-chosen-colour
- Add mana of any colour that your lands could produce
- Mana-spent restrictions (`ManaRestriction`)

### Tokens
- Create predefined creature token (`CreateToken`, `CreateDynamicToken`)
- Create token copy of self / target / equipped creature
- Create predefined named tokens: Treasure, Food, Lander, Map, Mutavault
- Create Role token (attached to target)
- Incubate N (creates an Incubator artifact token that flips into a 0/0 creature with N +1/+1 counters)
- Explore

### Stack manipulation
- Counter target spell / counter target spell to exile (optionally grant free cast from exile)
- Counter target triggering spell
- Counter unless its controller pays X (fixed or dynamic; optionally exile on counter)
- Counter target activated/triggered ability
- Counter all opponent stack objects
- Return target spell to its owner's hand
- Copy target spell / copy target triggered ability
- Copy next spell cast / copy each spell cast (storm-shaped batching is wired separately for Storm)
- Change spell's target / change target / reselect target randomly
- Grant a keyword to a spell on the stack
- Mark spell exile with counters (for Warp-style exile-and-recast tracking)

### Tap / untap / combat
- Tap / untap target
- Tap or untap a stored collection
- Tap target creatures
- Can't attack / can't block / can't attack-or-block (single target or group)
- Remove from combat
- Force-block (must-be-blocked / provoke)
- Prevent next N damage (target or chosen source)
- Prevent all combat damage / prevent all damage / prevent combat damage from a filter
- Deflect next damage from a chosen source
- Reflect combat damage; redirect combat damage to controller
- Taunt
- Grant attack/block tax per creature type
- Grant damage bonus

### Control
- Gain control of target permanent (permanent or end-of-turn)
- Exchange control of two permanents
- Gain control by "most of subtype"
- Give control of permanent to target player
- Choose creature type, gain control of all of that type

### Composition / control flow
- Composite (sequence of effects)
- Conditional effect (gate on a `Conditions.*` predicate)
- For-each (target, player, group element)
- Modal effect (choose one / one or more)
- "Choose one" action effect (with feasibility checks per branch)
- May effect (optional)
- Optional-cost effect / may-pay-mana / may-pay-X-for-effect
- "If you do" gate (pay cost then resolve consequent)
- Reflexive trigger ("when you do, …")
- Repeat-while / repeat dynamic times
- Flip a coin / flip two coins
- Create delayed triggered ability
- Budget modal effect (split a numeric budget across modes)

### Library / search / piles
- Gather cards (build a filtered collection from a zone)
- Select from collection (choose exactly / up to / all)
- Move collection to destination zone
- Filter collection
- Reveal collection
- Choose pile (separate permanents into piles for opponent to choose)
- Exile from top repeating; exile library until mana value
- Exile top card, may play it free
- Put on top or bottom of library
- Shuffle library

### Equipment / attachments
- Attach equipment (to ETB target, to chosen target, etc.)
- Return self to battlefield attached
- Grant "exile on leave" to an attached permanent

### Player-scope
- Take an extra turn / skip next turn / skip untap / skip combat phases
- Hijack next turn (you control your opponent's next turn)
- Add an additional combat phase
- Play additional lands this turn
- Prevent land plays this turn
- Can't cast spells (target player, duration)
- Lose the game / win the game
- Any player may pay (Mind's Desire-style payment offer)
- Pay-or-suffer
- Secret bid
- Create permanent emblem
- Create permanent / duration-bounded global triggered ability
- Grant cast-from-graveyard-with-Forage permission

## Triggered abilities

- When this enters the battlefield (basic ETB shape, no extra conditions)
- ETB with a condition (`triggerCondition` slot, e.g. `Conditions.Void`, "if you cast it")
- ETB on any permanent / another creature / another permanent you control
- ETB on a land you control (Landfall)
- ETB on an enchantment you control (Eerie)
- ETB on a face-down creature
- When this leaves the battlefield
- When this dies / when any creature dies / when another creature dies / when one of your creatures dies
- When another creature with a given subtype dies
- When this or one of your creatures leaves the battlefield without dying
- When one or more creatures (or filtered cards) are put into your graveyard from your library
- When one or more cards matching a filter are put into your graveyard (batched)
- When one or more permanents matching a filter you control enter (batched)
- When one or more other creatures you control leave without dying (batched)
- When you sacrifice one or more permanents matching a filter (batched) / when this is sacrificed
- Combat: attacks / attacks alone / any attacks; you-attack; you-attack with one or more matching
- Combat: this blocks / a creature you control blocks
- Combat: becomes blocked / a creature you control becomes blocked / filtered-creature becomes blocked
- Combat: blocks-or-becomes-blocked-by a creature matching a filter
- Damage: deals damage / deals combat damage / deals combat damage to a player / to a creature / to player-or-planeswalker
- Damage: a creature you control deals combat damage to a player
- "Whenever a creature dealt damage by this creature dies"
- Damage: this is dealt damage / dealt damage by a creature / by a spell
- Phase/step: your upkeep / your draw step / each upkeep / each opponent's upkeep / your end step / each end step / begin combat / your precombat main / your postcombat main
- Enchanted creature's controller's upkeep and end step (ATTACHED binding)
- Casting triggers: you cast a spell / creature / noncreature / instant or sorcery / enchantment / historic / kicked / by subtype / instant-or-sorcery from hand
- "Whenever a player casts their Nth spell each turn"
- Card draw: you draw a card / any player draws / reveal-creature-from-first-draw
- Stack: a spell or ability is put on the stack (any player)
- Counters: you put one or more +1/+1 counters on a creature you control
- Tap / untap (self or attached)
- Cycling: you cycle this / you cycle / any player cycles
- Crime: you commit a crime
- Gift: you give a gift
- Transform: this transforms / into back face / into front face
- Targeting: becomes the target of a spell or ability / by an opponent / Valiant (first time each turn by you)
- Life: you gain life / any player gains life / you lose life / any player loses life / you gain-or-lose life
- Aura/equipment-relative: enchanted creature dies / enchanted permanent leaves / enchanted creature takes damage / enchanted creature deals combat damage to a player / enchanted creature attacks / enchanted creature deals damage (any) / enchanted creature turned face up / equipped creature attacks / equipped creature dies
- Control change: when you gain control of this permanent
- Turned face up (self) / a creature is turned face up (any controller)
- Door / Rooms (Duskmourn): when you unlock this door; when you fully unlock a Room
- Expend N (Bloomburrow): when you spend your Nth total mana on spells this turn

## Costs

### Spell / activation costs
- Mana (parsed from cost strings, including hybrid and Phyrexian as represented in `ManaCost`)
- Tap (`{T}`) and Untap (`{Q}`)
- Pay N life
- Discard a card / discard a filtered card / discard hand / discard self
- Sacrifice a permanent matching a filter / sacrifice another / sacrifice multiple / sacrifice self
- Sacrifice a chosen creature type
- Exile self / exile granting permanent
- Exile N cards from graveyard / exile X from graveyard
- Tap another permanent / tap N permanents matching a filter / tap X
- Tap attached creature
- Return a card to hand
- Remove N counters from self / remove X +1/+1 counters
- Loyalty change (planeswalker abilities)
- Forage (exile two cards from graveyard or return a creature from graveyard to library)
- Blight N
- Composite (combine the above)
- Free (no cost)

### Alternative / additional costs
- Kicker (mana and/or non-mana additional cost; multikicker repeats)
- Flashback (alternative cost from graveyard; exile on resolution)
- Evoke (alternative cost; triggers sacrifice on ETB)
- Warp (alternative cost from hand; exile at next end step; recast from exile)
- Morph (alternative cost to cast face-down; turn-face-up cost)
- Conspire (additional cost on cast: tap two creatures sharing a colour)

## Static abilities

- "Equipped creature gets +N/+N" / "Enchanted creature gets +N/+N" via static + filter
- "While condition, this has <keyword>" (`ConditionalStaticAbility`) — e.g. conditional deathtouch / hexproof during your turn
- Global static effects on combat (can't-attack, can't-block, taunt, blocking restrictions)
- Cost-reduction static abilities (Convoke, Delve, Affinity, "this spell costs {X} less to cast" via dynamic amounts)
- Continuous effect layering through `StateProjector` (CR 613 layers)
- Replacement effects: damage prevention, draw replacement, token-creation replacement, ETB-with-counters
- "Plays with the top card revealed" / "reveal-first-draw each turn"

## Game zones and state-based actions

- Battlefield, hand, library, graveyard, exile, command, stack
- Standard SBAs: creature dies with lethal damage / 0 toughness; planeswalker dies with 0 loyalty; legend rule; aura/equipment without legal target; player loses at 0 life or empty library on draw
- Linked exile (track cards exiled by a specific source for later return)
- Face-down cards (morph / manifest payload via `TurnFaceUp`)
- Transform (double-faced cards)

## Other

- Targeting validation with hexproof / protection / shroud / ward suppression
- Crime detection (cast / activate / target opponents or their stuff → "you committed a crime")
- Gift mechanic (track that you gave a gift to trigger gift-given effects)
- Mulligan flow (London mulligan: take-mulligan, keep-hand, bottom-cards)
- Priority passing, stack resolution, the standard turn structure
- Rooms (Duskmourn): two-faced enchantments with per-door unlock costs and ETB / unlock triggers
- Dynamic amounts: counts of permanents / cards in zone / mana value / life totals, used as inputs to effects, costs and conditions
- Conditional effects with a wide library of predicates (life thresholds, hand/graveyard sizes, control checks, target-shape checks, "was cast from hand", "blight was paid", "you attacked this turn", "void", etc.)
