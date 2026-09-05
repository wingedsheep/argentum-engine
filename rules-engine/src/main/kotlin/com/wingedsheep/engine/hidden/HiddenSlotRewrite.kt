package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.InFlightEntityReferences
import com.wingedsheep.engine.core.InFlightReferenceProjector
import com.wingedsheep.engine.core.TypedEntityReferences
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * Swapping the card identity occupying a hidden zone slot, without disturbing anything else.
 *
 * Both hidden-information callers need the same three answers — is this slot safe to rewrite,
 * which slots are conservatively pinned by in-flight execution, and how is the rewrite applied —
 * while differing entirely in *policy*: [com.wingedsheep.engine.hidden.HiddenWorldMaterializer]
 * installs a caller-supplied assignment and refuses anything it cannot install, whereas the AI's
 * `Determinizer` samples an assignment from a visibility model and silently pins whatever it
 * cannot rewrite. Keeping the mechanics here means those two policies cannot drift into two
 * different notions of "safe".
 */
object HiddenSlotRewrite {

    /**
     * The one answer to which hidden slots cannot have their identities replaced while execution
     * is *in flight* — that is, while [GameState.stack], [GameState.pendingDecision], or
     * [GameState.continuationStack] holds work the engine has started and not finished.
     *
     * Those three carriers contribute every typed [EntityId] their serializable graphs contain.
     * That is a conservative superset *of them*, because not every typed occurrence intrinsically
     * depends on a card's current identity. Callers that apply different policies (rejecting an
     * explicit assignment or silently pinning a sampled slot) must consume this one answer rather
     * than independently approximating only part of in-flight execution.
     *
     * It is deliberately not a whole-[GameState] answer. `GameState` also carries lists that name
     * hand or library entities outside any in-flight execution — `grantedKeywordAbilities` (a cast
     * keyword granted to a card in hand), `mayPlayPermissions`, `lastCardDrawnThisTurnByPlayer` —
     * and none of those is read here or visible to [runtimeBlockers], which only inspects the
     * entity's own container. Rewriting such a slot leaves the grant pointing at a different card.
     * That gap predates this projection; closing it means extending this analysis, not assuming it
     * already covers them. Transient bookkeeping like `pendingSacrificeIds` and
     * `pendingDiscardCauseControllers` is correctly out of scope: both live and die inside a single
     * zone move, so no pause can observe them.
     */
    sealed interface IdentitySensitiveInFlightPins {
        data class Complete(val entityIds: Set<EntityId>) : IdentitySensitiveInFlightPins

        /**
         * The graph is incomplete; no candidate may be rewritten, so callers reject or pin by
         * policy. There is exactly one [reason] because the analysis returns on its first failure:
         * an incomplete traversal is already fatal, so enumerating the rest buys nothing.
         */
        data class Incomplete(val reason: String) : IdentitySensitiveInFlightPins
    }

    fun identitySensitiveInFlightPins(state: GameState): IdentitySensitiveInFlightPins =
        identitySensitiveInFlightPins(state, InFlightEntityReferences)

    /** Test-only seam for consumers that need to prove their fail-closed policy. */
    internal fun identitySensitiveInFlightPins(
        state: GameState,
        inFlightReferenceProjector: InFlightReferenceProjector,
    ): IdentitySensitiveInFlightPins {
        val referenced = mutableSetOf<EntityId>()
        state.stack.forEachIndexed { index, stackId ->
            val stackObject = state.getEntity(stackId)
                ?: return IdentitySensitiveInFlightPins.Incomplete(
                    "could not traverse stack[$index] $stackId: missing entity",
                )
            when (val projection = inFlightReferenceProjector.project(stackObject)) {
                is TypedEntityReferences.Projection.Complete -> referenced += projection.entityIds
                is TypedEntityReferences.Projection.Incomplete -> {
                    return IdentitySensitiveInFlightPins.Incomplete(
                        "could not traverse stack[$index] ${projection.rootType}: ${projection.failure}",
                    )
                }
            }
        }
        state.pendingDecision?.let { decision ->
            when (val projection = inFlightReferenceProjector.project(decision)) {
                is TypedEntityReferences.Projection.Complete -> referenced += projection.entityIds
                is TypedEntityReferences.Projection.Incomplete -> {
                    return IdentitySensitiveInFlightPins.Incomplete(
                        "could not traverse pending decision ${projection.rootType}: ${projection.failure}",
                    )
                }
            }
        }
        state.continuationStack.forEachIndexed { index, frame ->
            when (val projection = inFlightReferenceProjector.project(frame)) {
                is TypedEntityReferences.Projection.Complete -> referenced += projection.entityIds
                is TypedEntityReferences.Projection.Incomplete -> {
                    return IdentitySensitiveInFlightPins.Incomplete(
                        "could not traverse continuation[$index] ${projection.rootType}: ${projection.failure}",
                    )
                }
            }
        }
        return IdentitySensitiveInFlightPins.Complete(referenced)
    }

