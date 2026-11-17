package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.frf.cards.WhisperwoodElemental
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Whisperwood Elemental (FRF) — {3}{G}{G} 4/4.
 * "At the beginning of your end step, manifest the top card of your library.
 * Sacrifice this creature: Until end of turn, face-up nontoken creatures you control gain 'When this
 * creature dies, manifest the top card of your library.'"
 *
 * The sacrifice ability grants a temporary dies trigger to a filtered group locked in at resolution.
 * This proves the filter: after a board wipe, only the face-up nontoken creature's death manifests —
 * the face-down manifest and the token die without a trigger.
 */
class WhisperwoodElementalScenarioTest : ScenarioTestBase() {

    init {
        test("end step manifests; sacrifice grants the dies trigger only to face-up nontoken creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Whisperwood Elemental")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions", isToken = true)
                .withCardInHand(1, "Kozilek's Return")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun faceDownCount() = game.state.getBattlefield(game.player1Id)
                .count { game.state.getEntity(it)?.get<FaceDownComponent>() != null }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the end-step trigger manifested the top card") {
                faceDownCount() shouldBe 1
                game.librarySize(1) shouldBe 3
            }

            val whisperwood = game.findPermanent("Whisperwood Elemental")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = whisperwood,
                    abilityId = WhisperwoodElemental.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Whisperwood Elemental") shouldBe true

            // 2 damage to each creature: kills the Bears, the face-down 2/2 and the Lions token.
            game.castSpell(1, "Kozilek's Return").error shouldBe null
            game.resolveStack()

            withClue("all three creatures died") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Savannah Lions") shouldBe false
            }
            withClue("only the face-up nontoken Bears' death manifested a card") {
                faceDownCount() shouldBe 1
                game.librarySize(1) shouldBe 2
            }
        }
    }
}
