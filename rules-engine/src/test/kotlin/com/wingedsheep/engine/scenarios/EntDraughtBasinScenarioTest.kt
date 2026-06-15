package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.EntDraughtBasin
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ent-Draught Basin
 * {2} Artifact
 * {X}, {T}: Put a +1/+1 counter on target creature with power X. Activate only as a sorcery.
 *
 * Proves the new [com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerEqualsX] target
 * filter for an X-cost activated ability: with X=3 only a power-3 creature is a legal target
 * (a power-2 and a power-4 creature are rejected by activation-time validation, since X is bound
 * on the action). Also covers the +1/+1 counter payload and the sorcery-speed activation gate.
 */
class EntDraughtBasinScenarioTest : FunSpec({

    val abilityId = EntDraughtBasin.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EntDraughtBasin))
        return driver
    }

    fun addCounters(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
        driver.replaceState(newState)
    }

    fun counterCount(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /**
     * Set up a precombat main phase with Ent-Draught Basin in play (ready to tap) and three
     * creatures of power 2, 3 and 4. The power-4 creature is a 3/3 Centaur Courser with a +1/+1
     * counter so its projected power is 4. Returns (driver, activePlayer, basin, p2, p3, p4).
     */
    fun setup(): Setup {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val basin = driver.putPermanentOnBattlefield(activePlayer, "Ent-Draught Basin")
        driver.removeSummoningSickness(basin)

        val power2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")       // 2/1
        val power3 = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")    // 3/3
        val power4 = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")    // 3/3 -> 4/4
        addCounters(driver, power4, 1)

        // Pay for {X=3}: five untapped Forests to auto-tap for the {X} cost.
        repeat(5) { driver.putLandOnBattlefield(activePlayer, "Forest") }
        return Setup(driver, activePlayer, basin, power2, power3, power4)
    }

    test("X=3 cannot target a power-2 creature") {
        val s = setup()
        s.driver.submitExpectFailure(
            ActivateAbility(
                playerId = s.activePlayer,
                sourceId = s.basin,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(s.power2)),
                xValue = 3
            )
        )
    }

    test("X=3 cannot target a power-4 creature") {
        val s = setup()
        s.driver.submitExpectFailure(
            ActivateAbility(
                playerId = s.activePlayer,
                sourceId = s.basin,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(s.power4)),
                xValue = 3
            )
        )
    }

    test("X=3 can target the power-3 creature and puts a +1/+1 counter on it") {
        val s = setup()
        counterCount(s.driver, s.power3) shouldBe 0

        s.driver.submitSuccess(
            ActivateAbility(
                playerId = s.activePlayer,
                sourceId = s.basin,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(s.power3)),
                xValue = 3
            )
        )
        // Resolve the ability off the stack.
        s.driver.bothPass()

        counterCount(s.driver, s.power3) shouldBe 1
        s.driver.isTapped(s.basin) shouldBe true
    }

    test("cannot be activated at instant speed (sorcery-speed gate)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!

        // Advance into combat — not a main phase with an empty stack, so sorcery-speed is illegal.
        val basin = driver.putPermanentOnBattlefield(activePlayer, "Ent-Draught Basin")
        driver.removeSummoningSickness(basin)
        val power3 = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        repeat(5) { driver.putLandOnBattlefield(activePlayer, "Forest") }
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = basin,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(power3)),
                xValue = 3
            )
        )
    }
})

private data class Setup(
    val driver: GameTestDriver,
    val activePlayer: EntityId,
    val basin: EntityId,
    val power2: EntityId,
    val power3: EntityId,
    val power4: EntityId
)
