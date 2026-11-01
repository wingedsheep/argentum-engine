package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sentinel's Mark (RNA #20) — {1}{W} Enchantment — Aura.
 *
 *   Flash
 *   Enchant creature
 *   Enchanted creature gets +1/+2 and has vigilance.
 *   Addendum — When this Aura enters, if you cast it during your main phase, enchanted creature
 *   gains lifelink until end of turn.
 *
 * Proves the addendum fires when cast in your main phase and not when flashed in at another time;
 * the static +1/+2 and vigilance apply either way.
 */
class SentinelsMarkScenarioTest : ScenarioTestBase() {

    private fun board(phase: Phase, step: Step) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Sentinel's Mark")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(phase, step)
        .build()

    init {
        context("Sentinel's Mark") {

            test("cast during your main phase: +1/+2, vigilance, and lifelink until end of turn") {
                val game = board(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Sentinel's Mark", targetId = bears).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Grizzly Bears (2/2) gets +1/+2") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 4
                }
                projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                withClue("addendum: cast in your main phase grants lifelink") {
                    projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                }
            }

            test("flashed in outside your main phase: no lifelink") {
                val game = board(Phase.COMBAT, Step.BEGIN_COMBAT)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Sentinel's Mark", targetId = bears).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 4
                projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                withClue("addendum does not apply outside your main phase") {
                    projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                }
            }
        }
    }
}
