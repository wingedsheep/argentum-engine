package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rising Populace (WAR #29) — {2}{W} Creature — Human 2/2.
 *
 *   Whenever another creature or planeswalker you control dies, put a +1/+1 counter on this creature.
 */
class RisingPopulaceScenarioTest : ScenarioTestBase() {

    private fun TestGame.populaceCounters(): Int =
        state.getEntity(findPermanent("Rising Populace")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Rising Populace")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Glory Seeker")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("another creature you control dying puts a +1/+1 counter on it") {
            val game = board()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.populaceCounters() shouldBe 1
        }

        test("an opponent's creature dying does not trigger it") {
            val game = board()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Glory Seeker")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Glory Seeker") shouldBe true
            game.populaceCounters() shouldBe 0
        }
    }
}
