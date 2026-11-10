package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Munda's Vanguard — {4}{W} 3/3 Kor Knight Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: Put a +1/+1 counter on each creature you control."
 */
class MundasVanguardScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Munda's Vanguard")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Munda's Vanguard")
            .withCardOnBattlefield(1, "Kor Sky Climber")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("cohort puts a +1/+1 counter on each creature you control, tapped ones included") {
            val game = board()
            val vanguard = game.findPermanent("Munda's Vanguard")!!
            val ally = game.findPermanent("Kor Sky Climber")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, vanguard, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(vanguard)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
            val projected = game.state.projectedState
            projected.getPower(vanguard) shouldBe 4
            projected.getToughness(vanguard) shouldBe 4
            projected.getPower(ally) shouldBe 4
            projected.getToughness(ally) shouldBe 3
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            withClue("the opponent's creature gets nothing") {
                projected.getPower(giant) shouldBe 3
                projected.getToughness(giant) shouldBe 3
            }
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board()
            val vanguard = game.findPermanent("Munda's Vanguard")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, vanguard, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.state.projectedState.getPower(vanguard) shouldBe 3
        }
    }
}
