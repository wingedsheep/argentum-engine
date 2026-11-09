package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.c21.cards.Inkshield
import com.wingedsheep.mtg.sets.definitions.ktk.cards.DeflectingPalm
import com.wingedsheep.mtg.sets.definitions.tdm.cards.NewWayForward
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * What a prevention shield's reaction ([com.wingedsheep.sdk.scripting.effects.PreventDamageEffect.onPrevented])
 * makes of the card: Inkshield's "for each 1 damage prevented this way, create a 2/1 Inkling" is
 * protection that also makes tokens, so it reads as a token maker and feeds the token axes; a shield
 * that merely reflects damage (Deflecting Palm) does not become burn.
 */
class PreventionReactionReadingAiTest : FunSpec({

    test("a shield whose reaction makes tokens reads as protection and a token maker") {
        val tags = CardIntentAnalyzer.analyzeEffect(Inkshield.script.spellEffect!!).tags
        tags shouldContain IntentTag.PROTECTION
        tags shouldContain IntentTag.TOKEN_MAKER
    }

    test("a shield whose reaction makes tokens feeds the token axes") {
        val feeds = SynergyAxes.of(Inkshield).feeds
        feeds shouldContain SynergyAxis.TOKENS
        feeds shouldContain SynergyAxis.CREATURES_ENTER
    }

    test("a shield that gains life reads as life gain") {
        val shield = Effects.PreventDamage(combatOnly = true, alsoToYou = true, onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage()))
        CardIntentAnalyzer.analyzeEffect(shield).tags shouldContain IntentTag.LIFEGAIN
    }

    test("a reflecting shield stays protection, not burn") {
        val tags = CardIntentAnalyzer.analyzeEffect(DeflectingPalm.script.spellEffect!!).tags
        tags shouldContain IntentTag.PROTECTION
        tags shouldNotContain IntentTag.DAMAGE_TO_PLAYERS
    }

    test("a shield whose reaction draws is not read as a cantrip") {
        // New Way Forward draws only when it prevents damage; a DRAW reading would earn it the
        // opponent's-end-step cantrip window, where nothing is dealing damage.
        val tags = CardIntentAnalyzer.analyzeEffect(NewWayForward.script.spellEffect!!).tags
        tags shouldContain IntentTag.PROTECTION
        tags shouldNotContain IntentTag.DRAW
    }
})
