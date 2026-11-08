package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Enlightened Maniac (EMN #58) — {3}{U} Creature — Human, 0/2.
 *
 *   When this creature enters, create a 3/2 colorless Eldrazi Horror creature token.
 */
class EnlightenedManiacScenarioTest : ScenarioTestBase() {

    init {
        test("entering the battlefield creates a 3/2 colorless Eldrazi Horror token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Enlightened Maniac")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Enlightened Maniac").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Enlightened Maniac") shouldBe true
            val tokens = game.findAllPermanents("Eldrazi Horror Token")
            withClue("the ETB trigger created one 3/2 colorless Eldrazi Horror") {
                tokens shouldHaveSize 1
                val token = tokens.single()
                game.state.projectedState.getPower(token) shouldBe 3
                game.state.projectedState.getToughness(token) shouldBe 2
                game.state.projectedState.getColors(token).isEmpty() shouldBe true
                game.state.projectedState.hasSubtype(token, "Eldrazi") shouldBe true
                game.state.projectedState.hasSubtype(token, "Horror") shouldBe true
            }
        }
    }
}
