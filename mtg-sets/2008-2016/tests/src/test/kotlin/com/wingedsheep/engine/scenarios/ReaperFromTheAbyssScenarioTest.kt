package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Reaper from the Abyss (ISD) — Morbid: at the beginning of each end step, if a
 * creature died this turn, destroy target non-Demon creature.
 */
class ReaperFromTheAbyssScenarioTest : ScenarioTestBase() {

    init {
        context("Reaper from the Abyss") {
            test("with a creature dead this turn, the end-step trigger destroys a non-Demon creature") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reaper from the Abyss", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                game.castSpell(1, "Doom Blade", targetId = bears).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false

                val elves = game.findPermanent("Llanowar Elves").shouldNotBeNull()
                game.passUntilPhase(Phase.ENDING, Step.END)
                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the Reaper is a Demon, so only the non-Demon creatures are legal targets") {
                    decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(giant, elves)
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("morbid is satisfied, so the Hill Giant is destroyed") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isOnBattlefield("Reaper from the Abyss") shouldBe true
                }
            }

            test("with nothing dead this turn, the ability never triggers") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reaper from the Abyss", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("no creature died, so no target is asked for and the Giant survives") {
                    game.hasPendingDecision() shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
