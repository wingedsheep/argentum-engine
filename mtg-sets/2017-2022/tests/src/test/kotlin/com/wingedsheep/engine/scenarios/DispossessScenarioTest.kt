package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.Dispossess
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dispossess (AKH #86) — "Choose an artifact card name. Search target opponent's graveyard, hand,
 * and library for any number of cards with the chosen name and exile them. Then that player
 * shuffles."
 *
 * Pins the artifact-only name pool and the "any number of" selection.
 */
class DispossessScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Dispossess)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.cast(): ChooseOptionDecision {
        giveMana(player1, Color.BLACK, 3)
        val spell = putCardInHand(player1, "Dispossess")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(player2)))
        withClue(cast.error ?: "casting Dispossess failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("only artifact card names are offered") {
        val d = driver()
        val decision = d.cast()

        withClue("the caster names the card") { decision.playerId shouldBe d.player1 }
        decision.options shouldContain "Artifact Creature"
        decision.options shouldContain "Legend Rule Waiver"
        decision.options shouldNotContain "Centaur Courser"
        decision.options shouldNotContain "Dispossess"
        decision.options shouldNotContain "Swamp"
    }

    test("exiles the chosen cards with that name and leaves the rest") {
        val d = driver()
        val inGraveyard = d.putCardInGraveyard(d.player2, "Artifact Creature")
        val inHand = d.putCardInHand(d.player2, "Artifact Creature")
        val inHandKept = d.putCardInHand(d.player2, "Artifact Creature")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Artifact Creature")
        val otherHand = d.putCardInHand(d.player2, "Legend Rule Waiver")
        val librarySize = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size

        val naming = d.cast()
        d.submitDecision(d.player1, OptionChosenResponse(naming.id, naming.options.indexOf("Artifact Creature")))

        val selection = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        selection.playerId shouldBe d.player1
        selection.options shouldContainExactlyInAnyOrder listOf(inGraveyard, inHand, inHandKept, inLibrary)
        val resolved = d.submitCardSelection(d.player1, listOf(inGraveyard, inHand, inLibrary))
        resolved.error shouldBe null
        withClue("then that player shuffles") {
            resolved.events.filterIsInstance<LibraryShuffledEvent>().map { it.playerId } shouldBe listOf(d.player2)
        }

        val exile = d.getExile(d.player2)
        listOf(inGraveyard, inHand, inLibrary).forEach { exile shouldContain it }
        d.getHand(d.player2) shouldContain inHandKept
        d.getHand(d.player2) shouldContain otherHand
        d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size shouldBe librarySize - 1
        d.getGraveyardCardNames(d.player1) shouldContain "Dispossess"
    }

    test("can't target its own controller") {
        val d = driver()
        d.giveMana(d.player1, Color.BLACK, 3)
        val spell = d.putCardInHand(d.player1, "Dispossess")
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Player(d.player1))).outcome shouldNotBe Outcome.Done
    }
})
