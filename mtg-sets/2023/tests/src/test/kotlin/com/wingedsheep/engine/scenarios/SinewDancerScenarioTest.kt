package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.SinewDancer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sinew Dancer (ONE #32) — {W} 1/1 Phyrexian Soldier.
 *
 * "{3}{W}, {T}: Tap target creature.
 *  Corrupted — {W}, {T}: Tap target creature. Activate only if an opponent has three or more
 *  poison counters."
 */
class SinewDancerScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(plains: Int, opponentPoison: Int = 0, ownPoison: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Sinew Dancer")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", plains)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game
    }

    private fun TestGame.activate(index: Int, target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Sinew Dancer")!!,
            abilityId = SinewDancer.activatedAbilities[index].id,
            targets = listOf(ChosenTarget.Permanent(target)),
        )
    )

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)!!.has<TappedComponent>()

    init {
        test("{3}{W}, {T} taps target creature") {
            val game = game(plains = 4)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activate(0, bears).error shouldBe null
            game.resolveStack()

            game.isTapped(bears) shouldBe true
            game.isTapped(game.findPermanent("Sinew Dancer")!!) shouldBe true
        }

        test("corrupted {W}, {T} taps target creature when an opponent has three poison") {
            val game = game(plains = 1, opponentPoison = 3)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activate(1, bears).error shouldBe null
            game.resolveStack()

            game.isTapped(bears) shouldBe true
        }

        test("the corrupted ability can't be activated below three opponent poison") {
            val game = game(plains = 1, opponentPoison = 2, ownPoison = 5)
            game.activate(1, game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
