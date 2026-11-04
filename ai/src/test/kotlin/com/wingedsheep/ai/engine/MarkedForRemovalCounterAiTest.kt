package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.TetzimocPrimalDeath
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A counter whose only meaning is "the card that placed it destroys its holder" (Tetzimoc, Primal
 * Death's prey counter) is harmful to its holder, and placing one is the targeting half of removal.
 *
 * Not yet read: the *value* of a prey counter on the board (so the AI marks before casting Tetzimoc,
 * rather than casting it bare) — a new evaluator term, split out as RIX gap G15.
 */
class MarkedForRemovalCounterAiTest : ScenarioTestBase() {
    init {
        test("the prey-counter ability is aimed at opponents' creatures and read as removal") {
            val ability = TetzimocPrimalDeath.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(TetzimocPrimalDeath, ability) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(ability.effect).tags shouldContain IntentTag.REMOVAL
            AiReadingAudit.read(TetzimocPrimalDeath).abilities.flatMap { it.findings }
                .filter { !it.read } shouldBe emptyList()
        }

        fun activation(withTheirCreature: Boolean): Pair<TestGame, com.wingedsheep.engine.legalactions.LegalAction> {
            val builder = scenario().withPlayers().withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(1, TetzimocPrimalDeath.name)
                .withCardOnBattlefield(1, "Serra Angel")
            if (withTheirCreature) builder.withCardOnBattlefield(2, "Grizzly Bears")
            val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val tetzimoc = game.state.getZone(game.player1Id, Zone.HAND)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == TetzimocPrimalDeath.name }
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == tetzimoc }
            return game to activate
        }

        test("with only our own creatures to mark, the AI never spends mana marking one") {
            val (game, activate) = activation(withTheirCreature = false)
            TargetSelection.aimsOnlyAtTheWrongSide(
                game.state, activate, game.player1Id, IntentCatalog.of(cardRegistry),
            ) shouldBe true
        }

        test("activated from hand, the prey counter goes on their creature, not our bigger one") {
            val (game, activate) = activation(withTheirCreature = true)
            TargetSelection.aimsOnlyAtTheWrongSide(
                game.state, activate, game.player1Id, IntentCatalog.of(cardRegistry),
            ) shouldBe false
            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as ActivateAbility

            val target = (filled.targets.single() as ChosenTarget.Permanent).entityId
            game.state.projectedState.getController(target) shouldBe game.player2Id
        }
    }
}
