package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kaito Shizuki (NEO #226, {1}{U}{B} planeswalker, loyalty 3).
 *
 *   At the beginning of your end step, if Kaito entered this turn, he phases out.
 *
 * Covers the intervening-if end-step phase-out on a planeswalker: a Kaito cast this turn phases
 * out at the end step and phases back in at its controller's next untap step; a Kaito that was
 * already on the battlefield at the start of the turn stays put.
 */
class KaitoShizukiScenarioTest : ScenarioTestBase() {

    private val kaito = "Kaito Shizuki"

    init {
        test("Kaito cast this turn phases out at your end step and phases in at your next untap") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, kaito)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()

            game.castSpell(1, kaito).error shouldBe null
            game.resolveStack()
            val kaitoId = game.findPermanent(kaito).shouldNotBeNull()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("Kaito entered this turn, so he phases out at the end step") {
                (kaitoId in game.state.getBattlefield()) shouldBe false
                game.state.getEntity(kaitoId).shouldNotBeNull()
            }

            // Through the opponent's turn he stays phased out; he phases in at Player1's untap.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("still phased out during the opponent's turn") {
                game.state.activePlayerId shouldBe game.player2Id
                (kaitoId in game.state.getBattlefield()) shouldBe false
            }
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("Kaito phased back in at his controller's untap step") {
                game.state.activePlayerId shouldBe game.player1Id
                (kaitoId in game.state.getBattlefield()) shouldBe true
            }

            // Phasing in isn't entering: no second phase-out this turn.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            withClue("Kaito didn't enter this turn, so he stays on the battlefield") {
                (kaitoId in game.state.getBattlefield()) shouldBe true
            }
        }

        test("Kaito that was already on the battlefield doesn't phase out") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, kaito)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()
            val kaitoId = game.findPermanent(kaito).shouldNotBeNull()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            (kaitoId in game.state.getBattlefield()) shouldBe true
        }
    }
}
