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

- Equips the creature an Equipment does the most for: an equip target is read as a gift, so the AI tries its best creatures first instead of its worst
- #4157 Reads the payoff behind "you may pay/discard/sacrifice … If you do, …": aims those pumps and counters at its own creatures, and sees the tutors, draws and removal they unlock; reads a base-0/0 shrink as removal and a loyalty counter as a gift
- #4155 Generated 60-card constructed decks are built the constructed way — playsets of the best two-colour cards on a low curve (6/12/9/6/2/1 by mana value), eight interaction and sixteen creature copies first, tilted towards a build-around's tribe or theme when the top cards share one — and a format-matched AI seat plays a format-legal built-in starter when one qualifies instead of a generated deck
- #4151 Generated Commander decks now fill role quotas — about ten ramp (fixing ramp first for three-plus colours), eight interaction, seven card draw — and 25 cards on the commander's tribe, keywords or theme, and play 36 lands with the pool's on-colour duals instead of 38 basics split by pips
- #4150 Reads "choose one" spells (charms, commands): in hand a charm is worth what any of its modes does, and a cast is judged by the modes it chose — it no longer spends a charm's team pump at the opponent's end step or its counter mode on an empty stack, and sees a tax-counter mode's tax
- #4150 Reads +1/+1 counters put on another creature as a pump and -1/-1 counters on an opponent's creature as removal, so it flashes in a creature like Little Bear before combat when the counter makes its attack lethal; still holds a flash creature in its own upkeep, and one whose ETB only taps a blocker when it has nothing to attack with
- #4150 Returns the best card from its graveyard ("return target card from your graveyard"), not the cheapest, and prices graveyard cards by what they would be on the battlefield
- #4149 Limited decks run a non-basic land only when it makes nothing but deck colours (splash included) or fixes two of them — no more off-colour tapped duals — and at most one colourless utility land, only when the deck has something for it to do
- #4147 Limited decks whose two colours can't fill the spell slots now splash a few single-pip cards in a third colour instead of padding the gap with basics, running 18 lands rather than 21–22
- #4144 When removal ("destroy target land" and the like) would leave the board looking the same whichever side it hits, it hits the opponent's permanent, not its own
- #4143 Values a Treasure (and any permanent that sacrifices itself for mana: Gold, Eldrazi Spawn) as mana — half a land drop, a full one when it is the mana a spell in hand is one short of — so it takes the Treasure mode when that casts its next spell a turn early, and charges an opponent's Treasures the same
- #4138 Counts a land in hand that no coming land drop needs (its other lands already cover next turn and no spell in hand wants more mana) as a tenth of a card, so it stops trading spells, fights or draws for lands it has no use for; replaces #4113's landcycling-only floor
- #4136 Reconsiders a block that looks like a free kill on paper once the attacker's visible pump would win the fight (a 2/3 in front of a first-striking Skilled Battlecarver with {1}{R} open), instead of only second-guessing blocks by creatures worth more than the attacker
- #4128 Kills an attacker with a free double block (two blockers it can't kill) before trading for it, and no longer attacks a creature into two blockers that can do that to it
- #4127 Stops in its own upkeep when its library is empty, so it can refill the library before the losing draw (Paradox Shaper putting a graveyard card on the bottom) instead of auto-passing into a decking loss
- #4122 Answers no to an optional "Pay {1}?" it would then decline to pay (only a tap-another-permanent source like Gene Pollinator could pay), instead of yes followed by an empty payment
- #4120 Prices how close each library is to running out: no longer plays a land that exiles its whole library (Territorial Bruntar in Momir) or mills itself out, and values milling an opponent out
- #4119 Plays its land drop before activating the Momir avatar or casting an already-affordable X spell, so the activation or spell gets the land's extra point of X
- #4118 Sees an opponent's on-board +1/+1-counter pumps (Tenured Tethermage) and abilities with tap or sacrifice costs as combat answers, so it no longer attacks into a blocker the defender can grow before blocks
- #4116 Picks a "choose two" modal spell's modes by simulating each combination, aiming each mode by what it does (Ashling's Command draws for itself), instead of taking the first modes in printed order at the opponent
- #4115 Aims a granted "draw a card" trigger, "becomes prepared" and "target player draws" at its own side, and when it cannot tell which side an effect is for, simulates its best target on each side instead of only the opponent's
- #4114 Scry and surveil send a spell away to dig for a land when a spell in hand costs more than the lands in play and in hand, keeping a looked-at card only when it beats an average library draw
- #4113 Keeps a spell instead of landcycling it when the lands already in hand cover this turn's and next turn's land drops and no other spell in hand needs the extra land
- #4111 Mulligans a seven-card opening hand whose lands cannot cast any of its spells, and a two-lander with too few early plays (fewer than two spells castable off its two lands on the play; none, or no second three-drop, on the draw)
- #4110 Attacks with a creature it stole until end of turn instead of keeping it home as if it were its own, and no longer casts a steal once the turn has no combat left for the borrowed creature
- #4108 Holds an until-end-of-turn shrink spell instead of casting it on a creature that can no longer fight this turn (a tapped attacker on its own turn), and holds a flash creature when the other instant in hand costs more than all its mana
- #4105 Stops passing main phases with castable creatures when a single lost rollout outvotes the board: each candidate's playouts are read with its static evaluation counted as one more playout
- #4102 In Commander games, counts commander damage as lethal: chump-blocks a commander about to reach the commander-damage threshold, keeps home the blocker that stops it next turn, and aims at a seat its own commander can finish
- #4100 In multiplayer, attacks the opponent it can kill, else the one with the biggest board, instead of always the lowest life total
- #4099 Won't attack away blockers when one activated ability visible on the opponent's board (removal on a blocker, a pump) would make the swing back lethal, and counts idle blockers ganging up on a trampler instead of reading every crack-back past one as lethal
- #4097 Keeps its blockers home when only they stop a lethal swing back: the crack-back no longer counts an opposing creature that won't untap, an attack plan the simulator can't score is no longer submitted unscored, and a plan whose crack-back is lethal earns no credit for our next swing
- #4094 Crews Vehicles and saddles Mounts when the attack it buys beats the attack without it, tapping the creatures that give up least (summoning-sick ones first), instead of never riding at all
- #4092 Facing a 60-card deck it matches to a set, builds from all of that set's legal cards instead of only the set's reprints, and when that build would still be thin (under 4/5 of the spell slots or under 8 creatures in 60) builds a constructed deck for the deck's format instead, rather than seating a 44-land pile with no creatures
- #4085 Returning its own permanent to hand (Rescue Girl, First Responder) now picks the permanent worth recasting, like an enters-trigger creature, instead of a land or a token, and counts that recast as the reason to activate
- #4019 Facing a constructed deck with no single-set identity (a Standard deck spread over many sets), builds a fresh deck for the format that deck was built for — the one its saved or starter deck declares, else the narrowest of Standard to Vintage it is legal in — instead of a 40-card sealed pool from a random set; a deck from one or a few sets is still answered out of those sets, now to its declared format
- #3967 Drafts with the Draftsim scorer and set ratings its deckbuilder already uses, instead of a rarity-and-keyword heuristic that took weak rares over premium common removal; cards of a set with no ratings file are rated from their abilities rather than by rarity alone, for picks and for the Draftsim deckbuild
- #3968 Builds a 100-card singleton deck for Highlander and Canadian Highlander lobbies (leaving points-list cards out of the Canadian build) instead of a 60-card four-of list
- Trims its attack to fit a cap on how many creatures may attack a player (a tapped Mirri, Weatherlight Duelist, Dueling Grounds) instead of declaring too many, being rejected and skipping combat
- The sealed deckbuilder now plays the basic snow lands it opened (Kaldheim packs open one) instead of discarding them with the free basics.
- #3805 Makes a combat declaration handed to it from another seat (Master Warcraft, Brutal Hordechief) for that seat, honouring its must-block requirements, instead of submitting it as its own and conceding after the rejections
- #3678 Sees a blocker whose block trigger destroys the attacker (Alaborn Zealot) as a killer, the way it sees deathtouch: it no longer attacks a big creature into one, and as the defender it blocks with one to trade up
- #3674 No longer casts an end-of-combat sweep over what a creature blocked (Glyph of Doom) when that creature can't block before it fires, or a spells-only mana grant (Piracy) when nothing it could then cast would spend the mana
- #3662 In a multiplayer game, makes a lethal attack with only the creatures the kill needs (two fliers past a ground-only board, say) and keeps the rest home to block the opponents still in the game, instead of tapping out for an overkill alpha strike; in 1v1 it still sends everything
- #3594 Waits for the opponent's end step to use a creature's "{T}: put a counter on this creature" (Clockwork Hydra), instead of tapping it away from attacking and blocking
- #3494 Knows when desertwalk, nonbasic landwalk and legendary landwalk make an attacker unblockable, so it no longer plans blocks the rules forbid against those creatures (it only modelled the five basic landwalks)
- #3478 Holds "target creature can't attack this turn" for the opponent's turn and "can't block" for its own: no longer spends Edifice of Authority on its own turn (where it restrains nothing) just for the brick counter, keeping it untapped to stop the opponent's attacker before combat
- #3477 Holds an end-of-turn lifelink grant until blockers are declared, so it no longer pays (e.g. discards) for lifelink on a creature that then doesn't block, or for two life at 20 before blocks
- #3476 Puts mana and other gift Auras on its own permanents and steal/strip Auras on the opponent's: reads land-mana bonuses (Overgrowth, Wild Growth), granted activated abilities, protection, unblockability, dynamic stat boosts, "you control enchanted permanent", "loses all abilities" and conditional statics, so Overgrowth no longer ramps the opponent's Swamp
- #3421 Presses a winning position instead of sitting on it: when the board it would leave still dominates (creatures and cards in hand, and the race each way), an attack is credited for the damage it deals, so a team of four Bears swings past a lone Hill Giant and trades a Bear for six damage, and a bigger army trades evenly rather than waiting; behind, at parity, or facing a lethal crack-back it attacks exactly as before.
- #3420 Expects the opponent to use its board in a fight: every attack and block it weighs is played out with the opponent using the activated abilities its permanents can afford (a team pump like Leonin Sun Standard, a pinger like Prodigal Sorcerer, keyword granters, regeneration, token blockers) when that wins them the combat, so it stops attacking or blocking into a pump the opponent has mana for, and still makes the play when they are tapped out or the trick can't win.
- Stops casting an Aura whose only effect is an "if enchanted creature …" entry trigger onto a creature that fails the check (Earthbind on a creature without flying), since it would enter and do nothing.
- #3319 Flips Chaos Orb: aims at the spot that destroys the most opponent value (the seam between two cards when that pays) and throws it off the table rather than hit its own board; its release is a human's, a little early or late and off-centre, so it sometimes misses or clips a neighbour.
- #3282 Aims an until-end-of-turn pump at a creature that can still fight: when a beneficial target choice ties (a beginning-of-combat +2/+2 on creatures not yet attacking), it no longer gives it to a bigger creature cast this turn over the one that will attack (Ezekiel Sims, Spider-Totem pumps itself, not a summoning-sick Spider-Man). Permanent payoffs like +1/+1 counters are unaffected.
- #3274 Landcycles for the land it is missing: stuck short on lands with its land drop unused, a line that puts a land in hand (Plainscycling Noble Templar) is scored as playing that land the same turn, so it cycles and plays the land instead of passing with a hand of uncastable spells.
- #3258 Builds limited decks it can cast: basics follow how hard each color's cards press (double and triple pips and early drops count extra, and each color gets at least the sources its most demanding card needs), cards whose pips the mana base can't support are cut, off-color `{2/C}`-heavy cards like Karn, Gilded Guardian stay out, planeswalker tutors and sacrifice-a-planeswalker spells stay out of decks without planeswalkers to find, it runs at least 14 creatures when the pool has them (cutting tricks and Auras first), and no longer splashes a fourth color or a double-pip card.
- #3253 Reads a "counter unless they pay {N}" against the mana the caster has left: it no longer throws one at a spell whose caster can pay every tax it can stack, and it stacks two when the first drains their mana and the second counters; and it stops casting a Banishing Light-style permanent into a board with nothing to exile, or discarding a -3/-1 onto a creature that has no fight left this turn.
- #3252 Attacks by the race: an attack that brings its next-turn kill closer counts for it (so a team that leaves the opponent dead on board next turn swings past a lone blocker), a tapped attacker is charged only for the block it can't make on the swing back, and an attack that only looks safe because the defender would trade a creature away is also checked with the defender taking the hit and keeping its creatures for the crack-back.
- #3249 Reads whose cards a choice moves: when it picks an opponent's discard or exile it takes the card most valuable to *them* (not the one it couldn't cast itself), a discard asked as a bare "choose a card" pitches its least useful card instead of its only land, an "up to one" look takes the land a land-short hand needs, and a "top or bottom of your library" choice bottoms a spell to dig for a land when the hand is short of lands.
- #3248 Stacks several chump blockers behind a trampler when one body would still let lethal spill over, and counts double strike as two hits when adding up incoming damage; pays convoke spells with lands first, tapping only the cheapest creatures for whatever mana can't cover instead of tapping its would-be blockers and attackers.
- #3245 Prices planeswalkers as engines: attacks and burns the opposing walker climbing to its ultimate rather than a minus-only Jace token, and counts loyalty abilities granted by other permanents
- #3242 Spends minus-only planeswalkers like the empower-Jace token instead of sitting on them: −3 to draw whenever it can, otherwise −1 to surveil, last counters included — holding the token only when the next attack would be lethal and it is the only other thing to hit — and uses loyalty abilities the "Way of the X" enchantments grant (a granted +1 makes the token an engine worth keeping).
- Leaves an "up to one/up to N target" slot empty rather than aim it at the wrong side: Tam's Resistance with only an opposing creature is cast for its Empower Jace alone instead of buffing their creature, an "up to two" destroy or an optional harmful trigger never hits our own permanents, and when the effect's side can't be read the AI simulates leaving the slot empty and takes that when it scores strictly better.
- Casts enablers before the spells they discount: a spell is scored as at least the best spell in hand it makes cheaper or castable, so a noncreature spell goes down before a "costs {2} less if you've cast a noncreature spell this turn" creature instead of the creature being cast at full price.
- Casts a pump before combat when it changes who attacks: a combat trick or pump ability that lets a creature make an attack it otherwise wouldn't is priced by what that attack is worth over the unpumped one, instead of being deferred to a beginning-of-combat window the AI never stops in.

