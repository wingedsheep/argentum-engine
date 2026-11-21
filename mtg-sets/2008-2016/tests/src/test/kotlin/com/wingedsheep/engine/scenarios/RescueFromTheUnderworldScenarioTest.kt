package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rescue from the Underworld — sacrifice a creature, choose a creature card in your graveyard, and
 * both return under your control at the beginning of your next upkeep. Pins the rulings: the spell
 * exiles itself as it resolves; a sacrificed token never returns; a card that leaves its zone before
 * the upkeep stays put; and a sacrificed creature a replacement exiled instead of putting it into the
 * graveyard still returns, from exile.
 */
class RescueFromTheUnderworldScenarioTest : ScenarioTestBase() {

    private fun board(sacrificeToken: Boolean = false, restInPeace: Boolean = false, bobOwnsGiant: Boolean = false) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Rescue from the Underworld")
        .withLandsOnBattlefield(1, "Swamp", 5)
        .withCardOnBattlefield(if (bobOwnsGiant) 2 else 1, "Hill Giant", isToken = sacrificeToken)
        .withCardInGraveyard(1, "Grizzly Bears")
        .apply { if (restInPeace) withCardOnBattlefield(2, "Rest in Peace") }
        .withCardInLibrary(1, "Swamp").withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp").withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(targetCard: EntityId) {
        val result = execute(
            CastSpell(
                playerId = player1Id,
                cardId = findCardsInHand(1, "Rescue from the Underworld").single(),
                targets = listOf(ChosenTarget.Card(targetCard, player1Id, Zone.GRAVEYARD)),
                additionalCostPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(findPermanent("Hill Giant")!!)
                ),
            )
        )
        withClue("${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    /** Pass through Bob's turn to the beginning of Alice's next upkeep and resolve the trigger. */
    private fun TestGame.toAlicesNextUpkeep() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        withClue("Bob's upkeep returns nothing") {
            isOnBattlefield("Grizzly Bears") shouldBe false
            isOnBattlefield("Hill Giant") shouldBe false
        }
        passUntilPhase(Phase.BEGINNING, Step.DRAW)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        state.activePlayerId shouldBe player1Id
        resolveStack()
    }

    private fun TestGame.controlledByAlice(name: String) =
        state.projectedState.getController(findPermanent(name)!!) shouldBe player1Id

    init {
        test("both cards return under Alice's control at her next upkeep; the spell exiles itself") {
            val game = board()
            game.cast(game.findCardsInGraveyard(1, "Grizzly Bears").single())

            game.isInExile(1, "Rescue from the Underworld") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false

            game.toAlicesNextUpkeep()
            game.controlledByAlice("Grizzly Bears")
            game.controlledByAlice("Hill Giant")
        }

        test("a sacrificed token doesn't return, but the chosen card does") {
            val game = board(sacrificeToken = true)
            game.cast(game.findCardsInGraveyard(1, "Grizzly Bears").single())
            game.toAlicesNextUpkeep()
            game.controlledByAlice("Grizzly Bears")
            game.isOnBattlefield("Hill Giant") shouldBe false
        }

        test("a card that leaves the graveyard before the upkeep doesn't return") {
            val game = board()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.cast(bears)
            game.state = game.state.moveToZone(
                bears, ZoneKey(game.player1Id, Zone.GRAVEYARD), ZoneKey(game.player1Id, Zone.EXILE)
            )
            game.toAlicesNextUpkeep()
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.controlledByAlice("Hill Giant")
        }

        test("a sacrificed card that leaves the graveyard before the upkeep doesn't return") {
            val game = board()
            game.cast(game.findCardsInGraveyard(1, "Grizzly Bears").single())
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.state = game.state.moveToZone(
                giant, ZoneKey(game.player1Id, Zone.GRAVEYARD), ZoneKey(game.player1Id, Zone.EXILE)
            )
            game.toAlicesNextUpkeep()
            game.isInExile(1, "Hill Giant") shouldBe true
            game.controlledByAlice("Grizzly Bears")
        }

        test("a sacrificed creature Bob owns goes to his graveyard and returns under Alice's control") {
            val game = board(bobOwnsGiant = true)
            val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) { it.with(ControllerComponent(game.player1Id)) }
            game.cast(game.findCardsInGraveyard(1, "Grizzly Bears").single())
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.toAlicesNextUpkeep()
            game.controlledByAlice("Hill Giant")
        }

        test("a sacrificed creature exiled by a replacement returns from exile") {
            val game = board(restInPeace = true)
            game.cast(game.findCardsInGraveyard(1, "Grizzly Bears").single())
            withClue("Rest in Peace exiled the sacrificed Hill Giant instead") {
                game.isInExile(1, "Hill Giant") shouldBe true
            }
            game.toAlicesNextUpkeep()
            game.controlledByAlice("Hill Giant")
            game.controlledByAlice("Grizzly Bears")
        }
    }
}
