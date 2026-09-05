package com.wingedsheep.ai.engine.hidden

import com.wingedsheep.engine.hidden.HiddenSlotRewrite
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng

/**
 * Samples a complete state consistent with what [viewerId] is allowed to know.
 *
 * Entities and zone membership never change. Only hidden card identities and opponent library
 * ordering are sampled, which keeps continuations, targets and pending decisions structurally
 * valid. Cards carrying runtime components are pinned because changing their definition could make
 * those components nonsensical.
 *
 * Pinning is a coherence guarantee, not an information one, and the two pull in opposite
 * directions: a slot pinned because in-flight execution references it keeps its **real** identity
 * in the sampled world even when that identity is hidden from [viewerId]. That is accepted because
 * the alternative is a world whose paused decision or continuation points at a card that was never
 * there. It stays sound for search because the Strategist determinizes at roots where the AI holds
 * priority, so a paused root is one the AI itself must answer — the referenced slots are its own.
 * A caller that determinizes at someone else's pause would be handing its search the truth.
 */
class Determinizer internal constructor(
    private val cardRegistry: CardRegistry,
    private val visibility: Visibility,
    /** Test-only seam: inject the shared final analysis, never reference traversal rules. */
    private val inFlightPinAnalysis: (GameState) -> HiddenSlotRewrite.IdentitySensitiveInFlightPins,
) {
    constructor(
        cardRegistry: CardRegistry,
        visibility: Visibility = Visibility(cardRegistry),
    ) : this(cardRegistry, visibility, HiddenSlotRewrite::identitySensitiveInFlightPins)

    /** Pure per-position entry point used by the Strategist before it simulates any candidate. */
    fun sampleForSearch(
        state: GameState,
        viewerId: EntityId,
        models: Map<EntityId, OpponentModel> = emptyMap(),
    ): GameState = sample(
        state,
        viewerId,
        models,
        GameRng.seeded(
            state.rng.state xor
                (state.turnNumber.toLong() shl 32) xor
                (state.step.ordinal.toLong() shl 16) xor
                viewerId.value.hashCode().toLong()
        ),
    )

    fun sample(
        state: GameState,
        viewerId: EntityId,
        model: OpponentModel,
        rng: GameRng,
    ): GameState = sample(state, viewerId, emptyMap(), rng, model)

    fun sample(
        state: GameState,
        viewerId: EntityId,
        models: Map<EntityId, OpponentModel>,
        rng: GameRng,
        fallback: OpponentModel = OpponentModel.IdentityPermutation,
    ): GameState {
        var sampled = state
        var currentRng = rng

        // Even the viewer's own library order is hidden. Teammate hands are visible, but teammate
        // libraries are not. Walk every player and let Visibility decide which cards are hidden.
        for (opponentId in state.turnOrder) {
            val hidden = hiddenCards(state, opponentId, viewerId)
            if (hidden.isEmpty()) continue

            val definitions = when (val model = models[opponentId] ?: fallback) {
                is OpponentModel.KnownDecklist -> fromKnownDecklist(state, opponentId, hidden, model, currentRng)
                    .also { currentRng = it.second }
                    .first
                OpponentModel.IdentityPermutation -> {
                    val existing = hidden.mapNotNull { id ->
                        state.getEntity(id)?.get<CardComponent>()?.let { cardRegistry.getCard(it.cardDefinitionId) }
                    }
                    currentRng.shuffle(existing).also { currentRng = it.second }.first
                }
            }

            if (definitions.size != hidden.size) continue
            // No caller observes intermediate identities. Copy once for this player and publish
            // only after all replacements, without mutating an input or previously sampled world.
            val sampledEntities = sampled.entities.toMutableMap()
            for ((entityId, cardDef) in hidden.zip(definitions)) {
                val old = sampledEntities[entityId] ?: continue
                val ownerId = old.get<OwnerComponent>()?.playerId ?: opponentId
                sampledEntities[entityId] = HiddenSlotRewrite.rewrite(old, cardDef, ownerId)
            }

            val libraryKey = ZoneKey(opponentId, Zone.LIBRARY)
            val library = sampled.getZone(libraryKey)
            val hiddenLibraryIds = hidden.filterTo(mutableSetOf()) { it in library }
            val (shuffledHidden, next) = currentRng.shuffle(library.filter { it in hiddenLibraryIds })
            currentRng = next
            val iterator = shuffledHidden.iterator()
            val shuffledLibrary = library.map { id ->
                if (id in hiddenLibraryIds) iterator.next() else id
            }
            sampled = sampled.copy(
                entities = sampledEntities,
                zones = sampled.zones + (libraryKey to shuffledLibrary),
            )
        }
        return sampled
    }

    private fun hiddenCards(
        state: GameState,
        opponentId: EntityId,
        viewerId: EntityId,
    ): List<EntityId> {
        val handKey = ZoneKey(opponentId, Zone.HAND)
        val libraryKey = ZoneKey(opponentId, Zone.LIBRARY)
        val library = state.getLibrary(opponentId)
        val candidates = library + state.getHand(opponentId)
        val inFlightPins = when (val pins = inFlightPinAnalysis(state)) {
            is HiddenSlotRewrite.IdentitySensitiveInFlightPins.Complete -> pins.entityIds
            // An incomplete traversal cannot justify sampling any hidden identity. HWM rejects the
            // corresponding all-or-nothing request; sampling instead keeps every candidate slot.
            is HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete -> return emptyList()
        }
        return candidates.filter { id ->
            val zoneKey = if (id in library) libraryKey else handKey
            !visibility.isCardIdentityVisibleTo(state, zoneKey, id, viewerId) &&
                id !in inFlightPins &&
                isSafeToRewrite(state, id)
        }
    }

    /**
     * A normal hidden card has only components [com.wingedsheep.engine.core.CardEntityFactory]
     * derives from its printed definition. Anything else may be referenced by an in-flight effect
     * or carry state that a different definition cannot legally inherit, so the slot is pinned.
     * A card whose definition is no longer registered is pinned too: without it there is nothing to
     * compare the slot against.
     */
    private fun isSafeToRewrite(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val ownerId = container.get<OwnerComponent>()?.playerId ?: return false
        val definition = container.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            ?: return false
        return HiddenSlotRewrite.runtimeBlockers(container, definition, ownerId).isEmpty()
    }

    private fun fromKnownDecklist(
        state: GameState,
        opponentId: EntityId,
        hidden: List<EntityId>,
        model: OpponentModel.KnownDecklist,
        rng: GameRng,
    ): Pair<List<CardDefinition>, GameRng> {
        val remaining = model.cards.toMutableMap()
        val sampledIds = hidden.toSet()
        for ((id, container) in state.entities) {
            if (id in sampledIds || container.get<OwnerComponent>()?.playerId != opponentId) continue
            val name = container.get<CardComponent>()?.name ?: continue
            remaining.computeIfPresent(name) { _, n -> (n - 1).coerceAtLeast(0) }
        }
        val pool = remaining.flatMap { (name, copies) ->
            val definition = cardRegistry.getCard(name) ?: return@flatMap emptyList()
            List(copies) { definition }
        }
        if (pool.size < hidden.size) return emptyList<CardDefinition>() to rng
        val (shuffled, next) = rng.shuffle(pool)
        return shuffled.take(hidden.size) to next
    }
}
