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
 * Scale Blessing (Dragons of Tarkir #35) — {3}{W} Instant.
 * "Bolster 1, then put a +1/+1 counter on each creature you control with a +1/+1 counter on it."
 *
 * Pins the ordering: the group is read after the bolster resolves, so the bolstered creature —
 * counterless before the spell — ends with two counters.
 */
class ScaleBlessingScenarioTest : ScenarioTestBase() {
    init {
        test("bolstered creature gets a second counter; other countered creatures you control get one") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scale Blessing")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val angel = game.findPermanent("Serra Angel")!!
            fun plusOnes(id: EntityId) =
                game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            for (id in listOf(wurm, angel)) {
                game.state = game.state.updateEntity(id) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }
            }

            game.castSpell(1, "Scale Blessing").error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.selectCards(listOf(bears))
                game.resolveStack()
            }

            withClue("Grizzly Bears (least toughness) is bolstered, then counted in the group") {
                plusOnes(bears) shouldBe 2
            }
            withClue("a creature you control that already had a counter gets one more") { plusOnes(wurm) shouldBe 2 }
            withClue("a creature you control without a counter gets none") { plusOnes(giant) shouldBe 0 }
            withClue("the opponent's creature isn't yours") { plusOnes(angel) shouldBe 1 }
        }
    }
}
