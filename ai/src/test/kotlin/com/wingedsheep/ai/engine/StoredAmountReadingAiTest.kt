package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Readings the Kaladesh energy cards asked for:
 *
 * - a `ModifyStatsEffect` whose amount is a number the pipeline stored earlier, through arithmetic —
 *   Die Young's "-1/-1 for each {E} paid this way" is `-paid`, and `paid` (a `PayCountersEffect`)
 *   can't be negative, so the slot is HARMFUL and the spell is removal;
 * - a pipeline that gathers its chosen targets and moves them to a graveyard itself (Noxious
 *   Gearhulk's tracked destroy) is the destroy it is: HARMFUL on the target slot, and removal;
 * - `CanAttackDespiteDefenderThisTurnEffect` (Hightide Hermit) is a combat enhancement;
 * - an Aura's power-only cut (Aether Meltdown's -4/-0) is a combat enhancement.
 */
class StoredAmountReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    private fun unread(name: String) =
        AiReadingAudit.read(card(name)).abilities.flatMap { it.findings }.filterNot { it.read }

    init {
        test("Die Young's negated energy payment reads as a shrink on an opponent's creature") {
            val spell = card("Die Young").script
            val reqs = spell.targetRequirements
            TargetPolarityAnalyzer.forRequirements(spell.spellEffect, reqs) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(spell.spellEffect!!, reqs).tags shouldContain IntentTag.REMOVAL
            unread("Die Young").shouldBeEmpty()
        }

        test("Noxious Gearhulk's tracked destroy of its chosen target reads harmful") {
            val trigger = card("Noxious Gearhulk").script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(trigger.effect, listOfNotNull(trigger.targetRequirement)) shouldBe
                listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyze(card("Noxious Gearhulk")).tags shouldContain IntentTag.REMOVAL
            unread("Noxious Gearhulk").shouldBeEmpty()
        }

        test("an exile its delayed trigger returns is a blink, not removal (Phelia, Exuberant Shepherd)") {
            val trigger = card("Phelia, Exuberant Shepherd").script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(trigger.effect, listOfNotNull(trigger.targetRequirement)) shouldNotBe
                listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(trigger.effect, listOfNotNull(trigger.targetRequirement)).tags shouldNotContain
                IntentTag.EXILE_REMOVAL
        }

        test("a one-turn defender lift and a power-only Aura cut are combat enhancements") {
            val lift = card("Hightide Hermit").script.activatedAbilities.single()
            CardIntentAnalyzer.analyzeEffect(lift.effect).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
            unread("Hightide Hermit").shouldBeEmpty()
            CardIntentAnalyzer.analyze(card("Aether Meltdown")).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
        }

        test("the AI aims Noxious Gearhulk's destroy at the opponent's creature, not its own") {
            val game = scenario().withPlayers()
                .withTurnNumber(7)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInHand(1, "Noxious Gearhulk")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Savannah Lions")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Noxious Gearhulk").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            val picked = response.selectedTargets.values.flatten()
            withClue("Gearhulk's trigger picked $picked") {
                picked shouldBe listOf(game.findPermanent("Savannah Lions")!!)
            }
        }

        test("the AI aims Die Young at the opponent's creature, not its own") {
            val game = scenario().withPlayers()
                .withTurnNumber(5)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(1, "Die Young")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val theirs = game.state.getBattlefield().first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" &&
                    game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player2Id
            }
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? CastSpell)?.let { a -> game.state.getEntity(a.cardId)?.get<CardComponent>()?.name } == "Die Young" }
            // The heuristic fill has no simulation to lean on: only the slot's reading keeps it off our own Bears.
            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as CastSpell
            withClue("Die Young aimed at ${filled.targets}") {
                filled.targets shouldBe listOf(ChosenTarget.Permanent(theirs))
            }
            val choice = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $choice") {
                (choice as? CastSpell)?.targets shouldBe listOf(ChosenTarget.Permanent(theirs))
            }
        }
    }
}
