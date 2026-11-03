package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Flood of Recollection (RIX #38) — "Return target instant or sorcery card from your graveyard to
 * your hand. Exile Flood of Recollection."
 */
class FloodOfRecollectionScenarioTest : ScenarioTestBase() {

    init {
        test("returns an instant from your graveyard and exiles itself") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Flood of Recollection")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.castSpellTargetingGraveyardCard(1, "Flood of Recollection", listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.isInExile(1, "Flood of Recollection") shouldBe true
            game.isInGraveyard(1, "Flood of Recollection") shouldBe false
        }
    }
}
