package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Nullpriest of Oblivion (ZNR #118, reprinted in FDN #611) — {1}{B} 2/1 Vampire Cleric with
 * Kicker {3}{B}, Lifelink, Menace, and "When this creature enters, if it was kicked, return target
 * creature card from your graveyard to the battlefield."
 *
 * Proves the intervening "if" (CR 603.4): the ETB reanimation only happens when the spell was
 * kicked. Kicked → a graveyard creature is returned to the battlefield; not kicked → no trigger, so
 * no target is even requested and the graveyard is untouched.
 */
class NullpriestOfOblivionScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30))
        return d
    }

    test("kicked: the ETB returns a creature card from your graveyard to the battlefield") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardInGraveyard(you, "Centaur Courser")
        val nullpriest = d.putCardInHand(you, "Nullpriest of Oblivion")
        // {1}{B} + kicker {3}{B} = {4}{B}{B} → six Swamps cover it.
        repeat(6) { d.putLandOnBattlefield(you, "Swamp") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = nullpriest,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).isSuccess shouldBe true
        d.bothPass() // resolve the creature; the kicked ETB trigger goes on the stack and wants a target

        val courser = d.getGraveyard(you).first {
            d.state.getEntity(it)?.get<CardComponent>()?.name == "Centaur Courser"
        }
        d.pendingDecision.shouldNotBeNull()
        d.submitTargetSelection(you, listOf(courser))
        while (d.stackSize > 0) d.bothPass()

        d.findPermanent(you, "Centaur Courser").shouldNotBeNull()
    }

    test("not kicked: no ETB trigger fires and the graveyard creature stays put") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardInGraveyard(you, "Centaur Courser")
        val nullpriest = d.putCardInHand(you, "Nullpriest of Oblivion")
        // Base cost {1}{B} only.
        repeat(2) { d.putLandOnBattlefield(you, "Swamp") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = nullpriest,
                declaredCostSlot = null,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).isSuccess shouldBe true
        d.bothPass() // resolve the creature; the intervening "if" is false, so nothing triggers

        d.pendingDecision.shouldBeNull()
        d.findPermanent(you, "Centaur Courser").shouldBeNull()
    }
})
