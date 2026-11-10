package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CreateTokenEffectDescriptionTest : FunSpec({

    test("a token that enters tapped says so (Drana's Chosen, Null Caller)") {
        CreateTokenEffect(
            power = 2, toughness = 2, colors = setOf(Color.BLACK), creatureTypes = setOf("Zombie"), tapped = true,
        ).description shouldBe "Create a tapped 2/2 black Zombie creature token"
    }

    test("an untapped token keeps the plain wording") {
        CreateTokenEffect(
            power = 2, toughness = 2, colors = setOf(Color.BLACK), creatureTypes = setOf("Zombie"),
        ).description shouldBe "Create a 2/2 black Zombie creature token"
    }
})
