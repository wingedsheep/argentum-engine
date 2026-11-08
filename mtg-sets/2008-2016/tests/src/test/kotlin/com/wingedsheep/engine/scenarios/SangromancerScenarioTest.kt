package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sangromancer {2}{B}{B} — Creature — Vampire Shaman 3/3, Flying.
 *
 * "Whenever a creature an opponent controls dies, you may gain 3 life. Whenever an opponent
 *  discards a card, you may gain 3 life."
 *
 * Both are optional, per event: two discarded cards are two triggers.
 */
class SangromancerScenarioTest : ScenarioTestBase() {

    private fun TestGame.driveDecisions(gain: Boolean) {
        var guard = 0
        while ((hasPendingDecision() || state.stack.isNotEmpty()) && guard++ < 20) {
            if (!hasPendingDecision()) {
                resolveStack()
                continue
            }
            when (val d = getPendingDecision()) {
                is YesNoDecision -> answerYesNo(gain)
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections))
                else -> error("unexpected decision $d")
            }
        }
    }

    init {
        context("Sangromancer") {
            test("an opponent's creature dying lets you gain 3 life — and you may decline") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Sangromancer")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                game.driveDecisions(gain = true)
                game.getLifeTotal(1) shouldBe 13
            }

            test("declining the optional gain leaves life untouched") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Sangromancer")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                game.driveDecisions(gain = false)
                game.getLifeTotal(1) shouldBe 10
            }

            test("each card an opponent discards is its own 3-life trigger") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Sangromancer")
                    .withCardInHand(1, "Mind Rot")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Forest")
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
                game.resolveStack()
                game.driveDecisions(gain = true)
                game.getLifeTotal(1) shouldBe 16
                game.handSize(2) shouldBe 1
            }
        }
    }
}
