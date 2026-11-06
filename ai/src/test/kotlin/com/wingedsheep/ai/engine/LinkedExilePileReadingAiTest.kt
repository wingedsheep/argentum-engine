package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "Exile the top cards of a library" into a pile linked to the permanent, beside the card's own
 * "you may cast spells from among cards exiled with this" ([GrantMayCastFromLinkedExile]): the
 * impulse draw split across two abilities. The pile it can cast from on later turns is card draw;
 * one cast for free on the turn the cards arrive (Maralen, Fae Ascendant) is a free spell.
 *
 * The decision it changes: the effect heuristic behind a draft pick now pays for the exile — before,
 * Theater of Horrors rated the same with or without the grant that makes its upkeep exile a card.
 */
class LinkedExilePileReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)
    private fun tags(name: String) = CardIntentAnalyzer.analyze(card(name)).tags

    init {
        test("an exile into a pile its card may cast from on later turns reads as card draw") {
            for (name in listOf("Theater of Horrors", "Rona, Disciple of Gix", "Colfenor's Plans")) {
                withClue("$name banks the exiled cards to cast later") { tags(name) shouldContain IntentTag.DRAW }
            }
        }

        test("a pile cast from for free only the turn it is filled reads as a free spell, not a draw") {
            withClue("Maralen casts one exiled card this turn without paying its cost") {
                tags("Maralen, Fae Ascendant") shouldContain IntentTag.FREE_SPELL
                tags("Maralen, Fae Ascendant") shouldNotContain IntentTag.DRAW
            }
        }

        test("the cards built on it read fully") {
            for (name in listOf("Maralen, Fae Ascendant", "Theater of Horrors", "Rona, Disciple of Gix", "Colfenor's Plans")) {
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.readFully shouldBe true }
            }
        }

        test("the draft heuristic pays for the exile the pile grant turns into a card") {
            val theater = card("Theater of Horrors")
            val withoutGrant = theater.copy(
                script = theater.script.copy(
                    staticAbilities = theater.script.staticAbilities.filterNot {
                        ((it as? ConditionalStaticAbility)?.ability ?: it) is GrantMayCastFromLinkedExile
                    },
                ),
            )
            withClue("the upkeep exile is a card a turn only because the enchantment lets us cast it") {
                LimitedCardRater.explain(theater).heuristicRating shouldBeGreaterThan
                    LimitedCardRater.explain(withoutGrant).heuristicRating
            }
        }
    }
}
