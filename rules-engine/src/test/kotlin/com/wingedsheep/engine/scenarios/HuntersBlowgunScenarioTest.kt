package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hunter's Blowgun (LCI #255) — {1} Artifact — Equipment (common).
 *
 * Equipped creature gets +1/+1.
 * Equipped creature has deathtouch during your turn. Otherwise, it has reach.
 * Equip {2}
 *
 * Tests:
 *  1. Equipped creature always gets +1/+1, regardless of whose turn it is.
 *  2. Equipped creature has deathtouch on the controller's turn (IsYourTurn gate active).
 *  3. Equipped creature does NOT have deathtouch on the opponent's turn.
 *  4. Equipped creature has reach on the opponent's turn (IsNotYourTurn gate active).
 *  5. Equipped creature does NOT have reach on the controller's turn.
 *  6. Unequipped creature gains neither keyword and no stats bonus.
 */
class HuntersBlowgunScenarioTest : ScenarioTestBase() {

    init {

        context("Hunter's Blowgun — +1/+1 is always active") {

            test("equipped creature gets +1/+1 during controller's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Hunter's Blowgun", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Grizzly Bears base 2/2 + Blowgun +1/+1 = 3/3 on your turn") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
            }

            test("equipped creature gets +1/+1 during opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Hunter's Blowgun", "Grizzly Bears")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Grizzly Bears base 2/2 + Blowgun +1/+1 = 3/3 on opponent's turn too") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
            }
        }

        context("Hunter's Blowgun — deathtouch during your turn, reach otherwise") {

            test("equipped creature has deathtouch during controller's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Hunter's Blowgun", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Equipped creature should have deathtouch on controller's turn") {
                    projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
                }
                withClue("Equipped creature should NOT have reach on controller's turn") {
                    projected.hasKeyword(bears, Keyword.REACH) shouldBe false
                }
            }

            test("equipped creature has reach during opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Hunter's Blowgun", "Grizzly Bears")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Equipped creature should have reach on opponent's turn") {
                    projected.hasKeyword(bears, Keyword.REACH) shouldBe true
                }
                withClue("Equipped creature should NOT have deathtouch on opponent's turn") {
                    projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
                }
            }
        }

        context("Hunter's Blowgun — unequipped creature gets no bonus") {

            test("creature without blowgun attached has no bonus during your turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hunter's Blowgun") // not attached
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Grizzly Bears should stay 2/2 without the Blowgun equipped") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                }
                withClue("Unequipped creature should not have deathtouch") {
                    projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
                }
                withClue("Unequipped creature should not have reach") {
                    projected.hasKeyword(bears, Keyword.REACH) shouldBe false
                }
            }
        }
    }
}
