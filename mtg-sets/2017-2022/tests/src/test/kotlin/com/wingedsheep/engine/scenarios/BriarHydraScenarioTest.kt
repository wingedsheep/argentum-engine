package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Briar Hydra (DMU #286) — {5}{G} 6/6 Plant Hydra with trample.
 *
 * "Domain — Whenever this creature deals combat damage to a player, put X +1/+1 counters on
 * target creature you control, where X is the number of basic land types among lands you control."
 *
 * Domain reads *projected* land types: a land that has lost all land types (Ultima, Origin of
 * Oblivion's blight) still is a land but contributes no type, so its printed subtype must not count.
 */
class BriarHydraScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Briar Hydra") {

            test("combat damage to a player puts domain-many +1/+1 counters on the target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Briar Hydra", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Contaminated Aquifer") // Land — Island Swamp
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Briar Hydra" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                withClue("the combat damage trigger asks for a creature you control") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the Hydra's 6 trample damage reached the opponent") {
                    game.getLifeTotal(2) shouldBe 14
                }
                withClue("Forest, Island, Swamp → three +1/+1 counters on the Bears") {
                    plusOnes(game, bears) shouldBe 3
                }
            }

            test("a land that lost all land types adds nothing to X") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Briar Hydra", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ultima, Origin of Oblivion", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hydra = game.findPermanent("Briar Hydra")!!
                val forest = game.findPermanent("Forest")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Briar Hydra" to 2, "Ultima, Origin of Oblivion" to 2)
                ).error shouldBe null

                // Ultima's attack trigger blights the Forest: it stays a land with no land types.
                game.selectTargets(listOf(forest))
                game.resolveStack()
                withClue("the blighted Forest has no land subtypes") {
                    game.state.projectedState.getSubtypes(forest) shouldBe emptySet()
                }

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.selectTargets(listOf(hydra)).error shouldBe null
                game.resolveStack()

                withClue("only the Island counts → one +1/+1 counter") {
                    plusOnes(game, hydra) shouldBe 1
                }
            }
        }
    }
}
