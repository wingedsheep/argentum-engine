package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Conqueror's Galleon // Conqueror's Foothold (XLN #234) — {4} Artifact — Vehicle, 2/10
 *
 *   When this Vehicle attacks, exile it at end of combat, then return it to the battlefield
 *   transformed under your control.
 *   Crew 4
 *
 * Pins the delay (still the Galleon through combat damage, Foothold only once combat ends) and
 * "under your control": a Galleon the attacker controls but doesn't own comes back under the
 * attacker, not its owner.
 */
class ConquerorsGalleonScenarioTest : ScenarioTestBase() {

    init {
        fun attackWithGalleon(galleonOwner: Int): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(galleonOwner, "Conqueror's Galleon", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val galleon = game.findPermanent("Conqueror's Galleon")!!
            if (galleonOwner != 1) {
                // Player 1 has stolen the opponent's Galleon.
                game.state = game.state.updateEntity(galleon) { it.with(ControllerComponent(game.player1Id)) }
            }
            val giants = game.findAllPermanents("Hill Giant")

            game.execute(CrewVehicle(game.player1Id, galleon, giants)).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Conqueror's Galleon" to 2)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            withClue("the Galleon stays a Vehicle through combat damage") {
                game.isOnBattlefield("Conqueror's Galleon") shouldBe true
                game.isOnBattlefield("Conqueror's Foothold") shouldBe false
            }
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()
            return game
        }

        test("at end of combat the attacking Galleon returns as Conqueror's Foothold") {
            val game = attackWithGalleon(galleonOwner = 1)

            game.isOnBattlefield("Conqueror's Galleon") shouldBe false
            val foothold = game.findPermanent("Conqueror's Foothold").shouldNotBeNull()
            game.state.projectedState.getController(foothold) shouldBe game.player1Id
            game.getLifeTotal(2) shouldBe 18
        }

        test("a stolen Galleon returns under the attacker's control, not its owner's") {
            val game = attackWithGalleon(galleonOwner = 2)

            val foothold = game.findPermanent("Conqueror's Foothold").shouldNotBeNull()
            game.state.projectedState.getController(foothold) shouldBe game.player1Id
        }
    }
}
