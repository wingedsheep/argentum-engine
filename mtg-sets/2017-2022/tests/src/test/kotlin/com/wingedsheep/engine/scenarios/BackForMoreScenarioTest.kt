package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Back for More (IKO #177) — {4}{B}{G} Instant.
 *
 * "Return target creature card from your graveyard to the battlefield. When you do, it fights up to
 *  one target creature you don't control."
 *
 * The fight is a reflexive trigger (CR 603.12): its target is chosen only after the creature is on
 * the battlefield, and the fighter is the returned creature — read from a pipeline snapshot of the
 * spell's own target, since the reflexive resolution rebinds the context targets.
 */
class BackForMoreScenarioTest : ScenarioTestBase() {

    private fun scenarioWithCourserInGraveyard() = scenario()
        .withPlayers()
        .withCardInHand(1, "Back for More")
        .withCardInGraveyard(1, "Centaur Courser")      // 3/3
        .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the returned creature fights the opponent's creature chosen after it returns") {
            val game = scenarioWithCourserInGraveyard()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpellTargetingGraveyardCard(1, "Back for More", 1, "Centaur Courser").error shouldBe null
            game.resolveStack()

            withClue("the Courser is already back when the fight's target is asked for") {
                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("a 3/3 Courser fought a 2/2 Bears: the Bears died, the Courser survived with 2 damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                val courser = game.findPermanent("Centaur Courser")
                courser shouldNotBe null
            }
        }

        test("\"up to one\" — declining the fight still returns the creature") {
            val game = scenarioWithCourserInGraveyard()

            game.castSpellTargetingGraveyardCard(1, "Back for More", 1, "Centaur Courser").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.skipTargets()
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
