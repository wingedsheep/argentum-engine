package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AnnieFlashTheVeteran
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Annie Flash, the Veteran (OTJ #190) — {3}{R}{G}{W} 4/5 Legendary Human Rogue, Flash.
 *
 *  - "Whenever Annie Flash becomes tapped, exile the top two cards of your library. You may play
 *    those cards this turn."
 *
 * Exercises the impulse-draw becomes-tapped ability (now composed via `Patterns.Exile.impulse(2)`):
 * tapping Annie exiles the top two cards and grants permission to play them this turn.
 */
class AnnieFlashTheVeteranScenarioTest : FunSpec({

    // Minimal instant that taps a target creature so we can make Annie "become tapped".
    val tapThatThing = card("Tap That Thing") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        oracleText = "Tap target creature."
        spell {
            target = Targets.Creature
            effect = Effects.Tap(EffectTarget.ContextTarget(0))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AnnieFlashTheVeteran + tapThatThing)
        return driver
    }

    test("becoming tapped exiles the top two cards and grants permission to play them this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val annie = driver.putCreatureOnBattlefield(player, "Annie Flash, the Veteran")
        driver.putCardOnTopOfLibrary(player, "Mountain")
        driver.putCardOnTopOfLibrary(player, "Forest")

        val tapSpell = driver.putCardInHand(player, "Tap That Thing")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, tapSpell, listOf(annie))
        driver.bothPass() // resolve the tap spell — Annie becomes tapped
        if (driver.stackSize > 0) driver.bothPass() // resolve the becomes-tapped trigger

        driver.isTapped(annie) shouldBe true
        // The two top cards are exiled and flagged playable.
        driver.getExileCardNames(player).sorted() shouldBe listOf("Forest", "Mountain")
        driver.getExile(player).all { exiled ->
            driver.state.mayPlayPermissions.any { exiled in it.cardIds }
        } shouldBe true
    }
})
