package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.fin.cards.AncientAdamantoise
import com.wingedsheep.mtg.sets.definitions.rtr.cards.AzoriusArrester
import com.wingedsheep.mtg.sets.definitions.rtr.cards.PalisadeGiant
import com.wingedsheep.sdk.scripting.effects.CantAttackEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Detain ("until your next turn, that creature can't attack or block and its activated abilities
 * can't be activated") is a super-tap of an opponent's permanent: its `CantAttackEffect` and the
 * `PreventActivatedAbilities` scoped to the holder itself, granted to the target, read as
 * [IntentTag.TAPPER] and aim the slot at the opponent's side. "All damage that would be dealt to
 * you and other permanents you control is dealt to this creature instead" (Palisade Giant, Ancient
 * Adamantoise) is a damage shield over our side — [IntentTag.PROTECTION].
 */
class DetainAndDamageRedirectReadingAiTest : FunSpec({

    fun leaves(effect: Effect): List<Effect> =
        if (effect is CompositeEffect) effect.effects.flatMap(::leaves) else listOf(effect)

    val detain = AzoriusArrester.script.triggeredAbilities.single()
    val requirements = listOfNotNull(detain.targetRequirement) + detain.additionalTargetRequirements

    test("detain reads as a tapper, keeping its can't-block evasion read") {
        val tags = CardIntentAnalyzer.analyze(AzoriusArrester).tags
        tags shouldContain IntentTag.TAPPER
        tags shouldContain IntentTag.EVASION_GRANT
    }

    test("detain's can't-attack half alone aims at the opponent's creature") {
        val cantAttack = leaves(detain.effect).filterIsInstance<CantAttackEffect>().single()
        TargetPolarityAnalyzer.forRequirements(cantAttack, requirements) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("detain's granted activation lock alone aims at the opponent's creature") {
        val lock = leaves(detain.effect).filterIsInstance<GrantStaticAbilityEffect>().single()
        TargetPolarityAnalyzer.forRequirements(lock, requirements) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("a redirect of our side's damage onto the creature itself reads as protection") {
        CardIntentAnalyzer.analyze(PalisadeGiant).tags shouldContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyze(AncientAdamantoise).tags shouldContain IntentTag.PROTECTION
    }
})
