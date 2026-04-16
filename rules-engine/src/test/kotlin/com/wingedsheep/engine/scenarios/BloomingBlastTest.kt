package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.BloomingBlast
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Blooming Blast - a Gift a Treasure instant from Bloomburrow.
 *
 * Mode 0: No gift — deal 2 damage to target creature.
 * Mode 1: Gift — opponent creates a Treasure, deal 2 damage to target creature
 *         and 3 damage to that creature's controller.
 */
class BloomingBlastTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BloomingBlast)
        return driver
    }

    test("mode 0 (no gift) deals 2 damage to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a 3/3 creature
        val creatureId = driver.putCreatureOnBattlefield(opponent, "Hill Giant")

        // Give mana and put Blooming Blast in hand
        driver.giveMana(activePlayer, Color.RED, 2)
        val spell = driver.putCardInHand(activePlayer, "Blooming Blast")

        // Cast with mode 0 (no gift) targeting the creature
        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(creatureId)),
            chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(creatureId)))
        ))
        result.isSuccess shouldBe true

        driver.bothPass()

        // Creature took 2 damage (3/3 -> should still be alive with 2 damage)
        driver.findPermanent(opponent, "Hill Giant") shouldBe creatureId
        // Opponent life should be unchanged
        driver.assertLifeTotal(opponent, 20)
    }

    test("mode 1 (gift) deals 2 damage to creature and 3 damage to its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a 3/3 creature
        val creatureId = driver.putCreatureOnBattlefield(opponent, "Hill Giant")

        // Give mana and put Blooming Blast in hand
        driver.giveMana(activePlayer, Color.RED, 2)
        val spell = driver.putCardInHand(activePlayer, "Blooming Blast")

        // Cast with mode 1 (gift) targeting the creature
        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(creatureId)),
            chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(creatureId)))
        ))
        result.isSuccess shouldBe true

        driver.bothPass()

        // Creature took 2 damage (3/3 -> still alive)
        driver.findPermanent(opponent, "Hill Giant") shouldBe creatureId
        // Opponent should have taken 3 damage to life (controller of targeted creature)
        driver.assertLifeTotal(opponent, 17)
    }
})
