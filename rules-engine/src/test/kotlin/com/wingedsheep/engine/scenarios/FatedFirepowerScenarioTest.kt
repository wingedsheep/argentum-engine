package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.FatedFirepower
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fated Firepower (TLA #132).
 *
 * {X}{R}{R}{R} Enchantment
 * Flash
 * This enchantment enters with X fire counters on it.
 * If a source you control would deal damage to an opponent or a permanent an opponent
 * controls, it deals that much damage plus an amount of damage equal to the number of
 * fire counters on this enchantment instead.
 *
 * Exercises (a) the enters-with-X-fire-counters reuse of [CounterType.FIRE]
 * (EntersWithDynamicCounters + XValue), and (b) the new dynamic outgoing-damage
 * amplification (ModifyDamageAmount.dynamicModifier scoped to
 * SourceFilter.YouControl → RecipientFilter.OpponentOrPermanentTheyControl).
 */
class FatedFirepowerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FatedFirepower))
        return driver
    }

    /** Put Fated Firepower onto the active player's battlefield with [fireCounters] fire counters. */
    fun GameTestDriver.fatedFirepowerWith(active: com.wingedsheep.sdk.model.EntityId, fireCounters: Int): com.wingedsheep.sdk.model.EntityId {
        val firepower = putPermanentOnBattlefield(active, "Fated Firepower")
        replaceState(state.updateEntity(firepower) {
            it.with(CountersComponent(mapOf(CounterType.FIRE to fireCounters)))
        })
        return firepower
    }

    test("cast with X=2 enters with two fire counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(active, "Fated Firepower")
        // {X}{R}{R}{R} with X=2 → 5 mana, red covers the generic.
        driver.giveMana(active, Color.RED, 5)
        driver.castXSpell(active, card, xValue = 2).error shouldBe null
        driver.bothPass()

        val firepower = driver.state.getBattlefield().first { id ->
            driver.state.getEntity(id)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Fated Firepower"
        }
        withClue("enters with X = 2 fire counters") {
            driver.state.getEntity(firepower)?.get<CountersComponent>()
                ?.getCount(CounterType.FIRE) shouldBe 2
        }
    }

    test("a creature you control deals combat damage to an opponent plus the fire-counter count") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.fatedFirepowerWith(active, fireCounters = 2)
        val courser = driver.putCreatureOnBattlefield(active, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(courser)

        val lifeBefore = driver.getLifeTotal(opponent)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(courser), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }

        withClue("3 power + 2 fire counters = 5 combat damage to the opponent") {
            driver.getLifeTotal(opponent) shouldBe (lifeBefore - 5)
        }
    }

    test("amplification scope: source you control to an opponent or their permanent, not yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.fatedFirepowerWith(active, fireCounters = 2)
        val myCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val myOtherCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        withClue("your source dealing 3 to an opponent → 3 + 2 = 5") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = opponent, amount = 3, sourceId = myCreature
            ) shouldBe 5
        }
        withClue("your source dealing 3 to a permanent an opponent controls → 3 + 2 = 5") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = theirCreature, amount = 3, sourceId = myCreature
            ) shouldBe 5
        }
        withClue("your source dealing 3 to your OWN creature is NOT amplified") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = myOtherCreature, amount = 3, sourceId = myCreature
            ) shouldBe 3
        }
        withClue("an opponent's source dealing 3 to you is NOT amplified") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = active, amount = 3, sourceId = theirCreature
            ) shouldBe 3
        }
    }

    test("the bonus tracks the current number of fire counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val firepower = driver.fatedFirepowerWith(active, fireCounters = 3)
        val myCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")

        withClue("3 fire counters → 3 + 3 = 6") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = opponent, amount = 3, sourceId = myCreature
            ) shouldBe 6
        }

        driver.replaceState(driver.state.updateEntity(firepower) {
            it.with(CountersComponent(mapOf(CounterType.FIRE to 1)))
        })
        withClue("1 fire counter → 3 + 1 = 4") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = opponent, amount = 3, sourceId = myCreature
            ) shouldBe 4
        }
    }
})
