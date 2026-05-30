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
 * Scenario tests for Vodalian Serpent with kicker.
 *
 * Card reference:
 * - Vodalian Serpent ({3}{U}): Creature — Serpent 2/2
 *   Kicker {2}
 *   This creature can't attack unless defending player controls an Island.
 *   If this creature was kicked, it enters with four +1/+1 counters on it.
 */
class VodalianSerpentScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.getCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Vodalian Serpent kicker") {

            test("unkicked enters with no counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vodalian Serpent")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Vodalian Serpent")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val serpentId = game.findPermanent("Vodalian Serpent")!!
                withClue("Unkicked Vodalian Serpent should have no counters") {
                    game.getCounters(serpentId) shouldBe 0
                }
            }

            test("kicked enters with four +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Vodalian Serpent")
                    .withLandsOnBattlefield(1, "Island", 6) // {3}{U} + {2} kicker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Vodalian Serpent"
                }

                val castResult = game.execute(CastSpell(playerId, cardId, wasKicked = true))
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val serpentId = game.findPermanent("Vodalian Serpent")!!
                withClue("Kicked Vodalian Serpent should have four +1/+1 counters") {
                    game.getCounters(serpentId) shouldBe 4
                }
            }
        }

        context("Vodalian Serpent attack restriction") {

            test("cannot attack when defending player controls no Island") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vodalian Serpent")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Vodalian Serpent" to 2))
                withClue("Should not be able to attack player without an Island") {
                    (result.error != null) shouldBe true
                }
            }

            test("can attack when defending player controls an Island") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vodalian Serpent")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Vodalian Serpent" to 2))
                withClue("Should be able to attack player controlling an Island: ${result.error}") {
                    result.error shouldBe null
                }
            }
        }
    }
}
