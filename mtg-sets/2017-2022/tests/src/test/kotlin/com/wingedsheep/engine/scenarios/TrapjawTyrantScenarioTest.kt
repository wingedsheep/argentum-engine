package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Trapjaw Tyrant (RIX #29) — "Enrage — Whenever this creature is dealt damage, exile target
 * creature an opponent controls until this creature leaves the battlefield."
 */
class TrapjawTyrantScenarioTest : ScenarioTestBase() {

    init {
        test("enrage exiles an opponent's creature until the Tyrant leaves the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Trapjaw Tyrant")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tyrant = game.findPermanent("Trapjaw Tyrant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Shock", tyrant).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true

            game.castSpell(1, "Doom Blade", tyrant).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Trapjaw Tyrant") shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
