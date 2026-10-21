package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Calix, Guided by Fate (MAT #26) — "Whenever Calix or an enchanted creature you control deals
 * combat damage to a player, you may create a token that's a copy of a nonlegendary enchantment
 * you control. Do this only once each turn."
 *
 * The "Calix or an enchanted creature" subject is a creature-you-control watcher narrowed by a
 * trigger-time restriction (the damage source is Calix itself or is enchanted); these tests pin
 * both halves of that restriction and that an unenchanted creature doesn't trigger it.
 */
class CalixGuidedByFateScenarioTest : ScenarioTestBase() {

    /** Attack Bob with [attackers] and step through combat until a decision or the trigger is done. */
    private fun TestGame.attackAndConnect(vararg attackers: String) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(attackers.associateWith { 2 }).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && state.step != Step.END_COMBAT && guard++ < 30) {
            if (state.step == Step.DECLARE_BLOCKERS &&
                state.getEntity(player2Id)?.has<BlockersDeclaredThisCombatComponent>() != true
            ) {
                declareNoBlockers()
            } else {
                passPriority()
            }
        }
    }

    private fun base() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Calix, Guided by Fate", summoningSickness = false)
        .withCardOnBattlefield(1, "Glorious Anthem")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Calix, Guided by Fate — combat damage trigger") {
            test("Calix connecting lets you copy a nonlegendary enchantment you control") {
                val game = base().build()

                game.attackAndConnect("Calix, Guided by Fate")
                withClue("Calix's combat damage trigger asks whether to create the copy") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Glorious Anthem was copied — Calix itself is legendary and not a candidate") {
                    game.findPermanents("Glorious Anthem").size shouldBe 2
                    game.findPermanents("Calix, Guided by Fate").size shouldBe 1
                }
            }

            test("declining creates nothing") {
                val game = base().build()

                game.attackAndConnect("Calix, Guided by Fate")
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findPermanents("Glorious Anthem").size shouldBe 1
            }

            test("an enchanted creature you control connecting triggers it") {
                val game = base()
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .build()

                game.attackAndConnect("Grizzly Bears")
                withClue("the enchanted Bears' combat damage triggers Calix") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                if (game.hasPendingDecision()) {
                    game.selectCards(listOf(game.findPermanent("Glorious Anthem")!!)).error shouldBe null
                }
                game.resolveStack()

                game.findPermanents("Glorious Anthem").size shouldBe 2
            }

            test("an unenchanted creature connecting does not trigger it") {
                val game = base()
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .build()

                game.attackAndConnect("Grizzly Bears")
                withClue("no Calix trigger for an unenchanted creature") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                    game.getLifeTotal(2) shouldBe 17
                }
                game.findPermanents("Glorious Anthem").size shouldBe 1
            }
        }
    }
}
