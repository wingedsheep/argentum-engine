# Edge of Eternities (EOE) - Missing Effects & Implementation Plan

Engine features required to implement the remaining EOE cards. Each section lists the cards it
unblocks, the exact oracle clause that can't be expressed with current primitives, and a sketch of
the engine/SDK work needed.

As of the latest pass, **252 / 261** booster cards are implemented. The cards below remain
blocked on the engine features listed here. Cards whose every clause maps to an existing primitive
have already been implemented and are not listed. Section numbers are preserved from earlier
revisions of this document so that [`problem-cards.md`](problem-cards.md) cross-references stay
stable; gaps in the numbering correspond to features that have since been resolved.

---

## 5. Continuous "your creatures enter with extra counters" + lands-entered-this-turn tracker

**Cards:** Bioengineered Future.

**Clause:** "Each creature you control enters with an additional +1/+1 counter on it for each land
that entered the battlefield under your control this turn."

**Plan:** (a) Add a `TurnTracker`/`DynamicAmount` for "lands that entered under your control this
turn". (b) Add a continuous static replacement that adds ETB +1/+1 counters to *other* creatures
you control (not just self), with a dynamic count. The "create a Lander" ETB is already
expressible.

---

## 10. Dynamic-toughness mass destroy + reanimate-from-batch

**Cards:** Zero Point Ballad.

**Clause:** "Destroy all creatures with toughness X or less. ... If X is 6 or more, return a creature
card put into a graveyard this way to the battlefield under your control."

**Plan:** (a) `GameObjectFilter.toughnessAtMost` only accepts a fixed `Int`; add a dynamic-threshold
variant (`toughnessAtMost(DynamicAmount)`) so the destroy filter can read X. (b) Track which
creatures were put into a graveyard by this destruction and allow selecting one to return when
X ≥ 6. "Lose X life" is already expressible.

---

## 12. Opponent exiles from hand and may play it (with cost/tapped modifiers)

**Cards:** Lightstall Inquisitor.

**Clause:** "each opponent exiles a card from their hand and may play that card for as long as it
remains exiled. Each spell cast this way costs {1} more to cast. Each land played this way enters
tapped."

**Plan:** Add an opponent-targeted "exile a card from hand, grant the *owner* may-play from exile"
effect, plus a cost increase on those specific cards and a lands-enter-tapped modifier scoped to
them. Vigilance is already expressible.

---

## 13. Token-creation replacement: copies of a chosen permanent (once per turn)

**Cards:** Moonlit Meditation.

**Clause:** "The first time you would create one or more tokens each turn, you may instead create
that many tokens that are copies of enchanted permanent."

**Plan:** Add a replacement effect on `CreateTokenEffect` that, once per turn, optionally swaps the
created tokens for copies of the enchanted permanent. Requires once-per-turn gating and an
aura-attached "copy of enchanted permanent" token source.

---

## 15. Grant the Warp alternative-cast cost to cards in hand

**Cards:** Tannuk, Steadfast Second.

**Clause:** "Artifact cards and red creature cards in your hand have warp {2}{R}."

**Plan:** Add a static that grants the Warp keyword (with a specified cost) to cards in hand matching
a filter. "Other creatures you control have haste" is already expressible.

---

## 16. Put a permanent from hand, then grant it arbitrary abilities

**Cards:** Terminal Velocity.

**Clause:** "You may put an artifact or creature card from your hand onto the battlefield. That
permanent gains haste, 'When this permanent leaves the battlefield, it deals damage equal to its
mana value to each creature,' and 'At the beginning of your end step, sacrifice this permanent.'"

**Plan:** Add a way to grant a bundle of abilities (a triggered LTB ability + a self-sac end-step
trigger + haste) to a specific permanent put onto the battlefield. Put-from-hand exists; granting a
*new triggered ability* to a target permanent does not.

---

## 19. Cast spells from the top of your library (type-filtered) + restricted mana

**Cards:** Mm'menon, the Right Hand.

**Clause:** "You may cast artifact spells from the top of your library." + "Artifacts you control
have '{T}: Add {U}. Spend this mana only to cast a spell from anywhere other than your hand.'"

**Plan:** Add (a) a static "may cast cards of type X from the top of your library", (b) restricted
mana that can only pay for spells cast from non-hand zones, and (c) "look at the top card any time".
Flying is already expressible.

---

## 20. "First spell each turn may be cast without paying its mana cost"

**Cards:** Weftwalking.

**Clause:** "The first spell each player casts during each of their turns may be cast without paying
its mana cost."

**Plan:** Add a static that grants free-cast permission to the first spell each player casts on each
of their own turns (per-player, per-turn gate). The ETB "shuffle hand and graveyard into library,
then draw seven" is already expressible.

---


## 24. Planeswalker emblem + "becomes a 0/0 Robot artifact creature"

**Cards:** Tezzeret, Cruel Captain.

**Clause:** "−7: You get an emblem with 'At the beginning of combat on your turn, put three +1/+1
counters on target artifact you control. If it's not a creature, it becomes a 0/0 Robot artifact
creature.'"

**Plan:** Planeswalkers and loyalty abilities are supported. The gaps are (a) emblem creation with a
recurring combat-trigger ability, and (b) a "becomes a 0/0 Robot artifact creature" type-set on a
noncreature artifact. The passive ("whenever an artifact you control enters, add a loyalty
counter"), the 0 untap, and the −3 tutor are expressible.
