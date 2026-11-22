package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Burning-Tree Emissary (GTC #216) — When this creature enters, add {R}{G}. */
class BurningTreeEmissaryScenarioTest : ScenarioTestBase() {
    init {
        test("entering adds one red and one green mana") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Burning-Tree Emissary")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Burning-Tree Emissary").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Burning-Tree Emissary") shouldBe true
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
            withClue("the trigger added {R}{G}") {
                pool?.red shouldBe 1
                pool?.green shouldBe 1
            }
        }
    }
}
