package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.SkirsdagFlayer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class SkirsdagFlayerScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing itself (a Human) destroys target creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Skirsdag Flayer")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val flayer = game.findPermanent("Skirsdag Flayer")!!
            val angel = game.findPermanent("Serra Angel")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = flayer,
                    abilityId = SkirsdagFlayer.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(angel)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(flayer)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Skirsdag Flayer") shouldBe true
            game.isInGraveyard(2, "Serra Angel") shouldBe true
        }
    }
}
