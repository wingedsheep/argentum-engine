package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.ArmoredKincaller
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Armored Kincaller (LCI #174) — {2}{G} Creature — Dinosaur 3/3
 *
 * "When this creature enters, you may reveal a Dinosaur card from your hand.
 *  If you do or if you control another Dinosaur, you gain 3 life."
 *
 * Proves three distinct resolution paths:
 *  1. Reveal-Dinosaur path: player reveals a Dinosaur from hand → gain 3 life
 *     (no other Dinosaur on the battlefield, so the reveal alone satisfies "if you do").
 *  2. Controls-another-Dinosaur path: no Dinosaur in hand, but another Dinosaur
 *     is already on the battlefield → no prompt, gain 3 life via the second condition.
 *  3. Neither path: no Dinosaur in hand, no other Dinosaur on the battlefield →
 *     no prompt, no life gain.
 */
class ArmoredKincallerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ArmoredKincaller)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Give the player {2}{G}, cast Armored Kincaller, and drain all stack events and decisions.
     *
     * [onDecision] is invoked once whenever a [SelectCardsDecision] surfaces during
     * resolution (the "you may reveal" prompt). Pass an empty block for the "no-reveal"
     * cases — the loop just keeps passing priority and the decision never appears.
     */
    fun GameTestDriver.castKincallerAndDrain(
        playerId: EntityId,
        cardId: EntityId,
        onDecision: (SelectCardsDecision) -> Unit = {},
    ) {
        giveColorlessMana(playerId, 2)
        giveMana(playerId, Color.GREEN, 1)
        castSpell(playerId, cardId)
        var guard = 0
        while (guard++ < 30) {
            val pd = pendingDecision
            when {
                pd is SelectCardsDecision -> onDecision(pd)
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    // -------------------------------------------------------------------------
    // Test 1: Reveal path
    // -------------------------------------------------------------------------

    test("reveal-a-Dinosaur path: revealing from hand gains 3 life") {
        val driver = newDriver()
        val me = driver.player1

        // A Dinosaur card in hand to reveal. No other Dinos on the battlefield
        // (only Kincaller itself enters), so the life gain comes from the reveal alone.
        val dinoInHand = driver.putCardInHand(me, "Earthshaker Dreadmaw")
        val kincaller = driver.putCardInHand(me, "Armored Kincaller")
        val lifeBefore = driver.getLifeTotal(me)

        driver.castKincallerAndDrain(me, kincaller) { _ ->
            // The ETB "you may reveal" prompt appears — submit the Dino card as the reveal.
            driver.submitCardSelection(me, listOf(dinoInHand))
        }

        // "If you do" is satisfied by the reveal → gain 3 life.
        driver.getLifeTotal(me) shouldBe lifeBefore + 3
    }

    // -------------------------------------------------------------------------
    // Test 2: Controls-another-Dinosaur path (no card in hand)
    // -------------------------------------------------------------------------

    test("controls-another-Dinosaur path: another Dino on field with no Dino in hand gains 3 life") {
        val driver = newDriver()
        val me = driver.player1

        // Place a second Dinosaur directly on the battlefield (bypasses ETB triggers).
        driver.putCreatureOnBattlefield(me, "Earthshaker Dreadmaw")

        // No Dinosaur cards in hand (deck is all Forests, hand has only land cards).
        val kincaller = driver.putCardInHand(me, "Armored Kincaller")
        val lifeBefore = driver.getLifeTotal(me)

        // The reveal is asked even with no Dino in hand (#3340); nothing can be revealed.
        driver.castKincallerAndDrain(me, kincaller) { decision ->
            decision.options shouldBe emptyList()
            driver.submitCardSelection(me, emptyList())
        }

        // "If you control another Dinosaur" is satisfied by Earthshaker Dreadmaw → gain 3 life.
        driver.getLifeTotal(me) shouldBe lifeBefore + 3
    }

    // -------------------------------------------------------------------------
    // Test 3: Neither path
    // -------------------------------------------------------------------------

    test("neither path: no Dino in hand and no other Dino on field gains no life") {
        val driver = newDriver()
        val me = driver.player1

        // No Dinosaurs anywhere: hand has only Forest cards, battlefield is empty
        // (Kincaller itself enters but is excluded from the "another Dinosaur" check).
        val kincaller = driver.putCardInHand(me, "Armored Kincaller")
        val lifeBefore = driver.getLifeTotal(me)

        // The reveal is asked even with no Dino in hand (#3340); nothing can be revealed.
        driver.castKincallerAndDrain(me, kincaller) { decision ->
            decision.options shouldBe emptyList()
            driver.submitCardSelection(me, emptyList())
        }

        // Neither condition is satisfied → no life gain.
        driver.getLifeTotal(me) shouldBe lifeBefore
    }
})
