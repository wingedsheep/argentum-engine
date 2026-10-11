package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dusk // Dawn (AKH) — the first aftermath card.
 *
 * Dusk {2}{W}{W}: "Destroy all creatures with power 3 or greater."
 * Dawn {3}{W}{W}, Aftermath: "Return all creature cards with power 2 or less from your graveyard to
 * your hand."
 *
 * The aftermath rules themselves are pinned by the engine's `AftermathScenarioTest`; this proves
 * the card end to end: Dusk from hand only kills the big creatures and lands in the graveyard, then
 * Dawn — offered only from there — returns just your small creature cards and exiles itself.
 */
class DuskDawnScenarioTest : ScenarioTestBase() {

    init {
        test("Dusk sweeps power 3+, then Dawn from the graveyard returns your power 2- creature cards") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Dusk // Dawn")
                .withLandsOnBattlefield(1, "Plains", 9)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(1, "Savannah Lions")
                .withCardInGraveyard(1, "Craw Wurm")
                .withCardInGraveyard(2, "Elite Vanguard")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Dusk // Dawn").first()
            val handOffers = game.getLegalActions(1).map { it.description }
            withClue("from hand Dusk is castable and Dawn (aftermath) is not: $handOffers") {
                handOffers.contains("Cast Dusk") shouldBe true
                handOffers.none { it.startsWith("Cast Dawn") } shouldBe true
            }

            game.execute(CastSpell(game.player1Id, cardId, faceIndex = 0)).error.shouldBeNull()
            game.resolveStack()

            withClue("Dusk destroys creatures with power 3 or greater only") {
                game.findPermanent("Hill Giant").shouldBeNull()
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
            }
            withClue("Dusk, cast from hand, goes to the graveyard") {
                game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe true
            }

            val dawn = game.getLegalActions(1).firstOrNull { it.description == "Cast Dawn (Aftermath)" }
            withClue("Dawn is offered from the graveyard") { dawn.shouldNotBeNull() }
            withClue("Dusk is not offered from the graveyard") {
                game.getLegalActions(1).none { it.description.startsWith("Cast Dusk") } shouldBe true
            }

            game.execute(dawn!!.action).error.shouldBeNull()
            game.resolveStack()

            val handNames = game.state.getHand(game.player1Id)
                .mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
            withClue("Dawn returns your power-2-or-less creature cards: $handNames") {
                handNames.contains("Savannah Lions") shouldBe true
            }
            withClue("a power-3+ creature card stays in the graveyard") {
                game.isInGraveyard(1, "Craw Wurm") shouldBe true
            }
            withClue("only your graveyard is affected") {
                game.isInGraveyard(2, "Elite Vanguard") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("Dawn, cast from the graveyard, is exiled") {
                game.state.getExile(game.player1Id).contains(cardId) shouldBe true
            }
        }
    }
}
