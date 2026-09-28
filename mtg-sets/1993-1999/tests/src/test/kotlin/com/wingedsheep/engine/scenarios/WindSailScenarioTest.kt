package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class WindSailScenarioTest : ScenarioTestBase() {
    init {
        test("two target creatures gain flying") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Wind Sail")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Wind Sail").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(
                        entityIdToChosenTarget(game.state, bears),
                        entityIdToChosenTarget(game.state, giant),
                    ),
                )
            )

            result.error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe true
        }

        test("a single target creature gains flying") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Wind Sail")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Wind Sail", bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe false
        }
    }
}
