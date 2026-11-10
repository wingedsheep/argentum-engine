package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kor Sky Climber — {2}{W} 3/2 Kor Soldier Ally.
 * "{1}{W}: This creature gains flying until end of turn."
 */
class KorSkyClimberScenarioTest : ScenarioTestBase() {
    init {
        test("{1}{W} gives it flying") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kor Sky Climber")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val climber = game.findPermanent("Kor Sky Climber")!!
            val abilityId = cardRegistry.getCard("Kor Sky Climber")!!.script.activatedAbilities.single().id
            game.state.projectedState.hasKeyword(climber, Keyword.FLYING) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, climber, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(climber, Keyword.FLYING) shouldBe true
        }
    }
}
