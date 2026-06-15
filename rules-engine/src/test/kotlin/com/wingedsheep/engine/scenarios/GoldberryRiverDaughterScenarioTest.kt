package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GoldberryRiverDaughter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Goldberry, River-Daughter ({1}{U} Legendary Creature — Nymph, 1/3)
 *
 * Ability A — {T}: Move a counter of each kind not on Goldberry from another target permanent
 *   you control onto Goldberry.
 * Ability B — {U}, {T}: Move one or more counters from Goldberry onto another target permanent
 *   you control. If you do, draw a card.
 */
class GoldberryRiverDaughterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoldberryRiverDaughter))
        return driver
    }

    fun addCounters(driver: GameTestDriver, entityId: EntityId, type: CounterType, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        }
        driver.replaceState(newState)
    }

    fun count(driver: GameTestDriver, entityId: EntityId, type: CounterType): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("Ability A moves one counter of each kind Goldberry lacks; a kind it already has is not moved") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val goldberry = driver.putCreatureOnBattlefield(active, "Goldberry, River-Daughter")
        driver.removeSummoningSickness(goldberry)
        val target = driver.putCreatureOnBattlefield(active, "Centaur Courser")

        // Target has +1/+1 and charge; Goldberry already has a charge counter.
        addCounters(driver, target, CounterType.PLUS_ONE_PLUS_ONE, 1)
        addCounters(driver, target, CounterType.CHARGE, 1)
        addCounters(driver, goldberry, CounterType.CHARGE, 1)

        val abilityA = GoldberryRiverDaughter.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = goldberry,
                abilityId = abilityA,
                targets = listOf(entityIdToChosenTarget(driver.state, target))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Goldberry gained a +1/+1 (it lacked it) but NOT another charge (already had one).
        count(driver, goldberry, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        count(driver, goldberry, CounterType.CHARGE) shouldBe 1

        // The bear lost the moved +1/+1 but kept its charge (that kind was not moved).
        count(driver, target, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        count(driver, target, CounterType.CHARGE) shouldBe 1
    }

    test("Ability B moves chosen counters from Goldberry to another permanent and draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val goldberry = driver.putCreatureOnBattlefield(active, "Goldberry, River-Daughter")
        driver.removeSummoningSickness(goldberry)
        val target = driver.putCreatureOnBattlefield(active, "Centaur Courser")

        // Goldberry has two +1/+1 counters to move.
        addCounters(driver, goldberry, CounterType.PLUS_ONE_PLUS_ONE, 2)
        driver.giveMana(active, Color.BLUE, 1)

        val handBefore = driver.getHandSize(active)

        val abilityB = GoldberryRiverDaughter.activatedAbilities[1].id
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = goldberry,
                abilityId = abilityB,
                targets = listOf(entityIdToChosenTarget(driver.state, target))
            )
        )
        activateResult.isSuccess shouldBe true
        driver.bothPass()

        // Engine prompts how many +1/+1 counters to move. Move both.
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseNumberDecision>()
        driver.submitDecision(active, NumberChosenResponse(decision.id, 2))

        // Both counters moved onto the bear; Goldberry has none left.
        count(driver, target, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        count(driver, goldberry, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0

        // Drew a card because counters were moved.
        driver.getHandSize(active) shouldBe (handBefore + 1)
    }
})
