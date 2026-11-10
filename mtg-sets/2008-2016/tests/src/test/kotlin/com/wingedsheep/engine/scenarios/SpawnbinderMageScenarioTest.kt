package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spawnbinder Mage (OGW) — {3}{W} 2/4 Human Wizard Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: Tap target creature."
 */
class SpawnbinderMageScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Spawnbinder Mage")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Spawnbinder Mage")
            .withCardOnBattlefield(1, "Kor Sky Climber")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("cohort taps target creature") {
            val game = board()
            val mage = game.findPermanent("Spawnbinder Mage")!!
            val ally = game.findPermanent("Kor Sky Climber")!!
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, mage, cohortId, listOf(ChosenTarget.Permanent(giant)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(mage)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board()
            val mage = game.findPermanent("Spawnbinder Mage")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, mage, cohortId, listOf(ChosenTarget.Permanent(giant)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
        }
    }
}
