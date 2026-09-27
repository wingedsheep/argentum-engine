package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.shouldBe

/**
 * Devouring Rage (CHK #164) — "As an additional cost to cast this spell, you may sacrifice any number
 * of Spirits. Target creature gets +3/+0 until end of turn. For each Spirit sacrificed this way, that
 * creature gets an additional +3/+0 until end of turn."
 */
class DevouringRageScenarioTest : ScenarioTestBase() {

    private fun TestGame.castRage(target: EntityId, sacrificed: List<EntityId>?) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Devouring Rage").single(),
            targets = listOf(ChosenTarget.Permanent(target)),
            additionalCostPayment = sacrificed?.let { AdditionalCostPayment(variableCostPermanents = it) },
        )
    )

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Devouring Rage")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardOnBattlefield(1, "Kami of Old Stone")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Devouring Rage") {

            test("a sacrificed Spirit adds another +3/+0") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castRage(bears, listOf(game.findPermanent("Kami of Old Stone")!!)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Kami of Old Stone") shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 8
                game.state.projectedState.getToughness(bears) shouldBe 2
            }

            test("with no sacrifice it is +3/+0") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castRage(bears, null).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 5
            }
        }
    }
}
