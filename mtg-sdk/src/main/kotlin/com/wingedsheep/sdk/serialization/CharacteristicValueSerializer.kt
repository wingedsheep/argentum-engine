package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.scripting.DynamicAmount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Custom serializer for CharacteristicValue that produces compact JSON.
 *
 * - Fixed(3) serializes as just `3`
 * - Dynamic(source) serializes as `{"type": "Dynamic", "source": "..."}`
 * - DynamicWithOffset(source, 1) serializes as `{"type": "DynamicWithOffset", "source": "...", "offset": 1}`
 */
object CharacteristicValueSerializer : KSerializer<CharacteristicValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("CharacteristicValue")

    override fun serialize(encoder: Encoder, value: CharacteristicValue) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is CharacteristicValue.Fixed -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is CharacteristicValue.Dynamic -> jsonEncoder.encodeJsonElement(buildJsonObject {
                put("type", "Dynamic")
                put("source", CardSerialization.json.encodeToJsonElement(DynamicAmount.serializer(), value.source))
            })
            is CharacteristicValue.DynamicWithOffset -> jsonEncoder.encodeJsonElement(buildJsonObject {
                put("type", "DynamicWithOffset")
                put("source", CardSerialization.json.encodeToJsonElement(DynamicAmount.serializer(), value.source))
                put("offset", value.offset)
            })
        }
    }

    override fun deserialize(decoder: Decoder): CharacteristicValue {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> CharacteristicValue.Fixed(element.int)
            is JsonObject -> {
                when (val type = element["type"]?.jsonPrimitive?.content) {
                    "Dynamic" -> {
                        val source = CardSerialization.json.decodeFromJsonElement(DynamicAmount.serializer(), element["source"]!!)
                        CharacteristicValue.Dynamic(source)
                    }
                    "DynamicWithOffset" -> {
                        val source = CardSerialization.json.decodeFromJsonElement(DynamicAmount.serializer(), element["source"]!!)
                        val offset = element["offset"]!!.jsonPrimitive.int
                        CharacteristicValue.DynamicWithOffset(source, offset)
                    }
                    else -> throw IllegalArgumentException("Unknown CharacteristicValue type: $type")
                }
            }
            else -> throw IllegalArgumentException("Unexpected JSON element for CharacteristicValue: $element")
        }
    }
}
