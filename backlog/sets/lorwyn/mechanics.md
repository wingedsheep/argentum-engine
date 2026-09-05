# Lorwyn (LRW) — Mechanics

Audited against `build/backlog/lrw-oracle.tsv` (286 unique card names), the
[SDK language reference](../../../docs/card-sdk-language-reference.md), SDK source and the
[Lorwyn card corpus](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/).

A checked entry means the named mechanic has existing authoring vocabulary and an identified
implementation. It does
**not** certify every listed card's script or scenario tests. An unchecked entry identifies an
unimplemented package or a concrete semantic gap that must be resolved before set completion;
composition of existing primitives is preferred to adding a new engine type.

Card lists include printed uses, grants, token abilities and explicit references to the mechanic.
They overlap intentionally; they are not a partition of the set. Basic evergreen descriptions use
rule names rather than unverified rule numbers. Sections are ordered by card count, including
unnamed templates. Routine one-shot effects are covered by the SDK catalog; the set-specific
packages and exceptional interactions are called out here.

### - [x] Flying (36 cards)

Blocks creatures with flying and can be blocked only by creatures with flying or reach.

**Engine support:** Supported: `Keyword.FLYING`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Avian Changeling](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AvianChangeling.kt).

Cards: Avian Changeling, Boggart Sprite-Chaser, Cairn Wanderer, Cloudcrown Oak, Cloudgoat Ranger, Cloudthresher, Dreamspoiler Witches, Ethereal Whiskergill, Faerie Harbinger, Galepowder Mage, Glen Elendra Pranksters, Hoofprints of the Stag, Hurly-Burly, Jagged-Scar Archers, Kinsbaile Balloonist, Lowland Oaf, Marsh Flitter, Mistbind Clique, Mulldrifter, Nectar Faerie, Nightshade Stinger, Oona's Prowler, Pestermite, Plover Knights, Purity, Ringskipper, Scion of Oona, Sentinels of Glen Elendra, Soaring Hope, Sower of Temptation, Spellstutter Sprite, Thieving Sprite, Wings of Velis Vel, Wispmare, Wydwen, the Biting Gale, Zephyr Net

### - [x] Kindred card type (31 cards)

Noncreature spells and permanents carry creature subtypes and participate in subtype-based casting triggers, targeting, costs and recursion.

