package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Holdout Settlement — Land.
 * "{T}: Add {C}. {T}, Tap an untapped creature you control: Add one mana of any color."
 */
class HoldoutSettlementScenarioTest : ScenarioTestBase() {
    init {
        val abilities = cardRegistry.getCard("Holdout Settlement")!!.script.activatedAbilities
        val colorlessId = abilities[0].id
        val anyColorId = abilities[1].id

        fun board(withCreature: Boolean) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Holdout Settlement")
            .let { if (withCreature) it.withCardOnBattlefield(1, "Grizzly Bears") else it }
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun pool(game: TestGame) = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!

        test("{T} adds {C}") {
            val game = board(withCreature = false)
            val land = game.findPermanent("Holdout Settlement")!!
            game.execute(ActivateAbility(game.player1Id, land, colorlessId)).error shouldBe null
            pool(game).colorless shouldBe 1
        }

        test("tapping a creature too adds one mana of the chosen color") {
            val game = board(withCreature = true)
            val land = game.findPermanent("Holdout Settlement")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, land, anyColorId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                    manaColorChoice = Color.GREEN,
                )
            )
            withClue("the Bears pay the second tap: ${result.error}") { result.error shouldBe null }
            pool(game).green shouldBe 1
            game.state.getEntity(land)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
        }

        test("without a creature to tap, the colored ability can't be activated") {
            val game = board(withCreature = false)
            val land = game.findPermanent("Holdout Settlement")!!
            game.execute(
                ActivateAbility(game.player1Id, land, anyColorId, manaColorChoice = Color.GREEN)
            ).error shouldNotBe null
        }
    }
}
