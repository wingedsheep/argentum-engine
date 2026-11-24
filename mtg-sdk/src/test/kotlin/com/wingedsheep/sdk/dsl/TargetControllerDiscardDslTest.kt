package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Its controller discards a card" (Assassin's Strike): the hand gathered and the player choosing
 * from it are both the targeted permanent's controller — never the spell's caster.
 */
class TargetControllerDiscardDslTest : FunSpec({

    fun pipelineOf(target: EffectTarget): Pair<GatherCardsEffect, SelectFromCollectionEffect> {
        val steps = Effects.Discard(1, target).shouldBeInstanceOf<CompositeEffect>().effects
        return steps[0].shouldBeInstanceOf<GatherCardsEffect>() to steps[1].shouldBeInstanceOf<SelectFromCollectionEffect>()
    }

    test("EffectTarget.TargetController gathers and chooses as the target's controller") {
        val (gather, select) = pipelineOf(EffectTarget.TargetController)
        gather.source.shouldBeInstanceOf<CardSource.FromZone>().player shouldBe Player.ControllerOf("target")
        select.chooser shouldBe Chooser.ControllerOfTarget
    }

    test("Player.ControllerOf chooses as the target's controller, not the caster") {
        val (_, select) = pipelineOf(EffectTarget.PlayerRef(Player.ControllerOf("target creature")))
        select.chooser shouldBe Chooser.ControllerOfTarget
    }
})