**Engine support:** Supported: Kindred type lines and `GameObjectFilter.Any.withSubtype(...)`; [Tarfire](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Tarfire.kt), [Wort, Boggart Auntie](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/WortBoggartAuntie.kt) and [Auntie's Hovel](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AuntiesHovel.kt). Do not narrow a printed “Goblin card” or “Faerie” to a creature.

Cards: Aquitect's Will, Blades of Velis Vel, Boggart Birth Rite, Boggart Shenanigans, Consuming Bonfire, Crib Swap, Crush Underfoot, Ego Erasure, Elvish Promenade, Eyeblight's Ending, Eyes of the Wisent, Faerie Tauntings, Faerie Trickery, Favor of the Mighty, Fodder Launch, Giant's Ire, Gilt-Leaf Ambush, Hoofprints of the Stag, Lignify, Merrow Commerce, Militia's Pride, Nameless Inversion, Peppersmoke, Prowess of the Fair, Rebellion of the Flamekin, Rootgrapple, Shields of Velis Vel, Summon the School, Surge of Thoughtweft, Tarfire, Wings of Velis Vel

### - [x] Clash (23 cards)

Each participant reveals their top library card, chooses top or bottom, and wins only if their revealed mana value is greater than the other revealed cards. Clash observers trigger after the procedure finishes.

**Engine support:** Supported core: `Patterns.Mechanic.clash`, `ClashEffect`, `Triggers.WheneverYouClash`, and `Triggers.WheneverYouClashAndWin`; see [Adder-Staff Boggart](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AdderStaffBoggart.kt) and [Sylvan Echoes](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/SylvanEchoes.kt). Broken Ambitions captures the target spell’s controller before countering and mills that player on a clash win; twelve scenarios cover payment, uncounterable spells, fizzles, and a distinct multiplayer clash opponent. Other individual riders still need card tests, especially delayed mana and remembered target characteristics.

Cards: Adder-Staff Boggart, Bog Hoodlums, Broken Ambitions, Captivating Glance, Entangling Trap, Fistful of Force, Gilt-Leaf Ambush, Hoarder's Greed, Lash Out, Nath's Elite, Oaken Brawler, Paperfin Rascal, Pollen Lullaby, Rebellion of the Flamekin, Ringskipper, Scattering Stroke, Sentry Oak, Spring Cleaning, Springjack Knight, Sylvan Echoes, Weed Strangle, Whirlpool Whelm, Woodland Guidance

### - [x] Token creation (23 cards)

Create the printed number and kind of tokens, sometimes with a dynamic count, a copied characteristic set or tapped-and-attacking status.

**Engine support:** Supported token and copy-token effects; [Cloudgoat Ranger](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/CloudgoatRanger.kt), [Militia's Pride](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/MilitiasPride.kt) and [Heat Shimmer](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HeatShimmer.kt). Hostility's prevention-driven token creation remains a separate gap.

Cards: Ajani Goldmane, Benthicore, Boggart Mob, Cloudgoat Ranger, Crib Swap, Elvish Promenade, Eyes of the Wisent, Garruk Wildspeaker, Gilt-Leaf Ambush, Guardian of Cloverdell, Hearthcage Giant, Heat Shimmer, Hoofprints of the Stag, Hostility, Imperious Perfect, Lys Alana Huntmaster, Marsh Flitter, Militia's Pride, Nath of the Gilt-Leaf, Prowess of the Fair, Rebellion of the Flamekin, Summon the School, Wren's Run Packmaster

### - [x] Changeling (19 cards)

The card has every creature type in every zone. This also makes noncreature Kindred cards eligible for appropriate creature-type references.

**Engine support:** Supported: `Keyword.CHANGELING`; [Avian Changeling](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AvianChangeling.kt) and [Crib Swap](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/CribSwap.kt). Type-changing effects are listed separately because granting all creature types is not the same wording as granting a keyword.

Cards: Amoeboid Changeling, Avian Changeling, Blades of Velis Vel, Cairn Wanderer, Changeling Berserker, Changeling Hero, Changeling Titan, Crib Swap, Ego Erasure, Fire-Belly Changeling, Ghostly Changeling, Mirror Entity, Nameless Inversion, Shapesharer, Shields of Velis Vel, Skeletal Changeling, Turtleshell Changeling, Wings of Velis Vel, Woodland Changeling

### - [x] Creature-type counts (13 cards)

Use the current count of a named tribe to size damage, stat changes, counters, tokens, hand reveals or spell targets. Overlapping tribes count a permanent once when the text says “and/or”.

**Engine support:** Supported primitives: projected subtype filters, `DynamicAmount.Count` and dynamic target thresholds; [Harpoon Sniper](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HarpoonSniper.kt), [Dauntless Dourbark](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DauntlessDourbark.kt) and [Silvergill Douser](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/SilvergillDouser.kt).

Cards: Cenn's Heir, Dauntless Dourbark, Elvish Branchbender, Elvish Eulogist, Elvish Promenade, Harpoon Sniper, Immaculate Magistrate, Jagged-Scar Archers, Lys Alana Scarblade, Silvergill Douser, Spellstutter Sprite, Thieving Sprite, Thundercloud Shaman

### - [x] Haste (13 cards)

Allows attacking and tap/untap-symbol activations without waiting through summoning sickness.

**Engine support:** Supported: `Keyword.HASTE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Horde of Notions](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HordeofNotions.kt).

Cards: Ashling's Prerogative, Cairn Wanderer, Changeling Berserker, Glarewielder, Goatnapper, Heat Shimmer, Horde of Notions, Hostility, Incandescent Soulstoke, Inner-Flame Acolyte, Rebellion of the Flamekin, Thousand-Year Elixir, Warren Pilferers

### - [x] Evoke (12 cards)

An alternative casting cost causes a sacrifice trigger when the permanent enters if that cost was used. Its other enter triggers still trigger.

**Engine support:** Supported: `evoke` on the card script; [Mulldrifter](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Mulldrifter.kt) and [Cloudthresher](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Cloudthresher.kt).

Cards: Aethersnipe, Briarhorn, Cloudthresher, Dawnfluke, Faultgrinder, Glarewielder, Ingot Chewer, Inner-Flame Acolyte, Mournwhelk, Mulldrifter, Shriekmaw, Wispmare

### - [x] Flash (12 cards)

Allows casting at instant timing.

**Engine support:** Supported: `Keyword.FLASH`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Pestermite](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Pestermite.kt).

Cards: Briarhorn, Cloudthresher, Dawnfluke, Epic Proportions, Faerie Harbinger, Mistbind Clique, Pestermite, Scion of Oona, Sentinels of Glen Elendra, Spellstutter Sprite, Triclopean Sight, Wydwen, the Biting Gale

### - [x] Trample (12 cards)

Allows excess combat damage to be assigned to the defending player or attacked permanent.

**Engine support:** Supported: `Keyword.TRAMPLE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Oakgnarl Warrior](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/OakgnarlWarrior.kt).

Cards: Cairn Wanderer, Dauntless Dourbark, Epic Proportions, Faultgrinder, Fistful of Force, Garruk Wildspeaker, Horde of Notions, Nova Chaser, Oakgnarl Warrior, Soulbright Flamekin, Sunrise Sovereign, Vigor

### - [x] Aura attachment / enchant (11 cards)

The enchant restriction defines legal targets while casting and legal hosts while attached. An Aura falls off if its host ceases to qualify.

**Engine support:** Supported: Aura DSL, target filters and attachment-based static effects; [Glimmerdust Nap](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/GlimmerdustNap.kt), [Lignify](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Lignify.kt) and [Nettlevine Blight](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/NettlevineBlight.kt).

Cards: Battle Mastery, Captivating Glance, Epic Proportions, Fertile Ground, Glimmerdust Nap, Lignify, Nettlevine Blight, Protective Bubble, Soaring Hope, Triclopean Sight, Zephyr Net

### - [x] Creature-type lords (10 cards)

Continuously grant stats or abilities to a specified tribe; “other” excludes the source, and noncreature Kindred permanents qualify where the wording does not say creature.

**Engine support:** Supported: `ModifyStats`, `GrantKeyword`, projected `GroupFilter`; [Merrow Reejerey](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/MerrowReejerey.kt) and [Timber Protector](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/TimberProtector.kt).

Cards: Caterwauling Boggart, Imperious Perfect, Incandescent Soulstoke, Mad Auntie, Merrow Reejerey, Scion of Oona, Sunrise Sovereign, Timber Protector, Wizened Cenn, Wren's Run Packmaster

### - [x] Variable and altered power/toughness (10 cards)

Set characteristic-defined values, set base stats temporarily, exchange power and toughness, or assign combat damage from toughness.

**Engine support:** Supported main atoms: dynamic base stats, base-stat effects and `AssignDamageEqualToToughness`; [Dauntless Dourbark](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DauntlessDourbark.kt), [Mirror Entity](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/MirrorEntity.kt) and [Doran, the Siege Tower](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DorantheSiegeTower.kt). Turtleshell Changeling now uses `Effects.SwitchPowerToughness(target, duration)`, with seven card scenarios for repeated switches, stat boosts, cleanup, damage, and source blink. Three shared engine scenarios cover other targets, permanent duration, noncreature animation, and event emission.

Cards: Ajani Goldmane, Dauntless Dourbark, Doran, the Siege Tower, Elvish Branchbender, Jagged-Scar Archers, Lignify, Marsh Flitter, Mirror Entity, Turtleshell Changeling, Wings of Velis Vel

### - [ ] All creature types / loss of creature types (9 cards)

Add every creature subtype or remove creature subtypes until end of turn, or set an enchanted creature to Treefolk. These are type-layer changes.

**Engine support:** Existing authoring uses `Effects.GrantKeyword(CHANGELING)`, `Effects.LoseAllCreatureTypes` and subtype changes; see [Amoeboid Changeling](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AmoeboidChangeling.kt) and [Lignify](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Lignify.kt). Verify the all-types implementation under subsequent ability removal: granting the changeling keyword must not make a direct type-changing effect disappear when abilities are stripped. This interaction is not established by the current card scripts.

Cards: Amoeboid Changeling, Blades of Velis Vel, Ego Erasure, Lignify, Mirror Entity, Nameless Inversion, Runed Stalactite, Shields of Velis Vel, Wings of Velis Vel

### - [ ] Champion (9 cards)

An enter trigger sacrifices the source unless another qualifying permanent is exiled; a separate leave trigger returns its linked exiled card. Choosing a permanent does not target.

**Engine support:** Incomplete: no champion DSL/catalog entry or champion card implementation was found. Compose the optional selection, sacrifice fallback and linked exile/return before adding new vocabulary. Mistbind Clique also needs the successful-champion event/rider, and leave-before-enter ordering needs verification.

Cards: Boggart Mob, Changeling Berserker, Changeling Hero, Changeling Titan, Mistbind Clique, Nova Chaser, Thoughtweft Trio, Wanderwine Prophets, Wren's Run Packmaster

### - [x] Creature-type presence conditions (9 cards)

A tribal presence check enables combat, continuous bonuses or a spell's draw rider.

**Engine support:** Supported: `ConditionalStaticAbility`, conditions and subtype filters; [Kithkin Greatheart](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/KithkinGreatheart.kt) and [Surge of Thoughtweft](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/SurgeofThoughtweft.kt).

Cards: Aquitect's Will, Blind-Spot Giant, Boggart Sprite-Chaser, Dauntless Dourbark, Giant's Ire, Kithkin Greatheart, Peppersmoke, Rootgrapple, Surge of Thoughtweft

### - [x] Landwalk (9 cards)

A creature is unblockable while its defending player controls the specified land type. Includes granted landwalk and graveyard-derived landwalk.

**Engine support:** Supported: landwalk keyword variants in the keyword model; [Hillcomber Giant](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HillcomberGiant.kt), [Deeptread Merrow](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DeeptreadMerrow.kt) and [Streambed Aquitects](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/StreambedAquitects.kt). Cairn Wanderer's keyword acquisition is a separate gap.

Cards: Bog-Strider Ash, Boggart Loggers, Cairn Wanderer, Deeptread Merrow, Hillcomber Giant, Inkfathom Divers, Merrow Harbinger, Streambed Aquitects, Sygg, River Guide

### - [x] First strike (8 cards)

Deals combat damage in the first combat-damage step.

**Engine support:** Supported: `Keyword.FIRST_STRIKE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Plover Knights](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/PloverKnights.kt).

Cards: Brigid, Hero of Kinsbaile, Cairn Wanderer, Inner-Flame Igniter, Kithkin Greatheart, Knight of Meadowgrain, Lairwatch Giant, Plover Knights, Thoughtweft Trio

### - [x] Harbinger tutors (8 cards)

An enter trigger optionally searches for a card of the tribe (Treefolk also permits a Forest), reveals it, then shuffles and puts it on top.

**Engine support:** Supported: library search patterns; [Treefolk Harbinger](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/TreefolkHarbinger.kt) and [Boggart Harbinger](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/BoggartHarbinger.kt).

Cards: Boggart Harbinger, Elvish Harbinger, Faerie Harbinger, Flamekin Harbinger, Giant Harbinger, Kithkin Harbinger, Merrow Harbinger, Treefolk Harbinger

### - [x] Modal spells (8 cards)

Choose one or two modes as instructed and choose the targets associated with those modes when casting.

**Engine support:** Supported: `modal` / `ModalEffect.chooseN` and per-mode targets; [Cryptic Command](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/CrypticCommand.kt) and [Consuming Bonfire](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ConsumingBonfire.kt). The generated “Choose one/two” rows are modal templates, not ability words.

Cards: Austere Command, Consuming Bonfire, Cryptic Command, Final Revels, Hurly-Burly, Incendiary Command, Primal Command, Profane Command

### - [x] Creature-type sacrifice and discard costs (7 cards)

Pay a spell or activated ability cost with a specified tribe; a Kindred noncreature card/permanent is legal unless the text explicitly requires a creature.

**Engine support:** Supported: filtered sacrifice/discard cost atoms; [Facevaulter](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Facevaulter.kt), [Fodder Launch](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/FodderLaunch.kt) and [Lys Alana Scarblade](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/LysAlanaScarblade.kt).

Cards: Facevaulter, Fodder Launch, Guardian of Cloverdell, Hearthcage Giant, Lys Alana Scarblade, Marsh Flitter, Tar Pitcher

### - [x] Creature-type spell triggers (7 cards)

Trigger on casting a particular tribe, including Kindred noncreature spells and changelings. Some trigger for either player.

**Engine support:** Supported: `Triggers.YouCastSubtype` and filtered cast events; [Merrow Reejerey](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/MerrowReejerey.kt) and [Elvish Handservant](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ElvishHandservant.kt).

Cards: Battlewand Oak, Bog-Strider Ash, Elvish Handservant, Lys Alana Huntmaster, Merrow Reejerey, Quill-Slinger Boggart, Thorntooth Witch

### - [x] Fear (7 cards)

Can be blocked only by artifact creatures or black creatures.

**Engine support:** Supported: `Keyword.FEAR`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Shriekmaw](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Shriekmaw.kt).

Cards: Cairn Wanderer, Dread, Profane Command, Shriekmaw, Spiderwig Boggart, Squeaking Pie Sneak, Wort, Boggart Auntie

### - [x] Vigilance (7 cards)

Attacking does not cause the creature to tap.

**Engine support:** Supported: `Keyword.VIGILANCE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Oakgnarl Warrior](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/OakgnarlWarrior.kt).

