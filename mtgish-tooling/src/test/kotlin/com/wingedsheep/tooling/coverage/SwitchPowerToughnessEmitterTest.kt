package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.emitter.Emitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SwitchPowerToughnessEmitterTest : StringSpec({
    val effects = Registry.loadEffectSerialNames()
    val keywords = Registry.loadKeywords()
    fun render(expiration: String) = Emitter.renderCard(Json.parseToJsonElement("""
        {"Name":"Switch Test","Typeline":{"Supertypes":[],"Cardtypes":["Instant"],"Subtypes":[]},
        "ManaCost":[{"_ManaSymbol":"ManaCostU"}],"Rules":[{"_Rule":"SpellActions","args":{
        "_Actions":"Targeted","args":[[{"_Target":"TargetPermanent","args":{"_Permanents":"IsCardtype","args":"Creature"}}],
        {"_Actions":"ActionList","args":[{"_Action":"CreatePermanentLayerEffectUntil","args":[
        {"_Permanent":"Ref_TargetPermanent"},[{"_LayerEffect":"SwitchPT"}],{"_Expiration":"$expiration"}]}]}]}}]}
    """).jsonObject, null, effects, keywords)

    "switch preserves target and end-of-turn duration" {
        val result = render("UntilEndOfTurn")
        result.complete shouldBe true
        result.text shouldContain "Effects.SwitchPowerToughness("
        result.text shouldContain "Duration.EndOfTurn"
    }
    "unknown duration declines" {
        render("UnknownExpiration").complete shouldBe false
    }
})
