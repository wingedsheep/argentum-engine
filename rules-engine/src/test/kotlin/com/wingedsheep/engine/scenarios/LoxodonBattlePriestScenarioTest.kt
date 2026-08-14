package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Loxodon Battle Priest. */
class LoxodonBattlePriestScenarioTest : ScenarioTestBase() {

    init {
        context("Loxodon Battle Priest") {

            test("begin-combat trigger puts a +1/+1 counter on another creature you control") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Loxodon Battle Priest")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val seeker = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(seeker))
                game.resolveStack()

                val clientState = game.getClientState(1)
                val seekerCard = clientState.cards.values.find { it.name == "Glory Seeker" }
                withClue("Glory Seeker should be 3/3 after the +1/+1 counter") {
                    seekerCard shouldNotBe null
                    seekerCard!!.power shouldBe 3
                    seekerCard.toughness shouldBe 3
                }
            }

            test("trigger cannot target the Battle Priest itself (another target creature)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Loxodon Battle Priest")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val priest = game.findPermanent("Loxodon Battle Priest")!!
                val decision = game.getPendingDecision()
                withClue("There should be a target decision for the begin-combat trigger") {
                    decision shouldNotBe null
                }
                val result = game.selectTargets(listOf(priest))
                withClue("Targeting the Priest itself is illegal ('another target creature')") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
