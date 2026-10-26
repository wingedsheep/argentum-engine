package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Glimpse the Cosmos (KHM #60) — "As long as you control a Giant, you may cast this card from your
 * graveyard by paying {U} rather than paying its mana cost. If you cast this card this way and it
 * would be put into your graveyard, exile it instead." The self-zone permission's
 * `exileCastThisWay` rider: armed only by the graveyard cast, and it catches a countered spell too.
 */
class GlimpseTheCosmosScenarioTest : ScenarioTestBase() {

    private fun game(giant: Boolean = true, inGraveyard: Boolean = true, islands: Int = 1, counterspell: Boolean = false) =
        scenario()
            .withPlayers()
            .let { if (giant) it.withCardOnBattlefield(1, "Berg Strider") else it.withCardOnBattlefield(1, "Grizzly Bears") }
            .withLandsOnBattlefield(1, "Island", islands)
            .let { if (inGraveyard) it.withCardInGraveyard(1, "Glimpse the Cosmos") else it.withCardInHand(1, "Glimpse the Cosmos") }
            .let { if (counterspell) it.withCardInHand(2, "Counterspell").withLandsOnBattlefield(2, "Island", 2) else it }
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    private fun TestGame.graveyardCasts(glimpse: EntityId) =
        getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == glimpse }

    /** Keeps the second of the top three and leaves the bottomed pair in the order offered. */
    private fun TestGame.keepOne(): EntityId {
        val look = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        val kept = look.options[1]
        selectCards(listOf(kept))
        if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
        return kept
    }

    init {
        test("with a Giant, cast from the graveyard for {U}; it is exiled instead of returning to the graveyard") {
            val game = game()
            val glimpse = game.findCardsInGraveyard(1, "Glimpse the Cosmos").single()

            val offered = game.graveyardCasts(glimpse).single()
            offered.manaCostString shouldBe "{U}"
            offered.isAffordable shouldBe true
            (offered.action as CastSpell).alternativeCostType shouldBe AlternativeCostType.SELF_ZONE

            game.execute(offered.action).error shouldBe null
            game.resolveStack()
            val kept = game.keepOne()

            game.state.getHand(game.player1Id).contains(kept) shouldBe true
            game.isInGraveyard(1, "Glimpse the Cosmos") shouldBe false
            game.state.getExile(game.player1Id).contains(glimpse) shouldBe true
        }

        test("without a Giant there is no graveyard cast") {
            val game = game(giant = false)
            val glimpse = game.findCardsInGraveyard(1, "Glimpse the Cosmos").single()

            game.graveyardCasts(glimpse).size shouldBe 0
            game.execute(CastSpell(game.player1Id, glimpse, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ZONE)).error shouldNotBe null
        }

        test("cast from hand for {1}{U}, it goes to the graveyard as usual") {
            val game = game(inGraveyard = false, islands = 2)

            game.castSpell(1, "Glimpse the Cosmos").error shouldBe null
            game.resolveStack()
            game.keepOne()

            game.isInGraveyard(1, "Glimpse the Cosmos") shouldBe true
        }

        test("countered after a graveyard cast, it is still exiled") {
            val game = game(counterspell = true)
            val glimpse = game.findCardsInGraveyard(1, "Glimpse the Cosmos").single()

            game.execute(game.graveyardCasts(glimpse).single().action).error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Counterspell", "Glimpse the Cosmos").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Glimpse the Cosmos") shouldBe false
            game.state.getExile(game.player1Id).contains(glimpse) shouldBe true
        }
    }
}
