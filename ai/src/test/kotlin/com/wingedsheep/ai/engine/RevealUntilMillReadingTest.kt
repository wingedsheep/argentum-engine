package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target player reveals cards from the top of their library until they reveal a land card, then
 * puts those cards into their graveyard" — a [com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect]
 * whose revealed cards are binned. A mill of unknown depth: [IntentTag.MILL] when the library can be
 * an opponent's (a target player, or each opponent visited), and a [TargetPolarity.HARMFUL] slot, as
 * the fixed-count "target player mills N" is. Off our own library, or with the revealed cards put
 * anywhere but a graveyard, it is no mill.
 *
 * Unread, Balustrade Spy's enter trigger had no side to pick and could bin its own controller's library.
 */
class RevealUntilMillReadingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun card(name: String) = cardRegistry.getCard(name)!!

    private fun triggerPolarities(name: String): List<TargetPolarity> {
        val trigger = card(name).script.triggeredAbilities.single()
        val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
        return intents.effectTargetPolarities(trigger.effect, reqs, name)
    }

    init {
        test("Balustrade Spy's enter trigger reads as a mill aimed at an opponent") {
            val trigger = card("Balustrade Spy").script.triggeredAbilities.single()
            CardIntentAnalyzer.analyzeEffect(trigger.effect).tags shouldContain IntentTag.MILL
            triggerPolarities("Balustrade Spy") shouldBe listOf(TargetPolarity.HARMFUL)
            val reading = AiReadingAudit.read(card("Balustrade Spy"))
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("each opponent revealing until a land and binning the pile is a mill too") {
            val trigger = card("Consuming Aberration").script.triggeredAbilities.single()
            CardIntentAnalyzer.analyzeEffect(trigger.effect).tags shouldContain IntentTag.MILL
        }

        test("our own reveal-until, or one whose cards are not binned, is no mill") {
            withClue("Old Stickfingers bins its controller's own revealed cards: self-mill fuel") {
                val trigger = card("Old Stickfingers").script.triggeredAbilities.single()
                CardIntentAnalyzer.analyzeEffect(trigger.effect).tags shouldNotContain IntentTag.MILL
            }
            withClue("Polymorph's controller shuffles the revealed cards back") {
                CardIntentAnalyzer.analyze(card("Polymorph")).tags shouldNotContain IntentTag.MILL
            }
            withClue("Chaos Wand reveals the target opponent's library into exile, not their graveyard") {
                val ability = card("Chaos Wand").script.activatedAbilities.single()
                CardIntentAnalyzer.analyzeEffect(ability.effect).tags shouldNotContain IntentTag.MILL
            }
        }

        test("Balustrade Spy targets the opponent, not its own controller") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(1, "Balustrade Spy")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Balustrade Spy").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            withClue("the mill is aimed at the opponent: AI is ${game.player1Id}") {
                response.selectedTargets.values.flatten() shouldBe listOf(game.player2Id)
            }
        }
    }
}
