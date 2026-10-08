package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Earthshape — "Earthbend 3. Then each creature you control with power less than or equal to that
 * land's power gains hexproof and indestructible until end of turn. You gain hexproof until end
 * of turn."
 *
 * Pins that "that land's power" is read after the earthbend (0/0 base + counters), that the
 * comparison is ≤ (a 3-power creature qualifies, a 6-power one doesn't), that the land itself is
 * protected, that only creatures you control are covered, and the player hexproof.
 */
class EarthshapeScenarioTest : ScenarioTestBase() {

    init {
        test("protects your creatures with power at most the earthbent land's power, and you") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Earthshape")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Grizzly Bears")   // 2/2 — covered
                .withCardOnBattlefield(1, "Hill Giant")      // 3/3 — covered (equal)
                .withCardOnBattlefield(1, "Craw Wurm")       // 6/4 — not covered
                .withCardOnBattlefield(2, "Glory Seeker")    // opponent's 2/2 — not covered
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forest = game.findPermanent("Forest")!!
            val result = game.castSpell(1, "Earthshape", forest)
            withClue("cast succeeds: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the Forest is a 3/3 land creature") {
                projected.isCreature(forest) shouldBe true
                projected.getPower(forest) shouldBe 3
            }

            fun protected(name: String): Boolean {
                val id = game.findPermanent(name)!!
                return projected.hasKeyword(id, Keyword.HEXPROOF) && projected.hasKeyword(id, Keyword.INDESTRUCTIBLE)
            }
            withClue("the earthbent land itself is protected") {
                projected.hasKeyword(forest, Keyword.HEXPROOF) shouldBe true
                projected.hasKeyword(forest, Keyword.INDESTRUCTIBLE) shouldBe true
            }
            withClue("a 2-power and a 3-power creature you control are protected") {
                protected("Grizzly Bears") shouldBe true
                protected("Hill Giant") shouldBe true
            }
            withClue("a 6-power creature is not") {
                protected("Craw Wurm") shouldBe false
            }
            withClue("an opponent's creature is not") {
                protected("Glory Seeker") shouldBe false
            }
            withClue("you gain hexproof") {
                game.state.getEntity(game.player1Id)?.get<PlayerHexproofComponent>() shouldNotBe null
            }
        }
    }
}
