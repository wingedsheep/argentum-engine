package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.sets.BaseCardRegistry

/**
 * The Portal set - a simplified introductory set for Magic: The Gathering.
 * Portal was released in 1997 and contains 222 cards.
 */
object PortalSet : BaseCardRegistry() {
    override val setCode: String = "POR"
    override val setName: String = "Portal"

    init {
        registerAllCards()
    }

    private fun registerAllCards() {
        // Register basic lands first
        registerBasicLands()

        // Register all Portal cards
        registerAlabasterDragon()
        registerAlluringScent()
        registerAnaconda()
        registerAncestralMemories()
        registerAngelicBlessing()
        registerArchangel()
        registerArdentMilitia()
        registerArmageddon()
        registerArmoredPegasus()
        registerArrogantVampire()
        registerJungleLion()
    }

    // =========================================================================
    // Basic Lands
    // =========================================================================

    /**
     * Register all five basic lands with their mana abilities.
     */
    private fun registerBasicLands() {
        registerForest()
        registerIsland()
        registerMountain()
        registerPlains()
        registerSwamp()
    }

    private fun registerForest() {
        val definition = CardDefinition.basicLand("Forest", Subtype.FOREST)
        val script = cardScript("Forest") {
            manaAbility(AddManaEffect(Color.GREEN))
        }
        register(definition, script)
    }

    private fun registerIsland() {
        val definition = CardDefinition.basicLand("Island", Subtype.ISLAND)
        val script = cardScript("Island") {
            manaAbility(AddManaEffect(Color.BLUE))
        }
        register(definition, script)
    }

