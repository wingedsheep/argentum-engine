# Fallout (PIP) — Mechanics

Engine vocabulary checked against `docs/card-sdk-language-reference.md` and `mtg-sdk` source. A checked section means its base mechanic is expressible, not that every listed card is implemented. The loop re-verifies card-specific interactions during gap analysis. Flavor ability names and Saga chapter numerals are omitted.

Hail, Caesar is the first priority; see [its card list](hail-caesar.txt) and the [official decklists](https://magic.wizards.com/en/news/announcements/magic-the-gathering-fallout-commander-decklists). Its main missing shared capabilities are Squad and dice rolling; the rest of Fallout also needs radiation, Fortifications and mutate.

### - [x] Artifact payoffs (84 cards)

Count artifacts, animate or untap them, and reward artifact creation or entry.

**Engine support:** Supported: artifact filters, projected characteristics and ordinary triggers; some cards additionally require energy or dice.

Cards: All That Glitters, Arcade Gannon, Assaultron Dominator, Austere Command, Automated Assembly Line, Boomer Scrapper, Bottle-Cap Blast, Break Down, Brotherhood Scribe, Brotherhood Vertibird, Buried Ruin, C.A.M.P., Casualties of War, Commander Sofia Daguerre, Contagion Clasp, Crimson Caravaneer, Crush Contraband, Curie, Emergent Intelligence, Deadly Dispute, Desdemona, Freedom's Edge, Dispatch, Dogmeat, Ever Loyal, Dr. Madison Li, Duchess, Wayward Tavernkeep, Elder Owyn Lyons, Everflowing Chalice, Expert-Level Safe, Farewell, Gemrazer, Junk Jet, Junktown, Liberty Prime, Recharged, Loyal Apprentice, Mechanized Production, Megaton's Fate, Mind Stone, Mister Gutsy, Moira Brown, Guide Author, Mr. House, President and CEO, Mystic Forge, Nick Valentine, Private Eye, Nuka-Cola Vending Machine, Nuka-Nuke Launcher, One with the Machine, Open the Vaults, Overencumbered, Paladin Danse, Steel Maverick, Panharmonicon, Piper Wright, Publick Reporter, Pitiless Plunderer, Powder Ganger, Puresteel Paladin, Putrefy, Rampaging Yao Guai, Roadside Reliquary, Robobrain War Mind, Rose, Cutthroat Raider, Sentinel Sarah Lyons, Shaun, Father of Synths, Spire of Industry, Steel Overseer, Sticky Fingers, Survivor's Med Kit, Synth Infiltrator, Talisman of Conviction, Talisman of Creativity, Talisman of Curiosity, Talisman of Dominance, Talisman of Hierarchy, Talisman of Indulgence, Talisman of Progress, Talisman of Resilience, The Prydwen, Steel Flagship, Thirst for Knowledge, Tireless Tracker, Unexpected Windfall, Vandalblast, Vault 101: Birthday Party, Veronica, Dissident Scribe, Wake the Past, Wayfarer's Bauble, Wear, Whirler Rogue, Winding Constrictor

### - [x] Token sacrifice and death payoffs (73 cards)

Use tokens as sacrifice fuel and reward deaths or departures.

**Engine support:** Supported: sacrifice costs/effects, zone-change triggers and dynamic amounts; verify multiplayer riders per card.

Cards: Bastion of Remembrance, Battle of Hoover Dam, Behemoth of Vault 0, Bighorner Rancher, Black Market, Boomer Scrapper, Bottle-Cap Blast, Break Down, Buried Ruin, C.A.M.P., Caesar, Legion's Emperor, Cass, Hand of Vengeance, Colonel Autumn, Commander Sofia Daguerre, Crimson Caravaneer, Deadly Dispute, Dogmeat, Ever Loyal, Duchess, Wayward Tavernkeep, ED-E, Lonesome Eyebot, Elder Arthur Maxson, Elder Owyn Lyons, Entrapment Maneuver, Evolving Wilds, Expert-Level Safe, Feral Ghoul, Gunner Conscript, HELIOS One, Hancock, Ghoulish Mayor, Harold and Bob, First Numens, Jason Bright, Glowing Prophet, Junk Jet, Junktown, Kellogg, Dangerous Mind, Legate Lanius, Caesar's Ace, Liberty Prime, Recharged, Memorial to Glory, Mind Stone, Mister Gutsy, Myriad Landscape, Nick Valentine, Private Eye, Nuka-Cola Vending Machine, Piper Wright, Publick Reporter, Pitiless Plunderer, Roadside Reliquary, Rose, Cutthroat Raider, Scavenger Grounds, Shaun, Father of Synths, Sierra, Nuka's Biggest Fan, Single Combat, Skullclamp, Solemn Simulacrum, Sticky Fingers, Super Mutant Scavenger, Survivor's Med Kit, Terramorphic Expanse, Three Dog, Galaxy News DJ, Thrill-Kill Disciple, Tireless Tracker, Treasure Vault, Unexpected Windfall, Vault 101: Birthday Party, Vault 112: Sadistic Simulation, Vault 11: Voter's Dilemma, Vault 12: The Necropolis, Vault 13: Dweller's Journey, Vault 21: House Gambit, Vault 75: Middle School, Vault 87: Forced Evolution, Veronica, Dissident Scribe, Wasteland, Wasteland Raider, Wayfarer's Bauble, Yes Man, Personal Securitron

### - [ ] Radiation (22 cards)

Player counters mill cards at the precombat main phase, then lose life and remove counters for nonland cards milled.

**Engine support:** Needs radiation counter type and turn-based processing.

Cards: Acquired Mutation, Bloatfly Swarm, Contaminated Drink, Feral Ghoul, Glowing One, Harold and Bob, First Numens, Infesting Radroach, Mariposa Military Base, Megaton's Fate, Mirelurk Queen, Nightkin Ambusher, Nuclear Fallout, Nuka-Nuke Launcher, Screeching Scorchbeast, Strong, the Brutish Thespian, Struggle for Project Purity, Survivor's Med Kit, Tato Farmer, The Master, Transcendent, The Wise Mothman, Vault 12: The Necropolis, Vexing Radgull

### - [x] Enchant (19 cards)

Aura attachment restrictions.

**Engine support:** Supported: `Aura targeting and attachment DSL`. Card-specific riders still need pre-flight.

Cards: Abundant Growth, Acquired Mutation, All That Glitters, Almost Perfect, Animal Friend, Fertile Ground, Fraying Sanity, Grim Reaper's Sprint, Idolized, Mantle of the Ancients, Mechanized Production, Nerd Rage, Overencumbered, Rancor, Squirrel Nest, Sticky Fingers, Strong Back, Well Rested, Wild Growth

### - [x] Equip (18 cards)

Attach Equipment to a creature at sorcery speed.

**Engine support:** Supported: `CardBuilder.equipAbility`. Card-specific riders still need pre-flight.

Cards: Basilisk Collar, Behemoth Sledge, Bloodforged Battle-Axe, Brass Knuckles, Champion's Helm, Explorer's Scope, Fireshrieker, Junk Jet, Lightning Greaves, Nuka-Nuke Launcher, Pip-Boy 3000, Plasma Caster, Power Fist, Pre-War Formalwear, Silver Shroud Costume, Skullclamp, Swiftfoot Boots, T-45 Power Armor

### - [x] Energy (17 cards)

Receive and spend player energy counters for activated abilities and attack payments.

**Engine support:** Supported: CounterType.ENERGY, counter costs and dynamic amounts; card-specific payments need pre-flight.

Cards: Assaultron Dominator, Automated Assembly Line, Behemoth of Vault 0, Brotherhood Scribe, Dr. Madison Li, Electrosiphon, Glimmer of Genius, HELIOS One, Liberty Prime, Recharged, Plasma Caster, Rex, Cyber-Hound, Robobrain War Mind, Sentry Bot, Synth Eradicator, T-45 Power Armor, The Motherlode, Excavator, Vault 112: Sadistic Simulation

### - [x] Junk (15 cards)

Artifact tokens tap and sacrifice at sorcery speed to exile and permit playing the top card this turn.

**Engine support:** Compose token activated abilities, linked exile and play permissions; no dedicated Junk facade found.

Cards: Boomer Scrapper, Break Down, C.A.M.P., Commander Sofia Daguerre, Crimson Caravaneer, Dogmeat, Ever Loyal, Duchess, Wayward Tavernkeep, Gunner Conscript, Junk Jet, Junktown, Mister Gutsy, Overencumbered, Rose, Cutthroat Raider, Thrill-Kill Disciple, Veronica, Dissident Scribe

### - [x] Scry (13 cards)

Look at the top cards and put any on the bottom.

**Engine support:** Supported: `Patterns.Library.scry`. Card-specific riders still need pre-flight.

Cards: Glimmer of Genius, Path of Ancestry, Temple of Abandon, Temple of Deceit, Temple of Enlightenment, Temple of Epiphany, Temple of Malady, Temple of Malice, Temple of Mystery, Temple of Plenty, Temple of Silence, Temple of Triumph, Vault 13: Dweller's Journey

### - [x] Trample (13 cards)

Assign excess combat damage to the defending player.

**Engine support:** Supported: `Keyword.TRAMPLE`. Card-specific riders still need pre-flight.

Cards: Agent Frank Horrigan, Alpha Deathclaw, Armory Paladin, Behemoth of Vault 0, Crimson Caravaneer, Gemrazer, Gunner Conscript, Liberty Prime, Recharged, Lumbering Megasloth, Marcus, Mutant Mayor, Rampaging Yao Guai, Super Mutant Scavenger, Vigor

### - [x] Treasure (12 cards)

Artifact tokens sacrificed for one mana of any color.

**Engine support:** Supported: `Effects.CreateTreasure`. Card-specific riders still need pre-flight.

Cards: Bottle-Cap Blast, Deadly Dispute, Kellogg, Dangerous Mind, Luck Bobblehead, Megaton's Fate, Mr. House, President and CEO, Nuka-Cola Vending Machine, Pitiless Plunderer, Sticky Fingers, Treasure Vault, Unexpected Windfall, Vault 21: House Gambit

### - [x] Vigilance (12 cards)

Attacking does not tap this creature.

**Engine support:** Supported: `Keyword.VIGILANCE`. Card-specific riders still need pre-flight.

Cards: Bighorner Rancher, Captain of the Watch, Cass, Hand of Vengeance, Desdemona, Freedom's Edge, Harold and Bob, First Numens, Liberty Prime, Recharged, Lily Bowen, Raging Grandma, Marcus, Mutant Mayor, Mirelurk Queen, Paladin Danse, Steel Maverick, Rampaging Yao Guai, Securitron Squadron

### - [x] Flying (10 cards)

Restrict blocking to creatures with flying or reach.

**Engine support:** Supported: `Keyword.FLYING`. Card-specific riders still need pre-flight.

Cards: Bloatfly Swarm, Brotherhood Vertibird, ED-E, Lonesome Eyebot, Hornet Queen, Infesting Radroach, Recon Craft Theta, Screeching Scorchbeast, The Prydwen, Steel Flagship, The Wise Mothman, Vexing Radgull

### - [x] Proliferate (8 cards)

Choose permanents and players with counters and add another of each kind.

**Engine support:** Supported: `Effects.Proliferate`. Card-specific riders still need pre-flight.

Cards: Agent Frank Horrigan, Atomize, Contagion Clasp, Inexorable Tide, Mutational Advantage, Radstorm, Recon Craft Theta, Vexing Radgull

### - [x] Sagas (8 cards)

Lore counters advance chapter abilities and the final chapter leads to sacrifice.

**Engine support:** Supported: Saga DSL and chapter effects.

Cards: Vault 101: Birthday Party, Vault 112: Sadistic Simulation, Vault 11: Voter's Dilemma, Vault 12: The Necropolis, Vault 13: Dweller's Journey, Vault 21: House Gambit, Vault 75: Middle School, Vault 87: Forced Evolution

### - [x] Menace (7 cards)

Require at least two blockers.

**Engine support:** Supported: `Keyword.MENACE`. Card-specific riders still need pre-flight.

Cards: Alpha Deathclaw, Butch DeLoria, Tunnel Snake, Feral Ghoul, Ruthless Radrat, Screeching Scorchbeast, Veronica, Dissident Scribe, Young Deathclaws

### - [x] Cycling (6 cards)

Pay and discard this card to draw a card.

**Engine support:** Supported: `KeywordAbility.Cycling`. Card-specific riders still need pre-flight.

Cards: Ash Barrens, Canyon Slough, Fetid Pools, Irrigated Farmland, Scattered Groves, Sheltered Thicket

### - [x] Food (6 cards)

Artifact tokens sacrificed for life.

**Engine support:** Supported: `Effects.CreateFood`. Card-specific riders still need pre-flight.

Cards: Nuka-Cola Vending Machine, Overencumbered, Sierra, Nuka's Biggest Fan, Survivor's Med Kit, Vault 101: Birthday Party, White Glove Gourmand

### - [x] Mill (6 cards)

Put the top cards of a library into its graveyard.

**Engine support:** Supported: `Patterns.Library.mill`. Card-specific riders still need pre-flight.

Cards: Dogmeat, Ever Loyal, Fraying Sanity, Glowing One, Infesting Radroach, Raul, Trouble Shooter, Rex, Cyber-Hound

### - [ ] Squad (6 cards)

Pay an additional cost any number of times and make that many token copies on entry.

**Engine support:** Needs repeatable cast-payment tracking linked to token-copy creation.

Cards: Gary Clone, Powder Ganger, Ruthless Radrat, Securitron Squadron, Thrill-Kill Disciple, Wasteland Raider

### - [x] Flash (5 cards)

Allow casting at instant timing.

**Engine support:** Supported: `Keyword.FLASH`. Card-specific riders still need pre-flight.

Cards: Commander Sofia Daguerre, Hullbreaker Horror, Mysterious Stranger, Sentry Bot, Silver Shroud Costume

### - [x] Haste (5 cards)

Allow immediate attacks and tap abilities.

**Engine support:** Supported: `Keyword.HASTE`. Card-specific riders still need pre-flight.

Cards: Kellogg, Dangerous Mind, Liberty Prime, Recharged, Loyal Apprentice, Sentinel Sarah Lyons, Synth Eradicator

### - [x] Investigate (4 cards)

Create a Clue token.

**Engine support:** Supported: `Effects.Investigate`. Card-specific riders still need pre-flight.

Cards: James, Wandering Dad, Nick Valentine, Private Eye, Piper Wright, Publick Reporter, Tireless Tracker

### - [x] Crew (3 cards)

Tap creatures with enough total power to animate a Vehicle.

**Engine support:** Supported: `KeywordAbility.Crew`. Card-specific riders still need pre-flight.

Cards: Brotherhood Vertibird, Recon Craft Theta, The Prydwen, Steel Flagship

### - [x] Deathtouch (3 cards)

Any positive damage to a creature is lethal.

**Engine support:** Supported: `Keyword.DEATHTOUCH`. Card-specific riders still need pre-flight.

Cards: Glowing One, Grave Titan, Hornet Queen

### - [x] Indestructible (3 cards)

Prevent destruction.

**Engine support:** Supported: `Keyword.INDESTRUCTIBLE`. Card-specific riders still need pre-flight.

Cards: Razortide Bridge, Rustvale Bridge, Silverbluff Bridge

### - [x] Lifelink (3 cards)

Damage also gains its controller life.

**Engine support:** Supported: `Keyword.LIFELINK`. Card-specific riders still need pre-flight.

Cards: Colonel Autumn, Craig Boone, Novac Guard, Paladin Danse, Steel Maverick

### - [x] Metalcraft (3 cards)

A condition requiring three or more artifacts under your control.

**Engine support:** Supported: `artifact-count conditions and dynamic amounts`. Card-specific riders still need pre-flight.

Cards: Brotherhood Scribe, Dispatch, Puresteel Paladin

### - [x] Reach (3 cards)

Allow blocking flying creatures.

**Engine support:** Supported: `Keyword.REACH`. Card-specific riders still need pre-flight.

Cards: Craig Boone, Novac Guard, Gemrazer, Harold and Bob, First Numens

### - [x] Battalion (2 cards)

An attack trigger requiring this and at least two other attacking creatures.

**Engine support:** Supported: `attack triggers with AttackerCountAtLeast`. Card-specific riders still need pre-flight.

Cards: Paladin Elizabeth Taggerdy, Sentinel Sarah Lyons

### - [x] Convoke (2 cards)

Tap creatures to help pay a spell's mana cost.

**Engine support:** Supported: `Keyword.CONVOKE`. Card-specific riders still need pre-flight.

Cards: Hour of Reckoning, Lethal Scheme

### - [x] First strike (2 cards)

Deal combat damage in the first damage step.

**Engine support:** Supported: `Keyword.FIRST_STRIKE`. Card-specific riders still need pre-flight.

Cards: Kellogg, Dangerous Mind, Rose, Cutthroat Raider

### - [x] Goad (2 cards)

Require attacks and prefer players other than the goader.

**Engine support:** Supported: `Effects.Goad`. Card-specific riders still need pre-flight.

Cards: Acquired Mutation, Red Death, Shipwrecker

### - [x] Improvise (2 cards)

Tap artifacts to help pay a spell's generic mana cost.

**Engine support:** Supported: `Keyword.IMPROVISE`. Card-specific riders still need pre-flight.

Cards: Bottle-Cap Blast, Synth Infiltrator

### - [x] Landfall (2 cards)

Trigger when a land enters under your control.

**Engine support:** Supported: `Triggers.a land-entry filter`. Card-specific riders still need pre-flight.

Cards: Tato Farmer, Tireless Tracker

### - [x] Multikicker (2 cards)

Repeat an optional additional casting payment.

**Engine support:** Supported: `KeywordAbility.multikicker`. Card-specific riders still need pre-flight.

Cards: Everflowing Chalice, Marshal's Anthem

### - [x] Split second (2 cards)

Restrict spells and nonmana activations while this spell is on the stack.

**Engine support:** Supported: `Keyword.SPLIT_SECOND`. Card-specific riders still need pre-flight.

Cards: Inventory Management, V.A.T.S.

### - [x] Ward (2 cards)

Counter an opposing targeted spell or ability unless its controller pays.

**Engine support:** Supported: `KeywordAbility.Ward`. Card-specific riders still need pre-flight.

Cards: Nightkin Ambusher, Strong, the Brutish Thespian

### - [x] Basic landcycling (1 cards)

Pay and discard this card to search for a basic land.

**Engine support:** Supported: `KeywordAbility.BasicLandcycling`. Card-specific riders still need pre-flight.

Cards: Ash Barrens

### - [x] Connive (1 cards)

Draw, discard, and gain a counter if a nonland card was discarded.

**Engine support:** Supported: `Effects.Connive`. Card-specific riders still need pre-flight.

Cards: Lethal Scheme

### - [ ] Dice rolling (1 cards)

Roll dice and branch on results, including artifact-count bonuses.

**Engine support:** Needs dice outcomes, result tracking and roll-trigger support.

Cards: Mr. House, President and CEO

### - [x] Double strike (1 cards)

Deal damage in both combat damage steps.

**Engine support:** Supported: `Keyword.DOUBLE_STRIKE`. Card-specific riders still need pre-flight.

Cards: Crimson Caravaneer

### - [x] Enlist (1 cards)

Tap a qualifying nonattacking creature to add its power to an attacker.

**Engine support:** Supported: `StaticAbility.Enlist and Triggers.self.enlists`. Card-specific riders still need pre-flight.

Cards: Aradesh, the Founder

### - [x] Enrage (1 cards)

Trigger when this creature is dealt damage.

**Engine support:** Supported: `damage triggers`. Card-specific riders still need pre-flight.

Cards: Strong, the Brutish Thespian

### - [x] Evolve (1 cards)

Gain a counter when a creature enters with greater power or toughness.

**Engine support:** Supported: `evolve()`. Card-specific riders still need pre-flight.

Cards: Watchful Radstag

### - [x] Exploit (1 cards)

May sacrifice a creature on entry, with a linked payoff.

**Engine support:** Supported: `exploit() and Triggers.you.exploits`. Card-specific riders still need pre-flight.

Cards: Colonel Autumn

### - [x] Fight (1 cards)

Two creatures deal damage equal to their power to one another.

**Engine support:** Supported: `Effects.Fight`. Card-specific riders still need pre-flight.

Cards: Ayula, Queen Among Bears

### - [ ] Fortify (1 cards)

Attach a Fortification to a land at sorcery speed.

**Engine support:** Needs land attachment support for Fortifications.

Cards: C.A.M.P.

### - [ ] Fuse (1 cards)

Cast both halves of a split card from hand together.

**Engine support:** Split faces exist, but combined Fuse casting is missing; needs a cast-both-halves path.

Cards: Wear

### - [x] Hideaway (1 cards)

Exile a selected top card face down and link it to this permanent.

**Engine support:** Supported: `KeywordAbility.hideaway plus linked-exile composition`. Card-specific riders still need pre-flight.

Cards: Windbrisk Heights

### - [x] Lieutenant (1 cards)

A bonus while you control your commander.

**Engine support:** Supported: `commander-presence conditions`. Card-specific riders still need pre-flight.

Cards: Loyal Apprentice

### - [x] Monstrosity (1 cards)

If not monstrous, add counters and become monstrous.

**Engine support:** Supported: `Effects.Monstrosity`. Card-specific riders still need pre-flight.

Cards: Alpha Deathclaw

### - [x] Morbid (1 cards)

A condition checking whether a creature died this turn.

**Engine support:** Supported: `Conditions.CreatureDiedThisTurn`. Card-specific riders still need pre-flight.

Cards: Grim Reaper's Sprint

### - [ ] Mutate (1 cards)

Merge this creature spell with a qualifying non-Human creature.

**Engine support:** Needs merged-permanent and mutate casting support.

Cards: Gemrazer

### - [x] Overload (1 cards)

Replace targeted wording with an all-object alternative.

**Engine support:** Supported: `overload casting support`. Card-specific riders still need pre-flight.

Cards: Vandalblast

### - [x] Raid (1 cards)

A condition checking whether you attacked this turn.

**Engine support:** Supported: `turn attack tracking`. Card-specific riders still need pre-flight.

Cards: Rose, Cutthroat Raider

### - [x] Storm (1 cards)

Copy a spell for each spell cast before it this turn.

**Engine support:** Supported: `Keyword.STORM`. Card-specific riders still need pre-flight.

Cards: Radstorm

### - [x] Undying (1 cards)

Return a creature that died without a +1/+1 counter, with one.

**Engine support:** Supported: `Keyword.UNDYING`. Card-specific riders still need pre-flight.

Cards: Hancock, Ghoulish Mayor
