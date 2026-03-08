package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.Kurgadon
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TitanicBulvox
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Kurgadon:
 * {4}{G} Creature — Beast 3/3
 * Whenever you cast a creature spell with mana value 6 or greater,
 * put three +1/+1 counters on Kurgadon.
 */
class KurgadonTest : FunSpec({

    val allCards = TestCards.all + listOf(Kurgadon, TitanicBulvox)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("casting face-down morph with high CMC does not trigger Kurgadon") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Kurgadon on battlefield
        val kurgadonId = driver.putCreatureOnBattlefield(activePlayer, "Kurgadon")

        // Put Titanic Bulvox (CMC 8) in hand
        val bulvoxCard = driver.putCardInHand(activePlayer, "Titanic Bulvox")

        // Add enough mana to cast face-down ({3})
        driver.giveColorlessMana(activePlayer, 3)

        // Cast Titanic Bulvox face-down as a morph (costs {3}, mana value should be 0)
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bulvoxCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error == null) shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Kurgadon should NOT have any counters — face-down spell has mana value 0
        val counters = driver.state.getEntity(kurgadonId)?.get<CountersComponent>()
        val plusCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusCounters shouldBe 0
    }

    test("casting creature with CMC 6 or greater triggers Kurgadon") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Kurgadon on battlefield
        val kurgadonId = driver.putCreatureOnBattlefield(activePlayer, "Kurgadon")

        // Put Titanic Bulvox (CMC 8) in hand
        val bulvoxCard = driver.putCardInHand(activePlayer, "Titanic Bulvox")

        // Add enough mana to cast normally ({6}{G}{G})
        driver.giveColorlessMana(activePlayer, 6)
        driver.giveMana(activePlayer, com.wingedsheep.sdk.core.Color.GREEN, 2)

        // Cast Titanic Bulvox face-up (normal cast)
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bulvoxCard,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error == null) shouldBe true

        // Resolve the spell (trigger goes on stack after spell resolves)
        driver.bothPass()

        // Resolve Kurgadon's triggered ability
        driver.bothPass()

        // Kurgadon should have 3 +1/+1 counters
        val counters = driver.state.getEntity(kurgadonId)?.get<CountersComponent>()
        val plusCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusCounters shouldBe 3
    }
})
