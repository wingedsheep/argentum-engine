package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.UyoSilentProphet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Uyo, Silent Prophet (CHK) — "{2}, Return two lands you control to their owner's hand: Copy
 * target instant or sorcery spell. You may choose new targets for the copy."
 */
class UyoSilentProphetScenarioTest : FunSpec({

    val abilityId = UyoSilentProphet.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + UyoSilentProphet)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0, startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("copies a Lightning Bolt and the copy can be aimed at a new target") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val uyo = d.putCreatureOnBattlefield(p1, "Uyo, Silent Prophet")
        val lands = listOf(d.putLandOnBattlefield(p1, "Island"), d.putLandOnBattlefield(p1, "Island"))

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p2)))
        val boltOnStack = d.getTopOfStack()!!

        d.giveColorlessMana(p1, 2)
        d.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = uyo,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                costPayment = AdditionalCostPayment(bouncedPermanents = lands),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        withClue("the two lands return to hand as the cost is paid") {
            d.getHand(p1) shouldContainAll lands
        }

        var guard = 0
        while (d.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            d.bothPass()
            guard++
        }
        (d.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        // Redirect the copy at Uyo's controller; the original still targets the opponent.
        d.submitTargetSelection(p1, listOf(p1))

        guard = 0
        while (d.stackSize > 0 && guard < 20) {
            d.bothPass()
            guard++
        }
        d.getLifeTotal(p2) shouldBe 17
        d.getLifeTotal(p1) shouldBe 17
    }

    test("cannot be activated while controlling fewer than two lands") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val uyo = d.putCreatureOnBattlefield(p1, "Uyo, Silent Prophet")
        val land = d.putLandOnBattlefield(p1, "Island")

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p2)))
        val boltOnStack = d.getTopOfStack()!!

        d.giveColorlessMana(p1, 2)
        d.submitExpectFailure(
            ActivateAbility(
                playerId = p1,
                sourceId = uyo,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(land)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.findPermanent(p1, "Island") shouldBe land
    }
})
