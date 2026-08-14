package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tune Up — "Return target artifact card from your graveyard to the battlefield. If it's a Vehicle,
 * it becomes an artifact creature."
 *
 * The interesting part is the *order*: the conditional re-reads the chosen target after it has
 * already moved, so these cases prove the target still resolves once it's a battlefield permanent
 * and that the animation is permanent (no until-end-of-turn expiry) rather than crew's temporary one.
 * The non-Vehicle case proves the condition actually gates — a plain artifact must come back inert.
 */
class TuneUpScenarioTest : ScenarioTestBase() {

    init {
        test("a returned Vehicle becomes an artifact creature") {
            val game = tuneUpGame("Air Response Unit")

            val cast = game.castSpellTargetingGraveyardCard(1, "Tune Up", 1, "Air Response Unit")
            withClue("Tune Up cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val vehicle = game.findPermanent("Air Response Unit")!!
            val projected = game.state.projectedState

            withClue("It left the graveyard for the battlefield") {
                game.isInGraveyard(1, "Air Response Unit") shouldBe false
                game.isOnBattlefield("Air Response Unit") shouldBe true
            }
            withClue("It is a Vehicle, so it gains the Creature card type — and keeps Artifact") {
                projected.isCreature(vehicle) shouldBe true
                projected.hasType(vehicle, "ARTIFACT") shouldBe true
            }

            // No stated duration, so the animation survives the turn boundary (unlike crew).
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("\"It becomes an artifact creature\" has no duration — it stays a creature") {
                game.state.projectedState.isCreature(vehicle) shouldBe true
            }
        }

        test("a returned non-Vehicle artifact comes back unanimated") {
            val game = tuneUpGame("Starting Column")

            val cast = game.castSpellTargetingGraveyardCard(1, "Tune Up", 1, "Starting Column")
            withClue("Tune Up cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val column = game.findPermanent("Starting Column")!!
            withClue("It returned to the battlefield") { game.isOnBattlefield("Starting Column") shouldBe true }
            withClue("Not a Vehicle, so the conditional half does nothing") {
                game.state.projectedState.isCreature(column) shouldBe false
            }
        }
    }

    /** Tune Up in hand, [artifactInGraveyard] in the graveyard, and enough Plains to cast it. */
    private fun tuneUpGame(artifactInGraveyard: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Tune Up")
            .withCardInGraveyard(1, artifactInGraveyard)
            .withLandsOnBattlefield(1, "Plains", 4)
        repeat(8) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
