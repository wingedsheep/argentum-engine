package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Pursued Whale (M21 #60).
 *
 *   When this creature enters, each opponent creates a 1/1 red Pirate creature token with "This
 *   token can't block" and "Creatures you control attack each combat if able."
 *
 * The Pirate's "you" is its controller — the opponent — so the attack requirement binds the
 * opponent's creatures (the Pirate included), never the Whale's controller's.
 */
class PursuedWhaleScenarioTest : ScenarioTestBase() {

    init {
        test("the opponent's Pirate forces the opponent's creatures to attack, not yours") {
            val game = scenario()
                .withPlayers("Whale", "Opponent")
                .withCardInHand(1, "Pursued Whale")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
                .build()

            game.castSpell(1, "Pursued Whale").error shouldBe null
            game.resolveStack()

            val pirate = game.findPermanents("Pirate Token").single()
            game.state.projectedState.getController(pirate) shouldBe game.player2Id

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("the Whale's controller is under no attack requirement") {
                game.declareAttackers(emptyMap()).error shouldBe null
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("it is the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }

            withClue("the Pirate must attack too") {
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldNotBe null
            }
            withClue("every creature the opponent controls attacks") {
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Pirate Token" to 1)).error shouldBe null
            }
        }
    }
}
