package com.wingedsheep.engine.support

import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CounterSpellEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetSpell
import java.util.UUID

/**
 * Test card definitions for unit tests.
 *
 * These are simplified cards designed for testing specific mechanics.
 * They intentionally use minimal abilities to make test assertions clearer.
 *
 * Cards that exist in real sets (Portal, Onslaught, Scourge) are included
 * via [all] from those sets. Only test-only cards are defined here.
 */
object TestCards {

    // =========================================================================
    // Vanilla Creatures
    // =========================================================================

    /**
     * 3/3 for {2}{G}
     */
    val CentaurCourser = CardDefinition.creature(
        name = "Centaur Courser",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Centaur"), Subtype("Warrior")),
        power = 3,
        toughness = 3
    )

    /**
     * 5/5 for {3}{G}{G}
     */
    val ForceOfNature = CardDefinition.creature(
        name = "Force of Nature",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        subtypes = setOf(Subtype("Elemental")),
        power = 5,
        toughness = 5
    )

    /**
     * 2/1 for {R}
     */
    val GoblinGuide = CardDefinition.creature(
        name = "Goblin Guide",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Scout")),
        power = 2,
        toughness = 1
    )

    /**
     * 1/1 for {W}
     */
    val SavannahLions = CardDefinition.creature(
        name = "Savannah Lions",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Cat")),
        power = 1,
        toughness = 1
    )

