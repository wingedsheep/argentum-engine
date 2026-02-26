package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.DragonBreath
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Dragon Breath.
 *
 * Dragon Breath: {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has haste.
 * {R}: Enchanted creature gets +1/+0 until end of turn.
 * When a creature with mana value 6 or greater enters, you may return Dragon Breath
 * from your graveyard to the battlefield attached to that creature.
 */
class DragonBreathTest : FunSpec({

    val pumpAbilityId = DragonBreath.activatedAbilities.first().id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Activated ability gives enchanted creature +1/+0") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield (Goblin Brigand is a 2/2)
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Brigand")

        // Cast Dragon Breath on the goblin
        val breath = driver.putCardInHand(activePlayer, "Dragon Breath")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, breath, listOf(goblin))
        driver.bothPass()

        // Verify base stats: Goblin Brigand is 2/2
        projector.getProjectedPower(driver.state, goblin) shouldBe 2
        projector.getProjectedToughness(driver.state, goblin) shouldBe 2

        // Activate the pump ability: {R}: Enchanted creature gets +1/+0
        driver.giveMana(activePlayer, Color.RED, 1)
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = breath,
                abilityId = pumpAbilityId
            )
        )
        activateResult.isSuccess shouldBe true
        driver.bothPass()

        // Goblin should now be 3/2
        projector.getProjectedPower(driver.state, goblin) shouldBe 3
        projector.getProjectedToughness(driver.state, goblin) shouldBe 2
    }

    test("Activated ability can be used multiple times") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Brigand")

        val breath = driver.putCardInHand(activePlayer, "Dragon Breath")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, breath, listOf(goblin))
        driver.bothPass()

        // Activate twice
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = breath, abilityId = pumpAbilityId)
        )
        driver.bothPass()

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = breath, abilityId = pumpAbilityId)
        )
        driver.bothPass()

        // Goblin should now be 4/2 (+2/+0 from two activations)
        projector.getProjectedPower(driver.state, goblin) shouldBe 4
        projector.getProjectedToughness(driver.state, goblin) shouldBe 2
    }

    test("Pump effect wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Use Glory Seeker (2/2 vanilla) to avoid "must attack" complications with Goblin Brigand
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")

        val breath = driver.putCardInHand(activePlayer, "Dragon Breath")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, breath, listOf(creature))
        driver.bothPass()

        // Activate pump
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = breath, abilityId = pumpAbilityId)
        )
        driver.bothPass()

        projector.getProjectedPower(driver.state, creature) shouldBe 3

        // Pass to next turn
        driver.passPriorityUntil(Step.UPKEEP)

        // Pump should have worn off, back to 2/2
        projector.getProjectedPower(driver.state, creature) shouldBe 2
        projector.getProjectedToughness(driver.state, creature) shouldBe 2
    }
})
