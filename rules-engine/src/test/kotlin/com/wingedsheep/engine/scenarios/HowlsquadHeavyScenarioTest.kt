package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class HowlsquadHeavyScenarioTest : ScenarioTestBase() {

    init {
        test("beginning of combat creates a hasty Goblin that must attack this combat") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Howlsquad Heavy", summoningSickness = false)
                .withCardOnBattlefield(1, "Goblin Surveyor", summoningSickness = false)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val surveyor = game.findPermanent("Goblin Surveyor")!!
            val battlefieldBeforeCombat = game.state.getBattlefield().toSet()
            StateProjector().project(game.state).hasKeyword(surveyor, Keyword.HASTE) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.resolveStack()

            val token = game.state.getBattlefield().single { it !in battlefieldBeforeCombat }
            game.state.getEntity(token)?.has<MustAttackThisTurnComponent>() shouldBe true
            StateProjector().project(game.state).hasKeyword(token, Keyword.HASTE) shouldBe true
        }
    }
}