- Holds instant-speed deploys for the opponent's turn: a flash creature is judged by what it can do the turn it lands (an attack-trigger loot no longer gets it jammed in its own main phase), an instant that only makes no-haste tokens or empowers a walker it lets act at instant speed waits for their turn too, and either is cast into idle mana when holding it would leave another instant in hand without mana.

- Deck building for sets without rating data values planeswalkers as the engines they are instead of as filler (an on-color rare walker no longer loses its slot to a vanilla 3/9), and counts a legend's second copy at a discount for the legend rule, so it stops running doubles of filler legends.

- Defends its own planeswalkers: a walker is priced by the loyalty the next attack leaves it, so one about to die spends its loyalty first (the biggest useful minus) instead of dying with counters unspent, a blocker kept home for it counts, and blocks on attackers aimed at it are worth the walker they save — a chump or trade for a valuable walker, nothing for a near-dead token.

- On Auto against a deck the human brought, plays from that deck's set(s) — another built-in starter from them when one exists (Boros Mice meets another Bloomburrow tribe), else a deck built from those sets in the same 60-card or 40-card shape — instead of a sealed pool from an unrelated set.

- Attacks opposing planeswalkers: each attacker that will connect goes at the walker its damage hurts most — killing it, or knocking it below its biggest ability — when that is worth more than the life it would take, while a lethal or nearly-won race keeps its damage on the player; attack search also tries retargeting each attacker and runs when a walker is in reach and nothing can block.

