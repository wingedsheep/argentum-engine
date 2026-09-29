package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Merciless Repurposing (MOM #117) — "Exile target creature. Incubate 3."
 */
class MercilessRepurposingScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Merciless Repurposing") {

            test("exiles the target creature and incubates 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Merciless Repurposing")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Merciless Repurposing", bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears exiled") { game.isInExile(2, "Grizzly Bears") shouldBe true }
                val incubator = game.findPermanent("Incubator")
                withClue("an Incubator with three +1/+1 counters") {
                    incubator shouldNotBe null
                    game.plusOneCounters(incubator!!) shouldBe 3
                }
            }

            test("cannot target a noncreature permanent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Merciless Repurposing")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(2, "Millstone")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val millstone = game.findPermanent("Millstone")!!

                game.castSpell(1, "Merciless Repurposing", millstone).error shouldNotBe null
            }
        }
    }
}
