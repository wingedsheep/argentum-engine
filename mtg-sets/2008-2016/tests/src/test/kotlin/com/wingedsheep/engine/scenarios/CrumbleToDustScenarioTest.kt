package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.CrumbleToDust
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
 * Crumble to Dust (BFZ #128) — "Exile target nonbasic land. Search its controller's graveyard,
 * hand, and library for any number of cards with the same name as that land and exile them. Then
 * that player shuffles."
 *
 * The name is captured off the battlefield target, and "its controller" is read after the land
 * has already been exiled — so a stolen land sends the search through the thief's zones.
 */
class CrumbleToDustScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CrumbleToDust)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castAt(land: EntityId): SelectCardsDecision {
        giveMana(player1, Color.RED, 4)
        val spell = putCardInHand(player1, "Crumble to Dust")
        val cast = castSpellWithTargets(player1, spell, listOf(ChosenTarget.Permanent(land)))
        withClue(cast.error ?: "casting Crumble to Dust failed") { cast.outcome shouldBe Outcome.Done }
        bothPass()
        return pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("exiles the land, then any number of same-named cards from its controller's zones") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player2, "Wasteland")
        val inGraveyard = d.putCardInGraveyard(d.player2, "Wasteland")
        val inHand = d.putCardInHand(d.player2, "Wasteland")
        val inLibrary = d.putCardOnTopOfLibrary(d.player2, "Wasteland")
        val otherHand = d.putCardInHand(d.player2, "Savannah Lions")
        val librarySize = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size

        val select = d.castAt(land)

        withClue("the land is already exiled when the search happens") {
            d.getExile(d.player2) shouldContain land
        }
        withClue("the caster picks any number of the same-named cards") {
            select.playerId shouldBe d.player1
            select.options shouldContainExactlyInAnyOrder listOf(inGraveyard, inHand, inLibrary)
            select.minSelections shouldBe 0
        }
        // "Any number": leave the library copy behind.
        d.submitCardSelection(d.player1, listOf(inGraveyard, inHand))

        val exile = d.getExile(d.player2)
        listOf(land, inGraveyard, inHand).forEach { exile shouldContain it }
        exile shouldNotContain inLibrary
        d.getHand(d.player2) shouldContain otherHand
        d.getHand(d.player2) shouldNotContain inHand
        withClue("the library copy stays in the library") {
            d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)) shouldContain inLibrary
            d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size shouldBe librarySize
        }
        d.getGraveyardCardNames(d.player1) shouldContain "Crumble to Dust"
    }

    test("a stolen land sends the search through its controller's zones, not its owner's") {
        val d = driver()
        val stolen = d.putLandOnBattlefield(d.player2, "Wasteland")
        d.replaceState(d.state.updateEntity(stolen) { it.with(ControllerComponent(d.player1)) })
        val controllersCopy = d.putCardInGraveyard(d.player1, "Wasteland")
        val ownersCopy = d.putCardInGraveyard(d.player2, "Wasteland")

        val select = d.castAt(stolen)

        withClue("the exiled land goes to its owner's exile") { d.getExile(d.player2) shouldContain stolen }
        withClue("only the controller's (the caster's) same-named cards are found") {
            select.options shouldContainExactlyInAnyOrder listOf(controllersCopy)
        }
        d.submitCardSelection(d.player1, listOf(controllersCopy))

        d.getExile(d.player1) shouldContain controllersCopy
        d.getGraveyard(d.player2) shouldContain ownersCopy
    }
})
