package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Harvestguard Alseids (JOU #13) — {2}{W} Enchantment Creature — Nymph 2/3.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, prevent all
 *   damage that would be dealt to target creature this turn.
 */
class HarvestguardAlseidsScenarioTest : ScenarioTestBase() {
    init {
        test("its entry shields the targeted creature from a Lightning Bolt for the turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Harvestguard Alseids")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 3)
                // Extra Mountains so a red source survives however {2} is auto-paid.
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Harvestguard Alseids").error shouldBe null
            game.resolveStack()

            withClue("constellation asks for a target creature") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the Bolt's damage was prevented, so the Bears survive") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("an unshielded creature still dies to the Bolt") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Harvestguard Alseids")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
