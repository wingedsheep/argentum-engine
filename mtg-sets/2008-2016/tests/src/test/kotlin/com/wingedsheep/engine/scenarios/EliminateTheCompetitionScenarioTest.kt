package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.kld.cards.EliminateTheCompetition
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Eliminate the Competition {4}{B} — "As an additional cost to cast this spell, sacrifice X
 * creatures. Destroy X target creatures."
 *
 * The card announces X through its sacrifice cost rather than its mana cost: the cast offer carries
 * an X capped at the creatures the caster could sacrifice, the X sizes the targets, and the payment
 * must sacrifice exactly that many.
 */
class EliminateTheCompetitionScenarioTest : FunSpec({

    test("sacrifices two creatures to destroy two of the opponent's") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EliminateTheCompetition)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        val fodder = List(2) { driver.putCreatureOnBattlefield(caster, "Grizzly Bears") }
        val giants = List(3) { driver.putCreatureOnBattlefield(opponent, "Hill Giant") }
        val spell = driver.putCardInHand(caster, "Eliminate the Competition")
        driver.giveMana(caster, Color.BLACK, 5)

        val offer = driver.legalActions(caster).single { (it.action as? CastSpell)?.cardId == spell }
        offer.hasXCost shouldBe true
        offer.maxAffordableX shouldBe 2

        fun cast(sacrificed: Int, targets: Int) = driver.submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = giants.take(targets).map { ChosenTarget.Permanent(it) },
                xValue = 2,
                paymentStrategy = PaymentStrategy.FromPool,
                additionalCostPayment = AdditionalCostPayment(variableCostPermanents = fodder.take(sacrificed)),
            )
        )

        // X = 2 needs exactly two sacrifices and exactly two targets.
        cast(sacrificed = 1, targets = 2).error shouldNotBe null
        cast(sacrificed = 2, targets = 1).error shouldNotBe null
        cast(sacrificed = 2, targets = 2).error shouldBe null

        fodder.none { it in driver.state.getBattlefield() } shouldBe true
        driver.bothPass()

        val battlefield = driver.state.getBattlefield()
        giants.take(2).none { it in battlefield } shouldBe true
        battlefield shouldContain giants[2]
        driver.state.getZone(ZoneKey(caster, Zone.GRAVEYARD)) shouldContain spell
    }
})
