package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Readings the Hour of Devastation creature batch surfaced:
 *
 *  - **"Target creature can't block this turn"** (Earthshaker Khenra, Gilded Cerodon) takes a blocker
 *    out of the opponent's defence: [IntentTag.EVASION_GRANT], like the team-wide "can't block".
 *    "This creature can't block" on its own source (Ydwen Efreet's coin flip) is a drawback, not that.
 *  - **Casting a spell without paying its mana cost** (Wildfire Eternal, Omnispell Adept) reads
 *    [IntentTag.FREE_SPELL], and the draft heuristic prices it.
 *  - **Removing all -1/-1 counters from itself** (Ammit Eternal) is a counter shed on its own source,
 *    which needs no intent, as a single-counter removal from itself already did.
 *  - **"Lands you control don't untap during your next untap step"** (Oketra's Last Mercy, Bontu's
 *    Last Reckoning) binds its own caster: a price, which needs no intent. Skipping an opponent's
 *    untap (Mana Vapors) stays flagged.
 */
class CantBlockFreeSpellAndCounterShedReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")
    fun tags(name: String) = CardIntentAnalyzer.analyze(card(name)).tags
    fun flags(name: String) = AiReadingAudit.audit(card(name)).map { it.key }

    test("a target creature that can't block is evasion for our attack") {
        for (name in listOf("Earthshaker Khenra", "Gilded Cerodon")) withClue(name) {
            tags(name) shouldContain IntentTag.EVASION_GRANT
            flags(name).shouldBeEmpty()
            AiCardRating.of(card(name), listOf("HOU")).intent.unrecognised shouldBe false
        }
        withClue("Ydwen Efreet's 'it can't block this turn' binds itself: a drawback, not evasion") {
            tags("Ydwen Efreet") shouldNotContain IntentTag.EVASION_GRANT
        }
    }

    test("a spell cast without paying its mana cost reads as a free spell and is priced by the draft heuristic") {
        for (name in listOf("Wildfire Eternal", "Omnispell Adept")) withClue(name) {
            tags(name) shouldContain IntentTag.FREE_SPELL
            flags(name).shouldBeEmpty()
            AiCardRating.of(card(name), listOf("HOU", "GRN")).intent.unrecognised shouldBe false
        }
        withClue("Omnispell Adept's activated ability is priced for the free spell it casts, not zero") {
            LimitedCardRater.explain(card("Omnispell Adept")).heuristic
                .filter { it.part.startsWith("Activated") }.maxOf { it.value } shouldBeGreaterThan 0.0
        }
    }

    test("removing all -1/-1 counters from itself needs no intent") {
        flags("Ammit Eternal").shouldBeEmpty()
    }

    test("skipping your own lands' next untap is a price that needs no intent") {
        for (name in listOf("Oketra's Last Mercy", "Bontu's Last Reckoning", "Hazoret's Undying Fury")) withClue(name) {
            flags(name).shouldBeEmpty()
            AiCardRating.of(card(name), listOf("HOU")).intent.unrecognised shouldBe false
        }
        withClue("Mana Vapors skips an opponent's untap: a tapper's payoff, read as such") {
            flags("Mana Vapors").shouldBeEmpty()
            AiCardRating.of(card("Mana Vapors"), listOf("PCY")).intent.unrecognised shouldBe false
        }
    }
})
