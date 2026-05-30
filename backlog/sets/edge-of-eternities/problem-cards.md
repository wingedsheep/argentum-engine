https://scryfall.com/sets/eoe?order=name&as=grid
https://api.scryfall.com/cards/named?exact=Beyond%20the%20Quiet&set=eoe

# Problem Cards

## Status: cards still blocked on engine work (19)

The booster set is at **242 / 261**. The cards below are the only unimplemented ones; each is
blocked on a missing engine/SDK feature. The blocking clause and the engine change needed are
summarized here and detailed in [`missing-effects.md`](missing-effects.md) (section numbers in
parentheses).

| Card | Blocking clause | Engine change needed (missing-effects §) |
|------|-----------------|-------------------------------------------|
| Quantum Riddler | "if you would draw one or more cards, you draw that many plus one instead" while hand ≤ 1 | Conditional draw-replacement static (§1) |
| Orbital Plunge | "If excess damage was dealt this way, create a Lander token" | Excess-damage detection (§3) |
| Blade of the Swarm | "Put target exiled card with warp on the bottom of its owner's library" | Targeting warp-exiled cards (§4) |
| Bioengineered Future | "Each creature you control enters with an additional +1/+1 counter ... for each land that entered ... this turn" | Continuous extra-ETB-counters + lands-entered-this-turn tracker (§5) |
| Cosmogoyf | power/toughness = "number of cards you own in exile" (+1) | CDA P/T from cards-in-exile count (§7) |
| Territorial Bruntar | "exile cards from the top ... until you exile a nonland card. You may cast that card this turn" | Impulse-until-nonland effect (§9) |
| Zero Point Ballad | "Destroy all creatures with toughness X or less" + reanimate one destroyed this way | Dynamic-toughness mass destroy + reanimate-from-batch (§10) |
| Weapons Manufacturing | create the noncreature "Munitions" artifact token with a leaves-battlefield damage trigger | Noncreature tokens with embedded triggers (§11) |
| Lightstall Inquisitor | "each opponent exiles a card from their hand and may play that card ..." (+cost/tapped) | Opponent exile-from-hand-may-play with modifiers (§12) |
| Moonlit Meditation | "instead create that many tokens that are copies of enchanted permanent" (once/turn) | Token-creation replacement → copies (§13) |
| Tannuk, Steadfast Second | "Artifact cards and red creature cards in your hand have warp {2}{R}" | Grant Warp to hand cards (§15) |
| Terminal Velocity | put a permanent from hand and grant it haste + an LTB trigger + an end-step self-sac | Grant arbitrary abilities to a put-in permanent (§16) |
| Terrasymbiosis | "Whenever you put ... +1/+1 counters on a creature ... draw that many. Do this only once each turn." | Once-per-turn gating for triggered abilities (§17) |
| Roving Actuator | "exile ... an instant or sorcery ... Copy it. You may cast the copy without paying its mana cost." | Copy a card and cast the copy (§18) |
| Mm'menon, the Right Hand | "cast artifact spells from the top of your library" + restricted mana | Play-from-top static + restricted mana (§19) |
| Weftwalking | "The first spell each player casts ... may be cast without paying its mana cost" | First-spell-free static (§20) |
| Xu-Ifit, Osteoharmonist | reanimate "It's a Skeleton ... and has no abilities" | Reanimate as typed, ability-stripped permanent (§21) |
| Pull Through the Weft | return up to two nonland permanents to hand **and** up to two lands to the battlefield tapped | Dual-group graveyard return with split destinations (§23) |
| Tezzeret, Cruel Captain | −7 emblem; "it becomes a 0/0 Robot artifact creature" | Emblem creation + becomes-Robot type-set (§24) |
