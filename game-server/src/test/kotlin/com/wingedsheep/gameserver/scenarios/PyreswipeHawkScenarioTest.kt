package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pyreswipe Hawk (BLC #26).
 *
 * Card reference:
 * - Pyreswipe Hawk ({3}{R}{R}): Creature — Elemental Bird, 4/4
 *   Flying, haste
 *   Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the
 *     greatest mana value among artifacts you control.
 *   Whenever you expend 6, gain control of up to one target artifact for as long as
 *     you control this creature.
 *
 * Regression test for the engine fix: `Duration.WhileSourceOnBattlefield` floating
 * effects must be skipped by `StateProjector` as soon as the source leaves the
 * battlefield, not only at end-of-turn cleanup. Previously, killing the hawk mid-turn
 * left the stolen artifact under the hawk's controller for the rest of the turn.
 */
class PyreswipeHawkScenarioTest : ScenarioTestBase() {

    init {
        test("Expend 6 grants control of target artifact and reverts when the hawk leaves the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Pyreswipe Hawk")
                .withCardOnBattlefield(2, "Fellwar Stone")
                .withCardInHand(1, "Volcanic Dragon") // {4}{R}{R} = 6 mana → crosses Expend 6
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val hawkId = game.findPermanent("Pyreswipe Hawk")!!
            val stoneId = game.findPermanent("Fellwar Stone")!!

            withClue("Fellwar Stone is initially controlled by Player 2") {
                game.state.projectedState.getController(stoneId) shouldBe game.player2Id
            }

            // Cast Volcanic Dragon ({4}{R}{R}) → 6 mana spent → Expend 6 triggers.
            game.castSpell(1, "Volcanic Dragon")

            // Trigger pauses for "up to one target artifact" selection.
            withClue("Pyreswipe Hawk's Expend 6 trigger should be asking for a target") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectTargets(listOf(stoneId))
            game.resolveStack()

            withClue("After the trigger resolves, Player 1 should control Fellwar Stone") {
                game.state.projectedState.getController(stoneId) shouldBe game.player1Id
            }

            // Sanity: the floating ChangeController effect is in state with the hawk as source.
            withClue("ChangeController floating effect exists with Pyreswipe Hawk as source") {
                game.state.floatingEffects.any { floating ->
                    floating.effect.modification is SerializableModification.ChangeController &&
                        floating.sourceId == hawkId &&
                        stoneId in floating.effect.affectedEntities
                } shouldBe true
            }

            // Move Pyreswipe Hawk to its owner's graveyard — simulates the hawk being
            // destroyed mid-turn. The floating effect is intentionally NOT removed here;
            // we are asserting that the projector skips it because the source is no
            // longer on the battlefield (the fix in StateProjector.collectFloatingEffects).
            game.state = game.state.moveToZone(
                hawkId,
                ZoneKey(game.player1Id, Zone.BATTLEFIELD),
                ZoneKey(game.player1Id, Zone.GRAVEYARD),
            )

            withClue("Once the hawk has left the battlefield, control of Fellwar Stone reverts to Player 2") {
                game.state.projectedState.getController(stoneId) shouldBe game.player2Id
            }

            withClue("The ChangeController floating effect is still in state (not yet cleaned up)") {
                game.state.floatingEffects.any { floating ->
                    floating.effect.modification is SerializableModification.ChangeController &&
                        floating.sourceId == hawkId
                } shouldBe true
            }
        }
    }
}
