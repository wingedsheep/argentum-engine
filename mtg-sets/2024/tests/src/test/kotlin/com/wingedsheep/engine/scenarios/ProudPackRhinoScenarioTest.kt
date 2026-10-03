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
 * Proud Pack-Rhino (MH3 #41) — {2}{W} Creature — Rhino, 3/3.
 *
 * "When this creature enters, choose one —
 *  • Put a shield counter on target permanent.
 *  • Proliferate."
 */
class ProudPackRhinoScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun addCounter(game: TestGame, id: EntityId, type: CounterType) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, 1))
        }
    }

    private fun board() = scenario()
        .withPlayers("You", "Opponent")
        .withCardInHand(1, "Proud Pack-Rhino")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Millstone")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun castAndReachModeChoice(game: TestGame): ChooseOptionDecision {
        val cast = game.castSpell(1, "Proud Pack-Rhino")
        withClue("Proud Pack-Rhino should cast: ${cast.error}") { cast.error shouldBe null }
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return game.getPendingDecision() as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision for the ETB; got ${game.getPendingDecision()}")
    }

    init {
        test("mode 0 puts a shield counter on any target permanent, including a noncreature") {
            val game = board()
            val millstone = game.findPermanent("Millstone")!!

            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 0))
            val targets = game.getPendingDecision() as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision; got ${game.getPendingDecision()}")
            game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(millstone))))
            game.resolveStack()

            count(game, millstone, CounterType.SHIELD) shouldBe 1
        }

        test("mode 1 proliferates only the chosen permanents and players") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            addCounter(game, giant, CounterType.PLUS_ONE_PLUS_ONE)
            addCounter(game, game.player2Id, CounterType.POISON)

            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 1))
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("the chosen giant gets another +1/+1 counter") {
                count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }
            withClue("the unchosen opponent keeps one poison counter") {
                count(game, game.player2Id, CounterType.POISON) shouldBe 1
            }
        }
    }
}
