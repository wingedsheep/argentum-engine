package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.MerfolkSkydiver
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Merfolk Skydiver (WAR #205) — {G}{U} Creature — Merfolk Mutant 1/1, flying.
 *
 *   When this creature enters, put a +1/+1 counter on target creature you control.
 *   {3}{G}{U}: Proliferate.
 */
class MerfolkSkydiverScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering counters a creature you control, then the ability proliferates it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Merfolk Skydiver")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Merfolk Skydiver").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            plusOnes(game, bears) shouldBe 1

            val skydiver = game.findPermanent("Merfolk Skydiver")!!
            game.execute(
                ActivateAbility(game.player1Id, skydiver, MerfolkSkydiver.activatedAbilities[0].id)
            ).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            plusOnes(game, bears) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 4
        }
    }
}
