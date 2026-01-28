package com.wingedsheep.engine.support

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.CantBeBlockedByPower
import com.wingedsheep.sdk.targeting.AnyTarget
import com.wingedsheep.sdk.targeting.TargetCreature
import com.wingedsheep.sdk.targeting.TargetSpell
import com.wingedsheep.sdk.targeting.SpellTargetFilter
import com.wingedsheep.sdk.targeting.CreatureTargetFilter as TargetingCreatureFilter
import java.util.UUID

/**
 * Test card definitions for unit tests.
 *
 * These are simplified cards designed for testing specific mechanics.
 * They intentionally use minimal abilities to make test assertions clearer.
 */
object TestCards {

    // =========================================================================
    // Basic Lands
    // =========================================================================

    val Forest = CardDefinition.basicLand("Forest", Subtype("Forest"))
    val Island = CardDefinition.basicLand("Island", Subtype("Island"))
    val Mountain = CardDefinition.basicLand("Mountain", Subtype("Mountain"))
    val Plains = CardDefinition.basicLand("Plains", Subtype("Plains"))
    val Swamp = CardDefinition.basicLand("Swamp", Subtype("Swamp"))

    // =========================================================================
    // Vanilla Creatures
    // =========================================================================

    /**
     * 2/2 for {1}{G}
     */
    val GrizzlyBears = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

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
                trigger = OnDeath(selfOnly = true),
                effect = GainLifeEffect(3)
            )
        )
    )

    /**
     * 2/2 for {2}{W} with "When Venerable Monk enters the battlefield, you gain 2 life."
     */
    val VenerableMonk = CardDefinition.creature(
        name = "Venerable Monk",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Monk"), Subtype("Cleric")),
        power = 2,
        toughness = 2,
        oracleText = "When Venerable Monk enters the battlefield, you gain 2 life.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = GainLifeEffect(2)
            )
        )
    )

    /**
     * 2/2 for {3}{B}{B} with "When Serpent Assassin enters the battlefield, you may destroy target nonblack creature."
     */
    val SerpentAssassin = CardDefinition.creature(
        name = "Serpent Assassin",
        manaCost = ManaCost.parse("{3}{B}{B}"),
        subtypes = setOf(Subtype("Snake"), Subtype("Assassin")),
        power = 2,
        toughness = 2,
        oracleText = "When Serpent Assassin enters the battlefield, you may destroy target nonblack creature.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = DestroyEffect(EffectTarget.ContextTarget(0)),
                optional = true,
                targetRequirement = TargetCreature(filter = TargetingCreatureFilter.NotColor(Color.BLACK))
            )
        )
    )

    // =========================================================================
    // Creatures with Keywords
    // =========================================================================

    /**
     * 2/2 Flying for {1}{U}{U}
     */
    val WindDrake = CardDefinition.creature(
        name = "Wind Drake",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype("Drake")),
        power = 2,
        toughness = 2,
        oracleText = "Flying",
        keywords = setOf(Keyword.FLYING)
    )

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
     * 1/1 for {1}{W} - Can't be blocked by creatures with power 2 or greater.
     * Test card for power-based blocking restriction (like Fleet-Footed Monk).
     */
    val FleetFootedMonk = CardDefinition(
        name = "Fleet-Footed Monk",
        manaCost = ManaCost.parse("{1}{W}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Monk"))),
        oracleText = "Fleet-Footed Monk can't be blocked by creatures with power 2 or greater.",
        creatureStats = CreatureStats(1, 1),
        script = CardScript(
            staticAbilities = listOf(
                CantBeBlockedByPower(minPower = 2)
            )
        )
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
        oracleText = "Phantom Warrior can't be blocked.",
        keywords = setOf(Keyword.UNBLOCKABLE)
    )

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
            effect = DealDamageEffect(3, EffectTarget.AnyTarget),
            AnyTarget()
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
            effect = ModifyStatsEffect(3, 3, EffectTarget.TargetCreature, Duration.EndOfTurn),
            TargetCreature()
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
            TargetSpell(filter = SpellTargetFilter.Noncreature)
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
            effect = DestroyEffect(EffectTarget.TargetNonblackCreature),
            TargetCreature(filter = TargetingCreatureFilter.NotColor(Color.BLACK))
        )
    )

    /**
     * {1}{B}{B} - Target player discards two cards.
     */
    val MindRot = CardDefinition.sorcery(
        name = "Mind Rot",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        oracleText = "Target player discards two cards.",
        script = CardScript.spell(
            effect = DiscardCardsEffect(2, EffectTarget.Opponent)
        )
    )

    /**
     * {1}{B}{B} - Destroy two target nonblack creatures. You lose 5 life.
     * Test card for multi-target spells.
     */
    val WickedPact = CardDefinition.sorcery(
        name = "Wicked Pact",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        oracleText = "Destroy two target nonblack creatures. You lose 5 life.",
        script = CardScript.spell(
            effect = CompositeEffect(listOf(
                DestroyEffect(EffectTarget.ContextTarget(0)),
                DestroyEffect(EffectTarget.ContextTarget(1)),
                LoseLifeEffect(5, EffectTarget.Controller)
            )),
            TargetCreature(filter = TargetingCreatureFilter.NotColor(Color.BLACK)),
            TargetCreature(filter = TargetingCreatureFilter.NotColor(Color.BLACK))
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
                    DiscardCardsEffect(1, EffectTarget.Controller)
                )
            )
        )
    )

    /**
     * {2}{U} - Each player discards any number of cards, then draws that many cards.
     * Draw a card.
     */
    val Flux = CardDefinition.sorcery(
        name = "Flux",
        manaCost = ManaCost.parse("{2}{U}"),
        oracleText = "Each player discards any number of cards, then draws that many cards. Draw a card.",
        script = CardScript.spell(
            effect = EachPlayerDiscardsDrawsEffect(controllerBonusDraw = 1)
        )
    )

    /**
     * {1}{G} - You may play up to three additional lands this turn.
     */
    val SummerBloom = CardDefinition.sorcery(
        name = "Summer Bloom",
        manaCost = ManaCost.parse("{1}{G}"),
        oracleText = "You may play up to three additional lands this turn.",
        script = CardScript.spell(
            effect = PlayAdditionalLandsEffect(count = 3)
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
            effect = DealDamageEffect(4, EffectTarget.AnyTarget),
            AnyTarget()
        )
    )

    // =========================================================================
    // All Test Cards
    // =========================================================================

    val all: List<CardDefinition> = listOf(
        // Lands
        Forest,
        Island,
        Mountain,
        Plains,
        Swamp,
        // Creatures
        GrizzlyBears,
        CentaurCourser,
        ForceOfNature,
        GoblinGuide,
        SavannahLions,
        DeathTriggerTestCreature,
        VenerableMonk,
        SerpentAssassin,
        WindDrake,
        ForestWalker,
        IslandWalker,
        FleetFootedMonk,
        PhantomWarrior,
        BladeOfTheNinthWatch,
        // Mana Dorks
        LlanowarElves,
        PalladiumMyr,
        BirdsOfParadise,
        RagavanNimblePilferer,
        // Cost Reduction Cards
        GhaltaPrimalHunger,
        FrogmiteTestCard,
        // Alternative Payment Cards
        GurmagAngler,
        StokeBrillianceToken,
        // Instants
        LightningBolt,
        GiantGrowth,
        Counterspell,
        SpellPierce,
        // Sorceries
        DoomBlade,
        MindRot,
        CarefulStudy,
        Flux,
        SummerBloom,
        WickedPact
    )
}
