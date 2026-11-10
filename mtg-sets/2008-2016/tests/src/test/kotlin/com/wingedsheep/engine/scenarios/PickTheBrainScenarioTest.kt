package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.PickTheBrain
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
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pick the Brain (SOI #129) — "Target opponent reveals their hand. You choose a nonland card from
 * it and exile that card. Delirium — If there are four or more card types among cards in your
 * graveyard, search that player's graveyard, hand, and library for any number of cards with the
 * same name as the exiled card, exile those cards, then that player shuffles."
 */
class PickTheBrainScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PickTheBrain)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Artifact, creature, instant, land — delirium. */
    fun GameTestDriver.enableDelirium() {
        putCardInGraveyard(player1, "Artifact Creature")
        putCardInGraveyard(player1, "Lightning Bolt")
        putCardInGraveyard(player1, "Swamp")
    }

    fun GameTestDriver.castPickTheBrain() {
        giveMana(player1, Color.BLACK, 3)
        val spell = putCardInHand(player1, "Pick the Brain")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Player(player2)))
        withClue(cast.error ?: "casting Pick the Brain failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
    }

    test("without delirium only the chosen nonland card is exiled") {
        val d = driver()
        // Creature, instant, land — three types; Pick the Brain on the stack doesn't count.
        d.putCardInGraveyard(d.player1, "Centaur Courser")
        d.putCardInGraveyard(d.player1, "Lightning Bolt")
        d.putCardInGraveyard(d.player1, "Swamp")
        val courserA = d.putCardInHand(d.player2, "Centaur Courser")
        val courserB = d.putCardInHand(d.player2, "Centaur Courser")
        val lions = d.putCardInHand(d.player2, "Savannah Lions")
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val landInHand = d.getHand(d.player2).first { d.getCardName(it) == "Swamp" }

        d.castPickTheBrain()

        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the caster chooses from the revealed hand, nonland cards only") {
            choice.playerId shouldBe d.player1
            choice.options shouldContainExactlyInAnyOrder listOf(courserA, courserB, lions)
            choice.options shouldNotContain landInHand
        }
        d.submitCardSelection(d.player1, listOf(courserA))

        withClue("no delirium search follows") { d.isPaused shouldBe false }
        d.getExile(d.player2) shouldBe listOf(courserA)
        d.getHand(d.player2) shouldContain courserB
        d.getGraveyard(d.player2) shouldContain inGraveyard
        d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)) shouldContain inLibrary
    }

    test("with delirium the caster may exile any number of same-named cards from all three zones") {
        val d = driver()
        d.enableDelirium()
        val courserA = d.putCardInHand(d.player2, "Centaur Courser")
        val courserB = d.putCardInHand(d.player2, "Centaur Courser")
        val lions = d.putCardInHand(d.player2, "Savannah Lions")
        val inGraveyard = d.putCardInGraveyard(d.player2, "Centaur Courser")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Centaur Courser")
        val librarySize = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size

        d.castPickTheBrain()
        d.submitCardSelection(d.player1, listOf(courserA))

        val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the search offers every other card with the exiled card's name") {
            search.playerId shouldBe d.player1
            search.options shouldContainExactlyInAnyOrder listOf(courserB, inGraveyard, inLibrary)
            search.minSelections shouldBe 0
        }
        // "Any number" — leave the library copy behind.
        d.submitCardSelection(d.player1, listOf(courserB, inGraveyard))

        d.isPaused shouldBe false
        d.getExile(d.player2) shouldContainExactlyInAnyOrder listOf(courserA, courserB, inGraveyard)
        d.getHand(d.player2) shouldContain lions
        val library = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY))
        library shouldContain inLibrary
        library.size shouldBe librarySize
        d.getGraveyardCardNames(d.player1) shouldContain "Pick the Brain"
    }

    test("with delirium but no nonland card in hand, nothing is exiled and the spell still resolves") {
        val d = driver()
        d.enableDelirium()
        d.putCardInGraveyard(d.player2, "Centaur Courser")
        val handSize = d.getHand(d.player2).size

        d.castPickTheBrain()
        var guard = 0
        while (d.pendingDecision is SelectCardsDecision && guard++ < 5) {
            val decision = d.pendingDecision as SelectCardsDecision
            withClue("nothing is selectable") { decision.options shouldBe emptyList() }
            d.submitCardSelection(d.player1, emptyList())
        }

        d.isPaused shouldBe false
        d.getExile(d.player2) shouldBe emptyList()
        d.getHand(d.player2).size shouldBe handSize
        d.getGraveyardCardNames(d.player2) shouldContain "Centaur Courser"
        d.getGraveyardCardNames(d.player1) shouldContain "Pick the Brain"
    }
})