Cards: Ajani Goldmane, Arbiter of Knollridge, Cairn Wanderer, Horde of Notions, Oakgnarl Warrior, Thoughtweft Trio, Triclopean Sight

### - [x] Deathtouch (6 cards)

Any nonzero damage to a creature is lethal for damage assignment and destruction by state-based actions.

**Engine support:** Supported: `Keyword.DEATHTOUCH`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Moonglove Winnower](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/MoongloveWinnower.kt).

Cards: Cairn Wanderer, Gilt-Leaf Ambush, Lace with Moonglove, Moonglove Winnower, Wren's Run Packmaster, Wren's Run Vanquisher

### - [x] Protection (6 cards)

Prevents damage, attachment, blocking and targeting from the specified quality, including creature types and a chosen color.

**Engine support:** Supported: `KeywordAbility` plus `ProtectionScope`; [Nath's Buffoon](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/NathsBuffoon.kt) and [Sygg, River Guide](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/SyggRiverGuide.kt). Cairn Wanderer needs to acquire the actual protection qualities, not just a generic keyword.

Cards: Burrenton Forge-Tender, Cairn Wanderer, Favor of the Mighty, Nath's Buffoon, Sygg, River Guide, Warren-Scourge Elf

### - [ ] Hideaway and conditional free play (5 cards)

On entry, look at four cards, exile one face down, and randomize the remainder onto the library bottom. A separate activated ability conditionally permits playing that card during resolution.

**Engine support:** Incomplete card package: `KeywordAbility.hideaway(4)`, linked face-down exile and free-play atoms exist, but [Howltooth Hollow](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HowltoothHollow.kt) currently puts the printed condition in `ActivationRestriction.OnlyIfCondition` and grants later play permissions. Verify resolution-time condition checks, immediate optional land/spell play and the continuing hideaway look permission for each controller; these semantics are not proved by the current implementation.

Cards: Howltooth Hollow, Mosswort Bridge, Shelldock Isle, Spinerock Knoll, Windbrisk Heights

### - [ ] Incarnation graveyard shuffle triggers (5 cards)

When the card enters a graveyard from any zone, trigger to shuffle that card into its owner's library. It really enters the graveyard first.

**Engine support:** Incomplete package: `Effects.ShuffleIntoLibrary` and generic zone-change triggers exist, but none of this five-card cycle is authored. Prove self-trigger registration from library, hand, stack and battlefield; a replacement that skips the graveyard is not equivalent.

Cards: Dread, Guile, Hostility, Purity, Vigor

### - [ ] Reveal a tribal card or pay extra mana (5 cards)

An additional casting cost requires either revealing a card of the tribe from hand or paying {3}.

**Engine support:** Incomplete: `CostAtom.RevealFromHand` exists, but the five cards are unauthored and the documented `Costs.pay.RevealCard` consumer is morph only. Need a cast-time choice between reveal and extra mana, integrated with legal-action enumeration and authoritative payment; an enter trigger or resolution-time optional payment is too late.

Cards: Flamekin Bladewhirl, Goldmeadow Stalwart, Silvergill Adept, Squeaking Pie Sneak, Wren's Run Vanquisher

### - [x] Lifelink (5 cards)

Damage causes the source controller to gain that much life as part of the damage result.

**Engine support:** Supported: `Keyword.LIFELINK`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Knight of Meadowgrain](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/KnightofMeadowgrain.kt).

