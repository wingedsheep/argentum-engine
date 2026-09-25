package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.SoratamiMirrorMage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Soratami Mirror-Mage (CHK) — "{3}, Return three lands you control to their owner's hand: Return
 * target creature to its owner's hand."
 */
class SoratamiMirrorMageScenarioTest : FunSpec({

    val abilityId = SoratamiMirrorMage.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SoratamiMirrorMage)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("returns three lands as the cost and bounces the target creature to its owner's hand") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val mage = d.putCreatureOnBattlefield(p1, "Soratami Mirror-Mage")
        val lands = List(3) { d.putLandOnBattlefield(p1, "Island") }
        val bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        d.giveColorlessMana(p1, 3)

        d.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = mage,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = AdditionalCostPayment(bouncedPermanents = lands),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.getHand(p1) shouldContainAll lands
        d.bothPass()

        d.getHand(p2) shouldContain bears
        d.findPermanent(p2, "Grizzly Bears") shouldBe null
    }

    test("cannot be activated with only two lands to return") {
        val d = driver()
        val p1 = d.player1
        val mage = d.putCreatureOnBattlefield(p1, "Soratami Mirror-Mage")
        val lands = List(2) { d.putLandOnBattlefield(p1, "Island") }
        val bears = d.putCreatureOnBattlefield(d.getOpponent(p1), "Grizzly Bears")
        d.giveColorlessMana(p1, 3)

        d.submitExpectFailure(
            ActivateAbility(
                playerId = p1,
                sourceId = mage,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
                costPayment = AdditionalCostPayment(bouncedPermanents = lands),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.getLands(p1).containsAll(lands) shouldBe true
    }
})
