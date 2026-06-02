# Teenage Mutant Ninja Turtles (TMT) - Mechanics

**Set:** Teenage Mutant Ninja Turtles (`tmt`) · Released 2026-03-06
**Card pool:** 190 non-basic cards.

Counts below are cards in the set that use the mechanic. A card may appear under
multiple entries (e.g. a creature with Flying + Sneak).

**Implementation progress (27 / 190).** Mechanics already exercised by the cards
landed so far: Flying, Vigilance, Trample, Haste, Flash, Deathtouch, Double
strike, Reach (granted), Equip, ETB triggers, LTB triggers, begin-of-combat
triggers, attack triggers, intervening-if conditions, exile-until-leaves,
filtered "another permanent enters" triggers, Food artifact baseline, EntersTapped
land replacement, multi-color mana abilities, GatherCards/SelectFromCollection/
MoveCollection pipelines, scry, modal Or-predicate filters, ForEachTarget +
ContextTarget, and the standard sacrifice-for-life/sac-for-draw Food activations.
None of the **new TMT mechanics** (Sneak, Alliance, Disappear) are exercised yet —
they remain blocked on the Gap A / B / C engine work documented in TODO.md.

---

## New mechanics in TMT

### Sneak — 26 cards
Alternative cost keyword. Reminder text:

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
