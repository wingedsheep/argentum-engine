package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shadowbeast Sighting (MID #198) — "Create a 4/4 green Beast creature token. Flashback {6}{G}"
 */
class ShadowbeastSightingScenarioTest : ScenarioTestBase() {

    init {
        context("Shadowbeast Sighting") {

            test("creates a 4/4 Beast token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shadowbeast Sighting")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shadowbeast Sighting").error shouldBe null
                game.resolveStack()

                val beasts = game.findPermanents("Beast Token")
                beasts.size shouldBe 1
                game.state.projectedState.getPower(beasts.single()) shouldBe 4
                game.state.projectedState.getToughness(beasts.single()) shouldBe 4
            }

            test("flashback from the graveyard creates another Beast and exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Shadowbeast Sighting")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Shadowbeast Sighting").error shouldBe null
                game.resolveStack()

                game.findPermanents("Beast Token").size shouldBe 1
                withClue("flashback exiles the card on resolution") {
                    game.isInExile(1, "Shadowbeast Sighting") shouldBe true
                }
            }
        }
    }
}
