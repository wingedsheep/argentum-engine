package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Dimir Machinations — "Look at the top three cards of target player's library. Exile any number
 * of those cards, then put the rest back in any order."
 *
 * The scenario builder appends library cards top-down, so the first `withCardInLibrary` call is
 * the top card.
 */
class DimirMachinationsScenarioTest : ScenarioTestBase() {
    init {
        test("exiles the chosen cards and returns the rest to the top") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Dimir Machinations")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpellTargetingPlayer(1, "Dimir Machinations", 2).error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.options.size shouldBe 3
            val bears = game.findCardsInLibrary(2, "Grizzly Bears").single()
            val giant = game.findCardsInLibrary(2, "Hill Giant").single()
            game.selectCards(listOf(bears, giant)).error shouldBe null
            // A single returning card was just shown in the selection, so there is no order prompt.
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Hill Giant") shouldBe true
            // Island was the third card looked at and goes back; Forest was never touched.
            game.state.getZone(game.player2Id, Zone.LIBRARY).size shouldBe 2
            game.findCardsInLibrary(2, "Island").size shouldBe 1
            game.findCardsInLibrary(2, "Forest").size shouldBe 1
        }

        test("exiling nothing puts all three back") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Dimir Machinations")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpellTargetingPlayer(1, "Dimir Machinations", 2).error shouldBe null
            game.resolveStack()
            game.selectCards(emptyList()).error shouldBe null
            // Three cards go back, so the controller orders them; keep them as they are.
            game.keepLibraryOrder().error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player2Id, Zone.LIBRARY).size shouldBe 3
            game.state.getZone(game.player2Id, Zone.EXILE).size shouldBe 0
        }

        test("the caster's own library is untouched when an opponent is targeted") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Dimir Machinations")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpellTargetingPlayer(1, "Dimir Machinations", 2).error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as SelectCardsDecision
            // Only the two cards P2 actually has, never P1's.
            decision.options.size shouldBe 2
            game.selectCards(emptyList()).error shouldBe null
            game.keepLibraryOrder().error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.LIBRARY).size shouldBe 1
        }

        test("transmute searches for a card with mana value three") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Dimir Machinations")
                .withCardInLibrary(1, "Dimir Machinations")
                .withCardInLibrary(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            val source = game.findCardsInHand(1, "Dimir Machinations").single()
            val ability = cardRegistry.getCard("Dimir Machinations")!!
                .activatedAbilities.single { it.activateFromZone == Zone.HAND }
            game.execute(ActivateAbility(game.player1Id, source, ability.id)).error shouldBe null
            game.isInGraveyard(1, "Dimir Machinations") shouldBe true
            game.resolveStack()

            // Hill Giant's mana value is four, so only the second copy matches.
            val match = game.findCardsInLibrary(1, "Dimir Machinations").single()
            (game.state.pendingDecision as SelectCardsDecision).options shouldBe listOf(match)
            game.selectCards(listOf(match)).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Dimir Machinations") shouldBe true
        }
    }
}
