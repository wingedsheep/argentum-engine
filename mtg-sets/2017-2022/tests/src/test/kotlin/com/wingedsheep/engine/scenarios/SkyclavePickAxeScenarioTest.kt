package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Skyclave Pick-Axe (ZNR #204) — {G} Artifact — Equipment.
 *
 *   When this Equipment enters, attach it to target creature you control.
 *   Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.
 *   Equip {2}{G}
 */
class SkyclavePickAxeScenarioTest : ScenarioTestBase() {

    init {
        context("Skyclave Pick-Axe") {

            test("enters attached to the target, then landfall pumps the equipped creature until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skyclave Pick-Axe")
                    .withCardInHand(1, "Forest")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Skyclave Pick-Axe").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                val axe = game.findPermanent("Skyclave Pick-Axe")!!
                withClue("the ETB trigger attaches the Pick-Axe to the Bears") {
                    game.state.getEntity(axe)!!.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("the Pick-Axe has no static bonus") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }

                val forest = game.findCardsInHand(1, "Forest").single()
                game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                game.resolveStack()

                withClue("landfall: equipped creature gets +2/+2") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the bonus ends at end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }

            test("landfall with nothing equipped pumps no creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skyclave Pick-Axe")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findCardsInHand(1, "Forest").single()
                game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }
    }
}
