package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gruesome Deformity (ISD #103) — {B} Aura.
 *
 *   Enchant creature
 *   Enchanted creature has intimidate.
 */
class GruesomeDeformityScenarioTest : ScenarioTestBase() {

    init {
        test("the enchanted creature gains intimidate and can only be blocked by a creature sharing a color") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gruesome Deformity")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castSpell(1, "Gruesome Deformity", bears)
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("white Savannah Lions shares no color with the green Bears") {
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
            withClue("green Centaur Courser shares a color and may block") {
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }
    }
}
