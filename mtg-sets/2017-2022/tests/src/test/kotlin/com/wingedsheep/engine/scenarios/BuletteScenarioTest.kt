package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bulette (AFR #173) — "At the beginning of your end step, if a creature died this turn, put a
 * +1/+1 counter on this creature."
 */
class BuletteScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Bulette")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.buletteCounters(): Int =
        state.getEntity(findPermanent("Bulette")!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("gets a +1/+1 counter at your end step when a creature died this turn") {
            val game = game()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.buletteCounters() shouldBe 1
            game.state.projectedState.getPower(game.findPermanent("Bulette")!!) shouldBe 4
        }

        test("no counter when no creature died this turn") {
            val game = game()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.buletteCounters() shouldBe 0
        }
    }
}
