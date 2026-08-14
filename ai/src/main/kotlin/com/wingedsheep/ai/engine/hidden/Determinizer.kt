package com.wingedsheep.ai.engine.hidden

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.identity.HexproofFromComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.SelfZoneRedirectComponent
import com.wingedsheep.engine.state.components.identity.ToxicComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
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
 */
class Determinizer(
    private val cardRegistry: CardRegistry,
    private val visibility: Visibility = Visibility(cardRegistry),
) {
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
            for ((entityId, cardDef) in hidden.zip(definitions)) {
                val old = sampled.getEntity(entityId) ?: continue
                val ownerId = old.get<OwnerComponent>()?.playerId ?: opponentId
                var replacement = CardEntityFactory.create(cardDef, ownerId)
                old.get<RevealedToComponent>()?.let { replacement = replacement.with(it) }
                sampled = sampled.withEntity(entityId, replacement)
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
            sampled = sampled.copy(zones = sampled.zones + (libraryKey to shuffledLibrary))
        }
        return sampled
    }

    private fun hiddenCards(
        state: GameState,
        opponentId: EntityId,
        viewerId: EntityId,
    ): List<EntityId> {
        val handKey = ZoneKey(opponentId, Zone.HAND)
        val handHidden = !visibility.isZoneVisibleTo(state, handKey, viewerId)
        val candidates = buildList {
            addAll(state.getLibrary(opponentId))
            if (handHidden) addAll(state.getHand(opponentId))
        }
        val visibleTop = state.getLibrary(opponentId).firstOrNull()?.takeIf {
            visibility.revealsTopOfLibraryPublicly(state, opponentId) ||
                (opponentId == viewerId && visibility.hasLookAtTopOfLibrary(state, viewerId))
        }
        val referencedByStack = state.stack.flatMapTo(mutableSetOf()) { stackId ->
            state.getEntity(stackId)?.get<TargetsComponent>()?.targets.orEmpty().mapNotNull {
                when (it) {
                    is ChosenTarget.Card -> it.cardId
                    is ChosenTarget.Permanent -> it.entityId
                    is ChosenTarget.Spell -> it.spellEntityId
                    is ChosenTarget.Player -> null
                }
            }
        }
        return candidates.filter { id ->
            id != visibleTop &&
                id !in referencedByStack &&
                // Continuation frames carry entity references in several different shapes.
                // Quiet search roots normally have none; pinning while one exists is the safe
                // fallback until those shapes share a common reference visitor.
                state.continuationStack.isEmpty() &&
                !visibility.isCardRevealedTo(state, id, viewerId) &&
                isSafeToRewrite(state.getEntity(id))
        }
    }

    /**
     * A normal hidden card has only definition-derived identity/ownership components. Anything
     * else may be referenced by an in-flight effect or carry state that a different definition
     * cannot legally inherit, so it is pinned.
     */
    private fun isSafeToRewrite(container: ComponentContainer?): Boolean {
        if (container == null) return false
        return container.all().all {
            it is CardComponent ||
                it is OwnerComponent ||
                it is ControllerComponent ||
                it is RevealedToComponent ||
                it is CantBeCounteredComponent ||
                it is CantBeCopiedComponent ||
                it is HasMorphAbilityComponent ||
                it is ProtectionComponent ||
                it is SelfZoneRedirectComponent ||
                it is HexproofFromComponent ||
                it is ToxicComponent
        }
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