    private fun registerMountain() {
        val definition = CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN)
        val script = cardScript("Mountain") {
            manaAbility(AddManaEffect(Color.RED))
        }
        register(definition, script)
    }

    private fun registerPlains() {
        val definition = CardDefinition.basicLand("Plains", Subtype.PLAINS)
        val script = cardScript("Plains") {
            manaAbility(AddManaEffect(Color.WHITE))
        }
        register(definition, script)
    }

    private fun registerSwamp() {
        val definition = CardDefinition.basicLand("Swamp", Subtype.SWAMP)
        val script = cardScript("Swamp") {
            manaAbility(AddManaEffect(Color.BLACK))
        }
        register(definition, script)
    }

    // =========================================================================
    // Card Implementations
    // =========================================================================

    /**
     * Alabaster Dragon - 4WW
     * Creature — Dragon
     * 4/4
     * Flying
     * When Alabaster Dragon dies, shuffle it into its owner's library.
     */
    private fun registerAlabasterDragon() {
        val definition = CardDefinition.creature(
            name = "Alabaster Dragon",
            manaCost = ManaCost.parse("{4}{W}{W}"),
            subtypes = setOf(Subtype.DRAGON),
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying\nWhen Alabaster Dragon dies, shuffle it into its owner's library."
        )

        val script = cardScript("Alabaster Dragon") {
            keywords(Keyword.FLYING)
            triggered(
                trigger = OnDeath(),
                effect = ShuffleIntoLibraryEffect(EffectTarget.Self)
            )
        }

        register(definition, script)
    }

    /**
     * Alluring Scent - 1GG
     * Sorcery
     * All creatures able to block target creature this turn do so.
     */
    private fun registerAlluringScent() {
        val definition = CardDefinition.sorcery(
            name = "Alluring Scent",
            manaCost = ManaCost.parse("{1}{G}{G}"),
            oracleText = "All creatures able to block target creature this turn do so."
        )

        val script = cardScript("Alluring Scent") {
            spell(MustBeBlockedEffect(EffectTarget.TargetCreature))
        }

        register(definition, script)
    }

    /**
     * Anaconda - 3G
     * Creature — Snake
     * 3/3
     * Swampwalk
     */
    private fun registerAnaconda() {
        val definition = CardDefinition.creature(
            name = "Anaconda",
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = setOf(Subtype.SERPENT),
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.SWAMPWALK),
            oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)"
        )

        val script = cardScript("Anaconda") {
            keywords(Keyword.SWAMPWALK)
        }

        register(definition, script)
    }

    /**
     * Ancestral Memories - 2UUU
     * Sorcery
     * Look at the top seven cards of your library. Put two of them into your hand and the rest into your graveyard.
     */
    private fun registerAncestralMemories() {
        val definition = CardDefinition.sorcery(
            name = "Ancestral Memories",
            manaCost = ManaCost.parse("{2}{U}{U}{U}"),
            oracleText = "Look at the top seven cards of your library. Put two of them into your hand and the rest into your graveyard."
        )

        val script = cardScript("Ancestral Memories") {
            spell(LookAtTopCardsEffect(7, 2))
        }

        register(definition, script)
    }

    /**
     * Angelic Blessing - 2W
     * Sorcery
     * Target creature gets +3/+3 and gains flying until end of turn.
     */
    private fun registerAngelicBlessing() {
        val definition = CardDefinition.sorcery(
            name = "Angelic Blessing",
            manaCost = ManaCost.parse("{2}{W}"),
            oracleText = "Target creature gets +3/+3 and gains flying until end of turn."
        )

        val script = cardScript("Angelic Blessing") {
            spell(
                CompositeEffect(listOf(
                    ModifyStatsEffect(3, 3, EffectTarget.TargetCreature, untilEndOfTurn = true),
                    GrantKeywordUntilEndOfTurnEffect(Keyword.FLYING, EffectTarget.TargetCreature)
                ))
            )
        }

        register(definition, script)
    }

    /**
     * Archangel - 5WW
     * Creature — Angel
     * 5/5
     * Flying, vigilance
     */
    private fun registerArchangel() {
        val definition = CardDefinition.creature(
            name = "Archangel",
            manaCost = ManaCost.parse("{5}{W}{W}"),
            subtypes = setOf(Subtype.ANGEL),
            power = 5,
            toughness = 5,
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            oracleText = "Flying, vigilance"
        )

        registerFrenchVanilla(definition)
    }

    /**
     * Ardent Militia - 4W
     * Creature — Human Soldier
     * 2/5
     * Vigilance
     */
    private fun registerArdentMilitia() {
        val definition = CardDefinition.creature(
            name = "Ardent Militia",
            manaCost = ManaCost.parse("{4}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
            power = 2,
            toughness = 5,
            keywords = setOf(Keyword.VIGILANCE),
            oracleText = "Vigilance"
        )

        registerFrenchVanilla(definition)
    }

    /**
     * Armageddon - 3W
     * Sorcery
     * Destroy all lands.
     */
    private fun registerArmageddon() {
        val definition = CardDefinition.sorcery(
            name = "Armageddon",
            manaCost = ManaCost.parse("{3}{W}"),
            oracleText = "Destroy all lands."
        )

        val script = cardScript("Armageddon") {
            spell(DestroyAllLandsEffect)
        }

        register(definition, script)
    }

    /**
     * Armored Pegasus - 1W
     * Creature — Pegasus
     * 1/2
     * Flying
     */
    private fun registerArmoredPegasus() {
        val definition = CardDefinition.creature(
            name = "Armored Pegasus",
            manaCost = ManaCost.parse("{1}{W}"),
            subtypes = setOf(Subtype.of("Pegasus")),
            power = 1,
            toughness = 2,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )

        registerFrenchVanilla(definition)
    }

    /**
     * Arrogant Vampire - 3BB
     * Creature — Vampire
     * 4/3
     * Flying
     */
    private fun registerArrogantVampire() {
        val definition = CardDefinition.creature(
            name = "Arrogant Vampire",
            manaCost = ManaCost.parse("{3}{B}{B}"),
            subtypes = setOf(Subtype.of("Vampire")),
            power = 4,
            toughness = 3,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )

        registerFrenchVanilla(definition)
    }

    /**
     * Jungle Lion - G
     * Creature — Cat
     * 2/1
     * This creature can't block.
     */
    private fun registerJungleLion() {
        val definition = CardDefinition.creature(
            name = "Jungle Lion",
            manaCost = ManaCost.parse("{G}"),
            subtypes = setOf(Subtype.CAT),
            power = 2,
            toughness = 1,
            oracleText = "This creature can't block."
        )

        val script = cardScript("Jungle Lion") {
            // We register a static ability targeting the source creature itself
            staticAbility(CantBlock(StaticTarget.SourceCreature))
        }

        register(definition, script)
    }
}
