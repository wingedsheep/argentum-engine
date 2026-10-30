package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Erebos's Intervention (THB #94) — {X}{B} Instant, choose one:
 * target creature gets -X/-X until end of turn and you gain X life, or exile up to twice X
 * target cards from graveyards.
 *
 * Proves the graveyard mode's target cap is 2X (not X): with X = 1, two cards can be exiled and
 * three are rejected.
 */
class ErebossInterventionScenarioTest : ScenarioTestBase() {

    private val shrinkMode = 0
    private val exileMode = 1

    private fun graveyardGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Erebos's Intervention")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exile mode with X = 1 exiles two target cards from graveyards") {
            val game = graveyardGame()
            val spell = game.findCardsInHand(1, "Erebos's Intervention").single()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears")
            val targets = bears.take(2).map { ChosenTarget.Card(it, game.player2Id, Zone.GRAVEYARD) }

            game.execute(
                CastSpell(
                    game.player1Id, spell, targets,
                    xValue = 1,
                    chosenModes = listOf(exileMode),
                    modeTargetsOrdered = listOf(targets),
                )
            ).error shouldBe null
            game.resolveStack()

            game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
            game.state.getExile(game.player2Id).containsAll(bears.take(2)) shouldBe true
        }

        test("exile mode with X = 1 rejects three targets — the cap is twice X") {
            val game = graveyardGame()
            val spell = game.findCardsInHand(1, "Erebos's Intervention").single()
            val targets = game.findCardsInGraveyard(2, "Grizzly Bears")
                .map { ChosenTarget.Card(it, game.player2Id, Zone.GRAVEYARD) }

            game.execute(
                CastSpell(
                    game.player1Id, spell, targets,
                    xValue = 1,
                    chosenModes = listOf(exileMode),
                    modeTargetsOrdered = listOf(targets),
                )
            ).error shouldNotBe null
        }

        test("shrink mode gives the creature -X/-X and you gain X life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Erebos's Intervention")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Erebos's Intervention").single()
            val target = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
            val lifeBefore = game.getLifeTotal(1)
            game.execute(
                CastSpell(
                    game.player1Id, spell, target,
                    xValue = 2,
                    chosenModes = listOf(shrinkMode),
                    modeTargetsOrdered = listOf(target),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe lifeBefore + 2
            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.getPower(giant) shouldBe 1
            game.state.projectedState.getToughness(giant) shouldBe 1
        }
    }
}
