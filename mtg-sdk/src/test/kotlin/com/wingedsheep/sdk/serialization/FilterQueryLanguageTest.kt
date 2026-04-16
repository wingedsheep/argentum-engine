package com.wingedsheep.sdk.serialization

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class FilterQueryLanguageTest : DescribeSpec({

    describe("parseFilter") {

        it("parses a single type predicate") {
            val obj = FilterQueryLanguage.parseFilter("creature")
            val predicates = obj["cardPredicates"]!!.jsonArray
            predicates.size shouldBe 1
            predicates[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsCreature"
        }

        it("parses combined terms as AND (default matchAll)") {
            val obj = FilterQueryLanguage.parseFilter("creature tapped ctrl:you")
            val card = obj["cardPredicates"]!!.jsonArray
            val state = obj["statePredicates"]!!.jsonArray
            val ctrl = obj["controllerPredicate"]!!.jsonObject

            card.size shouldBe 1
            card[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsCreature"
            state.size shouldBe 1
            state[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsTapped"
            ctrl["type"]!!.jsonPrimitive.content shouldBe "ControlledByYou"
            // matchAll omitted when true
            obj.containsKey("matchAll") shouldBe false
        }

        it("parses OR expressions and sets matchAll=false") {
            val obj = FilterQueryLanguage.parseFilter("artifact|enchantment")
            obj["matchAll"]!!.jsonPrimitive.boolean.shouldBeFalse()

            val or = obj["cardPredicates"]!!.jsonArray[0].jsonObject
            or["type"]!!.jsonPrimitive.content shouldBe "Or"
            val children = or["predicates"]!!.jsonArray
            children.size shouldBe 2
            children[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsArtifact"
            children[1].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsEnchantment"
        }

        it("parses mana value comparisons (>=, <=, =)") {
            val at_least = FilterQueryLanguage.parseFilter("mv>=6").jsonObject
            at_least["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "ManaValueAtLeast"
            at_least["cardPredicates"]!!.jsonArray[0].jsonObject["min"]!!.jsonPrimitive.int shouldBe 6

            val at_most = FilterQueryLanguage.parseFilter("mv<=3").jsonObject
            at_most["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "ManaValueAtMost"
            at_most["cardPredicates"]!!.jsonArray[0].jsonObject["max"]!!.jsonPrimitive.int shouldBe 3

            val equals = FilterQueryLanguage.parseFilter("mv=5").jsonObject
            equals["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "ManaValueEquals"
            equals["cardPredicates"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.int shouldBe 5
        }

        it("parses power comparisons") {
            val ge = FilterQueryLanguage.parseFilter("pow>=3").jsonObject
            ge["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "PowerAtLeast"
            val le = FilterQueryLanguage.parseFilter("pow<=1").jsonObject
            le["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "PowerAtMost"
            val eq = FilterQueryLanguage.parseFilter("pow=2").jsonObject
            eq["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "PowerEquals"
        }

        it("parses toughness comparisons") {
            val ge = FilterQueryLanguage.parseFilter("tou>=4").jsonObject
            ge["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "ToughnessAtLeast"
            val le = FilterQueryLanguage.parseFilter("tou<=2").jsonObject
            le["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "ToughnessAtMost"
            val eq = FilterQueryLanguage.parseFilter("tou=5").jsonObject
            eq["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "ToughnessEquals"
        }

        it("parses keyword predicates as HasKeyword with uppercased keyword") {
            val obj = FilterQueryLanguage.parseFilter("kw:flying")
            val pred = obj["cardPredicates"]!!.jsonArray[0].jsonObject
            pred["type"]!!.jsonPrimitive.content shouldBe "HasKeyword"
            pred["keyword"]!!.jsonPrimitive.content shouldBe "FLYING"
        }

        it("parses color predicates") {
            val red = FilterQueryLanguage.parseFilter("red").jsonObject
            val pred = red["cardPredicates"]!!.jsonArray[0].jsonObject
            pred["type"]!!.jsonPrimitive.content shouldBe "HasColor"
            pred["color"]!!.jsonPrimitive.content shouldBe "RED"
        }

        it("parses colorless, multicolored, and monocolored") {
            val colorless = FilterQueryLanguage.parseFilter("colorless").jsonObject
            colorless["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsColorless"

            val multi = FilterQueryLanguage.parseFilter("multicolored").jsonObject
            multi["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsMulticolored"

            val mono = FilterQueryLanguage.parseFilter("monocolored").jsonObject
            mono["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsMonocolored"
        }

        it("parses name predicate") {
            val obj = FilterQueryLanguage.parseFilter("name:LightningBolt")
            val pred = obj["cardPredicates"]!!.jsonArray[0].jsonObject
            pred["type"]!!.jsonPrimitive.content shouldBe "NameEquals"
            pred["name"]!!.jsonPrimitive.content shouldBe "LightningBolt"
        }

        it("parses subtype predicates (capitalized tokens)") {
            val obj = FilterQueryLanguage.parseFilter("creature Goblin")
            val subs = obj["cardPredicates"]!!.jsonArray.filter {
                it.jsonObject["type"]!!.jsonPrimitive.content == "HasSubtype"
            }
            subs.size shouldBe 1
            subs[0].jsonObject["subtype"]!!.jsonPrimitive.content shouldBe "Goblin"
        }

        it("parses all state predicates") {
            for ((term, expected) in listOf(
                "tapped" to "IsTapped",
                "untapped" to "IsUntapped",
                "attacking" to "IsAttacking",
                "blocking" to "IsBlocking",
                "facedown" to "IsFaceDown",
            )) {
                val obj = FilterQueryLanguage.parseFilter(term)
                obj["statePredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe expected
            }
        }

        it("parses all controller predicates") {
            for ((term, expected) in listOf(
                "ctrl:you" to "ControlledByYou",
                "ctrl:opponent" to "ControlledByOpponent",
                "ctrl:any" to "ControlledByAny",
                "ctrl:target-opponent" to "ControlledByTargetOpponent",
                "ctrl:target-player" to "ControlledByTargetPlayer",
                "own:you" to "OwnedByYou",
                "own:opponent" to "OwnedByOpponent",
            )) {
                val obj = FilterQueryLanguage.parseFilter(term)
                obj["controllerPredicate"]!!.jsonObject["type"]!!.jsonPrimitive.content shouldBe expected
            }
        }

        it("parses every type keyword") {
            val types = listOf(
                "creature" to "IsCreature",
                "land" to "IsLand",
                "artifact" to "IsArtifact",
                "enchantment" to "IsEnchantment",
                "instant" to "IsInstant",
                "sorcery" to "IsSorcery",
                "permanent" to "IsPermanent",
                "nonland" to "IsNonland",
                "noncreature" to "IsNoncreature",
                "token" to "IsToken",
                "nontoken" to "IsNontoken",
                "basicland" to "IsBasicLand",
                "planeswalker" to "IsPlaneswalker",
            )
            for ((term, expected) in types) {
                val obj = FilterQueryLanguage.parseFilter(term)
                obj["cardPredicates"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe expected
            }
        }

        it("parses OR with comparisons, keywords, colors, and subtypes") {
            val obj = FilterQueryLanguage.parseFilter("creature|mv>=4|red|Goblin|kw:haste|colorless|multicolored")
            val or = obj["cardPredicates"]!!.jsonArray[0].jsonObject
            or["type"]!!.jsonPrimitive.content shouldBe "Or"
            val children = or["predicates"]!!.jsonArray
            val types = children.map { it.jsonObject["type"]!!.jsonPrimitive.content }
            types shouldBe listOf(
                "IsCreature", "ManaValueAtLeast", "HasColor",
                "HasSubtype", "HasKeyword", "IsColorless", "IsMulticolored"
            )
        }

        it("tokenizes with extra whitespace") {
            val obj = FilterQueryLanguage.parseFilter("  creature   tapped  ")
            obj["cardPredicates"]!!.jsonArray.size shouldBe 1
            obj["statePredicates"]!!.jsonArray.size shouldBe 1
        }

        it("throws on an unrecognized term") {
            shouldThrow<IllegalStateException> {
                FilterQueryLanguage.parseFilter("floopybloop")
            }
        }

        it("throws on OR children containing unknown terms") {
            shouldThrow<IllegalStateException> {
                FilterQueryLanguage.parseFilter("creature|unknownthing")
            }
        }
    }

    describe("formatFilter") {

        it("formats a minimal creature filter") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "IsCreature") }
                })
            }
            FilterQueryLanguage.formatFilter(obj) shouldBe "creature"
        }

        it("formats a singleton string predicate") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray { add(JsonPrimitive("IsCreature")) })
            }
            FilterQueryLanguage.formatFilter(obj) shouldBe "creature"
        }

        it("formats a filter with state and controller") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "IsCreature") }
                })
                put("statePredicates", buildJsonArray {
                    addJsonObject { put("type", "IsTapped") }
                })
                put("controllerPredicate", buildJsonObject { put("type", "ControlledByYou") })
            }
            FilterQueryLanguage.formatFilter(obj) shouldBe "creature tapped ctrl:you"
        }

        it("formats an Or expression") {
            val obj = buildJsonObject {
                put("matchAll", false)
                put("cardPredicates", buildJsonArray {
                    addJsonObject {
                        put("type", "Or")
                        put("predicates", buildJsonArray {
                            addJsonObject { put("type", "IsArtifact") }
                            addJsonObject { put("type", "IsEnchantment") }
                        })
                    }
                })
            }
            FilterQueryLanguage.formatFilter(obj) shouldBe "artifact|enchantment"
        }

        it("formats mana value / power / toughness comparisons") {
            fun filter(type: String, key: String, v: Int): JsonObject = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject {
                        put("type", type)
                        put(key, v)
                    }
                })
            }
            FilterQueryLanguage.formatFilter(filter("ManaValueAtLeast", "min", 6)) shouldBe "mv>=6"
            FilterQueryLanguage.formatFilter(filter("ManaValueAtMost", "max", 3)) shouldBe "mv<=3"
            FilterQueryLanguage.formatFilter(filter("ManaValueEquals", "value", 5)) shouldBe "mv=5"
            FilterQueryLanguage.formatFilter(filter("PowerAtLeast", "min", 3)) shouldBe "pow>=3"
            FilterQueryLanguage.formatFilter(filter("PowerAtMost", "max", 2)) shouldBe "pow<=2"
            FilterQueryLanguage.formatFilter(filter("PowerEquals", "value", 4)) shouldBe "pow=4"
            FilterQueryLanguage.formatFilter(filter("ToughnessAtLeast", "min", 4)) shouldBe "tou>=4"
            FilterQueryLanguage.formatFilter(filter("ToughnessAtMost", "max", 1)) shouldBe "tou<=1"
            FilterQueryLanguage.formatFilter(filter("ToughnessEquals", "value", 3)) shouldBe "tou=3"
        }

        it("formats HasSubtype, HasColor, and HasKeyword") {
            val subtype = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "HasSubtype"); put("subtype", "Dragon") }
                })
            }
            FilterQueryLanguage.formatFilter(subtype) shouldBe "Dragon"

            val color = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "HasColor"); put("color", "BLUE") }
                })
            }
            FilterQueryLanguage.formatFilter(color) shouldBe "blue"

            val keyword = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "HasKeyword"); put("keyword", "TRAMPLE") }
                })
            }
            FilterQueryLanguage.formatFilter(keyword) shouldBe "kw:trample"
        }

        it("formats IsColorless / IsMulticolored / IsMonocolored") {
            val mk = { type: String -> buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", type) }
                })
            } }
            FilterQueryLanguage.formatFilter(mk("IsColorless")) shouldBe "colorless"
            FilterQueryLanguage.formatFilter(mk("IsMulticolored")) shouldBe "multicolored"
            FilterQueryLanguage.formatFilter(mk("IsMonocolored")) shouldBe "monocolored"
        }

        it("formats NameEquals") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "NameEquals"); put("name", "Lightning Bolt") }
                })
            }
            FilterQueryLanguage.formatFilter(obj) shouldBe "name:Lightning Bolt"
        }

        it("formats state predicates from singleton strings and objects") {
            val fromObj = buildJsonObject {
                put("statePredicates", buildJsonArray {
                    addJsonObject { put("type", "IsAttacking") }
                })
            }
            FilterQueryLanguage.formatFilter(fromObj) shouldBe "attacking"

            val fromPrim = buildJsonObject {
                put("statePredicates", buildJsonArray { add(JsonPrimitive("IsBlocking")) })
            }
            FilterQueryLanguage.formatFilter(fromPrim) shouldBe "blocking"
        }

        it("formats controller predicates from singleton strings and objects") {
            val fromObj = buildJsonObject {
                put("controllerPredicate", buildJsonObject { put("type", "OwnedByYou") })
            }
            FilterQueryLanguage.formatFilter(fromObj) shouldBe "own:you"

            val fromPrim = buildJsonObject {
                put("controllerPredicate", JsonPrimitive("ControlledByOpponent"))
            }
            FilterQueryLanguage.formatFilter(fromPrim) shouldBe "ctrl:opponent"
        }

        it("returns null for an empty filter") {
            FilterQueryLanguage.formatFilter(buildJsonObject { }).shouldBeNull()
        }

        it("returns null when matchAll=false has multiple card predicates (not wrapped in Or)") {
            val obj = buildJsonObject {
                put("matchAll", false)
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "IsCreature") }
                    addJsonObject { put("type", "IsArtifact") }
                })
            }
            FilterQueryLanguage.formatFilter(obj).shouldBeNull()
        }

        it("returns null for card predicates the language cannot express") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray {
                    addJsonObject { put("type", "UnknownPredicate") }
                })
            }
            FilterQueryLanguage.formatFilter(obj).shouldBeNull()
        }

        it("returns null for singleton-string predicates that aren't type names") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray { add(JsonPrimitive("NotARealType")) })
            }
            FilterQueryLanguage.formatFilter(obj).shouldBeNull()
        }

        it("returns null when Or contains an unformattable sub-predicate") {
            val obj = buildJsonObject {
                put("matchAll", false)
                put("cardPredicates", buildJsonArray {
                    addJsonObject {
                        put("type", "Or")
                        put("predicates", buildJsonArray {
                            addJsonObject { put("type", "IsCreature") }
                            addJsonObject { put("type", "UnknownPredicate") }
                        })
                    }
                })
            }
            FilterQueryLanguage.formatFilter(obj).shouldBeNull()
        }

        it("returns null for array predicates (non-object, non-string)") {
            val obj = buildJsonObject {
                put("cardPredicates", buildJsonArray { add(JsonArray(emptyList())) })
            }
            FilterQueryLanguage.formatFilter(obj).shouldBeNull()
        }
    }

    describe("round-trip") {

        val queries = listOf(
            "creature",
            "creature tapped",
            "creature tapped ctrl:you",
            "artifact|enchantment",
            "mv>=6",
            "pow<=2 tou>=4",
            "kw:flying",
            "red",
            "colorless",
            "name:Sol Ring",
            "creature Goblin",
            "creature Dragon ctrl:opponent",
            "permanent untapped own:you",
        )

        queries.forEach { q ->
            it("round-trips '$q'") {
                val parsed = FilterQueryLanguage.parseFilter(q)
                val formatted = FilterQueryLanguage.formatFilter(parsed)
                formatted shouldBe q
            }
        }
    }

    describe("isGameObjectFilter") {

        it("returns true for objects with any filter key") {
            FilterQueryLanguage.isGameObjectFilter(
                buildJsonObject { put("cardPredicates", buildJsonArray { }) }
            ).shouldBeTrue()
            FilterQueryLanguage.isGameObjectFilter(
                buildJsonObject { put("statePredicates", buildJsonArray { }) }
            ).shouldBeTrue()
            FilterQueryLanguage.isGameObjectFilter(
                buildJsonObject { put("controllerPredicate", buildJsonObject { }) }
            ).shouldBeTrue()
            FilterQueryLanguage.isGameObjectFilter(
                buildJsonObject { put("matchAll", true) }
            ).shouldBeTrue()
        }

        it("returns false for unrelated objects") {
            FilterQueryLanguage.isGameObjectFilter(
                buildJsonObject { put("something", "else") }
            ).shouldBeFalse()
            FilterQueryLanguage.isGameObjectFilter(buildJsonObject { }).shouldBeFalse()
        }
    }
})
