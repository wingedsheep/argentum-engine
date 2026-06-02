package com.wingedsheep.sdk.scripting.effects

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Unit-level coverage of the `addCardTypes` field on
 * [CreateTokenCopyOfSourceEffect]. The full battlefield-level behavior is
 * exercised by Vaultborn Tyrant's interaction with its dies trigger; here we
 * lock down the description generation and parameter shape so the SDK
 * contract doesn't drift.
 */
class CreateTokenCopyOfSourceEffectTest : FunSpec({

    test("default addCardTypes is empty and omitted from the description") {
        val effect = CreateTokenCopyOfSourceEffect()
        effect.addCardTypes shouldBe emptySet()
        effect.description shouldBe "Create a token that's a copy of this creature"
    }

    test("addCardTypes renders the 'except it's a TYPE in addition to its other types' clause") {
        val effect = CreateTokenCopyOfSourceEffect(addCardTypes = setOf("ARTIFACT"))
        // "Create a token that's a copy of this creature, except it's an artifact in addition to its other types"
        effect.description shouldContain "except"
        effect.description shouldContain "artifact"
        effect.description shouldContain "in addition to its other types"
    }

    test("addCardTypes combines cleanly with P/T overrides") {
        val effect = CreateTokenCopyOfSourceEffect(
            overridePower = 1,
            overrideToughness = 1,
            addCardTypes = setOf("ARTIFACT")
        )
        // Both the P/T override and the type clause should be present, joined by " and ".
        effect.description shouldContain "it's 1/1"
        effect.description shouldContain "it's an artifact in addition to its other types"
        effect.description shouldContain " and "
    }
})
