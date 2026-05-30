# Edge of Eternities (EOE) - Missing Effects & Implementation Plan

Engine features required to implement the remaining EOE cards. Each section lists the cards it
unblocks, the exact oracle clause that can't be expressed with current primitives, and a sketch of
the engine/SDK work needed.

As of the latest pass, **238 / 261** booster cards are implemented. The cards below remain
blocked on the engine features listed here. Cards whose every clause maps to an existing primitive
have already been implemented and are not listed.

---

## 1. Conditional draw-replacement static

**Cards:** Quantum Riddler.

**Clause:** "As long as you have one or fewer cards in hand, if you would draw one or more cards,
you draw that many cards plus one instead."

**Plan:** Add a continuous draw-replacement static ability that intercepts draw events for its
controller, adds a fixed amount, and is gated by a `Condition` (hand-size). The flying body, the
ETB "draw a card", and `Warp {1}{U}` are all already expressible — only the replacement static is
missing.

---

## 2. "Was dealt damage this turn" tracking

**Cards:** Faller's Faithful.

**Clause:** "If that creature wasn't dealt damage this turn, its controller draws two cards."

**Plan:** Track per-permanent "damaged this turn" state (cleared at cleanup) and expose it as a
`Condition` usable on a context target. The ETB "destroy up to one target creature" is already
expressible; only the damage-history conditional is missing.

---

## 3. Excess-damage detection

**Cards:** Orbital Plunge.

**Clause:** "If excess damage was dealt this way, create a Lander token."

**Plan:** Add a way for a damage effect to report whether it dealt damage in excess of lethal
(toughness minus marked damage), surfaced as a condition or follow-up trigger. The base "deals 6
damage to target creature" and `Effects.CreateLander()` already exist.

---

## 4. Targeting cards in the warp-exile zone

**Cards:** Blade of the Swarm.

**Clause:** "Put target exiled card with warp on the bottom of its owner's library."

**Plan:** Make cards exiled with warp targetable: a `TargetFilter` for "exiled card with warp" plus
a move-to-library-bottom effect for an exile-zone target. The modal "two +1/+1 counters" branch is
already expressible.

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

## 6. `DynamicAmount` = mana spent to cast (usable in ETB replacement)

**Cards:** Dyadrine, Synthesis Amalgam.

**Clause:** "Dyadrine enters with a number of +1/+1 counters on it equal to the amount of mana spent
to cast it."

