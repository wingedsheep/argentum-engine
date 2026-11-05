package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Two readings the Hour of Devastation batch surfaced:
 *
 *  - "{4}, {T}, Sacrifice this land: Return each creature card exiled with this land to the
 *    battlefield under its owner's control" (Endless Sands, and Safe Haven's upkeep sacrifice). The
 *    card only ever exiles "target creature you control", so the pile is our own stashed creatures
 *    and handing it back to its owners brings them back to us: recursion. A pile that can hold
 *    another player's cards (Lagrella, the Magpie) is still a release. The exile that fills the
 *    pile, aimed only at our own creature, is a stash — protection, not removal.
 *  - "All damage that would be dealt this turn to you and permanents you control is dealt to
 *    enchanted creature instead" (Saving Grace): a damage-redirection shield, read as protection.
 */
class OwnStashAndDamageRedirectReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    fun readsFully(name: String) =
        AiReadingAudit.read(card(name)).abilities.filter { it.checked }.all { it.readFully }

    test("returning a pile of our own stashed creatures under their owners' control is recursion") {
        for (name in listOf("Endless Sands", "Safe Haven")) {
            val intent = CardIntentAnalyzer.analyze(card(name))
            withClue("$name brings back the creatures it stashed") {
                intent.tags shouldContain IntentTag.RECURSION
                readsFully(name) shouldBe true
            }
        }
        withClue("Endless Sands' return is an activation the AI can cash in") {
            CardIntentAnalyzer.analyze(card("Endless Sands")).cashableTags shouldContain IntentTag.RECURSION
        }
        withClue("exiling our own creature with Endless Sands is a stash, not removal") {
            val tags = CardIntentAnalyzer.analyze(card("Endless Sands")).tags
            tags shouldContain IntentTag.PROTECTION
            tags shouldNotContain IntentTag.REMOVAL
            tags shouldNotContain IntentTag.EXILE_REMOVAL
        }
        withClue("the draft heuristic prices that stash as a blink, not as premium exile removal") {
            val stash = LimitedCardRater.explain(card("Endless Sands")).heuristic
                .single { it.part == "Activated ability 2" } // {2}, {T}: Exile target creature you control
            stash.value shouldBeLessThan 0.5
        }
        withClue("Lagrella exiles other players' creatures too: handing that pile back is a release") {
            CardIntentAnalyzer.analyze(card("Lagrella, the Magpie")).tags shouldNotContain IntentTag.RECURSION
        }
    }

    test("redirecting the damage aimed at our side to one creature is protection") {
        CardIntentAnalyzer.analyze(card("Saving Grace")).tags shouldContain IntentTag.PROTECTION
        withClue("Saving Grace's enters trigger now reads fully") { readsFully("Saving Grace") shouldBe true }
    }
})
