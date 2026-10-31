package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Howling Giant (M20 #177) — {5}{G}{G} Creature — Giant Druid 5/5.
 * "Reach / When this creature enters, create two 2/2 green Wolf creature tokens."
 */
class HowlingGiantScenarioTest : ScenarioTestBase() {
    init {
        context("Howling Giant") {
            test("entering creates two 2/2 green Wolf tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Howling Giant")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Howling Giant").error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Howling Giant")!!
                game.state.projectedState.hasKeyword(giant, Keyword.REACH) shouldBe true

                val wolves = game.findPermanents("Wolf Token")
                withClue("exactly two Wolf tokens") { wolves.size shouldBe 2 }
                wolves.forEach { wolf ->
                    game.state.projectedState.getPower(wolf) shouldBe 2
                    game.state.projectedState.getToughness(wolf) shouldBe 2
                    game.state.getEntity(wolf)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.GREEN)
                }
            }
        }
    }
}
