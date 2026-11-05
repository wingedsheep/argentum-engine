package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sixth Sense (AKH #187) — {G} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature has "Whenever this creature deals combat damage to a player, you may draw a
 *   card."
 */
class SixthSenseScenarioTest : ScenarioTestBase() {
    init {
        context("Sixth Sense") {

            test("the enchanted creature connecting lets its controller draw") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Sixth Sense", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                game.hasPendingDecision() shouldBe true // the "you may" prompt
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.isInHand(1, "Island") shouldBe true
            }

            test("declining the draw leaves the library alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Sixth Sense", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Island") shouldBe false
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
