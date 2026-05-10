package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Demon Wall (FIN #97).
 *
 * Card reference:
 * - Demon Wall {1}{B} — Artifact Creature — Demon Wall 3/3
 *   Defender
 *   Menace
 *   As long as this creature has a counter on it, it can attack as though it didn't have defender.
 *   {5}{B}: Put two +1/+1 counters on this creature.
 */
class DemonWallScenarioTest : ScenarioTestBase() {

    init {
        context("Demon Wall - CanAttackDespiteDefender") {

            test("cannot attack when it has no counters (defender restriction applies)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Demon Wall")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Demon Wall" to 2))
                withClue("Demon Wall with no counters should not be able to attack") {
                    (result.error != null) shouldBe true
                }
            }

            test("can attack when it has a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Demon Wall")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val demonWallId = game.findPermanent("Demon Wall")!!
                game.state = game.state.updateEntity(demonWallId) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Demon Wall" to 2))
                withClue("Demon Wall with a counter should be able to attack") {
                    result.error shouldBe null
                }
            }
        }

        context("Demon Wall - activated ability") {

            test("{5}{B} puts two +1/+1 counters on the creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Demon Wall")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val demonWallId = game.findPermanent("Demon Wall")!!
                val cardDef = cardRegistry.getCard("Demon Wall")!!
                val pumpAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = demonWallId,
                        abilityId = pumpAbility.id
                    )
                )
                withClue("Ability activation should succeed") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val counters = game.state.getEntity(demonWallId)?.get<CountersComponent>()
                withClue("Demon Wall should have 2 +1/+1 counters after activation") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("two +1/+1 counters allow attacking despite defender") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Demon Wall")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val demonWallId = game.findPermanent("Demon Wall")!!
                val cardDef = cardRegistry.getCard("Demon Wall")!!
                val pumpAbility = cardDef.script.activatedAbilities[0]

                game.execute(ActivateAbility(game.player1Id, demonWallId, pumpAbility.id))
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val attackResult = game.declareAttackers(mapOf("Demon Wall" to 2))
                withClue("Demon Wall with counters from ability should be able to attack") {
                    attackResult.error shouldBe null
                }
            }
        }
    }
}
