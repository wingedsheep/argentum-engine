package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Adventuring Gear (ZEN #195) — {1} Artifact — Equipment
 * "Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.
 *  Equip {1}"
 *
 * Exercises the landfall trigger firing the one-shot [com.wingedsheep.sdk.dsl.Effects.ModifyStats]
 * on [com.wingedsheep.sdk.scripting.targets.EffectTarget.EquippedCreature].
 */
class AdventuringGearScenarioTest : ScenarioTestBase() {

    init {
        context("Adventuring Gear — landfall pumps the equipped creature") {

            test("playing a land gives the equipped creature +2/+2 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardAttachedTo(1, "Adventuring Gear", "Grizzly Bears")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.state.getBattlefield(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val forest = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                }

                // Base 2/2 before any land drop.
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2

                val played = game.execute(PlayLand(game.player1Id, forest))
                withClue("Playing the land should succeed: ${played.error}") { played.error shouldBe null }
                game.resolveStack() // resolve the landfall trigger

                withClue("equipped creature is now 4/4 until end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }
        }
    }
}
