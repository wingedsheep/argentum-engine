package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gravity Negator — {3}{U} 2/3 Eldrazi Drone.
 * "Devoid. Flying. Whenever this creature attacks, you may pay {C}. If you do, another target
 * creature gains flying until end of turn."
 */
class GravityNegatorScenarioTest : ScenarioTestBase() {

    /** Answer prompts until the stack is empty: [target] for the trigger, [pay] to the {C} offer. */
    private fun TestGame.drive(target: EntityId, pay: Boolean) {
        var guard = 0
        while (guard++ < 40) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> selectTargets(listOf(target))
                is YesNoDecision -> answerYesNo(pay)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("Unexpected decision: $decision")
            }
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Gravity Negator")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Wastes", 1)
        .withCardInLibrary(1, "Wastes")
        .withCardInLibrary(2, "Wastes")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("devoid flier: colorless and flying") {
            val game = board()
            val negator = game.findPermanent("Gravity Negator")!!
            game.state.projectedState.getColors(negator) shouldBe emptySet()
            game.state.projectedState.hasKeyword(negator, Keyword.FLYING) shouldBe true
        }

        test("paying {C} on attack gives another target creature flying") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Gravity Negator" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.drive(target = bears, pay = true)

            withClue("the Wastes paid {C}") {
                game.state.getEntity(game.findPermanent("Wastes")!!)?.has<TappedComponent>() shouldBe true
            }
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("declining the payment grants nothing") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Gravity Negator" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.drive(target = bears, pay = false)

            game.state.getEntity(game.findPermanent("Wastes")!!)?.has<TappedComponent>() shouldBe false
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
        }
    }
}
