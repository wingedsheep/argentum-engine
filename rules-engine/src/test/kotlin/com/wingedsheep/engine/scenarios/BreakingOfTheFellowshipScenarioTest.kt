package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Breaking of the Fellowship — "Target creature an opponent controls deals damage equal to
 * its power to another target creature that player controls. The Ring tempts you."
 *
 * The first target (a 3/3) deals 3 damage to the second (a 2/2), killing it.
 */
class BreakingOfTheFellowshipScenarioTest : ScenarioTestBase() {

    init {
        test("first targeted creature deals its power to the second") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Breaking of the Fellowship")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Hill Giant")    // 3/3 — the damage dealer
                .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 — the recipient
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            // Target order matters: [0] is the dealer, [1] the recipient.
            val card = game.findCardsInHand(1, "Breaking of the Fellowship").first()
            game.execute(
                CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            // The 3/3 dealt 3 to the 2/2, which died; the dealer survives.
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanent("Hill Giant") shouldBe giant
        }
    }
}
