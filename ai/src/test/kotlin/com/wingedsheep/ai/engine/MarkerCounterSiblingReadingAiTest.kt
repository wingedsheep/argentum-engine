package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.TriadOfFates
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A marker counter with no rule of its own (Triad of Fates' fate counter) means what the card's
 * other abilities do to a target that must bear one: beside "exile target creature that has a
 * fate counter on it", putting one on is the targeting half of that removal.
 */
class MarkerCounterSiblingReadingAiTest : ScenarioTestBase() {
    init {
        test("the fate-counter ability reads as removal aimed at the opponent's side") {
            val ability = TriadOfFates.script.activatedAbilities.first()
            TargetPolarityAnalyzer.forAbility(TriadOfFates, ability) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(ability.effect, ability.targetRequirements, source = TriadOfFates)
                .tags shouldContain IntentTag.REMOVAL
            AiReadingAudit.read(TriadOfFates).abilities.flatMap { it.findings }
                .filter { !it.read } shouldBe emptyList()
        }

        test("the AI puts the fate counter on the opponent's creature, not its own") {
            val game = scenario().withPlayers().withActivePlayer(1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, TriadOfFates.name)
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val triad = game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == TriadOfFates.name }
            val firstAbility = TriadOfFates.script.activatedAbilities.first().id
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.let { a -> a.sourceId == triad && a.abilityId == firstAbility } == true }

            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as ActivateAbility

            val target = (filled.targets.single() as ChosenTarget.Permanent).entityId
            game.state.projectedState.getController(target) shouldBe game.player2Id
        }
    }
}
