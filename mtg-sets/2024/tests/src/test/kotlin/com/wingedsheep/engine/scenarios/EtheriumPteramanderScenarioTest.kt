package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Etherium Pteramander (MH3) — "{6}{B}: Adapt 4. This ability costs {1} less to activate for each
 * other artifact you control." The reduction counts only *other* artifacts (the Pteramander is
 * itself an artifact), and Adapt does nothing once the creature already has +1/+1 counters.
 */
class EtheriumPteramanderScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.settle() {
        var guard = 0
        while (guard++ < 20) {
            when (val d = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("unexpected decision $d")
            }
        }
    }

    private fun game(swamps: Int, otherArtifacts: Int): TestGame {
        var b = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Etherium Pteramander")
            .withLandsOnBattlefield(1, "Swamp", swamps)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(otherArtifacts) { b = b.withCardOnBattlefield(1, "Millstone") }
        return b.build()
    }

    private fun TestGame.activate(): ExecutionResult {
        val ptera = findPermanent("Etherium Pteramander")!!
        val ability = cardRegistry.requireCard("Etherium Pteramander").script.activatedAbilities[0].id
        return execute(ActivateAbility(playerId = player1Id, sourceId = ptera, abilityId = ability))
    }

    init {
        test("two other artifacts reduce {6}{B} to {4}{B}; adapt puts four counters") {
            val game = game(swamps = 5, otherArtifacts = 2)
            val ptera = game.findPermanent("Etherium Pteramander")!!
            game.activate().error shouldBe null
            game.settle()
            withClue("adapt 4 on a creature with no counters") { game.plusOnes(ptera) shouldBe 4 }
            game.state.projectedState.getPower(ptera) shouldBe 5
            game.state.projectedState.getToughness(ptera) shouldBe 5
        }

        test("the Pteramander itself does not count toward the reduction") {
            val game = game(swamps = 6, otherArtifacts = 0)
            withClue("{6}{B} with no other artifacts needs seven mana") {
                game.activate().error shouldNotBe null
            }
        }

        test("adapt does nothing if it already has +1/+1 counters") {
            val game = game(swamps = 2, otherArtifacts = 6)
            val ptera = game.findPermanent("Etherium Pteramander")!!
            game.activate().error shouldBe null
            game.settle()
            game.plusOnes(ptera) shouldBe 4
            game.activate().error shouldBe null
            game.settle()
            withClue("second adapt finds counters already there") { game.plusOnes(ptera) shouldBe 4 }
        }
    }
}
