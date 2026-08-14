package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class MonumentToEnduranceScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        var guard = 0
        while (getPendingDecision() !is ChooseOptionDecision && guard++ < 20) resolveStack()
        return getPendingDecision().shouldNotBeNull() as ChooseOptionDecision
    }

    private fun TestGame.choose(decision: ChooseOptionDecision, mode: String) {
        val index = decision.options.indexOf(mode)
        check(index >= 0) { "Mode '$mode' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        test("discard triggers offer only modes not already chosen this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Monument to Endurance")
                .withCardInHand(1, "Disciple of Law")
                .withCardInHand(1, "Disciple of Law")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withLifeTotal(2, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cycleCard(1, "Disciple of Law").error shouldBe null
            val first = game.resolveToModeChoice()
            game.choose(first, "Each opponent loses 3 life")
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            game.cycleCard(1, "Disciple of Law").error shouldBe null
            val second = game.resolveToModeChoice()
            second.options shouldNotContain "Each opponent loses 3 life"
            game.choose(second, "Create a Treasure token")
            game.resolveStack()

            game.state.getBattlefield().count { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Treasure"
            } shouldBe 1
        }
    }
}
