package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Removing the creatures blocking or blocked by the source ([com.wingedsheep.sdk.scripting.effects.CardSource.LastKnownCombatPairedWithSource])
 * reads as spot removal — the opponent's creatures in the fight — never as a sweeper.
 */
class CombatPairedRemovalTest : FunSpec({
    val catalog by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = catalog[name] ?: error("$name is not in the card catalog")

    test("destroying or tucking the source's combat pairing reads as removal, not a sweeper") {
        listOf("Abu Ja'far", "Vortex Elemental").forEach { name ->
            withClue(name) {
                val tags = CardIntentAnalyzer.analyze(card(name)).tags
                tags shouldContain IntentTag.REMOVAL
                tags shouldNotContain IntentTag.SWEEPER
            }
        }
    }
})
