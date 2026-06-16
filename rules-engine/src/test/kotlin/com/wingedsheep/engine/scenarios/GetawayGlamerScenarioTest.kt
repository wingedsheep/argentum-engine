package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.GetawayGlamer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Getaway Glamer (OTJ Spree instant), {W}.
 *
 * + {1} — Exile target nontoken creature; return it at the next end step.
 * + {2} — Destroy target creature if no other creature has greater power.
 */
class GetawayGlamerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + GetawayGlamer)
        return driver
    }

    test("Mode 1 ({1}) exiles a creature and returns it at the next end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(player, "Centaur Courser") // 3/3

        val spell = driver.putCardInHand(player, "Getaway Glamer")
        driver.giveMana(player, Color.WHITE, 1) // {W}
        driver.giveColorlessMana(player, 1)      // {1} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(creature)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(creature)))
            )
        )
        driver.bothPass()

        // Right after resolution the creature is gone (exiled).
        driver.findPermanent(player, "Centaur Courser") shouldBe null

        // Advance to the end step; the delayed trigger returns it.
        driver.passPriorityUntil(Step.CLEANUP)
        driver.findPermanent(player, "Centaur Courser") shouldNotBe null
    }

    test("Mode 2 ({2}) destroys the largest creature (no other has greater power)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val biggest = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5
        driver.putCreatureOnBattlefield(player, "Centaur Courser") // 3/3

        val spell = driver.putCardInHand(player, "Getaway Glamer")
        driver.giveMana(player, Color.WHITE, 1) // {W}
        driver.giveColorlessMana(player, 2)      // {2} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(biggest)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(biggest)))
            )
        )
        driver.bothPass()

        // 5/5 is the largest → destroyed.
        driver.findPermanent(opponent, "Force of Nature") shouldBe null
    }

    test("Mode 2 ({2}) does NOT destroy when another creature has greater power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val smaller = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        driver.putCreatureOnBattlefield(player, "Force of Nature") // 5/5 has greater power

        val spell = driver.putCardInHand(player, "Getaway Glamer")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(smaller)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(smaller)))
            )
        )
        driver.bothPass()

        // A 5/5 has greater power than the targeted 3/3 → not destroyed.
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
    }
})
