package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Burn at the Stake (AVR #130) — "As an additional cost to cast this spell, tap any number of
 * untapped creatures you control. Burn at the Stake deals damage to any target equal to three times
 * the number of creatures tapped this way."
 */
class BurnAtTheStakeScenarioTest : ScenarioTestBase() {

    private fun TestGame.castBurn(target: ChosenTarget, tapped: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Burn at the Stake").single(),
            targets = listOf(target),
            additionalCostPayment = AdditionalCostPayment(variableCostPermanents = tapped),
        )
    )

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Burn at the Stake")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = true)
        .withCardOnBattlefield(1, "Raging Goblin", tapped = true)
        .withCardOnBattlefield(2, "Craw Wurm")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Burn at the Stake") {

            test("offers your untapped creatures, summoning-sick ones included, as a tap-any-number picker") {
                val game = board()
                val info = game.getLegalActions(1).single { it.description == "Cast Burn at the Stake" }
                    .additionalCostInfo.shouldNotBeNull()
                info.costType shouldBe "TapVariable"
                info.tapCount shouldBe 0
                info.validTapTargets shouldContainExactlyInAnyOrder listOf(
                    game.findPermanent("Grizzly Bears")!!,
                    game.findPermanent("Hill Giant")!!,
                )
            }

            test("deals three damage for each creature tapped this way") {
                val game = board()
                val tapped = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)
                game.castBurn(ChosenTarget.Player(game.player2Id), tapped).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 14
            }

            test("one creature tapped deals 3 — not enough for a 6/4") {
                val game = board()
                val wurm = game.findPermanent("Craw Wurm")!!
                game.castBurn(ChosenTarget.Permanent(wurm), listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                game.resolveStack()
                game.findPermanent("Craw Wurm").shouldNotBeNull()
            }
        }
    }
}
