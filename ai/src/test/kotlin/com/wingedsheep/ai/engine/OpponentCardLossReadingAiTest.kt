package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Shapes in which an opponent loses cards that no single leaf says:
 *
 * - "Target opponent exiles a creature they control" — a gather from that player's battlefield
 *   zone, exiled: the target slot is [TargetPolarity.HARMFUL], like the controlled-permanents edict.
 * - "Whenever this creature deals combat damage to a player, that player discards that many cards"
 *   — the trigger's player loses their hand: [IntentTag.DISCARD].
 * - "Each opponent loses 3 life unless that player sacrifices a nonland permanent or discards a
 *   card" — an unless-clause an opponent pays from hand: [IntentTag.DISCARD].
 */
class OpponentCardLossReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an opponent exiling a creature of their choice aims the opponent slot across the table") {
            val doomfall = card("Doomfall")
            withClue("'target opponent exiles a creature they control'") {
                TargetPolarityAnalyzer.forSpellModes(doomfall, listOf(0)) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("'you choose a nonland card from it; exile that card'") {
                TargetPolarityAnalyzer.forSpellModes(doomfall, listOf(1)) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("'that player discards that many cards' off a combat-damage trigger reads as discard") {
            CardIntentAnalyzer.analyze(card("Dreamstealer")).tags shouldContain IntentTag.DISCARD
        }

        test("an opponent's lose-life-unless-they-discard punisher reads as discard") {
            CardIntentAnalyzer.analyze(card("Torment of Hailfire")).tags shouldContain IntentTag.DISCARD
        }
    }
}
