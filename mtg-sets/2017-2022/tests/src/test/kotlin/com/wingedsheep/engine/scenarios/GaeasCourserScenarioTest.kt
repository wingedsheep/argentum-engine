package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gaea's Courser (BRO #181) — "Whenever this creature attacks, if there are three or more creature
 * cards in your graveyard, draw a card."
 */
class GaeasCourserScenarioTest : ScenarioTestBase() {

    private fun game(creatureCardsInGraveyard: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Gaea's Courser")
            .withCardInGraveyard(1, "Forest")
        repeat(creatureCardsInGraveyard) { builder.withCardInGraveyard(1, "Grizzly Bears") }
        return builder
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()
    }

    init {
        test("attacking with three creature cards in the graveyard draws a card") {
            val game = game(creatureCardsInGraveyard = 3)
            val handBefore = game.handSize(1)

            game.declareAttackers(mapOf("Gaea's Courser" to 2)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }

        test("attacking with only two creature cards in the graveyard draws nothing") {
            val game = game(creatureCardsInGraveyard = 2)
            val handBefore = game.handSize(1)

            game.declareAttackers(mapOf("Gaea's Courser" to 2)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore
        }
    }
}