- Chump-blocks to stay out of the opponent's next-attack range when one body now saves more bodies next turn (menace needs two blockers), instead of only when this hit alone is lethal.

- Stops paying for until-end-of-turn pumps nothing spends: a temporary +X/+X counts only on a creature already attacking or blocking, a precombat pump activation needs an attack plan that uses it, and an action that merely ties passing no longer wins on rounding.

- Casts wishes (Extrapolate the Impossible, Burning Wish) with idle mana in its last main phase instead of holding them all game, unless nothing outside the game is fetchable.

- Allocates the full required counter payment across permanents, including several counters on one, and compares paying against declining.

- The AI spreads required graveyard targets across owners instead of selecting several cards from one graveyard.

- #3182 AI puts shield counters, pumps and protection from triggered abilities and from modal or tiered spells (Proud Pack-Rhino, Restoration Magic) on its own permanents instead of an opponent's
- #3180 Chooses targets for the face being cast — a split half, both fused halves, an Adventure, Room or prepare face — avoiding beneficial enemy targets and harmful friendly targets.

- #3181 Scry and surveil no longer keep a surplus land on top just because this turn's land drop is unspent — a card left in the library is drawn next turn at the earliest

- #3174 Chooses compatible groups for spells and abilities whose targets must have equal toughness or another numeric value.

