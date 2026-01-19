package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.script.ResolvedTarget
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Phase 7 Portal cards - Vanilla Creatures, French Vanilla, Pump Spells, and Destruction Spells.
 */
class Phase7CardsTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        return cardId to state1.addToZone(cardId, ZoneId.BATTLEFIELD)
    }

    val registry = EffectHandlerRegistry.default()

    val testCreatureDef = CardDefinition.creature(
        name = "Test Creature",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    // =========================================================================
    // Vanilla Creature Registration Tests
    // =========================================================================

    context("Vanilla creature registration") {
        test("Grizzly Bears is registered correctly") {
            val card = PortalSet.getCardDefinition("Grizzly Bears")
            card.shouldNotBeNull()
            card.name shouldBe "Grizzly Bears"
            card.manaCost.toString() shouldBe "{1}{G}"
            card.isCreature shouldBe true
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
            card.typeLine.subtypes.contains(Subtype.BEAR) shouldBe true
        }

        test("Hill Giant is registered correctly") {
            val card = PortalSet.getCardDefinition("Hill Giant")
            card.shouldNotBeNull()
            card.name shouldBe "Hill Giant"
            card.manaCost.toString() shouldBe "{3}{R}"
            card.creatureStats?.basePower shouldBe 3
            card.creatureStats?.baseToughness shouldBe 3
            card.typeLine.subtypes.contains(Subtype.GIANT) shouldBe true
        }

        test("Spined Wurm is registered correctly") {
            val card = PortalSet.getCardDefinition("Spined Wurm")
            card.shouldNotBeNull()
            card.name shouldBe "Spined Wurm"
            card.manaCost.toString() shouldBe "{4}{G}"
            card.creatureStats?.basePower shouldBe 5
            card.creatureStats?.baseToughness shouldBe 4
            card.typeLine.subtypes.contains(Subtype.WURM) shouldBe true
        }

        test("Border Guard is registered correctly") {
            val card = PortalSet.getCardDefinition("Border Guard")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 1
            card.creatureStats?.baseToughness shouldBe 4
        }

        test("Muck Rats is registered correctly") {
            val card = PortalSet.getCardDefinition("Muck Rats")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{B}"
            card.creatureStats?.basePower shouldBe 1
            card.creatureStats?.baseToughness shouldBe 1
            card.typeLine.subtypes.contains(Subtype.RAT) shouldBe true
        }

        test("Merfolk of the Pearl Trident is registered correctly") {
            val card = PortalSet.getCardDefinition("Merfolk of the Pearl Trident")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{U}"
            card.creatureStats?.basePower shouldBe 1
            card.creatureStats?.baseToughness shouldBe 1
            card.typeLine.subtypes.contains(Subtype.MERFOLK) shouldBe true
        }

        test("Panther Warriors is registered correctly") {
            val card = PortalSet.getCardDefinition("Panther Warriors")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 6
            card.creatureStats?.baseToughness shouldBe 3
        }

        test("Whiptail Wurm is registered correctly") {
            val card = PortalSet.getCardDefinition("Whiptail Wurm")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 8
            card.creatureStats?.baseToughness shouldBe 5
        }
    }

    // =========================================================================
    // French Vanilla Creature Registration Tests
    // =========================================================================

    context("French vanilla creature registration") {
        test("Wind Drake is registered with flying") {
            val card = PortalSet.getCardDefinition("Wind Drake")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{2}{U}"
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
            card.keywords.contains(Keyword.FLYING) shouldBe true
        }

        test("Volcanic Dragon is registered with flying and haste") {
            val card = PortalSet.getCardDefinition("Volcanic Dragon")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{4}{R}{R}"
            card.creatureStats?.basePower shouldBe 4
            card.creatureStats?.baseToughness shouldBe 4
            card.keywords.contains(Keyword.FLYING) shouldBe true
            card.keywords.contains(Keyword.HASTE) shouldBe true
        }

        test("Starlit Angel is registered with flying and vigilance") {
            val card = PortalSet.getCardDefinition("Starlit Angel")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 3
            card.creatureStats?.baseToughness shouldBe 4
            card.keywords.contains(Keyword.FLYING) shouldBe true
            card.keywords.contains(Keyword.VIGILANCE) shouldBe true
        }

        test("Raging Cougar is registered with haste") {
            val card = PortalSet.getCardDefinition("Raging Cougar")
            card.shouldNotBeNull()
            card.keywords.contains(Keyword.HASTE) shouldBe true
        }

        test("Wall of Granite is registered with defender") {
            val card = PortalSet.getCardDefinition("Wall of Granite")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 0
            card.creatureStats?.baseToughness shouldBe 7
            card.keywords.contains(Keyword.DEFENDER) shouldBe true
        }

        test("Wall of Swords is registered with defender and flying") {
            val card = PortalSet.getCardDefinition("Wall of Swords")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 3
            card.creatureStats?.baseToughness shouldBe 5
            card.keywords.contains(Keyword.DEFENDER) shouldBe true
            card.keywords.contains(Keyword.FLYING) shouldBe true
        }

        test("Phantom Warrior is registered as unblockable") {
            val card = PortalSet.getCardDefinition("Phantom Warrior")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
            card.keywords.contains(Keyword.UNBLOCKABLE) shouldBe true
        }

        test("Djinn of the Lamp is registered correctly") {
            val card = PortalSet.getCardDefinition("Djinn of the Lamp")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 5
            card.creatureStats?.baseToughness shouldBe 4
            card.keywords.contains(Keyword.FLYING) shouldBe true
        }
    }

    // =========================================================================
    // Can't Block Creature Tests
    // =========================================================================

    context("Can't block creature registration") {
        test("Jungle Lion is registered correctly") {
            val card = PortalSet.getCardDefinition("Jungle Lion")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{G}"
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 1
        }

        test("Jungle Lion has can't block script") {
            val script = PortalSet.getCardScript("Jungle Lion")
            script.shouldNotBeNull()
            script.staticAbilities.size shouldBe 1
            script.staticAbilities.first().shouldBeInstanceOf<CantBlock>()
        }

        test("Hulking Cyclops is registered correctly") {
            val card = PortalSet.getCardDefinition("Hulking Cyclops")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 5
            card.creatureStats?.baseToughness shouldBe 5
        }

        test("Hulking Cyclops has can't block script") {
            val script = PortalSet.getCardScript("Hulking Cyclops")
            script.shouldNotBeNull()
            script.staticAbilities.size shouldBe 1
            script.staticAbilities.first().shouldBeInstanceOf<CantBlock>()
        }

        test("Craven Giant is registered correctly") {
            val card = PortalSet.getCardDefinition("Craven Giant")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 4
            card.creatureStats?.baseToughness shouldBe 1
        }

        test("Hulking Goblin is registered correctly") {
            val card = PortalSet.getCardDefinition("Hulking Goblin")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
        }

        test("Craven Knight is registered correctly") {
            val card = PortalSet.getCardDefinition("Craven Knight")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
        }
    }

    // =========================================================================
    // Pump Spell Tests
    // =========================================================================

    context("Pump spell registration") {
        test("Monstrous Growth is registered correctly") {
            val card = PortalSet.getCardDefinition("Monstrous Growth")
            card.shouldNotBeNull()
            card.name shouldBe "Monstrous Growth"
            card.manaCost.toString() shouldBe "{1}{G}"
            card.isSorcery shouldBe true
        }

        test("Monstrous Growth has +4/+4 effect") {
            val script = PortalSet.getCardScript("Monstrous Growth")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldNotBeNull()
            effect.shouldBeInstanceOf<ModifyStatsEffect>()
            (effect as ModifyStatsEffect).powerModifier shouldBe 4
            effect.toughnessModifier shouldBe 4
            effect.untilEndOfTurn shouldBe true
        }

        test("Howling Fury is registered correctly") {
            val card = PortalSet.getCardDefinition("Howling Fury")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{2}{B}"
            card.isSorcery shouldBe true
        }

        test("Howling Fury has +4/+0 effect") {
            val script = PortalSet.getCardScript("Howling Fury")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldNotBeNull()
            effect.shouldBeInstanceOf<ModifyStatsEffect>()
            (effect as ModifyStatsEffect).powerModifier shouldBe 4
            effect.toughnessModifier shouldBe 0
        }
    }

    context("Pump spell execution") {
        test("ModifyStatsEffect produces StatsModified event and temporary modifier") {
            var state = newGame()
            val (creatureId, state1) = state.addCreatureToBattlefield(testCreatureDef, player1Id)
            state = state1

            val effect = ModifyStatsEffect(4, 4, EffectTarget.TargetCreature, untilEndOfTurn = true)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(creatureId))
            )

            val result = registry.execute(state, effect, context)

            // Check that the event was produced
            result.events.any { event ->
                event is com.wingedsheep.rulesengine.ecs.script.EffectEvent.StatsModified &&
                    event.entityId == creatureId &&
                    event.powerDelta == 4 &&
                    event.toughnessDelta == 4
            } shouldBe true

            // Check that temporary modifier was created
            result.temporaryModifiers.size shouldBe 1
        }
    }

    // =========================================================================
    // Destruction Spell Tests
    // =========================================================================

    context("Destruction spell registration") {
        test("Hand of Death is registered correctly") {
            val card = PortalSet.getCardDefinition("Hand of Death")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{2}{B}"
            card.isSorcery shouldBe true
        }

        test("Hand of Death has destroy nonblack creature effect") {
            val script = PortalSet.getCardScript("Hand of Death")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldNotBeNull()
            effect.shouldBeInstanceOf<DestroyEffect>()
            (effect as DestroyEffect).target shouldBe EffectTarget.TargetNonblackCreature
        }

        test("Vengeance is registered correctly") {
            val card = PortalSet.getCardDefinition("Vengeance")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{3}{W}"
            card.isSorcery shouldBe true
        }

        test("Vengeance has destroy tapped creature effect") {
            val script = PortalSet.getCardScript("Vengeance")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldNotBeNull()
            effect.shouldBeInstanceOf<DestroyEffect>()
            (effect as DestroyEffect).target shouldBe EffectTarget.TargetTappedCreature
        }

        test("Path of Peace is registered correctly") {
            val card = PortalSet.getCardDefinition("Path of Peace")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{3}{W}"
            card.isSorcery shouldBe true
        }

        test("Path of Peace has destroy and life gain effect") {
            val script = PortalSet.getCardScript("Path of Peace")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldNotBeNull()
            effect.shouldBeInstanceOf<CompositeEffect>()
            val composite = effect as CompositeEffect
            composite.effects.size shouldBe 2
            composite.effects[0].shouldBeInstanceOf<DestroyEffect>()
            composite.effects[1].shouldBeInstanceOf<GainLifeEffect>()
            (composite.effects[1] as GainLifeEffect).amount shouldBe 4
            (composite.effects[1] as GainLifeEffect).target shouldBe EffectTarget.TargetController
        }
    }

    // =========================================================================
    // All Vanilla Creatures Registration Check
    // =========================================================================

    context("All vanilla creatures are registered") {
        val vanillaCreatures = listOf(
            "Border Guard", "Coral Eel", "Devoted Hero", "Foot Soldiers",
            "Giant Octopus", "Goblin Bully", "Gorilla Warrior", "Grizzly Bears",
            "Highland Giant", "Hill Giant", "Horned Turtle", "Knight Errant",
            "Lizard Warrior", "Merfolk of the Pearl Trident", "Minotaur Warrior",
            "Muck Rats", "Panther Warriors", "Python", "Redwood Treefolk",
            "Regal Unicorn", "Rowan Treefolk", "Skeletal Crocodile",
            "Skeletal Snake", "Spined Wurm", "Whiptail Wurm"
        )

        vanillaCreatures.forEach { cardName ->
            test("$cardName is registered") {
                val card = PortalSet.getCardDefinition(cardName)
                card.shouldNotBeNull()
                card.isCreature shouldBe true
            }
        }
    }

    context("All French vanilla creatures are registered") {
        val frenchVanillaCreatures = listOf(
            "Desert Drake" to setOf(Keyword.FLYING),
            "Djinn of the Lamp" to setOf(Keyword.FLYING),
            "Elvish Ranger" to emptySet(),  // Actually vanilla, no keywords
            "Feral Shadow" to setOf(Keyword.FLYING),
            "Moon Sprite" to setOf(Keyword.FLYING),
            "Phantom Warrior" to setOf(Keyword.UNBLOCKABLE),
            "Raging Cougar" to setOf(Keyword.HASTE),
            "Raging Minotaur" to setOf(Keyword.HASTE),
            "Spotted Griffin" to setOf(Keyword.FLYING),
            "Starlit Angel" to setOf(Keyword.FLYING, Keyword.VIGILANCE),
            "Volcanic Dragon" to setOf(Keyword.FLYING, Keyword.HASTE),
            "Wall of Granite" to setOf(Keyword.DEFENDER),
            "Wall of Swords" to setOf(Keyword.DEFENDER, Keyword.FLYING),
            "Wind Drake" to setOf(Keyword.FLYING)
        )

        frenchVanillaCreatures.forEach { (cardName, expectedKeywords) ->
            test("$cardName is registered with correct keywords") {
                val card = PortalSet.getCardDefinition(cardName)
                card.shouldNotBeNull()
                card.isCreature shouldBe true
                expectedKeywords.forEach { keyword ->
                    card.keywords.contains(keyword) shouldBe true
                }
            }
        }
    }
})
