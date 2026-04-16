package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CharacteristicValueSerializerTest : DescribeSpec({

    val json = CardSerialization.json
    val ser = CharacteristicValueSerializer

    describe("serialize") {

        it("encodes Fixed as a bare JSON number") {
            val element = json.encodeToJsonElement(ser, CharacteristicValue.Fixed(3))
            element.shouldBeInstanceOf<JsonPrimitive>()
            element.int shouldBe 3
        }

        it("encodes Dynamic as an object with type=Dynamic and nested source") {
            val value = CharacteristicValue.Dynamic(DynamicAmount.XValue)
            val element = json.encodeToJsonElement(ser, value)
            val obj = element.shouldBeInstanceOf<JsonObject>()
            obj["type"]!!.jsonPrimitive.content shouldBe "Dynamic"
            obj["source"]!!.jsonObject["type"]!!.jsonPrimitive.content shouldBe "XValue"
        }

        it("encodes DynamicWithOffset including the offset") {
            val value = CharacteristicValue.DynamicWithOffset(DynamicAmount.XValue, 1)
            val element = json.encodeToJsonElement(ser, value)
            val obj = element.shouldBeInstanceOf<JsonObject>()
            obj["type"]!!.jsonPrimitive.content shouldBe "DynamicWithOffset"
            obj["offset"]!!.jsonPrimitive.int shouldBe 1
        }
    }

    describe("deserialize") {

        it("reads bare integers as Fixed") {
            val decoded = json.decodeFromJsonElement(ser, JsonPrimitive(7))
            decoded shouldBe CharacteristicValue.Fixed(7)
        }

        it("reads Dynamic objects") {
            val element = json.encodeToJsonElement(ser, CharacteristicValue.Dynamic(DynamicAmount.XValue))
            val decoded = json.decodeFromJsonElement(ser, element)
            decoded shouldBe CharacteristicValue.Dynamic(DynamicAmount.XValue)
        }

        it("reads DynamicWithOffset objects") {
            val element = json.encodeToJsonElement(
                ser,
                CharacteristicValue.DynamicWithOffset(DynamicAmount.XValue, -2),
            )
            val decoded = json.decodeFromJsonElement(ser, element)
            decoded shouldBe CharacteristicValue.DynamicWithOffset(DynamicAmount.XValue, -2)
        }

        it("throws for unknown type discriminator") {
            val bad = json.parseToJsonElement(
                """{"type":"Nonsense","source":{"type":"XValue"}}""",
            )
            shouldThrow<IllegalArgumentException> {
                json.decodeFromJsonElement(ser, bad)
            }
        }

        it("throws for unexpected JSON shapes") {
            shouldThrow<IllegalArgumentException> {
                json.decodeFromJsonElement(ser, JsonArray(emptyList()))
            }
        }
    }

    describe("round-trip") {

        listOf(
            CharacteristicValue.Fixed(0),
            CharacteristicValue.Fixed(-5),
            CharacteristicValue.Fixed(99),
            CharacteristicValue.Dynamic(DynamicAmount.XValue),
            CharacteristicValue.DynamicWithOffset(DynamicAmount.XValue, 1),
            CharacteristicValue.DynamicWithOffset(DynamicAmount.XValue, -3),
        ).forEach { value ->
            it("round-trips $value") {
                val encoded = json.encodeToJsonElement(ser, value)
                val decoded = json.decodeFromJsonElement(ser, encoded)
                decoded shouldBe value
            }
        }
    }
})
