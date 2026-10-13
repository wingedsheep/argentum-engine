package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SplitSpellTargetPolarityTest : FunSpec({
    val split = card("Test Split Polarity") {
        layout = CardLayout.SPLIT; keywords(Keyword.FUSE)
        face("Polarity Harm") {
            manaCost = "{R}"; typeLine = "Instant"
            spell { val creature = target(TargetFilter.Creature); effect = Effects.DealDamage(2, creature) }
        }
        face("Polarity Benefit") {
            manaCost = "{W}"; typeLine = "Instant"
            spell { val creature = target(TargetFilter.Creature); effect = Effects.ModifyStats(2, 2, creature) }
        }
    }
    val intents = IntentCatalog.of(CardRegistry().apply { register(split) })
    test("each half reads its own target direction and the cache keeps the readings separate") {
        intents.spellTargetPolarities(split.name, faceIndex = 0) shouldBe listOf(TargetPolarity.HARMFUL)
        intents.spellTargetPolarities(split.name, faceIndex = 1) shouldBe listOf(TargetPolarity.BENEFICIAL)
        intents.spellTargetPolarities(split.name, faceIndex = 0) shouldBe listOf(TargetPolarity.HARMFUL)
    }
    test("fuse combines target directions in printed half order") {
        intents.spellTargetPolarities(split.name, castFused = true) shouldBe listOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL)
    }
})
