package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Daily Bugle Reporters (SPM #6) —
 * {3}{W} Creature — Human Citizen, 2/3, Common.
 *
 *   When this creature enters, choose one —
 *   • Puff Piece — Put a +1/+1 counter on each of up to two target creatures.
 *   • Investigative Journalism — Return target creature card with mana value 2 or less
 *     from your graveyard to your hand.
 *
 * Covers both ETB modes: the up-to-two-target +1/+1 fan-out (Puff Piece) and the
 * graveyard-return restricted to mana value 2 or less (Investigative Journalism).
 */
class DailyBugleReportersScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Daily Bugle Reporters") {

            test("Puff Piece puts a +1/+1 counter on each of up to two target creatures") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Daily Bugle Reporters")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val cast = game.castSpell(1, "Daily Bugle Reporters")
                withClue("Daily Bugle Reporters should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

                game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for Puff Piece; got ${game.getPendingDecision()}")
                game.selectTargets(listOf(bears, giant))
                game.resolveStack()

                withClue("Grizzly Bears gets one +1/+1 counter") {
                    plusOneCounters(game, bears) shouldBe 1
                }
                withClue("Hill Giant gets one +1/+1 counter") {
                    plusOneCounters(game, giant) shouldBe 1
                }
            }

            test("Investigative Journalism returns only a creature card with mana value 2 or less") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Daily Bugle Reporters")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInGraveyard(1, "Grizzly Bears") // {1}{G} — mana value 2, eligible
                    .withCardInGraveyard(1, "Hill Giant")    // {3}{R} — mana value 4, ineligible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("precondition: two creature cards in your graveyard") {
                    game.graveyardSize(1) shouldBe 2
                }

                val cast = game.castSpell(1, "Daily Bugle Reporters")
                withClue("Daily Bugle Reporters should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 1))

                val targetDecision = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for Investigative Journalism; got ${game.getPendingDecision()}")
                val bearsInGrave = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.selectTargets(listOf(bearsInGrave))
                game.resolveStack()

                withClue("the mana value 2 creature returns to your hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("the mana value 4 creature is not eligible and stays in the graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.graveyardSize(1) shouldBe 1
                }
            }
        }
    }
}
