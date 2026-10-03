package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Inspired Inventor (MH3 #32) — {2}{W} Creature — Human Artificer, 2/2.
 *
 * "When this creature enters, choose one —
 *  • You get {E}{E}{E}.
 *  • Put a +1/+1 counter on target creature.
 *  • Create a 1/1 colorless Servo artifact creature token."
 */
class InspiredInventorScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("You", "Opponent")
        .withCardInHand(1, "Inspired Inventor")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun castAndReachModeChoice(game: TestGame): ChooseOptionDecision {
        val cast = game.castSpell(1, "Inspired Inventor")
        withClue("Inspired Inventor should cast: ${cast.error}") { cast.error shouldBe null }
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return game.getPendingDecision() as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
    }

    private fun servos(game: TestGame): List<EntityId> =
        game.state.getBattlefield().filter { game.state.projectedState.hasSubtype(it, "Servo") }

    init {
        test("mode 0 gives three energy") {
            val game = board()
            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 0))
            game.resolveStack()

            count(game, game.player1Id, CounterType.ENERGY) shouldBe 3
            servos(game).size shouldBe 0
        }

        test("mode 1 puts a +1/+1 counter on any target creature") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 1))
            val targets = game.getPendingDecision() as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision; got ${game.getPendingDecision()}")
            game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(giant))))
            game.resolveStack()

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            count(game, game.player1Id, CounterType.ENERGY) shouldBe 0
        }

        test("mode 2 creates a 1/1 colorless Servo artifact creature token") {
            val game = board()
            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 2))
            game.resolveStack()

            val servo = servos(game).single()
            val projected = game.state.projectedState
            projected.hasType(servo, "ARTIFACT") shouldBe true
            projected.isCreature(servo) shouldBe true
            projected.getColors(servo).isEmpty() shouldBe true
            projected.getPower(servo) shouldBe 1
            projected.getToughness(servo) shouldBe 1
        }
    }
}
