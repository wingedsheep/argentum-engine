package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * A static that makes its controller's own spells cost more — Geist-Fueled Scarecrow's "creature
 * spells you cast cost {1} more", the Invasion Leeches' "white spells you cast cost {W} more".
 * Unread, each was "abilities not recognised" and the draft heuristic priced the Scarecrow as a
 * vanilla 4/4 for four. It is the price of the body, so it reads as OWN_SPELL_TAX — never the
 * opponent-facing SPELL_TAX nor a COST_REDUCER, either of which would count the tax as a plus.
 */
class OwnSpellTaxReadingAiTest : ScenarioTestBase() {

    private fun registered(name: String) = cardRegistry.getCard(name)!!
    private fun tags(name: String) = CardIntentAnalyzer.analyze(registered(name)).tags

    init {
        test("a tax on its controller's own spells reads as a drawback, not disruption or a discount") {
            listOf("Geist-Fueled Scarecrow", "Alabaster Leech", "Derelor").forEach { name ->
                withClue(name) {
                    tags(name) shouldContain IntentTag.OWN_SPELL_TAX
                    tags(name) shouldNotContain IntentTag.SPELL_TAX
                    tags(name) shouldNotContain IntentTag.COST_REDUCER
                }
            }
            withClue("Thalia, the Survivor taxes the opponent") {
                tags("Thalia, the Survivor") shouldNotContain IntentTag.OWN_SPELL_TAX
            }
        }

        test("the drawback is not aimed at the opponent and adds no worth on the battlefield") {
            val intent = CardIntentAnalyzer.analyze(registered("Geist-Fueled Scarecrow"))
            intent.affectsOpponent shouldBe false
            intent.staticPriorValue shouldBe CardIntent.UNKNOWN.staticPriorValue
            withClue("a drawback earns no \"static effect\" rung on the prior ladder") {
                CardIntentAnalyzer.priorLadder(intent).map { it.claim } shouldNotContain "static effect"
            }
        }

        test("the draft heuristic picks a vanilla 4/4 for four over the Scarecrow") {
            val vanilla = card("Plain Scarecrow") {
                manaCost = "{4}"
                typeLine = "Artifact Creature — Scarecrow"
                power = 4
                toughness = 4
                metadata { rarity = Rarity.UNCOMMON }
            }
            val scarecrow = LimitedCardRater.explain(registered("Geist-Fueled Scarecrow"))
            val plain = LimitedCardRater.explain(vanilla)
            withClue("terms: ${scarecrow.heuristic}") {
                scarecrow.heuristic.filter { it.part.startsWith("Static") }.sumOf { it.value } shouldBeLessThan 0.0
                scarecrow.heuristicRating shouldBeLessThan plain.heuristicRating
            }
        }
    }
}
