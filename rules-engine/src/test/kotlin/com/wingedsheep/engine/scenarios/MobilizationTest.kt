package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Mobilization.
 *
 * Mobilization: {2}{W}
 * Enchantment
 * Soldier creatures have vigilance.
 * {2}{W}: Create a 1/1 white Soldier creature token.
 */
class MobilizationTest : FunSpec({

    val Mobilization = card("Mobilization") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"

        staticAbility {
            ability = GrantKeywordToCreatureGroup(
                keyword = Keyword.VIGILANCE,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Soldier"))
            )
        }

        activatedAbility {
            cost = Costs.Mana("{2}{W}")
            effect = CreateTokenEffect(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
            )
        }
    }

    val abilityId = Mobilization.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Mobilization)
        return driver
    }

    test("activated ability creates a 1/1 white Soldier token") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mobilization = driver.putPermanentOnBattlefield(activePlayer, "Mobilization")

        driver.giveMana(activePlayer, Color.WHITE, 3)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mobilization,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        val creatures = driver.getCreatures(activePlayer)
        creatures.size shouldBe 1

        val token = creatures.first()
        projector.getProjectedPower(driver.state, token) shouldBe 1
        projector.getProjectedToughness(driver.state, token) shouldBe 1
    }

    test("Soldier tokens created by Mobilization have vigilance") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mobilization = driver.putPermanentOnBattlefield(activePlayer, "Mobilization")

        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mobilization,
                abilityId = abilityId
            )
        )
        driver.bothPass()

        val token = driver.getCreatures(activePlayer).first()
        val projected = projector.project(driver.state)
        projected.hasKeyword(token, Keyword.VIGILANCE) shouldBe true
    }

    test("existing Soldier creatures gain vigilance from Mobilization") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Blade of the Ninth Watch is a Human Soldier
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch")

        // Before Mobilization, soldier should NOT have vigilance
        var projected = projector.project(driver.state)
        projected.hasKeyword(soldier, Keyword.VIGILANCE) shouldBe false

        // Put Mobilization on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Mobilization")

        // Now soldier SHOULD have vigilance
        projected = projector.project(driver.state)
        projected.hasKeyword(soldier, Keyword.VIGILANCE) shouldBe true
    }

    test("non-Soldier creatures do not gain vigilance from Mobilization") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Mobilization")

        // Grizzly Bears is a Bear, not a Soldier
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val projected = projector.project(driver.state)
        projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
    }

    test("Mobilization persists on battlefield after activation") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mobilization = driver.putPermanentOnBattlefield(activePlayer, "Mobilization")

        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mobilization,
                abilityId = abilityId
            )
        )
        driver.bothPass()

        driver.findPermanent(activePlayer, "Mobilization") shouldNotBe null
    }
})
