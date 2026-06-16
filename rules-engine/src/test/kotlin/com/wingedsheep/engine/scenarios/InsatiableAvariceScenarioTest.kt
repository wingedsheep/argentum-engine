package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.InsatiableAvarice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Insatiable Avarice (OTJ Spree sorcery), {B}.
 *
 * + {2} — Search your library for a card, then shuffle and put that card on top.
 * + {B}{B} — Target player draws three cards and loses 3 life.
 */
class InsatiableAvariceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + InsatiableAvarice)
        return driver
    }

    test("Mode 1 ({2}) tutors a card and puts it on top of the library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Guarantee a Lightning Bolt is in the library to be searched for.
        driver.putCardOnTopOfLibrary(player, "Lightning Bolt")

        val spell = driver.putCardInHand(player, "Insatiable Avarice")
        driver.giveMana(player, Color.BLACK, 1) // {B}
        driver.giveColorlessMana(player, 2)      // {2} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0)
            )
        )
        driver.bothPass() // resolve the spell; the search pauses for a selection

        // Search picks Lightning Bolt from the library.
        val bolt = driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).first { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Lightning Bolt"
        }
        driver.submitCardSelection(player, listOf(bolt))

        // After shuffle, the searched card sits on top of the library.
        val top = driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).first()
        driver.state.getEntity(top)?.get<CardComponent>()?.name shouldBe "Lightning Bolt"
    }

    test("Mode 2 ({B}{B}) makes the target player draw three and lose 3 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.state.getZone(ZoneKey(player, Zone.HAND)).size
        val lifeBefore = driver.getLifeTotal(player)

        val spell = driver.putCardInHand(player, "Insatiable Avarice")
        driver.giveMana(player, Color.BLACK, 3) // {B} base + {B}{B} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(player)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(player)))
            )
        )
        driver.bothPass()

        // The spell itself left the hand; net +3 cards drawn (−1 spell +3 draw vs +1 spell added).
        val handAfter = driver.state.getZone(ZoneKey(player, Zone.HAND)).size
        handAfter shouldBe handBefore + 3
        driver.getLifeTotal(player) shouldBe lifeBefore - 3
    }
})
