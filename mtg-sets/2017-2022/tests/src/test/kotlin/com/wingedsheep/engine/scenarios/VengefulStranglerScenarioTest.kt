package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vengeful Strangler // Strangling Grasp (MID #126).
 *
 *   Front — This creature can't block. When this creature dies, return it to the battlefield
 *           transformed under your control attached to target creature or planeswalker an
 *           opponent controls.
 *   Back  — Enchant creature or planeswalker an opponent controls. At the beginning of your
 *           upkeep, enchanted permanent's controller sacrifices a nonland permanent of their
 *           choice, then that player loses 1 life.
 *
 * The test that matters is the controller one: an Aura returned onto a permanent follows that
 * permanent's controller unless the text says "under your control" — and if it followed here, the
 * opponent would control an Aura whose "an opponent controls" no longer holds, and whose upkeep
 * trigger would fire on the wrong player's turn.
 */
class VengefulStranglerScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.attachedTo(id: EntityId): EntityId? =
        state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    init {
        test("when it dies it returns as Strangling Grasp under its owner's control on the opponent's creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vengeful Strangler", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val strangler = game.findPermanent("Vengeful Strangler")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", targetId = strangler).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("the same card is back, showing its Aura face") {
                game.nameOf(strangler) shouldBe "Strangling Grasp"
                game.isOnBattlefield("Strangling Grasp") shouldBe true
            }
            game.attachedTo(strangler) shouldBe bears
            withClue("'under your control' — player 1's Aura on player 2's creature, still legal after SBAs") {
                game.state.projectedState.getController(strangler) shouldBe game.player1Id
            }
        }

        test("at its controller's upkeep the enchanted permanent's controller sacrifices and loses 1 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Strangling Grasp")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            val grasp = game.findPermanent("Strangling Grasp")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(grasp) { it.with(AttachedToComponent(bears)) }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectCards(listOf(bears))
            game.resolveStack()

            withClue("player 2's only nonland permanent is sacrificed; lands are not eligible") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanents("Forest").size shouldBe 2
            }
            game.getLifeTotal(2) shouldBe 19
            withClue("the Aura's controller loses nothing") {
                game.getLifeTotal(1) shouldBe 20
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
