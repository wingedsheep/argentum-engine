package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Folk Hero (CLB #650) — Commander creatures you own have "Whenever you cast a spell that shares a
 * creature type with this creature, draw a card. This ability triggers only once each turn."
 *
 * A Grizzly Bears tagged as player 1's commander carries the granted trigger: a Bear spell draws
 * (once per turn), a Changeling spell shares every creature type even on the stack (CR 702.73a),
 * and a Giant spell doesn't draw.
 */
class FolkHeroScenarioTest : ScenarioTestBase() {

    private fun game(vararg hand: String, land: String) = scenario()
        .withPlayers("You", "Opponent")
        .withCardOnBattlefield(1, "Folk Hero")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withLandsOnBattlefield(1, land, 8)
        .apply { hand.forEach { withCardInHand(1, it) } }
        .apply { repeat(5) { withCardInLibrary(1, "Forest") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { g ->
            val commander = g.findPermanent("Grizzly Bears")!!
            g.state = g.state.updateEntity(commander) { it.with(CommanderComponent(ownerId = g.player1Id)) }
        }

    private fun TestGame.castAndResolve(name: String) {
        castSpell(1, name).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        test("a spell sharing a creature type draws a card, but only once each turn") {
            val g = game("Grizzly Bears", "Grizzly Bears", land = "Forest")
            val library = g.librarySize(1)

            g.castAndResolve("Grizzly Bears")
            withClue("first Bear spell draws") { g.librarySize(1) shouldBe library - 1 }

            g.castAndResolve("Grizzly Bears")
            withClue("second Bear spell the same turn doesn't draw") { g.librarySize(1) shouldBe library - 1 }
        }

        test("a Changeling creature spell shares a creature type and draws") {
            val g = game("Avian Changeling", land = "Plains")
            val library = g.librarySize(1)

            g.castAndResolve("Avian Changeling")
            g.librarySize(1) shouldBe library - 1
        }

        test("a creature spell sharing no creature type doesn't draw") {
            val g = game("Hill Giant", land = "Mountain")
            val library = g.librarySize(1)

            g.castAndResolve("Hill Giant")
            g.librarySize(1) shouldBe library
        }
    }
}
