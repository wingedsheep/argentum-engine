package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Relentless Hunter — {1}{R}{G} 3/3 Human Warrior.
 * "{1}{R}{G}: This creature gets +1/+1 and gains trample until end of turn."
 */
class RelentlessHunterScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Relentless Hunter")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Relentless Hunter")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("one activation makes it a 4/4 with trample") {
            val game = board()
            val hunter = game.findPermanent("Relentless Hunter")!!
            game.state.projectedState.hasKeyword(hunter, Keyword.TRAMPLE) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, hunter, pumpId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(hunter) shouldBe 4
            projected.getToughness(hunter) shouldBe 4
            projected.hasKeyword(hunter, Keyword.TRAMPLE) shouldBe true
        }

        test("the bonus stacks across activations") {
            val game = board()
            val hunter = game.findPermanent("Relentless Hunter")!!

            repeat(2) {
                game.execute(ActivateAbility(game.player1Id, hunter, pumpId)).error shouldBe null
                game.resolveStack()
            }

            game.state.projectedState.getPower(hunter) shouldBe 5
            game.state.projectedState.getToughness(hunter) shouldBe 5
        }
    }
}
