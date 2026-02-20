package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Carrion Feeder.
 *
 * Carrion Feeder
 * {B}
 * Creature — Zombie
 * 1/1
 * Carrion Feeder can't block.
 * Sacrifice a creature: Put a +1/+1 counter on Carrion Feeder.
 */
class CarrionFeederTest : FunSpec({

    val CarrionFeeder = card("Carrion Feeder") {
        manaCost = "{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
        oracleText = "Carrion Feeder can't block.\nSacrifice a creature: Put a +1/+1 counter on Carrion Feeder."

        staticAbility {
            ability = CantBlock()
        }

        activatedAbility {
            cost = AbilityCost.Sacrifice(GameObjectFilter.Creature)
            effect = AddCountersEffect(
                counterType = "+1/+1",
                count = 1,
                target = EffectTarget.Self
            )
        }
    }

    val abilityId = CarrionFeeder.activatedAbilities[0].id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CarrionFeeder))
        return driver
    }

    test("sacrifice a creature to put a +1/+1 counter on Carrion Feeder") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activePlayer, "Carrion Feeder")
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Bear should be sacrificed
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null

        // Feeder should be 2/2 (1/1 base + one +1/+1 counter)
        projector.getProjectedPower(driver.state, feeder) shouldBe 2
        projector.getProjectedToughness(driver.state, feeder) shouldBe 2
    }

    test("sacrifice multiple creatures for multiple counters") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activePlayer, "Carrion Feeder")
        val bear1 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Sacrifice first creature
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear1))
            )
        )
        driver.bothPass()

        // Sacrifice second creature
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear2))
            )
        )
        driver.bothPass()

        // Feeder should be 3/3 (1/1 base + two +1/+1 counters)
        projector.getProjectedPower(driver.state, feeder) shouldBe 3
        projector.getProjectedToughness(driver.state, feeder) shouldBe 3
    }

    test("can sacrifice itself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activePlayer, "Carrion Feeder")

        // Sacrifice itself
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(feeder))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Feeder should be gone (sacrificed itself)
        driver.findPermanent(activePlayer, "Carrion Feeder") shouldBe null
    }

    test("no mana cost required to activate") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activePlayer, "Carrion Feeder")
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // No mana given - should still work since cost is only sacrifice
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        result.isSuccess shouldBe true
    }
})
