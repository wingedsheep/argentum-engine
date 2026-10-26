package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * A class-wide redirection shield names its protected class the way Oracle does — the controller
 * after the plural noun — and says how long it lasts.
 */
class RedirectNextDamageDescriptionTest : DescribeSpec({

    describe("pluralPermanentDescription") {
        it("puts the controller after the plural noun") {
            GameObjectFilter.Creature.youControl().pluralPermanentDescription shouldBe "creatures you control"
            GameObjectFilter.Creature.pluralPermanentDescription shouldBe "creatures"
        }
    }

    describe("RedirectNextDamageEffect") {
        it("describes an until-your-next-turn filter shield") {
            RedirectNextDamageEffect(
                protectedTargets = emptyList(),
                redirectTo = EffectTarget.Self,
                scope = RedirectScope.CONTINUOUS,
                protectedFilter = GameObjectFilter.Creature.youControl(),
                duration = Duration.UntilYourNextTurn,
            ).description shouldBe
                "All damage that would be dealt to creatures you control until your next turn is dealt to " +
                "${EffectTarget.Self.description} instead"
        }

        it("keeps \"this turn\" for the default duration") {
            RedirectNextDamageEffect(
                protectedTargets = emptyList(),
                redirectTo = EffectTarget.Controller,
                scope = RedirectScope.CONTINUOUS,
                protectedFilter = GameObjectFilter.Creature,
                optional = true,
            ).description shouldBe
                "All damage that would be dealt to creatures this turn may be dealt to " +
                "${EffectTarget.Controller.description} instead"
        }
    }
})