Cards: Brion Stoutarm, Cairn Wanderer, Changeling Hero, Knight of Meadowgrain, Nectar Faerie

### - [x] Planeswalkers and loyalty (5 cards)

Planeswalkers enter with loyalty and use once-per-turn, sorcery-timing loyalty abilities, including variable loyalty costs.

**Engine support:** Supported: `loyaltyAbility` / loyalty costs; [Ajani Goldmane](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AjaniGoldmane.kt), [Garruk Wildspeaker](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/GarrukWildspeaker.kt) and [Liliana Vess](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/LilianaVess.kt). Chandra's variable cost and player-or-planeswalker controller rider still need their own card test.

Cards: Ajani Goldmane, Chandra Nalaar, Garruk Wildspeaker, Jace Beleren, Liliana Vess

### - [x] Regeneration (5 cards)

Creates a shield that replaces the next destruction this turn, tapping the permanent, removing damage and removing it from combat.

**Engine support:** Supported: `RegenerateEffect`; [Black Poplar Shaman](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/BlackPoplarShaman.kt) and [Heal the Scars](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HealtheScars.kt).

Cards: Black Poplar Shaman, Heal the Scars, Herbal Poultice, Mad Auntie, Skeletal Changeling

### - [x] Tap creatures as a cost (5 cards)

