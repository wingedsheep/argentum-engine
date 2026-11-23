package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gruul Charm (GTC #169) — {R}{G} choose one: creatures without flying can't block this turn; gain
 * control of all permanents you own; or 3 damage to each creature with flying.
 *
 * The second mode is the one whose correctness depends on the engine: it must find the caster's own
 * permanents on the opponent's side by *owner*, and its newer control effect must override the
 * opponent's earlier one (Control Magic).
 */
class GruulCharmScenarioTest : ScenarioTestBase() {
    init {
        test("second mode takes back permanents you own and leaves the opponent's own permanents alone") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Gruul Charm")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(2, "Control Magic", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val controlMagic = game.findPermanent("Control Magic")!!
            withClue("Control Magic gives the opponent the Bears before the charm") {
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }

            val spell = game.findCardsInHand(1, "Gruul Charm").single()
            val result = game.execute(CastSpell(game.player1Id, spell, chosenModes = listOf(1)))
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the caster regains the Bears despite the opponent's Control Magic") {
                projected.getController(bears) shouldBe game.player1Id
            }
            withClue("the opponent's own permanents stay theirs") {
                projected.getController(giant) shouldBe game.player2Id
                projected.getController(controlMagic) shouldBe game.player2Id
            }
        }
    }
}
