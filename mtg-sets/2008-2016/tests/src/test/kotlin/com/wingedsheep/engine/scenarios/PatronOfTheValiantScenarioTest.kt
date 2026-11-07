package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Patron of the Valiant (Magic Origins #28) — {3}{W}{W} Creature — Angel 4/4, Flying.
 * "When this creature enters, put a +1/+1 counter on each creature you control with a +1/+1 counter on it."
 */
class PatronOfTheValiantScenarioTest : ScenarioTestBase() {
    init {
        test("enters: only your creatures that already have a +1/+1 counter get another") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Patron of the Valiant")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Child of Night")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val child = game.findPermanent("Child of Night")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            fun plusOnes(id: EntityId) =
                game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            for (id in listOf(bears, wurm)) {
                game.state = game.state.updateEntity(id) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }
            }

            game.castSpell(1, "Patron of the Valiant").error shouldBe null
            game.resolveStack()

            plusOnes(bears) shouldBe 2
            withClue("a creature without a counter gets none") { plusOnes(child) shouldBe 0 }
            withClue("the opponent's creature isn't yours") { plusOnes(wurm) shouldBe 1 }
            withClue("the Patron enters without a counter") {
                plusOnes(game.findPermanent("Patron of the Valiant")!!) shouldBe 0
            }
        }
    }
}
