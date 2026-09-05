package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.InFlightEntityReferences
import com.wingedsheep.engine.core.InFlightReferenceProjector
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng

/**
 * An explicit assignment of card definitions to unresolved hidden-zone entity slots.
 *
 * This request deliberately contains no sampling policy. The caller decides which slots are
 * unresolved, which definitions occupy them, and which random stream future simulated game events
 * use. Assigning definitions to the existing entity ids also assigns the ordered library: zone
 * membership and entity order are preserved exactly. The keys must include every slot the caller
 * considers unresolved; unmentioned slots are intentionally left unchanged because the complete
 * [GameState] substrate cannot reveal a semantic omission.
 */
data class HiddenWorldMaterializationRequest(
    val slotAssignments: Map<EntityId, CardDefinition>,
    val futureRng: GameRng,
)

/** Why an explicit hidden-world assignment cannot safely be applied. */
enum class UnsupportedHiddenWorldKind {
    /** The request names a missing entity, an unknown source definition, or a non-hand/library slot. */
    INVALID_ASSIGNMENT,

    /** A stack target or conservative paused-execution pin blocks identity replacement. */
    IN_FLIGHT_REFERENCES,

    /** The hidden object carries state beyond its printed-definition-derived components. */
    RUNTIME_STATE,
}

data class UnsupportedHiddenWorld(
    val kind: UnsupportedHiddenWorldKind,
    val entityId: EntityId? = null,
    val details: List<String> = emptyList(),
)

sealed interface HiddenWorldMaterializationResult {
    data class Materialized(val state: GameState) : HiddenWorldMaterializationResult
    data class Unsupported(val reason: UnsupportedHiddenWorld) : HiddenWorldMaterializationResult
}

/**
 * Applies caller-supplied hidden card identities to a hypothetical engine state.
 *
 * This is the engine-coherence half of hidden-world construction, not a visibility oracle or a
 * determinizer. It never chooses slots, reads their current identities as candidate assignments,
 * checks deck composition, or assigns probabilities. A caller operating from incomplete
 * information must name every unresolved slot itself. The source definition is inspected only to
 * verify that the slot's component shape is factory-derived; it is never copied into an assignment.
 *
 * Supported slots are ordinary cards in hand or library. Their entity ids, owners, controller
 * component, zone membership, and zone order are preserved while [CardEntityFactory] rebuilds
 * every component derived from the requested definition. An object with any other runtime state —
 * a reveal someone has already been shown included — is refused rather than transplanting that
 * state onto a different card. The mechanics live in [HiddenSlotRewrite], shared with the AI's
 * determinizer so the two cannot disagree about what is safe.
 *
 * References stored elsewhere in the state keep pointing at the same entity. Live consumers
 * therefore see the assigned current definition, while frozen historical snapshots remain frozen.
 * The materializer validates current structural safety, not whether the assignment could have been
 * reached through the state's recorded history.
 *
 * [HiddenWorldMaterializationRequest.slotAssignments] is also the caller's explicit declaration of
 * which slots are unresolved. Unmentioned slots are deliberately preserved. Because [GameState]
 * itself always contains complete identities, this utility cannot infer that a caller omitted a
 * semantically hidden slot and is not an information-security boundary.
 *
 * [HiddenWorldMaterializationRequest.futureRng] is mandatory and is installed verbatim.
 * Consequently a hypothetical world never inherits the source state's authoritative future random
 * stream unless the caller explicitly asks for that exact generator. Search callers that require
 * information separation must derive this generator from caller-owned randomness, not [GameState.rng].
 * A paused decision does not change that contract: choices and random results already represented
 * in the decision or continuation remain fixed, while random operations that execute only after the
 * pause consume the caller's future stream. If the typed in-flight graph cannot be traversed
 * completely, the whole request is refused before either identities or randomness change.
 */
