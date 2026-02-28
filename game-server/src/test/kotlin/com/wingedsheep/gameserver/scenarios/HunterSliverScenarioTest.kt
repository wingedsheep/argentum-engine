package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Hunter Sliver's GrantTriggeredAbilityToCreatureGroup.
 *
 * Card reference:
 * - Hunter Sliver ({1}{R}): 1/1 Creature — Sliver
 *   All Sliver creatures have provoke.
 *
 * Tests verify that:
 * 1. Another Sliver gains provoke when Hunter Sliver is on the battlefield
 * 2. Hunter Sliver itself has provoke (it's also a Sliver)
 * 3. Non-Sliver creatures don't gain provoke
 */
class HunterSliverScenarioTest : ScenarioTestBase() {

    init {
        context("Hunter Sliver - GrantTriggeredAbilityToCreatureGroup") {

            test("another Sliver gains provoke from Hunter Sliver") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hunter Sliver")
                    .withCardOnBattlefield(1, "Blade Sliver") // 2/2 Sliver — gains provoke
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true) // 3/3 tapped
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify Hill Giant starts tapped
                val hillGiantId = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant should start tapped") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe true
                }

                // Move to combat and declare Blade Sliver as attacker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Blade Sliver" to 2))

                // Wait for the provoke trigger target selection
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have a pending target selection for provoke on Blade Sliver") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Hill Giant as provoke target
                game.selectTargets(listOf(hillGiantId))

                // Resolve the provoke effect
                game.resolveStack()

                // Verify Hill Giant is now untapped
                withClue("Hill Giant should be untapped after provoke") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe false
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Hill Giant must block Blade Sliver
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should fail when provoke forces blocking") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must block"
                }

                // Blocking should succeed
                val blockResult = game.declareBlockers(mapOf("Hill Giant" to listOf("Blade Sliver")))
                withClue("Blocking Blade Sliver should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("Hunter Sliver itself has provoke (it is a Sliver)") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hunter Sliver")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hunter Sliver" to 2))

                // Wait for the provoke trigger target selection
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Hunter Sliver should have provoke trigger on attack") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Hill Giant as provoke target
                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(hillGiantId))
                game.resolveStack()

                // Hill Giant should be untapped
                withClue("Hill Giant should be untapped after provoke") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe false
                }
            }

            test("non-Sliver creatures do not gain provoke from Hunter Sliver") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hunter Sliver")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 Bear — not a Sliver
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2))

                // Pass priority — no provoke trigger should fire for non-Sliver
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // No forced blocking — declaring no blockers should succeed
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should succeed since Grizzly Bears has no provoke: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }
        }
    }
}
