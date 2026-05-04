package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Face-down morph spells have no characteristics (CR 708.2), so they should NOT
 * trigger "whenever you cast a historic spell" abilities like Daring Archaeologist.
 */
class MorphFaceDownHistoricTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("casting a morph face-down should not trigger YouCastHistoric") {
            val game = scenario()
                .withPlayers("Morpher", "Opponent")
                .withCardOnBattlefield(1, "Daring Archaeologist")
                .withCardInHand(1, "Proteus Machine")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Proteus Machine face-down for {3}
            val morphCardId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Proteus Machine"
            }
            val castResult = game.execute(CastSpell(game.player1Id, morphCardId, castFaceDown = true))
            withClue("Cast morph should succeed") {
                castResult.error shouldBe null
            }
            game.resolveStack()

            // Face-down creature should be on battlefield
            val faceDownId = game.state.getBattlefield().find { entityId ->
                game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
            }
            withClue("Face-down creature should be on battlefield") {
                faceDownId shouldNotBe null
            }

            // Daring Archaeologist should NOT have gotten a +1/+1 counter
            val archaeologistId = game.findPermanent("Daring Archaeologist")!!
            val projected = stateProjector.project(game.state)
            withClue("Daring Archaeologist should still be 3/3 (no historic trigger from face-down spell)") {
                projected.getPower(archaeologistId) shouldBe 3
                projected.getToughness(archaeologistId) shouldBe 3
            }
        }

        test("casting an artifact face-up should still trigger YouCastHistoric") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Daring Archaeologist")
                .withCardInHand(1, "Proteus Machine")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast Proteus Machine face-up (normal cast)
            game.castSpell(1, "Proteus Machine")
            game.resolveStack()

            // Daring Archaeologist SHOULD have gotten a +1/+1 counter
            val archaeologistId = game.findPermanent("Daring Archaeologist")!!
            val projected = stateProjector.project(game.state)
            withClue("Daring Archaeologist should be 4/4 (historic trigger from artifact spell)") {
                projected.getPower(archaeologistId) shouldBe 4
                projected.getToughness(archaeologistId) shouldBe 4
            }
        }
    }
}