Tap one or several untapped creatures of a specified tribe to pay a cost; the creature need not be able to pay its own tap-symbol cost.

**Engine support:** Supported: composite and filtered tap costs; [Cloudgoat Ranger](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/CloudgoatRanger.kt), [Drowner of Secrets](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DrownerofSecrets.kt) and [Summon the School](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/SummontheSchool.kt).

Cards: Benthicore, Cloudgoat Ranger, Drowner of Secrets, Springleaf Drum, Summon the School

### - [x] Tribal reveal lands (5 cards)

As a land enters, its controller may reveal a card of its tribe; otherwise it enters tapped. This is an entry replacement, not an enter trigger.

**Engine support:** Supported: `OnEnterRunEffect(Effects.MayRevealCardFromHand(...))`; [Auntie's Hovel](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AuntiesHovel.kt).

Cards: Ancient Amphitheater, Auntie's Hovel, Gilt-Leaf Palace, Secluded Glen, Wanderwine Hub

### - [x] Vivid lands (5 cards)

Enter tapped with two charge counters; tap for the printed color or remove a charge counter while tapping to produce any color.

**Engine support:** Supported: enters-with counters plus composite remove-counter mana costs; [Vivid Creek](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/VividCreek.kt).

Cards: Vivid Crag, Vivid Creek, Vivid Grove, Vivid Marsh, Vivid Meadow

### - [ ] Combat requirements and multiple blocking (4 cards)

Require a creature to block another, require all able creatures to block an attacker, or permit a blocker to block additional attackers. Requirements must be evaluated with restrictions.

**Engine support:** Partial: [Lairwatch Giant](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/LairwatchGiant.kt) demonstrates extra blocking; Nath's Elite now uses `MustBeBlocked(allCreatures = true)`. Hunt Down uses `ForceBlock` with two spell targets. Its scenarios cover projected animated lands, impossible blocks, partially illegal targets, both creatures leaving and returning, and end-of-turn expiry. Multiple simultaneous requirements still need the broader mechanic audit before declaring every template supported.

Cards: Hunt Down, Lairwatch Giant, Nath's Elite, Thoughtweft Trio

### - [x] Becomes-tapped triggers (4 cards)

Trigger whenever the permanent or a specified Merfolk becomes tapped, including tapping to attack or to pay another ability's cost.

**Engine support:** Supported: `Triggers.becomesTapped` / tap events; [Judge of Currents](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/JudgeofCurrents.kt) and [Fallowsage](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Fallowsage.kt).

Cards: Fallowsage, Judge of Currents, Surgespanner, Veteran of the Depths

### - [x] Copying and retargeting (4 cards)

Copy creature characteristics or stack objects, optionally retaining added abilities and choosing new targets.

**Engine support:** Supported: `Effects.EachPermanentBecomesCopyOfTarget`, token copies and `Effects.CopyTargetSpellOrAbility`; [Shapesharer](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Shapesharer.kt), [Heat Shimmer](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/HeatShimmer.kt), [Rings of Brighthearth](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/RingsofBrighthearth.kt) and [Wild Ricochet](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/WildRicochet.kt).

Cards: Heat Shimmer, Rings of Brighthearth, Shapesharer, Wild Ricochet

### - [x] Land-type changes and animation (4 cards)

Change a land's basic land types, optionally add a creature type and dynamic power/toughness, with the appropriate mana abilities and duration.

**Engine support:** Supported atoms: land-type changes and animation; [Streambed Aquitects](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/StreambedAquitects.kt), [Tideshaper Mystic](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/TideshaperMystic.kt) and [Elvish Branchbender](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ElvishBranchbender.kt). Aquitect's Will is tracked under counter-bound duration.

Cards: Aquitect's Will, Elvish Branchbender, Streambed Aquitects, Tideshaper Mystic

### - [x] Mill (4 cards)

Moves the specified number of top library cards to the graveyard, with any downstream rider reading the actual moved cards.

**Engine support:** Supported: library pipeline / mill effects; [Drowner of Secrets](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DrownerofSecrets.kt) and [Lammastide Weave](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/LammastideWeave.kt).

Cards: Broken Ambitions, Drowner of Secrets, Jace Beleren, Lammastide Weave

### - [x] Opponent-turn spell payoffs (4 cards)

Observe spells cast during another player's turn, or an opponent's blue spell during your turn.

**Engine support:** Supported: filtered cast triggers and turn conditions; [Dreamspoiler Witches](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/DreamspoilerWitches.kt), [Faerie Tauntings](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/FaerieTauntings.kt) and [Eyes of the Wisent](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/EyesoftheWisent.kt).

Cards: Dreamspoiler Witches, Eyes of the Wisent, Faerie Tauntings, Glen Elendra Pranksters

### - [x] Shroud (4 cards)

The permanent cannot be targeted by spells or abilities.

**Engine support:** Supported: `Keyword.SHROUD`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Protective Bubble](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ProtectiveBubble.kt).

