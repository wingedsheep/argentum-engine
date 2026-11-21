package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ToilTroubleScenarioTest : ScenarioTestBase() {
    init {
        test("fused at the same opponent, Trouble counts the two cards Toil drew") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Toil // Trouble")
                .withLandsOnBattlefield(1, "Swamp", 3).withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(2, "Plains").withCardInHand(2, "Plains").withCardInHand(2, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Toil // Trouble"
            }

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Player(game.player2Id)),
                    castFused = true
                )
            ).error.shouldBeNull()
            game.resolveStack()

            // Opponent: 3 cards + 2 drawn = 5 in hand; 20 - 2 (Toil) - 5 (Trouble) = 13.
            game.state.getHand(game.player2Id).size shouldBe 5
            game.getLifeTotal(2) shouldBe 13
            game.isInGraveyard(1, "Toil // Trouble") shouldBe true
        }

        test("Trouble alone deals damage equal to the target player's hand size") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Toil // Trouble")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(2, "Plains").withCardInHand(2, "Plains")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Toil // Trouble"
            }

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    faceIndex = 1
                )
            ).error.shouldBeNull()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.state.getHand(game.player2Id).size shouldBe 2
        }
    }
}
