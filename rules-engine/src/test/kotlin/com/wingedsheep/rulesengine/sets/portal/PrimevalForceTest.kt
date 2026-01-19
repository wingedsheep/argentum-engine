package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.SacrificeCost
import com.wingedsheep.rulesengine.ability.SacrificeUnlessEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.decision.EffectSacrificeUnlessDecision
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Primeval Force card.
 *
 * Primeval Force - 2GGG
 * Creature — Elemental
 * 8/8
 * When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests.
 */
class PrimevalForceTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val plainsDef = CardDefinition.basicLand("Plains", Subtype.PLAINS)

    val primevalForceDef = CardDefinition.creature(
        name = "Primeval Force",
        manaCost = ManaCost.parse("{2}{G}{G}{G}"),
        subtypes = setOf(Subtype.ELEMENTAL),
        power = 8,
        toughness = 8,
        oracleText = "When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests."
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addLandToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (landId, state1) = createEntity(EntityId.generate(), components)
        return landId to state1.addToZone(landId, ZoneId.BATTLEFIELD)
    }

    fun GameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (creatureId, state1) = createEntity(EntityId.generate(), components)
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    val registry = EffectHandlerRegistry.default()

    context("Primeval Force card registration") {
        test("is registered in PortalSet") {
            val primevalForce = PortalSet.getCardDefinition("Primeval Force")
            primevalForce.shouldNotBeNull()
            primevalForce.name shouldBe "Primeval Force"
            primevalForce.manaCost.toString() shouldBe "{2}{G}{G}{G}"
            primevalForce.isCreature shouldBe true
            primevalForce.creatureStats?.basePower shouldBe 8
            primevalForce.creatureStats?.baseToughness shouldBe 8
        }

        test("has ETB trigger with sacrifice unless effect") {
            val script = PortalSet.getCardScript("Primeval Force")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<SacrificeUnlessEffect>()

            val sacrificeEffect = trigger.effect as SacrificeUnlessEffect
            sacrificeEffect.permanentToSacrifice shouldBe EffectTarget.Self
            sacrificeEffect.cost.count shouldBe 3
            sacrificeEffect.cost.filter.shouldBeInstanceOf<CardFilter.HasSubtype>()
            (sacrificeEffect.cost.filter as CardFilter.HasSubtype).subtype shouldBe "Forest"
        }
    }

    context("SacrificeUnlessEffect execution") {
        test("auto-sacrifices when player has no Forests") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // No Forests on battlefield
            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val result = registry.execute(state, effect, context)

            // Force should be sacrificed (no choice possible)
            result.needsPlayerInput.shouldBeFalse()
            result.state.getBattlefield() shouldNotContain forceId
            result.state.getZone(graveyardZone) shouldContain forceId
        }

        test("auto-sacrifices when player has fewer than 3 Forests") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // Add only 2 Forests
            val (forest1Id, state2) = state.addLandToBattlefield(forestDef, player1Id)
            val (forest2Id, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            state = state3

            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val result = registry.execute(state, effect, context)

            // Force should be auto-sacrificed (can't pay cost)
            result.needsPlayerInput.shouldBeFalse()
            result.state.getBattlefield() shouldNotContain forceId
            result.state.getZone(graveyardZone) shouldContain forceId

            // Forests should still be on battlefield
            result.state.getBattlefield() shouldContain forest1Id
            result.state.getBattlefield() shouldContain forest2Id
        }

        test("presents choice when player has 3 or more Forests") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // Add 3 Forests
            val (forest1Id, state2) = state.addLandToBattlefield(forestDef, player1Id)
            val (forest2Id, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            val (forest3Id, state4) = state3.addLandToBattlefield(forestDef, player1Id)
            state = state4

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val result = registry.execute(state, effect, context)

            // Should need player input
            result.needsPlayerInput.shouldBeTrue()
            result.pendingDecision.shouldNotBeNull()
            result.pendingDecision.shouldBeInstanceOf<EffectSacrificeUnlessDecision>()

            val decision = result.pendingDecision as EffectSacrificeUnlessDecision
            decision.permanentToSacrifice shouldBe forceId
            decision.permanentName shouldBe "Primeval Force"
            decision.requiredCount shouldBe 3
            decision.canPayCost.shouldBeTrue()
            decision.validCostTargets shouldHaveSize 3
        }

        test("player can choose to sacrifice Forests to keep creature") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // Add 3 Forests
            val (forest1Id, state2) = state.addLandToBattlefield(forestDef, player1Id)
            val (forest2Id, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            val (forest3Id, state4) = state3.addLandToBattlefield(forestDef, player1Id)
            state = state4

            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val initialResult = registry.execute(state, effect, context)

            // Player chooses to pay the cost (sacrifice Forests)
            val finalResult = initialResult.continuation!!.resume(listOf(forest1Id, forest2Id, forest3Id))

            // Force should remain on battlefield
            finalResult.state.getBattlefield() shouldContain forceId

            // Forests should be in graveyard
            finalResult.state.getZone(graveyardZone) shouldContain forest1Id
            finalResult.state.getZone(graveyardZone) shouldContain forest2Id
            finalResult.state.getZone(graveyardZone) shouldContain forest3Id
        }

        test("player can choose to sacrifice creature instead of Forests") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // Add 3 Forests
            val (forest1Id, state2) = state.addLandToBattlefield(forestDef, player1Id)
            val (forest2Id, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            val (forest3Id, state4) = state3.addLandToBattlefield(forestDef, player1Id)
            state = state4

            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val initialResult = registry.execute(state, effect, context)

            // Player chooses NOT to pay (empty selection = sacrifice the creature)
            val finalResult = initialResult.continuation!!.resume(emptyList())

            // Force should be in graveyard
            finalResult.state.getBattlefield() shouldNotContain forceId
            finalResult.state.getZone(graveyardZone) shouldContain forceId

            // Forests should remain on battlefield
            finalResult.state.getBattlefield() shouldContain forest1Id
            finalResult.state.getBattlefield() shouldContain forest2Id
            finalResult.state.getBattlefield() shouldContain forest3Id
        }

        test("only counts Forests controlled by player") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // Add 2 Forests for player 1, 5 Forests for player 2
            val (forest1Id, state2) = state.addLandToBattlefield(forestDef, player1Id)
            val (forest2Id, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            val (_, state4) = state3.addLandToBattlefield(forestDef, player2Id)
            val (_, state5) = state4.addLandToBattlefield(forestDef, player2Id)
            val (_, state6) = state5.addLandToBattlefield(forestDef, player2Id)
            val (_, state7) = state6.addLandToBattlefield(forestDef, player2Id)
            val (_, state8) = state7.addLandToBattlefield(forestDef, player2Id)
            state = state8

            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val result = registry.execute(state, effect, context)

            // Player 1 only has 2 Forests, so auto-sacrifice
            result.needsPlayerInput.shouldBeFalse()
            result.state.getBattlefield() shouldNotContain forceId
            result.state.getZone(graveyardZone) shouldContain forceId
        }

        test("only counts lands with Forest subtype") {
            var state = newGame()

            // Add Primeval Force to battlefield
            val (forceId, state1) = state.addCreatureToBattlefield(primevalForceDef, player1Id)
            state = state1

            // Add 2 Forests and 3 Plains
            val (forest1Id, state2) = state.addLandToBattlefield(forestDef, player1Id)
            val (forest2Id, state3) = state2.addLandToBattlefield(forestDef, player1Id)
            val (_, state4) = state3.addLandToBattlefield(plainsDef, player1Id)
            val (_, state5) = state4.addLandToBattlefield(plainsDef, player1Id)
            val (_, state6) = state5.addLandToBattlefield(plainsDef, player1Id)
            state = state6

            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            // Execute sacrifice unless effect
            val effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )

            val context = ExecutionContext(player1Id, forceId)
            val result = registry.execute(state, effect, context)

            // Only 2 Forests, not 3, so auto-sacrifice
            result.needsPlayerInput.shouldBeFalse()
            result.state.getBattlefield() shouldNotContain forceId
            result.state.getZone(graveyardZone) shouldContain forceId
        }
    }
})
