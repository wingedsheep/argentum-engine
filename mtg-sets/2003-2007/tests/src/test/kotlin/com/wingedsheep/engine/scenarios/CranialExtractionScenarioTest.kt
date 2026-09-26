package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.CranialExtraction
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cranial Extraction (CHK #105) — "Choose a nonland card name. Search target player's graveyard,
 * hand, and library for all cards with that name and exile them. Then that player shuffles."
 */
class CranialExtractionScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CranialExtraction)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castAt(target: com.wingedsheep.sdk.model.EntityId): ChooseOptionDecision {
        giveMana(player1, Color.BLACK, 4)
        val spell = putCardInHand(player1, "Cranial Extraction")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(target)))
        withClue(cast.error ?: "casting Cranial Extraction failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("only nonland card names are offered") {
        val d = driver()
        val decision = d.castAt(d.player2)

        withClue("the caster names the card") { decision.playerId shouldBe d.player1 }
        decision.options shouldContain "Centaur Courser"
        decision.options shouldContain "Cranial Extraction"
        decision.options shouldNotContain "Swamp"
        decision.options shouldNotContain "Forest"
    }

    test("exiles every card with the chosen name from graveyard, hand, and library") {
        val d = driver()
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inHand = d.putCardInHand(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val otherHand = d.putCardInHand(d.player2, "Savannah Lions")
        val librarySize = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size

        val decision = d.castAt(d.player2)
        d.submitDecision(d.player1, OptionChosenResponse(decision.id, decision.options.indexOf("Centaur Courser")))

        val exile = d.getExile(d.player2)
        listOf(inGraveyard, inHand, inLibrary).forEach { exile shouldContain it }
        d.getHand(d.player2) shouldContain otherHand
        d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size shouldBe librarySize - 1
        d.getGraveyardCardNames(d.player1) shouldContain "Cranial Extraction"
    }

    test("can target its own controller") {
        val d = driver()
        val mine = d.putCardInGraveyard(d.player1, "Centaur Courser")
        val theirs = d.putCardInGraveyard(d.player2, "Centaur Courser")

        val decision = d.castAt(d.player1)
        d.submitDecision(d.player1, OptionChosenResponse(decision.id, decision.options.indexOf("Centaur Courser")))

        d.getExile(d.player1) shouldContain mine
        withClue("only the target player's cards are searched") {
            d.getGraveyardCardNames(d.player2) shouldContain "Centaur Courser"
            d.getExile(d.player2) shouldNotContain theirs
        }
    }
})
