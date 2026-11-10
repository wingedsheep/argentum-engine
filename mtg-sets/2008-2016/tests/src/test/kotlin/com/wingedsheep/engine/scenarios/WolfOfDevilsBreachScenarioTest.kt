package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wolf of Devil's Breach (SOI #192) — {3}{R}{R} Creature — Elemental Wolf 5/5.
 *
 *   Whenever this creature attacks, you may pay {1}{R} and discard a card. If you do, this
 *   creature deals damage to target creature or planeswalker equal to the discarded card's
 *   mana value.
 *
 * Covers the cost-linked amount: the card discarded on resolution (stored by the nested
 * pay-then-discard cost) sets the damage dealt to the trigger-time target; declining spends
 * nothing; an empty hand is never offered the payment.
 */
class WolfOfDevilsBreachScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Answer prompts until the stack is empty: [target] for the trigger, [yes] to the offer, [discard] for the card. */
    private fun TestGame.drive(target: EntityId, yes: Boolean, discard: String?) {
        var guard = 0
        while (guard++ < 40) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> selectTargets(listOf(target))
                is YesNoDecision -> answerYesNo(yes)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                is SelectCardsDecision -> selectCards(decision.options.filter { nameOf(it) == discard }.take(1))
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("Unexpected decision: $decision")
            }
        }
    }

    private fun TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.tappedMountains(): Int =
        findPermanents("Mountain").count { state.getEntity(it)?.has<TappedComponent>() == true }

    private fun board(withCardInHand: String?) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Wolf of Devil's Breach", summoningSickness = false)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardOnBattlefield(2, "Craw Wurm")
        .let { if (withCardInHand != null) it.withCardInHand(1, withCardInHand) else it }
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Wolf of Devil's Breach") {

            test("paying {1}{R} and discarding a card deals damage equal to its mana value to the target") {
                val game = board(withCardInHand = "Grizzly Bears")
                val wurm = game.findPermanent("Craw Wurm")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wolf of Devil's Breach" to 2))
                game.drive(target = wurm, yes = true, discard = "Grizzly Bears")

                withClue("{1}{R} was paid") { game.tappedMountains() shouldBe 2 }
                withClue("Grizzly Bears was discarded") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("the Wurm took 2 damage — Grizzly Bears' mana value") { game.damageOn(wurm) shouldBe 2 }
            }

            test("declining the payment spends nothing, keeps the card and deals no damage") {
                val game = board(withCardInHand = "Grizzly Bears")
                val wurm = game.findPermanent("Craw Wurm")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wolf of Devil's Breach" to 2))
                game.drive(target = wurm, yes = false, discard = null)

                game.tappedMountains() shouldBe 0
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.damageOn(wurm) shouldBe 0
            }

            test("discarding a land pays the cost but deals no damage") {
                val game = board(withCardInHand = "Mountain")
                val wurm = game.findPermanent("Craw Wurm")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wolf of Devil's Breach" to 2))
                game.drive(target = wurm, yes = true, discard = "Mountain")

                game.tappedMountains() shouldBe 2
                game.isInGraveyard(1, "Mountain") shouldBe true
                game.damageOn(wurm) shouldBe 0
            }

            test("with an empty hand the payment is never offered") {
                val game = board(withCardInHand = null)
                val wurm = game.findPermanent("Craw Wurm")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wolf of Devil's Breach" to 2))
                var offered = false
                var guard = 0
                while (guard++ < 40) {
                    when (game.getPendingDecision()) {
                        is ChooseTargetsDecision -> game.selectTargets(listOf(wurm))
                        is YesNoDecision -> { offered = true; game.answerYesNo(false) }
                        null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                        else -> error("Unexpected decision: ${game.getPendingDecision()}")
                    }
                }

                offered shouldBe false
                game.tappedMountains() shouldBe 0
                game.damageOn(wurm) shouldBe 0
            }
        }
    }
}
