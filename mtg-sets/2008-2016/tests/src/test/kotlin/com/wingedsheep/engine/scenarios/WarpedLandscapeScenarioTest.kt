package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.WarpedLandscape
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Warped Landscape (SOI #280) — Land.
 *
 *   {T}: Add {C}.
 *   {2}, {T}, Sacrifice this land: Search your library for a basic land card, put it onto the
 *   battlefield tapped, then shuffle.
 */
class WarpedLandscapeScenarioTest : FunSpec({

    val fetchAbility = WarpedLandscape.activatedAbilities[1].id

    test("sacrificing it fetches a basic land onto the battlefield tapped") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(WarpedLandscape))
        d.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!

        val landscape = d.putLandOnBattlefield(you, "Warped Landscape")
        val plains = d.putCardOnTopOfLibrary(you, "Plains")
        d.giveColorlessMana(you, 2)

        d.submit(
            ActivateAbility(playerId = you, sourceId = landscape, abilityId = fetchAbility)
        ).outcome shouldBe Outcome.Done
        d.getGraveyardCardNames(you).contains("Warped Landscape") shouldBe true

        d.bothPass()
        val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.options shouldBe listOf(plains)
        d.submitCardSelection(you, listOf(plains))

        d.getLands(you).contains(plains) shouldBe true
        d.isTapped(plains) shouldBe true
    }
})
