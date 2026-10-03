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
import io.kotest.matchers.shouldBe

/**
 * Evolution Witness (MH3) — "{1}{G}: Adapt 2." and "Whenever one or more +1/+1 counters are put on
 * this creature, return target permanent card from your graveyard to your hand."
 */
class EvolutionWitnessScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Resolves everything, answering any graveyard target prompt with [pick]; returns prompts seen. */
    private fun TestGame.settle(pick: () -> EntityId?): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 20) {
            when (val d = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                is ChooseTargetsDecision -> {
                    prompts++
                    selectTargets(listOfNotNull(pick())).error shouldBe null
                }
                null -> if (state.stack.isNotEmpty()) resolveStack() else return prompts
                else -> error("unexpected decision $d")
            }
        }
        return prompts
    }

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Evolution Witness")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Forest")
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardsInHand(1, "Battlegrowth", 1)
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.adapt(pick: () -> EntityId?): Int {
        val witness = findPermanent("Evolution Witness")!!
        val ability = cardRegistry.requireCard("Evolution Witness").script.activatedAbilities[0].id
        execute(ActivateAbility(playerId = player1Id, sourceId = witness, abilityId = ability)).error shouldBe null
        return settle(pick)
    }

    init {
        test("adapting returns a permanent card from the graveyard") {
            val game = game()
            val witness = game.findPermanent("Evolution Witness")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val prompts = game.adapt { bears }
            prompts shouldBe 1
            game.plusOnes(witness) shouldBe 2
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
        }

        test("a land card is a legal target but an instant is not") {
            val game = game()
            val forest = game.findCardsInGraveyard(1, "Forest").single()
            game.adapt { forest }
            game.isInHand(1, "Forest") shouldBe true

            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val game2 = game()
            val witness = game2.findPermanent("Evolution Witness")!!
            val ability = cardRegistry.requireCard("Evolution Witness").script.activatedAbilities[0].id
            game2.execute(ActivateAbility(playerId = game2.player1Id, sourceId = witness, abilityId = ability))
            var guard = 0
            while (guard++ < 10) {
                when (val d = game2.getPendingDecision()) {
                    is SelectManaSourcesDecision -> game2.submitManaSourcesAutoPay()
                    is ChooseTargetsDecision -> {
                        withClue("an instant card is not a permanent card") {
                            (game2.selectTargets(listOf(bolt)).error != null) shouldBe true
                        }
                        return@test
                    }
                    null -> if (game2.state.stack.isNotEmpty()) game2.resolveStack() else break
                    else -> error("unexpected decision $d")
                }
            }
            error("never prompted for a target")
        }

        test("adapting again with counters already on it does nothing and does not trigger") {
            val game = game()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.adapt { bears }
            val witness = game.findPermanent("Evolution Witness")!!
            val prompts = game.adapt { error("should not trigger") }
            prompts shouldBe 0
            game.plusOnes(witness) shouldBe 2
            game.isInGraveyard(1, "Forest") shouldBe true
        }

        test("any +1/+1 counter placement triggers it, not just adapt") {
            val game = game()
            val witness = game.findPermanent("Evolution Witness")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpell(1, "Battlegrowth", targetId = witness).error shouldBe null
            game.settle { bears } shouldBe 1
            game.plusOnes(witness) shouldBe 1
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }
    }
}
