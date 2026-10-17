package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Halfdane (LEG #229) — "At the beginning of your upkeep, Halfdane's base power and toughness become
 * equal to the power and toughness of target creature other than Halfdane until the end of your next
 * upkeep."
 *
 * The copied numbers last through the opponent's turn and into Halfdane's controller's next upkeep,
 * where the new trigger resets them; the old setting ends as that upkeep ends.
 */
class HalfdaneScenarioTest : ScenarioTestBase() {

    init {
        context("Halfdane") {

            test("copies the target's power and toughness until the end of its controller's next upkeep") {
                val builder = scenario()
                    .withPlayers("Halfdane Player", "Opponent")
                    .withCardOnBattlefield(1, "Halfdane")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()
                val halfdane = game.findPermanent("Halfdane")!!
                fun power() = game.state.projectedState.getPower(halfdane)
                fun toughness() = game.state.projectedState.getToughness(halfdane)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("printed 3/3 before the trigger resolves") { power() shouldBe 3 }
                game.selectTargets(listOf(game.findPermanent("Craw Wurm")!!))
                game.resolveStack()
                withClue("Craw Wurm is 6/4") {
                    power() shouldBe 6
                    toughness() shouldBe 4
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the upkeep it was set in doesn't end it") { power() shouldBe 6 }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("lasts through the opponent's turn") { power() shouldBe 6 }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("still 6/4 during the next upkeep, while the new trigger waits") {
                    power() shouldBe 6
                    toughness() shouldBe 4
                }
                game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!))
                game.resolveStack()
                withClue("the new setting (2/2) wins on timestamp") {
                    power() shouldBe 2
                    toughness() shouldBe 2
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the first setting ended with that upkeep; only the new one remains") {
                    game.state.floatingEffects.count { it.duration == Duration.EndOfYourNextUpkeep } shouldBe 1
                    power() shouldBe 2
                    toughness() shouldBe 2
                }
            }

            test("can't target itself") {
                val builder = scenario()
                    .withPlayers("Halfdane Player", "Opponent")
                    .withCardOnBattlefield(1, "Halfdane")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                repeat(5) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()
                val halfdane = game.findPermanent("Halfdane")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                withClue("no other creature: the trigger has no legal target and Halfdane stays 3/3") {
                    game.state.projectedState.getPower(halfdane) shouldBe 3
                    game.state.floatingEffects.none { it.duration == Duration.EndOfYourNextUpkeep } shouldBe true
                }
            }
        }
    }
}