    /**
     * The components on [container] that block rewriting the slot to a different card identity,
     * by simple name and sorted.
     *
     * The safe set is derived rather than listed: build what [CardEntityFactory] would produce for
     * the definition *currently* occupying the slot, and every component that doesn't match is
     * runtime state. That keeps the check in lockstep with new definition-derived components
     * instead of drifting from a hand-maintained allowlist. Two consequences worth naming:
     *
     * - [CardComponent] is excluded, because it is exactly the identity being replaced. It is also
     *   the one component that legitimately differs from the factory output on a well-formed slot:
     *   scenario builders and pinned printings stamp their own.
     * - `RevealedToComponent` is *not* excluded, so a slot someone has already been shown is
     *   blocked. Preserving a reveal across an identity swap would leave a player holding a
     *   pointer to a card they never saw, which is the incoherence this check exists to prevent.
     *
     * Components the factory would produce that are *missing* from [container] are not blockers:
     * the rewrite regenerates them, and a zone transition legitimately strips [ControllerComponent].
     */
    fun runtimeBlockers(
        container: ComponentContainer,
        currentDefinition: CardDefinition,
        ownerId: EntityId,
    ): List<String> = runtimeBlockers(container, CardEntityFactory.create(currentDefinition, ownerId))

    /** Reuse a factory-built expectation while still checking each source container separately. */
    internal fun runtimeBlockers(container: ComponentContainer, expected: ComponentContainer): List<String> {
        return container.all()
            .mapNotNull { component ->
                if (component is CardComponent || expected.components[component::class.java] == component) null
                else component::class.simpleName ?: component::class.java.name
            }
            .sorted()
    }

    /**
     * Rebuild [entityId] as [definition], keeping the slot itself intact.
     *
     * The entity id, its position in its zone, and every other part of [state] are untouched;
     * only the components [CardEntityFactory] derives from the printed card are replaced. The
     * source's [ControllerComponent] carries over as-is — including its absence, which is how a
     * card that has left the battlefield looks.
     *
     * Callers must have cleared [runtimeBlockers] for this slot first: this function applies the
     * rewrite, it does not re-check whether the rewrite is safe.
     */
    fun rewrite(
        state: GameState,
        entityId: EntityId,
        definition: CardDefinition,
        ownerId: EntityId,
    ): GameState {
        val source = state.getEntity(entityId) ?: return state
        return state.withEntity(entityId, rewrite(source, definition, ownerId))
    }

    /**
     * The container-only form lets callers batch several rewrites into one entity-map copy.
     * As with the state form, callers must first clear [runtimeBlockers] for the source slot.
     */
    fun rewrite(
        source: ComponentContainer,
        definition: CardDefinition,
        ownerId: EntityId,
    ): ComponentContainer = rewrite(source, CardEntityFactory.create(definition, ownerId))

    /** Reuse a factory container already built and validated by the materializer. */
    internal fun rewrite(source: ComponentContainer, rebuilt: ComponentContainer): ComponentContainer =
        source.get<ControllerComponent>()
            ?.let { rebuilt.with(it) }
            ?: rebuilt.without<ControllerComponent>()
}
