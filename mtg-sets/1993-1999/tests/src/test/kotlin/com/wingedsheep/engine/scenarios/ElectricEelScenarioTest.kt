package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ElectricEelScenarioTest : ScenarioTestBase() {
    init {
        test("entering deals 1 damage to its controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Electric Eel")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLifeTotal(1, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Electric Eel").error shouldBe null
            game.resolveStack()

            game.findPermanent("Electric Eel") shouldNotBe null
            game.getLifeTotal(1) shouldBe 19
        }

        test("the activated ability pumps it and deals 1 damage to its controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Electric Eel")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLifeTotal(1, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eel = game.findPermanent("Electric Eel")!!
            val abilityId = cardRegistry.getCard("Electric Eel")!!.script.activatedAbilities.single().id
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = eel, abilityId = abilityId)
            )

            result.error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(eel) shouldBe 3
            projected.getToughness(eel) shouldBe 1
            game.getLifeTotal(1) shouldBe 19
        }
    }
}
