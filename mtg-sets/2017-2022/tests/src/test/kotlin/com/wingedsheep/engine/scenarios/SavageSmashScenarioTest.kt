package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.shouldBe

/**
 * Savage Smash (RNA #203) — +2/+2 to your creature, then it fights a creature you don't control.
 */
class SavageSmashScenarioTest : ScenarioTestBase() {
    init {
        test("the pumped creature survives a fight it would otherwise lose") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Savage Smash")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val smash = game.findCardsInHand(1, "Savage Smash").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(
                CastSpell(game.player1Id, smash, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
