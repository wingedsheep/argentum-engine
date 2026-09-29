package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AngelOfLightScenarioTest : ScenarioTestBase() {
    init {
        test("attacks without tapping and deals three combat damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Angel of Light")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Angel of Light" to 2)).error shouldBe null

            val angel = game.findPermanent("Angel of Light")!!
            game.state.getEntity(angel)!!.has<TappedComponent>() shouldBe false

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 17
        }

        test("flying prevents ground blockers but permits flying blockers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Angel of Light")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wind Drake")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Angel of Light" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Angel of Light"))).error shouldNotBe null
            game.declareBlockers(mapOf("Wind Drake" to listOf("Angel of Light"))).error shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 20
            game.isOnBattlefield("Angel of Light") shouldBe true
            game.isInGraveyard(2, "Wind Drake") shouldBe true
        }
    }
}
