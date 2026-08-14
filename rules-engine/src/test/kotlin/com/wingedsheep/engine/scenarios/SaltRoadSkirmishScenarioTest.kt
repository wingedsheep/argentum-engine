package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Salt Road Skirmish. */
class SaltRoadSkirmishScenarioTest : ScenarioTestBase() {

    init {
        context("Salt Road Skirmish") {

            test("destroys target creature and creates two hasty Warriors") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Salt Road Skirmish")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Glory Seeker") // target to destroy
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Salt Road Skirmish", targetId = victim)
                game.resolveStack()

                withClue("Target creature should be destroyed") {
                    game.findPermanent("Glory Seeker") shouldBe null
                }
                val warriors = game.findPermanents("Warrior Token")
                withClue("Two Warrior tokens should be created with haste") {
                    warriors.size shouldBe 2
                    warriors.all {
                        game.getClientState(1).cards[it]?.keywords?.contains(Keyword.HASTE) == true
                    } shouldBe true
                }

                // They are sacrificed at the beginning of the next end step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("Warriors should be sacrificed at the next end step") {
                    game.findPermanents("Warrior Token").size shouldBe 0
                }
            }
        }
    }
}
