package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.GaeasBlessing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gaea's Blessing — {1}{G} Sorcery. "Target player shuffles up to three target cards from their
 * graveyard into their library. Draw a card."
 *
 * The card targets come only from the targeted player's graveyard, that player's library is the one
 * shuffled, and the caster draws.
 */
class GaeasBlessingScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GaeasBlessing))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(caster: EntityId, targets: List<ChosenTarget>) =
        submit(CastSpell(playerId = caster, cardId = putCardInHand(caster, "Gaea's Blessing").also {
            giveMana(caster, Color.GREEN, 2)
        }, targets = targets))

    test("the targeted opponent shuffles two of their graveyard cards in; the caster draws") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val oppCards = listOf(driver.putCardInGraveyard(opp, "Grizzly Bears"), driver.putCardInGraveyard(opp, "Mountain"))
        val kept = driver.putCardInGraveyard(opp, "Centaur Courser")
        val oppLibrary = driver.state.getLibrary(opp).size
        val myHand = driver.state.getHand(me).size

        driver.cast(me, listOf(ChosenTarget.Player(opp)) +
            oppCards.map { ChosenTarget.Card(it, opp, Zone.GRAVEYARD) }).error shouldBe null
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == opp }
        driver.bothPass()

        driver.getGraveyard(opp) shouldBe listOf(kept)
        driver.state.getLibrary(opp).size shouldBe oppLibrary + 2
        oppCards.forEach { driver.state.getLibrary(opp) shouldContain it }
        driver.events.count { it is LibraryShuffledEvent && it.playerId == opp } shouldBe shufflesBefore + 1
        // Measured before Gaea's Blessing was put into hand and cast, so the draw is the +1.
        driver.state.getHand(me).size shouldBe myHand + 1
    }

    test("a card from the caster's graveyard is not legal while the opponent is the target player") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val mine = driver.putCardInGraveyard(me, "Grizzly Bears")

        driver.cast(me, listOf(ChosenTarget.Player(opp), ChosenTarget.Card(mine, me, Zone.GRAVEYARD))).error shouldNotBe null
    }
})
