package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
 * Norn's Inquisitor — ETB incubate 2; "Whenever a permanent you control transforms into a
 * Phyrexian, put a +1/+1 counter on it."
 */
class NornsInquisitorScenarioTest : ScenarioTestBase() {

    private fun TestGame.transform(name: String) {
        val permanent = findPermanent(name)!!
        val abilityId = cardRegistry.getCard(name)!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = permanent, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Norn's Inquisitor") {

            test("its Incubator transforming into a Phyrexian gets an extra +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Norn's Inquisitor")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Norn's Inquisitor")
                game.resolveStack()

                val incubator = game.findPermanent("Incubator")!!
                withClue("incubate 2") { game.plusOneCounters(incubator) shouldBe 2 }

                game.transform("Incubator")
                withClue("the transformed Phyrexian token gets a third counter") {
                    game.findPermanent("Phyrexian") shouldBe incubator
                    game.plusOneCounters(incubator) shouldBe 3
                    game.state.projectedState.getPower(incubator) shouldBe 3
                }
            }

            test("a permanent that transforms into a non-Phyrexian gets nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Norn's Inquisitor")
                    .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Smoldering Werewolf")!!
                game.transform("Smoldering Werewolf")

                game.findPermanent("Erupting Dreadwolf") shouldBe wolf
                game.plusOneCounters(wolf) shouldBe 0
            }
        }
    }
}
