package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gixian Skullflayer (BRO #100) — {2}{B} Creature — Phyrexian Human Assassin, 2/3.
 *
 * "At the beginning of your upkeep, if there are three or more creature cards in your graveyard,
 * put a +1/+1 counter on this creature."
 */
class GixianSkullflayerScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(): Int =
        state.getEntity(findPermanent("Gixian Skullflayer")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun game(creatureCardsInGraveyard: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Gixian Skullflayer")
            .withCardInGraveyard(1, "Forest")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withCardInLibrary(2, "Swamp")
        repeat(creatureCardsInGraveyard) { builder = builder.withCardInGraveyard(1, "Grizzly Bears") }
        // Start on the opponent's turn so the next upkeep reached is Player1's.
        return builder
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private fun TestGame.passToPlayer1Upkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    init {
        context("Gixian Skullflayer") {

            test("three creature cards in the graveyard adds a +1/+1 counter at upkeep") {
                val game = game(3)
                game.passToPlayer1Upkeep()

                withClue("the intervening-if is met, so a counter is added") {
                    game.counters() shouldBe 1
                }
            }

            test("two creature cards (plus a land) is not enough") {
                val game = game(2)
                game.passToPlayer1Upkeep()

                withClue("only two creature cards — the land doesn't count") {
                    game.counters() shouldBe 0
                }
            }
        }
    }
}
