package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

class BattlewingMysticScenarioTest : ScenarioTestBase() {
    init {
        test("kicked Mystic discards the old hand before drawing two new cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Battlewing Mystic")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(2, "Swamp")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mystic = game.findCardsInHand(1, "Battlewing Mystic").single()
            game.execute(CastSpell(
                game.player1Id, mystic,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay
            )).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }

            game.isOnBattlefield("Battlewing Mystic") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.graveyardSize(1) shouldBe 2
            game.findCardsInHand(1, "Plains").size shouldBe 2
            game.handSize(1) shouldBe 2
            game.librarySize(1) shouldBe 1
            game.isInHand(2, "Swamp") shouldBe true
            game.state.pendingDecision shouldBe null
        }

        test("unkicked Mystic leaves the hand and library alone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Battlewing Mystic")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Battlewing Mystic").error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }

            game.isOnBattlefield("Battlewing Mystic") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 1
            game.graveyardSize(1) shouldBe 0
            game.librarySize(1) shouldBe 2
            game.state.pendingDecision shouldBe null
        }

        test("kicked Mystic draws two even with an empty hand after casting") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Battlewing Mystic")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mystic = game.findCardsInHand(1, "Battlewing Mystic").single()
            game.execute(CastSpell(
                game.player1Id, mystic,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay
            )).error shouldBe null
            game.handSize(1) shouldBe 0
            game.resolveStack().forEach { it.error shouldBe null }

            game.isOnBattlefield("Battlewing Mystic") shouldBe true
            game.handSize(1) shouldBe 2
            game.graveyardSize(1) shouldBe 0
            game.librarySize(1) shouldBe 1
            game.state.pendingDecision shouldBe null
        }
    }
}
