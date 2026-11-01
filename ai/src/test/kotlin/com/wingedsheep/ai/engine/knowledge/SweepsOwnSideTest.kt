package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * [CardIntentAnalyzer.sweepsOwnSide] — the deckbuilder's "does this wipe fight my own creatures?"
 * — and the per-player wipe shape ([IntentTag.SWEEPER] read through a `ForEach` over players) it
 * leans on, plus the Aura ask an "enchanted creature dies" payoff makes ([CardAsks.attachmentAsk]).
 */
class SweepsOwnSideTest : FunSpec({
    val catalog by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = catalog[name] ?: error("$name is not in the card catalog")

    test("a per-player 'choose one, destroy the rest' reads as a sweeper") {
        CardIntentAnalyzer.analyze(card("Unstable Glyphbridge")).tags shouldContain IntentTag.SWEEPER
        CardIntentAnalyzer.analyze(card("Winnowing")).tags shouldContain IntentTag.SWEEPER
    }

    test("symmetric wipes on a spell or a trigger hit their own side") {
        listOf("Wrath of God", "Pyroclasm", "Bane of the Living", "Unstable Glyphbridge", "Winnowing").forEach { name ->
            withClue(name) { CardIntentAnalyzer.sweepsOwnSide(card(name)) shouldBe true }
        }
    }

    test("one-sided wipes, symmetric activations and wipes of noncreatures do not") {
        listOf("Plague Wind", "Massacre Wurm", "Pestilence", "Global Ruin").forEach { name ->
            withClue(name) { CardIntentAnalyzer.sweepsOwnSide(card(name)) shouldBe false }
        }
    }

    test("'whenever an enchanted creature dies' asks for Auras, not for creatures dying") {
        val eidolon = SynergyAxes.of(card("Hateful Eidolon"))
        eidolon.rewards shouldNotContain SynergyAxis.DEATH
        withClue("asks were ${eidolon.asks}") { eidolon.asks.size shouldBe 1 }
        eidolon.isPayoff shouldBe true
    }
})
