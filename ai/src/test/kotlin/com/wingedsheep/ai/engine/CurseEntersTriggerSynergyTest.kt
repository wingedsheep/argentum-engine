package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.akh.cards.TrespassersCurse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * A Curse whose enters trigger watches the *enchanted* player's creatures ("whenever a creature
 * enchanted player controls enters", Trespasser's Curse) is aimed at an opponent: the deck's own
 * creatures entering pay it off no more than an opponent-scoped trigger does, so it must not read
 * as a CREATURES_ENTER payoff that the drafter would pair with its own ETB creatures.
 */
class CurseEntersTriggerSynergyTest : FunSpec({

    test("an enchanted-player enters trigger rewards no creatures-enter axis") {
        val profile = SynergyAxes.of(TrespassersCurse)
        profile.rewards shouldNotContain SynergyAxis.CREATURES_ENTER
        profile.feeds shouldContain SynergyAxis.LIFEGAIN
    }
})
