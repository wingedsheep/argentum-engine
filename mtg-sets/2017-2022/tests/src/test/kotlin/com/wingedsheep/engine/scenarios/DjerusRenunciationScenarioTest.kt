package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Djeru's Renunciation (HOU) — {1}{W} Instant. "Tap up to two target creatures." Cycling {W}.
 */
class DjerusRenunciationScenarioTest : ScenarioTestBase() {
    init {
        test("taps two target creatures") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Djeru's Renunciation")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.findCardsInHand(1, "Djeru's Renunciation").single()
            game.execute(
                CastSpell(
                    game.player1Id, spell,
                    listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
        }

        test("can be cast with a single target") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Djeru's Renunciation")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Djeru's Renunciation", bears).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
        }

        test("cycles for {W} and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Djeru's Renunciation").withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Djeru's Renunciation").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Djeru's Renunciation") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
