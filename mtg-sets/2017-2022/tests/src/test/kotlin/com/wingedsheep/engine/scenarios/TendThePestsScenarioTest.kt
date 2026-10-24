package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tend the Pests (STX #242) — "As an additional cost to cast this spell, sacrifice a creature.
 * Create X 1/1 black and green Pest creature tokens with 'When this token dies, you gain 1 life,'
 * where X is the sacrificed creature's power."
 *
 * The token count is a cost-linked amount: the sacrificed creature is gone by resolution, so X has
 * to come from its sacrifice-time snapshot, not a live read.
 */
class TendThePestsScenarioTest : ScenarioTestBase() {

    private fun TestGame.castTendThePests(sacrificing: String) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Tend the Pests").single(),
            additionalCostPayment = AdditionalCostPayment(
                sacrificedPermanents = listOf(findPermanent(sacrificing)!!)
            ),
        )
    )

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Tend the Pests")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Ornithopter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrificing a 3-power creature creates three Pests") {
            val game = board()
            game.castTendThePests("Hill Giant").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            withClue("X is the sacrificed Hill Giant's power") {
                game.findPermanents("Pest").size shouldBe 3
            }
        }

        test("sacrificing a 0-power creature creates no Pests") {
            val game = board()
            game.castTendThePests("Ornithopter").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.findPermanents("Pest").size shouldBe 0
        }
    }
}
