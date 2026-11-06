package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Shapes in which an opponent loses cards that no single leaf says:
 *
 * - "Target opponent exiles a creature they control" — a gather from that player's battlefield
 *   zone, exiled: the target slot is [TargetPolarity.HARMFUL], like the controlled-permanents edict.
 * - "Whenever this creature deals combat damage to a player, that player discards that many cards"
 *   — the trigger's player loses their hand: [IntentTag.DISCARD].
 * - "Choose an artifact card name. Search target opponent's graveyard, hand, and library for any
 *   number of cards with the chosen name and exile them" — a several-zone search reaching the
 *   hand: the slot is [TargetPolarity.HARMFUL], the spell reads as [IntentTag.DISCARD], and the
 *   effect heuristic prices that reading.
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
            withClue("the triggering player pays: Painful Quandary") {
                CardIntentAnalyzer.analyze(card("Painful Quandary")).tags shouldContain IntentTag.DISCARD
            }
        }

        test("a named search of a player's graveyard, hand and library reads as hand disruption") {
            val dispossess = card("Dispossess")
            withClue("'search target opponent's graveyard, hand, and library … exile them'") {
                TargetPolarityAnalyzer.forSpell(dispossess) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            CardIntentAnalyzer.analyze(dispossess).tags shouldContain IntentTag.DISCARD
            withClue("target player, not opponent: Cranial Extraction") {
                TargetPolarityAnalyzer.forSpell(card("Cranial Extraction")) shouldBe listOf(TargetPolarity.HARMFUL)
                CardIntentAnalyzer.analyze(card("Cranial Extraction")).tags shouldContain IntentTag.DISCARD
            }
            withClue("the heuristic prices the discard reading above a bare rare sorcery's base") {
                // Renamed so the AKH pick table's own rating doesn't stand in for the heuristic.
                LimitedCardRater.rate(dispossess.copy(name = "Dispossess (unrated copy)")) shouldBeGreaterThan 2.5
            }
        }
    }
}
