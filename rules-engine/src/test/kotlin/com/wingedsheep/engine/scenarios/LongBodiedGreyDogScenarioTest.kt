package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Long-Bodied Grey Dog (HOB #1) — {3} Creature — Dog 2/2.
 *
 * "Flash
 *  Reach
 *  When this creature enters, create a tapped Treasure token."
 *
 * Covers the printed keywords, the flash timing (castable on the opponent's turn), and that the
 * Treasure arrives *tapped*.
 */
class LongBodiedGreyDogScenarioTest : ScenarioTestBase() {

    init {
        context("Long-Bodied Grey Dog") {

            test("it enters as a 2/2 with reach and makes a tapped Treasure") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long-Bodied Grey Dog")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                g.castSpell(1, "Long-Bodied Grey Dog").error shouldBe null
                g.resolveStack()

                val dog = g.findPermanent("Long-Bodied Grey Dog")!!
                withClue("printed characteristics") {
                    g.state.projectedState.getPower(dog) shouldBe 2
                    g.state.projectedState.getToughness(dog) shouldBe 2
                    g.state.projectedState.hasKeyword(dog, Keyword.REACH) shouldBe true
                }

                val treasure = g.findPermanent("Treasure")
                    ?: error("no Treasure token was created")
                withClue("the Treasure enters tapped") {
                    g.state.getEntity(treasure)?.get<TappedComponent>() shouldBe TappedComponent
                }
            }

            test("flash lets it be cast on the opponent's turn") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Long-Bodied Grey Dog")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                g.castSpell(1, "Long-Bodied Grey Dog").error shouldBe null
                g.resolveStack()

                g.isOnBattlefield("Long-Bodied Grey Dog") shouldBe true
            }
        }
    }
}
