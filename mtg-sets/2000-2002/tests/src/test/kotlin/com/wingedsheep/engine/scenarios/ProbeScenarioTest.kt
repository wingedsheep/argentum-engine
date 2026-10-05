package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Probe — Invasion #66, {2}{U} Sorcery, Kicker {1}{B}
 *
 * "Draw three cards, then discard two cards. If this spell was kicked, target player discards two
 *  cards."
 *
 * CR 702.33g: the kicked clause's target is chosen only if the spell was kicked. So an unkicked
 * Probe is cast with no target at all, and a kicked one names the player who discards.
 */
class ProbeScenarioTest : ScenarioTestBase() {

    init {
        context("Probe") {

            fun game(kicked: Boolean) = scenario()
                .withPlayers("Caster", "Target")
                .withCardInHand(1, "Probe")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", if (kicked) 2 else 0)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("unkicked, it is cast with no target: the caster draws three and discards two") {
                val game = game(kicked = false)

                game.castSpell(1, "Probe").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 3
                game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player1Id
                game.selectCards(game.findCardsInHand(1, "Island").take(2)).error shouldBe null

                game.handSize(1) shouldBe 1
                game.handSize(2) shouldBe 3
                game.getPendingDecision() shouldBe null
            }

            test("kicked, the target player also discards two cards") {
                val game = game(kicked = true)
                val probe = game.findCardsInHand(1, "Probe").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = probe,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        declaredCostSlot = ChoiceSlot.KICKED,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player1Id
                game.selectCards(game.findCardsInHand(1, "Island").take(2)).error shouldBe null

                game.getPendingDecision().shouldNotBeNull().playerId shouldBe game.player2Id
                val bears = game.findCardsInHand(2, "Grizzly Bears").single()
                val courser = game.findCardsInHand(2, "Centaur Courser").single()
                game.selectCards(listOf(bears, courser)).error shouldBe null

                game.handSize(1) shouldBe 1
                game.handSize(2) shouldBe 1
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
            }

            test("kicked, it cannot be cast without a target player") {
                val game = game(kicked = true)
                val probe = game.findCardsInHand(1, "Probe").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = probe,
                        declaredCostSlot = ChoiceSlot.KICKED,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error.shouldNotBeNull()
                game.isInHand(1, "Probe") shouldBe true
            }
        }
    }
}
