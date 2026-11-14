package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.InfiniteObliteration
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
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Infinite Obliteration (ORI #103) — "Choose a creature card name. Search target opponent's
 * graveyard, hand, and library for any number of cards with that name and exile them. Then that
 * player shuffles."
 */
class InfiniteObliterationScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + InfiniteObliteration)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castAt(target: EntityId): ChooseOptionDecision {
        giveMana(player1, Color.BLACK, 3)
        val spell = putCardInHand(player1, "Infinite Obliteration")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(target)))
        withClue(cast.error ?: "casting Infinite Obliteration failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("only creature card names are offered, artifact creatures included") {
        val d = driver()
        val decision = d.castAt(d.player2)

        withClue("the caster names the card") { decision.playerId shouldBe d.player1 }
        decision.options shouldContain "Centaur Courser"
        decision.options shouldContain "Artifact Creature"
        withClue("noncreature spell names are not offered") {
            decision.options shouldNotContain "Lightning Bolt"
            decision.options shouldNotContain "Infinite Obliteration"
        }
        withClue("noncreature artifact and land names are not offered") {
            decision.options shouldNotContain "Legend Rule Waiver"
            decision.options shouldNotContain "Swamp"
        }
    }

    test("the caster can't target themselves") {
        val d = driver()
        d.giveMana(d.player1, Color.BLACK, 3)
        val spell = d.putCardInHand(d.player1, "Infinite Obliteration")
        val cast = d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Player(d.player1)))
        cast.outcome shouldNotBe Outcome.Done
    }

    test("exiles the chosen matches from graveyard, hand and library; the rest stay") {
        val d = driver()
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inHandA = d.putCardInHand(d.player2, "Centaur Courser")
        val inHandB = d.putCardInHand(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val otherHand = d.putCardInHand(d.player2, "Savannah Lions")
        val handSize = d.getHand(d.player2).size
        val librarySize = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size

        val naming = d.castAt(d.player2)
        d.submitDecision(d.player1, OptionChosenResponse(naming.id, naming.options.indexOf("Centaur Courser")))

        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the caster picks any number of the matches") {
            select.playerId shouldBe d.player1
            select.options shouldContainExactlyInAnyOrder listOf(inGraveyard, inHandA, inHandB, inLibrary)
            select.minSelections shouldBe 0
        }
        // "Any number" lets the caster leave the library copy behind.
        d.submitCardSelection(d.player1, listOf(inGraveyard, inHandA, inHandB))

        val exile = d.getExile(d.player2)
        listOf(inGraveyard, inHandA, inHandB).forEach { exile shouldContain it }
        exile shouldNotContain inLibrary
        withClue("no draw follows: the hand is two cards smaller") {
            d.getHand(d.player2).size shouldBe handSize - 2
            d.getHand(d.player2) shouldContain otherHand
        }
        withClue("the library copy stays in the (shuffled) library") {
            d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size shouldBe librarySize
            d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)) shouldContain inLibrary
        }
        d.getGraveyardCardNames(d.player1) shouldContain "Infinite Obliteration"
    }
})
