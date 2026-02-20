package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Centaur Glade.
 *
 * Centaur Glade: {3}{G}{G}
 * Enchantment
 * {2}{G}{G}: Create a 3/3 green Centaur creature token.
 */
class CentaurGladeTest : FunSpec({

    val CentaurGlade = card("Centaur Glade") {
        manaCost = "{3}{G}{G}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{2}{G}{G}")
            effect = CreateTokenEffect(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Centaur"),
            )
        }
    }

    val gladeAbilityId = CentaurGlade.activatedAbilities.first().id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CentaurGlade)
        return driver
    }

    test("activated ability creates a 3/3 Centaur token") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Centaur Glade on the battlefield
        val glade = driver.putPermanentOnBattlefield(activePlayer, "Centaur Glade")

        // Activate the ability with {2}{G}{G}
        driver.giveMana(activePlayer, Color.GREEN, 4)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = glade,
                abilityId = gladeAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Should have one Centaur token creature
        val creatures = driver.getCreatures(activePlayer)
        creatures.size shouldBe 1

        // The token should be a 3/3
        val token = creatures.first()
        projector.getProjectedPower(driver.state, token) shouldBe 3
        projector.getProjectedToughness(driver.state, token) shouldBe 3
    }

    test("can activate multiple times to create multiple tokens") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glade = driver.putPermanentOnBattlefield(activePlayer, "Centaur Glade")

        // Activate first time
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = glade,
                abilityId = gladeAbilityId
            )
        )
        driver.bothPass()

        // Activate second time
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = glade,
                abilityId = gladeAbilityId
            )
        )
        driver.bothPass()

        // Should have two Centaur token creatures
        val creatures = driver.getCreatures(activePlayer)
        creatures.size shouldBe 2
    }

    test("cannot activate without enough mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glade = driver.putPermanentOnBattlefield(activePlayer, "Centaur Glade")

        // Try to activate with only 2 green mana (need 4)
        driver.giveMana(activePlayer, Color.GREEN, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = glade,
                abilityId = gladeAbilityId
            )
        )
        result.isSuccess shouldBe false

        // No creatures should exist
        driver.getCreatures(activePlayer).size shouldBe 0
    }

    test("Centaur Glade persists on battlefield after activation") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glade = driver.putPermanentOnBattlefield(activePlayer, "Centaur Glade")

        // Activate
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = glade,
                abilityId = gladeAbilityId
            )
        )
        driver.bothPass()

        // Glade should still be on the battlefield
        driver.findPermanent(activePlayer, "Centaur Glade") shouldNotBe null
    }
})
