package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Expanding Ooze (MH3 #184) — "{B}{G}: Adapt 1." and "Whenever this creature attacks, put a +1/+1
 * counter on target modified creature you control."
 */
class ExpandingOozeScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.giveCounter(id: EntityId) {
        state = state.updateEntity(id) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        }
    }

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Expanding Ooze")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.adapt() {
        val ooze = findPermanent("Expanding Ooze")!!
        val ability = cardRegistry.requireCard("Expanding Ooze").script.activatedAbilities[0].id
        execute(ActivateAbility(playerId = player1Id, sourceId = ooze, abilityId = ability)).error shouldBe null
        var guard = 0
        while (guard++ < 10) {
            when (val d = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("unexpected decision $d")
            }
        }
    }

    init {
        test("adapt 1 adds a counter only when the Ooze has none") {
            val game = game()
            val ooze = game.findPermanent("Expanding Ooze")!!
            game.adapt()
            game.plusOnes(ooze) shouldBe 1
            game.adapt()
            withClue("second adapt does nothing") { game.plusOnes(ooze) shouldBe 1 }
            game.state.projectedState.getPower(ooze) shouldBe 4
        }

        test("attack trigger targets only modified creatures you control") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val ooze = game.findPermanent("Expanding Ooze")!!
            val courser = game.findPermanent("Centaur Courser")!!
            game.giveCounter(bears)
            game.giveCounter(ooze)
            game.giveCounter(courser)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Expanding Ooze" to 2)).error shouldBe null

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("unmodified Hill Giant and the opponent's Courser are not legal") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, ooze)
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.plusOnes(bears) shouldBe 2
            game.plusOnes(ooze) shouldBe 1
        }

        test("with no modified creature the trigger has no target and does nothing") {
            val game = game()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Expanding Ooze" to 2)).error shouldBe null
            game.getPendingDecision() shouldBe null
            game.state.stack.shouldBeEmpty()
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
        }
    }
}
