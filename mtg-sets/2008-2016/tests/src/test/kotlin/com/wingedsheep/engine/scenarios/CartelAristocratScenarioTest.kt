package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cartel Aristocrat (GTC #150) — Sacrifice another creature: this creature gains protection from
 * the color of your choice until end of turn.
 */
class CartelAristocratScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing another creature grants protection from the chosen color") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cartel Aristocrat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val aristocrat = game.findPermanent("Cartel Aristocrat")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Cartel Aristocrat")!!.script.activatedAbilities.single().id

            val result = game.execute(
                ActivateAbility(
                    game.player1Id,
                    aristocrat,
                    abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseColorDecision>()
            game.submitDecision(ColorChosenResponse(decision.id, Color.RED))
            game.resolveStack()

            withClue("a red spell can no longer target the Aristocrat") {
                game.castSpell(1, "Lightning Bolt", aristocrat).error shouldNotBe null
            }
            game.isOnBattlefield("Cartel Aristocrat") shouldBe true
        }

        test("it can't sacrifice itself to pay the cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cartel Aristocrat")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val aristocrat = game.findPermanent("Cartel Aristocrat")!!
            val abilityId = cardRegistry.getCard("Cartel Aristocrat")!!.script.activatedAbilities.single().id

            val result = game.execute(
                ActivateAbility(
                    game.player1Id,
                    aristocrat,
                    abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(aristocrat)),
                )
            )
            result.error shouldNotBe null
            game.isOnBattlefield("Cartel Aristocrat") shouldBe true
        }
    }
}
