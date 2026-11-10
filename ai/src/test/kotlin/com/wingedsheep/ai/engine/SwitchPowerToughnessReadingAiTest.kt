package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.aer.cards.QuicksmithSpy
import com.wingedsheep.mtg.sets.definitions.lrw.cards.TurtleshellChangeling
import com.wingedsheep.mtg.sets.definitions.ogw.cards.WanderingFumarole
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import com.wingedsheep.sdk.scripting.effects.SwitchPowerToughnessEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Switch this creature's power and toughness until end of turn" reshapes a creature's stats for
 * this turn's fight: [IntentTag.COMBAT_ENHANCEMENT], spent while the creature can still fight.
 *
 * - Turtleshell Changeling prints it; Wandering Fumarole hands it to itself with its animation, as
 *   a granted `{0}` ability that only touches its holder's own stats.
 * - A lasting card-flow grant (Quicksmith Spy's "{T}: Draw a card") is not a fight trick.
 */
class SwitchPowerToughnessReadingAiTest : FunSpec({

    val fumaroleAnimation = WanderingFumarole.script.activatedAbilities.last().effect
    val fumaroleGrant = EffectWalker.leaves(fumaroleAnimation).filterIsInstance<GrantActivatedAbilityEffect>().single()

    test("a printed self-switch is a combat enhancement with an until-end-of-turn window") {
        val switch = TurtleshellChangeling.script.activatedAbilities
            .flatMap { EffectWalker.leaves(it.effect) }.filterIsInstance<SwitchPowerToughnessEffect>().single()
        CardIntentAnalyzer.analyzeEffect(switch).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
        ExpiringGrantWindow.hasExpiringReading(switch, creatureGrants = true, source = TurtleshellChangeling) shouldBe true
    }

    test("a manland granting itself a switch for the turn reads as a fight trick") {
        CardIntentAnalyzer.grantsOwnFightTrick(fumaroleGrant.ability) shouldBe true
        CardIntentAnalyzer.analyzeEffect(fumaroleGrant).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
        ExpiringGrantWindow.hasExpiringReading(fumaroleGrant, creatureGrants = true, source = WanderingFumarole) shouldBe true
    }

    test("a granted draw ability is not a fight trick") {
        val spyGrant = QuicksmithSpy.script.triggeredAbilities
            .flatMap { EffectWalker.leaves(it.effect) }.filterIsInstance<GrantActivatedAbilityEffect>().single()
        CardIntentAnalyzer.grantsOwnFightTrick(spyGrant.ability) shouldBe false
    }
})
