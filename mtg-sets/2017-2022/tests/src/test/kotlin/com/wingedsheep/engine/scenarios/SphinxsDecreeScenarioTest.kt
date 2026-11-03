package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sphinx's Decree (RIX #24) — "Each opponent can't cast instant or sorcery spells during that
 * player's next turn."
 *
 * Exercises `Duration.DuringAffectedPlayersNextTurn` on a cast ban: dormant for the rest of the
 * caster's turn, binding (instants and sorceries only) through the opponent's next turn, and gone
 * once that turn ends.
 */
class SphinxsDecreeScenarioTest : ScenarioTestBase() {

    init {
        test("the ban waits for the opponent's next turn, covers only it, and only instants and sorceries") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sphinx's Decree")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardsInHand(2, "Lightning Bolt", 3)
                .withCardInHand(2, "Divination")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withLandsOnBattlefield(2, "Island", 3)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sphinx's Decree").error shouldBe null
            game.resolveStack()

            withClue("the rest of the caster's turn is not the opponent's next turn") {
                game.passPriority().error shouldBe null
                game.state.priorityPlayerId shouldBe game.player2Id
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 17
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id

            withClue("during the opponent's next turn instants and sorceries can't be cast") {
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
                game.castSpell(2, "Divination").error shouldNotBe null
            }
            withClue("other spells are untouched") {
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id

            withClue("the ban ended with that turn") {
                game.passPriority().error shouldBe null
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 14
            }
        }
    }
}
