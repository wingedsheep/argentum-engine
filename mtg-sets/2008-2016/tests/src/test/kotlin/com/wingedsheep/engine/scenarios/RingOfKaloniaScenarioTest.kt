package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ring of Kalonia (M13 #212) — {2} Artifact — Equipment.
 *
 * "Equipped creature has trample. At the beginning of your upkeep, put a +1/+1 counter on equipped
 * creature if it's green. Equip {1}"
 *
 * The colour check is made as the upkeep trigger resolves, against the equipped creature's colour:
 * a green host grows each upkeep, a non-green host only gets trample.
 */
class RingOfKaloniaScenarioTest : ScenarioTestBase() {

    private fun TestGame.passToAliceUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("a green equipped creature has trample and gets a +1/+1 counter at your upkeep") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Ring of Kalonia", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true

            game.passToAliceUpkeep()

            game.plusOneCounters("Grizzly Bears") shouldBe 1
        }

        test("a non-green equipped creature has trample but gets no counter") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardAttachedTo(1, "Ring of Kalonia", "Savannah Lions")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            game.state.projectedState.hasKeyword(lions, Keyword.TRAMPLE) shouldBe true

            game.passToAliceUpkeep()

            game.plusOneCounters("Savannah Lions") shouldBe 0
        }
    }
}
