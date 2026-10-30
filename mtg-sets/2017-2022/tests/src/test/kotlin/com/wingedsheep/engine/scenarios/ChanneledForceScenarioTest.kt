package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Channeled Force (IKO #180) — {2}{U}{R} Instant.
 *
 * "As an additional cost to cast this spell, discard X cards.
 *  Target player draws X cards. Channeled Force deals X damage to up to one target creature or
 *  planeswalker."
 *
 * The first spell on `Costs.additional.DiscardVariableCards`: the caster picks how many cards to
 * discard (zero included) and that number is the spell's X.
 */
class ChanneledForceScenarioTest : ScenarioTestBase() {

    private fun TestGame.channeledForce(): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Channeled Force" }

    private fun TestGame.cast(discarded: List<EntityId>, targets: List<ChosenTarget>) = execute(
        CastSpell(
            player1Id,
            channeledForce(),
            targets,
            additionalCostPayment = AdditionalCostPayment(discardedCards = discarded)
        )
    )

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Channeled Force")
        .withCardInHand(1, "Forest")
        .withCardInHand(1, "Island")
        .withCardInHand(1, "Mountain")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("offers a 0..hand-size discard, never the spell itself") {
            val game = board()
            val castInfo = game.getLegalActions(1).first {
                it.actionType == "CastSpell" && it.description.contains("Channeled Force")
            }
            val cost = castInfo.additionalCostInfo
            cost shouldNotBe null
            cost!!.costType shouldBe "DiscardCard"
            cost.discardCount shouldBe 0
            cost.discardMaxCount shouldBe 3
            withClue("the spell isn't in its own discard pool") {
                (game.channeledForce() in cost.validDiscardTargets) shouldBe false
            }
        }

        test("discarding two: target player draws two and the creature takes two") {
            val game = board()
            val discards = game.state.getHand(game.player1Id).filter { it != game.channeledForce() }.take(2)
            val bears = game.findPermanent("Grizzly Bears")!!

            val cast = game.cast(discards, listOf(ChosenTarget.Player(game.player1Id), ChosenTarget.Permanent(bears)))
            withClue("cast discarding two: ${cast.error}") { cast.error shouldBe null }
            withClue("both discards are in the graveyard") { game.graveyardSize(1) shouldBe 2 }
            game.resolveStack()

            withClue("one card left in hand plus two drawn") { game.handSize(1) shouldBe 3 }
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("X can be 0: discarding nothing draws nothing and deals no damage") {
            val game = board()
            val cast = game.cast(emptyList(), listOf(ChosenTarget.Player(game.player2Id)))
            withClue("cast discarding nothing, no creature target: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.handSize(1) shouldBe 3
            game.handSize(2) shouldBe 0
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("the spell can't discard itself") {
            val game = board()
            val cast = game.cast(listOf(game.channeledForce()), listOf(ChosenTarget.Player(game.player2Id)))
            cast.error shouldNotBe null
        }
    }
}
