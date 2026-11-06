package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.AmbushWindow
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Turning another permanent into a creature until end of turn — `BecomeCreatureEffect` /
 * `AnimateLandEffect` on a target rather than on the source — reads as
 * [IntentTag.COMBAT_ENHANCEMENT]: Nissa, Steward of Elements' −6 makes two lands 5/5 flying,
 * hasty attackers, Serpentine Ambush makes a creature a 5/5 for the fight. A manland animating
 * itself is just its body and gets no tag.
 *
 * The decision it changes: an instant animate is spent in this turn's fight, so [AmbushWindow] does
 * not hold it past our own attack.
 */
class AnimateReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    init {
        test("animating another permanent reads as a combat enhancement, not a pump") {
            for (card in listOf("Nissa, Steward of Elements", "Serpentine Ambush", "Sylvan Awakening")) {
                withClue(card) {
                    tags(card) shouldContain IntentTag.COMBAT_ENHANCEMENT
                    tags(card) shouldNotContain IntentTag.PUMP
                }
            }
        }

        test("the land-animating cards built on it read fully") {
            for (card in listOf("Nissa, Steward of Elements", "Sylvan Awakening")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("an instant that animates our attacker is not held past our attack") {
            val game = scenario().withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Serpentine Ambush")
                .withCardOnBattlefield(2, "Hill Giant")
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
                .build()
                .also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
            val ambush = intents.forName("Serpentine Ambush") ?: error("Serpentine Ambush is not in the registry")
            withClue("a 5/5 until end of turn is spent in this turn's fight") {
                AmbushWindow.holds(game.state, game.player1Id, ambush, readArrival = false) shouldBe false
            }
        }
    }
}
