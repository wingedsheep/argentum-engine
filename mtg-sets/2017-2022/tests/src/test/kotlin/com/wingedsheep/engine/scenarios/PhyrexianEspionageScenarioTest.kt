package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PhyrexianEspionageScenarioTest : ScenarioTestBase() {
    init {
        for (kicked in listOf(false, true)) {
            test("${if (kicked) "kicked" else "unkicked"} Espionage draws first and only kicked makes the opponent choose a discard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Phyrexian Espionage")
                    .withCardInHand(1, "Mountain")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Phyrexian Espionage").single()
                game.execute(CastSpell(
                    game.player1Id, spell,
                    declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                    paymentStrategy = PaymentStrategy.AutoPay
                )).error shouldBe null
                game.resolveStack().forEach { it.error shouldBe null }

                game.findCardsInHand(1, "Plains").size shouldBe 2
                game.isInHand(1, "Mountain") shouldBe true
                game.librarySize(1) shouldBe 1
                if (kicked) {
                    val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                    decision.playerId shouldBe game.player2Id
                    game.handSize(2) shouldBe 2
                    val giant = game.findCardsInHand(2, "Hill Giant").single()
                    game.selectCards(listOf(giant)).error shouldBe null
                    game.resolveStack().forEach { it.error shouldBe null }
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.handSize(2) shouldBe 1
                } else {
                    game.handSize(2) shouldBe 2
                    game.graveyardSize(2) shouldBe 0
                }
                game.state.pendingDecision shouldBe null
                game.isInGraveyard(1, "Phyrexian Espionage") shouldBe true
            }
        }

        test("kicked Espionage still draws with an empty opposing hand and needs no discard choice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Phyrexian Espionage")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(CastSpell(
                game.player1Id, game.findCardsInHand(1, "Phyrexian Espionage").single(),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay
            )).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }

            game.findCardsInHand(1, "Plains").size shouldBe 2
            game.handSize(2) shouldBe 0
            game.graveyardSize(2) shouldBe 0
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Phyrexian Espionage") shouldBe true
        }
    }
}
