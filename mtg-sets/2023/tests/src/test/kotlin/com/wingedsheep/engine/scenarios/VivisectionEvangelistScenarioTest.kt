package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vivisection Evangelist (ONE #220) — {3}{W}{B} 4/4 Phyrexian Cleric, vigilance.
 *
 * "Corrupted — When this creature enters, if an opponent has three or more poison counters,
 *  destroy target creature or planeswalker an opponent controls."
 */
class VivisectionEvangelistScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(opponentPoison: Int, ownPoison: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Vivisection Evangelist")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Glory Seeker")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game
    }

    init {
        test("with corrupted, the enter trigger destroys a target creature an opponent controls") {
            val game = game(opponentPoison = 3)
            val giant = game.findPermanent("Hill Giant")!!
            val seeker = game.findPermanent("Glory Seeker")!!

            game.castSpell(1, "Vivisection Evangelist").error shouldBe null
            game.resolveStack()

            val evangelist = game.findPermanent("Vivisection Evangelist")!!
            game.state.projectedState.hasKeyword(evangelist, Keyword.VIGILANCE) shouldBe true

            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("only creatures/planeswalkers an opponent controls are legal") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(giant, seeker)
            }

            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Glory Seeker") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("without an opponent at three poison, the trigger doesn't fire") {
            val game = game(opponentPoison = 2, ownPoison = 5)

            game.castSpell(1, "Vivisection Evangelist").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Vivisection Evangelist") shouldBe true
            (game.state.pendingDecision == null) shouldBe true
            game.state.stack.isEmpty() shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Glory Seeker") shouldBe true
        }
    }
}
