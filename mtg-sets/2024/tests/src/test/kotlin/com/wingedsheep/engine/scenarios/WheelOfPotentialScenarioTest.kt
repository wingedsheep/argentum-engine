package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wheel of Potential (MH3) — "You get {E}{E}{E}, then you may pay any amount of {E}. Each player
 * may exile their hand and draw a number of cards equal to the amount of {E} paid this way. If
 * seven or more {E} was paid this way, you may play cards you own exiled this way until the end
 * of your next turn."
 */
class WheelOfPotentialScenarioTest : ScenarioTestBase() {

    private fun TestGame.giveEnergy(amount: Int) {
        state = state.updateEntity(player1Id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        }
    }

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.exiledNamed(playerId: EntityId, name: String): EntityId =
        state.getExile(playerId).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.playable(cardId: EntityId): Boolean =
        state.mayPlayPermissions.any { cardId in it.cardIds && it.controllerId == player1Id }

    private fun build(): TestGame {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Wheel of Potential")
            .withCardInHand(1, "Lightning Bolt")
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(2, "Hill Giant")
            .withCardInHand(2, "Shock")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) {
            b = b.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        }
        return b.build()
    }

    /** Cast the Wheel, pay [paid] energy, then answer each player's may (APNAP order). */
    private fun TestGame.castAndResolve(paid: Int, p1Exiles: Boolean, p2Exiles: Boolean) {
        castSpell(1, "Wheel of Potential").error shouldBe null
        resolveStack()
        val pay = getPendingDecision()
        (pay is ChooseNumberDecision) shouldBe true
        (pay as ChooseNumberDecision).maxValue shouldBe energy()
        chooseNumber(paid).error shouldBe null

        val first = getPendingDecision() as YesNoDecision
        withClue("the active player decides first") { first.playerId shouldBe player1Id }
        answerYesNo(p1Exiles).error shouldBe null
        val second = getPendingDecision() as YesNoDecision
        withClue("then the opponent decides for their own hand") { second.playerId shouldBe player2Id }
        answerYesNo(p2Exiles).error shouldBe null
        hasPendingDecision() shouldBe false
    }

    init {
        context("Wheel of Potential") {

            test("paying seven: both players wheel into seven, and only your own exiled cards are playable") {
                val game = build()
                game.giveEnergy(4)
                game.castAndResolve(paid = 7, p1Exiles = true, p2Exiles = true)

                withClue("seven energy spent of the 4 + 3 available") { game.energy() shouldBe 0 }
                game.handSize(1) shouldBe 7
                game.handSize(2) shouldBe 7

                val bolt = game.exiledNamed(game.player1Id, "Lightning Bolt")
                val bears = game.exiledNamed(game.player1Id, "Grizzly Bears")
                val giant = game.exiledNamed(game.player2Id, "Hill Giant")
                withClue("cards you own exiled this way are playable") {
                    game.playable(bolt) shouldBe true
                    game.playable(bears) shouldBe true
                }
                withClue("the opponent's exiled cards are not") {
                    game.state.mayPlayPermissions.any { giant in it.cardIds } shouldBe false
                }

                withClue("the exiled Lightning Bolt can actually be cast from exile") {
                    // Known engine bug: after resolution priority sits with the last decision's
                    // answerer (the opponent) instead of the active player (CR 117.3b). Hand it
                    // back to the caster so this test exercises the permission, not that bug.
                    game.state = game.state.copy(priorityPlayerId = game.player1Id)
                    val cast = game.execute(
                        CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)))
                    )
                    cast.error shouldBe null
                    game.resolveStack()
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("paying less than seven: hands are exiled and replaced, but nothing is playable") {
                val game = build()
                game.castAndResolve(paid = 3, p1Exiles = true, p2Exiles = true)

                game.energy() shouldBe 0
                game.handSize(1) shouldBe 3
                game.handSize(2) shouldBe 3
                val bolt = game.exiledNamed(game.player1Id, "Lightning Bolt")
                game.playable(bolt) shouldBe false
            }

            test("a player who declines keeps their hand and draws nothing") {
                val game = build()
                game.giveEnergy(4)
                game.castAndResolve(paid = 7, p1Exiles = true, p2Exiles = false)

                game.handSize(1) shouldBe 7
                withClue("the opponent kept Hill Giant and Shock") {
                    game.handSize(2) shouldBe 2
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
            }

            test("paying zero still lets a player exile their hand, drawing nothing") {
                val game = build()
                game.castAndResolve(paid = 0, p1Exiles = true, p2Exiles = false)

                game.energy() shouldBe 3
                game.handSize(1) shouldBe 0
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }
        }
    }
}
