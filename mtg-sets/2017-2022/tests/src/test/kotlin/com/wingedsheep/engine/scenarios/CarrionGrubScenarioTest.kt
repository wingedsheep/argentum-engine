package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.CarrionGrub
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Carrion Grub — {3}{B} Creature — Insect 0/5
 * This creature gets +X/+0, where X is the greatest power among creature cards in your graveyard.
 * When this creature enters, mill four cards.
 *
 * Pins the static max-power read over the controller's graveyard: only creature cards count, only
 * your own graveyard counts, and an empty graveyard leaves X at 0.
 */
class CarrionGrubScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + CarrionGrub)
        initMirrorMatch(deck = Deck.of("Mountain" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("power is 0 with no creature cards in your graveyard") {
        val d = driver()
        val grub = d.putCreatureOnBattlefield(d.player1, CarrionGrub.name)
        d.putCardInGraveyard(d.player1, "Lightning Bolt")

        d.state.projectedState.getPower(grub) shouldBe 0
        d.state.projectedState.getToughness(grub) shouldBe 5
    }

    test("gets +X/+0 for the greatest power among creature cards in your graveyard only") {
        val d = driver()
        val grub = d.putCreatureOnBattlefield(d.player1, CarrionGrub.name)
        d.putCardInGraveyard(d.player1, "Savannah Lions")
        d.putCardInGraveyard(d.player1, "Centaur Courser")
        // An opponent's bigger creature card doesn't count.
        d.putCardInGraveyard(d.player2, "Force of Nature")

        d.state.projectedState.getPower(grub) shouldBe 3
        d.state.projectedState.getToughness(grub) shouldBe 5

        d.putCardInGraveyard(d.player1, "Force of Nature")
        d.state.projectedState.getPower(grub) shouldBe 5
    }
})
