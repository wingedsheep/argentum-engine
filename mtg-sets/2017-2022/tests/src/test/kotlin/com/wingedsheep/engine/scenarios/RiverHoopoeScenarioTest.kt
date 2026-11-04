package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.RiverHoopoe
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * River Hoopoe (HOU) — Flying; {3}{G}{U}: You gain 2 life and draw a card.
 */
class RiverHoopoeScenarioTest : ScenarioTestBase() {
    init {
        test("{3}{G}{U}: gains 2 life and draws a card") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "River Hoopoe")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val hoopoe = game.findPermanent("River Hoopoe")!!
            game.state.projectedState.hasKeyword(hoopoe, Keyword.FLYING) shouldBe true

            game.execute(
                ActivateAbility(game.player1Id, hoopoe, RiverHoopoe.activatedAbilities.single().id)
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 22
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }
    }
}
