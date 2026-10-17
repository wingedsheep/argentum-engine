package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.ExplosiveSingularity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Explosive Singularity — {8}{R}{R} sorcery: "you may tap any number of untapped creatures you
 * control. This spell costs {1} less to cast for each creature tapped this way. Explosive
 * Singularity deals 10 damage to any target."
 */
class ExplosiveSingularityScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ExplosiveSingularity))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("tapping eight creatures leaves {R}{R} to pay, and it deals 10 damage to the opponent") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val creatures = List(8) { d.putCreatureOnBattlefield(me, "Grizzly Bears") }
        val spell = d.putCardInHand(me, "Explosive Singularity")
        d.giveMana(me, Color.RED, 2)

        val cast = d.submit(CastSpell(
            me, spell, targets = listOf(ChosenTarget.Player(opponent)),
            additionalCostPayment = AdditionalCostPayment(tappedPermanents = creatures),
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        cast.outcome shouldBe Outcome.Done
        creatures.forEach { d.isTapped(it) shouldBe true }
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 10
    }

    test("tapping fewer creatures leaves more to pay — seven creatures and {R}{R} isn't enough") {
        val d = setup()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val creatures = List(7) { d.putCreatureOnBattlefield(me, "Grizzly Bears") }
        val spell = d.putCardInHand(me, "Explosive Singularity")
        d.giveMana(me, Color.RED, 2)

        val cast = d.submit(CastSpell(
            me, spell, targets = listOf(ChosenTarget.Player(opponent)),
            additionalCostPayment = AdditionalCostPayment(tappedPermanents = creatures),
            paymentStrategy = PaymentStrategy.FromPool,
        ))
        (cast.outcome is Outcome.Rejected) shouldBe true
        creatures.forEach { d.isTapped(it) shouldBe false }
    }
})
