package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Destroy all Auras attached to target land" (Street Sweeper): a pipeline that gathers what is
 * attached to a target (`CardSource.AttachedTo`) and destroys it. The land is untouched, but the
 * Auras on a permanent are almost always its controller's own, so the slot reads
 * [TargetPolarity.HARMFUL] and the ability reads as spot removal of those Auras — never a sweeper.
 *
 * The decision it changes: when Street Sweeper attacks, the AI aims its trigger at the opponent's
 * Wild Growth land rather than at a land of its own.
 */
class AttachedAuraRemovalPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("destroying the Auras on a target land is harmful to it, and is spot removal") {
            val sweeper = card("Street Sweeper")
            val trigger = sweeper.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements, source = sweeper) shouldBe
                listOf(TargetPolarity.HARMFUL)
            val tags = CardIntentAnalyzer.analyze(sweeper).tags
            tags shouldContain IntentTag.REMOVAL
            withClue("the Auras of one permanent are no wrath") { tags shouldNotContain IntentTag.SWEEPER }
            AiReadingAudit.read(sweeper).readFully shouldBe true
        }

        test("Street Sweeper's attack trigger strips the opponent's enchanted land") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withCardOnBattlefield(2, "Forest")
                .withCardAttachedTo(2, "Wild Growth", "Forest")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Street Sweeper")
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Street Sweeper" to 2))
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            val target = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten().single()

            withClue("the opponent's Forest carries the only Aura") {
                game.state.projectedState.getController(target) shouldBe game.player2Id
            }
        }
    }
}
