package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

class HurloonBattleHymnScenarioTest : ScenarioTestBase() {
    init {
        for (kicked in listOf(false, true)) {
            test("${if (kicked) "kicked" else "unkicked"} Hymn kills a creature and gains life only when kicked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hurloon Battle Hymn")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Hurloon Battle Hymn").single(),
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)),
                    declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                game.resolveStack().forEach { it.error shouldBe null }

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 2
                game.getLifeTotal(1) shouldBe if (kicked) 24 else 20
                game.getLifeTotal(2) shouldBe 20
                game.isInGraveyard(1, "Hurloon Battle Hymn") shouldBe true
                game.state.pendingDecision shouldBe null
            }
        }

        for (kicked in listOf(false, true)) {
            test("${if (kicked) "kicked" else "unkicked"} Hymn destroys a planeswalker by removing loyalty and gains life only when kicked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hurloon Battle Hymn")
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Hurloon Battle Hymn").single(),
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Jace Beleren")!!)),
                    declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                game.resolveStack().forEach { it.error shouldBe null }

                game.isOnBattlefield("Jace Beleren") shouldBe false
                game.isInGraveyard(2, "Jace Beleren") shouldBe true
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 2
                game.getLifeTotal(1) shouldBe if (kicked) 24 else 20
                game.getLifeTotal(2) shouldBe 20
                game.isInGraveyard(1, "Hurloon Battle Hymn") shouldBe true
                game.state.pendingDecision shouldBe null
            }
        }

        test("kicked Hymn with an illegal target gains no life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hurloon Battle Hymn")
                .withCardInHand(2, "Unsummon")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
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
                cardId = game.findCardsInHand(1, "Hurloon Battle Hymn").single(),
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
            game.isInGraveyard(1, "Hurloon Battle Hymn") shouldBe true
            game.state.pendingDecision shouldBe null
        }
    }
}
