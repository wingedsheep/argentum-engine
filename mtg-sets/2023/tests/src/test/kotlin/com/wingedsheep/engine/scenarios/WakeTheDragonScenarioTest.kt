package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wake the Dragon (LTC #74) — "Create a 6/6 black and red Dragon creature token with flying,
 * menace, and 'Whenever this token deals combat damage to a player, gain control of target
 * artifact that player controls.' Flashback {6}{B}{R}"
 *
 * The token's trigger carries its own target — an artifact the *damaged player* controls — and the
 * control change is permanent, so it must still hold on later turns.
 */
class WakeTheDragonScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Wake the Dragon")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardOnBattlefield(2, "Sol Ring")
            .withCardOnBattlefield(1, "Icy Manipulator") // our own artifact is never a legal target
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) { builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island") }
        return builder.build()
    }

    private fun TestGame.dragon() = findPermanent("Dragon Token") ?: error("no Dragon token on the battlefield")

    init {
        context("Wake the Dragon") {
            test("creates a 6/6 black and red flying, menace Dragon") {
                val game = setup()

                game.castSpell(1, "Wake the Dragon").error shouldBe null
                game.resolveStack()

                val dragon = game.dragon()
                val projected = game.state.projectedState
                projected.getPower(dragon) shouldBe 6
                projected.getToughness(dragon) shouldBe 6
                projected.hasKeyword(dragon, Keyword.FLYING) shouldBe true
                projected.hasKeyword(dragon, Keyword.MENACE) shouldBe true
                projected.hasSubtype(dragon, "Dragon") shouldBe true
                projected.getColors(dragon) shouldBe setOf(Color.BLACK.name, Color.RED.name)
            }

            test("when the Dragon hits a player it takes an artifact that player controls, for good") {
                val game = setup()
                val solRing = game.findPermanent("Sol Ring")!!
                game.castSpell(1, "Wake the Dragon").error shouldBe null
                game.resolveStack()

                // The token can't attack the turn it arrives: play through to our next turn.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("it is our turn again") { game.state.activePlayerId shouldBe game.player1Id }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Dragon Token" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                val decision = game.getPendingDecision()
                if (decision is ChooseTargetsDecision) {
                    withClue("only the damaged player's artifact is a legal target") {
                        decision.legalTargets.getValue(0) shouldBe listOf(solRing)
                    }
                    game.selectTargets(listOf(solRing)).error shouldBe null
                }
                game.resolveStack()

                game.state.projectedState.getController(solRing) shouldBe game.player1Id
                game.getLifeTotal(2) shouldBe 14

                // Permanent: still ours after the turn ends and the opponent's turn begins.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.projectedState.getController(solRing) shouldBe game.player1Id
            }
        }
    }
}
