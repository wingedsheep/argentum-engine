package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.RisonaAsariCommander
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Risona, Asari Commander (NEO #233) — {1}{R}{W} Legendary Creature — Human Samurai, 3/3.
 *
 *   Haste
 *   Whenever Risona deals combat damage to a player, if it doesn't have an indestructible counter
 *   on it, put an indestructible counter on it.
 *   Whenever combat damage is dealt to you, remove an indestructible counter from Risona.
 *
 * Covers the counter gain (and the intervening "if" holding it to one), the counter loss when an
 * attacker that died to the same combat damage hit you (last-known information), and the ruling
 * that losing the counter lets lethal damage Risona took while blocking finish it off.
 */
class RisonaAsariCommanderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RisonaAsariCommander))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.indestructibleCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.INDESTRUCTIBLE) ?: 0

    fun GameTestDriver.giveIndestructibleCounter(id: EntityId) = replaceState(
        state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.INDESTRUCTIBLE, 1)) }
    )

    test("dealing combat damage to a player gives Risona one indestructible counter, never two") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val risona = driver.putCreatureOnBattlefield(active, "Risona, Asari Commander")

        // Haste: attacks the turn it arrives.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(risona), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(opponent, 17)
        driver.indestructibleCounters(risona) shouldBe 1
        driver.state.projectedState.hasKeyword(risona, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("the intervening if stops a second counter when Risona already has one") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val risona = driver.putCreatureOnBattlefield(active, "Risona, Asari Commander")
        driver.giveIndestructibleCounter(risona)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(risona), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(opponent, 17)
        driver.indestructibleCounters(risona) shouldBe 1
    }

    test("an attacker that died to the same combat damage still removes the counter") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val risona = driver.putCreatureOnBattlefield(defender, "Risona, Asari Commander")
        driver.giveIndestructibleCounter(risona)
        val rat = driver.putCreatureOnBattlefield(defender, "Deathtouch Rat")
        val beast = driver.putCreatureOnBattlefield(attacker, "Trample Beast")
        driver.removeSummoningSickness(beast)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(beast), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(rat to listOf(beast))).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // The Beast tramples 4 over the Rat and dies to its deathtouch in the same damage step.
        driver.assertLifeTotal(defender, 16)
        driver.getGraveyardCardNames(attacker) shouldContain "Trample Beast"
        driver.indestructibleCounters(risona) shouldBe 0
    }

    test("losing the counter lets lethal damage from blocking destroy Risona") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val risona = driver.putCreatureOnBattlefield(defender, "Risona, Asari Commander")
        driver.giveIndestructibleCounter(risona)
        val courser = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(courser)
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(courser, bears), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(risona to listOf(courser))).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(defender, 18)
        driver.getGraveyardCardNames(defender) shouldContain "Risona, Asari Commander"
    }
})
