package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.legions.cards.VexingBeetle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Vexing Beetle:
 * - "This spell can't be countered" mechanic
 * - Conditional +3/+3 when no opponent controls a creature
 */
class VexingBeetleTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + VexingBeetle)
        return driver
    }

    test("Vexing Beetle can't be countered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val beetle = driver.putCardInHand(activePlayer, "Vexing Beetle")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(activePlayer, Color.GREEN, 5)
        driver.giveMana(opponent, Color.BLUE, 2)

        // Cast Vexing Beetle
        driver.castSpell(activePlayer, beetle)
        driver.stackSize shouldBe 1

        // Pass to opponent
        driver.passPriority(activePlayer)

        // Opponent casts Counterspell targeting the Beetle
        val topSpellId = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(topSpellId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass - Counterspell resolves but Beetle can't be countered
        driver.bothPass()

        // Beetle should still be on the stack (not countered)
        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Vexing Beetle"

        // Both pass - Beetle resolves and enters the battlefield
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.findPermanent(activePlayer, "Vexing Beetle") shouldNotBe null
    }

    test("Vexing Beetle gets +3/+3 when no opponent controls a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val beetle = driver.putCardInHand(activePlayer, "Vexing Beetle")
        driver.giveMana(activePlayer, Color.GREEN, 5)

        // Cast Vexing Beetle (opponent has no creatures)
        driver.castSpell(activePlayer, beetle)
        driver.bothPass()
        driver.bothPass()

        // Should be 6/6 (3/3 base + 3/3 bonus)
        val beetleId = driver.findPermanent(activePlayer, "Vexing Beetle")!!
        projector.getProjectedPower(driver.state, beetleId) shouldBe 6
        projector.getProjectedToughness(driver.state, beetleId) shouldBe 6
    }

    test("Vexing Beetle does not get +3/+3 when opponent controls a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent a creature first
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Now cast Vexing Beetle
        val beetle = driver.putCardInHand(activePlayer, "Vexing Beetle")
        driver.giveMana(activePlayer, Color.GREEN, 5)
        driver.castSpell(activePlayer, beetle)
        driver.bothPass()
        driver.bothPass()

        // Should be 3/3 (no bonus since opponent has a creature)
        val beetleId = driver.findPermanent(activePlayer, "Vexing Beetle")!!
        projector.getProjectedPower(driver.state, beetleId) shouldBe 3
        projector.getProjectedToughness(driver.state, beetleId) shouldBe 3
    }
})
