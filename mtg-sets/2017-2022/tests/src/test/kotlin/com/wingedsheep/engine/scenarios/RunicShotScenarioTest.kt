package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RunicShotScenarioTest : ScenarioTestBase() {
    init {
        for (kicked in listOf(false, true)) {
            test("${if (kicked) "kicked" else "unkicked"} Shot destroys before its optional scry") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Runic Shot")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val library = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Runic Shot").single(),
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)),
                    declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                game.resolveStack().forEach { it.error shouldBe null }
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                if (kicked) {
                    val choice = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                    choice.options shouldBe library.take(2)
                    game.selectCards(listOf(library.first())).error shouldBe null
                    val order = game.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
                    game.submitDecision(OrderedResponse(order.id, order.cards)).error shouldBe null
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)) shouldBe
                        listOf(library[1], library[2], library[0])
                } else {
                    game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)) shouldBe library
                }
                game.state.pendingDecision shouldBe null
                game.isInGraveyard(1, "Runic Shot") shouldBe true
            }
        }

        test("an untapped creature cannot be targeted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Runic Shot")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Runic Shot", game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }

        test("a kicked Shot whose target leaves does not scry") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Runic Shot")
                .withCardInHand(2, "Unsummon")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val library = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
            game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Runic Shot").single(),
                targets = listOf(ChosenTarget.Permanent(giant)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            )).error shouldBe null
            game.passPriority()
            game.castSpell(2, "Unsummon", targetId = giant).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }
            game.isInHand(2, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Runic Shot") shouldBe true
            game.state.pendingDecision shouldBe null
            game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)) shouldBe library
        }
    }
}
