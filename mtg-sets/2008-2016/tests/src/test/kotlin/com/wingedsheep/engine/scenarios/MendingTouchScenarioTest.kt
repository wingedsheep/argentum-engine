package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mending Touch (DGM #44) — {G} Instant.
 *
 *   Regenerate target creature.
 */
class MendingTouchScenarioTest : ScenarioTestBase() {
    init {
        test("the regeneration shield saves the creature from lethal damage and taps it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mending Touch")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Mending Touch", bears).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("regenerated instead of destroyed, and tapped") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
