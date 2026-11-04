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
 * Combat enhancements that add no power or toughness — a granted first strike, deathtouch or haste,
 * a "must be blocked", and a base power and toughness that *replaces* the creature's stats
 * (Exuberant Wolfbear, Square Up) — read as [IntentTag.COMBAT_ENHANCEMENT], never as a pump a plan
 * may add on top. A base small enough to shut the creature off (Sorceress Queen's 0/2) stays a
 * neutralizer.
 *
 * The decision it changes: a flash creature whose arrival hands our attacker first strike pays off
 * before we attack, so [AmbushWindow] no longer holds it for the opponent's end step — where the
 * first strike would expire unspent.
 */
class CombatEnhancementReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    init {
        test("keyword grants, forced blocks and base-stat replacement read as combat enhancements, not pumps") {
            for (card in listOf("Viashino Bladescout", "Magitek Scythe", "Exuberant Wolfbear", "Square Up")) {
                withClue(card) { tags(card) shouldContain IntentTag.COMBAT_ENHANCEMENT }
            }
            withClue("Wolfbear replaces the Human's stats; it adds nothing a pump plan may stack") {
                tags("Exuberant Wolfbear") shouldNotContain IntentTag.PUMP
            }
            withClue("a 0/2 base takes away everything the creature does in combat") {
                tags("Sorceress Queen") shouldContain IntentTag.NEUTRALIZE
                tags("Sorceress Queen") shouldNotContain IntentTag.COMBAT_ENHANCEMENT
            }
        }

        test("the cards built on them read fully") {
            for (card in listOf("Exuberant Wolfbear", "Magitek Scythe", "Viashino Bladescout")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("a flash creature that hands our attacker first strike is not held past our attack") {
            val game = scenario().withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Viashino Bladescout")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .apply { repeat(4) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Forest") } }
                .build()
                .also { it.advanceToPriority(1, Step.PRECOMBAT_MAIN) }
            val bladescout = intents.forName("Viashino Bladescout") ?: error("Viashino Bladescout is not in the registry")
            for (readArrival in listOf(false, true)) {
                withClue("readArrival=$readArrival: first strike until end of turn is spent in this turn's fight") {
                    AmbushWindow.holds(game.state, game.player1Id, bladescout, readArrival = readArrival) shouldBe false
                }
            }
        }
    }
}
