package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ephara's Radiance (BNG #9) — "Enchanted creature has '{1}{W}, {T}: You gain 3 life.'"
 */
class EpharasRadianceScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = cardRegistry.getCard("Ephara's Radiance")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("the enchanted creature pays {1}{W} and taps to gain 3 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Ephara's Radiance", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(ActivateAbility(game.player1Id, bears, grantedId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
