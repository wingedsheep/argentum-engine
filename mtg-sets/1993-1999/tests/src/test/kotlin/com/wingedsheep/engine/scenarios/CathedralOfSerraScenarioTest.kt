package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cathedral of Serra (LEG) — Land: "White legendary creatures you control have 'bands with other
 * legendary creatures.'" (CR 702.22b)
 */
class CathedralOfSerraScenarioTest : ScenarioTestBase() {

    private val bandsWithOtherLegendary = KeywordAbility.bandsWithOther(Supertype.LEGENDARY).projectedKeyword

    private fun TestGame.declareBand(vararg names: String): com.wingedsheep.engine.core.ExecutionResult {
        val members = names.map { findPermanent(it)!! }
        return execute(
            DeclareAttackers(
                player1Id,
                members.associateWith { player2Id },
                bands = listOf(members.toSet()),
            )
        )
    }

    init {
        context("Cathedral of Serra") {
            test("grants bands with other legendary creatures only to white legendary creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathedral of Serra")
                    .withCardOnBattlefield(1, "Kasimir the Lone Wolf")
                    .withCardOnBattlefield(1, "Pavel Maliki")
                    .withCardOnBattlefield(2, "Hunding Gjornersen")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Kasimir the Lone Wolf")!!, bandsWithOtherLegendary) shouldBe true
                projected.hasKeyword(game.findPermanent("Pavel Maliki")!!, bandsWithOtherLegendary) shouldBe false
                projected.hasKeyword(game.findPermanent("Hunding Gjornersen")!!, bandsWithOtherLegendary) shouldBe false
            }

            test("a white legend bands with a non-white legend, though neither has banding") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathedral of Serra")
                    .withCardOnBattlefield(1, "Kasimir the Lone Wolf")
                    .withCardOnBattlefield(1, "Pavel Maliki")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareBand("Kasimir the Lone Wolf", "Pavel Maliki").error shouldBe null
            }

            test("a non-legendary creature can't join the band") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathedral of Serra")
                    .withCardOnBattlefield(1, "Kasimir the Lone Wolf")
                    .withCardOnBattlefield(1, "Pavel Maliki")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareBand("Kasimir the Lone Wolf", "Pavel Maliki", "Grizzly Bears").error shouldNotBe null
            }

            test("non-white legends get nothing, so they can't band") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathedral of Serra")
                    .withCardOnBattlefield(1, "Pavel Maliki")
                    .withCardOnBattlefield(1, "Lady Orca")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareBand("Pavel Maliki", "Lady Orca").error shouldNotBe null
            }

            test("blocking with two legends, one of them white, hands the attacker's damage to you (CR 702.22j)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Cathedral of Serra")
                    .withCardOnBattlefield(2, "Kasimir the Lone Wolf")
                    .withCardOnBattlefield(2, "Pavel Maliki")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Kasimir the Lone Wolf" to listOf("Hill Giant"),
                        "Pavel Maliki" to listOf("Hill Giant"),
                    )
                ).error shouldBe null

                var passes = 0
                while (!game.hasPendingDecision() && passes++ < 20) game.passPriority()
                val decision = game.getPendingDecision().shouldBeInstanceOf<CombatResolutionDecision>()
                val giant = game.findPermanent("Hill Giant")!!
                val edges = decision.edges.filter {
                    it.sourceId == giant && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
                }
                edges.shouldNotBeEmpty()
                edges.forEach { it.editableBy shouldBe game.player2Id }
            }
        }
    }
}
