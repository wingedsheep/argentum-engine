package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.ReapIntellect
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reap Intellect (DGM #95) — {X}{2}{U}{B} Sorcery.
 * "Target opponent reveals their hand. You choose up to X nonland cards from it and exile them.
 *  For each card exiled this way, search that player's graveyard, hand, and library for any number
 *  of cards with the same name as that card and exile them. Then that player shuffles."
 *
 * Pins the composition: the hand pick is capped at X and offers nonland cards only, each exiled
 * card drives its own same-name search over all three zones (any number — copies may be left
 * behind), and with X = 0 nothing is exiled and no search happens.
 */
class ReapIntellectScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ReapIntellect)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castWithX(x: Int) {
        giveMana(player1, Color.BLUE, 1 + x)
        giveMana(player1, Color.BLACK, 3)
        val spell = putCardInHand(player1, "Reap Intellect")
        val cast = castXSpell(player1, spell, xValue = x, targets = listOf(player2))
        withClue(cast.error ?: "casting Reap Intellect failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
    }

    fun GameTestDriver.library(player: EntityId) = state.getZone(ZoneKey(player, Zone.LIBRARY))

    test("exiles up to X nonland cards from hand, then searches for each one's name") {
        val d = driver()
        val courser = d.putCardInHand(d.player2, "Centaur Courser")
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        val lions = d.putCardInHand(d.player2, "Savannah Lions")
        val courserInGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val boltInLibrary = d.putCardOnTopOfLibrary(d.player2, "Lightning Bolt")
        val courserInLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val librarySize = d.library(d.player2).size

        d.castWithX(2)

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the caster picks up to X nonland cards; the opponent's Swamps aren't selectable") {
            pick.playerId shouldBe d.player1
            pick.options shouldContainExactlyInAnyOrder listOf(courser, bolt, lions)
            pick.minSelections shouldBe 0
            pick.maxSelections shouldBe 2
        }
        d.submitCardSelection(d.player1, listOf(courser, bolt))

        // One search per exiled card, in whichever order the loop visits them.
        repeat(2) {
            val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            search.playerId shouldBe d.player1
            search.minSelections shouldBe 0
            if (courserInGraveyard in search.options) {
                withClue("the Courser search spans graveyard and library") {
                    search.options shouldContainExactlyInAnyOrder listOf(courserInGraveyard, courserInLibrary)
                }
                // "Any number": leave the library Courser behind.
                d.submitCardSelection(d.player1, listOf(courserInGraveyard))
            } else {
                withClue("the Bolt search finds only same-named cards") {
                    search.options shouldContainExactlyInAnyOrder listOf(boltInLibrary)
                }
                d.submitCardSelection(d.player1, listOf(boltInLibrary))
            }
        }

        d.pendingDecision shouldBe null
        val exile = d.getExile(d.player2)
        listOf(courser, bolt, courserInGraveyard, boltInLibrary).forEach { exile shouldContain it }
        exile shouldNotContain courserInLibrary
        withClue("the unpicked Lions stays in hand") { d.getHand(d.player2) shouldContain lions }
        withClue("the library lost only the Bolt; the left-behind Courser is still there") {
            d.library(d.player2).size shouldBe librarySize - 1
            d.library(d.player2) shouldContain courserInLibrary
        }
        d.getGraveyardCardNames(d.player1) shouldContain "Reap Intellect"
    }

    test("with X = 0 nothing is exiled and no search happens") {
        val d = driver()
        val courser = d.putCardInHand(d.player2, "Centaur Courser")
        val courserInGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val handSize = d.getHand(d.player2).size

        d.castWithX(0)

        // An up-to-zero pick may still be shown; it can only be declined.
        (d.pendingDecision as? SelectCardsDecision)?.let { pick ->
            pick.maxSelections shouldBe 0
            d.submitCardSelection(d.player1, emptyList())
        }

        withClue("no search decision follows an empty exile") { d.pendingDecision shouldBe null }
        d.getExile(d.player2) shouldBe emptyList()
        d.getHand(d.player2).size shouldBe handSize
        d.getHand(d.player2) shouldContain courser
        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)) shouldContain courserInGraveyard
    }
})
