package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * Sun-Blessed Healer — "{1}{W} 3/1. Kicker {1}{W}. Lifelink. When this creature enters, if it was
 * kicked, return target nonland permanent card with mana value 2 or less from your graveyard to the
 * battlefield."
 */
class SunBlessedHealerScenarioTest : ScenarioTestBase() {

    init {
        test("kicked: reanimates a nonland permanent with mana value 2 or less") {
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Plains", 4)   // {1}{W} + kicker {1}{W}
                .withCardInHand(1, "Sun-Blessed Healer")
                .withCardInGraveyard(1, "Bear Cub")        // 2/2, MV 2
                .build()

            val healer = game.findCardsInHand(1, "Sun-Blessed Healer").first()
            game.execute(
                CastSpell(playerId = game.player1Id, cardId = healer, declaredCostSlot = ChoiceSlot.KICKED)
            ).error shouldBe null
            game.resolveStack()

            // The kicked ETB trigger targets the only legal graveyard card.
            val bearCub = game.findCardsInGraveyard(1, "Bear Cub").first()
            val td = game.getPendingDecision() as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision for the kicked ETB; got ${game.getPendingDecision()}")
            game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bearCub))))
            game.resolveStack()

            game.isOnBattlefield("Bear Cub") shouldBe true
            game.isInGraveyard(1, "Bear Cub") shouldBe false
        }

        test("unkicked: ETB does nothing, graveyard card stays put") {
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Sun-Blessed Healer")
                .withCardInGraveyard(1, "Bear Cub")
                .build()

            game.castSpell(1, "Sun-Blessed Healer").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Bear Cub") shouldBe true
            game.isOnBattlefield("Bear Cub") shouldBe false
        }
    }
}
