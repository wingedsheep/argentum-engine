package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Marshal of the Lost. */
class MarshalOfTheLostScenarioTest : ScenarioTestBase() {

    init {
        context("Marshal of the Lost") {

            test("attack trigger pumps target by the number of attacking creatures") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Marshal of the Lost") // 3/3
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Attack with both creatures → 2 attacking creatures → +2/+2.
                game.declareAttackers(mapOf("Marshal of the Lost" to 2, "Glory Seeker" to 2))

                val seeker = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(seeker))
                game.resolveStack()

                val clientState = game.getClientState(1)
                val seekerCard = clientState.cards.values.find { it.name == "Glory Seeker" }
                withClue("Glory Seeker should be 4/4 (2/2 base + 2 attacking creatures)") {
                    seekerCard shouldNotBe null
                    seekerCard!!.power shouldBe 4
                    seekerCard.toughness shouldBe 4
                }
            }
        }
    }
}