class HiddenWorldMaterializer internal constructor(
    private val cardRegistry: CardRegistry,
    /** Test-only seam for proving that an incomplete paused-state projection fails closed. */
    private val inFlightReferenceProjector: InFlightReferenceProjector,
) {
    constructor(cardRegistry: CardRegistry) : this(cardRegistry, InFlightEntityReferences)

    /**
     * A request is answered as a whole: either every named slot is installed, or nothing is and the
     * first obstruction is reported. Partial worlds are not a useful answer to "is this hypothesis
     * coherent", and a caller that wants per-slot pinning is choosing a sampling policy this class
     * deliberately doesn't own.
     */
    fun materialize(
        state: GameState,
        request: HiddenWorldMaterializationRequest,
    ): HiddenWorldMaterializationResult {
        val inFlightPins = when (
            val pins = HiddenSlotRewrite.identitySensitiveInFlightPins(state, inFlightReferenceProjector)
        ) {
            is HiddenSlotRewrite.IdentitySensitiveInFlightPins.Complete -> pins
            is HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete -> {
                return unsupported(
                    UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES,
                    details = listOf(pins.reason),
                )
            }
        }
        if (request.slotAssignments.isEmpty()) {
            return HiddenWorldMaterializationResult.Materialized(state.copy(rng = request.futureRng))
        }
        // Preserve each occurrence, including duplicates within one zone. Validation below still
        // chooses the first obstruction in assignment order; unrelated malformed slots are ignored.
        // For one or two assignments the original scan costs less than building an index.
        val memberships = if (request.slotAssignments.size > 2) {
            mutableMapOf<EntityId, MutableList<ZoneKey>>().also { index ->
                for ((zoneKey, ids) in state.zones) {
                    for (id in ids) {
                        if (id in request.slotAssignments) index.getOrPut(id) { mutableListOf() }.add(zoneKey)
                    }
                }
            }
        } else null
        // Accumulate privately: validation still reads the source, and a refusal publishes nothing.
        val materializedEntities = state.entities.toMutableMap()
        // Deck copies share factory defaults, but ownership and per-slot runtime state still differ.
        val expectedContainers = mutableMapOf<Pair<String, EntityId>, ComponentContainer>()

        // Slots are independent, so the order only decides *which* obstruction is reported when a
        // request has several. Sorting makes that report stable across equal requests.
        for ((entityId, replacementDefinition) in request.slotAssignments.entries.sortedBy { it.key.value }) {
            val container = state.getEntity(entityId)
                ?: return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("entity does not exist"),
                )
            if (entityId in inFlightPins.entityIds) {
                return unsupported(
                    UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES,
                    entityId,
                    listOf("slot is conservatively pinned by in-flight execution"),
                )
            }
            val zones = if (memberships == null) state.zones.filterValues { entityId in it } else null
            val indexedZones = memberships?.get(entityId).orEmpty()
            val occurrences = if (zones != null) zones.values.sumOf { zone -> zone.count { it == entityId } }
                else indexedZones.size
            if (occurrences != 1) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf(
                        if (occurrences == 0) "entity is not in a zone"
                        else "entity occurs in zones $occurrences times"
                    ),
                )
            }
            val zoneKey = zones?.keys?.single() ?: indexedZones.single()
            if (zoneKey.zoneType !in SUPPORTED_ZONES) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("supported slots are HAND/LIBRARY; found ${zoneKey.zoneType.name}"),
                )
            }

            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("entity has no owner"),
                )
            if (zoneKey.ownerId != ownerId) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("zone owner ${zoneKey.ownerId.value} differs from card owner ${ownerId.value}"),
                )
            }
            val currentCard = container.get<CardComponent>()
                ?: return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("entity has no card identity"),
                )
            if (currentCard.ownerId != ownerId) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("card identity owner differs from OwnerComponent"),
                )
            }
            val currentDefinition = cardRegistry.getCard(currentCard.cardDefinitionId)
                ?: return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("source definition is not registered: ${currentCard.cardDefinitionId}"),
                )
            val expected = expectedContainers.getOrPut(currentCard.cardDefinitionId to ownerId) {
                CardEntityFactory.create(currentDefinition, ownerId)
            }
            val blockers = HiddenSlotRewrite.runtimeBlockers(container, expected)
            if (blockers.isNotEmpty()) {
                return unsupported(UnsupportedHiddenWorldKind.RUNTIME_STATE, entityId, blockers)
            }
            if (isTransformBackFace(replacementDefinition)) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("replacement HAND/LIBRARY identity is a DFC back face: ${replacementDefinition.name}"),
                )
            }
            val replacementContainer = CardEntityFactory.create(replacementDefinition, ownerId)
            val replacementDefinitionId = replacementContainer
                .require<CardComponent>()
                .cardDefinitionId
            if (cardRegistry.getCard(replacementDefinitionId) != replacementDefinition) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("replacement definition is not registered: $replacementDefinitionId"),
                )
            }
            materializedEntities[entityId] = HiddenSlotRewrite.rewrite(container, replacementContainer)
        }

        return HiddenWorldMaterializationResult.Materialized(
            state.copy(entities = materializedEntities, rng = request.futureRng)
        )
    }

    /**
     * Transform DFCs register their back face as a standalone definition so the transform machinery
     * can resolve it, but a back face is never a legal card identity in hand or library. Modal DFC
     * backs are outside this check by construction: they are faces of one definition, not separate
     * registrations, so a caller cannot name one in the first place.
     */
    private fun isTransformBackFace(definition: CardDefinition): Boolean =
        cardRegistry.getFrontFace(definition.name) != null

    private fun unsupported(
        kind: UnsupportedHiddenWorldKind,
        entityId: EntityId? = null,
        details: List<String> = emptyList(),
    ): HiddenWorldMaterializationResult.Unsupported =
        HiddenWorldMaterializationResult.Unsupported(
            UnsupportedHiddenWorld(kind, entityId, details)
        )

    private companion object {
        val SUPPORTED_ZONES = setOf(Zone.HAND, Zone.LIBRARY)
    }
}
