package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

class TolarianGeyserScenarioTest : ScenarioTestBase() {
    init {
        for (kicked in listOf(false, true)) {
            test("${if (kicked) "kicked" else "unkicked"} Geyser bounces and draws with life only when kicked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tolarian Geyser")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Tolarian Geyser").single(),
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)),
                    declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                game.resolveStack().forEach { it.error shouldBe null }

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInHand(2, "Hill Giant") shouldBe true
                game.findCardsInHand(1, "Plains").size shouldBe 1
                game.librarySize(1) shouldBe 1
                game.getLifeTotal(1) shouldBe if (kicked) 23 else 20
                game.getLifeTotal(2) shouldBe 20
                game.isInGraveyard(1, "Tolarian Geyser") shouldBe true
                game.state.pendingDecision shouldBe null
            }
        }

        test("kicked Geyser with an illegal target draws no card and gains no life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tolarian Geyser")
                .withCardInHand(2, "Unsummon")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Tolarian Geyser").single(),
                targets = listOf(ChosenTarget.Permanent(giant)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            )).error shouldBe null
            game.passPriority()
            game.castSpell(2, "Unsummon", targetId = giant).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }

            game.isInHand(2, "Hill Giant") shouldBe true
            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 2
            game.getLifeTotal(1) shouldBe 20
            game.isInGraveyard(1, "Tolarian Geyser") shouldBe true
            game.state.pendingDecision shouldBe null
        }
    }
}
