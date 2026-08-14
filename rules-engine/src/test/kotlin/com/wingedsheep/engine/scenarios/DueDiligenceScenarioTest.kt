package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Due Diligence (MKM #14) — {2}{W} Enchantment — Aura.
 * "Enchant creature / When this Aura enters, target creature you control other than enchanted
 * creature gets +2/+2 and gains vigilance until end of turn. / Enchanted creature gets +2/+2
 * and has vigilance."
 *
 * The load-bearing part is "other than enchanted creature": the trigger's target filter is the
 * source-relative `notAttachedToBySource()` exclusion, and a fail-open filter would silently let
 * the player stack both buffs onto the enchanted creature. Both tests below pin that down — one
 * on the legal-target list the engine offers, one on the resulting stats.
 */
class DueDiligenceScenarioTest : ScenarioTestBase() {

    init {
        context("Due Diligence") {

            test("the enters trigger cannot target the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(1, "Due Diligence")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                // Enchant the Bears.
                game.castSpell(1, "Due Diligence", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets[0].orEmpty()
                withClue("the other creature you control is offered") {
                    legal shouldContain courser
                }
                withClue("the enchanted creature is excluded — 'other than enchanted creature'") {
                    legal shouldNotContain bears
                }
            }

            test("the static buffs the host and the trigger buffs a second creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(1, "Due Diligence")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Due Diligence", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("enchanted Grizzly Bears is 2/2 + 2/+2 = 4/4 with vigilance") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 4
                    projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
                withClue("the triggered target Centaur Courser is 3/3 + 2/+2 = 5/5 with vigilance") {
                    projected.getPower(courser) shouldBe 5
                    projected.getToughness(courser) shouldBe 5
                    projected.hasKeyword(courser, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
