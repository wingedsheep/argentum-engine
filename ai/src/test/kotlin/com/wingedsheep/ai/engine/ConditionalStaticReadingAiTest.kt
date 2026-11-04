package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * A static ability gated on a condition ([com.wingedsheep.sdk.scripting.ConditionalStaticAbility]) is
 * read by the ability inside it: ascend's "this creature has flying as long as you have the city's
 * blessing" is the same evasion an unconditional grant is, some of the time. Before, the intent
 * analyzer saw nothing ("abilities not recognised") and the draft heuristic priced Skymarcher Aspirant
 * as a vanilla 2/1.
 *
 * The heuristic prices a keyword the creature grants itself like a printed one, keeping the
 * conditional discount `LimitedCardRater` applies to any gated static. A creature's own conditional penalty (the
 * Invasion Djinn's "-2/-2 as long as …") is its drawback, never read as a pump.
 */
class ConditionalStaticReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")
    fun tags(name: String) = CardIntentAnalyzer.analyze(card(name)).tags
    fun term(name: String, part: String): Double =
        LimitedCardRater.explain(card(name)).heuristic.singleOrNull { it.part == part }?.value ?: 0.0

    test("a conditional static is read by the ability inside it") {
        tags("Skymarcher Aspirant") shouldContain IntentTag.EVASION_GRANT
        tags("Snubhorn Sentry") shouldContain IntentTag.PUMP
        tags("Spire Winder") shouldContain IntentTag.PUMP
        withClue("Ruham Djinn's conditional -2/-2 on itself is a drawback") {
            tags("Ruham Djinn") shouldNotContain IntentTag.PUMP
        }
        withClue("Ruham Djinn's own penalty is part of its body, not an unrecognised ability") {
            AiCardRating.of(card("Ruham Djinn"), listOf("INV")).intent.unrecognised shouldBe false
        }
    }

    test("a static granting only the creature itself a keyword is body, not an unrecognised ability") {
        withClue("Sun-Crested Pterodon: vigilance as long as you control another Dinosaur") {
            AiCardRating.of(card("Sun-Crested Pterodon"), listOf("RIX")).intent.unrecognised shouldBe false
        }
        withClue("Juggernaut's attack requirement and blocking restriction are no keyword on its body") {
            AiReadingAudit.read(card("Juggernaut")).abilities.none { it.body } shouldBe true
        }
    }

    test("a keyword the creature grants itself is priced like a printed one, discounted when conditional") {
        withClue("flying (0.8) the creature grants itself, gated on the city's blessing (× 0.6)") {
            term("Skymarcher Aspirant", "Static ability 1") shouldBe (0.48 plusOrMinus 1e-9)
        }
        withClue("vigilance (0.2) while you control another Dinosaur (× 0.6)") {
            term("Sun-Crested Pterodon", "Static ability 1") shouldBe (0.12 plusOrMinus 1e-9)
        }
    }
})
