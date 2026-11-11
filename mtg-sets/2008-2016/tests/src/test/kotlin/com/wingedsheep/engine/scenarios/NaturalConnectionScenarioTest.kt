package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Natural Connection — fetches a basic land onto the battlefield tapped. */
class NaturalConnectionScenarioTest : ScenarioTestBase() {
    init {
        test("puts a basic land from the library onto the battlefield tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Natural Connection")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val plains = game.findCardsInLibrary(1, "Plains").first()

            game.castSpell(1, "Natural Connection").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(plains)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Plains") shouldBe true
            game.state.getEntity(plains)!!.has<TappedComponent>() shouldBe true
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
        }
    }
}
