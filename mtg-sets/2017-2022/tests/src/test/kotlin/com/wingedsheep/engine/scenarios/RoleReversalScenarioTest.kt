package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Role Reversal (WAR #214) — {U}{U}{R} Sorcery.
 *
 * Exchange control of two target permanents that share a permanent type.
 *
 * Pins the exchange itself and the `among = permanent types` restriction on the second target:
 * supertypes such as legendary don't make two permanents share a permanent type, while an artifact
 * creature and an artifact pair up on artifact even though only one is a creature.
 */
class RoleReversalScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Role Reversal")
            .withLandsOnBattlefield(1, "Island", 2)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.castRoleReversal(first: String, second: String): ExecutionResult =
            execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = findCardsInHand(1, "Role Reversal").first(),
                    targets = listOf(
                        ChosenTarget.Permanent(findPermanent(first)!!),
                        ChosenTarget.Permanent(findPermanent(second)!!),
                    ),
                    paymentStrategy = PaymentStrategy.AutoPay,
                )
            )

        fun TestGame.controllerOf(name: String) = state.projectedState.getController(findPermanent(name)!!)

        test("exchanges control of two creatures") {
            val game = builder()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()

            game.castRoleReversal("Grizzly Bears", "Hill Giant").error shouldBe null
            game.resolveStack()

            game.controllerOf("Grizzly Bears") shouldBe game.player2Id
            game.controllerOf("Hill Giant") shouldBe game.player1Id
        }

        test("a legendary creature and a legendary enchantment share no permanent type") {
            val game = builder()
                .withCardOnBattlefield(1, "Isamaru, Hound of Konda")
                .withCardOnBattlefield(2, "Night of Souls' Betrayal")
                .build()

            withClue("legendary is a supertype, not a permanent type") {
                game.castRoleReversal("Isamaru, Hound of Konda", "Night of Souls' Betrayal").error shouldNotBe null
            }
            game.controllerOf("Isamaru, Hound of Konda") shouldBe game.player1Id
            game.controllerOf("Night of Souls' Betrayal") shouldBe game.player2Id
        }

        test("an artifact creature and an artifact share artifact and are exchanged") {
            val game = builder()
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(2, "Darksteel Ingot")
                .build()

            game.castRoleReversal("Ornithopter", "Darksteel Ingot").error shouldBe null
            game.resolveStack()

            game.controllerOf("Ornithopter") shouldBe game.player2Id
            game.controllerOf("Darksteel Ingot") shouldBe game.player1Id
        }
    }
}
