package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.BackFromTheBrink
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * A graveyard card exiled as a cost is the payoff when the effect reads it back: Back from the
 * Brink copies the card it exiled, so the AI exiles the biggest creature it can pay for, not the
 * first one the offer lists — and never one whose mana cost it can't pay.
 */
class ExiledCardPayoffPickAiTest : FunSpec({
    val cards = TestCards.all + listOf(BackFromTheBrink)

    test("Back from the Brink copies the biggest affordable creature, not the first listed") {
        val driver = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = driver.player1
        val brink = driver.putPermanentOnBattlefield(player, "Back from the Brink")
        val bears = driver.putCardInGraveyard(player, "Grizzly Bears") // {1}{G}
        val courser = driver.putCardInGraveyard(player, "Centaur Courser") // {2}{G}
        driver.putCardInGraveyard(player, "Force of Nature") // {3}{G}{G}: unaffordable
        driver.giveMana(player, Color.GREEN, 3)

        val registry = CardRegistry().apply { register(cards) }
        val activation = GameSimulator(registry).getLegalActions(driver.state, player)
            .first { (it.action as? ActivateAbility)?.sourceId == brink }
        activation.additionalCostInfo!!.validExileTargets shouldContainExactlyInAnyOrder listOf(bears, courser)

        val chosen = AIPlayer.create(registry, player)
            .chooseFrom(driver.state, listOf(activation)).action as ActivateAbility
        chosen.costPayment?.exiledCards shouldBe listOf(courser)
    }
})
