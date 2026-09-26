package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * He Who Hungers (CHK #114) — Flying; "{1}, Sacrifice a Spirit: Target opponent reveals their
 * hand. You choose a card from it. That player discards that card. Activate only as a sorcery.";
 * Soulshift 4.
 */
class HeWhoHungersScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "He Who Hungers")
            .withCardOnBattlefield(1, "Kami of the Hunt")
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withCardInHand(2, "Forest").withCardInHand(2, "Grizzly Bears")
            .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")

        fun ability() = cardRegistry.getCard("He Who Hungers")!!.activatedAbilities.single()

        test("has flying") {
            val game = base().withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state.projectedState.hasKeyword(game.findPermanent("He Who Hungers")!!, Keyword.FLYING) shouldBe true
        }

        test("sacrificing a Spirit lets the controller choose the opponent's discard") {
            val game = base().withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("He Who Hungers")!!
            val kami = game.findPermanent("Kami of the Hunt")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, source, ability().id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(kami))
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.playerId shouldBe game.player1Id
            val bear = game.state.getHand(game.player2Id)
                .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            game.selectCards(listOf(bear)).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInHand(2, "Forest") shouldBe true
        }

        test("cannot be activated at instant speed") {
            val game = base().withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority()
            val source = game.findPermanent("He Who Hungers")!!
            val kami = game.findPermanent("Kami of the Hunt")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, source, ability().id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(kami))
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Kami of the Hunt") shouldBe true
        }

        test("cannot target yourself") {
            val game = base().withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val source = game.findPermanent("He Who Hungers")!!
            val kami = game.findPermanent("Kami of the Hunt")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, source, ability().id,
                    targets = listOf(ChosenTarget.Player(game.player1Id)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(kami))
                )
            ).error shouldNotBe null
        }
    }
}
