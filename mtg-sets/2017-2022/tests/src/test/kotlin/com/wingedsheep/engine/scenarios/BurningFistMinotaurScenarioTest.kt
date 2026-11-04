package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.BurningFistMinotaur
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Burning-Fist Minotaur (HOU) — {1}{R} 2/1 first strike.
 * "{1}{R}, Discard a card: This creature gets +2/+0 until end of turn."
 */
class BurningFistMinotaurScenarioTest : ScenarioTestBase() {
    private val pumpId = BurningFistMinotaur.activatedAbilities.single().id

    init {
        test("paying {1}{R} and discarding a card gives it +2/+0") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Burning-Fist Minotaur")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val minotaur = game.findPermanent("Burning-Fist Minotaur")!!
            game.state.projectedState.hasKeyword(minotaur, Keyword.FIRST_STRIKE) shouldBe true

            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            game.execute(
                ActivateAbility(
                    game.player1Id, minotaur, pumpId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.projectedState.getPower(minotaur) shouldBe 4
            game.state.projectedState.getToughness(minotaur) shouldBe 1
        }

        test("can't be activated with no card to discard") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Burning-Fist Minotaur")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val minotaur = game.findPermanent("Burning-Fist Minotaur")!!
            game.execute(ActivateAbility(game.player1Id, minotaur, pumpId)).error shouldNotBe null
            game.state.projectedState.getPower(minotaur) shouldBe 2
        }
    }
}
