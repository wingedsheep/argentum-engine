package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantShroudToController
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for True Believer.
 *
 * True Believer
 * {W}{W}
 * Creature — Human Cleric
 * 2/2
 * You have shroud. (You can't be the target of spells or abilities.)
 */
class TrueBelieverTest : FunSpec({

    val TrueBeliever = card("True Believer") {
        manaCost = "{W}{W}"
        typeLine = "Creature — Human Cleric"
        power = 2
        toughness = 2

        staticAbility {
            ability = GrantShroudToController
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TrueBeliever)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 10,
                "Mountain" to 10,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("controller with shroud cannot be targeted by 'any target' spells") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put True Believer on battlefield for the active player
        driver.putCreatureOnBattlefield(activePlayer, "True Believer")

        // Active player tries to target themselves with Lightning Bolt — should fail (shroud)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        val castResult = driver.castSpell(activePlayer, bolt, listOf(activePlayer))
        castResult.isSuccess shouldBe false
    }

    test("opponent cannot target player with shroud") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put True Believer on battlefield for the active player
        driver.putCreatureOnBattlefield(activePlayer, "True Believer")

        // Active player passes priority to opponent
        driver.passPriority(activePlayer)

        // Opponent tries to cast Lightning Bolt targeting the active player
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        val castResult = driver.castSpell(opponent, bolt, listOf(activePlayer))
        castResult.isSuccess shouldBe false
    }

    test("True Believer creature itself can still be targeted") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put True Believer on battlefield for the active player
        val trueBelieverId = driver.putCreatureOnBattlefield(activePlayer, "True Believer")

        // Active player passes priority to opponent
        driver.passPriority(activePlayer)

        // Opponent targets the True Believer creature with Lightning Bolt — should succeed
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        val castResult = driver.castSpell(opponent, bolt, listOf(trueBelieverId))
        castResult.isSuccess shouldBe true
    }

    test("controller becomes targetable again when True Believer leaves the battlefield") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put True Believer on battlefield for the active player
        val trueBelieverId = driver.putCreatureOnBattlefield(activePlayer, "True Believer")

        // Verify player is not targetable while True Believer is around
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        val failResult = driver.castSpell(activePlayer, bolt1, listOf(activePlayer))
        failResult.isSuccess shouldBe false

        // Kill the True Believer with a Lightning Bolt targeting the creature
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        val killResult = driver.castSpell(activePlayer, bolt2, listOf(trueBelieverId))
        killResult.isSuccess shouldBe true
        driver.bothPass() // Resolve — True Believer dies

        // Now the active player should be targetable again
        val bolt3 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        val successResult = driver.castSpell(activePlayer, bolt3, listOf(opponent))
        successResult.isSuccess shouldBe true
    }
})
