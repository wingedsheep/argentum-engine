package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deal Gone Bad (SNC #74) — {3}{B} Instant
 *
 *   Target creature gets -3/-3 until end of turn. Target player mills three cards.
 *
 * Two independent targets: the creature and the milling player need not be related.
 */
class DealGoneBadScenarioTest : ScenarioTestBase() {

    init {
        context("Deal Gone Bad") {

            test("shrinks the creature and mills a different player three cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Deal Gone Bad")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val spell = game.findCardsInHand(1, "Deal Gone Bad").single()
                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player1Id))
                    )
                )
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the -3/-3 kills the 2/2") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("the targeted player (the caster) milled three") {
                    game.librarySize(1) shouldBe 1
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 3
                }
            }
        }
    }
}
