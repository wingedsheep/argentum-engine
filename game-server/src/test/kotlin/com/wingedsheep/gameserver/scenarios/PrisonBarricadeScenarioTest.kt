package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Prison Barricade with kicker.
 *
 * Card reference:
 * - Prison Barricade ({1}{W}): Creature — Wall 1/3
 *   Defender
 *   Kicker {1}{W}
 *   If this creature was kicked, it enters with a +1/+1 counter on it and with
 *   "This creature can attack as though it didn't have defender."
 *
 * The kicked +1/+1 counter is the persistent marker; the defender-bypass is a
 * CanAttackDespiteDefender static gated on the presence of that counter
 * (Demon Wall pattern).
 */
class PrisonBarricadeScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.getCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Prison Barricade kicker") {

            test("unkicked enters with no counter and cannot attack (defender)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Prison Barricade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Prison Barricade")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val barricadeId = game.findPermanent("Prison Barricade")!!
                withClue("Unkicked Prison Barricade should have no counters") {
                    game.getCounters(barricadeId) shouldBe 0
                }
            }

            test("kicked enters with a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Prison Barricade")
                    .withLandsOnBattlefield(1, "Plains", 4) // {1}{W} + {1}{W} kicker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Prison Barricade"
                }

                val castResult = game.execute(CastSpell(playerId, cardId, wasKicked = true))
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val barricadeId = game.findPermanent("Prison Barricade")!!
                withClue("Kicked Prison Barricade should have a +1/+1 counter") {
                    game.getCounters(barricadeId) shouldBe 1
                }
            }
        }

        context("Prison Barricade defender bypass") {

            test("cannot attack with no counter (defender restriction applies)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prison Barricade")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Prison Barricade" to 2))
                withClue("Prison Barricade with no counter should not be able to attack") {
                    (result.error != null) shouldBe true
                }
            }

            test("can attack when it has a +1/+1 counter (kicked)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prison Barricade")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val barricadeId = game.findPermanent("Prison Barricade")!!
                game.state = game.state.updateEntity(barricadeId) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Prison Barricade" to 2))
                withClue("Kicked Prison Barricade should be able to attack despite defender") {
                    result.error shouldBe null
                }
            }
        }
    }
}
