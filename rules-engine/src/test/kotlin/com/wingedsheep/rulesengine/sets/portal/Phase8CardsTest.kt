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
 * Tests for Phase 8 Portal cards - Auras, Land Destruction, Mass Removal, Tutors, and more.
 */
class Phase8CardsTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCardToBattlefield(
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

    // =========================================================================
    // Aura Registration Tests
    // =========================================================================

    context("Aura card registration") {
        test("Burning Cloak is registered correctly") {
            val card = PortalSet.getCardDefinition("Burning Cloak")
            card.shouldNotBeNull()
            card.name shouldBe "Burning Cloak"
            card.manaCost.toString() shouldBe "{R}"
            card.isEnchantment shouldBe true
            card.isAura shouldBe true
        }

        test("Burning Cloak has ModifyStats static ability") {
            val script = PortalSet.getCardScript("Burning Cloak")
            script.shouldNotBeNull()
            script.staticAbilities.size shouldBe 1
            script.staticAbilities.first().shouldBeInstanceOf<ModifyStats>()
            val ability = script.staticAbilities.first() as ModifyStats
            ability.powerBonus shouldBe 2
            ability.toughnessBonus shouldBe 0
        }

        test("Cloak of Feathers is registered correctly") {
            val card = PortalSet.getCardDefinition("Cloak of Feathers")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{U}"
            card.isAura shouldBe true
        }

        test("Cloak of Feathers has GrantKeyword and ETB trigger") {
            val script = PortalSet.getCardScript("Cloak of Feathers")
            script.shouldNotBeNull()
            script.staticAbilities.size shouldBe 1
            script.staticAbilities.first().shouldBeInstanceOf<GrantKeyword>()
            val ability = script.staticAbilities.first() as GrantKeyword
            ability.keyword shouldBe Keyword.FLYING
            script.triggeredAbilities.size shouldBe 1
        }

        test("Defiant Stand is registered as instant") {
            val card = PortalSet.getCardDefinition("Defiant Stand")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{1}{W}"
            card.isInstant shouldBe true
        }
    }

    // =========================================================================
    // Land Destruction Tests
    // =========================================================================

    context("Land destruction spells") {
        test("Stone Rain is registered correctly") {
            val card = PortalSet.getCardDefinition("Stone Rain")
            card.shouldNotBeNull()
            card.name shouldBe "Stone Rain"
            card.manaCost.toString() shouldBe "{2}{R}"
            card.isSorcery shouldBe true
        }

        test("Stone Rain has destroy land effect") {
            val script = PortalSet.getCardScript("Stone Rain")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<DestroyEffect>()
            (effect as DestroyEffect).target shouldBe EffectTarget.TargetLand
        }

        test("Flashfires is registered correctly") {
            val card = PortalSet.getCardDefinition("Flashfires")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{3}{R}"
        }

        test("Flashfires destroys all Plains") {
            val script = PortalSet.getCardScript("Flashfires")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<DestroyAllLandsOfTypeEffect>()
            (effect as DestroyAllLandsOfTypeEffect).landType shouldBe "Plains"
        }

        test("Boiling Seas is registered correctly") {
            val card = PortalSet.getCardDefinition("Boiling Seas")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{3}{U}"
        }

        test("Boiling Seas destroys all Islands") {
            val script = PortalSet.getCardScript("Boiling Seas")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<DestroyAllLandsOfTypeEffect>()
            (effect as DestroyAllLandsOfTypeEffect).landType shouldBe "Island"
        }
    }

    // =========================================================================
    // Mass Removal Tests
    // =========================================================================

    context("Mass removal spells") {
        test("Wrath of God is registered correctly") {
            val card = PortalSet.getCardDefinition("Wrath of God")
            card.shouldNotBeNull()
            card.name shouldBe "Wrath of God"
            card.manaCost.toString() shouldBe "{2}{W}{W}"
            card.isSorcery shouldBe true
        }

        test("Wrath of God has destroy all creatures effect") {
            val script = PortalSet.getCardScript("Wrath of God")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect shouldBe DestroyAllCreaturesEffect
        }
    }

    context("DestroyAllCreaturesEffect execution") {
        test("destroys all creatures on the battlefield") {
            var state = newGame()

            val creatureDef = CardDefinition.creature(
                name = "Test Creature",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.BEAR),
                power = 2,
                toughness = 2
            )

            val (creature1, state1) = state.addCardToBattlefield(creatureDef, player1Id)
            val (creature2, state2) = state1.addCardToBattlefield(creatureDef, player2Id)
            state = state2

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, DestroyAllCreaturesEffect, context)

            // Both creatures should be in graveyards
            result.state.getBattlefield().contains(creature1) shouldBe false
            result.state.getBattlefield().contains(creature2) shouldBe false
        }
    }

    // =========================================================================
    // Graveyard Recursion Tests
    // =========================================================================

    context("Graveyard recursion") {
        test("Raise Dead is registered correctly") {
            val card = PortalSet.getCardDefinition("Raise Dead")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{B}"
            card.isSorcery shouldBe true
        }

        test("Raise Dead has return from graveyard effect") {
            val script = PortalSet.getCardScript("Raise Dead")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<ReturnFromGraveyardEffect>()
            val raiseEffect = effect as ReturnFromGraveyardEffect
            raiseEffect.filter shouldBe CardFilter.CreatureCard
            raiseEffect.destination shouldBe SearchDestination.HAND
        }
    }

    // =========================================================================
    // Tutor Tests
    // =========================================================================

    context("Tutor spells") {
        test("Personal Tutor is registered correctly") {
            val card = PortalSet.getCardDefinition("Personal Tutor")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{U}"
            card.isSorcery shouldBe true
        }

        test("Personal Tutor searches for sorcery card") {
            val script = PortalSet.getCardScript("Personal Tutor")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<SearchLibraryEffect>()
            val searchEffect = effect as SearchLibraryEffect
            searchEffect.filter shouldBe CardFilter.SorceryCard
            searchEffect.destination shouldBe SearchDestination.TOP_OF_LIBRARY
        }
    }

    // =========================================================================
    // Draw/Discard Tests
    // =========================================================================

    context("Draw and discard spells") {
        test("Mind Rot is registered correctly") {
            val card = PortalSet.getCardDefinition("Mind Rot")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{2}{B}"
            card.isSorcery shouldBe true
        }

        test("Mind Rot makes opponent discard 2") {
            val script = PortalSet.getCardScript("Mind Rot")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<DiscardCardsEffect>()
            val discardEffect = effect as DiscardCardsEffect
            discardEffect.count shouldBe 2
        }

        test("Touch of Brilliance is registered correctly") {
            val card = PortalSet.getCardDefinition("Touch of Brilliance")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{3}{U}"
        }

        test("Touch of Brilliance draws 2 cards") {
            val script = PortalSet.getCardScript("Touch of Brilliance")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<DrawCardsEffect>()
            (effect as DrawCardsEffect).count shouldBe 2
        }

        test("Winds of Change is registered correctly") {
            val card = PortalSet.getCardDefinition("Winds of Change")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{R}"
        }

        test("Winds of Change has wheel effect") {
            val script = PortalSet.getCardScript("Winds of Change")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<WheelEffect>()
        }
    }

    // =========================================================================
    // Bounce Spell Tests
    // =========================================================================

    context("Bounce spells") {
        test("Symbol of Unsummoning is registered correctly") {
            val card = PortalSet.getCardDefinition("Symbol of Unsummoning")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{2}{U}"
        }

        test("Symbol of Unsummoning returns creature to hand") {
            val script = PortalSet.getCardScript("Symbol of Unsummoning")
            script.shouldNotBeNull()
            val effect = script.spellEffect?.effect
            effect.shouldBeInstanceOf<ReturnToHandEffect>()
        }
    }

    // =========================================================================
    // More Vanilla Creature Tests
    // =========================================================================

    context("Phase 8 vanilla creatures") {
        test("Raging Goblin is registered with haste") {
            val card = PortalSet.getCardDefinition("Raging Goblin")
            card.shouldNotBeNull()
            card.manaCost.toString() shouldBe "{R}"
            card.creatureStats?.basePower shouldBe 1
            card.creatureStats?.baseToughness shouldBe 1
            card.keywords.contains(Keyword.HASTE) shouldBe true
        }

        test("Giant Spider is registered with reach") {
            val card = PortalSet.getCardDefinition("Giant Spider")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 4
            card.keywords.contains(Keyword.REACH) shouldBe true
        }

        test("Bog Imp is registered with flying") {
            val card = PortalSet.getCardDefinition("Bog Imp")
            card.shouldNotBeNull()
            card.keywords.contains(Keyword.FLYING) shouldBe true
        }

        test("Elite Cat Warrior is registered with forestwalk") {
            val card = PortalSet.getCardDefinition("Elite Cat Warrior")
            card.shouldNotBeNull()
            card.keywords.contains(Keyword.FORESTWALK) shouldBe true
        }

        test("Mountain Goat is registered with mountainwalk") {
            val card = PortalSet.getCardDefinition("Mountain Goat")
            card.shouldNotBeNull()
            card.keywords.contains(Keyword.MOUNTAINWALK) shouldBe true
        }

        test("Wood Elves has ETB search for Forest") {
            val card = PortalSet.getCardDefinition("Wood Elves")
            card.shouldNotBeNull()

            val script = PortalSet.getCardScript("Wood Elves")
            script.shouldNotBeNull()
            script.triggeredAbilities.size shouldBe 1
            val trigger = script.triggeredAbilities.first()
            trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
            trigger.effect.shouldBeInstanceOf<SearchLibraryEffect>()
        }

        test("Charging Rhino is registered with trample") {
            val card = PortalSet.getCardDefinition("Charging Rhino")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 4
            card.creatureStats?.baseToughness shouldBe 4
            card.keywords.contains(Keyword.TRAMPLE) shouldBe true
        }

        test("Thundering Wurm is vanilla 4/4") {
            val card = PortalSet.getCardDefinition("Thundering Wurm")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 4
            card.creatureStats?.baseToughness shouldBe 4
        }

        test("Sacred Knight is vanilla 2/2") {
            val card = PortalSet.getCardDefinition("Sacred Knight")
            card.shouldNotBeNull()
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
        }
    }

    // =========================================================================
    // All Phase 8 Cards Registration Check
    // =========================================================================

    context("All Phase 8 cards are registered") {
        val phase8Cards = listOf(
            // Auras
            "Burning Cloak", "Cloak of Feathers", "Defiant Stand",
            // Land Destruction
            "Stone Rain", "Flashfires", "Boiling Seas",
            // Mass Removal
            "Wrath of God",
            // Recursion
            "Raise Dead",
            // Tutors
            "Personal Tutor",
            // Draw/Discard
            "Mind Rot", "Prosperity", "Winds of Change", "Touch of Brilliance",
            // Bounce
            "Symbol of Unsummoning",
            // Creatures
            "Raging Goblin", "Giant Spider", "Bog Imp", "Cloud Spirit",
            "Elite Cat Warrior", "Mountain Goat", "Sacred Knight",
            "Stalking Tiger", "Thundering Wurm", "Wood Elves", "Charging Rhino"
        )

        phase8Cards.forEach { cardName ->
            test("$cardName is registered") {
                val card = PortalSet.getCardDefinition(cardName)
                card.shouldNotBeNull()
            }
        }
    }
})
