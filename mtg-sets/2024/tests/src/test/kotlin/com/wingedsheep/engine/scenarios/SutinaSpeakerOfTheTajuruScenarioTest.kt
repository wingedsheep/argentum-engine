package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sutina, Speaker of the Tajuru (J25 #56):
 *   Whenever Sutina attacks, you may return a land you control to its owner's hand. When you do,
 *   put a +1/+1 counter on target creature.
 *
 * The land is chosen (not targeted) as the attack trigger resolves, and the counter ability is a
 * reflexive trigger that only goes on the stack — choosing its target then — once a land actually
 * returned. Declining the "may" returns nothing and puts no counter anywhere.
 */
class SutinaSpeakerOfTheTajuruScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Sutina, Speaker of the Tajuru")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Sutina, Speaker of the Tajuru") {

            test("returning a land puts a +1/+1 counter on the chosen target creature") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findPermanents("Forest").first()

                game.declareAttackers(mapOf("Sutina, Speaker of the Tajuru" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger offers the optional land return") {
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null

                withClue("the land is chosen, not targeted") {
                    (game.state.pendingDecision is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf(forest)).error shouldBe null

                withClue("the reflexive counter ability chooses its target only after the land went back") {
                    (game.state.pendingDecision is ChooseTargetsDecision) shouldBe true
                    game.state.getZone(game.player1Id, Zone.HAND).contains(forest) shouldBe true
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.plusOneCounters("Grizzly Bears") shouldBe 1
                game.plusOneCounters("Sutina, Speaker of the Tajuru") shouldBe 0
                game.findPermanents("Forest").size shouldBe 1
            }

            test("declining returns no land and puts no counter") {
                val game = setup()

                game.declareAttackers(mapOf("Sutina, Speaker of the Tajuru" to 2)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findPermanents("Forest").size shouldBe 2
                game.plusOneCounters("Grizzly Bears") shouldBe 0
                game.plusOneCounters("Sutina, Speaker of the Tajuru") shouldBe 0
            }
        }
    }
}
