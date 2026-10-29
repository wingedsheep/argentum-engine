package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.Necromentia
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Necromentia (M21 #116) — "Choose a card name other than a basic land card name. Search target
 * opponent's graveyard, hand, and library for any number of cards with that name and exile them.
 * That player shuffles, then creates a 2/2 black Zombie creature token for each card exiled from
 * their hand this way."
 *
 * Pins the "any number of" selection, the hand-only token count, and that the tokens go to the
 * targeted opponent.
 */
class NecromentiaScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Necromentia)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.cast(): ChooseOptionDecision {
        giveMana(player1, Color.BLACK, 3)
        val spell = putCardInHand(player1, "Necromentia")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(player2)))
        withClue(cast.error ?: "casting Necromentia failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("basic land names are not offered") {
        val d = driver()
        val decision = d.cast()

        decision.playerId shouldBe d.player1
        decision.options shouldContain "Centaur Courser"
        decision.options shouldNotContain "Swamp"
        decision.options shouldNotContain "Forest"
    }

    test("exiles the chosen cards and the opponent gets a Zombie per card exiled from their hand") {
        val d = driver()
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inHandA = d.putCardInHand(d.player2, "Centaur Courser")
        val inHandB = d.putCardInHand(d.player2, "Centaur Courser")
        val inHandKept = d.putCardInHand(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val otherHand = d.putCardInHand(d.player2, "Savannah Lions")

        val naming = d.cast()
        d.submitDecision(d.player1, OptionChosenResponse(naming.id, naming.options.indexOf("Centaur Courser")))

        val selection = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        selection.playerId shouldBe d.player1
        selection.options shouldContainExactlyInAnyOrder listOf(inGraveyard, inHandA, inHandB, inHandKept, inLibrary)
        d.submitCardSelection(d.player1, listOf(inGraveyard, inHandA, inHandB, inLibrary)).error shouldBe null

        val exile = d.getExile(d.player2)
        listOf(inGraveyard, inHandA, inHandB, inLibrary).forEach { exile shouldContain it }
        d.getHand(d.player2) shouldContain inHandKept
        d.getHand(d.player2) shouldContain otherHand

        withClue("two cards left their hand, so they create two Zombies") {
            d.getCreatures(d.player2).map { d.getCardName(it) } shouldBe listOf("Zombie Token", "Zombie Token")
        }
        withClue("the caster gets no tokens") { d.getCreatures(d.player1).size shouldBe 0 }
    }

    test("choosing nothing exiles nothing and creates no tokens") {
        val d = driver()
        val inHand = d.putCardInHand(d.player2, "Centaur Courser")

        val naming = d.cast()
        d.submitDecision(d.player1, OptionChosenResponse(naming.id, naming.options.indexOf("Centaur Courser")))
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null

        d.getHand(d.player2) shouldContain inHand
        d.getCreatures(d.player2).size shouldBe 0
    }
})
