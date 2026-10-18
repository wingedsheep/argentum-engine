package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.ante.AnteSettlement
import com.wingedsheep.engine.mechanics.ante.AnteTransferCause
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.leg.cards.TempestEfreet
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tempest Efreet {1}{R}{R}{R} — Creature — Efreet 3/3:
 * "{T}, Sacrifice this creature: Target opponent may pay 10 life. If that player doesn't, they
 * reveal a card at random from their hand. Exchange ownership of the revealed card and Tempest
 * Efreet. Put the revealed card into your hand and Tempest Efreet from anywhere into that player's
 * graveyard. This change in ownership is permanent."
 */
class TempestEfreetScenarioTest : ScenarioTestBase() {

    private fun setUp(
        opponentLife: Int = 20,
        opponentHand: List<String> = listOf("Shivan Dragon"),
        leylineOfTheVoid: Boolean = false,
    ) = scenario()
        .withPlayers("Player1", "Player2")
        .playingForAnte()
        .withCardOnBattlefield(1, "Tempest Efreet")
        .apply { if (leylineOfTheVoid) withCardOnBattlefield(2, "Leyline of the Void") }
        .apply { opponentHand.forEach { withCardInHand(2, it) } }
        .withLifeTotal(2, opponentLife)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate() {
        execute(
            ActivateAbility(
                player1Id, findPermanent("Tempest Efreet")!!, TempestEfreet.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(player2Id)),
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun TestGame.ownerOf(playerNumber: Int, zone: Zone, name: String) =
        state.getZone(if (playerNumber == 1) player1Id else player2Id, zone)
            .single { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name }
            .let { state.getEntity(it)?.get<OwnerComponent>()?.playerId }

    init {
        test("the opponent declines: you own their revealed card, in your hand, and they own the Efreet") {
            val game = setUp()
            game.activate()

            withClue("the opponent is offered the 10 life") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(false)

            game.getLifeTotal(2) shouldBe 20
            game.isInHand(1, "Shivan Dragon") shouldBe true
            game.handSize(2) shouldBe 0
            game.isInGraveyard(2, "Tempest Efreet") shouldBe true
            game.graveyardSize(1) shouldBe 0
            game.ownerOf(1, Zone.HAND, "Shivan Dragon") shouldBe game.player1Id
            game.ownerOf(2, Zone.GRAVEYARD, "Tempest Efreet") shouldBe game.player2Id

            val settled = AnteSettlement.settle(game.state).associateBy { it.cardName }
            settled.getValue("Shivan Dragon").toPlayerId shouldBe game.player1Id
            settled.getValue("Tempest Efreet").toPlayerId shouldBe game.player2Id
            settled.values.all { it.cause == AnteTransferCause.OWNERSHIP } shouldBe true
        }

        test("the opponent pays 10 life: nothing changes hands, the Efreet stays in your graveyard") {
            val game = setUp()
            game.activate()
            game.answerYesNo(true)

            game.getLifeTotal(2) shouldBe 10
            game.isInHand(2, "Shivan Dragon") shouldBe true
            game.isInGraveyard(1, "Tempest Efreet") shouldBe true
            AnteSettlement.settle(game.state) shouldBe emptyList()
        }

        test("an opponent below 10 life can't pay, so the exchange happens without asking (CR 119.4)") {
            val game = setUp(opponentLife = 9)
            game.activate()

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Shivan Dragon") shouldBe true
            game.isInGraveyard(2, "Tempest Efreet") shouldBe true
        }

        test("from anywhere: an Efreet exiled instead of dying (their Leyline of the Void) still goes to their graveyard") {
            val game = setUp(leylineOfTheVoid = true)
            game.activate()
            withClue("the sacrificed Efreet was exiled instead") { game.isInExile(1, "Tempest Efreet") shouldBe true }
            game.answerYesNo(false)

            game.isInHand(1, "Shivan Dragon") shouldBe true
            game.isInGraveyard(2, "Tempest Efreet") shouldBe true
            game.state.getZone(game.player1Id, Zone.EXILE) shouldBe emptyList()
            game.state.getZone(game.player2Id, Zone.EXILE) shouldBe emptyList()
        }

        test("an empty hand reveals nothing, so the exchange can't happen and nothing changes (CR 701.12a)") {
            val game = setUp(opponentHand = emptyList())
            game.activate()
            if (game.hasPendingDecision()) game.answerYesNo(false)

            game.isInGraveyard(1, "Tempest Efreet") shouldBe true
            game.ownerOf(1, Zone.GRAVEYARD, "Tempest Efreet") shouldBe game.player1Id
            AnteSettlement.settle(game.state) shouldBe emptyList()
        }
    }
}
