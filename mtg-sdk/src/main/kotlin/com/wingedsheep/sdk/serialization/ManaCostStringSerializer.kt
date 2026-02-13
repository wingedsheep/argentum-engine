package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.ManaCost
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer that allows ManaCost to be written as a string like "{2}{R}{R}"
 * instead of the verbose structured form with nested ManaSymbol objects.
 *
 * Serialization: ManaCost → "{2}{R}{R}" (uses ManaCost.toString())
 * Deserialization: "{2}{R}{R}" → ManaCost (uses ManaCost.parse())
 */
object ManaCostStringSerializer : KSerializer<ManaCost> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ManaCost", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ManaCost) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ManaCost {
        val costString = decoder.decodeString()
        return if (costString.isBlank()) ManaCost.ZERO else ManaCost.parse(costString)
    }
}
