package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Seasons Past (SOI #226) — {4}{G}{G} Sorcery.
 *
 *   Return any number of cards with different mana values from your graveyard to your hand. Put
 *   Seasons Past on the bottom of its owner's library.
 *
 * Exercises `SelectionRestriction.OnePerManaValue`: the decision carries the flag for the UI, and
 * the server keeps only the first card of each mana value the response names.
 */
class SeasonsPastScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Seasons Past")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Llanowar Elves")
        .withCardInGraveyard(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("returns one card of each mana value; a second card of a claimed value stays put") {
            val game = board()
            game.castSpell(1, "Seasons Past").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the decision tells the UI to keep mana values distinct") {
                decision.onePerManaValue shouldBe true
                decision.maxSelections shouldBe 3 // three distinct mana values: 2, 1, 0
            }

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears")
            // Both Bears (mana value 2) plus the Elves: the second Bears can't share a claimed value.
            game.selectCards(bears + game.findCardsInGraveyard(1, "Llanowar Elves")).error shouldBe null
            game.resolveStack()

            withClue("the first Bears and the Elves came back; the unpicked Forest stayed") {
                game.findCardsInHand(1, "Grizzly Bears") shouldBe listOf(bears.first())
                game.isInHand(1, "Llanowar Elves") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
            }
            withClue("the second mana-value-2 card was rejected and stayed in the graveyard") {
                game.findCardsInGraveyard(1, "Grizzly Bears") shouldBe listOf(bears.last())
            }
            withClue("Seasons Past went to the bottom of its owner's library, not the graveyard") {
                game.isInGraveyard(1, "Seasons Past") shouldBe false
                val library = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                game.state.getEntity(library.last())
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name shouldBe "Seasons Past"
            }
        }

        test("choosing nothing returns nothing and still bottoms the spell") {
            val game = board()
            game.castSpell(1, "Seasons Past").error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 4
            game.isInGraveyard(1, "Seasons Past") shouldBe false
        }
    }
}
