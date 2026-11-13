package com.wingedsheep.ai.audit

import com.wingedsheep.ai.audit.AiReadingAudit.Check
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotBeEmpty

/**
 * An end-of-turn keyword riding on +1/+1 counters put on the same creature — Infuse with the
 * Elements' "put X +1/+1 counters on target creature … it gains trample until end of turn", Arbor
 * Armament's counter and reach — needs no hold of its own: the counters are a lasting pump worth
 * casting in any window (an instant-speed counter stays PUMP, never COMBAT_TRICK), and the cast's
 * simulation prices the keyword wherever it is cast. The timing reader reads the keyword with them.
 */
class KeywordBesideCountersTimingReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("an end-of-turn keyword beside +1/+1 counters on the same creature has a timing reading") {
        for (name in listOf("Infuse with the Elements", "Arbor Armament")) {
            withClue(name) {
                val timing = AiReadingAudit.read(card(name)).abilities.flatMap { it.findings }
                    .filter { it.check == Check.TIMING }
                timing.shouldNotBeEmpty()
                timing.filterNot { it.read }.shouldBeEmpty()
                val tags = CardIntentAnalyzer.analyze(card(name)).tags
                tags shouldContain IntentTag.PUMP
                tags shouldNotContain IntentTag.COMBAT_TRICK
            }
        }
    }
})
