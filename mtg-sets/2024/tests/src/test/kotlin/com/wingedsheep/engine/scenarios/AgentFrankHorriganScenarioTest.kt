package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Agent Frank Horrigan (PIP #89): indestructible from the moment it's declared as an attacker
 * through the rest of the turn, and "enters or attacks" proliferates twice.
 */
class AgentFrankHorriganScenarioTest : ScenarioTestBase() {
    init {
        test("has indestructible only once it has attacked, and keeps it for the rest of the turn") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Agent Frank Horrigan", tapped = false, summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val frank = game.findPermanent("Agent Frank Horrigan")!!
            withClue("not indestructible before attacking") {
                game.state.projectedState.hasKeyword(frank, Keyword.INDESTRUCTIBLE) shouldBe false
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Agent Frank Horrigan" to 2)).error shouldBe null
            withClue("indestructible as soon as it is declared as an attacker") {
                game.state.projectedState.hasKeyword(frank, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("still indestructible after combat this turn") {
                game.state.projectedState.hasKeyword(frank, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }

        test("entering proliferates twice") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Agent Frank Horrigan")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }

            game.castSpell(1, "Agent Frank Horrigan").error shouldBe null
            game.resolveStack()

            withClue("first proliferate prompts") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(bears))
            withClue("second proliferate prompts separately") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(bears))
            if (!game.hasPendingDecision() && game.state.stack.isNotEmpty()) game.resolveStack()

            game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }
}
