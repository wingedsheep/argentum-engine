# Star Trek (TRK) — Mechanics

Based on the 236 unique cards (449 printings) available in Scryfall on October 26, 2026.
TRK releases November 13, 2026; this is a preview snapshot, not a claim that the set is complete.
Scryfall currently marks every printing `booster: false`, so the generated checklist correctly
places all 236 cards under Extras. Refresh the bootstrap as preview data and booster flags change.

A checked box means the named mechanic or theme has existing SDK support, not that every ability
on every listed card is implemented. An unchecked box identifies missing capability or unresolved
rules timing that needs `add-feature` investigation before those cards can be completed.
Lists overlap intentionally; each contains every card in this snapshot matching the stated theme.
Cycling/typecycling metadata is consolidated into basic landcycling; Saga numerals and the empty
keyword are not independent mechanics. Strange new worlds and Federation are ability words.

Support was checked against [the SDK language reference](../../../docs/card-sdk-language-reference.md),
[the SDK index](../../../docs/sdk-index.md), SDK definitions and existing corpus examples.
Rule numbers below were checked in the official September 25, 2026 Comprehensive Rules downloaded
from [Wizards' rules page](https://magic.wizards.com/en/rules). That edition predates TRK: new Trek
mechanics are described from Scryfall's reminder text, with no invented rule numbers.

Scaffold decisions: standalone expansion, no block; all five basics are printed, so no fallback;
limited remains enabled by default, with `incomplete = true` keeping the set out of the default picker.
The backlog was created alongside the beam me up engine support and its first cards (Open Communications,
Scotty, Miracle Worker and Marooned).

## Mechanics and themes, largest card groups first

### - [x] Counters and counter thresholds (67 cards)

Place, amplify, remove or reward counters, including +1/+1 growth, quest/promotion thresholds and station charge counters. The snapshot spells Jett Reno's counter as +1/1; confirm that preview typo before authoring.

**Engine support:** ✅ `Effects.AddCounters`, dynamic counter amounts, counter replacement effects and `SourceCounterCountAtLeast`. Dr. McCoy's heal rider is tracked separately.

Cards: All Good Things..., Amok Time, Anthropological Mission, Battle-Scarred Survivalist, Beckett Mariner, Impetuous Ensign, Borg Cube, Brad Boimler, Eager Ensign, Captain Jean-Luc Picard, Captain Kathryn Janeway, Captain Michael Burnham, Cetacean Ops, Cold-Blooded Crew, Conquest Mission, Consider the Prime Directive, Constable Odo, D-7 Battle Cruiser, Data, New Life, Deanna Troi, Psychic Counsel, Death Is Irrelevant, Deep Space 9, Dominion Saboteur, Dominion Supervisor, Dr. Beverly Crusher, Dr. Joseph M'Benga, Dr. McCoy, Surly Surgeon, Emergency Medical Hologram, Engineered Explosives, Excalbian Gamemaster, Exploratory Mission, Federation Field Medic, General Chang, Cold Warrior, Gowron, Eager for Battle, I'm a Doctor, Not a . . ., In the Pale Moonlight, Jadzia Dax, Living Legacy, Jett Reno, Problem Solver, Lieutenant Commander Data, Lt. Cmdr. Geordi La Forge, Overflow Submatrix, Peace in Our Time, Pure Energy, Romulan Bird-of-Prey, Rutherford, Upgraded Ensign, Set Phasers to . . ., Shields Up!, Storied Veteran, Subcommander T'Pol, Support Mission, Suurok-Class Science Vessel, Sylvia Tilly, Joy of Discovery, Syndicate Liquidators, Tactical Officer, Test of Conviction, The City on the Edge of Forever, The Doctor, EMH, The Inner Light, The Wrath of Khan, Those Old Scientists, Type-2 Phaser, U.S.S. Enterprise-A, Free Spirit, U.S.S. Enterprise-D, Galaxy-Class, U.S.S. Reliant, From Hell's Heart, Warp Core, Worf, Honor-Bound Warrior, Xindi Surveyors, Year of Hell, Zefram Cochrane, Warp Pioneer

### - [x] Sacrifice fuel and death payoffs (39 cards)

Sacrifice permanents for value or trigger from creatures dying; includes optional sacrifices during resolution and temporary tokens sacrificed later.

**Engine support:** ✅ `Costs.Sacrifice`, sacrifice effects, dies triggers, reflexive triggers and delayed effects. These are established corpus patterns.

Cards: A Good Day to Die, All Good Things..., Amok Time, Automated Warfare System, Battle-Scarred Survivalist, Blastoneuron, Commander Raffi Musiker, Constable Odo, D'Ghor Bloodletter, D-7 Battle Cruiser, Dahar Master, Dal R'El, Augment Orphan, Dathon and Picard at El-Adrel, Eject the Warp Core, Engineered Explosives, Excalbian Gamemaster, General Chang, Cold Warrior, Gowron, Eager for Battle, Grand Nagus Zek, Gray and Adira, Forever Joined, In the Pale Moonlight, Jake Sisko, War Correspondent, Khan, Vengeful Heart, Kirk, Commanding Officer, L'Rell, Fierce Visionary, Munitions Enthusiast, Planetary Bombardment, Quark, Humble Proprietor, Replicator, Spatial Anomaly, Subspace Anomaly, Tea, Earl Grey, Hot, The City on the Edge of Forever, The Inner Light, The Wrath of Khan, Those Old Scientists, Unexplored Territory, Will Riker, First in the Field, Year of Hell

### - [x] Play and cast from other zones (32 cards)

Grant access to exiled cards, the library top, the graveyard or cards outside the game, with printed timing and cost limits. Beam me up has its own section below.

**Engine support:** ✅ `GrantMayPlayFromExile`, top-library play statics and cast permissions provide the general infrastructure; evaluate each card's restrictions when authoring.

Cards: Adversary by Algorithm, Captain Benjamin Sisko, Captain Kathryn Janeway, Captain Pike, Fullness of Time, Energize, Ensign Pavel Chekov, Exploratory Mission, Final Frontier, First Contact, Infinite Diversity, Khan, Vengeful Heart, Kirk, Commanding Officer, Malfunctioning Holodeck, Open Communications, Overflow Submatrix, Pure Energy, Q, Stacking the Deck, Ravenous Cosmozoan, Reckless Impulse, Salt Vampire, Scotty, Miracle Worker, Silicate Surveyor, Storied Veteran, The Caretaker, The Holodeck, The Inner Light, There's Coffee in that Nebula, U.S.S. Discovery, at Black Alert, U.S.S. Enterprise-D, Galaxy-Class, U.S.S. Reliant, From Hell's Heart, Zefram Cochrane, Warp Pioneer, Zero, Incorporeal Sensate

### - [x] Modal spells and abilities (28 cards)

Choose among printed modes, sometimes with a conditional mode count or modes that cannot be chosen again. This is the fuel for dilemma payoffs, whose event support is tracked separately.

**Engine support:** ✅ `ModalEffect`, conditional mode bounds and `excludePreviouslyChosenModes`; the latter covers Kobayashi Maru Scenario. Existing modal cards include the Commands and Riku of Many Paths.

Cards: Captain James T. Kirk, Command Decision, Commander Raffi Musiker, Consider the Prime Directive, Constable Odo, D'Ghor Bloodletter, Dathon and Picard at El-Adrel, Federation Field Medic, Federation Outpost, Federation Probe, Impossible Choice, Kobayashi Maru Scenario, Lore, Positronic Provocateur, Make It So, Perils of the Past, Quark's Bar, Resistance Is Futile, Rogue Artificial Intelligence, Rules of Acquisition, Set Phasers to . . ., Subcommander T'Pol, Tea, Earl Grey, Hot, Test of Conviction, Test of Courage, Test of Loyalty, Test of Priorities, The Unicomplex, V'Ger, the Intruder

### - [x] Artifact-creature collective (24 cards)

Create, count, strengthen or recur artifact creatures; the Borg army uses artifact-creature filters rather than merely checking the Borg subtype.

**Engine support:** ✅ `GameObjectFilter.Creature.artifact()`, group statics, filtered triggers and token/zone-move effects. Assimilate itself is a separate gap below.

Cards: Adversary by Algorithm, Antagonist Incorporator, Assault Drone, Assembly Specialist, Borg Cube, Collective Drone, Command Subadjunct, Conquest Mission, Crystalline Entity, D-7 Battle Cruiser, Death Is Irrelevant, Hive Mind Coprocessor, Hologram Stalker, Locutus, the One Who Speaks, Lt. Cmdr. Geordi La Forge, Miles O'Brien, Transporter Ops, Organic Avulsion Unit, Overflow Submatrix, Primary Unimatrix Adjunct, Resistance Is Futile, Suurok-Class Science Vessel, U.S.S. Enterprise-A, Free Spirit, U.S.S. Enterprise-D, Galaxy-Class, U.S.S. Reliant, From Hell's Heart

### - [x] Surveil (16 cards)

Look at the top N cards; put any into your graveyard and the rest back on top. CR 701.25.

**Engine support:** ✅ `Effects.Surveil` / `Patterns.Library.surveil`, including the surveil event.

Cards: Cybernetic Specialist, D'Vana Tendi, Optimistic Ensign, Federation Outpost, Federation Probe, He's Dead, Jim, Jett Reno, Problem Solver, L'Rell, Fierce Visionary, Lore, Positronic Provocateur, Quark's Bar, Rogue Artificial Intelligence, Sylvia Tilly, Joy of Discovery, Tell Me What You See, Test of Priorities, The Unicomplex, Tricorder, Vash, Freelance Archaeologist

### - [x] Spacecraft interaction and support (15 cards)

Interact with Spacecraft independently of creature status, search or discount them, reward their spells, or accelerate charge counters.

**Engine support:** ✅ Subtype filters, filtered cast triggers, cost modification, search effects and counter placement; station itself is separately catalogued.

Cards: 70,000 Light-Years from Home, Borg Cube, Captain Jean-Luc Picard, D-7 Battle Cruiser, Deep Space 9, Guidance Failure, Hemmer, Fixing What Is Broken, Romulan Bird-of-Prey, Rutherford, Upgraded Ensign, Suurok-Class Science Vessel, U.S.S. Enterprise-A, Free Spirit, U.S.S. Enterprise-D, Galaxy-Class, U.S.S. Reliant, From Hell's Heart, Warp Core, Zefram Cochrane, Warp Pioneer

### - [x] Planet lands and New Planet tokens (14 cards)

Planet fixing lands enter tapped and gain life; other cards find Planets or create tapped New Planet lands that make any color.

**Engine support:** ✅ Land subtypes, `EntersTapped`, mana abilities, land-search filters and custom land-token definitions. Existing EOE Planets establish the subtype infrastructure.

Cards: Andoria, Bajor, Cardassia Prime, Earth, Sector 001, Ferenginar, Final Frontier, Orion, Qo'noS, Risa, Romulus, Saurian Explorer, Spatial Anomaly, Subspace Anomaly, Vulcan

### - [x] Officer tokens and Officer payoffs (10 cards)

Create red 1/1 Officers or count/tap Officers to power a payoff.

**Engine support:** ✅ Subtype-filtered token creation, battlefield counts and tap-permanent costs.

Cards: Captain Benjamin Sisko, Captain James T. Kirk, Captain Jean-Luc Picard, Excalbian Gamemaster, Georgiou, Terran Emperor, Impossible Choice, Kobayashi Maru Scenario, Make It So, Subspace Anomaly, U.S.S. Enterprise-A, Free Spirit

### - [x] Shock lands (10 cards)

Typed dual lands may enter untapped if their controller pays two life.

**Engine support:** ✅ `EntersTapped(payLifeCost = 2)` and basic-land-type mana abilities; the RAV/GPT/DIS originals already use this composition.

Cards: Blood Crypt, Breeding Pool, Godless Shrine, Hallowed Fountain, Overgrown Tomb, Sacred Foundry, Steam Vents, Stomping Ground, Temple Garden, Watery Grave

### - [x] Station (10 cards)

Tap another untapped creature you control, at sorcery speed, to add charge counters equal to its power; thresholds unlock printed abilities. CR 702.184.

**Engine support:** ✅ `station()`, `DynamicAmount.StationCharge`, and `SourceCounterCountAtLeast`; see the existing EOE Spacecraft.

Cards: Borg Cube, D-7 Battle Cruiser, Deep Space 9, H.M.S. Bounty, Unlikely Timeship, Romulan Bird-of-Prey, Suurok-Class Science Vessel, U.S.S. Discovery, at Black Alert, U.S.S. Enterprise-A, Free Spirit, U.S.S. Enterprise-D, Galaxy-Class, U.S.S. Reliant, From Hell's Heart

### - [x] Landfall (9 cards)

Ability word for triggers when a land you control enters; the individual card supplies the payoff.

**Engine support:** ✅ Land-entry event filters and triggered abilities, as used by existing landfall cards.

Cards: Captain Michael Burnham, Cleveland Booker, Empath, Dal R'El, Augment Orphan, Exploratory Mission, Jake Sisko, War Correspondent, Planetary Patrol, Saurian Explorer, Syndicate Liquidators, Xindi Surveyors

### - [x] Beam me up and beaming restrictions (8 cards)

Cast an instant or sorcery from your graveyard for its beam-me-up mana cost plus returning a creature you control to hand, then exile the spell. Scotty grants it with timing and source exclusions; Marooned prohibits returning the enchanted object this way.

**Engine support:** ✅ `beamMeUp(cost)`: graveyard alternative casting with a return-creature cost and exile on every stack exit. Grants are `GraveyardCardsHaveCastKeyword(GraveyardCastKeyword.BeamMeUp(returnFilter), filter)`, gated with `staticAbility { condition = … }` (Scotty); "can't be beamed up" is `GrantKeyword(AbilityFlag.CANT_BE_BEAMED_UP.name)` (Marooned). Implemented: Open Communications, Scotty, Miracle Worker, Marooned.

Cards: Adversary by Algorithm, Energize, Final Frontier, Marooned, Open Communications, Pure Energy, Scotty, Miracle Worker, There's Coffee in that Nebula

### - [x] Becoming tapped and tap outlets (8 cards)

Station and other tap costs fuel triggers when creatures become tapped; some effects last while the source remains tapped.

**Engine support:** ✅ `Triggers.self.becomesTapped`, `Costs.TapPermanents` and source-tapped duration conditions; EOE's station users exercise the same tap infrastructure.

Cards: Beckett Mariner, Impetuous Ensign, Brad Boimler, Eager Ensign, Erica Ortegas, Plucky Pilot, Hemmer, Fixing What Is Broken, Jett Reno, Problem Solver, Lwaxana Troi, Charming Guest, Shuttle Ace, Worf, Honor-Bound Warrior

### - [x] Copies with modifications (8 cards)

Create token copies, become a copy with exceptions, or copy exiled cards for casting.

**Engine support:** ✅ Copy effects with exceptions, token-copy effects and copied-card casting permissions; individual counter-copy riders still need scenario coverage.

Cards: Blastoneuron, Dominion Saboteur, Gray and Adira, Forever Joined, Infinite Diversity, Mirror Universe Intrusion, Q, Stacking the Deck, Replicator, Troubling Tribbles

### - [x] Differently named land thresholds (8 cards)

The same land-name diversity count also gates looting and an activated base-stat change without the ability-word label.

**Engine support:** ✅ `DynamicAmounts.differentlyNamedLandsYouControl()` with comparison conditions and base-stat effects.

Cards: Branching Timeline, Captain Pike, Fullness of Time, Deep Space Scan, Ensign Pavel Chekov, Excalbian Gamemaster, Pah-Wraith, Seven of Nine, Unassimilated, The Traveler

### - [x] Sagas (8 cards)

Add lore counters on entry and after the draw step, trigger numbered chapters, then sacrifice after the final chapter. CR 714.

**Engine support:** ✅ `sagaChapter`, lore counters and Saga lifecycle handling; linked exile and phasing riders require their own composition review.

Cards: All Good Things..., Amok Time, In the Pale Moonlight, The City on the Edge of Forever, The Inner Light, The Wrath of Khan, Those Old Scientists, Year of Hell

### - [ ] Face a dilemma (7 cards)

Choosing one or more modes for a spell or ability constitutes facing a dilemma. Payoffs trigger on it or inspect the turn's dilemma history. Captain Benjamin Sisko also needs a per-turn count.

**Engine support:** ❌ Missing a generic mode-choice event for spells AND activated/triggered abilities, plus per-turn history/count. Modal-spell cast predicates alone are insufficient.

Cards: Captain Benjamin Sisko, Elim Garak, Simple Tailor, Jett Reno, Problem Solver, Obsidian Order Operative, Seven of Nine, Tell Me What You See, The Caretaker

### - [x] Flying (7 cards)

Can be blocked only by creatures with flying or reach. CR 702.9.

**Engine support:** ✅ `Keyword.FLYING`.

Cards: Automated Warfare System, Blastoneuron, Crystalline Entity, DOT-7 Repair Squad, Federation Probe, Ravenous Cosmozoan, V'Ger, the Intruder

### - [x] Noncreature-spell payoffs (7 cards)

Trigger when you cast noncreature spells or count noncreature, nonland graveyard cards.

**Engine support:** ✅ Filtered cast triggers and zone counts; Anthropological Mission composes these with quest counters.

Cards: Anthropological Mission, Cetacean Ops, Gray and Adira, Forever Joined, Lieutenant Commander Data, Ravenous Cosmozoan, Romulan Bird-of-Prey, Spock, Always Your Friend

### - [x] Strange new worlds (7 cards)

Ability word scaling effects by the number of differently named lands you control.

**Engine support:** ✅ `DynamicAmounts.differentlyNamedLandsYouControl()` (`DISTINCT_NAMES`); also used by Field of the Dead and TRC's Christine Chapel.

Cards: Branching Timeline, Captain Pike, Fullness of Time, Deep Space Scan, Ensign Pavel Chekov, Excalbian Gamemaster, Pah-Wraith, The Traveler

### - [x] Trample (7 cards)

Allows excess combat damage to be assigned to the defending recipient. CR 702.19.

**Engine support:** ✅ `Keyword.TRAMPLE`.

Cards: Captain Michael Burnham, Horta, Khan, Despot by Design, Kira Nerys, Firebrand, Munitions Enthusiast, Phlox, Unorthodox Doctor, Syndicate Liquidators

### - [x] Total power eight (6 cards)

Reward an attack or improve another effect when creatures you control have total power eight or greater.

**Engine support:** ✅ Projected battlefield power aggregation with comparison conditions, attack triggers and conditional stat/keyword effects.

Cards: Cantankerous Captain, Kirk, Commanding Officer, La'An Noonien-Singh, Security, Tactical Officer, Una Chin-Riley, Number One, Worf, Honor-Bound Warrior

### - [x] Basic landcycling (5 cards)

Pay the printed cost and discard this card to find and reveal a basic land, put it into hand, then shuffle. Cycling, landcycling and typecycling metadata describe this same ability and are merged here. CR 702.29e.

**Engine support:** ✅ `KeywordAbility.BasicLandcycling` / the shared typecycling handler.

Cards: Cold-Blooded Crew, Free Borg Revolutionaries, Horta, Humpback Whales, Rogue Artificial Intelligence

### - [x] Federation (5 cards)

Ability word counting the distinct creature types among non-Borg creatures you control, for mana, damage, pump, selection or spell copies.

**Engine support:** ✅ `Aggregation.DISTINCT_CREATURE_TYPES`; facade `DynamicAmounts.federation()` (non-Borg creatures you control). The Star Trek creature types are in `Subtype.ALL_CREATURE_TYPES`.

Cards: Captain Jonathan Archer, Hoshi Sato, Exolinguist, Infinite Diversity, Saavik, Stoic Student, Starfleet Academy

### - [x] Stun counters (5 cards)

Tap creatures and put stun counters on them; each counter replaces an untap by being removed.

**Engine support:** ✅ `CounterType.STUN`, tap effects and the existing untap replacement handling.

Cards: Consider the Prime Directive, Constable Odo, Set Phasers to . . ., Subcommander T'Pol, Type-2 Phaser

### - [x] Vigilance (5 cards)

Attacking does not cause the creature to tap. CR 702.20.

**Engine support:** ✅ `Keyword.VIGILANCE`.

Cards: Federation Field Medic, Humpback Whales, Mugato, The Caretaker, The Traveler

### - [x] Ward (5 cards)

Counters an opponent's targeting spell or ability unless they pay the printed ward cost. CR 702.21.

**Engine support:** ✅ `KeywordAbility.Ward` and `WardCost`; includes mana and nonmana costs.

Cards: Cetacean Ops, Humpback Whales, Malfunctioning Holodeck, The Caretaker, U.S.S. Enterprise-A, Free Spirit

### - [x] Flash (4 cards)

Allows casting at instant timing. CR 702.8.

**Engine support:** ✅ `Keyword.FLASH`.

Cards: Captain's Tutelage, Cloistered Telepath, Saavik, Stoic Student, Subcommander T'Pol

### - [x] Investigate (4 cards)

Create a Clue token. CR 701.16.

**Engine support:** ✅ `Effects.Investigate` and the predefined Clue token.

Cards: Commander Raffi Musiker, Constable Odo, In the Pale Moonlight, Jake Sisko, War Correspondent

### - [x] Lifelink (4 cards)

Damage dealt also causes its controller to gain that much life. CR 702.15.

**Engine support:** ✅ `Keyword.LIFELINK`.

Cards: Brad Boimler, Eager Ensign, Relentless Drednok, Salt Vampire, Zero, Incorporeal Sensate

### - [x] Treasure (4 cards)

Create a predefined artifact token that can be tapped and sacrificed for one mana of any color. CR 111.10a.

**Engine support:** ✅ `Effects.CreateTreasure`.

Cards: Grand Nagus Zek, Quark, Humble Proprietor, Temporal Conqueror, The Inner Light

### - [x] Equip (3 cards)

Pay the equip cost at sorcery speed to attach the Equipment to a creature you control. CR 702.6.

**Engine support:** ✅ `equipAbility` and attachment statics.

Cards: Bat'leth, Tricorder, Type-2 Phaser

### - [x] Graveyard departures (3 cards)

Reward one or more cards leaving your graveyard, sometimes only once per turn; beam me up and recursion provide fuel.

**Engine support:** ✅ Zone-change triggers with origin/ownership filters and trigger frequency limits.

Cards: Blastoneuron, Hologram Stalker, Vash, Freelance Archaeologist

### - [x] Haste (3 cards)

Allows attacking and tap-symbol activations without waiting a turn. CR 702.10.

**Engine support:** ✅ `Keyword.HASTE`.

Cards: Erica Ortegas, Plucky Pilot, Pah-Wraith, Ravenous Cosmozoan

### - [x] Kicker (3 cards)

Optional additional cost changes the spell's effect when paid. CR 702.33.

**Engine support:** ✅ Kicker additional costs, kicked conditions and `kickerTarget`.

Cards: Eject the Warp Core, Highly Illogical, Mirror Universe Intrusion

### - [x] Menace (3 cards)

Requires at least two creatures to block. CR 702.111.

**Engine support:** ✅ `Keyword.MENACE`.

Cards: Armus, Skin of Evil, Battle-Scarred Survivalist, Elim Garak, Simple Tailor

### - [x] Reach (3 cards)

Allows blocking creatures with flying. CR 702.17.

**Engine support:** ✅ `Keyword.REACH`.

Cards: Mugato, Silicate Surveyor, The Traveler

### - [ ] Assimilate (2 cards)

Put the selected creature card onto the battlefield under your control with a +1/+1 counter; it becomes a Borg artifact creature and loses other creature types. Borg Cube also links the candidates to its own exile.

**Engine support:** ❌ Partial: `Effects.Move`, entry counters, `AddCardType` and `SetCreatureSubtypes` cover pieces. No verified assimilation entry-characteristic rider exists; establish entry timing from the TRK rules update before treating a post-entry composition as correct.

Cards: Borg Cube, Death Is Irrelevant

### - [x] Deathtouch (2 cards)

Any positive damage dealt to a creature is lethal for the relevant rules. CR 702.2.

**Engine support:** ✅ `Keyword.DEATHTOUCH`.

Cards: Moopsy, Mugato

### - [x] Double (2 cards)

Double a quantity; TRK doubles life total or power, rather than granting double strike. CR 701.10.

**Engine support:** ✅ `Effects.GainLife` with current life and dynamic power modification with current power.

Cards: Live Long and Prosper, Worf, Honor-Bound Warrior

### - [x] Enchant (2 cards)

Restricts an Aura's legal targets and attachments. CR 702.5.

**Engine support:** ✅ Aura target requirements and attached-permanent statics.

Cards: Captain's Tutelage, Marooned

### - [x] First strike (2 cards)

Deals combat damage in the first combat damage step. CR 702.7.

**Engine support:** ✅ `Keyword.FIRST_STRIKE`.

Cards: Beckett Mariner, Impetuous Ensign, Pah-Wraith

### - [x] Mill (2 cards)

Put the specified number of cards from the top of a library into its owner's graveyard. CR 701.17.

**Engine support:** ✅ `Patterns.Library.mill` with mill event tracking.

Cards: First Contact, Temporal Conqueror

### - [x] Affinity (1 card)

Reduce the spell's generic cost by one for each matching permanent you control; Borg Cube counts artifact creatures. CR 702.41.

**Engine support:** ✅ `ModifySpellCost` with `CostReductionSource.PermanentsYouControlMatching` and an artifact-creature filter. The narrow `KeywordAbility.Affinity(forType)` alone is insufficient.

Cards: Borg Cube

### - [x] Discover (1 card)

Exile to a qualifying nonland card; cast it free or put it into hand, then randomize the other exiled cards on the bottom. CR 701.57.

**Engine support:** ✅ `Effects.Discover`.

Cards: U.S.S. Discovery, at Black Alert

### - [x] Explore (1 card)

Reveal the top card; put a land into hand, otherwise add a +1/+1 counter and optionally put the revealed card into the graveyard. CR 701.44.

**Engine support:** ✅ `Effects.Explore`.

Cards: Captain Kathryn Janeway

### - [ ] Extra turn begun this game (1 card)

H.M.S. Bounty checks whether its controller has begun an extra turn this game, rather than merely creating or scheduling one.

**Engine support:** ❌ Extra turns and the creature-type gate exist; missing game-long history of extra turns actually begun by each player.

Cards: H.M.S. Bounty, Unlikely Timeship

### - [x] Fight (1 card)

Two creatures each deal damage equal to their power to the other. CR 701.14.

**Engine support:** ✅ `Effects.Fight`.

Cards: Amok Time

### - [ ] First spell cast this game (1 card)

First Contact checks whether it is the first spell its controller has cast this game, rather than this turn.

**Engine support:** ❌ Missing per-player game-long cast history/condition. `SpellsCastThisTurn` and nth-spell-per-turn events do not supply it.

Cards: First Contact

### - [x] Food (1 card)

Create a Food artifact token with the standard life-gain sacrifice ability. CR 111.10b.

**Engine support:** ✅ `Effects.CreateFood`; Replicator's additional token-copy option is a separate composition.

Cards: Replicator

### - [ ] Global phase-in prohibition (1 card)

The City on the Edge of Forever phases out artifacts and creatures, then creates a Human token whose static ability prevents permanents from phasing in.

**Engine support:** ❌ Ordinary `Effects.PhaseOut` exists, but there is no global phase-in prohibition. A source-bound phase-in lock does not model this token's static ability.

Cards: The City on the Edge of Forever

### - [ ] Heal (1 card)

Remove damage marked on a permanent. Dr. McCoy replaces destruction by removing its +1/+1 counters and healing the damage. CR 701.69.

**Engine support:** ❌ Partial: `ReplaceDestructionWith` and `RemoveAllCountersOfType` exist; missing an immediate remove-marked-damage effect. `HealOtherDamage` is a damage replacement, and `RemoveDamageShield` is a future shield.

Cards: Dr. McCoy, Surly Surgeon

### - [x] Sunburst (1 card)

Enter with a counter per color of mana spent to cast the spell: charge counters for noncreatures, +1/+1 counters for creatures. CR 702.44.

**Engine support:** ✅ `convergeEntersWithCounters` / `DistinctColorsManaSpent` with charge counters for Engineered Explosives; the counter-entry composition is used by the SOS Archaics.

Cards: Engineered Explosives
