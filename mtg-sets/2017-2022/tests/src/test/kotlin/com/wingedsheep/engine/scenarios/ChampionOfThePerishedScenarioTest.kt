package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/** Champion of the Perished (MID #91) — "Whenever another Zombie you control enters, put a +1/+1 counter on this creature." */
class ChampionOfThePerishedScenarioTest : ScenarioTestBase() {
    init {
        fun counters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("another Zombie entering under your control adds a +1/+1 counter") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Champion of the Perished")
                .withCardInHand(1, "Siege Zombie")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val champion = game.findPermanent("Champion of the Perished")!!
            game.castSpell(1, "Siege Zombie").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Siege Zombie") shouldBe true
            counters(game, champion) shouldBe 1
            game.state.projectedState.getPower(champion) shouldBe 2
        }

        test("a non-Zombie entering does not trigger it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Champion of the Perished")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val champion = game.findPermanent("Champion of the Perished")!!
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            counters(game, champion) shouldBe 0
        }
    }
}
