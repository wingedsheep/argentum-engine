# Edge of Eternities (EOE) - Engine Features Shipped

Historical log of the engine/SDK features that EOE forced. The set is complete (266 / 266
implemented); every entry below is resolved and the listed card now ships. Section numbers are
preserved from earlier revisions so external references stay stable.

---

## 12. Opponent exiles from hand and may play it (with cost/tapped modifiers)

**Card:** Lightstall Inquisitor.

**Clause:** "each opponent exiles a card from their hand and may play that card for as long as it
remains exiled. Each spell cast this way costs {1} more to cast. Each land played this way enters
tapped."

**Shipped:** Opponent-targeted "exile a card from hand, grant the *owner* may-play from exile"
effect, plus a scoped cost increase on those specific cards and a scoped lands-enter-tapped
modifier.

---

## 13. Token-creation replacement: copies of a chosen permanent (once per turn)

**Card:** Moonlit Meditation.

**Clause:** "The first time you would create one or more tokens each turn, you may instead create
that many tokens that are copies of enchanted permanent."

**Shipped:** Replacement effect on `CreateTokenEffect` with once-per-turn gating and an
aura-attached "copy of enchanted permanent" token source.

---

## 15. Grant the Warp alternative-cast cost to cards in hand

**Card:** Tannuk, Steadfast Second.

**Clause:** "Artifact cards and red creature cards in your hand have warp {2}{R}."

**Shipped:** Static that grants the Warp keyword (with a specified cost) to cards in hand matching
a filter.

---

## 16. Put a permanent from hand, then grant it arbitrary abilities

**Card:** Terminal Velocity.

**Clause:** "You may put an artifact or creature card from your hand onto the battlefield. That
permanent gains haste, 'When this permanent leaves the battlefield, it deals damage equal to its
mana value to each creature,' and 'At the beginning of your end step, sacrifice this permanent.'"

**Shipped:** Grant a bundle of abilities (triggered LTB + self-sac end-step trigger + haste) to a
specific permanent put onto the battlefield.

---

## 19. Cast spells from the top of your library (type-filtered) + restricted mana

**Card:** Mm'menon, the Right Hand.

**Clause:** "You may cast artifact spells from the top of your library." + "Artifacts you control
have '{T}: Add {U}. Spend this mana only to cast a spell from anywhere other than your hand.'"

**Shipped:** (a) Static "may cast cards of type X from the top of your library", (b) restricted
mana that can only pay for spells cast from non-hand zones, and (c) "look at the top card any
time".

---

## 20. "First spell each turn may be cast without paying its mana cost"

**Card:** Weftwalking.

**Clause:** "The first spell each player casts during each of their turns may be cast without
paying its mana cost."

**Shipped:** Static granting free-cast permission to the first spell each player casts on each of
their own turns (per-player, per-turn gate).

---

## 24. Planeswalker emblem + "becomes a 0/0 Robot artifact creature"

**Card:** Tezzeret, Cruel Captain.

**Clause:** "−7: You get an emblem with 'At the beginning of combat on your turn, put three +1/+1
counters on target artifact you control. If it's not a creature, it becomes a 0/0 Robot artifact
creature.'"

**Shipped:** Emblem creation with a recurring combat-trigger ability, plus a "becomes a 0/0 Robot
artifact creature" type-set on a noncreature artifact.
