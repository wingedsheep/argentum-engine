package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.ChitteringSkitterling
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chittering Skitterling (ONE #87) — {2}{B} 1/4 Phyrexian Rat.
 *
 * "Corrupted — Sacrifice an artifact or creature: Draw a card. Activate only if an opponent has
 *  three or more poison counters and only once each turn."
 */
class ChitteringSkitterlingScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(opponentPoison: Int, ownPoison: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Chittering Skitterling")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game
    }

    private fun TestGame.activate(fodder: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Chittering Skitterling")!!,
            abilityId = ChitteringSkitterling.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
        )
    )

    init {
        test("with corrupted, sacrificing a creature draws a card") {
            val game = game(opponentPoison = 3)
            val handBefore = game.state.getHand(game.player1Id).size
            val fodder = game.findPermanents("Grizzly Bears").first()

            game.activate(fodder).error shouldBe null
            game.resolveStack()

            game.state.getHand(game.player1Id).size shouldBe handBefore + 1
            game.state.getBattlefield().contains(fodder) shouldBe false
        }

        test("only once each turn") {
            val game = game(opponentPoison = 3)
            game.activate(game.findPermanents("Grizzly Bears").first()).error shouldBe null
            game.resolveStack()

            game.activate(game.findPermanents("Grizzly Bears").single()).error shouldNotBe null
        }

        test("can't be activated without an opponent at three poison") {
            val game = game(opponentPoison = 2, ownPoison = 5)
            game.activate(game.findPermanents("Grizzly Bears").first()).error shouldNotBe null
        }
    }
}
