package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.frf.cards.GrimContest
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Each of those creatures deals damage equal to its toughness to the other" — Grim Contest's
 * [com.wingedsheep.sdk.scripting.effects.MutualDamageEffect]. Read like a fight it isn't: the
 * opponent's creature is dealt damage (harmful), ours lends its toughness the way a fighter lends
 * its power (beneficial, so the simulation picks our best one rather than the removal ranking's
 * worst), and the card is removal — but not a fight, so it feeds no "whenever a creature fights" deck.
 */
class MutualDamageReadingAiTest : FunSpec({

    test("Grim Contest: our creature lends its toughness, theirs is dealt damage") {
        val spell = GrimContest.script.spellEffect!!
        TargetPolarityAnalyzer.forRequirements(spell, GrimContest.script.targetRequirements) shouldBe
            listOf(TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
    }

    test("Grim Contest reads as removal, not as a fight") {
        val tags = CardIntentAnalyzer.analyze(GrimContest).tags
        tags shouldContain IntentTag.REMOVAL
        withClue("no fought event happens, so no fight payoff is fed") { tags shouldNotContain IntentTag.FIGHT }
    }
})
