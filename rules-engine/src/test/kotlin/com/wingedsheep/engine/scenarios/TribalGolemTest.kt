package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.TribalGolem
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Tribal Golem.
 *
 * Tribal Golem: {6}
 * Artifact Creature — Golem
 * 4/4
 * Tribal Golem has trample as long as you control a Beast, haste as long as you
 * control a Goblin, first strike as long as you control a Soldier, flying as long
 * as you control a Wizard, and "{B}: Regenerate Tribal Golem" as long as you
 * control a Zombie.
 */
class TribalGolemTest : FunSpec({

    val regenerateAbilityId = TribalGolem.activatedAbilities.first().id

    val TestWizard = CardDefinition.creature(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestWizard))
        return driver
    }

    test("Tribal Golem is 4/4 with no keywords by default") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        val projected = projector.project(driver.state)

        projected.getPower(golem) shouldBe 4
        projected.getToughness(golem) shouldBe 4
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe false
        projected.hasKeyword(golem, Keyword.HASTE) shouldBe false
        projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe false
        projected.hasKeyword(golem, Keyword.FLYING) shouldBe false
    }

    test("gains trample when you control a Beast") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe true
    }

    test("gains haste when you control a Goblin") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.HASTE) shouldBe true
    }

    test("gains first strike when you control a Soldier") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("gains flying when you control a Wizard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.FLYING) shouldBe true
    }

    test("gains multiple keywords with multiple creature types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")
        driver.putCreatureOnBattlefield(activePlayer, "Test Wizard")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(golem, Keyword.HASTE) shouldBe true
        projected.hasKeyword(golem, Keyword.FLYING) shouldBe true
        projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe false // No Soldier
    }

    test("does not gain keywords from opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.putCreatureOnBattlefield(opponent, "Trample Beast")

        val projected = projector.project(driver.state)
        projected.hasKeyword(golem, Keyword.TRAMPLE) shouldBe false
    }

    test("regenerate ability is available when you control a Zombie") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.removeSummoningSickness(golem)
        driver.putCreatureOnBattlefield(activePlayer, "Gravedigger")

        // Give black mana for regenerate cost
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Should be able to activate regenerate
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = golem,
                abilityId = regenerateAbilityId
            )
        )
        result.isSuccess shouldBe true
    }

    test("regenerate ability is NOT available without a Zombie") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val golem = driver.putCreatureOnBattlefield(activePlayer, "Tribal Golem")
        driver.removeSummoningSickness(golem)

        // Give black mana
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Should NOT be able to activate regenerate (no Zombie)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = golem,
                abilityId = regenerateAbilityId
            )
        )
        result.isSuccess shouldBe false
    }
})
