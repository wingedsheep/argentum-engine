package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.AmbushWindow
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

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
 *
 * A replacing base P/T *until end of turn* on an instant (Phantasmal Form's "up to two target
 * creatures each have base power and toughness 3/3 and gain flying") is a combat trick: [HoldPolicy]
 * holds it for a fight instead of dumping it in the main phase. The flag "assigns combat damage equal
 * to its toughness rather than its power" (Plagon, Lord of the Beach) is a combat enhancement aimed
 * at our own creature.
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

        test("a base power and toughness until end of turn on an instant is a combat trick, not cast in the main phase") {
            // Square Up's single target, Phantasmal Form's two run through a ForEach over the targets.
            for (card in listOf("Square Up", "Phantasmal Form")) {
                withClue("$card: ${tags(card)}") { tags(card) shouldContain IntentTag.COMBAT_TRICK }
            }
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Phantasmal Form"))
            withClue("Phantasmal Form: $reading") { reading.readFully shouldBe true }

            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(1, "Phantasmal Form")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
            HoldPolicy(intents).verdictFor(game.state, game.player1Id, "Phantasmal Form")
                .shouldBeInstanceOf<TimingVerdict.NoWindow>()
        }

        // Phantasmal Form's 3/3 replaces a 2/2's base toughness: it carries Grizzly Bears out of
        // Shock's two damage but not out of Lightning Bolt's three. It adds no toughness, so only
        // the base reset ([CardIntent.setToughness]) earns the response window.
        for ((burn, saves) in listOf("Shock" to true, "Lightning Bolt" to false)) {
            test("a base 3/3 until end of turn ${if (saves) "answers" else "does not answer"} $burn on our 2/2") {
                val game = scenario().withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, burn)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Phantasmal Form")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
                game.castSpell(2, burn, game.findPermanent("Grizzly Bears"))
                val verdict = HoldPolicy(intents).verdictFor(game.state, game.player1Id, "Phantasmal Form")
                if (saves) verdict.shouldBeInstanceOf<TimingVerdict.Adjust>()
                else verdict.shouldBeInstanceOf<TimingVerdict.NoWindow>()
            }
        }

        test("assigning combat damage by toughness is a combat enhancement for our own creature") {
            withClue("Plagon: ${tags("Plagon, Lord of the Beach")}") {
                tags("Plagon, Lord of the Beach") shouldContain IntentTag.COMBAT_ENHANCEMENT
            }
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Plagon, Lord of the Beach"))
            withClue("Plagon: $reading") { reading.readFully shouldBe true }
        }
    }
}
