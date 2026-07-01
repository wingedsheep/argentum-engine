package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m12.cards.AdaptiveAutomaton
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val projector = StateProjector()

/**
 * Adaptive Automaton (M12) — {3} 2/2 Artifact Creature — Construct
 *
 * "As this creature enters, choose a creature type.
 *  This creature is the chosen type in addition to its other types.
 *  Other creatures you control of the chosen type get +1/+1."
 */
class AdaptiveAutomatonScenarioTest : FunSpec({

    val goblin = CardDefinition.creature(
        name = "Test Goblin",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AdaptiveAutomaton, goblin, bear))
        return driver
    }

    test("the Automaton becomes the chosen type and buffs OTHER creatures of that type you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val automaton = driver.putCreatureOnBattlefield(you, "Adaptive Automaton")
        driver.replaceState(driver.state.updateEntity(automaton) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val goblinId = driver.putCreatureOnBattlefield(you, "Test Goblin")
        val bearId = driver.putCreatureOnBattlefield(you, "Test Bear")

        val projected = projector.project(driver.state)

        // The Automaton is now a Goblin in addition to Construct.
        projected.hasSubtype(automaton, "Goblin") shouldBe true
        projected.hasSubtype(automaton, "Construct") shouldBe true

        // The other Goblin you control gets +1/+1.
        projected.getPower(goblinId) shouldBe 2
        projected.getToughness(goblinId) shouldBe 2

        // "Other" excludes the Automaton itself — it stays a base 2/2.
        projected.getPower(automaton) shouldBe 2
        projected.getToughness(automaton) shouldBe 2

        // A non-Goblin is unaffected.
        projected.getPower(bearId) shouldBe 2
        projected.getToughness(bearId) shouldBe 2
    }

    test("opponents' creatures of the chosen type are not buffed (creatures YOU control)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), skipMulligans = true)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val automaton = driver.putCreatureOnBattlefield(you, "Adaptive Automaton")
        driver.replaceState(driver.state.updateEntity(automaton) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice("Goblin"))))
        })

        val enemyGoblin = driver.putCreatureOnBattlefield(opponent, "Test Goblin")

        val projected = projector.project(driver.state)
        projected.getPower(enemyGoblin) shouldBe 1
        projected.getToughness(enemyGoblin) shouldBe 1
    }
})
