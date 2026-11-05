package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Compelling Argument (AKH #47) — {1}{U} Sorcery.
 *
 *   Target player mills five cards.
 *   Cycling {U}
 */
class CompellingArgumentScenarioTest : ScenarioTestBase() {

    init {
        context("Compelling Argument") {

            test("the target player mills five cards") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Compelling Argument")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(7) { builder.withCardInLibrary(2, "Forest") }
                repeat(3) { builder.withCardInLibrary(1, "Island") }
                val game = builder.build()

                val spell = game.findCardsInHand(1, "Compelling Argument").first()
                game.execute(CastSpell(game.player1Id, spell, listOf(ChosenTarget.Player(game.player2Id))))
                    .error shouldBe null
                game.resolveStack()

                withClue("the opponent milled five") {
                    game.librarySize(2) shouldBe 2
                    game.findCardsInGraveyard(2, "Forest").size shouldBe 5
                }
                withClue("the caster's library is untouched") { game.librarySize(1) shouldBe 3 }
            }
        }
    }
}
