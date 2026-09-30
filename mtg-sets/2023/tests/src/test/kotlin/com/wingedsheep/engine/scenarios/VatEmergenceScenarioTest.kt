package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vat Emergence (ONE #112) — {4}{B} Sorcery.
 *
 *   Put target creature card from a graveyard onto the battlefield under your control. Proliferate.
 */
class VatEmergenceScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("returns an opponent's creature card under your control, then proliferates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Vat Emergence")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            val spell = game.state.getHand(game.player1Id).single {
                game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Vat Emergence"
            }

            val cast = game.execute(
                CastSpell(game.player1Id, spell, listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD)))
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }

            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) game.selectCards(listOf(giant)) else game.resolveStack()
            }

            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.getBattlefield().contains(bears) shouldBe true
            counters(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.isInGraveyard(1, "Vat Emergence") shouldBe true
        }
    }
}
