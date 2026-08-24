package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.DEFAULT_SELF_NOUN
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.SELF_NOUN_TOKEN
import com.wingedsheep.sdk.scripting.targets.resolveSelfNoun
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The self-noun placeholder mechanism (feature: type-aware self-noun rendering). A
 * [SelfReferentialDescription] effect emits [SELF_NOUN_TOKEN] for its [EffectTarget.Self] source in
 * [SelfReferentialDescription.descriptionTemplate]; [Effect.description] default-resolves the token
 * to [DEFAULT_SELF_NOUN] ("this permanent") so it never leaks, while the render layer substitutes the
 * host permanent's type noun (proven in the engine's transformer test).
 */
class SelfReferentialDescriptionTest : FunSpec({

    test("a Self-targeting effect emits the placeholder token in its template") {
        TransformEffect(EffectTarget.Self).descriptionTemplate shouldBe "Transform $SELF_NOUN_TOKEN"
    }

    test("description default-resolves the token to the type-neutral default; no token leaks") {
        DEFAULT_SELF_NOUN shouldBe "this permanent"
        val effect = TransformEffect(EffectTarget.Self)
        effect.description shouldBe "Transform this permanent"
        effect.description shouldNotContain SELF_NOUN_TOKEN
    }

    test("resolveSelfNoun substitutes the requested noun for the placeholder") {
        resolveSelfNoun("Transform $SELF_NOUN_TOKEN", "this creature") shouldBe "Transform this creature"
        resolveSelfNoun("Transform $SELF_NOUN_TOKEN", "this land") shouldBe "Transform this land"
    }

    test("a non-Self target carries no token and keeps its own noun") {
        val effect = TransformEffect(EffectTarget.EnchantedCreature)
        effect.descriptionTemplate shouldNotContain SELF_NOUN_TOKEN
        effect.description shouldBe "Transform enchanted creature"
    }

    test("ExileAndReturnTransformed carries the token per return face") {
        val effect = ExileAndReturnTransformedEffect(EffectTarget.Self, ReturnFace.TRANSFORMED)
        effect.descriptionTemplate shouldContain SELF_NOUN_TOKEN
        resolveSelfNoun(effect.descriptionTemplate, "this creature") shouldBe
            "Exile this creature, then return it to the battlefield transformed"
    }

    test("a self-sacrifice adapts its noun instead of calling a land a creature") {
        // Safe Haven is a Land whose upkeep trigger sacrifices itself. `EffectTarget.Self`'s own
        // description is the legacy "this creature", so the generated prompt read "you may
        // sacrifice this creature" on a land.
        val effect = SacrificeTargetEffect(EffectTarget.Self)
        effect.descriptionTemplate shouldBe "sacrifice $SELF_NOUN_TOKEN"
        effect.description shouldBe "sacrifice this permanent"
        resolveSelfNoun(effect.descriptionTemplate, "this land") shouldBe "sacrifice this land"
    }

    test("sacrificing a chosen target keeps that target's own wording") {
        SacrificeTargetEffect(EffectTarget.ContextTarget(0)).description shouldNotContain SELF_NOUN_TOKEN
    }

    test("serialization round-trips the effect; description recomputes (template is not serialized)") {
        val original: Effect = TransformEffect(EffectTarget.Self)
        val json = CardSerialization.json
        val restored = json.decodeFromString(
            Effect.serializer(),
            json.encodeToString(Effect.serializer(), original)
        )
        restored shouldBe original
        restored.shouldBeInstanceOf<SelfReferentialDescription>()
        (restored as SelfReferentialDescription).descriptionTemplate shouldBe "Transform $SELF_NOUN_TOKEN"
        restored.description shouldBe "Transform this permanent"
    }
})
