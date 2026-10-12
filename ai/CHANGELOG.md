# Engine AI changelog

One line per change to how the built-in engine AI **plays or builds its decks** — the changes whose
effect you'd expect to see in its record against humans. The admin dashboard's **AI vs humans**
screen reads this file (baked into the server jar at build time by `game-server`'s
`generateEngineAiHistory` task): each entry becomes a marker on the win-rate timeline, an item in its
"Engine AI changes" list, and a count against the first deployed build that carried it.

**When you open a PR that changes the engine AI's behaviour, add a line at the top of the list in the
same PR.** That's a change under `ai/src/main/kotlin/com/wingedsheep/ai/engine/` that alters a
decision — what it casts, attacks, blocks, targets, pays, keeps or drafts — or which profile live
players face. Leave out refactors, performance work, tooling, per-card AI advisors, and set archetype
data — they'd bury the changes worth correlating with the win rate.

Format — newest first, one bullet each:

```
- #<pr> <what the AI now does differently, in a sentence>
```

- Anchor the line with the PR number. Don't know it yet? Write the line without one: the build then
  ties it to the commit that added the line, which on `main` is the PR's own merge. Add `#<pr>` once
  the PR exists if you like.
- A change that landed without a PR is anchored by its commit sha on `main` instead: `- 1a2b3c4d5e6f …`.
- The date shown is when the change merged; never write one by hand.
- Describe the behaviour, not the code: "Blocks for survival when facing lethal", not "Add
  `survivalBlocks` flag".

## Changes

- #3079 AI divides multi-block combat damage to kill what it can
- #3078 AI treats its own stack objects as never threats; block-only grants need a blocker
- #3065 A draw or tutor while tapped out no longer forfeits the idle-land refund
- #3063 AI spends idle mana on drawing in the last window that can
- #3046 AI refuses precombat Equipment moves onto no better an attacker
- #3042 AI activates sacrifice-mana sources before a cast auto-pay can't cover
- #3040 AI spends a minus-only planeswalker's loyalty (the empower-Jace token)
- #3037 AI refuses activations whose payoff lands on nothing
- #3032 A library search's pick knows what it searches for and where it goes
- #3030 A beginning-of-combat pump counts only on creatures the attack plan sends
- #3027 AI refuses a fetch whose library holds nothing it can find
- #3025 AI holds end-of-turn pumps nothing this turn can use
- #3024 The pending land drop counts as mana in the idle-mana refund
- #3023 AI fills "up to N targets" decisions past one target while each helps
- #3021 Selection counts are picked by value: searches take, scry and surveil sift
- #3020 An optional sacrifice is asked once: pick the permanent or decline
- #2992 AI attacks price the crack-back as life and keep evasion after attacking
- #2988 AI deploys castable permanents instead of hoarding them
- #2986 AI blocks seed legal menace and protection plans; no high-life chump blocks
- #2984 AI aims targets by the polarity the effect implies
- #2981 AI refuses end-of-turn payoffs nothing can spend
- #2979 AI answers colour, land-type, card-name and skip-a-draw choices by what they are for
- #2976 AiProfile.LIVE becomes the one profile real players face
- #2975 Castability-aware card selection (keep, take, discard) behind a profile flag
- #2526 AI targeting fixed for beneficial activated stat boosts
- #2319 AI stops paying to re-equip where its Equipment already is
- #1982 AI stops looping on a free redundant Equip activation
- #1833 AI stops livelocking on a combat tax it cannot pay
- #1821 Momir: never flips a creature with no mana cost, never activates for X=0
- #1799 AI chooses a value for {X}
- #1791 AI blocks for survival when facing lethal
- #1715 AI stops paying for expiring grants in windows that can't spend them
- #1711 AI holds flash creatures for the ambush window instead of jamming them
- #1709 AI holds a counterspell for a spell worth the card
- f62a05ddfa34 AI prices a land in hand as mana, on a curve, and values the cantrip window
- 427daa8dcef8 AI prices a creature by what it can still do
- 32742ee91787 AI holds removal for a target worth a card
- #1692 AI scores its race in urgency instead of turns
- #1690 AI spends a combat trick in the window that decides the fight
- 6cf4f514151d AI plays lands in the order its hand can actually use
- 1cd9c5d3309a A combat trick must earn its response window
- #1687 A land drop no longer counts as card loss
- #1686 The rollout AI goes to production, with two evaluation fixes
- #1665 AI understands what a Room's two halves do
- #1663 AI stops going in circles
- #1605 AI seats build and play their own Commander decks
- #1559 Resource-backed AI evaluation weights
- #1531 Fair hidden-information determinization for the engine AI
- #1511 Engine AI phase 7: the rollout evaluator
- #1508 AI constructed decks are built with Draftsim
- #1504 Engine AI phase 6: CardIntent — card knowledge that generalizes
- #1493 Engine AI phase 4: branching factor and decision budget
- #1487 Engine AI phase 3: multiplayer evaluation
- #761 AI stops mulliganing every Momir Basic opening hand
- ebdbcba38671 Better Momir Basic activation strategy
- #737 AI no longer loops casting a "target spell" instant
- #658 Quick Game AI decks are built with the Draftsim autobuilder
- #624 AI stops aiming two "can't block" effects at the same creature
- #613 AI fills targets for activated abilities, not just spells
- #338 AI skips attack simulation when the opponent has no creatures
- 0dfce8d95fd2 AI no longer freezes when only Treasures can pay
- 5917a66bff8d AI sealed deck building fixed for Lorwyn Eclipsed
- 8891a5f4189b AI gang-block cost miscalculation fixed
