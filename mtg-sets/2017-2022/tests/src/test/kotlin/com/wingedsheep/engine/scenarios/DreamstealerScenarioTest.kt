package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dreamstealer (HOU #63) — {2}{B} Creature — Human Wizard, 1/2.
 *
 * "Whenever this creature deals combat damage to a player, that player discards that many cards."
 *
 * The discard count is the damage on the triggering event, read when the trigger resolves. With a
 * Glorious Anthem out the 1/2 hits for 2, so the defender discards two — a script that discarded a
 * fixed one card (the Specter shape) would leave two cards in hand.
 */
class DreamstealerScenarioTest : ScenarioTestBase() {

    init {
        context("Dreamstealer") {

            test("the damaged player discards as many cards as the damage dealt") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dreamstealer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardsInHand(2, "Grizzly Bears", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Dreamstealer" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("2 combat damage means 2 discards, leaving 1 of 3 cards") {
                    game.getLifeTotal(2) shouldBe 18
                    game.handSize(2) shouldBe 1
                }
            }
        }
    }
}
