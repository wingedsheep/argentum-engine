package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for The Fall of Lord Konda (NEO #12).
 *
 *   I — Exile target creature an opponent controls with mana value 4 or greater.
 *   II — Each player gains control of all permanents they own.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Chapter II is the layer-2 timestamp case: the control it hands back is a new effect, so it must
 * beat an older Confiscate's static control ability (CR 613.7a — a static ability's effect has
 * its permanent's timestamp, not "now").
 */
class TheFallOfLordKondaScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "The Fall of Lord Konda")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(6) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Island") } }

        test("chapter I exiles an opponent's creature with mana value 4 or greater") {
            val game = builder()
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "The Fall of Lord Konda").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Hill Giant") shouldBe null
            withClue("a mana value 2 creature isn't a legal target") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }

        test("chapter II takes back a permanent stolen by an older Confiscate") {
            val game = builder()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Confiscate")
                .withLandsOnBattlefield(2, "Island", 6)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "The Fall of Lord Konda").error shouldBe null
            game.resolveStack()

            // Opponent's turn: Confiscate steals the Bears.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.castSpell(2, "Confiscate", bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getController(bears) shouldBe game.player2Id

            // My next turn: chapter II after the draw step.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            withClue("the chapter's control effect is newer than Confiscate's static ability") {
                game.state.projectedState.getController(bears) shouldBe game.player1Id
            }
            val confiscate = game.findPermanent("Confiscate")!!
            game.state.projectedState.getController(confiscate) shouldBe game.player2Id
        }
    }
}
