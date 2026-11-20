package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target land becomes an Island in addition to its other types"
 * ([com.wingedsheep.sdk.scripting.effects.AddSubtypeEffect]) on a card whose own attack names
 * Islands — Sealock Monster's "can't attack unless defending player controls an Island": the type
 * is handed to the defending player's land to open that attack, so the slot is aimed at an
 * opponent's land ([TargetPolarity.HARMFUL]) and reads as an [IntentTag.EVASION_GRANT].
 *
 * The decision it changes: when Sealock Monster becomes monstrous against an opponent with no
 * Island, the AI turns one of the opponent's lands into an Island, not one of its own.
 */
class LandTypeAttackEnablerReadingAiTest : ScenarioTestBase() {

    private val sealock by lazy { cardRegistry.requireCard("Sealock Monster") }

    init {
        test("the land type the card's own attack names reads as aimed at the defending player") {
            val trigger = sealock.script.triggeredAbilities.single()
            val reqs = listOfNotNull(trigger.targetRequirement)
            TargetPolarityAnalyzer.forRequirements(trigger.effect, reqs, source = sealock) shouldBe
                listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyzeEffect(trigger.effect, reqs, source = sealock).tags shouldContain
                IntentTag.EVASION_GRANT
            val reading = AiReadingAudit.read(sealock)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("Sealock Monster's Island goes on the opponent's land") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Sealock Monster")
                .withLandsOnBattlefield(1, "Island", 7)
                .withLandsOnBattlefield(2, "Plains", 2)
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("Sealock Monster")!!, sealock.script.activatedAbilities[0].id),
            ).error shouldBe null
            while (game.getPendingDecision() == null && game.state.stack.isNotEmpty()) {
                game.execute(PassPriority(game.state.priorityPlayerId!!))
            }
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            val chosen = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten()
            val theirLands = game.state.getBattlefield().filter { game.state.getEntity(it)?.let { e ->
                e.get<ControllerComponent>()?.playerId == game.player2Id
            } == true }
            withClue("the Island is handed to the opponent's Plains, opening Sealock Monster's attack") {
                chosen.size shouldBe 1
                (chosen.single() in theirLands) shouldBe true
            }
        }
    }
}
