package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.MinionOfTheMighty
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Minion of the Mighty (AFR #156) — "Pack tactics — Whenever this creature attacks, if you attacked
 * with creatures with total power 6 or greater this combat, you may put a Dragon creature card from
 * your hand onto the battlefield tapped and attacking."
 *
 * The 2021-07-23 ruling: "You choose which player or planeswalker the Dragon is attacking as you put
 * it onto the battlefield. It doesn't have to be the same player or planeswalker Minion of the Mighty
 * is attacking."
 */
class MinionOfTheMightyScenarioTest : FunSpec({

    val dragon = card("Test Hoard Dragon") {
        manaCost = "{4}{R}{R}"
        typeLine = "Creature — Dragon"
        power = 5
        toughness = 5
    }

    val walker = card("Test Minion Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 6
        oracleText = ""
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MinionOfTheMighty, dragon, walker))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.ready(name: String): EntityId =
        putCreatureOnBattlefield(player1, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 50) passPriority(priorityPlayer!!)
    }

    test("with 6 power attacking, the Dragon enters tapped and attacking the planeswalker its controller chose") {
        val d = newDriver()
        val theirWalker = d.putPermanentOnBattlefield(d.player2, "Test Minion Walker").also { pw ->
            d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5)) })
        }
        val hoard = d.putCardInHand(d.player1, "Test Hoard Dragon")
        val attackers = listOf(d.ready("Minion of the Mighty"), d.ready("Centaur Courser"), d.ready("Centaur Courser"))

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers, d.player2).error shouldBe null
        d.resolveUntilDecision()
        d.submitCardSelection(d.player1, listOf(hoard)).error shouldBe null

        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        choice.legalTargets[0]!! shouldContainExactly listOf(d.player2, theirWalker)
        d.submitTargetSelection(d.player1, listOf(theirWalker)).error shouldBe null

        d.state.getEntity(hoard)!!.get<AttackingComponent>()!!.defenderId shouldBe theirWalker
        d.state.getEntity(hoard)!!.has<TappedComponent>() shouldBe true

        d.passPriorityUntil(Step.END_COMBAT)
        d.state.getBattlefield().contains(theirWalker) shouldBe false
        d.getLifeTotal(d.player2) shouldBe 14
    }

    test("attacking with less than 6 power doesn't trigger: the Dragon stays in hand") {
        val d = newDriver()
        val hoard = d.putCardInHand(d.player1, "Test Hoard Dragon")
        val attackers = listOf(d.ready("Minion of the Mighty"), d.ready("Centaur Courser"))

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers, d.player2).error shouldBe null
        d.resolveUntilDecision()

        d.pendingDecision shouldBe null
        d.state.getHand(d.player1).contains(hoard) shouldBe true
    }
})
