package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import io.kotest.matchers.shouldBe

class ZoralineCosmosCallerTest : ScenarioTestBase() {

    init {
        test("Zoraline ETB - pay costs and reanimate with finality counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Zoraline, Cosmos Caller")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Zoraline, Cosmos Caller")
            game.resolveStack()

            // OptionalCostEffect yes/no
            game.answerYesNo(true)

            // Pipeline: Gather -> Select (auto if 1) -> Move -> AddCounters
            if (game.hasPendingDecision()) {
                val cards = game.findCardsInGraveyard(1, "Glory Seeker")
                game.selectCards(cards)
            }

            game.isOnBattlefield("Glory Seeker") shouldBe true
            game.isInGraveyard(1, "Glory Seeker") shouldBe false

            val glorySeekerId = game.findPermanent("Glory Seeker")!!
            val counters = game.state.getEntity(glorySeekerId)?.get<CountersComponent>()
            counters?.getCount(CounterType.FINALITY) shouldBe 1

            game.getLifeTotal(1) shouldBe lifeBefore - 2
        }

        test("Zoraline ETB - declining costs does nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Zoraline, Cosmos Caller")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Zoraline, Cosmos Caller")
            game.resolveStack()

            game.answerYesNo(false)

            game.isInGraveyard(1, "Glory Seeker") shouldBe true
            game.getLifeTotal(1) shouldBe lifeBefore
        }

        test("Zoraline - bat attack triggers life gain") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Zoraline, Cosmos Caller")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .withActivePlayer(1)
                .build()

            val lifeBefore = game.getLifeTotal(1)

            game.declareAttackers(mapOf("Zoraline, Cosmos Caller" to 2))

            // Two triggers on stack (LIFO):
            // Top: one of the two triggers
            // Bottom: the other
            // Resolve top trigger — if it's the may-pay, answer no
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.answerYesNo(false)
            }

            // Resolve remaining trigger
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.answerYesNo(false)
            }

            // Player should have gained 1 life from the bat attack trigger
            game.getLifeTotal(1) shouldBe lifeBefore + 1
        }
    }
}
