package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Torch Courier — "Haste. Sacrifice this creature: Another target creature gains haste until end
 * of turn."
 */
class TorchCourierScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Torch Courier")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Torch Courier", summoningSickness = true)
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("sacrificing it gives another creature haste") {
            val game = board()
            val courier = game.findPermanent("Torch Courier")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(courier, Keyword.HASTE) shouldBe true
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false

            val result = game.execute(
                ActivateAbility(game.player1Id, courier, abilityId, targets = listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.isInGraveyard(1, "Torch Courier") shouldBe true
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
        }

        test("it can't target itself") {
            val game = board()
            val courier = game.findPermanent("Torch Courier")!!

            game.execute(
                ActivateAbility(game.player1Id, courier, abilityId, targets = listOf(ChosenTarget.Permanent(courier)))
            ).error shouldNotBe null
            game.isOnBattlefield("Torch Courier") shouldBe true
        }
    }
}
