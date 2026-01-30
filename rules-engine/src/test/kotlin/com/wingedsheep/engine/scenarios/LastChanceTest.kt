package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.TakeExtraTurnEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for TakeExtraTurnEffect with loseAtEndStep (Last Chance and similar cards).
 *
 * Last Chance: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
 *
 * Implementation note: In a 2-player game, "take an extra turn" is modeled as the opponent skipping their next turn.
 */
class LastChanceTest : FunSpec({

    // Test card that mimics Last Chance
    val LastChance = CardDefinition.sorcery(
        name = "Last Chance",
        manaCost = ManaCost.parse("{R}{R}"),
        oracleText = "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game.",
        script = CardScript.spell(
            effect = TakeExtraTurnEffect(loseAtEndStep = true)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LastChance)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Last Chance adds SkipNextTurnComponent to opponent") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Last Chance in hand and give mana
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)

        // Cast Last Chance
        val castResult = driver.castSpell(caster, lastChance, emptyList())
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Verify opponent has SkipNextTurnComponent
        val opponentEntity = driver.state.getEntity(opponent)
        opponentEntity?.has<SkipNextTurnComponent>() shouldBe true
    }

    test("Last Chance adds LoseAtEndStepComponent to caster") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Last Chance in hand and give mana
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)

        // Cast Last Chance
        driver.castSpell(caster, lastChance, emptyList())
        driver.bothPass()

        // Verify caster has LoseAtEndStepComponent
        val casterEntity = driver.state.getEntity(caster)
        casterEntity?.has<LoseAtEndStepComponent>() shouldBe true
    }

    test("opponent's turn is skipped after Last Chance") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Last Chance in hand and give mana
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)

        // Cast Last Chance
        driver.castSpell(caster, lastChance, emptyList())
        driver.bothPass()

        // End the current turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass() // End caster's turn

        // Opponent's turn should be skipped - caster should still be the active player
        // (caster gets another turn because opponent's turn was skipped)
        driver.activePlayer shouldBe caster
    }

    test("caster loses at beginning of their end step after using extra turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Last Chance in hand and give mana
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)

        // Cast Last Chance
        driver.castSpell(caster, lastChance, emptyList())
        driver.bothPass()

        // End the current turn - this starts the "extra turn" (opponent's turn is skipped)
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Caster should still be active player (got extra turn)
        driver.activePlayer shouldBe caster

        // Advance to end step of the extra turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)

        // Caster should have lost at the beginning of end step
        val casterEntity = driver.state.getEntity(caster)
        casterEntity?.has<PlayerLostComponent>() shouldBe true

        // Game should be over with opponent as winner
        driver.state.gameOver shouldBe true
        driver.state.winnerId shouldBe opponent
    }

    test("LoseAtEndStepComponent is consumed when player loses") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Last Chance in hand and give mana
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)

        // Cast Last Chance
        driver.castSpell(caster, lastChance, emptyList())
        driver.bothPass()

        // Verify caster has the component
        driver.state.getEntity(caster)?.has<LoseAtEndStepComponent>() shouldBe true

        // End the current turn and reach end step of extra turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.passPriorityUntil(Step.END, maxPasses = 200)

        // Component should be consumed after loss triggered
        driver.state.getEntity(caster)?.has<LoseAtEndStepComponent>() shouldBe false
    }
})