Cards: Benthicore, Cairn Wanderer, Protective Bubble, Scion of Oona

### - [x] Tribal death payoffs (4 cards)

Observe a tribe going from battlefield to graveyard, with attacking, nontoken, owner or controller restrictions as printed. Use last-known characteristics.

**Engine support:** Supported: filtered zone-change events with last-known data; [Knucklebone Witch](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/KnuckleboneWitch.kt) and [Prowess of the Fair](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ProwessoftheFair.kt).

Cards: Boggart Shenanigans, Kithkin Mourncaller, Knucklebone Witch, Prowess of the Fair

### - [ ] Prevention with immediate additional results (3 cards)

Prevent damage and immediately gain life, create tokens or add counters equal to damage actually prevented. The additional result is part of the prevention effect, not a later triggered ability.

**Engine support:** Gap: `PreventDamage` has amount/event/restrictions only. `PreventDamageEffect.onPrevented` is documented as a delayed stack trigger and `ReplaceDamageWithCounters` explicitly replaces rather than prevents damage. Neither establishes these three printed prevention effects; add a reusable prevention outcome mechanism that respects unpreventable damage.

Cards: Hostility, Purity, Vigor

### - [x] Cost increases, reductions and restricted mana (3 cards)

Modify spell costs or generate mana restricted to casting a tribe and activating its abilities.

