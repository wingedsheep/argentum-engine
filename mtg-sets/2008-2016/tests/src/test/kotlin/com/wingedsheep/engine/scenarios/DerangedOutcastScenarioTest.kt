package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.DerangedOutcast
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DerangedOutcastScenarioTest : ScenarioTestBase() {
    init {
        fun game() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Deranged Outcast")
            .withCardOnBattlefield(1, "Elite Vanguard")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("sacrificing a Human puts two +1/+1 counters on target creature") {
            val game = game()
            val outcast = game.findPermanent("Deranged Outcast")!!
            val human = game.findPermanent("Elite Vanguard")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = outcast,
                    abilityId = DerangedOutcast.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(human)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Elite Vanguard") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
        }

        test("a non-Human can't pay the sacrifice cost") {
            val game = game()
            val outcast = game.findPermanent("Deranged Outcast")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = outcast,
                    abilityId = DerangedOutcast.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(outcast)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            )
            result.error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
