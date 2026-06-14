package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.FinalShowdown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Final Showdown (OTJ Spree instant).
 *
 * + {1} (mode 0): All creatures lose all abilities until end of turn.
 * + {1} (mode 1): Choose a creature you control (non-targeted, resolution-time). It gains
 *                 indestructible until end of turn.
 * + {3}{W}{W} (mode 2): Destroy all creatures.
 */
class OtjFinalShowdownScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FinalShowdown)
        return driver
    }

    test("Mode 0: all creatures lose all abilities until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A first-strike creature on each side.
        val mine = driver.putCreatureOnBattlefield(player, "First Strike Knight")
        val theirs = driver.putCreatureOnBattlefield(opponent, "First Strike Knight")

        val projectorBefore = StateProjector()
        projectorBefore.project(driver.state).hasKeyword(mine, Keyword.FIRST_STRIKE) shouldBe true
        projectorBefore.project(driver.state).hasKeyword(theirs, Keyword.FIRST_STRIKE) shouldBe true

        val spell = driver.putCardInHand(player, "Final Showdown")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(emptyList())
            )
        )
        driver.bothPass()

        val projector = StateProjector()
        projector.project(driver.state).hasKeyword(mine, Keyword.FIRST_STRIKE) shouldBe false
        projector.project(driver.state).hasKeyword(theirs, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("Mode 1: chosen creature you control gains indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two creatures you control, so the resolution-time choice presents a real decision
        // (a single eligible creature would auto-resolve with no prompt).
        val mine = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val other = driver.putCreatureOnBattlefield(player, "Savannah Lions")

        StateProjector().project(driver.state).hasKeyword(mine, Keyword.INDESTRUCTIBLE) shouldBe false

        val spell = driver.putCardInHand(player, "Final Showdown")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(emptyList())
            )
        )
        driver.bothPass()

        // Mode 1 chooses a creature at resolution (non-targeted) via the battlefield selection UI.
        driver.submitCardSelection(player, listOf(mine))

        StateProjector().project(driver.state).hasKeyword(mine, Keyword.INDESTRUCTIBLE) shouldBe true
        // The unchosen creature does not gain indestructible.
        StateProjector().project(driver.state).hasKeyword(other, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("Mode 2: destroy all creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val spell = driver.putCardInHand(player, "Final Showdown")
        driver.giveMana(player, Color.WHITE, 3) // {W} base + {W}{W} of {3}{W}{W}
        driver.giveColorlessMana(player, 3)      // {3} of {3}{W}{W}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(emptyList())
            )
        )
        driver.bothPass()

        driver.findPermanent(player, "Centaur Courser") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }

    test("Modes 1 + 2: the chosen indestructible creature survives the board wipe") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two of the player's creatures (so the mode-1 choice is a real decision) plus the
        // opponent's. The chosen one (Centaur Courser) survives; the unchosen Savannah Lions and
        // the opponent's creature are destroyed.
        val saved = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.putCreatureOnBattlefield(player, "Savannah Lions")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val spell = driver.putCardInHand(player, "Final Showdown")
        driver.giveMana(player, Color.WHITE, 3) // {W} base + {W}{W}
        driver.giveColorlessMana(player, 4)      // {1} (mode 1) + {3} (mode 2)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(1, 2),
                modeTargetsOrdered = listOf(emptyList(), emptyList())
            )
        )
        driver.bothPass()

        // Indestructible is granted (mode 1) before "destroy all" (mode 2) resolves.
        driver.submitCardSelection(player, listOf(saved))

        // The chosen creature has indestructible and survives; the others are destroyed.
        StateProjector().project(driver.state).hasKeyword(saved, Keyword.INDESTRUCTIBLE) shouldBe true
        driver.findPermanent(player, "Centaur Courser") shouldBe saved
        driver.findPermanent(player, "Savannah Lions") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }
})
