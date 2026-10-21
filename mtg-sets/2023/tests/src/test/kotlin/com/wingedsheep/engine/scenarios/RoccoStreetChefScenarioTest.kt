package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rocco, Street Chef — "At the beginning of your end step, each player exiles the top card of
 * their library. Until your next end step, each player may play the card they exiled this way.
 * Whenever a player plays a land from exile or casts a spell from exile, you put a +1/+1 counter
 * on target creature and create a Food token."
 */
class RoccoStreetChefScenarioTest : ScenarioTestBase() {

    private fun TestGame.rocco(): EntityId = findPermanent("Rocco, Street Chef")!!

    private fun TestGame.roccoCounters(): Int =
        state.getEntity(rocco())?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.exiled(playerId: EntityId, name: String): EntityId =
        state.getExile(playerId).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.mayPlay(playerId: EntityId, cardId: EntityId): Boolean =
        state.mayPlayPermissions.any { it.controllerId == playerId && cardId in it.cardIds }

    /** Rocco's exile-play trigger targets a creature; aim it at Rocco. */
    private fun TestGame.resolveTargetingRocco() {
        if (state.pendingDecision != null) selectTargets(listOf(rocco())).error shouldBe null
        resolveStack()
    }

    private fun TestGame.passUntilTurnOf(playerId: EntityId) {
        var guard = 0
        do {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            if (state.activePlayerId != playerId) passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        } while (state.activePlayerId != playerId && guard++ < 4)
    }

    private fun roccoGame(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Rocco, Street Chef")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Llanowar Elves")
        .withCardInLibrary(2, "Mountain")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        .build()

    init {
        test("each player exiles their top card and may play it — an opponent's land play feeds Rocco") {
            val g = roccoGame()
            g.passUntilPhase(Phase.ENDING, Step.END)
            g.resolveStack()

            val bears = g.exiled(g.player1Id, "Grizzly Bears")
            val mountain = g.exiled(g.player2Id, "Mountain")
            withClue("each player may play only the card they exiled") {
                g.mayPlay(g.player1Id, bears) shouldBe true
                g.mayPlay(g.player2Id, mountain) shouldBe true
                g.mayPlay(g.player1Id, mountain) shouldBe false
            }

            g.passUntilTurnOf(g.player2Id)
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            g.execute(PlayLand(g.player2Id, mountain)).error shouldBe null
            g.resolveTargetingRocco()

            g.isOnBattlefield("Mountain") shouldBe true
            withClue("Rocco's controller — not the player who played the land — gets the counter and the Food") {
                g.roccoCounters() shouldBe 1
                g.findPermanents("Food").single().let { g.state.projectedState.getController(it) } shouldBe g.player1Id
            }
        }

        test("casting your exiled card triggers too, and the window closes as your next end step begins") {
            val g = roccoGame()
            g.passUntilPhase(Phase.ENDING, Step.END)
            g.resolveStack()
            val bears = g.exiled(g.player1Id, "Grizzly Bears")
            val mountain = g.exiled(g.player2Id, "Mountain")

            g.passUntilTurnOf(g.player1Id)
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            g.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            g.resolveTargetingRocco()
            g.resolveStack()
            g.isOnBattlefield("Grizzly Bears") shouldBe true
            g.roccoCounters() shouldBe 1
            g.findPermanents("Food").size shouldBe 1

            g.passUntilPhase(Phase.ENDING, Step.END)
            withClue("the opponent's unplayed Mountain stopped being playable as Rocco's controller's end step began") {
                g.mayPlay(g.player2Id, mountain) shouldBe false
            }
            g.resolveStack()
            withClue("and the new end-step trigger handed out a fresh round") {
                g.mayPlay(g.player1Id, g.exiled(g.player1Id, "Island")) shouldBe true
                g.mayPlay(g.player2Id, g.exiled(g.player2Id, "Island")) shouldBe true
            }
            g.state.getExile(g.player2Id).contains(mountain) shouldBe true
            g.mayPlay(g.player1Id, bears) shouldBe false
        }
    }
}
