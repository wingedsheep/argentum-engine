package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Murmuring Mystic (GRN #45) — {3}{U} 1/5 Human Wizard.
 *
 *   Whenever you cast an instant or sorcery spell, create a 1/1 blue Bird Illusion creature token
 *   with flying.
 */
class MurmuringMysticScenarioTest : ScenarioTestBase() {

    init {
        test("casting an instant creates a 1/1 flying Bird Illusion") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Murmuring Mystic")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            val bird = game.findPermanents("Bird Illusion Token").single()
            val projected = game.state.projectedState
            projected.getPower(bird) shouldBe 1
            projected.getToughness(bird) shouldBe 1
            projected.hasKeyword(bird, Keyword.FLYING) shouldBe true
        }

        test("casting a creature spell makes no token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Murmuring Mystic")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.findPermanents("Bird Illusion Token").shouldBeEmpty()
        }
    }
}
