package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Pyrulea // Gargantuan Slabhorn.
 *
 * Front: the enters trigger scries 3, reveals the top card, and draws only for a land or a
 * double-faced card. Back: "Other transformed permanents you control have trample and ward {2}" —
 * the projection-side read of CR 701.27g, proved with Smoldering Werewolf ({4}{R}{R}: Transform),
 * which gains the grant only once it is back face up.
 */
class InvasionOfPyruleaScenarioTest : ScenarioTestBase() {

    /** Scry: keep everything on top, in the order offered. */
    private fun TestGame.keepScryOnTop() {
        var guard = 0
        while (guard++ < 5) {
            when (val d = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(emptyList()).error shouldBe null
                is ReorderLibraryDecision -> submitDecision(OrderedResponse(d.id, d.cards)).error shouldBe null
                else -> return
            }
        }
    }

    private fun TestGame.defeatSiegeAndCastBack() {
        checkStateBasedActions()
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(
            permanentAttackers = mapOf("Serra Angel" to "Invasion of Pyrulea")
        ).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && guard++ < 30) {
            if (state.step == Step.DECLARE_BLOCKERS &&
                state.getEntity(player2Id)
                    ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
            ) {
                declareNoBlockers()
            } else {
                passPriority()
            }
        }
        answerYesNo(true).error shouldBe null
        resolveStack()
    }

    private fun TestGame.transformWerewolf() {
        val wolf = findPermanent("Smoldering Werewolf")!!
        val abilityId = cardRegistry.getCard("Smoldering Werewolf")!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = wolf, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("front face — the enters trigger") {

            test("a land revealed after scry 3 is drawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Pyrulea")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Pyrulea").error shouldBe null
                game.resolveStack()
                game.keepScryOnTop()
                game.resolveStack()

                withClue("the Siege left the hand, the revealed Island was drawn") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("a single-faced nonland revealed is not drawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Pyrulea")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Pyrulea").error shouldBe null
                game.resolveStack()
                game.keepScryOnTop()
                game.resolveStack()

                game.handSize(1) shouldBe 0
            }

            test("a double-faced card revealed is drawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Pyrulea")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Smoldering Werewolf")
                    .withCardInLibrary(1, "Smoldering Werewolf")
                    .withCardInLibrary(1, "Smoldering Werewolf")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Pyrulea").error shouldBe null
                game.resolveStack()
                game.keepScryOnTop()
                game.resolveStack()

                game.handSize(1) shouldBe 1
            }
        }

        context("back face — Gargantuan Slabhorn") {

            test("other transformed permanents you control have trample and ward {2}; front faces don't") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Invasion of Pyrulea")
                    .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
                    .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.defeatSiegeAndCastBack()
                val slabhorn = game.findPermanent("Gargantuan Slabhorn")!!
                withClue("the Slabhorn has its own printed trample") {
                    game.state.projectedState.hasKeyword(slabhorn, Keyword.TRAMPLE) shouldBe true
                }
                val wolf = game.findPermanent("Smoldering Werewolf")!!
                withClue("a front-face Werewolf is not a transformed permanent") {
                    game.state.projectedState.hasKeyword(wolf, Keyword.TRAMPLE) shouldBe false
                }

                game.advanceToPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.transformWerewolf()
                withClue("the flipped Erupting Dreadwolf gains trample") {
                    game.state.projectedState.hasKeyword(wolf, Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
