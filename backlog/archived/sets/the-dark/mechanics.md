# The Dark (DRK) — Mechanics

The Dark (August 1994) is a **punishing, land-conscious set**: 119 cards built around effects that
cost their own controller life or damage, payoffs keyed to which basic land types are on the
battlefield, and upkeep taxes that eat the permanent unless you keep paying. It names almost no
keywords beyond the 1994 evergreens — the mechanics that carry it are templates, not keywords, so
most sections below are themes rather than `Keyword` entries.

Every mechanic is listed with the cards that use it, ordered by card count. **A ticked box means the
engine already models it** (an SDK primitive exists, and where possible a DRK card in the corpus
already uses it). An unticked box is `add-feature` work, and the cards under it are blocked on it.

Card data: a full Scryfall dump of the set (`e:drk`, `unique=prints`), read card by card. Rule
numbers verified against `MagicCompRules_20260808.txt`, and every SDK primitive named below was
checked against `docs/card-sdk-language-reference.md` and the `Keyword` enum rather than recalled.
Progress lives in [`cards.md`](cards.md).

---

## - [x] "It costs you too" — self-damage and life as a price (16 cards)

The set's defining axis. A DRK card rarely gives you an effect outright: it deals you damage, makes
you pay life, or hits both players symmetrically. Mechanically ordinary — damage to the controller
and life payment are basic vocabulary — but it's the reason the set plays the way it does, and it's
why so many cards need a "damage to you" clause wired into an otherwise plain effect.

**Engine support:** ✅ `DealDamageEffect` to `EffectTarget.Controller`, `Costs.pay.PayLife` (CR 119.4).
Implemented in-set: Inferno, Book of Rass, Standing Stones.

Cards: Ashes to Ashes, Banshee, Book of Rass, Brothers of Fire, Electric Eel, Elves of Deep Shadow,
Eternal Flame, Fire and Brimstone, Inferno, Mana Clash, Mind Bomb, Nameless Race, Season of the
Witch, Sorrow's Path, Standing Stones, Wormwood Treefolk

> Banshee, Eternal Flame and Dark Sphere want **half of X, rounded**. `DynamicAmount.Divide(…,
> roundUp = true/false)` covers it; Dark Sphere's is the awkward one (see prevention, below).

## - [x] Basic land types matter (14 cards)

Payoffs read the battlefield for a *basic land type* — yours or an opponent's — rather than for
colors or permanents. It runs both ways: cards that grow off your Forests/Mountains, cards that
punish or require an opponent's Islands/Swamps, and cards that change what a land *is*.

**Engine support:** ✅ counting via `DynamicAmounts` battlefield aggregates; `SetLandTypesForGroup`
for Blood Moon's "nonbasic lands are Mountains" (CR 305.7, layer 4); `Costs.pay.Sacrifice(filter)`
for the "sacrifice two Islands" costs. Implemented in-set: Water Wurm, People of the Woods, Dark
Heart of the Wood.

Cards: Angry Mob, Blood Moon, Dark Heart of the Wood, Deep Water, Eternal Flame, Gaea's Touch,
Giant Shark, Goblin Caves, Goblin Rock Sled, Goblin Shrine, Leviathan, People of the Woods, Psychic
Allergy, Water Wurm

> Deep Water ("if you tap a land for mana, it produces {U} instead") is a mana-production
> replacement, not a count — check it against the mana-replacement vocabulary before batching it
> with the rest.

## - [x] Goblin, Orc and Dwarf tribal (11 cards)

The Dark is where Goblins first become a deck. A Goblin lord that cheats Goblins into play, two
Auras that pump Goblins while enchanting a basic Mountain, an Orc sacrifice engine, and a white
sweeper printed to answer the whole thing.

**Engine support:** ✅ subtype filters (`GameObjectFilter` on creature type), `Effects.DestroyAll`
with a subtype filter, `Costs.pay.SacrificeAnother(filter)`. Implemented in-set: Tivadar's Crusade,
Goblin Digging Team, Goblin Hero, Marsh Goblins, Scarwood Goblins.

Cards: Goblin Caves, Goblin Digging Team, Goblin Hero, Goblin Rock Sled, Goblin Shrine, Goblin
Wizard, Goblins of the Flarg, Marsh Goblins, Orc General, Scarwood Goblins, Tivadar's Crusade

## - [x] Upkeep tax — "sacrifice this unless you pay" (10 cards)

The set's signature drawback template: a triggered ability at upkeep that eats the permanent unless
its controller pays mana, life, or lands. Variants tax the *opponent* (Curse Artifact, Erosion),
tax every player (Mana Vortex, Worms of the Earth), or self-destruct on a counter clock (Fasting).