**Plan:** Persist `manaSpentToCast` on the permanent and expose a `DynamicAmount` that
`EntersWithDynamicCounters` can read (today `TotalManaSpent` is only meaningful during cast
resolution). The attack ability ("remove a +1/+1 counter from each of two creatures you control;
if you do, draw a card and create a Robot token") is close to expressible but depends on a
"remove a counter from each of N chosen creatures" cost/effect — verify before implementing.

---

## 7. Characteristic-defining P/T from cards in exile

**Cards:** Cosmogoyf.

**Clause:** "Cosmogoyf's power is equal to the number of cards you own in exile and its toughness is
equal to that number plus 1."

**Plan:** Add a `DynamicAmount` that counts cards a player owns across all exile sub-zones, then use
it as a characteristic-defining P/T (Layer 7b set-base from a dynamic value, applied in all zones).

---

## 7b. Land: "sacrifice unless you tap an untapped permanent"

**Cards:** Command Bridge.

**Clause:** "When this land enters, sacrifice it unless you tap an untapped permanent you control."

**Plan:** Add an ETB "sacrifice this unless you pay <cost>" intervening-choice effect where the
alternative cost is tapping an untapped permanent you control. The "enters tapped" and the
any-color mana ability are already expressible.

---

## 9. Impulse "exile from top until a nonland card, you may cast it this turn"

**Cards:** Territorial Bruntar.

**Clause:** "Whenever a land you control enters, exile cards from the top of your library until you
exile a nonland card. You may cast that card this turn."

**Plan:** Add an effect/pattern that exiles from the top until a nonland card is hit and grants
may-play-this-turn on it (`GrantMayPlayFromExileEffect`). `ExileFromTopRepeatingEffect` puts the
card into hand and doesn't grant impulse, so it can't be reused. Reach and the landfall trigger are
already expressible.

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

## 11. Noncreature artifact tokens with custom names + embedded triggered abilities

**Cards:** Weapons Manufacturing.

**Clause:** "create a colorless artifact token named Munitions with 'When this token leaves the
battlefield, it deals 2 damage to any target.'"

**Plan:** `CreateTokenExecutor` hardcodes "Creature" into every token's type line, so it can only make
creature tokens. Either (a) add a predefined "Munitions" token CardDefinition (noncreature artifact
with the leaves-battlefield damage trigger) to `PredefinedTokens.kt` and create it via
`CreatePredefinedTokenEffect`, or (b) generalize token creation to allow a noncreature type line +
embedded triggered abilities. The "whenever a nontoken artifact you control enters" trigger is
already expressible.

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

## 14. Affinity granted to the spells you cast / affinity for a subtype

**Cards:** Sami, Wildcat Captain (affinity for artifacts on all your spells);
Thrumming Hivepool (affinity for Slivers on itself).

**Clause:** "Spells you cast have affinity for artifacts." / "Affinity for Slivers."

**Plan:** Affinity for artifacts exists as a per-card keyword, but (a) granting it to *all spells a
player casts* via a static, and (b) an affinity-for-`<subtype>` variant, are both missing. Sami's
double strike + vigilance and Hivepool's Sliver anthem / upkeep Sliver tokens are already
expressible.

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

## 17. Once-per-turn gating for triggered abilities (counters-placed → draw)

**Cards:** Terrasymbiosis.

**Clause:** "Whenever you put one or more +1/+1 counters on a creature you control, you may draw that
many cards. Do this only once each turn."

**Plan:** The trigger and "draw that many"
(`ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT`) likely exist; the gap is a generic
"do this only once each turn" limiter on a triggered ability.

---

## 18. Copy a card from a zone and cast the copy for free

**Cards:** Roving Actuator.

**Clause:** "exile up to one target instant or sorcery card with mana value 2 or less from your
graveyard. Copy it. You may cast the copy without paying its mana cost."

**Plan:** Add an effect that creates a copy of a *card* (not a spell on the stack) and offers to cast
the copy without paying its cost. The Void trigger gate is already expressible.

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

## 21. Reanimate as a typed, ability-stripped permanent

**Cards:** Xu-Ifit, Osteoharmonist.

**Clause:** "Return target creature card from your graveyard to the battlefield. It's a Skeleton in
addition to its other types and has no abilities."

**Plan:** Add a continuous effect, tied to the reanimated permanent, that adds the Skeleton subtype
(layer 4) and removes all abilities (layer 6). Plain reanimation already exists.

---

## 22. Odd/even mass destroy + mass temporary control of all creatures

**Cards:** Mutinous Massacre.

**Clause:** "Choose odd or even. Destroy each creature with mana value of the chosen quality. Then
gain control of all creatures until end of turn. Untap them. They gain haste until end of turn."

**Plan:** Add (a) an odd/even mana-value filter (parity predicate) for the destroy, and (b) mass
"gain control of all creatures until end of turn" (today `Effects.GainControl` targets a single
permanent). Untap-all + grant-haste-all are expressible.

---

## 23. Multi-group graveyard return with split destinations

**Cards:** Pull Through the Weft.

**Clause:** "Return up to two target nonland permanent cards from your graveyard to your hand, then
return up to two target land cards from your graveyard to the battlefield tapped."

**Plan:** A single spell with two independent "up to two target" graveyard selections that route to
*different* zones (hand vs. battlefield-tapped). Single-group graveyard return to one zone exists
(e.g. Scout for Survivors); the dual-group/dual-destination shape needs verification or a new
multi-target pattern.

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

---

## Already covered elsewhere

Poison counters (Virulent Silencer), token doubling (Exalted Sunborn), devour-land (Famished
Worldsire), "mana spent" for X-counters (Astelli Reclaimer), and "your next turn" delayed-trigger
timing (Kav Landseeker — `CreateDelayedTriggerEffect.timing = DelayedTriggerTiming.NEXT_TURN`) were previously listed
here and are now implemented; their entries have been removed.
