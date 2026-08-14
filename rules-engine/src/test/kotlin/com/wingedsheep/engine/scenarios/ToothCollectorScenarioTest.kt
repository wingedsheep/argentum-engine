package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tooth Collector (DDQ #64) — ETB -1/-1, plus a Delirium-gated -1/-1 at the beginning of each
 * opponent's upkeep.
 */
class ToothCollectorScenarioTest : ScenarioTestBase() {
    init {
        test("ETB gives opponent creature -1/-1 until end of turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Tooth Collector")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Tooth Collector", bears).error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.selectTargets(listOf(bears))
            }
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.findPermanent("Tooth Collector") shouldNotBe null
            game.state.projectedState.getPower(bears) shouldBe 1
            game.state.projectedState.getToughness(bears) shouldBe 1
        }

        test("delirium shrinks a creature at the beginning of the opponent's upkeep") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Tooth Collector")
                .withCardOnBattlefield(2, "Grizzly Bears")
                // Four card types in P1's graveyard: creature, instant, sorcery, land.
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInGraveyard(1, "Shock")
                .withCardInGraveyard(1, "Demonic Counsel")
                .withCardInGraveyard(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id

            if (game.state.pendingDecision is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears))
            }
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("delirium is satisfied, so the upkeep trigger applies -1/-1") {
                game.state.projectedState.getPower(bears) shouldBe 1
                game.state.projectedState.getToughness(bears) shouldBe 1
            }
        }

        test("without delirium the upkeep trigger does nothing") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Tooth Collector")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Glory Seeker")
                .withCardInGraveyard(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("two card types is short of delirium, so the Bears stay 2/2") {
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }
    }
}
