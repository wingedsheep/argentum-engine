package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.ExtusOriqOverlord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Extus, Oriq Overlord // Awaken the Blood Avatar (STX #149).
 *
 * The load-bearing claim is the back face's "costs {2} less for each creature sacrificed this
 * way": the hand enumeration must count that discount when it judges the modal back affordable,
 * exactly as the primary-face path does for Torgaar.
 */
class ExtusOriqOverlordScenarioTest : FunSpec({

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ExtusOriqOverlord))
        initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Awaken the Blood Avatar is offered affordable when sacrifices cover the gap") {
        val d = setup()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val extus = d.putCardInHand(me, "Extus, Oriq Overlord")
        // {6}{B}{R} less {4} for two sacrifices = {2}{B}{R}.
        d.giveMana(me, Color.BLACK, 3)
        d.giveMana(me, Color.RED, 1)

        val back = d.legalActions(me).single {
            val cast = it.action as? CastSpell
            cast?.cardId == extus && cast.faceIndex == 0
        }
        withClue("the back face should be castable once its discount is counted") {
            back.affordable shouldBe true
        }
        back.manaCostString shouldBe "{2}{B}{R}"
    }

    test("sacrificing two creatures casts it for {2}{B}{R}; opponent sacrifices and an Avatar appears") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bear1 = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bear2 = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val extus = d.putCardInHand(me, "Extus, Oriq Overlord")
        d.giveMana(me, Color.BLACK, 3)
        d.giveMana(me, Color.RED, 1)

        val cast = d.submit(
            CastSpell(
                playerId = me,
                cardId = extus,
                faceIndex = 0,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear1, bear2)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        withClue("cast should succeed: ${cast.error}") { cast.outcome shouldBe Outcome.Done }
        d.findPermanent(me, "Grizzly Bears") shouldBe null

        d.bothPass()

        d.findPermanent(opp, "Grizzly Bears") shouldBe null
        val avatar = d.findPermanent(me, "Avatar Token")
        avatar shouldNotBe null
        d.state.projectedState.getPower(avatar!!) shouldBe 3
        d.state.projectedState.getToughness(avatar) shouldBe 6
    }
})
