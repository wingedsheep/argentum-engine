package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Target player's life total becomes 1" (Vraska, Relic Seeker's −10): a total set low is a loss
 * for the player who holds it, so the slot is an opponent. Unread, the slot was a coin toss, and
 * the AI could set its own life to 1.
 */
class SetLifeTotalReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a low fixed life total is harmful to its target player") {
            val ultimate = cardRegistry.getCard("Vraska, Relic Seeker")!!.script.activatedAbilities.last()
            intents.effectTargetPolarities(ultimate.effect, ultimate.targetRequirements) shouldBe listOf(TargetPolarity.HARMFUL)
        }

        test("Vraska's −10 sets the opponent's life to 1, not ours") {
            val game = scenario().withPlayers()
                .withActivePlayer(1)
                .withLifeTotal(1, 20)
                .withLifeTotal(2, 20)
                .withCardOnBattlefield(1, "Vraska, Relic Seeker")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val vraska = game.findPermanent("Vraska, Relic Seeker")!!
            game.state = game.state.updateEntity(vraska) { container ->
                container.with(CountersComponent().withAdded(CounterType.LOYALTY, 10))
            }
            val ultimate = cardRegistry.getCard("Vraska, Relic Seeker")!!.script.activatedAbilities.last()
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.let { a -> a.sourceId == vraska && a.abilityId == ultimate.id } == true }

            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility

            withClue("life total becomes 1 is aimed at the opponent") {
                (filled.targets.single() as ChosenTarget.Player).playerId shouldBe game.player2Id
            }
        }
    }
}
