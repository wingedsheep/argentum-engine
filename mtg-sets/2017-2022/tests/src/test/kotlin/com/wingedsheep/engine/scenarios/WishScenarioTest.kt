package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wish {2}{R} — Sorcery: "You may play a card you own from outside the game this turn."
 *
 * Outside the game is the owner's sideboard (CR 400.11a). Pinned below: before Wish nothing in the
 * sideboard is playable; after it a spell or a land from the sideboard can be played, paid for and
 * timed normally (a land uses the turn's land play); only one card; and the permission ends with
 * the turn.
 */
class WishScenarioTest : ScenarioTestBase() {

    private fun TestGame.sideboardCard(name: String): EntityId =
        state.getZone(player1Id, Zone.SIDEBOARD).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.sideboardActions(): List<String> =
        getLegalActions(1).filter { it.sourceZone == "SIDEBOARD" }.map { it.actionType + ":" + it.description }

    private fun wishGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Wish")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardInSideboard(1, "Raging Goblin")
        .withCardInSideboard(1, "Lightning Bolt")
        .withCardInSideboard(1, "Island")
        .withCardInSideboard(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("nothing outside the game is playable before Wish resolves") {
            val game = wishGame()
            game.sideboardActions() shouldBe emptyList()
            withClue("a direct cast from the sideboard is rejected") {
                game.execute(CastSpell(game.player1Id, game.sideboardCard("Lightning Bolt"))).error shouldNotBe null
            }
        }

        test("after Wish, a spell from the sideboard is cast and paid for normally — and only one card") {
            val game = wishGame()
            game.castSpell(1, "Wish").error shouldBe null
            game.resolveStack()

            val offered = game.sideboardActions()
            withClue("every own sideboard card is offered, the opponent's is not: $offered") {
                offered.any { it.contains("Raging Goblin") } shouldBe true
                offered.any { it.contains("Lightning Bolt") } shouldBe true
                offered.any { it.startsWith("PlayLand") && it.contains("Island") } shouldBe true
                offered.any { it.contains("Hill Giant") } shouldBe false
            }

            game.execute(CastSpell(game.player1Id, game.sideboardCard("Raging Goblin"))).error shouldBe null
            withClue("the cast left the sideboard") { game.isInSideboard(1, "Raging Goblin") shouldBe false }
            game.resolveStack()
            game.isOnBattlefield("Raging Goblin") shouldBe true

            withClue("Wish lets you play one card, not each of them") {
                game.sideboardActions() shouldBe emptyList()
                game.execute(CastSpell(game.player1Id, game.sideboardCard("Lightning Bolt"))).error shouldNotBe null
            }
        }

        test("an instant cast from the sideboard goes to its owner's graveyard once it resolves") {
            val game = wishGame()
            game.castSpell(1, "Wish")
            game.resolveStack()

            game.execute(
                CastSpell(game.player1Id, game.sideboardCard("Lightning Bolt"), listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
            withClue("a card that entered the game stays in it (CR 400.11b): graveyard, not back outside") {
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInSideboard(1, "Lightning Bolt") shouldBe false
            }
        }

        test("a land from the sideboard is played with the turn's land play") {
            val game = wishGame()
            game.castSpell(1, "Wish")
            game.resolveStack()

            game.execute(PlayLand(game.player1Id, game.sideboardCard("Island"))).error shouldBe null
            game.isOnBattlefield("Island") shouldBe true
            game.isInSideboard(1, "Island") shouldBe false
            withClue("that was the one card") { game.sideboardActions() shouldBe emptyList() }
        }

        test("the permission ends with the turn") {
            val game = wishGame()
            game.castSpell(1, "Wish")
            game.resolveStack()
            game.sideboardActions().isEmpty() shouldBe false

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("on the next turn, the grant is gone") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.mayPlayPermissions.none { it.controllerId == game.player1Id } shouldBe true
            }
        }
    }
}
