package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vodalian Mindsinger (DMU #76).
 *
 * "Kicker {1}{R} and/or {1}{G}" (two kicker abilities, CR 702.33b) — it enters with two +1/+1
 * counters per kicker paid, and its enters trigger steals a creature whose power is less than the
 * Mindsinger's own. The counters must already be there when the trigger picks its target, so each
 * kicker paid widens what it can take.
 */
class VodalianMindsingerScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Vodalian Mindsinger")
        .withLandsOnBattlefield(1, "Island", 5)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(2, "Savannah Lions") // 1/1
        .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
        .withCardOnBattlefield(2, "Centaur Courser") // 3/3
        .withCardOnBattlefield(2, "Force of Nature") // 5/5
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast through the server-offered action for [variant] (null = unkicked) until the trigger asks for a target. */
    private fun TestGame.cast(variant: String?) {
        val cardId = findCardsInHand(1, "Vodalian Mindsinger").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = if (variant == null) {
            actions.single { it.actionType == "CastSpell" }
        } else {
            actions.single { it.actionType == "CastWithKicker" && it.description.endsWith("($variant)") }
        }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    /** Answer the enters trigger's target prompt with [steal], checking the legal set first. */
    private fun TestGame.steal(steal: EntityId, illegal: List<EntityId>) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].orEmpty()
        legal shouldContain steal
        illegal.forEach { legal shouldNotContain it }
        selectTargets(listOf(steal)).error shouldBe null
        resolveStack()
        state.stack.isEmpty() shouldBe true
    }

    private fun TestGame.mindsingerCounters(): Int =
        state.getEntity(findPermanent("Vodalian Mindsinger")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("unkicked: a 2/2 can only take a creature with power 1 or less") {
            val game = board()
            val lions = game.findPermanent("Savannah Lions")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.cast(null)

            game.mindsingerCounters() shouldBe 0
            game.steal(lions, illegal = listOf(bears))

            withClue("control of the Lions moves to the Mindsinger's controller") {
                game.state.projectedState.getController(lions) shouldBe game.player1Id
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }
        }

        test("kicked with {1}{G} only: two counters, so a 4/4 can take a 3/3 but not a 5/5") {
            val game = board()
            val courser = game.findPermanent("Centaur Courser")!!
            val force = game.findPermanent("Force of Nature")!!
            game.cast("Kicked {1}{G}")

            game.mindsingerCounters() shouldBe 2
            game.steal(courser, illegal = listOf(force))
            game.state.projectedState.getController(courser) shouldBe game.player1Id
        }

        test("kicked with {1}{R} only: two counters") {
            val game = board()
            game.cast("Kicked {1}{R}")

            game.mindsingerCounters() shouldBe 2
            game.state.projectedState.getPower(game.findPermanent("Vodalian Mindsinger")!!) shouldBe 4
        }

        test("kicked twice: four counters, so a 6/6 can take a 5/5") {
            val game = board()
            val force = game.findPermanent("Force of Nature")!!
            game.cast("Kicked {1}{R} + {1}{G}")

            game.mindsingerCounters() shouldBe 4
            game.steal(force, illegal = emptyList())
            game.state.projectedState.getController(force) shouldBe game.player1Id
        }
    }
}
