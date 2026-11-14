package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Bounding Krasis (ORI #212) — {1}{G}{U} Creature — Fish Lizard, 3/3.
 *
 *   Flash
 *   When this creature enters, you may tap or untap target creature.
 */
class BoundingKrasisScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.chooseOption(prefix: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(prefix, ignoreCase = true) }
        check(index >= 0) { "$prefix not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
    }

    /** Cast Bounding Krasis, aim its ETB at [target], accept the "may" and pick [mode]. */
    private fun TestGame.castKrasisAndChoose(target: EntityId, mode: String) {
        val cast = castSpell(1, "Bounding Krasis")
        withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
        resolveStack()

        withClue("the ETB trigger asks for its target") {
            selectTargets(listOf(target)).error shouldBe null
        }
        resolveStack()
        getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        answerYesNo(true).error shouldBe null
        chooseOption(mode)
        resolveStack()
    }

    init {
        context("Bounding Krasis") {

            test("has flash: cast on the opponent's turn, it untaps your tapped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bounding Krasis")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.isTapped(bears) shouldBe true

                game.castKrasisAndChoose(bears, "Untap")

                game.isOnBattlefield("Bounding Krasis") shouldBe true
                withClue("Grizzly Bears was untapped") { game.isTapped(bears) shouldBe false }
            }

            test("taps an opponent's untapped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bounding Krasis")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castKrasisAndChoose(giant, "Tap")

                withClue("Hill Giant was tapped") { game.isTapped(giant) shouldBe true }
            }
        }
    }
}
