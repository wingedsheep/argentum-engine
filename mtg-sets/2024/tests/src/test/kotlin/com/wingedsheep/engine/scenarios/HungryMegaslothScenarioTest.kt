package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hungry Megasloth (J25 #21) — "Reach. {2}, {T}: Put a +1/+1 counter on this creature."
 */
class HungryMegaslothScenarioTest : ScenarioTestBase() {

    init {
        test("{2}, {T} grows the Megasloth with a +1/+1 counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hungry Megasloth")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sloth = game.findPermanent("Hungry Megasloth")!!
            val abilityId = cardRegistry.requireCard("Hungry Megasloth").activatedAbilities.single().id

            game.state.projectedState.hasKeyword(sloth, Keyword.REACH) shouldBe true

            game.execute(ActivateAbility(game.player1Id, sloth, abilityId)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(sloth)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(sloth) shouldBe 4
            game.state.projectedState.getToughness(sloth) shouldBe 4
        }
    }
}