    /**
     * 1/1 for {2}{B} with "When this creature dies, you gain 3 life."
     * Test card for death triggers.
     */
    val DeathTriggerTestCreature = CardDefinition.creature(
        name = "Death Trigger Test Creature",
        manaCost = ManaCost.parse("{2}{B}"),
        subtypes = setOf(Subtype("Test")),
        power = 1,
        toughness = 1,
        oracleText = "When this creature dies, you gain 3 life.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
                binding = TriggerBinding.SELF,
                effect = GainLifeEffect(3)
            )
        )
    )

    // =========================================================================
    // Creatures with Keywords
    // =========================================================================

    /**
     * 2/2 Forestwalk for {1}{G}
     * Test card for landwalk evasion.
     */
    val ForestWalker = CardDefinition.creature(
        name = "Forest Walker",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Dryad")),
        power = 2,
        toughness = 2,
        oracleText = "Forestwalk",
        keywords = setOf(Keyword.FORESTWALK)
    )

    /**
     * 2/2 Islandwalk for {1}{U}
     * Test card for landwalk evasion.
     */
    val IslandWalker = CardDefinition.creature(
        name = "Island Walker",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype("Merfolk")),
        power = 2,
        toughness = 2,
        oracleText = "Islandwalk",
        keywords = setOf(Keyword.ISLANDWALK)
    )

    /**
     * 2/2 Unblockable for {1}{U}{U}
     * Phantom Warrior can't be blocked.
     */
    val PhantomWarrior = CardDefinition.creature(
        name = "Phantom Warrior",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype("Illusion"), Subtype("Warrior")),
        power = 2,
        toughness = 2,
        oracleText = "Phantom Warrior can't be blocked."
    ).copy(flags = setOf(AbilityFlag.CANT_BE_BLOCKED))

    /**
     * 2/1 First Strike for {1}{R}
     */
    val BladeOfTheNinthWatch = CardDefinition.creature(
        name = "Blade of the Ninth Watch",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 1,
        oracleText = "First strike",
        keywords = setOf(Keyword.FIRST_STRIKE)
    )

    /**
     * 3/1 First Strike for {2}{W}
     */
    val FirstStrikeKnight = CardDefinition.creature(
        name = "First Strike Knight",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 3,
        toughness = 1,
        oracleText = "First strike",
        keywords = setOf(Keyword.FIRST_STRIKE)
    )

    /**
     * 2/2 Fear for {1}{B}{B}
     * Test card for fear evasion.
     */
    val FearCreature = CardDefinition.creature(
        name = "Fear Creature",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        subtypes = setOf(Subtype("Zombie")),
        power = 2,
        toughness = 2,
        oracleText = "Fear",
        keywords = setOf(Keyword.FEAR)
    )

    /**
     * 2/2 Artifact Creature for {2}
     * Colorless artifact creature for testing fear blocking.
     */
    val ArtifactCreature = CardDefinition(
        name = "Artifact Creature",
        manaCost = ManaCost.parse("{2}"),
        typeLine = TypeLine.artifactCreature(setOf(Subtype("Golem"))),
        creatureStats = CreatureStats(2, 2)
    )

    /**
     * 2/2 Black Creature for {1}{B}
     * For testing fear blocking.
     */
    val BlackCreature = CardDefinition.creature(
        name = "Black Creature",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Zombie")),
        power = 2,
        toughness = 2
    )

    // =========================================================================
    // Instants
    // =========================================================================

    /**
     * {R} - Deal 3 damage to any target.
     */
    val LightningBolt = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Lightning Bolt deals 3 damage to any target.",
        script = CardScript.spell(
            effect = DealDamageEffect(3, EffectTarget.BoundVariable("target")),
            AnyTarget(id = "target")
        )
    )

    /**
     * {G} - +3/+3 until end of turn.
     */
    val GiantGrowth = CardDefinition.instant(
        name = "Giant Growth",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Target creature gets +3/+3 until end of turn.",
        script = CardScript.spell(
            effect = ModifyStatsEffect(3, 3, EffectTarget.BoundVariable("target"), Duration.EndOfTurn),
            TargetCreature(id = "target")
        )
    )

    /**
     * {U}{U} - Counter target spell.
     */
    val Counterspell = CardDefinition.instant(
        name = "Counterspell",
        manaCost = ManaCost.parse("{U}{U}"),
        oracleText = "Counter target spell.",
        script = CardScript.spell(
            effect = CounterSpellEffect,
            TargetSpell()
        )
    )

    /**
     * {U} - Counter target spell with mana value 2 or less.
     */
    val SpellPierce = CardDefinition.instant(
        name = "Spell Pierce",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "Counter target noncreature spell unless its controller pays {2}.",
        script = CardScript.spell(
            effect = CounterSpellEffect,  // Simplified - no tax mechanic for now
            TargetSpell(filter = TargetFilter.NoncreatureSpellOnStack)
        )
    )

    // =========================================================================
    // Sorceries
    // =========================================================================

    /**
     * {1}{B} - Destroy target nonblack creature.
     */
    val DoomBlade = CardDefinition.sorcery(
        name = "Doom Blade",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Destroy target nonblack creature.",
        script = CardScript.spell(
            effect = MoveToZoneEffect(EffectTarget.BoundVariable("target"), Zone.GRAVEYARD, byDestruction = true),
            TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK), id = "target")
        )
    )

    /**
     * {B} - Draw a card, then discard a card.
     */
    val CarefulStudy = CardDefinition.sorcery(
        name = "Careful Study",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Draw a card, then discard a card.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    DrawCardsEffect(1, EffectTarget.Controller),
                    Effects.Discard(1, EffectTarget.Controller)
                )
            )
        )
    )

    // =========================================================================
    // Mana Dorks (Creatures with Tap: Add mana abilities)
    // =========================================================================

    /**
     * 1/1 for {G} with "{T}: Add {G}"
     * The classic mana dork.
     */
    val LlanowarElves = CardDefinition(
        name = "Llanowar Elves",
        manaCost = ManaCost.parse("{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Elf"), Subtype("Druid"))),
        oracleText = "{T}: Add {G}.",
        creatureStats = CreatureStats(1, 1),
        script = CardScript.permanent(
            ActivatedAbility(
                id = AbilityId(UUID.randomUUID().toString()),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.GREEN),
                isManaAbility = true
            )
        )
    )

    /**
     * 0/2 for {2} with "{T}: Add {C}{C}"
     * A colorless mana rock creature.
     */
    val PalladiumMyr = CardDefinition(
        name = "Palladium Myr",
        manaCost = ManaCost.parse("{3}"),
        typeLine = TypeLine.artifactCreature(setOf(Subtype("Myr"))),
        oracleText = "{T}: Add {C}{C}.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.permanent(
            ActivatedAbility(
                id = AbilityId(UUID.randomUUID().toString()),
                cost = AbilityCost.Tap,
                effect = AddColorlessManaEffect(2),
                isManaAbility = true
            )
        )
    )

    /**
     * 1/1 for {G} with "{T}: Add one mana of any color"
     */
    val BirdsOfParadise = CardDefinition(
        name = "Birds of Paradise",
        manaCost = ManaCost.parse("{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Bird"))),
        oracleText = "Flying\n{T}: Add one mana of any color.",
        keywords = setOf(Keyword.FLYING),
        creatureStats = CreatureStats(0, 1),
        script = CardScript.permanent(
            ActivatedAbility(
                id = AbilityId(UUID.randomUUID().toString()),
                cost = AbilityCost.Tap,
                effect = AddAnyColorManaEffect(1),
                isManaAbility = true
            )
        )
    )

    /**
     * 1/1 Haste for {R} with "{T}: Add {R}"
     * A hasty mana dork that can tap immediately.
     */
    val RagavanNimblePilferer = CardDefinition(
        name = "Ragavan, Nimble Pilferer",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine(
            supertypes = setOf(Supertype.LEGENDARY),
            cardTypes = setOf(CardType.CREATURE),
            subtypes = setOf(Subtype("Monkey"), Subtype("Pirate"))
        ),
        oracleText = "Haste\n{T}: Add {R}.",
        keywords = setOf(Keyword.HASTE),
        creatureStats = CreatureStats(2, 1),
        script = CardScript.permanent(
            ActivatedAbility(
                id = AbilityId(UUID.randomUUID().toString()),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.RED),
                isManaAbility = true
            )
        )
    )

    // =========================================================================
    // Cost Reduction Cards
    // =========================================================================

    /**
     * 12/12 Trample for {10}{G}{G}
     * "This spell costs {X} less to cast, where X is the total power of creatures you control."
     */
    val GhaltaPrimalHunger = CardDefinition(
        name = "Ghalta, Primal Hunger",
        manaCost = ManaCost.parse("{10}{G}{G}"),
        typeLine = TypeLine(
            supertypes = setOf(Supertype.LEGENDARY),
            cardTypes = setOf(CardType.CREATURE),
            subtypes = setOf(Subtype("Elder"), Subtype("Dinosaur"))
        ),
        oracleText = "This spell costs {X} less to cast, where X is the total power of creatures you control.\nTrample",
        keywords = setOf(Keyword.TRAMPLE),
        creatureStats = CreatureStats(12, 12),
        script = CardScript(
            staticAbilities = listOf(
                SpellCostReduction(CostReductionSource.TotalPowerYouControl)
            )
        )
    )

    /**
     * 4/4 for {6} with Affinity for artifacts.
     * Costs {1} less for each artifact you control.
     */
    val FrogmiteTestCard = CardDefinition(
        name = "Frogmite",
        manaCost = ManaCost.parse("{4}"),
        typeLine = TypeLine.artifactCreature(setOf(Subtype("Frog"))),
        oracleText = "Affinity for artifacts",
        creatureStats = CreatureStats(2, 2),
        keywordAbilities = listOf(KeywordAbility.Affinity(CardType.ARTIFACT))
    )

    // =========================================================================
    // Morph Creatures
    // =========================================================================

    /**
     * 2/3 for {2}{W} with Morph {1}{W}.
     * A basic morph creature for testing the mechanic.
     */
    val MorphTestCreature = CardDefinition(
        name = "Morph Test Creature",
        manaCost = ManaCost.parse("{2}{W}"),
        typeLine = TypeLine.creature(setOf(Subtype("Test"))),
        oracleText = "Morph {1}{W}",
        creatureStats = CreatureStats(2, 3),
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{1}{W}")))
    )

    /**
     * 4/4 for {3}{G}{G} with Morph {4}{G}{G}.
     * When this creature is turned face up, draw a card.
     */
    val MorphWithTriggerTestCreature = CardDefinition(
        name = "Morph Trigger Test Creature",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Beast"))),
        oracleText = "Morph {4}{G}{G}\nWhen this creature is turned face up, draw a card.",
        creatureStats = CreatureStats(4, 4),
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{4}{G}{G}"))),
        script = CardScript(
            triggeredAbilities = listOf(
                TriggeredAbility(
                    id = AbilityId("morph-trigger-draw"),
                    trigger = GameEvent.TurnFaceUpEvent,
                    binding = TriggerBinding.SELF,
                    effect = DrawCardsEffect(1)
                )
            )
        )
    )

    // =========================================================================
    // Alternative Payment Cards (Delve/Convoke)
    // =========================================================================

    /**
     * 4/4 for {7}{B} with Delve.
     * Can exile cards from graveyard to pay generic costs.
     */
    val GurmagAngler = CardDefinition(
        name = "Gurmag Angler",
        manaCost = ManaCost.parse("{6}{B}"),
        typeLine = TypeLine.creature(setOf(Subtype("Zombie"), Subtype("Fish"))),
        oracleText = "Delve",
        keywords = setOf(Keyword.DELVE),
        creatureStats = CreatureStats(5, 5)
    )

    /**
     * 8/8 for {8} with Convoke.
     * Can tap creatures to pay mana costs.
     */
    val StokeBrillianceToken = CardDefinition(
        name = "Stoke the Flames",
        manaCost = ManaCost.parse("{2}{R}{R}"),
        typeLine = TypeLine.instant(),
        oracleText = "Convoke\nStoke the Flames deals 4 damage to any target.",
        keywords = setOf(Keyword.CONVOKE),
        script = CardScript.spell(
            effect = DealDamageEffect(4, EffectTarget.BoundVariable("target")),
            AnyTarget(id = "target")
        )
    )

    // =========================================================================
    // Trample & Deathtouch Test Creatures
    // =========================================================================

    /**
     * 5/5 Trample for {3}{G}{G}
     * Test card for trample damage assignment.
     */
    val TrampleBeast = CardDefinition.creature(
        name = "Trample Beast",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5,
        toughness = 5,
        oracleText = "Trample",
        keywords = setOf(Keyword.TRAMPLE)
    )

    /**
     * 3/3 Trample Deathtouch for {1}{B}{G}
     * Test card for deathtouch+trample interaction.
     */
    val DeathtouchTrampler = CardDefinition.creature(
        name = "Deathtouch Trampler",
        manaCost = ManaCost.parse("{1}{B}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 3,
        toughness = 3,
        oracleText = "Deathtouch, trample",
        keywords = setOf(Keyword.DEATHTOUCH, Keyword.TRAMPLE)
    )

    /**
     * 2/2 Cleric for {1}{W}
     * Vanilla Cleric creature for testing Daunting Defender interaction.
     */
    val TestCleric = CardDefinition.creature(
        name = "Test Cleric",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 2,
        toughness = 2
    )

    // =========================================================================
    // Enchantments
    // =========================================================================

    /**
     * Simple non-aura enchantment for testing enchantment targeting/removal.
     */
    val TestEnchantment = CardDefinition.enchantment(
        name = "Test Enchantment",
        manaCost = ManaCost.parse("{1}{W}"),
        oracleText = "Test enchantment with no abilities."
    )

    // =========================================================================
    // All Test Cards
    // =========================================================================

    private val testOnlyCards: List<CardDefinition> = listOf(
        // Creatures
        CentaurCourser,
        ForceOfNature,
        GoblinGuide,
        SavannahLions,
        DeathTriggerTestCreature,
        ForestWalker,
        IslandWalker,
        PhantomWarrior,
        BladeOfTheNinthWatch,
        FirstStrikeKnight,
        FearCreature,
        ArtifactCreature,
        BlackCreature,
        TrampleBeast,
        DeathtouchTrampler,
        TestCleric,
        // Mana Dorks
        LlanowarElves,
        PalladiumMyr,
        BirdsOfParadise,
        RagavanNimblePilferer,
        // Morph Creatures
        MorphTestCreature,
        MorphWithTriggerTestCreature,
        // Cost Reduction Cards
        GhaltaPrimalHunger,
        FrogmiteTestCard,
        // Alternative Payment Cards
        GurmagAngler,
        StokeBrillianceToken,
        // Enchantments
        TestEnchantment,
        // Instants
        LightningBolt,
        GiantGrowth,
        Counterspell,
        SpellPierce,
        // Sorceries
        DoomBlade,
        CarefulStudy,
    )

    val all: List<CardDefinition> =
        PortalSet.allCards + OnslaughtSet.allCards + ScourgeSet.allCards + testOnlyCards
}
