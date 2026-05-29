package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.SpiritOfResistance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spirit of Resistance (INV #38) — Invasion engine gaps #2 + #7.
 *
 * "As long as you control a permanent of each color, prevent all damage that would be dealt to you."
 * Built as a [com.wingedsheep.sdk.scripting.PreventDamage] gated by a five-distinct-colors
 * restriction.
 */
class SpiritOfResistanceTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SpiritOfResistance))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.lifeOf(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0

    // One creature of each of the five colors.
    val fiveColorBoard = listOf(
        "Savannah Lions",   // W
        "Phantom Warrior",  // U
        "Black Creature",   // B
        "Goblin Guide",     // R
        "Llanowar Elves",   // G
    )

    test("prevents all damage to you while you control a permanent of each color") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Spirit of Resistance")
        fiveColorBoard.forEach { driver.putCreatureOnBattlefield(you, it) }

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.bothPass()

        driver.lifeOf(you) shouldBe 20
    }

    test("does not prevent damage when a color is missing") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Spirit of Resistance")
        // Only four colors — no green permanent.
        fiveColorBoard.dropLast(1).forEach { driver.putCreatureOnBattlefield(you, it) }

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.bothPass()

        driver.lifeOf(you) shouldBe 17
    }
})
