package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.EnvoyOfTheAncestors
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Envoy of the Ancestors (MH3 #23): "Outlast {W}. Modified creatures you control have lifelink."
 */
class EnvoyOfTheAncestorsScenarioTest : ScenarioTestBase() {

    init {
        context("Envoy of the Ancestors") {

            test("only modified creatures you control have lifelink") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Envoy of the Ancestors")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                listOf(courser, giant).forEach { id ->
                    game.state = game.state.updateEntity(id) {
                        it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                    }
                }

                val projected = game.state.projectedState
                projected.hasKeyword(courser, Keyword.LIFELINK) shouldBe true
                projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                projected.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
                projected.hasKeyword(game.findPermanent("Envoy of the Ancestors")!!, Keyword.LIFELINK) shouldBe false
            }

            test("outlast puts a +1/+1 counter on the Envoy, giving it lifelink") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Envoy of the Ancestors")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val envoy = game.findPermanent("Envoy of the Ancestors")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = envoy,
                        abilityId = EnvoyOfTheAncestors.activatedAbilities.first().id
                    )
                ).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(envoy) shouldBe 3
                projected.getToughness(envoy) shouldBe 4
                projected.hasKeyword(envoy, Keyword.LIFELINK) shouldBe true
            }
        }
    }
}
