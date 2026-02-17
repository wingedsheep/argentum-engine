package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Prosperity-style X cost handling.
 */
class ProsperityXCostTest : FunSpec({

    // Define a test Prosperity card directly
    val Prosperity = CardDefinition.sorcery(
        name = "Prosperity",
        manaCost = ManaCost.parse("{X}{U}"),
        oracleText = "Each player draws X cards.",
        script = CardScript.spell(
            effect = Effects.EachPlayerDrawsX(
                includeController = true,
                includeOpponents = true
            )
        )
    )

    test("ManaCost.parse correctly identifies {X}{U} as having X") {
        val cost = ManaCost.parse("{X}{U}")
        cost.hasX shouldBe true
        cost.cmc shouldBe 1  // X contributes 0, U contributes 1
    }

    test("Prosperity card definition has X in mana cost") {
        Prosperity.manaCost.hasX shouldBe true
        Prosperity.manaCost.toString() shouldBe "{X}{U}"
    }

    test("Prosperity can be cast with X value and draws cards for all players") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Prosperity))

        driver.initMirrorMatch(
            deck = Deck.of(
                "Island" to 20
            ),
            skipMulligans = true,
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Get starting hand sizes
        val startingActiveHandSize = driver.state.getHand(activePlayer).size
        val startingOpponentHandSize = driver.state.getHand(opponent).size

        // Put Prosperity in hand and give mana
        val prosperity = driver.putCardInHand(activePlayer, "Prosperity")

        // Give 3 mana (1 blue for the {U} + 2 for X=2)
        driver.giveMana(activePlayer, Color.BLUE, 3)

        // Cast Prosperity with X=2
        driver.castXSpell(activePlayer, prosperity, xValue = 2)
        driver.bothPass()

        // Each player should have drawn 2 cards
        val activeHandSize = driver.state.getHand(activePlayer).size
        val opponentHandSize = driver.state.getHand(opponent).size

        // Active player: starting hand size + 1 (put prosperity) - 1 (cast it) + 2 (drew from prosperity) = starting + 2
        activeHandSize shouldBe startingActiveHandSize + 2
        // Opponent: starting hand size + 2 (drew from prosperity)
        opponentHandSize shouldBe startingOpponentHandSize + 2
    }
})
