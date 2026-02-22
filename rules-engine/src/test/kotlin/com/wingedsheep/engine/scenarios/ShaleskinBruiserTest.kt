package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.ShaleskinBruiser
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Shaleskin Bruiser.
 *
 * Shaleskin Bruiser: {6}{R}
 * Creature — Beast
 * 4/4
 * Trample
 * Whenever Shaleskin Bruiser attacks, it gets +3/+0 until end of turn for each other attacking Beast.
 */
class ShaleskinBruiserTest : FunSpec({

    val TestBeast = CardDefinition.creature(
        name = "Test Beast",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val TestWarrior = CardDefinition.creature(
        name = "Test Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShaleskinBruiser, TestBeast, TestWarrior))
        return driver
    }

    test("Shaleskin Bruiser gets +3/+0 for each other attacking Beast") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        // Put Shaleskin Bruiser and two Beasts on battlefield
        val bruiser = driver.putCreatureOnBattlefield(player, "Shaleskin Bruiser")
        driver.removeSummoningSickness(bruiser)
        val beast1 = driver.putCreatureOnBattlefield(player, "Test Beast")
        driver.removeSummoningSickness(beast1)
        val beast2 = driver.putCreatureOnBattlefield(player, "Test Beast")
        driver.removeSummoningSickness(beast2)

        // Base power should be 4
        projector.getProjectedPower(driver.state, bruiser) shouldBe 4

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with all three creatures
        driver.declareAttackers(player, listOf(bruiser, beast1, beast2), opponent)

        // Resolve the attack trigger
        driver.bothPass()

        // Bruiser should get +3/+0 for each of the 2 other attacking Beasts = +6/+0
        // Total power = 4 + 6 = 10
        projector.getProjectedPower(driver.state, bruiser) shouldBe 10
        projector.getProjectedToughness(driver.state, bruiser) shouldBe 4
    }

    test("Shaleskin Bruiser gets no bonus when attacking alone") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bruiser = driver.putCreatureOnBattlefield(player, "Shaleskin Bruiser")
        driver.removeSummoningSickness(bruiser)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bruiser), opponent)

        // Resolve the attack trigger
        driver.bothPass()

        // No other attacking Beasts, so no bonus
        projector.getProjectedPower(driver.state, bruiser) shouldBe 4
        projector.getProjectedToughness(driver.state, bruiser) shouldBe 4
    }

    test("Shaleskin Bruiser does not count non-Beast attackers") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bruiser = driver.putCreatureOnBattlefield(player, "Shaleskin Bruiser")
        driver.removeSummoningSickness(bruiser)
        val warrior = driver.putCreatureOnBattlefield(player, "Test Warrior")
        driver.removeSummoningSickness(warrior)
        val beast = driver.putCreatureOnBattlefield(player, "Test Beast")
        driver.removeSummoningSickness(beast)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bruiser, warrior, beast), opponent)

        // Resolve the attack trigger
        driver.bothPass()

        // Only 1 other attacking Beast (not counting the Warrior)
        // Power = 4 + 3 = 7
        projector.getProjectedPower(driver.state, bruiser) shouldBe 7
        projector.getProjectedToughness(driver.state, bruiser) shouldBe 4
    }

    test("Shaleskin Bruiser does not count non-attacking Beasts") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bruiser = driver.putCreatureOnBattlefield(player, "Shaleskin Bruiser")
        driver.removeSummoningSickness(bruiser)
        val beast = driver.putCreatureOnBattlefield(player, "Test Beast")
        // beast has summoning sickness - cannot attack

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bruiser), opponent)

        // Resolve the attack trigger
        driver.bothPass()

        // Beast is on battlefield but not attacking, so no bonus
        projector.getProjectedPower(driver.state, bruiser) shouldBe 4
        projector.getProjectedToughness(driver.state, bruiser) shouldBe 4
    }
})
