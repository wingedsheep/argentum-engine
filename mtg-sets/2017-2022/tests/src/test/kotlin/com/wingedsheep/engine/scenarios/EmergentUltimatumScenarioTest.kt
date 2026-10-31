package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.EmergentUltimatum
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Emergent Ultimatum — {B}{B}{G}{G}{G}{U}{U} Sorcery (Ikoria #185)
 *
 * "Search your library for up to three monocolored cards with different names and exile them.
 *  An opponent chooses one of those cards. Shuffle that card into your library. You may cast the
 *  other cards without paying their mana costs. Exile Emergent Ultimatum."
 *
 * Covers the opponent's pick going back into the library, the free casts happening during
 * resolution, uncast cards staying in exile, the lone-card ruling, and the self-exile.
 */
class EmergentUltimatumScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + EmergentUltimatum)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castUltimatum() {
        val ultimatum = putCardInHand(player1, "Emergent Ultimatum")
        giveMana(player1, Color.BLACK, 2)
        giveMana(player1, Color.GREEN, 3)
        giveMana(player1, Color.BLUE, 2)
        castSpell(player1, ultimatum).error shouldBe null
        bothPass()
    }

    test("the opponent's pick is shuffled away; the others may be cast for free during resolution") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val guide = d.putCardOnTopOfLibrary(d.player1, "Goblin Guide")
        val lions = d.putCardOnTopOfLibrary(d.player1, "Savannah Lions")

        d.castUltimatum()

        withClue("the caster searches") { d.pendingDecision?.playerId shouldBe d.player1 }
        d.submitCardSelection(d.player1, listOf(courser, guide, lions)).error shouldBe null

        withClue("an opponent chooses the card to shuffle away") {
            d.pendingDecision?.playerId shouldBe d.player2
        }
        d.submitCardSelection(d.player2, listOf(guide)).error shouldBe null

        withClue("the chosen card is shuffled into the caster's library") {
            (guide in d.state.getLibrary(d.player1)) shouldBe true
        }

        val castChoice = d.pendingDecision
        withClue("the caster is offered the other two cards to cast during resolution") {
            (castChoice is SelectCardsDecision) shouldBe true
            castChoice as SelectCardsDecision
            castChoice.playerId shouldBe d.player1
            castChoice.options.toSet() shouldBe setOf(courser, lions)
        }
        d.submitCardSelection(d.player1, listOf(courser)).error shouldBe null
        // Decline the remaining card if offered again.
        (d.pendingDecision as? SelectCardsDecision)?.let { d.submitCardSelection(d.player1, emptyList()) }

        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        withClue("the free Centaur Courser resolves") {
            d.findPermanent(d.player1, "Centaur Courser") shouldNotBe null
        }
        withClue("the card not cast stays in exile") {
            (lions in d.getExile(d.player1)) shouldBe true
        }
        withClue("Emergent Ultimatum exiles itself") {
            d.getExileCardNames(d.player1).contains("Emergent Ultimatum") shouldBe true
            d.getGraveyardCardNames(d.player1).contains("Emergent Ultimatum") shouldBe false
        }
    }

    test("finding only one card shuffles it away and nothing is cast") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")

        d.castUltimatum()

        d.submitCardSelection(d.player1, listOf(courser)).error shouldBe null

        withClue("the lone card goes back into the library (ruling)") {
            (courser in d.state.getLibrary(d.player1)) shouldBe true
        }
        withClue("nothing is left to cast") {
            (d.pendingDecision is SelectCardsDecision) shouldBe false
            d.findPermanent(d.player1, "Centaur Courser") shouldBe null
        }
        withClue("Emergent Ultimatum exiles itself") {
            d.getExileCardNames(d.player1).contains("Emergent Ultimatum") shouldBe true
        }
    }

    test("\"with different names\" refuses a second copy of a name") {
        val d = driver()
        val courser1 = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val courser2 = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")

        d.castUltimatum()

        // Either the selection is rejected outright or the duplicate is dropped. Checked before the
        // opponent's pick: without the restriction both copies would sit in exile here.
        d.submitCardSelection(d.player1, listOf(courser1, courser2))
        withClue("at most one Centaur Courser may be exiled") {
            val exile = d.getExile(d.player1)
            listOf(courser1, courser2).count { it in exile } shouldBeLessThanOrEqual 1
        }
    }
})
