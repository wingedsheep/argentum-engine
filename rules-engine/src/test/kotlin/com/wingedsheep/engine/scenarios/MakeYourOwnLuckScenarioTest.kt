package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.MakeYourOwnLuck
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Make Your Own Luck (OTJ) — {3}{G}{U} Sorcery.
 *
 * "Look at the top three cards of your library. You may exile a nonland card from among them.
 *  If you do, it becomes plotted. Put the rest into your hand."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.effects.MakePlottedEffect]: the exiled nonland
 * card gets a [PlottedComponent] and a permanent free-cast permission; the remaining cards go to
 * hand. Declining the optional exile puts all three cards into hand.
 */
class MakeYourOwnLuckScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + MakeYourOwnLuck)
        return driver
    }

    test("exile a nonland from the top three → it becomes plotted, the rest go to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Build a known top three: bottom-to-top so the creature ends up among the top three.
        driver.putCardOnTopOfLibrary(player, "Forest")
        driver.putCardOnTopOfLibrary(player, "Forest")
        val creatureId = driver.putCardOnTopOfLibrary(player, "Centaur Courser") // top card

        val spell = driver.putCardInHand(player, "Make Your Own Luck")
        val handBefore = driver.state.getZone(ZoneKey(player, Zone.HAND)).size
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 3)
        driver.submit(CastSpell(playerId = player, cardId = spell))
        driver.bothPass() // resolve the spell off the stack

        // Resolution pauses to let the controller choose the nonland to exile/plot.
        driver.submitCardSelection(player, listOf(creatureId))

        // The creature is exiled and plotted.
        val exile = driver.state.getZone(ZoneKey(player, Zone.EXILE))
        (creatureId in exile) shouldBe true
        driver.state.getEntity(creatureId)?.get<PlottedComponent>() shouldNotBe null

        // The two remaining cards (Forests) went to hand; the spell left the hand.
        val handAfter = driver.state.getZone(ZoneKey(player, Zone.HAND)).size
        handAfter shouldBe handBefore - 1 + 2
    }

    test("decline the exile → all three cards go to hand, nothing plotted") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(player, "Forest")
        driver.putCardOnTopOfLibrary(player, "Forest")
        val creatureId = driver.putCardOnTopOfLibrary(player, "Centaur Courser")

        val spell = driver.putCardInHand(player, "Make Your Own Luck")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 3)
        driver.submit(CastSpell(playerId = player, cardId = spell))
        driver.bothPass() // resolve the spell off the stack

        // Decline by selecting nothing.
        driver.submitCardSelection(player, emptyList())

        // Nothing exiled or plotted; all three cards in hand.
        driver.state.getEntity(creatureId)?.get<PlottedComponent>() shouldBe null
        val hand = driver.state.getZone(ZoneKey(player, Zone.HAND))
        (creatureId in hand) shouldBe true
    }
})
