package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sublime Archangel (M13 #36) — "Flying. Exalted. Other creatures you control have exalted."
 *
 * Ruling (2012-07-01): a lone attacker gets +1/+1 once per instance of exalted among permanents you
 * control — the Archangel's own plus one granted to each other creature.
 */
class SublimeArchangelScenarioTest : ScenarioTestBase() {

    private fun TestGame.advanceTo(step: Step) {
        var guard = 0
        while (state.step != step && guard++ < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
        }
        state.step shouldBe step
    }

    init {
        test("a lone attacker gets +1/+1 for every exalted instance you control") {
            val game = scenario()
                .withPlayers("Angel", "Defender")
                .withCardOnBattlefield(1, "Sublime Archangel")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            withClue("other creatures you control have exalted; the opponent's don't") {
                game.state.projectedState.hasKeyword(lions, Keyword.EXALTED) shouldBe true
                val theirBears = game.findPermanents("Grizzly Bears").single { game.state.projectedState.getController(it) == game.player2Id }
                game.state.projectedState.hasKeyword(theirBears, Keyword.EXALTED) shouldBe false
            }

            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
            withClue("Archangel, Bears and Lions each have one instance") { game.state.stack.size shouldBe 3 }
            val power = game.state.projectedState.getPower(lions)!!
            val toughness = game.state.projectedState.getToughness(lions)!!
            game.resolveStack()

            withClue("three +1/+1 pumps") {
                game.state.projectedState.getPower(lions) shouldBe power + 3
                game.state.projectedState.getToughness(lions) shouldBe toughness + 3
            }
        }

        test("two creatures attacking: no exalted triggers") {
            val game = scenario()
                .withPlayers("Angel", "Defender")
                .withCardOnBattlefield(1, "Sublime Archangel")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Sublime Archangel" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.state.stack.size shouldBe 0
        }
    }
}
