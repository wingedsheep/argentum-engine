package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Memory's Journey (ISD #66) — {1}{U} Instant.
 *
 *   Target player shuffles up to three target cards from their graveyard into their library.
 *   Flashback {G}
 */
class MemorysJourneyScenarioTest : ScenarioTestBase() {

    init {
        test("shuffles the three chosen cards from the target player's graveyard into their library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Memory's Journey")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withCardInGraveyard(2, "Savannah Lions")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val p2 = game.player2Id
            val chosen = listOf("Grizzly Bears", "Hill Giant", "Lightning Bolt")
                .map { game.findCardsInGraveyard(2, it).single() }
            val journey = game.findCardsInHand(1, "Memory's Journey").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    journey,
                    listOf(ChosenTarget.Player(p2)) + chosen.map { ChosenTarget.Card(it, p2, Zone.GRAVEYARD) },
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.librarySize(2) shouldBe 5
            game.graveyardSize(2) shouldBe 1
            game.isInGraveyard(2, "Savannah Lions") shouldBe true
            game.findCardsInLibrary(2, "Hill Giant").size shouldBe 1
        }

        test("flashback {G} casts it from the graveyard and exiles it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Memory's Journey")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val p2 = game.player2Id
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            val journey = game.findCardsInGraveyard(1, "Memory's Journey").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    journey,
                    listOf(ChosenTarget.Player(p2), ChosenTarget.Card(bears, p2, Zone.GRAVEYARD)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                )
            )
            withClue("flashback cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.graveyardSize(2) shouldBe 0
            game.isInExile(1, "Memory's Journey") shouldBe true
        }
    }
}
