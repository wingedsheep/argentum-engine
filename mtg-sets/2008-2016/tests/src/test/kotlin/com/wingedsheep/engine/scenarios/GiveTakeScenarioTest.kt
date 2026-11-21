package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class GiveTakeScenarioTest : ScenarioTestBase() {
    init {
        test("fused onto the same creature, Take removes Give's three counters and draws three") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Give // Take")
                .withLandsOnBattlefield(1, "Forest", 3).withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Give // Take"
            }
            val bears = game.findPermanent("Grizzly Bears")!!
            val handBefore = game.state.getHand(game.player1Id).size

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(bears)),
                    castFused = true
                )
            ).error.shouldBeNull()
            game.resolveStack()

            (game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            // The spell left the hand (-1) and Take drew three.
            game.state.getHand(game.player1Id).size shouldBe handBefore - 1 + 3
            game.isInGraveyard(1, "Give // Take") shouldBe true
        }

        test("Take alone on a creature without counters draws nothing") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Give // Take")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Give // Take"
            }
            val handBefore = game.state.getHand(game.player1Id).size

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)),
                    faceIndex = 1
                )
            ).error.shouldBeNull()
            game.resolveStack()

            game.state.getHand(game.player1Id).size shouldBe handBefore - 1
        }
    }
}
