package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glissa, Herald of Predation (MOM #226) — modal beginning-of-combat trigger on your turn.
 */
class GlissaHeraldOfPredationScenarioTest : ScenarioTestBase() {

    private fun TestGame.toCombatAndChoose(mode: Int) {
        var guard = 0
        while (state.pendingDecision == null && state.step != Step.BEGIN_COMBAT && guard++ < 10) passPriority()
        guard = 0
        while (state.pendingDecision !is ChooseOptionDecision && guard++ < 5) passPriority()
        val decision = state.pendingDecision as? ChooseOptionDecision
            ?: error("expected mode choice, got ${state.pendingDecision}")
        submitDecision(OptionChosenResponse(decision.id, optionIndex = mode)).error shouldBe null
        resolveStack()
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Glissa, Herald of Predation")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Glissa, Herald of Predation") {
            test("mode 1 incubates 2 twice — two Incubator tokens with two +1/+1 counters each") {
                val game = board().build()
                game.toCombatAndChoose(0)

                val incubators = game.findAllPermanents("Incubator")
                incubators shouldHaveSize 2
                incubators.forEach {
                    game.state.getEntity(it)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("mode 2 transforms every Incubator token you control") {
                val game = board()
                    .withCardInHand(1, "Progenitor Exarch")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .build()
                game.castXSpell(1, "Progenitor Exarch", xValue = 2).error shouldBe null
                game.resolveStack()
                game.findAllPermanents("Incubator") shouldHaveSize 2

                game.toCombatAndChoose(1)

                game.findAllPermanents("Incubator") shouldHaveSize 0
                val phyrexians = game.findAllPermanents("Phyrexian")
                phyrexians shouldHaveSize 2
                phyrexians.forEach { game.state.projectedState.getPower(it) shouldBe 3 }
            }

            test("mode 3 gives Phyrexians you control first strike and deathtouch, but not other creatures") {
                val game = board().build()
                game.toCombatAndChoose(2)

                val glissa = game.findPermanent("Glissa, Herald of Predation")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.hasKeyword(glissa, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(glissa, Keyword.DEATHTOUCH) shouldBe true
                projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
            }

            test("does not trigger at the beginning of combat on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Glissa, Herald of Predation")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                var guard = 0
                while (game.state.step != Step.BEGIN_COMBAT && guard++ < 10) game.passPriority()
                game.state.step shouldBe Step.BEGIN_COMBAT
                (game.state.pendingDecision is ChooseOptionDecision) shouldBe false
                game.state.stack.size shouldBe 0
                game.findPermanent("Glissa, Herald of Predation") shouldNotBe null
            }
        }
    }
}
