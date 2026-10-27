package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kitesail Cleric (ZNR #20) — {W} Creature — Kor Cleric, 1/1, Kicker {2}{W}, Flying.
 *
 *   When this creature enters, if it was kicked, tap up to two target creatures.
 */
class KitesailClericScenarioTest : ScenarioTestBase() {

    init {
        context("Kitesail Cleric's kicked enters trigger") {

            fun game() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Kitesail Cleric")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("kicked, it taps up to two target creatures") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val cleric = game.findCardsInHand(1, "Kitesail Cleric").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cleric,
                        targets = emptyList(),
                        declaredCostSlot = ChoiceSlot.KICKED,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                withClue("both targeted creatures are tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                }
            }

            test("unkicked, the trigger does not fire") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Kitesail Cleric").error shouldBe null
                game.resolveStack()

                withClue("no target decision and nothing tapped") {
                    game.hasPendingDecision() shouldBe false
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
                game.findPermanent("Kitesail Cleric") shouldNotBe null
            }
        }
    }
}
