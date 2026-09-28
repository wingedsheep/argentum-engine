package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.BloatedProcessor
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bloated Processor — "Sacrifice another Phyrexian: Put a +1/+1 counter on this creature.
 * When this creature dies, incubate X, where X is its power."
 */
class BloatedProcessorScenarioTest : ScenarioTestBase() {

    private val growAbility = BloatedProcessor.activatedAbilities.single().id

    private fun TestGame.incubatorCounts(): List<Int> =
        state.getBattlefield(player1Id)
            .filter { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Incubator" }
            .map { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 }

    init {
        context("Bloated Processor") {
            test("dying incubates for its power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloated Processor")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val processor = game.findPermanent("Bloated Processor")!!
                game.castSpell(2, "Lightning Bolt", processor).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Bloated Processor") shouldBe true
                game.incubatorCounts() shouldBe listOf(3)
            }

            test("sacrificing another Phyrexian grows it, and death incubates for the new power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloated Processor", summoningSickness = false)
                    .withCardOnBattlefield(1, "Infected Defector")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val processor = game.findPermanent("Bloated Processor")!!
                game.execute(ActivateAbility(game.player1Id, processor, growAbility)).error shouldBe null
                var guard = 0
                while (guard++ < 10) {
                    val d = game.getPendingDecision()
                    if (d is SelectCardsDecision) game.selectCards(d.options.take(d.minSelections))
                    else if (game.state.stack.isNotEmpty()) game.resolveStack()
                    else break
                }
                withClue("Defector sacrificed") { game.isInGraveyard(1, "Infected Defector") shouldBe true }
                game.state.getEntity(processor)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.incubatorCounts() shouldBe listOf(3)
            }
        }
    }
}
