package com.wingedsheep.engine.mechanics.counters

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Loyalty counters leaving a planeswalker are an ordinary counter removal: damage dealt to it
 * (CR 120.3c) and a [−N] / [−X] loyalty cost (CR 606.4) both emit `CountersRemovedEvent(LOYALTY)`,
 * so `Triggers.self.losesCounters(CounterType.LOYALTY)` ("whenever one or more loyalty counters are
 * removed from ~", Chandra, Fire Artisan) sees each of them. Combat damage from several attackers is
 * one simultaneous removal (CR 510.2), so it fires once with the total.
 */
class LoyaltyCountersRemovedTest : FunSpec({

    // "Whenever one or more loyalty counters are removed from this, you gain that much life."
    val walker = card("Test Ember Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = ""
        triggeredAbility {
            trigger = Triggers.self.losesCounters(CounterType.LOYALTY)
            effect = Effects.GainLife(DynamicAmounts.triggerCountersRemoved())
        }
        loyaltyAbility(+1) { effect = Effects.DrawCards(1) }
        loyaltyAbility(0) { effect = Effects.DrawCards(1) }
        loyaltyAbility(-2) { effect = Effects.DrawCards(1) }
    }

    val unmake = card("Test Unmake Walker") {
        manaCost = "{B}"
        typeLine = "Instant"
        oracleText = "Destroy target creature or planeswalker."
        spell {
            val pw = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.Destroy(pw)
        }
    }

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(walker, unmake))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.walkerWith(owner: EntityId, loyalty: Int): EntityId =
        putPermanentOnBattlefield(owner, "Test Ember Walker").also { pw ->
            replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) })
        }

    fun GameTestDriver.loyalty(pw: EntityId): Int =
        state.getEntity(pw)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    fun List<GameEvent>.loyaltyRemovals(pw: EntityId) =
        filterIsInstance<CountersRemovedEvent>().filter { it.entityId == pw && it.counterType == CounterType.LOYALTY }

    fun GameTestDriver.activate(pw: EntityId, index: Int) =
        submitSuccess(ActivateAbility(player1, pw, walker.script.activatedAbilities[index].id))

    test("noncombat damage removes loyalty as a counter removal and fires the trigger with the amount removed") {
        val d = newDriver()
        val pw = d.walkerWith(d.player1, 5)
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.giveMana(d.player2, Color.RED, 1)
        d.passPriority(d.player1)
        val cast = d.castSpellWithTargets(d.player2, bolt, listOf(ChosenTarget.Permanent(pw)))
        cast.error shouldBe null
        d.bothPass()

        d.loyalty(pw) shouldBe 2
        val removal = d.events.loyaltyRemovals(pw).single()
        removal.amount shouldBe 3
        removal.remainingCount shouldBe 2
        withClue("damage removes counters no player removed") { removal.removedBy shouldBe null }
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("damage beyond loyalty reports only the counters removed, and the trigger fires after the walker dies") {
        val d = newDriver()
        val pw = d.walkerWith(d.player1, 2)
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.giveMana(d.player2, Color.RED, 1)
        d.passPriority(d.player1)
        d.castSpellWithTargets(d.player2, bolt, listOf(ChosenTarget.Permanent(pw))).error shouldBe null
        d.bothPass()

        d.state.getBattlefield().contains(pw) shouldBe false
        d.events.loyaltyRemovals(pw).single().amount shouldBe 2
        d.bothPass()
        withClue("the trigger still resolves for the two counters removed") {
            d.getLifeTotal(d.player1) shouldBe 22
        }
    }

    test("a minus loyalty cost removes counters paid by the activator; plus and zero costs remove none") {
        val d = newDriver()
        val pw = d.walkerWith(d.player1, 5)

        val minus = d.activate(pw, 2)
        val removal = minus.events.loyaltyRemovals(pw).single()
        removal.amount shouldBe 2
        removal.remainingCount shouldBe 3
        removal.removedBy shouldBe d.player1
        d.bothPass() // the trigger
        d.getLifeTotal(d.player1) shouldBe 22
        d.bothPass() // the ability

        val d2 = newDriver()
        val pw2 = d2.walkerWith(d2.player1, 5)
        d2.activate(pw2, 0).events.loyaltyRemovals(pw2) shouldBe emptyList()
        val d3 = newDriver()
        val pw3 = d3.walkerWith(d3.player1, 5)
        d3.activate(pw3, 1).events.loyaltyRemovals(pw3) shouldBe emptyList()
        d3.loyalty(pw3) shouldBe 5
    }

    test("combat damage from two attackers is one removal and one trigger") {
        val d = newDriver()
        val pw = d.walkerWith(d.player2, 7)
        val a = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val b = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(a)
        d.removeSummoningSickness(b)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, mapOf(a to pw, b to pw)).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)

        d.loyalty(pw) shouldBe 1
        val removal = d.events.loyaltyRemovals(pw).single()
        removal.amount shouldBe 6
        removal.remainingCount shouldBe 1
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 26
    }

    test("leaving the battlefield without losing counters doesn't fire it") {
        val d = newDriver()
        val pw = d.walkerWith(d.player1, 3)
        val doomBlade = d.putCardInHand(d.player2, "Test Unmake Walker")
        d.giveMana(d.player2, Color.BLACK, 1)
        d.passPriority(d.player1)
        d.castSpellWithTargets(d.player2, doomBlade, listOf(ChosenTarget.Permanent(pw))).error shouldBe null
        d.bothPass()

        d.state.getBattlefield().contains(pw) shouldBe false
        d.events.loyaltyRemovals(pw) shouldBe emptyList()
        d.state.stack.size shouldBe 0
    }
})