**Engine support:** ✅ upkeep triggers (`EachOpponentUpkeep`, controller upkeep) composed with
`Costs.pay.Mana` / `Costs.pay.PayLife` / `Costs.pay.Sacrifice` in the "unless you pay" shape.
Implemented in-set: Sunken City.

Cards: Curse Artifact, Dance of Many, Erosion, Fasting, Leviathan, Mana Vortex, Psychic Allergy,
Season of the Witch, Sunken City, Worms of the Earth

> Two are more than a tax. **Fasting** replaces your draw step ("if you would begin your draw step,
> you may skip that step instead") — `Effects.SkipNextDrawStep` is a one-shot marker, not this
> optional per-turn replacement. **Worms of the Earth** is a permanent "players can't play lands"
> *and* "lands can't enter the battlefield" static; `Effects.CantPlayLandsThisTurn` is the
> turn-scoped one-shot, and the entry restriction has no counterpart at all. Both are feature work.

## - [x] Landwalk — printed, granted, and cared about (9 cards)

CR 702.14. Three creatures print it, five cards *grant* it (including two that grant it to a
creature they don't control, and one that grants itself two different types), and one card destroys
creatures that have it.

**Engine support:** ✅ `Keyword.FORESTWALK` / `SWAMPWALK` / `MOUNTAINWALK` / `ISLANDWALK`;
`GrantLandwalkOfChosenType` for the aura-granted shape. Implemented in-set: Marsh Goblins, Hidden
Path, Cave People, Merfolk Assassin.

Cards: Cave People, Goblins of the Flarg, Hidden Path, Marsh Goblins, Merfolk Assassin, Scarwood
Bandits, Scarwood Hag, War Barge, Wormwood Treefolk

> Scarwood Hag also *removes* forestwalk ("target creature loses forestwalk until end of turn") —
> a keyword-removal grant, not a grant.

## - [x] Graveyard as a resource, and graveyard hate (8 cards)

Exiling cards out of graveyards is both a cost (Frankenstein's Monster, Necropolis, Eater of the
Dead) and an attack (Tormod's Crypt, Grave Robbers). Nameless Race reads opponents' graveyards for
its P/T; Skull of Orm buys enchantments back.

**Engine support:** ✅ `Costs.pay.Exile(zone = GRAVEYARD)`, graveyard-scoped targets and counts.
Implemented in-set: Skull of Orm, Grave Robbers.

Cards: Eater of the Dead, Frankenstein's Monster, Grave Robbers, Nameless Race, Necropolis, Skull of
Orm, Tormod's Crypt, Whippoorwill

> Frankenstein's Monster is the hard one: it exiles X creature cards **as it enters**, fails to
> enter at all if it can't, and takes a player-chosen mix of +2/+0, +1/+1 and +0/+2 counters — one
> choice per card exiled.

## - [x] Color hate (8 cards)

1994-style hosers, aimed overwhelmingly at white (the block's Church-of-Tal flavor) and answered
in kind. Two read a *chosen* color rather than a fixed one.

**Engine support:** ✅ color filters on targets, group destruction and static P/T modification;
`ChooseColorForTarget` for Psychic Allergy's entering color choice. Implemented in-set: Exorcist, Holy
Light, Knights of Thorn, Riptide.

Cards: Exorcist, Holy Light, Inquisition, Knights of Thorn, Martyr's Cry, Nameless Race, Psychic
Allergy, Riptide

## - [x] Regeneration, and turning it off (8 cards)

CR 701.19. Four creatures regenerate (three of them for mana of a color they aren't), one grants it
to any creature, and three cards specifically shut it off — Fissure's "can't be regenerated",
Runesword's and War Barge's "destroyed this way can't be regenerated".

**Engine support:** ✅ `RegenerateEffect`, and `Effects.Destroy(noRegenerate = true)` /
`CantBeRegeneratedEffect` for the shut-off side. Implemented in-set: Diabolic Machine, Drowned,
Ghost Ship, Niall Silvain, Fissure.

Cards: Diabolic Machine, Drowned, Fissure, Ghost Ship, Niall Silvain, Runesword, War Barge,
Whippoorwill

## - [x] Auras — "Enchant" (7 cards)

CR 702.5. Every DRK Aura is a drawback or a punisher rather than a buff: three enchant a land, two
enchant a creature to restrain it, one enchants an artifact to burn its controller.

**Engine support:** ✅ `auraTarget: TargetRequirement` on the card definition, with conditional
statics for the enchanted permanent. **No DRK Aura is implemented yet** — this whole section is
unbuilt card work, not blocked engine work.

Cards: Brainwash, Curse Artifact, Erosion, Goblin Caves, Goblin Shrine, Tangle Kelp, Venom

> Goblin Caves and Goblin Shrine both gate their lord effect on "as long as enchanted land is a
> **basic** Mountain", and Goblin Shrine adds a leaves-the-battlefield trigger that damages every
> Goblin. Build them together — they're the same card twice.

## - [x] Damage prevention and redirection (6 cards)

CR 615. A prevention-heavy set even by 1994 standards, and the area where the engine has been
extended most recently for DRK (Maze of Ith, and Scarecrow's controller-named shield).

**Engine support:** ✅ `PreventDamageEffect` — `recipientGroup` / `sourceFilter` / `scope` /
`nextInstanceOnly` cover every shape here — plus `DamageCantBePreventedThisTurn` for Whippoorwill's
anti-prevention clause. Implemented in-set: Maze of Ith, Scarecrow.

Cards: Blood of the Martyr, Dark Sphere, Maze of Ith, Scarecrow, Uncle Istvan, Whippoorwill

> Two open sub-cases. **Dark Sphere** prevents *half* the next instance from a chosen source,
> rounded down — the shield takes a fixed/dynamic `amount`, so this needs the amount computed from
> the incoming damage rather than at resolution. **Blood of the Martyr** redirects damage from *any*
> creature to you — a controller-wide redirection replacement, not a shield.

## - [x] Wall-matters (6 cards)

Walls are a real card type consideration here: two cards can't be blocked by them, one grants that,
one destroys them, and the set prints two of its own.

**Engine support:** ✅ subtype-filtered blocking restrictions and targets. Implemented in-set: Bog
Rats, Goblin Digging Team, Carnivorous Plant.

Cards: Bog Rats, Carnivorous Plant, Goblin Digging Team, Necropolis, Tower of Coireall, Venom

> Venom's "non-Wall creature" clause is the negated form of the same filter — its destruction
> trigger deliberately doesn't fire against Walls.

## - [x] Land destruction and denial (6 cards)

Symmetric, expensive and often permanent: a one-sided sweeper that lets players buy out with life,
an instant that kills a creature *or* a land, and three enchantments that grind the whole
battlefield's lands down.

**Engine support:** ✅ `Effects.Destroy` / `DestroyAll` on land filters, "unless any player pays"
composed from `Costs.pay.PayLife`. Implemented in-set: Fissure.

Cards: Blood Moon, Cleansing, Erosion, Fissure, Mana Vortex, Worms of the Earth

> Worms of the Earth's entry restriction is the blocker noted under the upkeep-tax section; the
> rest of this group is card work.

## - [x] Trample (5 cards)

CR 702.19.

**Engine support:** ✅ `Keyword.TRAMPLE`. Implemented in-set: Ball Lightning.

Cards: Angry Mob, Ball Lightning, Goblin Rock Sled, Leviathan, Nameless Race

> Giant Shark *gains* trample conditionally, off a "blocks or becomes blocked by a creature that has
> been dealt damage this turn" trigger.

## - [x] Hand disruption (5 cards)

Reveal-and-discard, discard-at-random, and two cards that convert the revealed hand into damage.

**Engine support:** ✅ reveal + discard effects, `DynamicAmount` over revealed cards. **None
implemented in-set yet.**

Cards: Amnesia, Inquisition, Mind Bomb, Rag Man, Wand of Ith

> Mind Bomb is a *may*-discard-up-to-three with damage equal to 3 minus what each player discarded —
> a per-player optional choice feeding a per-player amount.

## - [x] Artifact and Aura removal (4 cards)

Cheap, narrow answers rather than a removal suite: two green one-drops that eat an Aura or an
artifact by sacrificing themselves, a white one-drop that pops an Aura off your own creature, and a
sorcery that exiles two artifacts at once.

**Engine support:** ✅ `Effects.Destroy` / `Effects.Exile` over artifact and Aura filters,
`Costs.pay.Sacrifice` for the self-sacrificing activations. Implemented in-set: Scavenger Folk.

Cards: Dust to Dust, Miracle Worker, Savaen Elves, Scavenger Folk

> Miracle Worker and Savaen Elves both restrict *which* Aura — attached to a creature you control,
> attached to a land — so the filter is on the Aura's attachment, not on the Aura.

## - [x] Flying (3 cards)

CR 702.9.

**Engine support:** ✅ `Keyword.FLYING`. Implemented in-set: Bog Imp, Fire Drake, Ghost Ship.

Cards: Bog Imp, Fire Drake, Ghost Ship

> Scarecrow is the payoff on the other side — it prevents all damage fliers would deal to you.

## - [x] Duration-bound control theft (2 cards)

Both DRK steal effects last only while a condition holds, rather than permanently or until end of
turn — Preacher for as long as it stays tapped, Scarwood Bandits for as long as it stays on the
battlefield.

**Engine support:** ✅ `GainControlEffect(target, duration)` with `Duration.WhileSourceTapped`
(Callous Oppressor's shape) and `Duration.WhileSourceOnBattlefield`.

Cards: Preacher, Scarwood Bandits

> Preacher's target is chosen by the **opponent** ("target creature of an opponent's choice they
> control") — an opponent-chosen target on your own ability. Verify that before building it; it is
> the one genuinely unusual thing about the card.

## - [x] Banding (2 cards)

CR 702.22. The set's only combat keyword of note, on two white creatures.

**Engine support:** ✅ `Keyword.BANDING`. Implemented in-set: Knights of Thorn, Pikemen.

Cards: Knights of Thorn, Pikemen

## - [x] Defender (2 cards)

CR 702.3 — printed as "Wall" in 1994, oracle-updated to Defender.

**Engine support:** ✅ `Keyword.DEFENDER`. Implemented in-set: Carnivorous Plant.

Cards: Carnivorous Plant, Necropolis

## - [x] First strike (2 cards)

CR 702.7.

**Engine support:** ✅ `Keyword.FIRST_STRIKE`. Implemented in-set: Land Leeches, Pikemen.

Cards: Land Leeches, Pikemen

> Spitting Slug grants it — to itself for {1}{G}, or to *everything blocking or blocked by it* if
> you decline.

## - [x] Protection (2 cards)

CR 702.16 — one printed, one granted.

**Engine support:** ✅ `Keyword.PROTECTION`. Implemented in-set: Knights of Thorn.

Cards: Goblin Wizard, Knights of Thorn

## - [x] Haste (1 card)

CR 702.10.

**Engine support:** ✅ `Keyword.HASTE`. Implemented in-set: Ball Lightning.

Cards: Ball Lightning

## - [x] Poison counters (1 card)

CR 104.3d — ten poison counters lose the game. Marsh Viper predates infect and toxic: it simply
gives two poison counters whenever it damages a player.

**Engine support:** ✅ player-scoped counters (CR 122.1); a fixed grant off a damage trigger needs
no keyword vocabulary.

Cards: Marsh Viper

## - [x] Storage counters (1 card)

City of Shadows exiles your own creatures to accumulate storage counters, then taps for one
colorless mana per counter.

**Engine support:** ✅ `Counters.STORAGE` + `AddMana(amount = DynamicAmounts.countersOnSelf(…))`.
Confirm `Costs.pay.Exile` accepts the battlefield zone for the "exile a creature you control"
activation cost — that's the only unproven piece.

Cards: City of Shadows

## - [x] Coin flips with an unbounded repeat (1 card)

CR 705. Mana Clash flips for both players and **repeats until both coins come up heads on the same
flip**, dealing damage each round.

**Engine support:** ✅ `Effects.RepeatWhile` is the repeat-while primitive this needed. It is a
do-while: the body runs once, then `RepeatCondition.WhileCondition` is re-evaluated after each pass,
and `RepeatWhileExecutor` recurses with `resolutionDepth + 1`, so the only bound is
`GameLimits.MAX_RESOLUTION_DEPTH` (500) — unreachable in practice for this card. Mana Clash uses two
separate one-coin `FlipCoins(storeHeadsAs = …)` calls so each player's result stays distinguishable,
deals 1 damage per tails, and exits only on a simultaneous double-heads.

Known nit, not a gap: `FlipCoinsExecutor` uses `flipperId = context.controllerId`, so the opponent's
coin is flipped by you. Observable only with a Krark's Thumb-style flip replacement or a "whenever a
player flips a coin" trigger, neither of which exists alongside this card today.

Cards: Mana Clash

---

## Cards outside every mechanic above

23 cards belong to no group — one-off effects and plain permanents rather than a shared template.
Listing them keeps the map honest about its own coverage: 96 of 119 cards are accounted for above.

French vanilla: Squire. (Goblin Hero and Scarwood Goblins are French vanilla too, but they appear
above under Goblin tribal.)

One-offs: Apprentice Wizard, Barl's Cage, Bone Flute, Coal Golem, Fellwar Stone, Festival, Flood,
Fountain of Youth, Living Armor, Lurker, Marsh Gas, Morale, Murk Dwellers, Reflecting Mirror, Safe
Haven, Sisters of the Flame, Spitting Slug, Stone Calendar, The Fallen, Tracker, Witch Hunter, Word
of Binding.

> Two of those are worth a look before anyone batches them as easy. **Reflecting Mirror** changes
> the target of a spell that targets you, for a cost of twice that spell's mana value. **Safe Haven**
> exiles your creatures and returns them all when you sacrifice it — a card-linked exile zone.
