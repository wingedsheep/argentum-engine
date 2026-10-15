package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Illicit Shipment (SNC #83) — {3}{B}{B} Sorcery.
 *
 *   Casualty 3. Search your library for a card, put it into your hand, then shuffle.
 *
 * Paying casualty with a power-3 creature copies the tutor, so one cast finds two cards.
 */
class IllicitShipmentScenarioTest : ScenarioTestBase() {

    init {
        test("casualty with a power-3 creature tutors twice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Illicit Shipment")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Swamp")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Illicit Shipment").single()
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(CastSpell(game.player1Id, spell, casualtyCreature = giant)).error shouldBe null
            withClue("the casualty creature is sacrificed as the spell is cast") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            val wanted = ArrayDeque(listOf("Grizzly Bears", "Savannah Lions"))
            var searches = 0
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                val decision = game.getPendingDecision()
                if (decision is SelectCardsDecision) {
                    val pick = game.findCardsInLibrary(1, wanted.removeFirst()).single()
                    game.selectCards(listOf(pick)).error shouldBe null
                    searches++
                } else {
                    game.resolveStack()
                }
            }

            withClue("the original and its casualty copy each searched once") {
                searches shouldBe 2
            }
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Savannah Lions") shouldBe true
            game.isInGraveyard(1, "Illicit Shipment") shouldBe true
        }
    }
}
