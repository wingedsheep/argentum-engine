package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.emitter.Emitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Pins how the emitter renders prowess. Prowess is a keyword ability whose +1/+1-on-noncreature-cast
 * trigger the engine derives from an explicit triggered ability, NOT from the keyword tag — so:
 *
 * - A printed `Prowess` keyword must render via the `prowess()` DSL helper (keyword + trigger), never
 *   a bare `keywords(Keyword.PROWESS)`, which would compile but make the pump a no-op.
 * - "Other creatures you control have prowess" (`EachPermanentLayerEffect` → `AddAbility[Prowess]`)
 *   can't be modeled by the generic `GrantKeyword` lord path (it would grant only the tag, no trigger),
 *   so it declines to a SCAFFOLD rather than emitting a confidently-wrong complete render.
 *
 * Hermetic: synthetic IR, no IR download / Scryfall cache.
 */
class ProwessEmitterTest : StringSpec({

    val effects = Registry.loadEffectSerialNames()
    val keywords = Registry.loadKeywords()

    fun creatureBuilder(name: String, rules: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
        put("Name", JsonPrimitive(name))
        putJsonObject("Typeline") {
            putJsonArray("Supertypes") {}
            putJsonArray("Cardtypes") { add(JsonPrimitive("Creature")) }
            putJsonArray("Subtypes") { add(JsonPrimitive("Otter")) }
        }
        putJsonArray("ManaCost") {
            addJsonObject { put("_ManaSymbol", JsonPrimitive("ManaCostU")) }
            addJsonObject { put("_ManaSymbol", JsonPrimitive("ManaCostR")) }
        }
        putJsonObject("CardPT") { put("Power", JsonPrimitive(2)); put("Toughness", JsonPrimitive(2)) }
        rules()
    }

    "a printed Prowess keyword renders via prowess(), not a bare keywords(Keyword.PROWESS)" {
        val card = creatureBuilder("Test Prowess Otter") {
            putJsonArray("Rules") {
                addJsonObject { put("_Rule", JsonPrimitive("Prowess")) }
            }
        }
        val r = Emitter.renderCard(card, null, effects, keywords)

        r.complete shouldBe true
        r.text shouldContain "prowess()"
        r.text shouldNotContain "keywords(Keyword.PROWESS)"
    }

    "\"other creatures you control have prowess\" declines to a scaffold, not a no-op GrantKeyword" {
        val card = creatureBuilder("Test Prowess Lord") {
            putJsonArray("Rules") {
                addJsonObject {
                    put("_Rule", JsonPrimitive("EachPermanentLayerEffect"))
                    putJsonArray("args") {
                        // Affected group: Other(ThisPermanent) AND Creature AND ControlledBy You.
                        addJsonObject {
                            put("_Permanents", JsonPrimitive("And"))
                            putJsonArray("args") {
                                addJsonObject {
                                    put("_Permanents", JsonPrimitive("Other"))
                                    putJsonObject("args") { put("_Permanent", JsonPrimitive("ThisPermanent")) }
                                }
                                addJsonObject {
                                    put("_Permanents", JsonPrimitive("And"))
                                    putJsonArray("args") {
                                        addJsonObject {
                                            put("_Permanents", JsonPrimitive("IsCardtype"))
                                            put("args", JsonPrimitive("Creature"))
                                        }
                                        addJsonObject {
                                            put("_Permanents", JsonPrimitive("ControlledByAPlayer"))
                                            putJsonObject("args") {
                                                put("_Players", JsonPrimitive("SinglePlayer"))
                                                putJsonObject("args") { put("_Player", JsonPrimitive("You")) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // The granted layer effect: AddAbility [Prowess].
                        addJsonArray {
                            addJsonObject {
                                put("_StaticLayerEffect", JsonPrimitive("AddAbility"))
                                putJsonArray("args") {
                                    addJsonObject { put("_Rule", JsonPrimitive("Prowess")) }
                                }
                            }
                        }
                    }
                }
            }
        }
        val r = Emitter.renderCard(card, null, effects, keywords)

        r.complete shouldBe false
        r.text shouldNotContain "GrantKeyword(Keyword.PROWESS"
    }
})
