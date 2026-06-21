package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Osseous Sticktwister (DSK #112) — {1}{B} Artifact Creature — Scarecrow 2/2.
 *
 * "Lifelink. Delirium — At the beginning of your end step, if there are four or more card types
 * among cards in your graveyard, each opponent may sacrifice a nonland permanent of their choice or
 * discard a card. Then this creature deals damage equal to its power to each opponent who didn't
 * sacrifice a permanent or discard a card this way."
 *
 * Exercises: the Delirium intervening-if (the trigger does nothing with fewer than four card
 * types), the per-opponent choice (sacrifice / discard / take damage), the damage-on-decline branch,
 * and lifelink on that damage.
 */
class OsseousSticktwisterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Four distinct card types in my graveyard so Delirium is satisfied.
    fun GameTestDriver.fillGraveyardForDelirium(player: com.wingedsheep.sdk.model.EntityId) {
        putCardInGraveyard(player, "Centaur Courser")   // creature
        putCardInGraveyard(player, "Lightning Bolt")     // instant
        putCardInGraveyard(player, "Careful Study")      // sorcery
        putCardInGraveyard(player, "Test Enchantment")   // enchantment
    }

    // Advance from my precombat main to my end step, resolving anything queued along the way.
    fun GameTestDriver.advanceToMyEndStep() {
        var guard = 0
        while (currentStep != Step.END && guard++ < 60) {
            if (state.stack.isNotEmpty() || pendingDecision != null) bothPass() else passPriorityUntil(Step.END)
        }
    }

    test("does nothing without Delirium (fewer than four card types)") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val twister = driver.putCreatureOnBattlefield(me, "Osseous Sticktwister")
        driver.removeSummoningSickness(twister)
        // Only one card type in graveyard.
        driver.putCardInGraveyard(me, "Lightning Bolt")

        val oppLifeBefore = driver.getLifeTotal(opp)
        driver.advanceToMyEndStep()

        // No pending choice; trigger's intervening-if fails, so no damage.
        driver.getLifeTotal(opp) shouldBe oppLifeBefore
    }

    test("opponent who can't pay takes damage and controller gains life from lifelink") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val twister = driver.putCreatureOnBattlefield(me, "Osseous Sticktwister")
        driver.removeSummoningSickness(twister)
        driver.fillGraveyardForDelirium(me)

        // The opponent may have a hand card and/or permanents, so they get a real choice; we
        // explicitly pick the "take damage" branch below to exercise the damage + lifelink path.
        val oppLifeBefore = driver.getLifeTotal(opp)
        val myLifeBefore = driver.getLifeTotal(me)

        driver.advanceToMyEndStep()

        // Resolve the trigger. If the opponent has a hand card they get a choice; pick "discard"
        // is the cooperative path — but to test the damage branch we explicitly choose damage.
        var guard = 0
        while (guard++ < 30) {
            val pd = driver.pendingDecision
            when (pd) {
                is ChooseOptionDecision -> {
                    val damageIdx = pd.options.indexOfFirst { it.contains("damage", ignoreCase = true) }
                    driver.submitDecision(opp, OptionChosenResponse(pd.id, damageIdx))
                }
                is SelectCardsDecision -> driver.submitCardSelection(opp, pd.options.take(1))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        // 2 damage dealt to the opponent; lifelink gains the controller 2.
        driver.getLifeTotal(opp) shouldBe oppLifeBefore - 2
        driver.getLifeTotal(me) shouldBe myLifeBefore + 2
    }

    test("opponent who discards takes no damage") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val twister = driver.putCreatureOnBattlefield(me, "Osseous Sticktwister")
        driver.removeSummoningSickness(twister)
        driver.fillGraveyardForDelirium(me)

        // Guarantee the opponent has a card in hand to discard.
        driver.putCardInHand(opp, "Lightning Bolt")

        val oppLifeBefore = driver.getLifeTotal(opp)

        driver.advanceToMyEndStep()

        var guard = 0
        var discarded = false
        while (guard++ < 30) {
            val pd = driver.pendingDecision
            when (pd) {
                is ChooseOptionDecision -> {
                    val discardIdx = pd.options.indexOfFirst { it.contains("Discard", ignoreCase = true) }
                    driver.submitDecision(opp, OptionChosenResponse(pd.id, discardIdx))
                }
                is SelectCardsDecision -> {
                    driver.submitCardSelection(opp, pd.options.take(1))
                    discarded = true
                }
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        discarded shouldBe true
        // No damage because the opponent discarded "this way".
        driver.getLifeTotal(opp) shouldBe oppLifeBefore
    }
})
