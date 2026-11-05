package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.BrambleweftBehemoth
import com.wingedsheep.mtg.sets.definitions.hou.cards.NissasEncouragement
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Nissa's Encouragement — {4}{G} Sorcery (Hour of Devastation #203)
 *
 * "Search your library and graveyard for a card named Forest, a card named Brambleweft Behemoth,
 *  and a card named Nissa, Genesis Mage. Reveal those cards, put them into your hand, then shuffle."
 *
 * One search across library *and* graveyard, capped at one card per name: the test puts the
 * Behemoth in the library and Forests in both zones, checks the offered pool spans both zones and
 * excludes non-matching cards, and that a second Forest is refused.
 */
class NissasEncouragementScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NissasEncouragement + BrambleweftBehemoth)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.handNames() =
        state.getHand(player1).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    fun GameTestDriver.cast() {
        val spell = putCardInHand(player1, "Nissa's Encouragement")
        giveMana(player1, Color.GREEN, 1)
        giveColorlessMana(player1, 4)
        castSpell(player1, spell).error shouldBe null
        bothPass()
    }

    test("finds one card of each name across library and graveyard") {
        val d = driver()
        val behemoth = d.putCardOnTopOfLibrary(d.player1, "Brambleweft Behemoth")
        val libraryForest = d.putCardOnTopOfLibrary(d.player1, "Forest")
        val graveyardForest = d.putCardInGraveyard(d.player1, "Forest")
        d.putCardInGraveyard(d.player1, "Plains")

        d.cast()

        val decision = d.pendingDecision as SelectCardsDecision
        withClue("the pool is every named card in both zones, and nothing else") {
            decision.options shouldContainExactlyInAnyOrder listOf(behemoth, libraryForest, graveyardForest)
        }

        withClue("only one card named Forest may be taken") {
            d.submitCardSelection(d.player1, listOf(behemoth, libraryForest, graveyardForest))
                .error shouldBe "Too many cards selected: maximum is 2"
        }

        d.submitCardSelection(d.player1, listOf(behemoth, graveyardForest)).error shouldBe null

        withClue("one Forest and the Behemoth reach hand") {
            d.handNames().count { it == "Forest" } shouldBe 1
            d.handNames().count { it == "Brambleweft Behemoth" } shouldBe 1
        }
        withClue("the graveyard Forest was the one taken; the library Forest stays put") {
            (graveyardForest in d.state.getGraveyard(d.player1)) shouldBe false
            (libraryForest in d.state.getLibrary(d.player1)) shouldBe true
        }
    }

    test("finding none is legal") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Forest")
        val handBefore = d.getHandSize(d.player1)

        d.cast()
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null

        withClue("only the spell left hand") {
            d.getHandSize(d.player1) shouldBe handBefore
        }
    }
})
