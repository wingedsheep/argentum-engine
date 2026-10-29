package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reality Shift (FRF #46).
 *
 *   Exile target creature. Its controller manifests the top card of their library.
 *
 * Pins whose library and whose battlefield: "its controller" is the exiled creature's last-known
 * controller, so the face-down 2/2 comes from — and enters under — the opponent, never the caster.
 */
class RealityShiftScenarioTest : ScenarioTestBase() {

    init {
        context("Reality Shift") {

            test("the exiled creature's controller manifests the top card of their own library") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Reality Shift")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findCardsInLibrary(2, "Hill Giant").single()

                game.castSpell(1, "Reality Shift", bears).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                withClue("Bob's top card is now a face-down manifested permanent he controls") {
                    game.state.getBattlefield(game.player2Id).contains(giant) shouldBe true
                    game.state.getEntity(giant)?.get<FaceDownComponent>() shouldBe FaceDownComponent
                    game.state.getEntity(giant)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
                withClue("Alice's library is untouched") {
                    game.findCardsInLibrary(1, "Island").size shouldBe 1
                }
            }
        }
    }
}
