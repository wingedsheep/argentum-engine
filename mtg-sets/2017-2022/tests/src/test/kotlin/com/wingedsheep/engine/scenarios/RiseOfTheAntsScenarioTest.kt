package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rise of the Ants (MID #196) — "Create two 3/3 green Insect creature tokens. You gain 2 life.
 * Flashback {6}{G}{G}"
 */
class RiseOfTheAntsScenarioTest : ScenarioTestBase() {

    init {
        context("Rise of the Ants") {

            test("creates two 3/3 Insects and gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rise of the Ants")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Rise of the Ants").error shouldBe null
                game.resolveStack()

                val insects = game.findPermanents("Insect Token")
                insects.size shouldBe 2
                insects.forEach {
                    game.state.projectedState.getPower(it) shouldBe 3
                    game.state.projectedState.getToughness(it) shouldBe 3
                }
                game.getLifeTotal(1) shouldBe 22
            }

            test("flashback from the graveyard does it again and exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Rise of the Ants")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Rise of the Ants").error shouldBe null
                game.resolveStack()

                game.findPermanents("Insect Token").size shouldBe 2
                game.getLifeTotal(1) shouldBe 22
                withClue("flashback exiles the card on resolution") {
                    game.isInExile(1, "Rise of the Ants") shouldBe true
                }
            }
        }
    }
}
