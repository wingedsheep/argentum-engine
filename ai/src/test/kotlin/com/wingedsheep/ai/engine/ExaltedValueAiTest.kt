package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Exalted (CR 702.83) — "whenever a creature you control attacks alone, that creature gets +1/+1
 * until end of turn" — is worth something on any permanent that carries it, once per instance:
 * Cathedral of War is a land that pumps the lone attacker, and is the land worth destroying.
 */
class ExaltedValueAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("land destruction aims at the exalted land over a basic") {
            val game = scenario().withPlayers()
                .withActivePlayer(1)
                .withCardInHand(1, "Stone Rain")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Plains")
                .withCardOnBattlefield(2, "Cathedral of War")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Plains")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cathedral = game.findPermanent("Cathedral of War")!!
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? CastSpell)?.let { c -> game.state.getEntity(c.cardId)?.get<CardComponent>()?.name == "Stone Rain" } == true }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true, intents = intents,
            ) as CastSpell

            withClue("Stone Rain destroys Cathedral of War, not the Plains") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe cathedral
            }
        }

        test("each exalted counter is another instance on top of the printed one") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Cathedral of War").build()
            val id = game.findPermanent("Cathedral of War")!!
            val card = game.state.getEntity(id)!!.get<CardComponent>()!!
            val printed = BoardPresence.permanentValue(game.state, game.state.projectedState, id, card, intents)
            game.state = game.state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.EXALTED, 2)) }
            BoardPresence.permanentValue(game.state, game.state.projectedState, id, card, intents) shouldBe
                (printed + 2 * BoardPresence.EXALTED_INSTANCE_VALUE plusOrMinus 1e-9)
        }

        test("printed exalted reads as a combat enhancement on a creature, a land and an enchantment") {
            for (name in listOf("Guardians of Akrasa", "Cathedral of War", "Angelic Benediction")) {
                withClue(name) {
                    CardIntentAnalyzer.analyze(cardRegistry.requireCard(name)).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
                }
            }
        }
    }
}
