package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Hammer of Ruin (WWK #124) — "Equipped creature gets +2/+0. Whenever equipped creature deals combat
 * damage to a player, you may destroy target Equipment that player controls. Equip {2}"
 *
 * The first card pairing an ATTACHED-bound combat-damage trigger with
 * `controlledByTriggeringPlayer()`. This pins that "that player" is the damaged player on the
 * Equipment's trigger too: only the damaged player's Equipment are legal targets — not the Hammer
 * itself, not another Equipment its controller has, and not the damaged player's non-Equipment
 * artifact — and that the "you may" is honoured both ways at resolution.
 */
class HammerOfRuinScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardAttachedTo(1, "Hammer of Ruin", "Grizzly Bears")
        .withCardOnBattlefield(1, "Leonin Scimitar")   // our own unattached Equipment — never a target
        .withCardOnBattlefield(2, "Bonesplitter")      // the damaged player's Equipment
        .withCardOnBattlefield(2, "Basilisk Collar")   // the damaged player's Equipment
        .withCardOnBattlefield(2, "Sol Ring")          // the damaged player's non-Equipment artifact
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Hammer of Ruin") {
            test("combat damage to a player lets you destroy an Equipment that player controls") {
                val game = buildGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                val collar = game.findPermanent("Basilisk Collar")!!

                withClue("equipped creature gets +2/+0") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack(); guard++
                }
                val td = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected ChooseTargetsDecision for Hammer of Ruin's trigger; got ${game.state.pendingDecision}")

                withClue("only the damaged player's Equipment are legal — not ours, not their Sol Ring") {
                    td.legalTargets[0]!!.shouldContainExactlyInAnyOrder(bonesplitter, collar)
                }

                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bonesplitter)))).error shouldBe null
                game.resolveStack()

                withClue("the 'you may' is asked on resolution") {
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("4 combat damage went through") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("the targeted Equipment is destroyed") {
                    game.isInGraveyard(2, "Bonesplitter") shouldBe true
                }
                withClue("everything else stays") {
                    game.isOnBattlefield("Basilisk Collar") shouldBe true
                    game.isOnBattlefield("Sol Ring") shouldBe true
                    game.isOnBattlefield("Hammer of Ruin") shouldBe true
                    game.isOnBattlefield("Leonin Scimitar") shouldBe true
                }
            }

            test("declining the may leaves the targeted Equipment in play") {
                val game = buildGame()
                val bonesplitter = game.findPermanent("Bonesplitter")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack(); guard++
                }
                val td = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected ChooseTargetsDecision for Hammer of Ruin's trigger; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bonesplitter)))).error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declined — Bonesplitter survives") {
                    game.isOnBattlefield("Bonesplitter") shouldBe true
                    game.isInGraveyard(2, "Bonesplitter") shouldBe false
                }
            }
        }
    }
}
