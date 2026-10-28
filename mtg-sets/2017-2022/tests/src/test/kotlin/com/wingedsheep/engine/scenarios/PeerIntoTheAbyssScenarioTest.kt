package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Peer into the Abyss (M21 #117) — {4}{B}{B}{B} Sorcery
 *
 *   Target player draws cards equal to half the number of cards in their library and loses half
 *   their life. Round up each time.
 *
 * Both amounts are read off the *targeted* player and rounded up (ruling 2020-06-23: 7 life → lose 4).
 */
class PeerIntoTheAbyssScenarioTest : ScenarioTestBase() {

    init {
        context("Peer into the Abyss") {

            test("the targeted opponent draws half their library and loses half their life, rounded up") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Peer into the Abyss")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .apply { repeat(4) { withCardInLibrary(1, "Forest") } }
                    .apply { repeat(5) { withCardInLibrary(2, "Island") } }
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Peer into the Abyss").single()
                val result = game.execute(
                    CastSpell(game.player1Id, spell, listOf(ChosenTarget.Player(game.player2Id)))
                )
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("five cards in library → draws three") {
                    game.handSize(2) shouldBe 3
                    game.librarySize(2) shouldBe 2
                }
                withClue("7 life → loses 4") { game.getLifeTotal(2) shouldBe 3 }
                withClue("the caster is untouched") {
                    game.getLifeTotal(1) shouldBe 20
                    game.librarySize(1) shouldBe 4
                }
            }
        }
    }
}
