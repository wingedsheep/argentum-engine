package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hedron Crawler — {2} 0/1 Artifact Creature — Construct.
 * "{T}: Add {C}."
 */
class HedronCrawlerScenarioTest : ScenarioTestBase() {
    init {
        test("tapping it adds {C} as a mana ability") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hedron Crawler")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val crawler = game.findPermanent("Hedron Crawler")!!
            val abilityId = cardRegistry.getCard("Hedron Crawler")!!.script.activatedAbilities.single().id

            game.execute(ActivateAbility(game.player1Id, crawler, abilityId)).error shouldBe null
            game.state.stack.size shouldBe 0
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
        }
    }
}
