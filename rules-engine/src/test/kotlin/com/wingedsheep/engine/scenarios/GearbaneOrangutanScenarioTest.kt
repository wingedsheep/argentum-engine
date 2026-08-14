package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gearbane Orangutan (MKM #129) — {2}{R} Creature — Ape, 2/2, Reach.
 *
 * "When this creature enters, choose one —
 *  • Destroy up to one target artifact.
 *  • Sacrifice an artifact. If you do, put two +1/+1 counters on this creature."
 *
 * The interesting half is mode 1: the sacrifice is mandatory, not a "may", and the counters are
 * gated on it actually happening (`SuccessCriterion.PermanentsSacrificed`). With no artifact to
 * sacrifice the mode must be a legal choice that simply accomplishes nothing — `Always` would
 * hand out two free counters there, which is what these tests pin down.
 */
class GearbaneOrangutanScenarioTest : ScenarioTestBase() {

    init {
        context("Gearbane Orangutan") {

            fun castAndReachModeChoice(game: TestGame): ChooseOptionDecision {
                val cast = game.castSpell(1, "Gearbane Orangutan")
                withClue("Gearbane Orangutan should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                return game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
            }

            test("mode 0 destroys the targeted artifact") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Gearbane Orangutan")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val millstone = game.findPermanent("Millstone")!!

                val mode = castAndReachModeChoice(game)
                game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 0))

                val targets = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the destroy mode; got ${game.getPendingDecision()}")
                game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(millstone))))
                game.resolveStack()

                withClue("the opponent's artifact is destroyed") {
                    game.isOnBattlefield("Millstone") shouldBe false
                }
                withClue("no counters — that's the other mode") {
                    val ape = game.findPermanent("Gearbane Orangutan")!!
                    game.state.projectedState.getPower(ape) shouldBe 2
                    game.state.projectedState.getToughness(ape) shouldBe 2
                }
            }

            test("mode 1 sacrifices your artifact and grows the Ape to 4/4") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Gearbane Orangutan")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mode = castAndReachModeChoice(game)
                game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 1))
                game.resolveStack()

                withClue("the only legal artifact is sacrificed") {
                    game.isOnBattlefield("Millstone") shouldBe false
                    game.isInGraveyard(1, "Millstone") shouldBe true
                }
                withClue("two +1/+1 counters on a 2/2") {
                    val ape = game.findPermanent("Gearbane Orangutan")!!
                    game.state.projectedState.getPower(ape) shouldBe 4
                    game.state.projectedState.getToughness(ape) shouldBe 4
                }
            }

            test("mode 1 with no artifact sacrifices nothing and gives no counters") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Gearbane Orangutan")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mode = castAndReachModeChoice(game)
                game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 1))
                game.resolveStack()

                withClue("nothing was sacrificed, so the payoff must not fire") {
                    val ape = game.findPermanent("Gearbane Orangutan")!!
                    game.state.projectedState.getPower(ape) shouldBe 2
                    game.state.projectedState.getToughness(ape) shouldBe 2
                }
            }

            test("mode 0 targets nothing when there is no artifact — the Ape still enters") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Gearbane Orangutan")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mode = castAndReachModeChoice(game)
                game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 0))
                if (game.getPendingDecision() is ChooseTargetsDecision) game.skipTargets()
                game.resolveStack()

                withClue("\"up to one\" tolerates zero targets") {
                    game.isOnBattlefield("Gearbane Orangutan") shouldBe true
                    val ape = game.findPermanent("Gearbane Orangutan")!!
                    game.state.projectedState.getPower(ape) shouldBe 2
                }
            }
        }
    }
}
