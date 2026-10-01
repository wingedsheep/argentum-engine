package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.CopperLonglegs
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Copper Longlegs (ONE #165) — {1}{G} 1/3 Phyrexian Spider.
 *
 *   Reach
 *   {1}{G}, Sacrifice this creature: Proliferate.
 *
 * Pins the sacrifice cost (Longlegs is in the graveyard before the ability resolves) and the
 * proliferate resolution, including choosing nothing.
 */
class CopperLonglegsScenarioTest : ScenarioTestBase() {

    private val abilityId = CopperLonglegs.activatedAbilities.single().id

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Copper Longlegs")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrifices itself and proliferates a chosen permanent's counters") {
            val game = board()
            val longlegs = game.findPermanent("Copper Longlegs")!!
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = longlegs, abilityId = abilityId)
            ).error shouldBe null
            game.isInGraveyard(1, "Copper Longlegs") shouldBe true

            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(giant) shouldBe 5
        }

        test("choosing nothing leaves counters unchanged") {
            val game = board()
            val longlegs = game.findPermanent("Copper Longlegs")!!
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = longlegs, abilityId = abilityId)
            ).error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.isInGraveyard(1, "Copper Longlegs") shouldBe true
        }
    }
}
