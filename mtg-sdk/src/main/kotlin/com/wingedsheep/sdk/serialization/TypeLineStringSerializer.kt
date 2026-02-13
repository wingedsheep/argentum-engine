package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.TypeLine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer that allows TypeLine to be written as a string like
 * "Legendary Creature — Human Wizard" instead of the verbose structured form
 * with nested supertypes/cardTypes/subtypes sets.
 *
 * Serialization: TypeLine → "Legendary Creature — Human Wizard" (uses TypeLine.toString())
 * Deserialization: "Legendary Creature — Human Wizard" → TypeLine (uses TypeLine.parse())
 */
object TypeLineStringSerializer : KSerializer<TypeLine> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TypeLine", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TypeLine) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): TypeLine {
        return TypeLine.parse(decoder.decodeString())
    }
}
