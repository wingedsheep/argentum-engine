package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain

/**
 * Statics that let their controller play on an opponent's turn: "you may cast creature spells as
 * though they had flash" (Prophet of Kruphix, Vedalken Orrery) and "untap all creatures and lands
 * you control during each other player's untap step" (Prophet of Kruphix, Seedborn Muse). An
 * untapped land is mana held up on their turn ([IntentTag.OFF_TURN_PLAY]); an untapped creature
 * attacks and still blocks, as vigilance does ([IntentTag.COMBAT_ENHANCEMENT]).
 */
class OffTurnPlayReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun tags(name: String) = CardIntentAnalyzer.analyze(byName[name] ?: error("'$name' is not in the corpus")).tags

    test("Prophet of Kruphix reads as off-turn play and a vigilance-like untap") {
        tags("Prophet of Kruphix") shouldContainAll listOf(IntentTag.OFF_TURN_PLAY, IntentTag.COMBAT_ENHANCEMENT)
    }

    test("a controller-only flash grant reads as off-turn play") {
        tags("Vedalken Orrery") shouldContain IntentTag.OFF_TURN_PLAY
    }

    test("an untap over creatures alone is combat, not mana on their turn") {
        val drumbellower = tags("Drumbellower")
        drumbellower shouldContain IntentTag.COMBAT_ENHANCEMENT
        withClue("no land or rock untaps") { drumbellower shouldNotContain IntentTag.OFF_TURN_PLAY }
    }
})
