package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target player creates a 1/1 … creature token" ([com.wingedsheep.sdk.scripting.effects.CreateTokenEffect]
 * with its `controller` on a target slot) hands that player a body, so the slot is
 * [TargetPolarity.BENEFICIAL] — the same reading a Treasure for a target player already had. Baffling
 * End's "target opponent creates a 3/3 Dinosaur" has no side to pick: it is the card's price and
 * reads [TargetPolarity.FLEXIBLE], so it neither prunes nor vetoes.
 *
 * Unread, Emeritus of Truce's enter trigger took the removal ranking, which puts an opponent first,
 * and handed them the flier.
 */
class TokenForTargetPlayerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun triggerPolarities(card: String, index: Int): List<TargetPolarity> {
        val trigger = cardRegistry.getCard(card)!!.script.triggeredAbilities[index]
        val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
        return intents.effectTargetPolarities(trigger.effect, reqs, card)
    }

    init {
        test("a token for a target player reads as a gift to that player") {
            triggerPolarities("Emeritus of Truce", 0) shouldBe listOf(TargetPolarity.BENEFICIAL)
            withClue("Baffling End: the Dinosaur for a target opponent is the card's price — read, but it prunes nothing") {
                triggerPolarities("Baffling End", 1) shouldBe listOf(TargetPolarity.FLEXIBLE)
            }
            withClue("Necromentia: a 'target opponent' spell must not read as a gift, which would veto casting it") {
                intents.spellTargetPolarities("Necromentia") shouldNotBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("Emeritus of Truce gives the Inkling to its own controller") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInHand(1, "Emeritus of Truce")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Emeritus of Truce").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            withClue("the token is a body on its controller's side: AI is ${game.player1Id}") {
                response.selectedTargets.values.flatten() shouldBe listOf(game.player1Id)
            }
        }
    }
}
