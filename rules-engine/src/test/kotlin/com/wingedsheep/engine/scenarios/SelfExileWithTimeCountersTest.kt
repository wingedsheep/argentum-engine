package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `spell { selfExile(timeCounters = n) }` — "Exile <card name> with N time counters on it."
 * (Arc Blade, Suspended Sentence, the Time Spiral Remastered / Commander "Refrain" cycle). The card
 * exiles itself as the last part of its resolution (CR 608.2n's destination replaced), with N time
 * counters, and — having suspend — is suspended (CR 702.62b), so its owner's upkeeps count it
 * down and it can be cast again for free, exiling itself once more.
 */
class SelfExileWithTimeCountersTest : FunSpec({

    val recurringBoon = card("Recurring Boon") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.suspend("{W}", 3))
        spell {
            effect = Effects.GainLife(2)
            selfExile(timeCounters = 3)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(recurringBoon))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun timeCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    fun castBoon(driver: GameTestDriver, player: EntityId): EntityId {
        val boon = driver.putCardInHand(player, "Recurring Boon")
        driver.giveMana(player, Color.WHITE, 2)
        driver.castSpell(player, boon)
        return boon
    }

    fun resolveNextOwnerUpkeep(driver: GameTestDriver, owner: EntityId) {
        do {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            driver.passPriorityUntil(Step.UPKEEP)
        } while (driver.activePlayer != owner)
        driver.bothPass() // resolve the suspend countdown trigger
    }

    test("resolving, the spell exiles itself with N time counters and is suspended") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val boon = castBoon(driver, player)
        driver.bothPass()

        driver.getLifeTotal(player) shouldBe 22
        driver.getExile(player).contains(boon) shouldBe true
        driver.getGraveyard(player).contains(boon) shouldBe false
        timeCounters(driver, boon) shouldBe 3
        driver.state.getEntity(boon)?.has<SuspendedComponent>() shouldBe true
        driver.events.filterIsInstance<CountersAddedEvent>()
            .any { it.entityId == boon && it.counterType == CounterType.TIME && it.amount == 3 } shouldBe true
    }

    test("the owner's upkeeps count it down, it is cast free, and it exiles itself again") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val boon = castBoon(driver, player)
        driver.bothPass()

        resolveNextOwnerUpkeep(driver, player)
        timeCounters(driver, boon) shouldBe 2
        resolveNextOwnerUpkeep(driver, player)
        timeCounters(driver, boon) shouldBe 1
        resolveNextOwnerUpkeep(driver, player)
        // CR 702.62a — the last counter is gone; its owner may cast it without paying its mana cost.
        driver.bothPass() // resolve suspend's last-counter cast trigger
        driver.submitYesNo(player, true)
        driver.bothPass()

        driver.getLifeTotal(player) shouldBe 24
        // It resolved and exiled itself with three fresh time counters — suspended again.
        driver.getExile(player).contains(boon) shouldBe true
        timeCounters(driver, boon) shouldBe 3
        driver.state.getEntity(boon)?.has<SuspendedComponent>() shouldBe true
    }

    test("a countered spell never reaches its exile instruction — it goes to the graveyard with no counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val boon = castBoon(driver, player)

        driver.passPriority(player)
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(driver.getTopOfStack()!!)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.stackSize shouldBe 2
        driver.bothPass()

        driver.getLifeTotal(player) shouldBe 20
        driver.getGraveyard(player).contains(boon) shouldBe true
        timeCounters(driver, boon) shouldBe 0
        driver.state.getEntity(boon)?.has<SuspendedComponent>() shouldBe false
    }

    test("a graveyard-worded rider has nothing to replace — the card still lands suspended") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val boon = castBoon(driver, player)
        // Kylox's Voltstrider / Quistis Trepe shape: "if that spell would be put into a graveyard,
        // put it on the bottom of its owner's library instead".
        driver.replaceState(
            driver.state.updateEntity(boon) {
                it.with(AfterResolveDestinationComponent(destination = AfterResolveDestination.BOTTOM_OF_LIBRARY))
            }
        )
        driver.bothPass()

        driver.getExile(player).contains(boon) shouldBe true
        timeCounters(driver, boon) shouldBe 3
        driver.state.getEntity(boon)?.has<SuspendedComponent>() shouldBe true
    }

    test("granted rebound has nothing to replace — suspended, with no rebound recast armed") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val boon = castBoon(driver, player)
        val delayedBefore = driver.state.delayedTriggers.size
        driver.replaceState(
            driver.state.updateEntity(boon) {
                it.with(SpellGrantedKeywordsComponent(setOf(Keyword.REBOUND.name)))
            }
        )
        driver.bothPass()

        driver.getExile(player).contains(boon) shouldBe true
        timeCounters(driver, boon) shouldBe 3
        driver.state.getEntity(boon)?.has<SuspendedComponent>() shouldBe true
        driver.state.delayedTriggers.size shouldBe delayedBefore
    }

    test("the DSL rejects time counters on a card without suspend (CR 702.62b)") {
        shouldThrow<IllegalArgumentException> {
            card("Unsuspendable Boon") {
                manaCost = "{W}"
                typeLine = "Instant"
                spell {
                    effect = Effects.GainLife(2)
                    selfExile(timeCounters = 3)
                }
            }
        }
    }
})
