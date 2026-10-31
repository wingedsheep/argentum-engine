package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Voracious Hydra (M20 #200) — {X}{G}{G} Creature — Hydra 0/1, trample.
 *
 *   This creature enters with X +1/+1 counters on it.
 *   When this creature enters, choose one —
 *   • Double the number of +1/+1 counters on this creature.
 *   • This creature fights target creature you don't control.
 *
 * The X counters arrive through an entry replacement, so the ETB modal sees them: doubling turns
 * X = 3 into six counters, and the fight mode uses the X-sized power.
 */
class VoraciousHydraScenarioTest : ScenarioTestBase() {

    init {
        context("Voracious Hydra ETB modal") {

            test("doubling mode doubles the X +1/+1 counters it entered with") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Voracious Hydra")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Voracious Hydra", xValue = 3).error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB modal; got ${game.getPendingDecision()}")
                val doubleIndex = modeDecision.options.indexOfFirst { it.contains("Double", ignoreCase = true) }
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = doubleIndex))
                game.resolveStack()

                val hydra = game.findPermanent("Voracious Hydra")!!
                withClue("three counters from X, doubled to six") {
                    game.state.getEntity(hydra)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 6
                    game.state.projectedState.getPower(hydra) shouldBe 6
                    game.state.projectedState.getToughness(hydra) shouldBe 7
                }
                withClue("the opponent's creature is untouched") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("fight mode fights a creature you don't control with its X-sized power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Voracious Hydra")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castXSpell(1, "Voracious Hydra", xValue = 3).error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB modal; got ${game.getPendingDecision()}")
                val fightIndex = modeDecision.options.indexOfFirst { it.contains("fights", ignoreCase = true) }
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = fightIndex))
                if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the 3/4 Hydra kills the Bears") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                val hydra = game.findPermanent("Voracious Hydra")!!
                withClue("the Hydra keeps its three counters (no doubling) and survives") {
                    game.state.getEntity(hydra)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                }
            }
        }
    }
}
