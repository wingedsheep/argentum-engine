package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Affa Protector — {2}{W} 1/4 Human Soldier Ally. "Vigilance"
 */
class AffaProtectorScenarioTest : ScenarioTestBase() {
    init {
        test("vigilance: it attacks without tapping") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Affa Protector")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val affa = game.findPermanent("Affa Protector")!!

            game.state.projectedState.hasKeyword(affa, Keyword.VIGILANCE) shouldBe true
            game.state.projectedState.getPower(affa) shouldBe 1
            game.state.projectedState.getToughness(affa) shouldBe 4

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Affa Protector" to 2)).error shouldBe null

            withClue("a vigilant attacker stays untapped") {
                game.state.getEntity(affa)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
