package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.aer.cards.HinterlandDrake
import com.wingedsheep.mtg.sets.definitions.aer.cards.IceOver
import com.wingedsheep.mtg.sets.definitions.mrd.cards.VulshokGauntlets
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Two statics the intent reader left "not recognised":
 * - `CanOnlyBlockCreaturesWith` on its own body ("can't block artifact creatures", "can block only
 *   creatures with flying") — a combat restriction, as "can't block" is;
 * - an Aura's `GrantKeyword(DOESNT_UNTAP)` ("enchanted permanent doesn't untap") — the standing
 *   form of the tapper's lock. The same grant on an Equipment is its wearer's price.
 */
class BlockingAndUntapLockStaticReadingAiTest : FunSpec({

    test("a self block restriction reads as a combat restriction") {
        CardIntentAnalyzer.analyze(HinterlandDrake).tags shouldContain IntentTag.COMBAT_RESTRICTION
    }

    test("an Aura's doesn't-untap lock reads as a tapper") {
        CardIntentAnalyzer.analyze(IceOver).tags shouldContain IntentTag.TAPPER
    }

    test("an Equipment's doesn't-untap price is no tapper") {
        CardIntentAnalyzer.analyze(VulshokGauntlets).tags shouldNotContain IntentTag.TAPPER
    }
})