**Engine support:** Supported: filtered cost modifiers and `ManaRestriction`; [Stinkdrinker Daredevil](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/StinkdrinkerDaredevil.kt), [Thorn of Amethyst](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ThornofAmethyst.kt) and [Smokebraider](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Smokebraider.kt).

Cards: Smokebraider, Stinkdrinker Daredevil, Thorn of Amethyst

### - [x] Double strike (3 cards)

Deals combat damage in both combat-damage steps.

**Engine support:** Supported: `Keyword.DOUBLE_STRIKE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Battle Mastery](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/BattleMastery.kt).

Cards: Battle Mastery, Cairn Wanderer, Springjack Knight

### - [x] Linked exile and delayed return (3 cards)

Remember exactly which cards were exiled with a source or effect and return them on the relevant leave/end-step trigger.

**Engine support:** Supported building blocks: `MoveCollectionEffect(linkToSource=true)`, `CardSource.FromLinkedExile` and delayed/leave triggers; [Colfenor's Urn](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ColfenorsUrn.kt), [Oblivion Ring](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/OblivionRing.kt) and [Galepowder Mage](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/GalepowderMage.kt). Champion needs its additional selection and sacrifice semantics.

Cards: Colfenor's Urn, Galepowder Mage, Oblivion Ring

### - [x] Reach (3 cards)

Can block creatures with flying.

**Engine support:** Supported: `Keyword.REACH`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Cloudcrown Oak](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/CloudcrownOak.kt).

Cards: Cairn Wanderer, Cloudcrown Oak, Cloudthresher

### - [x] Third ability resolution in a turn (3 cards)

Count resolutions of a particular source ability, including copies; the bonus occurs exactly on the third resolution, not on the third activation or on every later resolution.

**Engine support:** Supported: `IncrementAbilityResolutionCountEffect` + `Conditions.SourceAbilityResolvedNTimes(3)`; [Ashling the Pilgrim](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/AshlingthePilgrim.kt), [Inner-Flame Igniter](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/InnerFlameIgniter.kt) and [Soulbright Flamekin](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/SoulbrightFlamekin.kt).

Cards: Ashling the Pilgrim, Inner-Flame Igniter, Soulbright Flamekin

### - [ ] Counter-bound continuous effects (2 cards)

Keep an effect active for as long as its affected permanent has the specified counter, including after the original spell has resolved.

**Engine support:** Partial: `Duration.WhileAffectedHasCounter` exists and [Makeshift Mannequin](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/MakeshiftMannequin.kt) uses it for a granted trigger. Aquitect's Will needs the same lifetime on an additive land-type change, including normal Island mana abilities. Verify layer support instead of replacing the printed lifetime with permanent or end-of-turn duration.

Cards: Aquitect's Will, Makeshift Mannequin

### - [x] Defender (2 cards)

The creature cannot attack.

**Engine support:** Supported: `Keyword.DEFENDER`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Zephyr Net](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ZephyrNet.kt).

Cards: Sentry Oak, Zephyr Net

### - [x] Equipment (2 cards)

Equipment grants effects to its attached creature; equip is a targeted sorcery-timing activated ability that moves the attachment.

**Engine support:** Supported: Equipment/equip DSL and attachment effects; [Runed Stalactite](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/RunedStalactite.kt) and [Deathrender](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/Deathrender.kt).

Cards: Deathrender, Runed Stalactite

### - [ ] Acquire keywords from graveyards (1 card)

Continuously obtain each listed keyword found on a creature card in any graveyard, preserving specific landwalk and protection qualities.

**Engine support:** Gap: ordinary conditional grants exist, but Cairn Wanderer is unauthored and no complete donor-keyword acquisition shape was found. A boolean “has protection” loses its protected qualities; all graveyards and changing donors must be covered.

Cards: Cairn Wanderer

### - [ ] Damage history targeting (1 card)

Restrict a spell target to a player or permanent that was dealt damage during the current turn.

**Engine support:** Gap to verify: Needle Drop remains unauthored. A shared player/permanent damage-history target predicate must be checked at both target selection and resolution; marked damage alone cannot represent the player half or damage subsequently removed.

Cards: Needle Drop

### - [ ] Replace countering with exile and immediate free play (1 card)

A controller's attempted spell counter instead exiles the spell and permits playing that card without its mana cost during resolution.

**Engine support:** Gap: no matching counter-event replacement was found in `ReplacementEffect.kt`. A counterspell's exile destination and a later cast permission do not model replacement of any counter effect controlled by Guile's controller.

Cards: Guile

### - [ ] Same-name spell history free cast (1 card)

Optionally cast a card from hand during resolution only if a spell with that name was cast earlier this turn.

**Engine support:** Gap to verify: generic spell-history counts and free-cast effects exist, but no name-history predicate/application for Twinning Glass was found. Need actual cast history, including countered spells and other players' casts.

Cards: Twinning Glass

### - [x] Any player may activate (1 card)

The current priority holder may pay the activated ability's costs even if they do not control its source.

**Engine support:** `ActivationRestriction.AnyPlayerMay` supports free, mana-only, and standalone discard costs for opponent activations. Oona’s Prowler has card scenarios for the activating player’s discard, source-controller independence, repeated activations, duration, and source blink; shared engine scenarios cover filtered and random multi-card discard costs. The client uses server-provided legal activations to make opposing permanents interactive; it does not infer activation permission from card text.

Cards: Oona's Prowler

### - [x] Indestructible (1 card)

Cannot be destroyed by lethal damage or effects that destroy.

**Engine support:** Supported: `Keyword.INDESTRUCTIBLE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Timber Protector](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/TimberProtector.kt).

