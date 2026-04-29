package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Shardmage's Rescue (DSK #29).
 *
 * Card text:
 * - Flash
 * - Enchant creature you control
 * - As long as this Aura entered this turn, enchanted creature has hexproof.
 * - Enchanted creature gets +1/+1.
 */
class ShardmagesRescueScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Shardmage's Rescue") {

            test("hexproof prevents removal spell on the same turn Rescue enters") {
                // It is Player 2's turn. P1 controls Grizzly Bears.
                // P2 casts Smother targeting Bears. P1 responds with Rescue (flash).
                // Rescue resolves first, immediately granting Bears hexproof via static ability.
                // Smother then fizzles because its target is now illegal.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shardmage's Rescue")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Smother")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // P2 casts Smother. Stack: [Smother]. P2 (active) retains priority.
                val smotherResult = game.castSpell(2, "Smother", bearsId)
                withClue("Smother cast should succeed") {
                    smotherResult.error shouldBe null
                }

                // P2 passes priority to P1.
                game.passPriority()

                // P1 responds with Shardmage's Rescue (flash). Stack: [Smother, Rescue].
                val rescueResult = game.castSpell(1, "Shardmage's Rescue", bearsId)
                withClue("Shardmage's Rescue should be castable at instant speed (flash)") {
                    rescueResult.error shouldBe null
                }

                // Both players pass: Rescue resolves first → Bears gains hexproof.
                // Both players pass again: Smother resolves but its target is illegal → fizzles.
                game.resolveStack()

                withClue("Shardmage's Rescue should be on the battlefield") {
                    game.isOnBattlefield("Shardmage's Rescue") shouldBe true
                }
                withClue("Grizzly Bears should survive (Smother fizzled due to hexproof)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                // Verify hexproof is actually present on the turn Rescue entered
                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should have hexproof this turn") {
                    projected.hasKeyword(bearsId, Keyword.HEXPROOF) shouldBe true
                }
            }

            test("enchanted creature always gets +1/+1") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shardmage's Rescue")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Shardmage's Rescue", bearsId)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears (2/2) should be 3/3 with Rescue's +1/+1 bonus") {
                    projected.getPower(bearsId) shouldBe 3
                    projected.getToughness(bearsId) shouldBe 3
                }
            }

            test("hexproof does not apply when Rescue did not enter this turn") {
                // Simulate Rescue already being on the battlefield from a previous turn:
                // withCardOnBattlefield does NOT add EnteredThisTurnComponent, so the
                // conditional hexproof (SourceEnteredThisTurn) evaluates to false.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Shardmage's Rescue")
                    .withCardInHand(2, "Smother")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val rescueId = game.findPermanent("Shardmage's Rescue")!!

                // Wire the Aura attachment so the static ability's AffectsFilter.AttachedPermanent
                // targets Grizzly Bears (as it would after normal casting).
                game.state = game.state.updateEntity(rescueId) { container ->
                    container.with(AttachedToComponent(bearsId))
                }

                // Rescue has no EnteredThisTurnComponent → hexproof condition is false.
                val projected = stateProjector.project(game.state)
                withClue("Bears should NOT have hexproof when Rescue did not enter this turn") {
                    projected.hasKeyword(bearsId, Keyword.HEXPROOF) shouldBe false
                }

                // P2 can now target and destroy Bears with Smother.
                val smotherResult = game.castSpell(2, "Smother", bearsId)
                withClue("Smother should be able to target Bears (no hexproof)") {
                    smotherResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears should be destroyed by Smother") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Grizzly Bears should be in P1's graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
