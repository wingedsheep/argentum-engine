package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.SelectCardsDecision

/** Invasion of Tarkir // Defiant Thundermaw. */
class InvasionOfTarkirScenarioTest : ScenarioTestBase() {
    private fun TestGame.cast() {
        castSpell(1, "Invasion of Tarkir").error shouldBe null
        resolveStack()
    }

    private fun setup(dragons: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Invasion of Tarkir")
        .also { b -> repeat(dragons) { b.withCardInHand(1, "Shivan Dragon") } }
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("front: revealing two Dragons deals 4") {
            val game = setup(2)
            game.cast()
            val d = game.getPendingDecision() as SelectCardsDecision
            game.selectCards(d.options.filter { id ->
                game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Shivan Dragon"
            }).error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 16
        }

        test("front: revealing nothing still deals 2") {
            val game = setup(1)
            game.cast()
            game.selectCards(emptyList()).error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("back: an attacking Dragon deals 2 damage to any target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Tarkir")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()
            // defense 5: Bolt + Bolt = 6
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Tarkir")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val thundermaw = game.findPermanent("Defiant Thundermaw")!!
            game.state = game.state.updateEntity(thundermaw) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Defiant Thundermaw" to 2)).error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
