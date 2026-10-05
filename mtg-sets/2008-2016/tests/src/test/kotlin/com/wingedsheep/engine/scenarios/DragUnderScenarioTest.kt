package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Drag Under (EMN #57) — "Return target creature to its owner's hand. Draw a card."
 */
class DragUnderScenarioTest : ScenarioTestBase() {
    init {
        test("returns an opponent's creature to its owner's hand and draws a card") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Drag Under")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Drag Under", bears).error shouldBe null
            game.resolveStack()

            withClue("the creature goes back to its owner's hand") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.state.getZone(game.player2Id, Zone.HAND).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Grizzly Bears")
            }
            withClue("the caster draws one card (Drag Under itself left the hand)") {
                game.state.getZone(game.player1Id, Zone.HAND).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                } shouldBe listOf("Island")
            }
        }

        test("can target the caster's own creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Drag Under")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Drag Under", bears).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.state.getZone(game.player1Id, Zone.HAND).mapNotNull {
                game.state.getEntity(it)?.get<CardComponent>()?.name
            }.sorted() shouldBe listOf("Grizzly Bears", "Island")
        }
    }
}
