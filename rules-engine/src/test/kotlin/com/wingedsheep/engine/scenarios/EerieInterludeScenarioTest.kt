package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eerie Interlude (DDQ #8) — exile any number of target creatures you control, return them at the
 * beginning of the next end step.
 */
class EerieInterludeScenarioTest : ScenarioTestBase() {
    init {
        test("exiles a creature and returns it at the beginning of the next end step") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Eerie Interlude")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Eerie Interlude", bears).error shouldBe null
            game.resolveStack()

            withClue("the creature is exiled on resolution") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            // Walk real game flow so the delayed trigger actually fires.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the delayed trigger returns it at the beginning of the next end step") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("returns every exiled creature when several are targeted") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardInHand(1, "Eerie Interlude")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.findCardsInHand(1, "Eerie Interlude").single()

            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = spell,
                    targets = listOf(bears, seeker, giant).map { entityIdToChosenTarget(game.state, it) },
                ),
            )
            withClue("casting with three targets: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("all three are exiled — ForEachTarget rebinds ContextTarget(0) per target") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Glory Seeker") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("each gets its own delayed return, not just the first target") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Glory Seeker") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
