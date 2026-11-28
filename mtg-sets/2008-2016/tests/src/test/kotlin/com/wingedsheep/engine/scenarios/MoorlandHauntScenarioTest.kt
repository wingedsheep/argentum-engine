package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Moorland Haunt (ISD #244) — Land.
 *
 *   {T}: Add {C}.
 *   {W}{U}, {T}, Exile a creature card from your graveyard: Create a 1/1 white Spirit creature
 *   token with flying.
 */
class MoorlandHauntScenarioTest : ScenarioTestBase() {

    init {
        val spiritAbilityId = cardRegistry.getCard("Moorland Haunt")!!.script.activatedAbilities[1].id

        test("exiling a creature card from your graveyard makes a 1/1 flying Spirit") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Moorland Haunt")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val haunt = game.findPermanent("Moorland Haunt")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val result = game.execute(
                ActivateAbility(
                    game.player1Id,
                    haunt,
                    spiritAbilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.resolveStack()

            val spirit = game.findPermanent("Spirit Token")
            spirit shouldNotBe null
            game.state.projectedState.getPower(spirit!!) shouldBe 1
            game.state.projectedState.getToughness(spirit) shouldBe 1
            game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
        }

        test("a noncreature card can't pay the exile cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Moorland Haunt")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val haunt = game.findPermanent("Moorland Haunt")!!
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.execute(
                ActivateAbility(
                    game.player1Id,
                    haunt,
                    spiritAbilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(bolt)),
                )
            ).error shouldNotBe null
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.findPermanent("Spirit Token") shouldBe null
        }
    }
}
