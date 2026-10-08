package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Union of the Third Path (BRO #31) — draw a card, then gain life equal to the number of cards in
 * your hand. The hand is counted after the draw, so the drawn card is included.
 */
class UnionOfTheThirdPathScenarioTest : ScenarioTestBase() {

    init {
        test("gains life equal to hand size after the draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Union of the Third Path")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Union of the Third Path").error shouldBe null
            game.resolveStack()

            withClue("two Bears plus the drawn card") { game.handSize(1) shouldBe 3 }
            withClue("gained 3 life") { game.getLifeTotal(1) shouldBe 23 }
        }
    }
}
