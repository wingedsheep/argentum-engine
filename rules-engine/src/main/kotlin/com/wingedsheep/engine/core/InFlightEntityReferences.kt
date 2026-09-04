package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
private val entityIdSerialName = EntityId.serializer().descriptor.serialName

@OptIn(ExperimentalSerializationApi::class)
private val characteristicValueSerialName = CharacteristicValue.serializer().descriptor.serialName

/** Internal seam for testing consumers' fail-closed response to projection errors. */
internal interface InFlightReferenceProjector {
    fun project(stackObject: ComponentContainer): InFlightEntityReferences.Projection
    fun project(decision: PendingDecision): InFlightEntityReferences.Projection
    fun project(frame: ContinuationFrame): InFlightEntityReferences.Projection
}

/**
 * Exhaustive typed [EntityId] traversal for sealed engine inputs.
 *
 * This follows the normal engine serializer schema rather than a hand-written list of fields. The
 * result retains every occurrence in serialization order and enough structural location to tell a
 * map key from its value. Consumers that need a different wire representation can therefore apply
 * their own naming policy without rediscovering engine schema shapes or collapsing repeated ids.
 *
 * For example, a policy adapter can replace the [EntityId] keys of a spell's damage-distribution
 * map with viewer-safe aliases while leaving a counter name or decision id unchanged, even when
 * that ordinary string has identical bytes. The occurrence path supplies the engine-owned schema
 * fact; the adapter still owns what alias is safe and what the resulting payload means.
 *
 * This is deliberately not a JSON policy API: entity aliases, alias-key collisions, visibility, and
 * any choice-specific canonicalization belong to the caller. A failed traversal is explicit, so a
 * caller that cannot safely proceed must not mistake it for an empty reference list.
 */
object TypedEntityReferences {

    /** One typed [EntityId] occurrence, including repeated occurrences of the same id. */
    data class Occurrence(
        val entityId: EntityId,
        val path: List<PathSegment>,
    )

    /** Serializer-structural path to an [Occurrence], not a policy-specific JSON pointer. */
    sealed interface PathSegment {
        /** A named field of a serializable record or sealed payload. */
        data class Field(val name: String) : PathSegment

        /** A collection member by serializer index. */
        data class Element(val index: Int) : PathSegment

        /**
         * The synthetic payload edge introduced by a sealed or open polymorphic serializer.
         *
         * Default object-polymorphic JSON flattens this payload into the containing object beside
         * its class discriminator, so a consumer reconciling these paths to that JSON form drops
         * only this typed segment. A real serializable property named `value` remains a
         * `Field("value")` and must not be dropped.
         */
        data object PolymorphicPayload : PathSegment

        /** A map entry, retaining whether the occurrence is the key or value. */
        data class MapEntry(
            val index: Int,
            val role: Role,
        ) : PathSegment {
            enum class Role { KEY, VALUE }
        }
    }

    sealed interface Projection {
        data class Complete(val occurrences: List<Occurrence>) : Projection {
            /** Compatibility view for conservative consumers that only need membership. */
            val entityIds: Set<EntityId> get() = occurrences.mapTo(linkedSetOf()) { it.entityId }
        }

        /** The graph could not be exhaustively traversed, so its references are unknown. */
        data class Incomplete(
            val rootType: String,
            val failure: String,
        ) : Projection
    }

    fun action(action: GameAction): Projection = project(GameAction.serializer(), action)

    fun response(response: DecisionResponse): Projection =
        project(DecisionResponse.serializer(), response)

