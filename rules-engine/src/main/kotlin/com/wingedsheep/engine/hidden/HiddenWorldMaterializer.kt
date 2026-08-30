package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
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

    /** A stack, pending decision, or continuation caches identities that cannot be audited generically. */
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
 * every component derived from the requested definition. Individually revealed-to relationships
 * are also preserved. An object with any other runtime state is refused rather than transplanting
 * that state onto a different card.
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
 */
class HiddenWorldMaterializer(
    private val cardRegistry: CardRegistry,
) {
    fun materialize(
        state: GameState,
        request: HiddenWorldMaterializationRequest,
    ): HiddenWorldMaterializationResult {
        if (request.slotAssignments.isNotEmpty() && hasInFlightReferences(state)) {
            return unsupported(
                kind = UnsupportedHiddenWorldKind.IN_FLIGHT_REFERENCES,
                details = buildList {
                    if (state.stack.isNotEmpty()) add("stackDepth=${state.stack.size}")
                    state.pendingDecision?.let { add(it::class.simpleName ?: "PendingDecision") }
                    if (state.continuationStack.isNotEmpty()) {
                        add("continuationDepth=${state.continuationStack.size}")
                    }
                },
            )
        }

        var materialized = state

        for ((entityId, replacementDefinition) in request.slotAssignments.entries.sortedBy { it.key.value }) {
            val container = state.getEntity(entityId)
                ?: return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("entity does not exist"),
                )
            val zones = state.zones.filterValues { entityId in it }
            val occurrences = zones.values.sumOf { zone -> zone.count { it == entityId } }
            if (occurrences != 1 || zones.keys.singleOrNull()?.zoneType !in SUPPORTED_ZONES) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf(
                        if (occurrences == 0) "entity is not in a zone"
                        else if (occurrences > 1) "entity occurs in zones $occurrences times"
                        else "supported slots are HAND/LIBRARY; found ${zones.keys.single().zoneType.name}"
                    ),
                )
            }
            val zoneKey = zones.keys.single()

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
            val runtimeDifferences = runtimeDifferences(container, currentDefinition, ownerId)
            if (runtimeDifferences.isNotEmpty()) {
                return unsupported(
                    UnsupportedHiddenWorldKind.RUNTIME_STATE,
                    entityId,
                    runtimeDifferences,
                )
            }
            if (isDfcBackFace(replacementDefinition)) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("replacement HAND/LIBRARY identity is a DFC back face: ${replacementDefinition.name}"),
                )
            }
            var replacement = CardEntityFactory.create(replacementDefinition, ownerId)
            val replacementDefinitionId = replacement.require<CardComponent>().cardDefinitionId
            if (cardRegistry.getCard(replacementDefinitionId) != replacementDefinition) {
                return unsupported(
                    UnsupportedHiddenWorldKind.INVALID_ASSIGNMENT,
                    entityId,
                    listOf("replacement definition is not registered: $replacementDefinitionId"),
                )
            }
            replacement = container.get<ControllerComponent>()
                ?.let { replacement.with(it) }
                ?: replacement.without<ControllerComponent>()
            container.get<RevealedToComponent>()?.let { replacement = replacement.with(it) }
            materialized = materialized.withEntity(entityId, replacement)
        }

        return HiddenWorldMaterializationResult.Materialized(
            materialized.copy(rng = request.futureRng)
        )
    }

    /**
     * Compare against the factory output for the card currently occupying the slot. This keeps the
     * safe set in lockstep with new definition-derived components without teaching the materializer
     * a second hard-coded list. Every component present on the slot must match the factory value:
     * a normally definition-derived component that has been changed at runtime is runtime state
     * for this purpose. Missing factory components are safe to regenerate; zone transitions
     * legitimately strip [com.wingedsheep.engine.state.components.identity.ControllerComponent].
     */
    private fun runtimeDifferences(
        actual: ComponentContainer,
        currentDefinition: CardDefinition,
        ownerId: EntityId,
    ): List<String> {
        val expectedByType = CardEntityFactory.create(currentDefinition, ownerId)
            .all()
            .associateBy { it::class.java }
        return actual.all()
            .filterNot { it is CardComponent || it is RevealedToComponent }
            .filter { component -> expectedByType[component::class.java] != component }
            .map { component -> component::class.simpleName ?: component::class.java.name }
            .sorted()
    }

    private fun isDfcBackFace(definition: CardDefinition): Boolean =
        cardRegistry.getFrontFace(definition.name) != null

    private fun hasInFlightReferences(state: GameState): Boolean =
        state.stack.isNotEmpty() || state.pendingDecision != null || state.continuationStack.isNotEmpty()

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
