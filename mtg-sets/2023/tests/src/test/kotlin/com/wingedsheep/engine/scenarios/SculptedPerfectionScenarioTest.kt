package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sculpted Perfection (MOM #253) — {2}{W}{B} Enchantment
 *
 * When this enchantment enters, incubate 2.
 * Phyrexians you control get +1/+1.
 */
class SculptedPerfectionScenarioTest : ScenarioTestBase() {
    init {
        test("entering incubates 2; only Phyrexians you control get +1/+1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sculpted Perfection")
                .withCardOnBattlefield(1, "Dreg Recycler")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Dreg Recycler")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sculpted Perfection").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            val incubators = game.state.getBattlefield(game.player1Id)
                .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Incubator" }
            incubators.size shouldBe 1
            game.state.getEntity(incubators.single())?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

            val projected = game.state.projectedState
            val myRecycler = game.state.getBattlefield(game.player1Id)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Dreg Recycler" }
            val theirRecycler = game.state.getBattlefield(game.player2Id)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Dreg Recycler" }
            val bears = game.findPermanent("Grizzly Bears")!!

            projected.getPower(myRecycler) shouldBe 3
            projected.getToughness(myRecycler) shouldBe 3
            projected.getPower(theirRecycler) shouldBe 2
            projected.getPower(bears) shouldBe 2
        }
    }
}