    /** Internal generic seam shared with persisted in-flight traversal and failure witnesses. */
    internal fun <T : Any> project(
        serializer: SerializationStrategy<T>,
        value: T,
    ): Projection {
        val occurrences = mutableListOf<Occurrence>()
        return try {
            serializer.serialize(EntityReferenceEncoder(occurrences), value)
            Projection.Complete(occurrences)
        } catch (failure: Exception) {
            Projection.Incomplete(
                rootType = value.javaClass.name,
                failure = failure.javaClass.name,
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private class EntityReferenceEncoder(
        private val occurrences: MutableList<Occurrence>,
        private val path: List<PathSegment> = emptyList(),
        private val expectsEntityId: Boolean = false,
    ) : AbstractEncoder() {
        private var nextPath: List<PathSegment>? = null

        override val serializersModule = engineSerializersModule

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            check(!expectsEntityId) {
                "EntityId serializer changed to a structured form; projection must be updated"
            }
            return EntityReferenceEncoder(occurrences, consumePath())
        }

        override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
            nextPath = path + descriptor.pathSegment(index)
            return true
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun encodeInline(descriptor: SerialDescriptor): Encoder =
            EntityReferenceEncoder(
                occurrences = occurrences,
                path = consumePath(),
                expectsEntityId = descriptor.serialName == entityIdSerialName,
            )

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

        /**
         * [CharacteristicValue] is the one engine value family whose serializer deliberately
         * requires a JSON encoder. Traverse its actual sealed value instead: fixed values contain
         * no references, while dynamic values retain their typed [DynamicAmount] graph. A new
         * CharacteristicValue case therefore makes this exhaustive `when` fail to compile, and
         * every other unknown JSON-only serializer still makes the outer projection fail closed.
         */
        override fun <T> encodeSerializableValue(
            serializer: SerializationStrategy<T>,
            value: T,
        ) {
            if (serializer.descriptor.serialName == characteristicValueSerialName) {
                check(!expectsEntityId)
                val valuePath = consumePath()
                when (val characteristic = value as CharacteristicValue) {
                    is CharacteristicValue.Fixed -> Unit
                    is CharacteristicValue.Dynamic -> DynamicAmount.serializer().serialize(
                        EntityReferenceEncoder(
                            occurrences,
                            valuePath + PathSegment.Field("source"),
                        ),
                        characteristic.source,
                    )
                    is CharacteristicValue.DynamicWithOffset -> DynamicAmount.serializer().serialize(
                        EntityReferenceEncoder(
                            occurrences,
                            valuePath + PathSegment.Field("source"),
                        ),
                        characteristic.source,
                    )
                }
                return
            }
            super.encodeSerializableValue(serializer, value)
        }

        override fun encodeString(value: String) {
            if (expectsEntityId) occurrences += Occurrence(EntityId.of(value), path)
            nextPath = null
        }

        override fun encodeBoolean(value: Boolean) = rejectNonStringEntityId()
        override fun encodeByte(value: Byte) = rejectNonStringEntityId()
        override fun encodeChar(value: Char) = rejectNonStringEntityId()
        override fun encodeDouble(value: Double) = rejectNonStringEntityId()
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = rejectNonStringEntityId()
        override fun encodeFloat(value: Float) = rejectNonStringEntityId()
        override fun encodeInt(value: Int) = rejectNonStringEntityId()
        override fun encodeLong(value: Long) = rejectNonStringEntityId()
        override fun encodeShort(value: Short) = rejectNonStringEntityId()
        override fun encodeNull() = rejectNonStringEntityId()

        private fun consumePath(): List<PathSegment> = (nextPath ?: path).also { nextPath = null }

        private fun rejectNonStringEntityId() {
            check(!expectsEntityId) {
                "EntityId serializer changed to a non-string form; projection must be updated"
            }
            nextPath = null
        }

        private fun SerialDescriptor.pathSegment(index: Int): PathSegment = when (kind) {
            StructureKind.LIST -> PathSegment.Element(index)
            StructureKind.MAP -> PathSegment.MapEntry(
                index = index / 2,
                role = if (index % 2 == 0) PathSegment.MapEntry.Role.KEY else PathSegment.MapEntry.Role.VALUE,
            )
            PolymorphicKind.OPEN, PolymorphicKind.SEALED -> when (index) {
                1 -> PathSegment.PolymorphicPayload
                else -> PathSegment.Field(getElementName(index))
            }
            else -> PathSegment.Field(getElementName(index))
        }
    }
}

/**
 * Typed entity-reference projection for persisted in-flight engine execution.
 *
 * Live stack objects, pending decisions, and continuation frames use the same serializer-backed
 * traversal as [TypedEntityReferences], retaining only its set-membership compatibility view.
 * Any serialization failure remains explicit so hidden-world consumers fail closed rather than
 * treating an incomplete graph as unreferenced.
 */
internal object InFlightEntityReferences : InFlightReferenceProjector {

    sealed interface Projection {
        data class Complete(val entityIds: Set<EntityId>) : Projection

        /** The graph could not be exhaustively traversed, so its references are unknown. */
        data class Incomplete(
            val rootType: String,
            val failure: String,
        ) : Projection
    }

    override fun project(stackObject: ComponentContainer): Projection =
        project(ComponentContainer.serializer(), stackObject)

    override fun project(decision: PendingDecision): Projection =
        project(PolymorphicSerializer(PendingDecision::class), decision)

    override fun project(frame: ContinuationFrame): Projection =
        project(PolymorphicSerializer(ContinuationFrame::class), frame)

    private fun <T : Any> project(
        serializer: SerializationStrategy<T>,
        value: T,
    ): Projection {
        return when (val projection = TypedEntityReferences.project(serializer, value)) {
            is TypedEntityReferences.Projection.Complete -> Projection.Complete(projection.entityIds)
            is TypedEntityReferences.Projection.Incomplete -> Projection.Incomplete(
                rootType = projection.rootType,
                failure = projection.failure,
            )
        }
    }
}
