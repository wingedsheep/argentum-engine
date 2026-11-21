package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Polis Crusher (THS #198): "Whenever this creature deals combat damage to a player, if this
 * creature is monstrous, destroy target enchantment that player controls."
 *
 * Proves the intervening-if gates the trigger, the target is an enchantment the damaged player
 * controls, and (ruling 2013-09-15) once the ability has triggered it still resolves after Polis
 * Crusher has left the battlefield — "is monstrous" is rechecked on its last-known information.
 */
class PolisCrusherScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateMonstrosity() = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Polis Crusher") ?: error("Polis Crusher is not on the battlefield"),
            abilityId = cardRegistry.getCard("Polis Crusher")!!.activatedAbilities[0].id,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    private fun board(withRemoval: Boolean = false) = scenario().withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Polis Crusher")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardOnBattlefield(1, "Glorious Anthem")
        .withCardOnBattlefield(2, "Glorious Anthem")
        .apply {
            if (withRemoval) {
                withLandsOnBattlefield(1, "Swamp", 3)
                withCardInHand(1, "Murder")
            }
        }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.opponentsAnthem() =
        state.getBattlefield(player2Id)
            .single { state.getEntity(it)?.get<CardComponent>()?.name == "Glorious Anthem" }

    private fun TestGame.attackUnblocked() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Polis Crusher" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers().error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
    }

    init {
        test("a monstrous Polis Crusher destroys target enchantment the damaged player controls") {
            val game = board()
            game.activateMonstrosity().error shouldBe null
            game.resolveStack()
            val anthem = game.opponentsAnthem()

            game.attackUnblocked()
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(anthem)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Glorious Anthem") shouldBe true
            game.isInGraveyard(1, "Glorious Anthem") shouldBe false
        }

        test("an enchantment its own controller controls is not a legal target") {
            val game = board()
            game.activateMonstrosity().error shouldBe null
            game.resolveStack()
            val ownAnthem = game.state.getBattlefield(game.player1Id)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Glorious Anthem" }

            game.attackUnblocked()
            game.hasPendingDecision() shouldBe true
            (game.selectTargets(listOf(ownAnthem)).error != null) shouldBe true
        }

        test("a Polis Crusher that isn't monstrous doesn't trigger") {
            val game = board()
            game.attackUnblocked()

            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.isInGraveyard(2, "Glorious Anthem") shouldBe false
        }

        test("killed in response, the trigger still resolves on last-known monstrous status") {
            // Monstrous already, so the Swamps stay free for Murder.
            val game = board(withRemoval = true)
            val crusher = game.findPermanent("Polis Crusher").shouldNotBeNull()
            game.state = game.state.updateEntity(crusher) { it.with(MonstrousComponent) }
            val anthem = game.opponentsAnthem()

            game.attackUnblocked()
            game.selectTargets(listOf(anthem)).error shouldBe null

            game.castSpell(1, "Murder", crusher).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Polis Crusher") shouldBe true
            game.isInGraveyard(2, "Glorious Anthem") shouldBe true
        }
    }
}
