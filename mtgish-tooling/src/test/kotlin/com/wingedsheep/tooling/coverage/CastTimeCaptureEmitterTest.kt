package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.emitter.Emitter
import com.wingedsheep.tooling.coverage.emitter.subtypeArg
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Regression net for the `Modal_IfElse` "as you cast this spell" cast-time condition-capture handler
 * (CR 601.2i). Hermetic: feeds Steer Clear's exact mtgish IR (no 29 MB corpus, no network) and pins
 * that the emitter renders the cast-time form — `captureAtCast(...)` + `Conditions.CapturedAtCast(...)`
 * over a `ConditionalEffect`, NOT a resolution-time conditional that would test the Mount at the wrong
 * time. The two {X}-valued cards sharing the envelope (Faerie Fencing, Flame Discharge) intentionally
 * decline to SCAFFOLD through the inner effect renderer, so this fixed-damage card is the AUTO case.
 */
class CastTimeCaptureEmitterTest : StringSpec({

    val effects = Registry.loadEffectSerialNames()
    val keywords = Registry.loadKeywords()

    // Steer Clear's IR verbatim from the mtgish corpus.
    val steerClearIr = """
        {"_OracleCard":"Card","Name":"Steer Clear","Typeline":{"Supertypes":[],"Cardtypes":["Instant"],"Subtypes":[]},"ManaCost":[{"_ManaSymbol":"ManaCostW"}],"Rules":[{"_Rule":"SpellActions","args":{"_Actions":"Modal_IfElse","args":[{"_Condition":"PlayerPassesFilter","args":[{"_Player":"You"},{"_Players":"ControlsA","args":{"_Permanents":"IsCreatureType","args":"Mount"}}]},{"_Actions":"Targeted","args":[[{"_Target":"TargetPermanent","args":{"_Permanents":"And","args":[{"_Permanents":"Or","args":[{"_Permanents":"IsAttacking"},{"_Permanents":"IsBlocking"}]},{"_Permanents":"IsCardtype","args":"Creature"}]}}],{"_Actions":"ActionList","args":[{"_Action":"SpellDealsDamage","args":[{"_Spell":"ThisSpell"},{"_GameNumber":"Integer","args":4},{"_DamageRecipient":"Permanent","args":{"_Permanent":"Ref_TargetPermanent"}}]}]}]},{"_Actions":"Targeted","args":[[{"_Target":"TargetPermanent","args":{"_Permanents":"And","args":[{"_Permanents":"Or","args":[{"_Permanents":"IsAttacking"},{"_Permanents":"IsBlocking"}]},{"_Permanents":"IsCardtype","args":"Creature"}]}}],{"_Actions":"ActionList","args":[{"_Action":"SpellDealsDamage","args":[{"_Spell":"ThisSpell"},{"_GameNumber":"Integer","args":2},{"_DamageRecipient":"Permanent","args":{"_Permanent":"Ref_TargetPermanent"}}]}]}]}]}}]}
    """.trimIndent()

    "Steer Clear renders the cast-time capture form whole (AUTO)" {
        val card = Json.parseToJsonElement(steerClearIr) as JsonObject
        val result = Emitter.renderCard(card, scryfall = null, effects = effects, keywords = keywords)

        result.complete shouldBe true
        // The capture is declared as you cast, and read back at resolution — not a board re-check.
        result.text shouldContain "captureAtCast(\"controlledMount\", Conditions.YouControl"
        // Via the emitter's own renderer, not a literal: whether this reads `Subtype.MOUNT` or
        // `"Mount"` depends on whether the SDK names the constant today, which is not what this
        // test is about — pinning the literal broke it the day someone added `Subtype.MOUNT`.
        result.text shouldContain "withSubtype(${subtypeArg("Mount")})"
        result.text shouldContain "condition = Conditions.CapturedAtCast(\"controlledMount\")"
        // 4-damage then-branch, 2-damage else-branch, both on the one shared target.
        result.text shouldContain "effect = DealDamageEffect(4, t)"
        result.text shouldContain "elseEffect = DealDamageEffect(2, t)"
        result.text shouldContain "TargetFilter.AttackingOrBlockingCreature"
    }
})
