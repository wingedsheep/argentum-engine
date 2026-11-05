package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * An "Enchant player" Aura reaches the player it enchants through their actions ("enchanted
 * player can't activate abilities …") and the permanents they control ("creatures enchanted player
 * controls lose all abilities and have base power and toughness 1/1"). Both read as a curse, so
 * the enchant slot is [TargetPolarity.HARMFUL] — aimed across the table — and the shrink-and-strip
 * of their creatures is [IntentTag.NEUTRALIZE], like Lignify on a single host.
 *
 * Unread, the enchant slot was UNKNOWN for Overwhelming Splendor, so nothing told the AI which
 * player its eight-mana Curse belongs on.
 */
class EnchantedPlayerCurseReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("Overwhelming Splendor's enchant-player slot is a curse and its statics neutralize") {
            withClue("the enchanted player is the one hurt") {
                TargetPolarityAnalyzer.forSpell(card("Overwhelming Splendor")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("stripping and shrinking their creatures neutralizes them") {
                CardIntentAnalyzer.analyze(card("Overwhelming Splendor")).tags shouldContain IntentTag.NEUTRALIZE
            }
        }

        test("a Curse's upkeep punisher on the enchanted player is a curse, and its discard option reads as discard") {
            withClue("'that player loses 3 life unless they sacrifice or discard' hurts the enchanted player") {
                TargetPolarityAnalyzer.forSpell(card("Torment of Scarabs")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            CardIntentAnalyzer.analyze(card("Torment of Scarabs")).tags shouldContain IntentTag.DISCARD
        }

        test("a gift to the enchanted player is not read as aiming the Aura at our own side") {
            withClue("Tenuous Truce enchants an opponent though both players draw; BENEFICIAL would filter every cast") {
                TargetPolarityAnalyzer.forSpell(card("Tenuous Truce")) shouldNotBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("an enchanted-creature 'loses all abilities' Aura neutralizes its host") {
            CardIntentAnalyzer.analyze(card("Lignify")).tags shouldContain IntentTag.NEUTRALIZE
        }
    }
}
