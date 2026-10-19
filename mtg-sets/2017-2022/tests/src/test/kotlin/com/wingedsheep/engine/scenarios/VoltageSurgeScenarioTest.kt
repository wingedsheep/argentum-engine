package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Voltage Surge — {R} Instant (NEO #171).
 *
 * "As an additional cost to cast this spell, you may sacrifice an artifact. Voltage Surge deals 2
 * damage to target creature or planeswalker. If this spell's additional cost was paid, Voltage
 * Surge deals 4 damage instead."
 *
 * A 3/3 is the discriminator: 2 damage leaves it alive, 4 kills it.
 */
class VoltageSurgeScenarioTest : ScenarioTestBase() {

    private fun TestGame.castSurgeSacrificing(artifact: String, target: EntityId): ExecutionResult {
        val surge = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Voltage Surge" }
        val fodder = findPermanent(artifact).shouldNotBeNull()
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = surge,
                targets = listOf(ChosenTarget.Permanent(target)),
                declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        )
    }

    private fun board(fodder: String = "Ornithopter") = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Voltage Surge")
        .withCardOnBattlefield(1, fodder)
        .withCardOnBattlefield(2, "Centaur Courser")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrificing an artifact makes it 4 damage") {
            val game = board()
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()

            game.castSurgeSacrificing("Ornithopter", courser).error shouldBe null
            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe false
        }

        test("without the sacrifice it deals only 2 damage") {
            val game = board()
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()

            game.castSpell(1, "Voltage Surge", courser).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isOnBattlefield("Ornithopter") shouldBe true
        }

        test("the sacrifice variant is offered and names its cost") {
            val game = board()
            game.getLegalActions(1).filter { it.actionType == "CastWithKicker" }.map { it.description } shouldContain
                "Cast Voltage Surge (Sacrifice an artifact)"
        }

        test("a nonartifact permanent can't pay the additional cost") {
            val game = board(fodder = "Grizzly Bears")
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()

            game.castSurgeSacrificing("Grizzly Bears", courser).error.shouldNotBeNull()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
