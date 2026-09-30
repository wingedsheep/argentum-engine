package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fleshless Gladiator (ONE #94) — Corrupted graveyard recursion:
 * "{2}{B}: Return this card from your graveyard to the battlefield tapped. You lose 1 life.
 *  Activate only if an opponent has three or more poison counters."
 */
class FleshlessGladiatorScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Fleshless Gladiator")!!.activatedAbilities.first().id

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(opponentPoison: Int, ownPoison: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInGraveyard(1, "Fleshless Gladiator")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game
    }

    private fun TestGame.activate() = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findCardsInGraveyard(1, "Fleshless Gladiator").single(),
            abilityId = abilityId,
        )
    )

    init {
        test("corrupted: returns tapped from the graveyard and its controller loses 1 life") {
            val game = game(opponentPoison = 3)
            val result = game.activate()
            withClue("activation: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val gladiator = game.findPermanent("Fleshless Gladiator")
            gladiator shouldNotBe null
            game.state.getEntity(gladiator!!)?.has<TappedComponent>() shouldBe true
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }

        test("not corrupted: the ability can't be activated") {
            val game = game(opponentPoison = 2)
            game.activate().error shouldNotBe null
            game.isInGraveyard(1, "Fleshless Gladiator") shouldBe true
        }

        test("the controller's own poison counters don't enable it") {
            val game = game(opponentPoison = 0, ownPoison = 5)
            game.activate().error shouldNotBe null
        }
    }
}
