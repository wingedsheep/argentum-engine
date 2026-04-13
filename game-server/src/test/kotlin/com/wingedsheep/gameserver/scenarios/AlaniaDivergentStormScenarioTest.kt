package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Alania, Divergent Storm.
 *
 * Alania, Divergent Storm {3}{U}{R}
 * Legendary Creature — Otter Wizard
 * 3/5
 * Whenever you cast a spell, if it's the first instant spell, the first sorcery spell,
 * or the first Otter spell other than Alania you've cast this turn, you may have target
 * opponent draw a card. If you do, copy that spell. You may choose new targets for the copy.
 */
class AlaniaDivergentStormScenarioTest : ScenarioTestBase() {

    init {
        context("Alania, Divergent Storm trigger conditions") {
            test("triggers when casting first instant of the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alania, Divergent Storm")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Shock", 2)
                // Shock goes on stack, Alania triggers (first instant).
                // Resolve until we hit Alania's MayEffect decision (opponent target auto-selected in 2-player).
                game.resolveStack()

                // We should be paused on Alania's "you may have target opponent draw a card" decision
                game.hasPendingDecision() shouldBe true

                // Say yes — opponent draws a card, spell is copied
                game.answerYesNo(true)
                game.resolveStack()

                // Opponent drew 1 card from Alania's effect
                game.handSize(2) shouldBe 1
            }

            test("does NOT trigger when casting an artifact after an instant was already cast") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alania, Divergent Storm")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Short Bow")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shock (instant) — triggers Alania (first instant)
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()
                game.answerYesNo(false) // decline the may ability
                game.resolveStack()

                // Now cast Short Bow (artifact) — should NOT trigger Alania
                game.castSpell(1, "Short Bow")
                game.resolveStack()

                // No pending decision means no Alania trigger fired
                game.hasPendingDecision() shouldBe false
                // Opponent should not have drawn any cards
                game.handSize(2) shouldBe 0
            }

            test("triggers separately for first instant AND first sorcery in same turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alania, Divergent Storm")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Fell")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shock (instant) — triggers Alania (first instant)
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                // Cast Fell (sorcery targeting creature) — should also trigger Alania (first sorcery)
                val glorySeeker = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Fell", glorySeeker)
                game.resolveStack()

                // Should have a pending decision from Alania's trigger (first sorcery)
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()
            }
        }
    }
}
