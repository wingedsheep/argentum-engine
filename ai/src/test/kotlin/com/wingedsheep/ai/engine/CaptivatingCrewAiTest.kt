package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.xln.cards.CaptivatingCrew
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class CaptivatingCrewAiTest : ScenarioTestBase() {
    init {
        test("theft followed by untap and haste is harmful to its original controller") {
            val ability = CaptivatingCrew.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(CaptivatingCrew, ability) shouldBe listOf(TargetPolarity.HARMFUL)
            AiReadingAudit.read(CaptivatingCrew).abilities.flatMap { it.findings }
                .filter { it.check == AiReadingAudit.Check.POLARITY && !it.read } shouldBe emptyList()
        }

        test("a gift before theft remains mixed even when an equal gift follows it") {
            val ability = CaptivatingCrew.script.activatedAbilities.single()
            val parts = (ability.effect as com.wingedsheep.sdk.scripting.effects.CompositeEffect).effects
            val untap = parts.filterIsInstance<com.wingedsheep.sdk.scripting.effects.TapUntapEffect>().single()
            val reversed = com.wingedsheep.sdk.scripting.effects.CompositeEffect(listOf(untap.copy()) + parts)
            TargetPolarityAnalyzer.forRequirements(reversed, ability.targetRequirements) shouldBe
                listOf(TargetPolarity.UNKNOWN)
        }

        test("the repeatable theft premium reaches the board evaluator but not a face-down card") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Captivating Crew").build()
            val id = game.findPermanent("Captivating Crew")!!
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            val intents = IntentCatalog.of(cardRegistry)
            val plain = BoardPresence.permanentValue(game.state, game.state.projectedState, id, component)
            BoardPresence.permanentValue(game.state, game.state.projectedState, id, component, intents) shouldBe plain + 1.5
            game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
            BoardPresence.permanentValue(game.state, game.state.projectedState, id, component, intents) shouldBe
                BoardPresence.permanentValue(game.state, game.state.projectedState, id, component)
        }

        test("target selection steals the opposing attacker rather than treating haste as a gift") {
            val game = scenario().withPlayers().withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Captivating Crew")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val source = game.findPermanent("Captivating Crew")!!
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == source }
            val filled = TargetSelection.fillHeuristically(
                game.state, action, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as ActivateAbility
            (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.findPermanent("Craw Wurm")!!
        }
    }
}
