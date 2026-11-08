package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unlicensed Disintegration (KLD #187) — "Destroy target creature. If you control an artifact,
 * Unlicensed Disintegration deals 3 damage to that creature's controller."
 *
 * The artifact check happens after the destroy (2020-08-07 ruling), and "that creature's controller"
 * is its last-known controller once it has left the battlefield (CR 608.2h).
 */
class UnlicensedDisintegrationScenarioTest : ScenarioTestBase() {
    init {
        fun board(withArtifact: Boolean, targetCard: String = "Grizzly Bears") = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Unlicensed Disintegration")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, targetCard)
            .apply { if (withArtifact) withCardOnBattlefield(1, "Bonesplitter") }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.disintegrate(targetName: String) {
            val target = findPermanent(targetName)!!
            val result = castSpell(1, "Unlicensed Disintegration", targetId = target)
            withClue("cast: ${result.error}") { result.error shouldBe null }
            resolveStack()
        }

        test("destroys the creature and deals 3 to its controller when you control an artifact") {
            val game = board(withArtifact = true)
            game.disintegrate("Grizzly Bears")

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
        }

        test("without an artifact it only destroys") {
            val game = board(withArtifact = false)
            game.disintegrate("Grizzly Bears")

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("the artifact check is made after the destroy: killing your only artifact deals no damage") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Unlicensed Disintegration")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.disintegrate("Ornithopter")

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            withClue("no artifact remains once Ornithopter is destroyed") {
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("an indestructible target survives but its controller is still dealt 3") {
            val game = board(withArtifact = true, targetCard = "Darksteel Myr")
            game.disintegrate("Darksteel Myr")

            withClue("Darksteel Myr is still on the battlefield") {
                (game.findPermanent("Darksteel Myr") != null) shouldBe true
            }
            game.getLifeTotal(2) shouldBe 17
        }

        test("a creature you control but don't own deals the damage to you, its controller at death") {
            val game = board(withArtifact = true)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { it.with(ControllerComponent(game.player1Id)) }
            game.disintegrate("Grizzly Bears")

            withClue("the Bears go to their owner's graveyard") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("last-known controller (P1) is dealt the damage, not the owner") {
                game.getLifeTotal(1) shouldBe 17
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
