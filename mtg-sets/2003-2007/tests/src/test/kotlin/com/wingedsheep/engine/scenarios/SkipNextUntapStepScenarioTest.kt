package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine test for [SkipNextUntapStepComponent] — "that player skips their next untap step"
 * (CR 500.11, 614.10a). Shisato, Whispering Hunter's scenario test covers the card; this one covers
 * the step-skip rules that no single card exercises: stacked skips, and "next untap step" markers
 * waiting for the first untap step that isn't skipped.
 */
class SkipNextUntapStepScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(name: String) =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    /** On to Bob's next upkeep — just past his next untap step — through Alice's turn. */
    private fun TestGame.toBobsNextUpkeep() {
        do {
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        } while (state.activePlayerId != player2Id)
    }

    private fun bobWithTappedBears() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
        .apply { repeat(3) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)
        .build()

    init {
        context("skip next untap step") {

            test("two pending skips skip the next two untap steps (CR 614.10a)") {
                val game = bobWithTappedBears()
                game.state = game.state.updateEntity(game.player2Id) { it.with(SkipNextUntapStepComponent(steps = 2)) }

                game.toBobsNextUpkeep()
                withClue("first untap step skipped, one skip still pending") {
                    game.isTapped("Grizzly Bears") shouldBe true
                    game.state.getEntity(game.player2Id)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 1
                }

                game.toBobsNextUpkeep()
                withClue("second untap step skipped too, nothing pending") {
                    game.isTapped("Grizzly Bears") shouldBe true
                    game.state.getEntity(game.player2Id)!!.has<SkipNextUntapStepComponent>() shouldBe false
                }

                game.toBobsNextUpkeep()
                withClue("the third untap step happens") { game.isTapped("Grizzly Bears") shouldBe false }
            }

            test("a 'doesn't untap during the next untap step' marker waits out a skipped step") {
                val game = bobWithTappedBears()
                game.state = game.state.updateEntity(game.player2Id) {
                    it.with(SkipNextUntapStepComponent()).with(SkipUntapComponent())
                }

                game.toBobsNextUpkeep()
                withClue("the skipped step didn't consume the Exhaustion-style marker") {
                    game.isTapped("Grizzly Bears") shouldBe true
                    game.state.getEntity(game.player2Id)!!.has<SkipUntapComponent>() shouldBe true
                }

                game.toBobsNextUpkeep()
                withClue("the first real untap step applies the marker and consumes it") {
                    game.isTapped("Grizzly Bears") shouldBe true
                    game.state.getEntity(game.player2Id)!!.has<SkipUntapComponent>() shouldBe false
                }

                game.toBobsNextUpkeep()
                game.isTapped("Grizzly Bears") shouldBe false
            }

            test("only the skipping player is affected — the other player's untap step is normal") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .apply { repeat(2) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                game.state = game.state.updateEntity(game.player2Id) { it.with(SkipNextUntapStepComponent()) }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.isTapped("Grizzly Bears") shouldBe false
                game.state.getEntity(game.player2Id)!!.has<SkipNextUntapStepComponent>() shouldBe true
            }
        }
    }
}
