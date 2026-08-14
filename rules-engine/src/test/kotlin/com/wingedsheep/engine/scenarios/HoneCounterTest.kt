package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hone counters — CR 122.1j: "A hone counter on an Equipment gives +1/+0 to any creature that
 * Equipment is attached to."
 *
 * The whole point of the rule is that the bonus lives on the *counter*, not on the permanent
 * carrying it. Every test here therefore uses **Bonesplitter**, a Mirrodin Equipment that has never
 * heard of hone: if the +1/+0 shows up on the creature it equips, the engine is reading the counter
 * and not some ability printed on the card. That is what Dwalin, Weaponmaster depends on when he
 * puts a hone counter on *each* Equipment you control.
 *
 * Realized in `StateProjector.collectContinuousEffects` as a Layer 7c modification
 * (CR 613.4c — "effects **and counters** that modify power and/or toughness").
 */
class HoneCounterTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Bonesplitter)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun addCounters(driver: GameTestDriver, entityId: EntityId, type: CounterType, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        }
        driver.replaceState(newState)
    }

    fun equip(driver: GameTestDriver, player: EntityId, equipment: EntityId, creature: EntityId) {
        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(
                player,
                equipment,
                Bonesplitter.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe creature
    }

    test("a hone counter on an equipped Equipment gives the equipped creature +1/+0") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        equip(driver, you, sword, courser)

        // Bonesplitter's own +2/+0 only.
        projector.getProjectedPower(driver.state, courser) shouldBe 5
        projector.getProjectedToughness(driver.state, courser) shouldBe 3

        addCounters(driver, sword, CounterType.HONE, 1)

        // +1/+0 on top, and toughness is untouched.
        projector.getProjectedPower(driver.state, courser) shouldBe 6
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("hone counters stack — each one is a separate +1/+0") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        equip(driver, you, sword, courser)

        addCounters(driver, sword, CounterType.HONE, 3)

        // 3 base + 2 (Bonesplitter) + 3 (hone) = 8.
        projector.getProjectedPower(driver.state, courser) shouldBe 8
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("hone counters on an UNATTACHED Equipment do nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")

        addCounters(driver, sword, CounterType.HONE, 4)

        // Nothing is equipped, so there is no creature to pump.
        driver.state.getEntity(sword)?.get<AttachedToComponent>() shouldBe null
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("hone counters on a NON-Equipment permanent do nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        // CR 122.1j is scoped to Equipment; a hone counter parked on a creature is inert.
        addCounters(driver, courser, CounterType.HONE, 5)

        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("re-equipping moves the hone bonus to the new creature") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears") // 2/2
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")

        equip(driver, you, sword, courser)
        addCounters(driver, sword, CounterType.HONE, 2)

        projector.getProjectedPower(driver.state, courser) shouldBe 7 // 3 + 2 + 2
        projector.getProjectedPower(driver.state, bears) shouldBe 2

        equip(driver, you, sword, bears)

        // The counters never moved — they are still on the Equipment — but the rule aims them at
        // whatever it is attached to *now*.
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedPower(driver.state, bears) shouldBe 6 // 2 + 2 + 2
        driver.state.getEntity(sword)?.get<CountersComponent>()
            ?.getCount(CounterType.HONE) shouldBe 2
    }
})
