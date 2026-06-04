package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Divination
import com.wingedsheep.mtg.sets.definitions.ltr.cards.StalwartsOfOsgiliath
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stalwarts of Osgiliath (LTR #33)
 * {4}{W} Creature — Human Soldier 4/3
 * When this creature enters, the Ring tempts you.
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 */
class StalwartsOfOsgiliathTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StalwartsOfOsgiliath, Divination))
        return driver
    }

    test("ETB ability: the Ring tempts you") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stalwarts = driver.putCardInHand(p1, "Stalwarts of Osgiliath")
        driver.giveMana(p1, Color.WHITE, 1)
        driver.giveColorlessMana(p1, 4)
        driver.castSpell(p1, stalwarts)
        // Resolve Stalwarts → ETB trigger goes on the stack and asks for a Ring-bearer.
        driver.bothPass()
        driver.bothPass()

        // Only one creature on the battlefield — Stalwarts itself becomes the Ring-bearer.
        val decision = driver.pendingDecision
        if (decision is SelectCardsDecision) {
            driver.submitDecision(p1, CardsSelectedResponse(decision.id, listOf(stalwarts)))
        }
        driver.bothPass()

        driver.state.getEntity(stalwarts)?.get<RingBearerComponent>()?.ownerId shouldBe p1
    }

    test("gets a +1/+1 counter when controller draws their second card via Divination") {
        // Drop Stalwarts directly on the battlefield so the ETB Ring-tempt trigger
        // doesn't fire (this test exercises only the draw trigger).
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 10, "Divination" to 10))

        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val stalwarts = driver.putCreatureOnBattlefield(p1, "Stalwarts of Osgiliath")

        val divination = driver.putCardInHand(p1, "Divination")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, divination)
        driver.bothPass()
        driver.bothPass()

        driver.state.getEntity(stalwarts)?.get<CountersComponent>()
            ?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("does not get a counter when only one card is drawn this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val stalwarts = driver.putCreatureOnBattlefield(p1, "Stalwarts of Osgiliath")

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        val counters = driver.state.getEntity(stalwarts)?.get<CountersComponent>()
        counters?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
    }
})
