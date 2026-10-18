package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eiganjo Uprising (NEO #217) — {X}{R}{W} Sorcery.
 *
 *   Create X 2/2 white Samurai creature tokens with vigilance. They gain menace and haste until
 *   end of turn.
 *   Each opponent creates X minus one 2/2 white Samurai creature tokens with vigilance.
 *
 * What a reader can't confirm from the script: the opponents' count is X − 1 (and nothing at
 * X = 0), the fan-out puts those tokens under the opponent's control, and the menace/haste grant
 * reaches only the caster's tokens.
 */
class EiganjoUprisingScenarioTest : ScenarioTestBase() {

    private fun game(extraMountains: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Eiganjo Uprising")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Mountain", 1 + extraMountains)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Eiganjo Uprising") {

            test("X = 3: three hasty, menacing Samurai for you; two plain Samurai for the opponent") {
                val game = game(extraMountains = 3)
                withClue("cast with X = 3") { game.castXSpell(1, "Eiganjo Uprising", 3).error shouldBe null }
                game.resolveStack()

                val samurai = game.findPermanents("Samurai Token")
                val projected = game.state.projectedState
                val mine = samurai.filter { projected.getController(it) == game.player1Id }
                val theirs = samurai.filter { projected.getController(it) == game.player2Id }

                withClue("caster gets X tokens") { mine.size shouldBe 3 }
                withClue("opponent gets X minus one tokens") { theirs.size shouldBe 2 }
                withClue("every Samurai has printed vigilance") {
                    samurai.all { projected.hasKeyword(it, Keyword.VIGILANCE) } shouldBe true
                }
                withClue("'They' are the caster's tokens: menace and haste") {
                    mine.all {
                        projected.hasKeyword(it, Keyword.MENACE) && projected.hasKeyword(it, Keyword.HASTE)
                    } shouldBe true
                }
                withClue("the opponent's tokens get neither") {
                    theirs.none {
                        projected.hasKeyword(it, Keyword.MENACE) || projected.hasKeyword(it, Keyword.HASTE)
                    } shouldBe true
                }
            }

            test("X = 1: one Samurai for you, none for the opponent") {
                val game = game(extraMountains = 1)
                game.castXSpell(1, "Eiganjo Uprising", 1).error shouldBe null
                game.resolveStack()

                val samurai = game.findPermanents("Samurai Token")
                samurai.size shouldBe 1
                game.state.projectedState.getController(samurai.single()) shouldBe game.player1Id
            }

            test("X = 0: nobody gets a token") {
                val game = game(extraMountains = 0)
                game.castXSpell(1, "Eiganjo Uprising", 0).error shouldBe null
                game.resolveStack()

                game.findPermanents("Samurai Token").size shouldBe 0
                game.isInGraveyard(1, "Eiganjo Uprising") shouldBe true
            }
        }
    }
}
