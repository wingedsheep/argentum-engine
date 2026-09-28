package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class JaggedLightningScenarioTest : ScenarioTestBase() {
    init {
        test("deals 3 damage to each of two target creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Jagged Lightning")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Jagged Lightning").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(
                        entityIdToChosenTarget(game.state, bears),
                        entityIdToChosenTarget(game.state, giant),
                    ),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Glory Seeker") shouldBe true
        }
    }
}
