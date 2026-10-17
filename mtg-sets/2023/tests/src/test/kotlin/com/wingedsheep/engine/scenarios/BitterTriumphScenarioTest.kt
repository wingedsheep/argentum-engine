package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.BitterTriumph
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bitter Triumph (LCI #91) — {1}{B} Instant.
 *
 * "As an additional cost to cast this spell, discard a card or pay 3 life.
 *  Destroy target creature or planeswalker."
 *
 * The cost is a cost-vs-cost choice, not a mode: each branch is its own cast offer and pays only
 * that branch.
 */
class BitterTriumphScenarioTest : FunSpec({
    val slot = ChoiceSlot.ADDITIONAL_COST_BRANCH

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + BitterTriumph)
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.offer(spell: EntityId, branch: Int) =
        legalActions(activePlayer!!).single {
            (it.action as? CastSpell)?.cardId == spell &&
                (it.action as CastSpell).additionalCostChoices[slot] == branch
        }.action as CastSpell

    test("paying 3 life destroys the target and keeps the hand") {
        val d = setup()
        val p = d.activePlayer!!
        val opponent = d.state.turnOrder.first { it != p }
        val victim = d.putPermanentOnBattlefield(opponent, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Bitter Triumph")
        d.giveMana(p, Color.BLACK, 2)
        val handBefore = d.state.getHand(p).size
        d.submit(d.offer(spell, 1).copy(
            targets = listOf(ChosenTarget.Permanent(victim)),
            paymentStrategy = PaymentStrategy.FromPool,
        )).error shouldBe null
        d.getLifeTotal(p) shouldBe 17
        d.state.getHand(p).size shouldBe handBefore - 1
        d.bothPass()
        (victim in d.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD))) shouldBe true
    }

    test("discarding a card pays the cost with no life lost") {
        val d = setup()
        val p = d.activePlayer!!
        val opponent = d.state.turnOrder.first { it != p }
        val victim = d.putPermanentOnBattlefield(opponent, "Grizzly Bears")
        val fodder = d.putCardInHand(p, "Swamp")
        val spell = d.putCardInHand(p, "Bitter Triumph")
        d.giveMana(p, Color.BLACK, 2)
        d.submit(d.offer(spell, 0).copy(
            targets = listOf(ChosenTarget.Permanent(victim)),
            additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
            paymentStrategy = PaymentStrategy.FromPool,
        )).error shouldBe null
        (fodder in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
        d.getLifeTotal(p) shouldBe 20
        d.bothPass()
        (victim in d.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD))) shouldBe true
    }
})
