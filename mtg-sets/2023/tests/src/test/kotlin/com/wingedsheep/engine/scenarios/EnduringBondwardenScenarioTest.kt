package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Enduring Bondwarden (MOM #14) — {W} 0/1. Backup 1; "When this creature dies, put its counters on
 * target creature you control."
 *
 * Backup on another creature grants it the dies trigger until end of turn; on itself only the counter
 * lands, and the printed dies trigger moves it on.
 */
class EnduringBondwardenScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castBondwarden(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Enduring Bondwarden")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Enduring Bondwarden").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    private fun answerDiesTrigger(game: TestGame, recipient: EntityId) {
        if (game.hasPendingDecision()) {
            game.selectTargets(listOf(recipient)).error shouldBe null
        }
        game.resolveStack()
    }

    init {
        context("Enduring Bondwarden") {

            test("backup on another creature: it gains the dies trigger and passes its counters on") {
                val game = castBondwarden()
                val bears = game.findPermanent("Grizzly Bears")!!
                val bondwarden = game.findPermanent("Enduring Bondwarden")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                plusOnes(game, bears) shouldBe 1
                plusOnes(game, bondwarden) shouldBe 0

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                withClue("the Bears died") { game.findPermanent("Grizzly Bears") shouldBe null }

                answerDiesTrigger(game, bondwarden)
                withClue("the granted dies trigger moved the Bears' counter onto the Bondwarden") {
                    plusOnes(game, bondwarden) shouldBe 1
                }
            }

            test("backup on itself: the printed dies trigger moves its counter to another creature") {
                val game = castBondwarden()
                val bondwarden = game.findPermanent("Enduring Bondwarden")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bondwarden)).error shouldBe null
                game.resolveStack()

                plusOnes(game, bondwarden) shouldBe 1
                game.state.projectedState.getToughness(bondwarden) shouldBe 2

                game.castSpell(1, "Lightning Bolt", bondwarden).error shouldBe null
                game.resolveStack()
                withClue("the Bondwarden died") { game.findPermanent("Enduring Bondwarden") shouldBe null }

                answerDiesTrigger(game, bears)
                withClue("its +1/+1 counter moved onto the Bears") { plusOnes(game, bears) shouldBe 1 }
            }
        }
    }
}
