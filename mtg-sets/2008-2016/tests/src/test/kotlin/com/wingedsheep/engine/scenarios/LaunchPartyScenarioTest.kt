package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Launch Party (RTR) — As an additional cost, sacrifice a creature. Destroy target creature. Its
 * controller loses 2 life.
 */
class LaunchPartyScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing a creature destroys the target and its controller loses 2 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Launch Party")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val spell = game.findCardsInHand(1, "Launch Party").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(ChosenTarget.Permanent(giant)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            withClue("the sacrificed Bears are already in the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.resolveStack()

            withClue("Hill Giant destroyed, its controller lost 2 life, caster unaffected") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
