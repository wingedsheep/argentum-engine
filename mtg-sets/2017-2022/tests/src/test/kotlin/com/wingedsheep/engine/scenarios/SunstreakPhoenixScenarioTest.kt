package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Sunstreak Phoenix (MID #162).
 *
 * "Whenever day becomes night or night becomes day, you may pay {1}{R}. If you do, return this card
 *  from your graveyard to the battlefield tapped." — the trigger functions from the graveyard.
 */
class SunstreakPhoenixScenarioTest : ScenarioTestBase() {

    /** Player 1 ends a turn with no spells cast while it's day; the opponent's untap step makes it night. */
    private fun phoenixInGraveyardAtDusk(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInGraveyard(1, "Sunstreak Phoenix")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.ENDING, Step.END)
            .build()
        game.state = game.state.copy(dayNight = DayNight.DAY)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.state.dayNight shouldBe DayNight.NIGHT
        game.resolveStack()
        return game
    }

    init {
        context("Sunstreak Phoenix") {

            test("paying {1}{R} when day becomes night returns it from the graveyard tapped") {
                val game = phoenixInGraveyardAtDusk()

                withClue("the graveyard trigger offers the may-pay") { game.hasPendingDecision() shouldBe true }
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                val phoenix = game.findPermanent("Sunstreak Phoenix")
                withClue("the Phoenix is back on the battlefield") { phoenix shouldNotBe null }
                withClue("and it entered tapped") {
                    game.state.getEntity(phoenix!!)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("declining to pay leaves it in the graveyard") {
                val game = phoenixInGraveyardAtDusk()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(1, "Sunstreak Phoenix") shouldBe true
            }
        }
    }
}
