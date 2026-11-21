package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.UNLEASH_MODE_COUNTER
import com.wingedsheep.sdk.dsl.UNLEASH_MODE_NONE
import com.wingedsheep.sdk.dsl.UNLEASH_MODE_OPTIONS
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unleash (CR 702.98a): "You may have this permanent enter with an additional +1/+1 counter on it"
 * and "This permanent can't block as long as it has a +1/+1 counter on it."
 *
 * Pins both halves: the optional entry counter is the controller's choice as the permanent enters,
 * and the blocking restriction reads the permanent's +1/+1 counters, whatever put them there — not
 * the entry choice — so it lifts when they're gone and applies to counters from other sources.
 */
class UnleashTest : FunSpec({

    val unleasher = card("Test Unleasher") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        unleash()
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(unleasher))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castUnleasher(me: EntityId, unleash: Boolean): EntityId {
        val id = putCardInHand(me, "Test Unleasher")
        giveMana(me, Color.RED, 2)
        castSpell(me, id).error shouldBe null
        bothPass()
        val pick = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("the unleash choice is the controller's") { pick.playerId shouldBe me }
        pick.options.size shouldBe 2
        submitDecision(me, OptionChosenResponse(pick.id, if (unleash) 0 else 1)).error shouldBe null
        return id
    }

    test("the keyword is displayed and the choice offers unleash or not") {
        unleasher.keywords shouldContain Keyword.UNLEASH
        UNLEASH_MODE_OPTIONS.map { it.id } shouldBe listOf(UNLEASH_MODE_COUNTER, UNLEASH_MODE_NONE)
    }

    test("unleashed: it enters with a +1/+1 counter and can't block") {
        val d = driver()
        val id = d.castUnleasher(d.player1, unleash = true)
        d.state.getBattlefield() shouldContain id
        d.plusOneCounters(id) shouldBe 1
        d.state.projectedState.getPower(id) shouldBe 3
        d.state.projectedState.cantBlock(id) shouldBe true
    }

    test("not unleashed: it enters without a counter and can block") {
        val d = driver()
        val id = d.castUnleasher(d.player1, unleash = false)
        d.state.getBattlefield() shouldContain id
        d.plusOneCounters(id) shouldBe 0
        d.state.projectedState.getPower(id) shouldBe 2
        d.state.projectedState.cantBlock(id) shouldBe false
    }

    test("once its +1/+1 counters are gone an unleashed creature can block again") {
        val d = driver()
        val id = d.castUnleasher(d.player1, unleash = true)
        d.state.projectedState.cantBlock(id) shouldBe true
        d.addComponent(id, CountersComponent())
        d.state.projectedState.cantBlock(id) shouldBe false
    }

    test("a +1/+1 counter from any other source also stops it blocking") {
        val d = driver()
        val id = d.castUnleasher(d.player1, unleash = false)
        d.state.projectedState.cantBlock(id) shouldBe false
        d.addComponent(id, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        d.state.projectedState.cantBlock(id) shouldBe true
    }

    test("other counter kinds don't stop it blocking") {
        val d = driver()
        val id = d.castUnleasher(d.player1, unleash = false)
        d.addComponent(id, CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
        d.state.projectedState.cantBlock(id) shouldBe false
    }
})
