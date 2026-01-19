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
        registerNaturesLore()
        registerNaturalOrder()
        registerGiftOfEstates()
        registerPrimevalForce()

        // Phase 5: ETB and Death Trigger cards
        registerVenerableMonk()
        registerSpiritualGuardian()
        registerDreadReaper()
        registerSerpentWarrior()
        registerFireImp()
        registerManOWar()
        registerOwlFamiliar()
        registerEbonDragon()
        registerGravedigger()
        registerEndlessCockroaches()
        registerUndyingBeast()
        registerNoxiousToad()
        registerFireSnake()
        registerChargingBandits()
        registerChargingPaladin()
        registerSeasonedMarshal()
        registerSerpentAssassin()

        // Phase 6: Damage and Drain Spells
        registerScorchingSpear()
        registerVolcanicHammer()
        registerLavaAxe()
        registerBeeString()
        registerPyroclasm()
        registerDrySpell()
        registerFireTempest()
        registerNeedleStorm()
        registerVampiricFeast()
        registerVampiricTouch()
        registerSoulShred()
        registerSacredNectar()
        registerNaturalSpring()

        // Phase 7: Vanilla Creatures
        registerBorderGuard()
        registerCoralEel()
        registerDevotedHero()
        registerFootSoldiers()
        registerGiantOctopus()
        registerGoblinBully()
        registerGorillaWarrior()
        registerGrizzlyBears()
        registerHighlandGiant()
        registerHillGiant()
        registerHornedTurtle()
        registerKnightErrant()
        registerLizardWarrior()
        registerMerfolkOfThePearlTrident()
        registerMinotaurWarrior()
        registerMuckRats()
        registerPantherWarriors()
        registerPython()
        registerRedwoodTreefolk()
        registerRegalUnicorn()
        registerRowanTreefolk()
        registerSkeletalCrocodile()
        registerSkeletalSnake()
        registerSpinedWurm()
        registerWhiptailWurm()

        // Phase 7: French Vanilla Creatures
        registerDesertDrake()
        registerDjinnOfTheLamp()
        registerElvishRanger()
        registerFeralShadow()
        registerMoonSprite()
        registerPhantomWarrior()
        registerRagingCougar()
        registerRagingMinotaur()
        registerSpottedGriffin()
        registerStarlitAngel()
        registerVolcanicDragon()
        registerWallOfGranite()
        registerWallOfSwords()
        registerWindDrake()

        // Phase 7: Can't Block Creatures
        registerCravenGiant()
        registerCravenKnight()
        registerHulkingCyclops()
        registerHulkingGoblin()

        // Phase 7: Pump Spells
        registerMonstrousGrowth()
        registerHowlingFury()

        // Phase 7: Destruction Spells
        registerHandOfDeath()
        registerVengeance()
        registerPathOfPeace()

        // Phase 8: Aura Enchantments
        registerBurningCloak()
        registerCloakOfFeathers()
        registerNaturesCloak()
        registerDefiantStand()

        // Phase 8: Land Destruction
        registerStoneRain()
        registerFlashfires()
        registerBoilingSeas()

        // Phase 8: Mass Removal
        registerWrathOfGod()

        // Phase 8: Graveyard Recursion
        registerRaiseDead()
        registerBreathOfLife()

        // Phase 8: Tutors
        registerPersonalTutor()
        registerSylvanTutor()

        // Phase 8: Draw/Discard Spells
        registerMindRot()
        registerProsperity()
        registerWindsOfChange()
        registerTouchOfBrilliance()

        // Phase 8: Bounce Spells
        registerSymbolOfUnsummoning()

        // Phase 8: More Vanilla Creatures
        registerRagingGoblin()
        registerGiantSpider()
        registerBogImp()
        registerCloudSpirit()
        registerEliteCatWarrior()
        registerMountainGoat()
        registerSacredKnight()
        registerStalkingTiger()
        registerThunderingWurm()
        registerWoodElves()
        registerChargingRhino()
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

    /**
     * Nature's Lore - 1G
     * Sorcery
     * Search your library for a Forest card and put that card onto the battlefield.
     * Then shuffle your library.
     */
    private fun registerNaturesLore() {
        val definition = CardDefinition.sorcery(
            name = "Nature's Lore",
            manaCost = ManaCost.parse("{1}{G}"),
            oracleText = "Search your library for a Forest card and put that card onto the battlefield. Then shuffle your library."
        )

        val script = cardScript("Nature's Lore") {
            spell(
                SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = false,
                    shuffleAfter = true
                )
            )
        }

        register(definition, script)
    }

    /**
     * Natural Order - 2GG
     * Sorcery
     * As an additional cost to cast this spell, sacrifice a green creature.
     * Search your library for a green creature card and put it onto the battlefield.
     * Then shuffle your library.
     */
    private fun registerNaturalOrder() {
        val definition = CardDefinition.sorcery(
            name = "Natural Order",
            manaCost = ManaCost.parse("{2}{G}{G}"),
            oracleText = "As an additional cost to cast this spell, sacrifice a green creature.\n" +
                    "Search your library for a green creature card and put it onto the battlefield. " +
                    "Then shuffle your library."
        )

        // Filter for green creatures
        val greenCreatureFilter = CardFilter.And(listOf(
            CardFilter.CreatureCard,
            CardFilter.HasColor(Color.GREEN)
        ))

        val script = cardScript("Natural Order") {
            // Additional cost: sacrifice a green creature
            sacrificeCost(greenCreatureFilter)

            // Effect: search for a green creature and put it onto battlefield
            spell(
                SearchLibraryEffect(
                    filter = greenCreatureFilter,
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = false,
                    shuffleAfter = true
                )
            )
        }

        register(definition, script)
    }

    /**
     * Gift of Estates - 1W
     * Sorcery
     * If an opponent controls more lands than you, search your library for up to
     * three Plains cards, reveal them, put them into your hand, then shuffle.
     */
    private fun registerGiftOfEstates() {
        val definition = CardDefinition.sorcery(
            name = "Gift of Estates",
            manaCost = ManaCost.parse("{1}{W}"),
            oracleText = "If an opponent controls more lands than you, search your library for up to " +
                    "three Plains cards, reveal them, put them into your hand, then shuffle."
        )

        val script = cardScript("Gift of Estates") {
            spell(
                ConditionalEffect(
                    condition = OpponentControlsMoreLands,
                    effect = SearchLibraryEffect(
                        filter = CardFilter.HasSubtype("Plains"),
                        count = 3,
                        destination = SearchDestination.HAND,
                        entersTapped = false,
                        shuffleAfter = true,
                        reveal = true
                    )
                )
            )
        }

        register(definition, script)
    }

    /**
     * Primeval Force - 2GGG
     * Creature — Elemental
     * 8/8
     * When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests.
     */
    private fun registerPrimevalForce() {
        val definition = CardDefinition.creature(
            name = "Primeval Force",
            manaCost = ManaCost.parse("{2}{G}{G}{G}"),
            subtypes = setOf(Subtype.ELEMENTAL),
            power = 8,
            toughness = 8,
            oracleText = "When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests."
        )

        // Note: The full ETB trigger with sacrifice choice requires the trigger system
        // and player decision system. For now, we register the card with a placeholder.
        // The triggered ability would be: OnEntersBattlefield -> SacrificeUnless(SacrificeForests(3))
        val script = cardScript("Primeval Force") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = SacrificeUnlessEffect(
                    permanentToSacrifice = EffectTarget.Self,
                    cost = SacrificeCost(
                        filter = CardFilter.HasSubtype("Forest"),
                        count = 3
                    )
                )
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: ETB Life Gain/Loss Cards
    // =========================================================================

    /**
     * Venerable Monk - 2W
     * Creature — Human Monk Cleric
     * 2/2
     * When Venerable Monk enters the battlefield, you gain 2 life.
     */
    private fun registerVenerableMonk() {
        val definition = CardDefinition.creature(
            name = "Venerable Monk",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.MONK, Subtype.CLERIC),
            power = 2,
            toughness = 2,
            oracleText = "When Venerable Monk enters the battlefield, you gain 2 life."
        )

        val script = cardScript("Venerable Monk") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = GainLifeEffect(2, EffectTarget.Controller)
            )
        }

        register(definition, script)
    }

    /**
     * Spiritual Guardian - 3WW
     * Creature — Spirit
     * 3/4
     * When Spiritual Guardian enters the battlefield, you gain 4 life.
     */
    private fun registerSpiritualGuardian() {
        val definition = CardDefinition.creature(
            name = "Spiritual Guardian",
            manaCost = ManaCost.parse("{3}{W}{W}"),
            subtypes = setOf(Subtype.SPIRIT),
            power = 3,
            toughness = 4,
            oracleText = "When Spiritual Guardian enters the battlefield, you gain 4 life."
        )

        val script = cardScript("Spiritual Guardian") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = GainLifeEffect(4, EffectTarget.Controller)
            )
        }

        register(definition, script)
    }

    /**
     * Dread Reaper - 3BBB
     * Creature — Horror
     * 6/5
     * Flying
     * When Dread Reaper enters the battlefield, you lose 5 life.
     */
    private fun registerDreadReaper() {
        val definition = CardDefinition.creature(
            name = "Dread Reaper",
            manaCost = ManaCost.parse("{3}{B}{B}{B}"),
            subtypes = setOf(Subtype.HORROR),
            power = 6,
            toughness = 5,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying\nWhen Dread Reaper enters the battlefield, you lose 5 life."
        )

        val script = cardScript("Dread Reaper") {
            keywords(Keyword.FLYING)
            triggered(
                trigger = OnEnterBattlefield(),
                effect = LoseLifeEffect(5, EffectTarget.Controller)
            )
        }

        register(definition, script)
    }

    /**
     * Serpent Warrior - 2B
     * Creature — Snake Warrior
     * 3/3
     * When Serpent Warrior enters the battlefield, you lose 3 life.
     */
    private fun registerSerpentWarrior() {
        val definition = CardDefinition.creature(
            name = "Serpent Warrior",
            manaCost = ManaCost.parse("{2}{B}"),
            subtypes = setOf(Subtype.SERPENT, Subtype.WARRIOR),
            power = 3,
            toughness = 3,
            oracleText = "When Serpent Warrior enters the battlefield, you lose 3 life."
        )

        val script = cardScript("Serpent Warrior") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = LoseLifeEffect(3, EffectTarget.Controller)
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: ETB Damage Cards
    // =========================================================================

    /**
     * Fire Imp - 2R
     * Creature — Imp
     * 2/1
     * When Fire Imp enters the battlefield, it deals 2 damage to target creature.
     */
    private fun registerFireImp() {
        val definition = CardDefinition.creature(
            name = "Fire Imp",
            manaCost = ManaCost.parse("{2}{R}"),
            subtypes = setOf(Subtype.IMP),
            power = 2,
            toughness = 1,
            oracleText = "When Fire Imp enters the battlefield, it deals 2 damage to target creature."
        )

        val script = cardScript("Fire Imp") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = DealDamageEffect(2, EffectTarget.TargetCreature)
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: ETB Bounce Cards
    // =========================================================================

    /**
     * Man-o'-War - 2U
     * Creature — Jellyfish
     * 2/2
     * When Man-o'-War enters the battlefield, return target creature to its owner's hand.
     */
    private fun registerManOWar() {
        val definition = CardDefinition.creature(
            name = "Man-o'-War",
            manaCost = ManaCost.parse("{2}{U}"),
            subtypes = setOf(Subtype.JELLYFISH),
            power = 2,
            toughness = 2,
            oracleText = "When Man-o'-War enters the battlefield, return target creature to its owner's hand."
        )

        val script = cardScript("Man-o'-War") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = ReturnToHandEffect(EffectTarget.TargetCreature)
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: ETB Draw/Discard Cards
    // =========================================================================

    /**
     * Owl Familiar - 1U
     * Creature — Bird
     * 1/1
     * Flying
     * When Owl Familiar enters the battlefield, draw a card, then discard a card.
     */
    private fun registerOwlFamiliar() {
        val definition = CardDefinition.creature(
            name = "Owl Familiar",
            manaCost = ManaCost.parse("{1}{U}"),
            subtypes = setOf(Subtype.BIRD),
            power = 1,
            toughness = 1,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying\nWhen Owl Familiar enters the battlefield, draw a card, then discard a card."
        )

        val script = cardScript("Owl Familiar") {
            keywords(Keyword.FLYING)
            triggered(
                trigger = OnEnterBattlefield(),
                effect = CompositeEffect(listOf(
                    DrawCardsEffect(1, EffectTarget.Controller),
                    DiscardCardsEffect(1, EffectTarget.Controller)
                ))
            )
        }

        register(definition, script)
    }

    /**
     * Ebon Dragon - 5BB
     * Creature — Dragon
     * 5/4
     * Flying
     * When Ebon Dragon enters the battlefield, you may have target opponent discard a card.
     */
    private fun registerEbonDragon() {
        val definition = CardDefinition.creature(
            name = "Ebon Dragon",
            manaCost = ManaCost.parse("{5}{B}{B}"),
            subtypes = setOf(Subtype.DRAGON),
            power = 5,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying\nWhen Ebon Dragon enters the battlefield, you may have target opponent discard a card."
        )

        val script = cardScript("Ebon Dragon") {
            keywords(Keyword.FLYING)
            triggered(
                trigger = OnEnterBattlefield(),
                effect = DiscardCardsEffect(1, EffectTarget.Opponent)
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: ETB Graveyard Recursion
    // =========================================================================

    /**
     * Gravedigger - 3B
     * Creature — Zombie
     * 2/2
     * When Gravedigger enters the battlefield, you may return target creature card
     * from your graveyard to your hand.
     */
    private fun registerGravedigger() {
        val definition = CardDefinition.creature(
            name = "Gravedigger",
            manaCost = ManaCost.parse("{3}{B}"),
            subtypes = setOf(Subtype.ZOMBIE),
            power = 2,
            toughness = 2,
            oracleText = "When Gravedigger enters the battlefield, you may return target creature card from your graveyard to your hand."
        )

        // Note: Full implementation requires targeting cards in graveyard
        // For now, register with placeholder - would need ReturnFromGraveyardEffect
        val script = cardScript("Gravedigger") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = ReturnFromGraveyardEffect(
                    filter = CardFilter.CreatureCard,
                    destination = SearchDestination.HAND
                )
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: On-Death Triggers
    // =========================================================================

    /**
     * Endless Cockroaches - 1BB
     * Creature — Insect
     * 1/1
     * When Endless Cockroaches dies, return it to its owner's hand.
     */
    private fun registerEndlessCockroaches() {
        val definition = CardDefinition.creature(
            name = "Endless Cockroaches",
            manaCost = ManaCost.parse("{1}{B}{B}"),
            subtypes = setOf(Subtype.INSECT),
            power = 1,
            toughness = 1,
            oracleText = "When Endless Cockroaches dies, return it to its owner's hand."
        )

        val script = cardScript("Endless Cockroaches") {
            triggered(
                trigger = OnDeath(),
                effect = ReturnToHandEffect(EffectTarget.Self)
            )
        }

        register(definition, script)
    }

    /**
     * Undying Beast - 3B
     * Creature — Beast
     * 3/2
     * When Undying Beast dies, put it on top of its owner's library.
     */
    private fun registerUndyingBeast() {
        val definition = CardDefinition.creature(
            name = "Undying Beast",
            manaCost = ManaCost.parse("{3}{B}"),
            subtypes = setOf(Subtype.BEAST),
            power = 3,
            toughness = 2,
            oracleText = "When Undying Beast dies, put it on top of its owner's library."
        )

        val script = cardScript("Undying Beast") {
            triggered(
                trigger = OnDeath(),
                effect = PutOnTopOfLibraryEffect(EffectTarget.Self)
            )
        }

        register(definition, script)
    }

    /**
     * Noxious Toad - 2B
     * Creature — Frog
     * 1/1
     * When Noxious Toad dies, each opponent discards a card.
     */
    private fun registerNoxiousToad() {
        val definition = CardDefinition.creature(
            name = "Noxious Toad",
            manaCost = ManaCost.parse("{2}{B}"),
            subtypes = setOf(Subtype.FROG),
            power = 1,
            toughness = 1,
            oracleText = "When Noxious Toad dies, each opponent discards a card."
        )

        val script = cardScript("Noxious Toad") {
            triggered(
                trigger = OnDeath(),
                effect = DiscardCardsEffect(1, EffectTarget.EachOpponent)
            )
        }

        register(definition, script)
    }

    /**
     * Fire Snake - 4R
     * Creature — Snake
     * 3/1
     * When Fire Snake dies, destroy target land.
     */
    private fun registerFireSnake() {
        val definition = CardDefinition.creature(
            name = "Fire Snake",
            manaCost = ManaCost.parse("{4}{R}"),
            subtypes = setOf(Subtype.SERPENT),
            power = 3,
            toughness = 1,
            oracleText = "When Fire Snake dies, destroy target land."
        )

        val script = cardScript("Fire Snake") {
            triggered(
                trigger = OnDeath(),
                effect = DestroyEffect(EffectTarget.TargetLand)
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 5: Attack Triggers
    // =========================================================================

    /**
     * Charging Bandits - 4B
     * Creature — Human Rogue
     * 3/3
     * Whenever Charging Bandits attacks, it gets +2/+0 until end of turn.
     */
    private fun registerChargingBandits() {
        val definition = CardDefinition.creature(
            name = "Charging Bandits",
            manaCost = ManaCost.parse("{4}{B}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.ROGUE),
            power = 3,
            toughness = 3,
            oracleText = "Whenever Charging Bandits attacks, it gets +2/+0 until end of turn."
        )

        val script = cardScript("Charging Bandits") {
            triggered(
                trigger = OnAttack(),
                effect = ModifyStatsEffect(2, 0, EffectTarget.Self, untilEndOfTurn = true)
            )
        }

        register(definition, script)
    }

    /**
     * Charging Paladin - 2W
     * Creature — Human Knight
     * 2/2
     * Whenever Charging Paladin attacks, it gets +0/+3 until end of turn.
     */
    private fun registerChargingPaladin() {
        val definition = CardDefinition.creature(
            name = "Charging Paladin",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
            power = 2,
            toughness = 2,
            oracleText = "Whenever Charging Paladin attacks, it gets +0/+3 until end of turn."
        )

        val script = cardScript("Charging Paladin") {
            triggered(
                trigger = OnAttack(),
                effect = ModifyStatsEffect(0, 3, EffectTarget.Self, untilEndOfTurn = true)
            )
        }

        register(definition, script)
    }

    /**
     * Seasoned Marshal - 2WW
     * Creature — Human Soldier
     * 2/2
     * Whenever Seasoned Marshal attacks, you may tap target creature.
     */
    private fun registerSeasonedMarshal() {
        val definition = CardDefinition.creature(
            name = "Seasoned Marshal",
            manaCost = ManaCost.parse("{2}{W}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
            power = 2,
            toughness = 2,
            oracleText = "Whenever Seasoned Marshal attacks, you may tap target creature."
        )

        val script = cardScript("Seasoned Marshal") {
            triggered(
                trigger = OnAttack(),
                effect = TapUntapEffect(EffectTarget.TargetCreature, tap = true)
            )
        }

        register(definition, script)
    }

    /**
     * Serpent Assassin - 3BB
     * Creature — Snake Assassin
     * 2/2
     * When Serpent Assassin enters the battlefield, you may destroy target nonblack creature.
     */
    private fun registerSerpentAssassin() {
        val definition = CardDefinition.creature(
            name = "Serpent Assassin",
            manaCost = ManaCost.parse("{3}{B}{B}"),
            subtypes = setOf(Subtype.SERPENT, Subtype.ASSASSIN),
            power = 2,
            toughness = 2,
            oracleText = "When Serpent Assassin enters the battlefield, you may destroy target nonblack creature."
        )

        val script = cardScript("Serpent Assassin") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = DestroyEffect(EffectTarget.TargetNonblackCreature)
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 6: Simple Damage Spells
    // =========================================================================

    /**
     * Scorching Spear - R
     * Sorcery
     * Scorching Spear deals 1 damage to any target.
     */
    private fun registerScorchingSpear() {
        val definition = CardDefinition.sorcery(
            name = "Scorching Spear",
            manaCost = ManaCost.parse("{R}"),
            oracleText = "Scorching Spear deals 1 damage to any target."
        )

        val script = cardScript("Scorching Spear") {
            spell(DealDamageEffect(1, EffectTarget.AnyTarget))
        }

        register(definition, script)
    }

    /**
     * Volcanic Hammer - 1R
     * Sorcery
     * Volcanic Hammer deals 3 damage to any target.
     */
    private fun registerVolcanicHammer() {
        val definition = CardDefinition.sorcery(
            name = "Volcanic Hammer",
            manaCost = ManaCost.parse("{1}{R}"),
            oracleText = "Volcanic Hammer deals 3 damage to any target."
        )

        val script = cardScript("Volcanic Hammer") {
            spell(DealDamageEffect(3, EffectTarget.AnyTarget))
        }

        register(definition, script)
    }

    /**
     * Lava Axe - 4R
     * Sorcery
     * Lava Axe deals 5 damage to target player or planeswalker.
     */
    private fun registerLavaAxe() {
        val definition = CardDefinition.sorcery(
            name = "Lava Axe",
            manaCost = ManaCost.parse("{4}{R}"),
            oracleText = "Lava Axe deals 5 damage to target player or planeswalker."
        )

        val script = cardScript("Lava Axe") {
            spell(DealDamageEffect(5, EffectTarget.Opponent))
        }

        register(definition, script)
    }

    /**
     * Bee Sting - 3G
     * Sorcery
     * Bee Sting deals 2 damage to any target.
     */
    private fun registerBeeString() {
        val definition = CardDefinition.sorcery(
            name = "Bee Sting",
            manaCost = ManaCost.parse("{3}{G}"),
            oracleText = "Bee Sting deals 2 damage to any target."
        )

        val script = cardScript("Bee Sting") {
            spell(DealDamageEffect(2, EffectTarget.AnyTarget))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 6: Mass Damage Spells
    // =========================================================================

    /**
     * Pyroclasm - 1R
     * Sorcery
     * Pyroclasm deals 2 damage to each creature.
     */
    private fun registerPyroclasm() {
        val definition = CardDefinition.sorcery(
            name = "Pyroclasm",
            manaCost = ManaCost.parse("{1}{R}"),
            oracleText = "Pyroclasm deals 2 damage to each creature."
        )

        val script = cardScript("Pyroclasm") {
            spell(DealDamageToAllCreaturesEffect(2))
        }

        register(definition, script)
    }

    /**
     * Dry Spell - 1B
     * Sorcery
     * Dry Spell deals 1 damage to each creature and each player.
     */
    private fun registerDrySpell() {
        val definition = CardDefinition.sorcery(
            name = "Dry Spell",
            manaCost = ManaCost.parse("{1}{B}"),
            oracleText = "Dry Spell deals 1 damage to each creature and each player."
        )

        val script = cardScript("Dry Spell") {
            spell(DealDamageToAllEffect(1))
        }

        register(definition, script)
    }

    /**
     * Fire Tempest - 5RR
     * Sorcery
     * Fire Tempest deals 6 damage to each creature and each player.
     */
    private fun registerFireTempest() {
        val definition = CardDefinition.sorcery(
            name = "Fire Tempest",
            manaCost = ManaCost.parse("{5}{R}{R}"),
            oracleText = "Fire Tempest deals 6 damage to each creature and each player."
        )

        val script = cardScript("Fire Tempest") {
            spell(DealDamageToAllEffect(6))
        }

        register(definition, script)
    }

    /**
     * Needle Storm - 2G
     * Sorcery
     * Needle Storm deals 4 damage to each creature with flying.
     */
    private fun registerNeedleStorm() {
        val definition = CardDefinition.sorcery(
            name = "Needle Storm",
            manaCost = ManaCost.parse("{2}{G}"),
            oracleText = "Needle Storm deals 4 damage to each creature with flying."
        )

        val script = cardScript("Needle Storm") {
            spell(DealDamageToAllCreaturesEffect(4, onlyFlying = true))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 6: Drain Effects
    // =========================================================================

    /**
     * Vampiric Feast - 5B
     * Sorcery
     * Vampiric Feast deals 4 damage to any target and you gain 4 life.
     */
    private fun registerVampiricFeast() {
        val definition = CardDefinition.sorcery(
            name = "Vampiric Feast",
            manaCost = ManaCost.parse("{5}{B}"),
            oracleText = "Vampiric Feast deals 4 damage to any target and you gain 4 life."
        )

        val script = cardScript("Vampiric Feast") {
            spell(DrainEffect(4, EffectTarget.AnyTarget))
        }

        register(definition, script)
    }

    /**
     * Vampiric Touch - 2B
     * Sorcery
     * Vampiric Touch deals 2 damage to target opponent or planeswalker and you gain 2 life.
     */
    private fun registerVampiricTouch() {
        val definition = CardDefinition.sorcery(
            name = "Vampiric Touch",
            manaCost = ManaCost.parse("{2}{B}"),
            oracleText = "Vampiric Touch deals 2 damage to target opponent or planeswalker and you gain 2 life."
        )

        val script = cardScript("Vampiric Touch") {
            spell(DrainEffect(2, EffectTarget.Opponent))
        }

        register(definition, script)
    }

    /**
     * Soul Shred - 2B
     * Sorcery
     * Soul Shred deals 3 damage to target nonblack creature. You gain 3 life.
     */
    private fun registerSoulShred() {
        val definition = CardDefinition.sorcery(
            name = "Soul Shred",
            manaCost = ManaCost.parse("{2}{B}"),
            oracleText = "Soul Shred deals 3 damage to target nonblack creature. You gain 3 life."
        )

        val script = cardScript("Soul Shred") {
            spell(DrainEffect(3, EffectTarget.TargetNonblackCreature))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 6: Simple Life Spells
    // =========================================================================

    /**
     * Sacred Nectar - 1W
     * Sorcery
     * You gain 4 life.
     */
    private fun registerSacredNectar() {
        val definition = CardDefinition.sorcery(
            name = "Sacred Nectar",
            manaCost = ManaCost.parse("{1}{W}"),
            oracleText = "You gain 4 life."
        )

        val script = cardScript("Sacred Nectar") {
            spell(GainLifeEffect(4, EffectTarget.Controller))
        }

        register(definition, script)
    }

    /**
     * Natural Spring - 3GG
     * Sorcery
     * Target player gains 8 life.
     */
    private fun registerNaturalSpring() {
        val definition = CardDefinition.sorcery(
            name = "Natural Spring",
            manaCost = ManaCost.parse("{3}{G}{G}"),
            oracleText = "Target player gains 8 life."
        )

        val script = cardScript("Natural Spring") {
            spell(GainLifeEffect(8, EffectTarget.AnyPlayer))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 7: Vanilla Creatures
    // =========================================================================

    /**
     * Border Guard - 2W
     * Creature — Human Soldier
     * 1/4
     */
    private fun registerBorderGuard() {
        val definition = CardDefinition.creature(
            name = "Border Guard",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
            power = 1,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Coral Eel - 1U
     * Creature — Fish
     * 2/1
     */
    private fun registerCoralEel() {
        val definition = CardDefinition.creature(
            name = "Coral Eel",
            manaCost = ManaCost.parse("{1}{U}"),
            subtypes = setOf(Subtype.EEL),
            power = 2,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Devoted Hero - W
     * Creature — Human Soldier
     * 1/2
     */
    private fun registerDevotedHero() {
        val definition = CardDefinition.creature(
            name = "Devoted Hero",
            manaCost = ManaCost.parse("{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
            power = 1,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Foot Soldiers - 3W
     * Creature — Human Soldier
     * 2/4
     */
    private fun registerFootSoldiers() {
        val definition = CardDefinition.creature(
            name = "Foot Soldiers",
            manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
            power = 2,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Giant Octopus - 3U
     * Creature — Octopus
     * 3/3
     */
    private fun registerGiantOctopus() {
        val definition = CardDefinition.creature(
            name = "Giant Octopus",
            manaCost = ManaCost.parse("{3}{U}"),
            subtypes = setOf(Subtype.OCTOPUS),
            power = 3,
            toughness = 3
        )
        registerVanilla(definition)
    }

    /**
     * Goblin Bully - 1R
     * Creature — Goblin
     * 2/1
     */
    private fun registerGoblinBully() {
        val definition = CardDefinition.creature(
            name = "Goblin Bully",
            manaCost = ManaCost.parse("{1}{R}"),
            subtypes = setOf(Subtype.GOBLIN),
            power = 2,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Gorilla Warrior - 2G
     * Creature — Ape Warrior
     * 3/2
     */
    private fun registerGorillaWarrior() {
        val definition = CardDefinition.creature(
            name = "Gorilla Warrior",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype.of("Ape"), Subtype.WARRIOR),
            power = 3,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Grizzly Bears - 1G
     * Creature — Bear
     * 2/2
     */
    private fun registerGrizzlyBears() {
        val definition = CardDefinition.creature(
            name = "Grizzly Bears",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype.BEAR),
            power = 2,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Highland Giant - 2RR
     * Creature — Giant
     * 3/4
     */
    private fun registerHighlandGiant() {
        val definition = CardDefinition.creature(
            name = "Highland Giant",
            manaCost = ManaCost.parse("{2}{R}{R}"),
            subtypes = setOf(Subtype.GIANT),
            power = 3,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Hill Giant - 3R
     * Creature — Giant
     * 3/3
     */
    private fun registerHillGiant() {
        val definition = CardDefinition.creature(
            name = "Hill Giant",
            manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype.GIANT),
            power = 3,
            toughness = 3
        )
        registerVanilla(definition)
    }

    /**
     * Horned Turtle - 2U
     * Creature — Turtle
     * 1/4
     */
    private fun registerHornedTurtle() {
        val definition = CardDefinition.creature(
            name = "Horned Turtle",
            manaCost = ManaCost.parse("{2}{U}"),
            subtypes = setOf(Subtype.TURTLE),
            power = 1,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Knight Errant - 3W
     * Creature — Human Knight
     * 2/2
     */
    private fun registerKnightErrant() {
        val definition = CardDefinition.creature(
            name = "Knight Errant",
            manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
            power = 2,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Lizard Warrior - 3R
     * Creature — Lizard Warrior
     * 4/2
     */
    private fun registerLizardWarrior() {
        val definition = CardDefinition.creature(
            name = "Lizard Warrior",
            manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype.LIZARD, Subtype.WARRIOR),
            power = 4,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Merfolk of the Pearl Trident - U
     * Creature — Merfolk
     * 1/1
     */
    private fun registerMerfolkOfThePearlTrident() {
        val definition = CardDefinition.creature(
            name = "Merfolk of the Pearl Trident",
            manaCost = ManaCost.parse("{U}"),
            subtypes = setOf(Subtype.MERFOLK),
            power = 1,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Minotaur Warrior - 2R
     * Creature — Minotaur Warrior
     * 2/3
     */
    private fun registerMinotaurWarrior() {
        val definition = CardDefinition.creature(
            name = "Minotaur Warrior",
            manaCost = ManaCost.parse("{2}{R}"),
            subtypes = setOf(Subtype.MINOTAUR, Subtype.WARRIOR),
            power = 2,
            toughness = 3
        )
        registerVanilla(definition)
    }

    /**
     * Muck Rats - B
     * Creature — Rat
     * 1/1
     */
    private fun registerMuckRats() {
        val definition = CardDefinition.creature(
            name = "Muck Rats",
            manaCost = ManaCost.parse("{B}"),
            subtypes = setOf(Subtype.RAT),
            power = 1,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Panther Warriors - 4G
     * Creature — Cat Warrior
     * 6/3
     */
    private fun registerPantherWarriors() {
        val definition = CardDefinition.creature(
            name = "Panther Warriors",
            manaCost = ManaCost.parse("{4}{G}"),
            subtypes = setOf(Subtype.CAT, Subtype.WARRIOR),
            power = 6,
            toughness = 3
        )
        registerVanilla(definition)
    }

    /**
     * Python - 3G
     * Creature — Snake
     * 3/2
     */
    private fun registerPython() {
        val definition = CardDefinition.creature(
            name = "Python",
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = setOf(Subtype.SERPENT),
            power = 3,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Redwood Treefolk - 4G
     * Creature — Treefolk
     * 3/6
     */
    private fun registerRedwoodTreefolk() {
        val definition = CardDefinition.creature(
            name = "Redwood Treefolk",
            manaCost = ManaCost.parse("{4}{G}"),
            subtypes = setOf(Subtype.TREEFOLK),
            power = 3,
            toughness = 6
        )
        registerVanilla(definition)
    }

    /**
     * Regal Unicorn - 2W
     * Creature — Unicorn
     * 2/3
     */
    private fun registerRegalUnicorn() {
        val definition = CardDefinition.creature(
            name = "Regal Unicorn",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype.UNICORN),
            power = 2,
            toughness = 3
        )
        registerVanilla(definition)
    }

    /**
     * Rowan Treefolk - 3G
     * Creature — Treefolk
     * 3/4
     */
    private fun registerRowanTreefolk() {
        val definition = CardDefinition.creature(
            name = "Rowan Treefolk",
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = setOf(Subtype.TREEFOLK),
            power = 3,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Skeletal Crocodile - 3B
     * Creature — Crocodile Skeleton
     * 5/1
     */
    private fun registerSkeletalCrocodile() {
        val definition = CardDefinition.creature(
            name = "Skeletal Crocodile",
            manaCost = ManaCost.parse("{3}{B}"),
            subtypes = setOf(Subtype.CROCODILE, Subtype.of("Skeleton")),
            power = 5,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Skeletal Snake - 1B
     * Creature — Snake Skeleton
     * 2/1
     */
    private fun registerSkeletalSnake() {
        val definition = CardDefinition.creature(
            name = "Skeletal Snake",
            manaCost = ManaCost.parse("{1}{B}"),
            subtypes = setOf(Subtype.SERPENT, Subtype.of("Skeleton")),
            power = 2,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Spined Wurm - 4G
     * Creature — Wurm
     * 5/4
     */
    private fun registerSpinedWurm() {
        val definition = CardDefinition.creature(
            name = "Spined Wurm",
            manaCost = ManaCost.parse("{4}{G}"),
            subtypes = setOf(Subtype.WURM),
            power = 5,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Whiptail Wurm - 6G
     * Creature — Wurm
     * 8/5
     */
    private fun registerWhiptailWurm() {
        val definition = CardDefinition.creature(
            name = "Whiptail Wurm",
            manaCost = ManaCost.parse("{6}{G}"),
            subtypes = setOf(Subtype.WURM),
            power = 8,
            toughness = 5
        )
        registerVanilla(definition)
    }

    // =========================================================================
    // Phase 7: French Vanilla Creatures
    // =========================================================================

    /**
     * Desert Drake - 3R
     * Creature — Drake
     * 2/2
     * Flying
     */
    private fun registerDesertDrake() {
        val definition = CardDefinition.creature(
            name = "Desert Drake",
            manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype.DRAKE),
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Djinn of the Lamp - 5UU
     * Creature — Djinn
     * 5/4
     * Flying
     */
    private fun registerDjinnOfTheLamp() {
        val definition = CardDefinition.creature(
            name = "Djinn of the Lamp",
            manaCost = ManaCost.parse("{5}{U}{U}"),
            subtypes = setOf(Subtype.DJINN),
            power = 5,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Elvish Ranger - 2G
     * Creature — Elf Ranger
     * 4/1
     */
    private fun registerElvishRanger() {
        val definition = CardDefinition.creature(
            name = "Elvish Ranger",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype.ELF, Subtype.RANGER),
            power = 4,
            toughness = 1
        )
        registerVanilla(definition)
    }

    /**
     * Feral Shadow - 2B
     * Creature — Shade
     * 2/1
     * Flying
     */
    private fun registerFeralShadow() {
        val definition = CardDefinition.creature(
            name = "Feral Shadow",
            manaCost = ManaCost.parse("{2}{B}"),
            subtypes = setOf(Subtype.of("Shade")),
            power = 2,
            toughness = 1,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Moon Sprite - 1G
     * Creature — Faerie
     * 1/1
     * Flying
     */
    private fun registerMoonSprite() {
        val definition = CardDefinition.creature(
            name = "Moon Sprite",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype.of("Faerie")),
            power = 1,
            toughness = 1,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Phantom Warrior - 1UU
     * Creature — Illusion Warrior
     * 2/2
     * Phantom Warrior can't be blocked.
     */
    private fun registerPhantomWarrior() {
        val definition = CardDefinition.creature(
            name = "Phantom Warrior",
            manaCost = ManaCost.parse("{1}{U}{U}"),
            subtypes = setOf(Subtype.of("Illusion"), Subtype.WARRIOR),
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.UNBLOCKABLE),
            oracleText = "Phantom Warrior can't be blocked."
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Raging Cougar - 2R
     * Creature — Cat
     * 2/2
     * Haste
     */
    private fun registerRagingCougar() {
        val definition = CardDefinition.creature(
            name = "Raging Cougar",
            manaCost = ManaCost.parse("{2}{R}"),
            subtypes = setOf(Subtype.CAT),
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.HASTE),
            oracleText = "Haste"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Raging Minotaur - 2RR
     * Creature — Minotaur Berserker
     * 3/3
     * Haste
     */
    private fun registerRagingMinotaur() {
        val definition = CardDefinition.creature(
            name = "Raging Minotaur",
            manaCost = ManaCost.parse("{2}{R}{R}"),
            subtypes = setOf(Subtype.MINOTAUR, Subtype.of("Berserker")),
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.HASTE),
            oracleText = "Haste"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Spotted Griffin - 3W
     * Creature — Griffin
     * 2/3
     * Flying
     */
    private fun registerSpottedGriffin() {
        val definition = CardDefinition.creature(
            name = "Spotted Griffin",
            manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype.GRIFFIN),
            power = 2,
            toughness = 3,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Starlit Angel - 3WW
     * Creature — Angel
     * 3/4
     * Flying, vigilance
     */
    private fun registerStarlitAngel() {
        val definition = CardDefinition.creature(
            name = "Starlit Angel",
            manaCost = ManaCost.parse("{3}{W}{W}"),
            subtypes = setOf(Subtype.ANGEL),
            power = 3,
            toughness = 4,
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            oracleText = "Flying, vigilance"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Volcanic Dragon - 4RR
     * Creature — Dragon
     * 4/4
     * Flying, haste
     */
    private fun registerVolcanicDragon() {
        val definition = CardDefinition.creature(
            name = "Volcanic Dragon",
            manaCost = ManaCost.parse("{4}{R}{R}"),
            subtypes = setOf(Subtype.DRAGON),
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            oracleText = "Flying, haste"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Wall of Granite - 1RR
     * Creature — Wall
     * 0/7
     * Defender
     */
    private fun registerWallOfGranite() {
        val definition = CardDefinition.creature(
            name = "Wall of Granite",
            manaCost = ManaCost.parse("{1}{R}{R}"),
            subtypes = setOf(Subtype.WALL),
            power = 0,
            toughness = 7,
            keywords = setOf(Keyword.DEFENDER),
            oracleText = "Defender"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Wall of Swords - 3W
     * Creature — Wall
     * 3/5
     * Defender, flying
     */
    private fun registerWallOfSwords() {
        val definition = CardDefinition.creature(
            name = "Wall of Swords",
            manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype.WALL),
            power = 3,
            toughness = 5,
            keywords = setOf(Keyword.DEFENDER, Keyword.FLYING),
            oracleText = "Defender, flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Wind Drake - 2U
     * Creature — Drake
     * 2/2
     * Flying
     */
    private fun registerWindDrake() {
        val definition = CardDefinition.creature(
            name = "Wind Drake",
            manaCost = ManaCost.parse("{2}{U}"),
            subtypes = setOf(Subtype.DRAKE),
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    // =========================================================================
    // Phase 7: Can't Block Creatures
    // =========================================================================

    /**
     * Craven Giant - 2R
     * Creature — Giant
     * 4/1
     * Craven Giant can't block.
     */
    private fun registerCravenGiant() {
        val definition = CardDefinition.creature(
            name = "Craven Giant",
            manaCost = ManaCost.parse("{2}{R}"),
            subtypes = setOf(Subtype.GIANT),
            power = 4,
            toughness = 1,
            oracleText = "Craven Giant can't block."
        )

        val script = cardScript("Craven Giant") {
            staticAbility(CantBlock(StaticTarget.SourceCreature))
        }

        register(definition, script)
    }

    /**
     * Craven Knight - 1B
     * Creature — Human Knight
     * 2/2
     * Craven Knight can't block.
     */
    private fun registerCravenKnight() {
        val definition = CardDefinition.creature(
            name = "Craven Knight",
            manaCost = ManaCost.parse("{1}{B}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
            power = 2,
            toughness = 2,
            oracleText = "Craven Knight can't block."
        )

        val script = cardScript("Craven Knight") {
            staticAbility(CantBlock(StaticTarget.SourceCreature))
        }

        register(definition, script)
    }

    /**
     * Hulking Cyclops - 3RR
     * Creature — Cyclops
     * 5/5
     * Hulking Cyclops can't block.
     */
    private fun registerHulkingCyclops() {
        val definition = CardDefinition.creature(
            name = "Hulking Cyclops",
            manaCost = ManaCost.parse("{3}{R}{R}"),
            subtypes = setOf(Subtype.CYCLOPS),
            power = 5,
            toughness = 5,
            oracleText = "Hulking Cyclops can't block."
        )

        val script = cardScript("Hulking Cyclops") {
            staticAbility(CantBlock(StaticTarget.SourceCreature))
        }

        register(definition, script)
    }

    /**
     * Hulking Goblin - 1R
     * Creature — Goblin
     * 2/2
     * Hulking Goblin can't block.
     */
    private fun registerHulkingGoblin() {
        val definition = CardDefinition.creature(
            name = "Hulking Goblin",
            manaCost = ManaCost.parse("{1}{R}"),
            subtypes = setOf(Subtype.GOBLIN),
            power = 2,
            toughness = 2,
            oracleText = "Hulking Goblin can't block."
        )

        val script = cardScript("Hulking Goblin") {
            staticAbility(CantBlock(StaticTarget.SourceCreature))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 7: Pump Spells
    // =========================================================================

    /**
     * Monstrous Growth - 1G
     * Sorcery
     * Target creature gets +4/+4 until end of turn.
     */
    private fun registerMonstrousGrowth() {
        val definition = CardDefinition.sorcery(
            name = "Monstrous Growth",
            manaCost = ManaCost.parse("{1}{G}"),
            oracleText = "Target creature gets +4/+4 until end of turn."
        )

        val script = cardScript("Monstrous Growth") {
            spell(ModifyStatsEffect(4, 4, EffectTarget.TargetCreature, untilEndOfTurn = true))
        }

        register(definition, script)
    }

    /**
     * Howling Fury - 2B
     * Sorcery
     * Target creature gets +4/+0 until end of turn.
     */
    private fun registerHowlingFury() {
        val definition = CardDefinition.sorcery(
            name = "Howling Fury",
            manaCost = ManaCost.parse("{2}{B}"),
            oracleText = "Target creature gets +4/+0 until end of turn."
        )

        val script = cardScript("Howling Fury") {
            spell(ModifyStatsEffect(4, 0, EffectTarget.TargetCreature, untilEndOfTurn = true))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 7: Destruction Spells
    // =========================================================================

    /**
     * Hand of Death - 2B
     * Sorcery
     * Destroy target nonblack creature.
     */
    private fun registerHandOfDeath() {
        val definition = CardDefinition.sorcery(
            name = "Hand of Death",
            manaCost = ManaCost.parse("{2}{B}"),
            oracleText = "Destroy target nonblack creature."
        )

        val script = cardScript("Hand of Death") {
            spell(DestroyEffect(EffectTarget.TargetNonblackCreature))
        }

        register(definition, script)
    }

    /**
     * Vengeance - 3W
     * Sorcery
     * Destroy target tapped creature.
     */
    private fun registerVengeance() {
        val definition = CardDefinition.sorcery(
            name = "Vengeance",
            manaCost = ManaCost.parse("{3}{W}"),
            oracleText = "Destroy target tapped creature."
        )

        val script = cardScript("Vengeance") {
            spell(DestroyEffect(EffectTarget.TargetTappedCreature))
        }

        register(definition, script)
    }

    /**
     * Path of Peace - 3W
     * Sorcery
     * Destroy target creature. Its controller gains 4 life.
     */
    private fun registerPathOfPeace() {
        val definition = CardDefinition.sorcery(
            name = "Path of Peace",
            manaCost = ManaCost.parse("{3}{W}"),
            oracleText = "Destroy target creature. Its controller gains 4 life."
        )

        val script = cardScript("Path of Peace") {
            spell(
                CompositeEffect(listOf(
                    DestroyEffect(EffectTarget.TargetCreature),
                    GainLifeEffect(4, EffectTarget.TargetController)
                ))
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Aura Enchantments
    // =========================================================================

    /**
     * Burning Cloak - R
     * Enchantment — Aura
     * Enchant creature
     * Enchanted creature gets +2/+0.
     */
    private fun registerBurningCloak() {
        val definition = CardDefinition.aura(
            name = "Burning Cloak",
            manaCost = ManaCost.parse("{R}"),
            oracleText = "Enchant creature\nEnchanted creature gets +2/+0."
        )

        val script = cardScript("Burning Cloak") {
            staticAbility(ModifyStats(2, 0, StaticTarget.AttachedCreature))
        }

        register(definition, script)
    }

    /**
     * Cloak of Feathers - U
     * Enchantment — Aura
     * Enchant creature
     * When Cloak of Feathers enters the battlefield, draw a card.
     * Enchanted creature has flying.
     */
    private fun registerCloakOfFeathers() {
        val definition = CardDefinition.aura(
            name = "Cloak of Feathers",
            manaCost = ManaCost.parse("{U}"),
            oracleText = "Enchant creature\nWhen Cloak of Feathers enters the battlefield, draw a card.\nEnchanted creature has flying."
        )

        val script = cardScript("Cloak of Feathers") {
            staticAbility(GrantKeyword(Keyword.FLYING, StaticTarget.AttachedCreature))
            triggered(
                trigger = OnEnterBattlefield(),
                effect = DrawCardsEffect(1, EffectTarget.Controller)
            )
        }

        register(definition, script)
    }

    /**
     * Nature's Cloak - 2G
     * Sorcery
     * All creatures you control gain forestwalk until end of turn.
     */
    private fun registerNaturesCloak() {
        val definition = CardDefinition.sorcery(
            name = "Nature's Cloak",
            manaCost = ManaCost.parse("{2}{G}"),
            oracleText = "All green creatures gain forestwalk until end of turn."
        )

        // For now, register as a simple spell - full forestwalk grant would need more infrastructure
        val script = cardScript("Nature's Cloak") {
            spell(GrantKeywordUntilEndOfTurnEffect(Keyword.FORESTWALK, EffectTarget.AllControlledCreatures))
        }

        register(definition, script)
    }

    /**
     * Defiant Stand - 1W
     * Instant
     * Target creature gets +1/+3 until end of turn.
     */
    private fun registerDefiantStand() {
        val definition = CardDefinition.instant(
            name = "Defiant Stand",
            manaCost = ManaCost.parse("{1}{W}"),
            oracleText = "Target creature gets +1/+3 until end of turn."
        )

        val script = cardScript("Defiant Stand") {
            spell(ModifyStatsEffect(1, 3, EffectTarget.TargetCreature, untilEndOfTurn = true))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Land Destruction
    // =========================================================================

    /**
     * Stone Rain - 2R
     * Sorcery
     * Destroy target land.
     */
    private fun registerStoneRain() {
        val definition = CardDefinition.sorcery(
            name = "Stone Rain",
            manaCost = ManaCost.parse("{2}{R}"),
            oracleText = "Destroy target land."
        )

        val script = cardScript("Stone Rain") {
            spell(DestroyEffect(EffectTarget.TargetLand))
        }

        register(definition, script)
    }

    /**
     * Flashfires - 3R
     * Sorcery
     * Destroy all Plains.
     */
    private fun registerFlashfires() {
        val definition = CardDefinition.sorcery(
            name = "Flashfires",
            manaCost = ManaCost.parse("{3}{R}"),
            oracleText = "Destroy all Plains."
        )

        val script = cardScript("Flashfires") {
            spell(DestroyAllLandsOfTypeEffect("Plains"))
        }

        register(definition, script)
    }

    /**
     * Boiling Seas - 3U
     * Sorcery
     * Destroy all Islands.
     */
    private fun registerBoilingSeas() {
        val definition = CardDefinition.sorcery(
            name = "Boiling Seas",
            manaCost = ManaCost.parse("{3}{U}"),
            oracleText = "Destroy all Islands."
        )

        val script = cardScript("Boiling Seas") {
            spell(DestroyAllLandsOfTypeEffect("Island"))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Mass Removal
    // =========================================================================

    /**
     * Wrath of God - 2WW
     * Sorcery
     * Destroy all creatures. They can't be regenerated.
     */
    private fun registerWrathOfGod() {
        val definition = CardDefinition.sorcery(
            name = "Wrath of God",
            manaCost = ManaCost.parse("{2}{W}{W}"),
            oracleText = "Destroy all creatures. They can't be regenerated."
        )

        val script = cardScript("Wrath of God") {
            spell(DestroyAllCreaturesEffect)
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Graveyard Recursion
    // =========================================================================

    /**
     * Raise Dead - B
     * Sorcery
     * Return target creature card from your graveyard to your hand.
     */
    private fun registerRaiseDead() {
        val definition = CardDefinition.sorcery(
            name = "Raise Dead",
            manaCost = ManaCost.parse("{B}"),
            oracleText = "Return target creature card from your graveyard to your hand."
        )

        val script = cardScript("Raise Dead") {
            spell(ReturnFromGraveyardEffect(CardFilter.CreatureCard, SearchDestination.HAND))
        }

        register(definition, script)
    }

    /**
     * Breath of Life - 3W
     * Sorcery
     * Return target creature card from your graveyard to the battlefield.
     */
    private fun registerBreathOfLife() {
        val definition = CardDefinition.sorcery(
            name = "Breath of Life",
            manaCost = ManaCost.parse("{3}{W}"),
            oracleText = "Return target creature card from your graveyard to the battlefield."
        )

        val script = cardScript("Breath of Life") {
            spell(ReturnFromGraveyardEffect(CardFilter.CreatureCard, SearchDestination.BATTLEFIELD))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Tutors
    // =========================================================================

    /**
     * Personal Tutor - U
     * Sorcery
     * Search your library for a sorcery card, reveal it, then shuffle and put that card on top.
     */
    private fun registerPersonalTutor() {
        val definition = CardDefinition.sorcery(
            name = "Personal Tutor",
            manaCost = ManaCost.parse("{U}"),
            oracleText = "Search your library for a sorcery card, reveal it, then shuffle and put that card on top."
        )

        val script = cardScript("Personal Tutor") {
            spell(
                SearchLibraryEffect(
                    filter = CardFilter.SorceryCard,
                    count = 1,
                    destination = SearchDestination.TOP_OF_LIBRARY,
                    shuffleAfter = true,
                    reveal = true
                )
            )
        }

        register(definition, script)
    }

    /**
     * Sylvan Tutor - G
     * Sorcery
     * Search your library for a creature card, reveal that card, shuffle, then put the card on top of your library.
     */
    private fun registerSylvanTutor() {
        val definition = CardDefinition.sorcery(
            name = "Sylvan Tutor",
            manaCost = ManaCost.parse("{G}"),
            oracleText = "Search your library for a creature card, reveal that card, shuffle, then put the card on top of your library."
        )

        val script = cardScript("Sylvan Tutor") {
            spell(
                SearchLibraryEffect(
                    filter = CardFilter.CreatureCard,
                    count = 1,
                    destination = SearchDestination.TOP_OF_LIBRARY,
                    shuffleAfter = true,
                    reveal = true
                )
            )
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Draw/Discard Spells
    // =========================================================================

    /**
     * Mind Rot - 2B
     * Sorcery
     * Target player discards two cards.
     */
    private fun registerMindRot() {
        val definition = CardDefinition.sorcery(
            name = "Mind Rot",
            manaCost = ManaCost.parse("{2}{B}"),
            oracleText = "Target player discards two cards."
        )

        val script = cardScript("Mind Rot") {
            spell(DiscardCardsEffect(2, EffectTarget.Opponent))
        }

        register(definition, script)
    }

    /**
     * Prosperity - XUU
     * Sorcery
     * Each player draws X cards.
     */
    private fun registerProsperity() {
        val definition = CardDefinition.sorcery(
            name = "Prosperity",
            manaCost = ManaCost.parse("{X}{U}{U}"),
            oracleText = "Each player draws X cards."
        )

        // For now, register with a placeholder - X costs need infrastructure
        val script = cardScript("Prosperity") {
            spell(DrawCardsEffect(3, EffectTarget.EachPlayer))
        }

        register(definition, script)
    }

    /**
     * Winds of Change - R
     * Sorcery
     * Each player shuffles the cards from their hand into their library,
     * then draws that many cards.
     */
    private fun registerWindsOfChange() {
        val definition = CardDefinition.sorcery(
            name = "Winds of Change",
            manaCost = ManaCost.parse("{R}"),
            oracleText = "Each player shuffles the cards from their hand into their library, then draws that many cards."
        )

        val script = cardScript("Winds of Change") {
            spell(WheelEffect(EffectTarget.EachPlayer))
        }

        register(definition, script)
    }

    /**
     * Touch of Brilliance - 3U
     * Sorcery
     * Draw two cards.
     */
    private fun registerTouchOfBrilliance() {
        val definition = CardDefinition.sorcery(
            name = "Touch of Brilliance",
            manaCost = ManaCost.parse("{3}{U}"),
            oracleText = "Draw two cards."
        )

        val script = cardScript("Touch of Brilliance") {
            spell(DrawCardsEffect(2, EffectTarget.Controller))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: Bounce Spells
    // =========================================================================

    /**
     * Symbol of Unsummoning - 2U
     * Sorcery
     * Return target creature to its owner's hand.
     */
    private fun registerSymbolOfUnsummoning() {
        val definition = CardDefinition.sorcery(
            name = "Symbol of Unsummoning",
            manaCost = ManaCost.parse("{2}{U}"),
            oracleText = "Return target creature to its owner's hand."
        )

        val script = cardScript("Symbol of Unsummoning") {
            spell(ReturnToHandEffect(EffectTarget.TargetCreature))
        }

        register(definition, script)
    }

    // =========================================================================
    // Phase 8: More Vanilla Creatures
    // =========================================================================

    /**
     * Raging Goblin - R
     * Creature — Goblin Berserker
     * 1/1
     * Haste
     */
    private fun registerRagingGoblin() {
        val definition = CardDefinition.creature(
            name = "Raging Goblin",
            manaCost = ManaCost.parse("{R}"),
            subtypes = setOf(Subtype.GOBLIN, Subtype.of("Berserker")),
            power = 1,
            toughness = 1,
            keywords = setOf(Keyword.HASTE),
            oracleText = "Haste"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Giant Spider - 3G
     * Creature — Spider
     * 2/4
     * Reach
     */
    private fun registerGiantSpider() {
        val definition = CardDefinition.creature(
            name = "Giant Spider",
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = setOf(Subtype.of("Spider")),
            power = 2,
            toughness = 4,
            keywords = setOf(Keyword.REACH),
            oracleText = "Reach"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Bog Imp - 1B
     * Creature — Imp
     * 1/1
     * Flying
     */
    private fun registerBogImp() {
        val definition = CardDefinition.creature(
            name = "Bog Imp",
            manaCost = ManaCost.parse("{1}{B}"),
            subtypes = setOf(Subtype.IMP),
            power = 1,
            toughness = 1,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Cloud Spirit - 2U
     * Creature — Spirit
     * 3/1
     * Flying
     * Cloud Spirit can block only creatures with flying.
     */
    private fun registerCloudSpirit() {
        val definition = CardDefinition.creature(
            name = "Cloud Spirit",
            manaCost = ManaCost.parse("{2}{U}"),
            subtypes = setOf(Subtype.SPIRIT),
            power = 3,
            toughness = 1,
            keywords = setOf(Keyword.FLYING),
            oracleText = "Flying\nCloud Spirit can block only creatures with flying."
        )
        // The blocking restriction would need more infrastructure
        registerFrenchVanilla(definition)
    }

    /**
     * Elite Cat Warrior - 2G
     * Creature — Cat Warrior
     * 2/3
     * Forestwalk
     */
    private fun registerEliteCatWarrior() {
        val definition = CardDefinition.creature(
            name = "Elite Cat Warrior",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype.CAT, Subtype.WARRIOR),
            power = 2,
            toughness = 3,
            keywords = setOf(Keyword.FORESTWALK),
            oracleText = "Forestwalk"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Mountain Goat - R
     * Creature — Goat
     * 1/1
     * Mountainwalk
     */
    private fun registerMountainGoat() {
        val definition = CardDefinition.creature(
            name = "Mountain Goat",
            manaCost = ManaCost.parse("{R}"),
            subtypes = setOf(Subtype.of("Goat")),
            power = 1,
            toughness = 1,
            keywords = setOf(Keyword.MOUNTAINWALK),
            oracleText = "Mountainwalk"
        )
        registerFrenchVanilla(definition)
    }

    /**
     * Sacred Knight - 2W
     * Creature — Human Knight
     * 2/2
     */
    private fun registerSacredKnight() {
        val definition = CardDefinition.creature(
            name = "Sacred Knight",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
            power = 2,
            toughness = 2
        )
        registerVanilla(definition)
    }

    /**
     * Stalking Tiger - 3G
     * Creature — Cat
     * 3/3
     * Stalking Tiger can't be blocked by more than one creature.
     */
    private fun registerStalkingTiger() {
        val definition = CardDefinition.creature(
            name = "Stalking Tiger",
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = setOf(Subtype.CAT),
            power = 3,
            toughness = 3,
            oracleText = "Stalking Tiger can't be blocked by more than one creature."
        )
        // Blocking restriction would need infrastructure - register as vanilla for now
        registerVanilla(definition)
    }

    /**
     * Thundering Wurm - 2GG
     * Creature — Wurm
     * 4/4
     */
    private fun registerThunderingWurm() {
        val definition = CardDefinition.creature(
            name = "Thundering Wurm",
            manaCost = ManaCost.parse("{2}{G}{G}"),
            subtypes = setOf(Subtype.WURM),
            power = 4,
            toughness = 4
        )
        registerVanilla(definition)
    }

    /**
     * Wood Elves - 2G
     * Creature — Elf Scout
     * 1/1
     * When Wood Elves enters the battlefield, search your library for a Forest card
     * and put that card onto the battlefield. Then shuffle your library.
     */
    private fun registerWoodElves() {
        val definition = CardDefinition.creature(
            name = "Wood Elves",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype.ELF, Subtype.SCOUT),
            power = 1,
            toughness = 1,
            oracleText = "When Wood Elves enters the battlefield, search your library for a Forest card and put that card onto the battlefield. Then shuffle your library."
        )

        val script = cardScript("Wood Elves") {
            triggered(
                trigger = OnEnterBattlefield(),
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = false,
                    shuffleAfter = true
                )
            )
        }

        register(definition, script)
    }

    /**
     * Charging Rhino - 3GG
     * Creature — Rhino
     * 4/4
     * Trample
     */
    private fun registerChargingRhino() {
        val definition = CardDefinition.creature(
            name = "Charging Rhino",
            manaCost = ManaCost.parse("{3}{G}{G}"),
            subtypes = setOf(Subtype.RHINO),
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.TRAMPLE),
            oracleText = "Charging Rhino can't be blocked by more than one creature."
        )
        // Blocking restriction would need infrastructure - trample is enough for now
        registerFrenchVanilla(definition)
    }
}
