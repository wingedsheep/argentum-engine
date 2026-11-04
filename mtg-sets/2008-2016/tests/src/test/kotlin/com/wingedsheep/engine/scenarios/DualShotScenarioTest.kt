package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DualShotScenarioTest : ScenarioTestBase() {
    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Dual Shot")
        .withCardInHand(2, "Unsummon")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(2, "Island", 1)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(targets: List<EntityId> = emptyList()) = execute(CastSpell(
        player1Id,
        findCardsInHand(1, "Dual Shot").single(),
        targets.map { ChosenTarget.Permanent(it) },
    ))

    init {
        test("may resolve with zero targets") {
            val game = game()
            game.cast().error shouldBe null
            game.resolveStack().flatMap { it.events }.filterIsInstance<DamageDealtEvent>() shouldBe emptyList()
            game.isInGraveyard(1, "Dual Shot") shouldBe true
        }

        test("may deal one damage to just one creature") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            game.cast(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
            (game.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }

        test("deals one damage to each of two creatures") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            game.cast(listOf(bears, courser)).error shouldBe null
            val damage = game.resolveStack().flatMap { it.events }.filterIsInstance<DamageDealtEvent>()
            damage.map { it.targetId }.toSet() shouldBe setOf(bears, courser)
            damage.map { it.amount } shouldBe listOf(1, 1)
            game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
            game.state.getEntity(courser)?.get<DamageComponent>()?.amount shouldBe 1
        }

        test("cannot choose the same creature twice") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            (game.cast(listOf(bears, bears)).error != null) shouldBe true
        }

        test("the remaining legal creature is damaged when the first target leaves") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            game.cast(listOf(bears, courser)).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpell(2, "Unsummon", bears).error shouldBe null
            game.resolveStack()
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.state.getEntity(courser)?.get<DamageComponent>()?.amount shouldBe 1
        }

        test("a shared finite shield can reserve prevention for the second target") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            val shield = ActiveFloatingEffect(
                id = EntityId("dual-shot-shared-shield"),
                effect = FloatingEffectData(
                    layer = Layer.ABILITY,
                    modification = SerializableModification.PreventNextDamage(1),
                    affectedEntities = setOf(bears, courser),
                ),
                duration = Duration.EndOfTurn,
                sourceId = null,
                sourceName = "Shared shield",
                controllerId = game.player2Id,
                timestamp = game.state.timestamp,
            )
            game.state = game.state.copy(floatingEffects = game.state.floatingEffects + shield)
            game.cast(listOf(bears, courser)).error shouldBe null
            game.resolveStack()
            val allocation = game.state.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
            allocation.playerId shouldBe game.player2Id
            allocation.minValue shouldBe 0
            allocation.maxValue shouldBe 1
            // The damage instruction is still pending in full while the shield allocation is chosen.
            (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            (game.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            game.chooseNumber(0).error shouldBe null
            game.state.pendingDecision shouldBe null
            game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
            (game.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            game.state.floatingEffects.any { it.id == shield.id } shouldBe false
            game.isInGraveyard(1, "Dual Shot") shouldBe true
        }
    }
}
