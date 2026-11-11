package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.ProcessorAssault
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Processor Assault — as an additional cost, put a card an opponent owns from exile into that
 * player's graveyard; it deals 5 damage to target creature. No opponent-owned exiled card, no cast.
 */
class ProcessorAssaultScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(ProcessorAssault)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("processing an opponent's exiled card pays for 5 damage to target creature") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val assault = d.putCardInHand(alice, "Processor Assault")
        val bears = d.putPermanentOnBattlefield(bob, "Grizzly Bears")
        val exiled = d.putCardInExile(bob, "Mountain")
        d.giveMana(alice, Color.RED, 1)
        d.giveColorlessMana(alice, 1)

        d.submit(
            CastSpell(
                playerId = alice,
                cardId = assault,
                targets = listOf(ChosenTarget.Permanent(bears)),
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        d.getGraveyard(bob) shouldContain exiled
        d.bothPass()
        d.getGraveyardCardNames(bob) shouldContain "Grizzly Bears"
    }

    test("it can't be cast without a card an opponent owns in exile") {
        val d = driver()
        val alice = d.activePlayer!!
        val bob = d.getOpponent(alice)
        val assault = d.putCardInHand(alice, "Processor Assault")
        d.putPermanentOnBattlefield(bob, "Grizzly Bears")
        d.putCardInExile(alice, "Mountain")
        d.giveMana(alice, Color.RED, 1)
        d.giveColorlessMana(alice, 1)

        d.legalActions(alice).none { (it.action as? CastSpell)?.cardId == assault } shouldBe true
    }
})
