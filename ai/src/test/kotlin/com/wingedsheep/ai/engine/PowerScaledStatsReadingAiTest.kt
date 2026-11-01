package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A [com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect] scaled by a creature's power —
 * "double target creature's power" (Unleash Fury, Bulk Up), "+X/+X where X is this creature's
 * power" (Auriok Bladewarden), "-X/-X where X is its power" (Stalactite Stalker).
 *
 * A power can be negative, so the reader called the modifier's sign unknown, and the whole card
 * went unread: not a pump, not a combat trick, and its target slot fell back to the removal ranking,
 * which aims at the opponent's best creature. But CR 107.1b floors a calculation that yields a
 * negative result at zero, so "+X" for X a power is never a loss.
 */
class PowerScaledStatsReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.ours(name: String): EntityId =
        state.getBattlefield(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a power-scaled +X is a pump on a friendly target") {
            for (name in listOf("Unleash Fury", "Bulk Up")) {
                withClue("$name: +X/+0 with X its power reads as a pump on a friendly target") {
                    intents.forName(name)!!.tags shouldContain IntentTag.PUMP
                    intents.spellTargetPolarities(name) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
            }
        }

        test("Unleash Fury doubles our Craw Wurm, not their Hill Giant") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Unleash Fury")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { action ->
                    val spell = action.action as? CastSpell ?: return@first false
                    game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Unleash Fury"
                }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell

            withClue("double-its-power reads as a pump (CR 107.1b floors a negative X at zero): it goes on our creature") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.ours("Craw Wurm")
            }
        }
    }
}