- #3170 Values every token payoff in a mixed token-creation batch when assessing cards for play and deckbuilding.

- Keeps ranked secret bids at their floor, chooses matching numbers independently, and casts creature ballots from the visible board without reading unrevealed answers.

- #3137 AI plays its land before a cheaper spell when the land makes a bigger spell castable — hard-casting Shriekmaw instead of casting a 2-drop and then evoking it
- #3135 AI seats on a random deck now own the rest of their sealed pool (or a fifteen-card wishboard) outside the game, so their Learn and wish cards fetch a card instead of whiffing
- #3132 AI puts helpful Role tokens and Auras like Strands of Undeath on its own creatures, and no longer pays to regenerate an opponent's creature
- #3129 AI pays sacrifice, discard, bounce and tap costs with its least valuable candidates (the source included) instead of the first legal ones
- #3128 In multiplayer the AI sends each attacker at a defender it may legally attack (skipping a seat behind Blazing Archon, routing Goblin Rock Sled to a seat with a Mountain) instead of aiming everything at the lowest-life opponent and getting the whole attack rejected
- #3120 AI holds flash creatures for the opponent's turn and no longer releases them for its own triggers on the stack
- #3114 AI keeps modal double-faced spells instead of playing them as lands it doesn't need, prices surplus lands in hand near zero, and won't bounce its own land into a hand that already holds the next land drops
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
