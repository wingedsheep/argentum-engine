package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.ArmorThrull
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Armor Thrull (Fallen Empires).
 *
 * Armor Thrull: {2}{B}
 * Creature — Thrull
 * 1/3
 * {T}, Sacrifice this creature: Put a +1/+2 counter on target creature.
 *
 * The point of interest is the counter kind, not the ability: CR 122.1a says a +X/+Y counter adds
 * X to power and Y to toughness, and the engine sums an enumerated set of kinds. `+1/+2` is one of
 * three sizes Fallen Empires added, so these tests pin the asymmetric arithmetic — including that
 * several counters stack and that they combine with the ordinary `+1/+1`.
 */
class ArmorThrullScenarioTest : FunSpec({

    val abilityId = ArmorThrull.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ArmorThrull)
        return driver
    }

    test("sacrificing puts a +1/+2 counter on the target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val thrull = driver.putCreatureOnBattlefield(alice, "Armor Thrull")
        driver.removeSummoningSickness(thrull)
        val target = driver.putCreatureOnBattlefield(alice, "Elvish Warrior")  // 2/3

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = thrull,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        withClue("Armor Thrull sacrificed itself to pay") {
            driver.state.getBattlefield().contains(thrull) shouldBe false
        }

        val projected = projector.project(driver.state)
        withClue("+1/+2 adds 1 power and 2 toughness") {
            projected.getPower(target) shouldBe 3
            projected.getToughness(target) shouldBe 5
        }
    }

    test("+1/+2 counters stack, and add to a +1/+1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val target = driver.putCreatureOnBattlefield(alice, "Elvish Warrior")  // 2/3
        driver.replaceState(
            driver.state.updateEntity(target) { c ->
                c.with(
                    CountersComponent(
                        mapOf(
                            CounterType.PLUS_ONE_PLUS_TWO to 2,
                            CounterType.PLUS_ONE_PLUS_ONE to 1,
                        )
                    )
                )
            }
        )

        val projected = projector.project(driver.state)
        withClue("2/3 + two +1/+2 + one +1/+1 = 5/8") {
            projected.getPower(target) shouldBe 5
            projected.getToughness(target) shouldBe 8
        }
    }
})