Cards: Timber Protector

### - [x] Menace (1 card)

Cannot be blocked except by two or more creatures.

**Engine support:** Supported: `Keyword.MENACE`, keyword grants, and the [keyword model](../../../mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt); corpus example: [Caterwauling Boggart](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/CaterwaulingBoggart.kt).

Cards: Caterwauling Boggart

### - [x] Playing linked face-down exile and skipping draws (1 card)

Maintain visibility and normal-cost play permission for a linked face-down pile while restricting the controller to one spell per turn and skipping their draw step.

**Engine support:** Supported atoms: `GrantMayCastFromLinkedExile`, `SkipDrawStep`, `RestrictSpellsCastPerTurn` and explicit `lookableInExile`; [Colfenor's Plans](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/lrw/cards/ColfenorsPlans.kt).

Cards: Colfenor's Plans

### - [x] Spell prohibition by mana value and X (1 card)

Forbid noncreature spells above the printed mana-value threshold and noncreature spells with X in their mana costs.

**Engine support:** Supported vocabulary: `PlayersCantCastSpells(spellFilter=...)` (used by [Grid Monitor](../../../mtg-sets/2003-2007/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/mrd/cards/GridMonitor.kt)), `CardPredicate.HasXInManaCost` and mana-value predicates. Gaddock Teeg remains unauthored; use both restrictions together and test alternative/free casts.

Cards: Gaddock Teeg

