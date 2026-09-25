package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FlippedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.JushiApprentice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Jushi Apprentice // Tomoya the Revealer (CHK) — a flip card.
 *
 * "{2}{U}, {T}: Draw a card. If you have nine or more cards in hand, flip this creature."
 * Tomoya: "{3}{U}{U}, {T}: Target player draws X cards, where X is the number of cards in your hand."
 */
class JushiApprenticeScenarioTest : FunSpec({

    val drawAbility = JushiApprentice.activatedAbilities.single().id
    val tomoyaAbility = JushiApprentice.flipSide!!.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + JushiApprentice)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.fillHandTo(player: EntityId, size: Int) {
        while (getHandSize(player) < size) putCardInHand(player, "Island")
    }

    fun GameTestDriver.activateDraw(apprentice: EntityId) {
        giveMana(player1, Color.BLUE, 3)
        submitSuccess(
            ActivateAbility(player1, apprentice, drawAbility, paymentStrategy = PaymentStrategy.FromPool)
        )
        bothPass()
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    test("drawing up to eight cards in hand leaves it unflipped") {
        val d = driver()
        val apprentice = d.putCreatureOnBattlefield(d.player1, "Jushi Apprentice")
        d.removeSummoningSickness(apprentice)
        d.fillHandTo(d.player1, 7)

        d.activateDraw(apprentice)

        d.getHandSize(d.player1) shouldBe 8
        d.name(apprentice) shouldBe "Jushi Apprentice"
        d.state.getEntity(apprentice)!!.get<FlippedComponent>().shouldBeNull()
    }

    test("drawing to nine cards in hand flips it into Tomoya the Revealer") {
        val d = driver()
        val apprentice = d.putCreatureOnBattlefield(d.player1, "Jushi Apprentice")
        d.removeSummoningSickness(apprentice)
        d.fillHandTo(d.player1, 8)

        d.activateDraw(apprentice)

        d.getHandSize(d.player1) shouldBe 9
        d.name(apprentice) shouldBe "Tomoya the Revealer"
        d.state.getEntity(apprentice)!!.get<FlippedComponent>().shouldNotBeNull()
        withClue("legendary 2/3, still blue") {
            d.state.projectedState.getPower(apprentice) shouldBe 2
            d.state.projectedState.getToughness(apprentice) shouldBe 3
            d.state.projectedState.isLegendary(apprentice) shouldBe true
            d.state.projectedState.getColors(apprentice) shouldBe setOf(Color.BLUE.name)
        }
    }

    test("Tomoya makes the target player draw as many cards as you hold") {
        val d = driver()
        val apprentice = d.putCreatureOnBattlefield(d.player1, "Jushi Apprentice")
        d.removeSummoningSickness(apprentice)
        d.fillHandTo(d.player1, 8)
        d.activateDraw(apprentice)
        d.untapPermanent(apprentice)
        val opponentHand = d.getHandSize(d.player2)

        d.giveMana(d.player1, Color.BLUE, 5)
        d.submitSuccess(
            ActivateAbility(
                d.player1, apprentice, tomoyaAbility,
                targets = listOf(ChosenTarget.Player(d.player2)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.getHandSize(d.player2) shouldBe opponentHand + 9
    }
})
