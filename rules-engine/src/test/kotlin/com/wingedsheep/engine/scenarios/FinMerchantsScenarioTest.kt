package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Final Fantasy "merchants" batch:
 *  - Item Shopkeep ({1}{R} 2/2): "Whenever you attack, target attacking equipped creature gains
 *    menace until end of turn."
 *  - Magitek Infantry ({W} 1/1 Artifact Creature): "+1/+0 as long as you control another artifact"
 *    (conditional static buff) plus a search-for-another-copy activated ability (stock searchLibrary,
 *    left to the snapshot net).
 */
class FinMerchantsScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {

        // -----------------------------------------------------------------------------------------
        // Item Shopkeep
        // -----------------------------------------------------------------------------------------

        test("Item Shopkeep grants menace to an attacking equipped creature when you attack") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Item Shopkeep")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Coral Sword", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            projector.hasProjectedKeyword(game.state, bears, Keyword.MENACE) shouldBe false

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Item Shopkeep" to 2, "Grizzly Bears" to 2)).error shouldBe null

            // The "whenever you attack" trigger targets the attacking equipped creature; the target
            // decision may pend immediately or after the stack starts resolving.
            if (game.state.pendingDecision == null) game.resolveStack()
            if (game.state.pendingDecision != null) game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("equipped attacker gains menace") {
                projector.hasProjectedKeyword(game.state, bears, Keyword.MENACE) shouldBe true
            }
        }

        // -----------------------------------------------------------------------------------------
        // Magitek Infantry
        // -----------------------------------------------------------------------------------------

        test("Magitek Infantry is 1/1 with no other artifact") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Magitek Infantry")
                .build()

            val magitek = game.findPermanent("Magitek Infantry")!!
            projector.getProjectedPower(game.state, magitek) shouldBe 1
            projector.getProjectedToughness(game.state, magitek) shouldBe 1
        }

        test("Magitek Infantry gets +1/+0 while you control another artifact") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Magitek Infantry")
                .withCardOnBattlefield(1, "Ornithopter")
                .build()

            val magitek = game.findPermanent("Magitek Infantry")!!
            withClue("another artifact (Ornithopter) makes Magitek 2/1") {
                projector.getProjectedPower(game.state, magitek) shouldBe 2
                projector.getProjectedToughness(game.state, magitek) shouldBe 1
            }
        }
    }
}
