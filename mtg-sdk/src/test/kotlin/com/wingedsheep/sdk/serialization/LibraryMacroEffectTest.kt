package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EmitScriedEventEffect
import com.wingedsheep.sdk.scripting.effects.EmitSurveiledEventEffect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The scry / surveil macro effects (compact-library-serialization): a card whose effect *is* "scry N"
 * / "surveil N" serializes as one node ([ScryEffect] / [SurveilEffect]) and expands to the shared
 * Gather → Select → Move pipeline only at execution time. These tests pin the compact representation,
 * the round-trip, and that the expansion still carries the emit-event tail that fires the triggers.
 */
class LibraryMacroEffectTest : FunSpec({

    val json = CardSerialization.json

    test("Patterns.Library.scry/surveil and the Effects facade return the compact macro node") {
        LibraryPatterns.scry(2).shouldBeInstanceOf<ScryEffect>().count shouldBe 2
        LibraryPatterns.surveil(3).shouldBeInstanceOf<SurveilEffect>().count shouldBe 3
        Effects.Scry(1).shouldBeInstanceOf<ScryEffect>()
        Effects.Surveil(4).shouldBeInstanceOf<SurveilEffect>()
    }

    test("the macro nodes serialize compactly and round-trip to the same object") {
        val scry: Effect = Effects.Scry(2)
        val encoded = json.encodeToString(Effect.serializer(), scry)
        encoded shouldContain "\"Scry\""
        // Compact: no unrolled pipeline node leaks into the serialized form.
        encoded.shouldNotContainPipeline()
        json.decodeFromString(Effect.serializer(), encoded) shouldBe scry

        val surveil: Effect = Effects.Surveil(3)
        val surveilEncoded = json.encodeToString(Effect.serializer(), surveil)
        surveilEncoded shouldContain "\"Surveil\""
        surveilEncoded.shouldNotContainPipeline()
        json.decodeFromString(Effect.serializer(), surveilEncoded) shouldBe surveil
    }

    test("expandMacro produces the scry pipeline, tail-terminated by the scried event") {
        val pipeline = LibraryPatterns.expandMacro(ScryEffect(2))
        pipeline.shouldBeInstanceOf<CompositeEffect>()
        pipeline shouldBe LibraryPatterns.scryPipeline(2)
        pipeline.effects.last().shouldBeInstanceOf<EmitScriedEventEffect>()

        val surveilPipeline = LibraryPatterns.expandMacro(SurveilEffect(1))
        surveilPipeline.shouldBeInstanceOf<CompositeEffect>()
        surveilPipeline shouldBe LibraryPatterns.surveilPipeline(1)
        surveilPipeline.effects.last().shouldBeInstanceOf<EmitSurveiledEventEffect>()
    }

    test("a literal scry/surveil 0 expands without an emit-event tail (CR 701.18b / 701.42c)") {
        LibraryPatterns.scryPipeline(0).effects.none { it is EmitScriedEventEffect } shouldBe true
        LibraryPatterns.surveilPipeline(0).effects.none { it is EmitSurveiledEventEffect } shouldBe true
    }

    test("expandMacro returns null for a non-macro effect") {
        LibraryPatterns.expandMacro(EmitScriedEventEffect()) shouldBe null
    }
})

private fun String.shouldNotContainPipeline() {
    listOf("GatherCards", "SelectFromCollection", "MoveCollection", "Composite").forEach { node ->
        if (this.contains("\"$node\"")) error("compact macro serialization leaked pipeline node \"$node\": $this")
    }
}
