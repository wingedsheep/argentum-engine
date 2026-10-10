package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * State carried through pipeline effect execution (Gather → Select → Move).
 *
 * This groups the fields that are only relevant during pipeline effect chains,
 * keeping [EffectContext] focused on core effect execution concerns.
 */
@Serializable
data class PipelineState(
    /** Named card collections for pipeline effects (GatherCards → SelectFromCollection → MoveCollection) */
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    /** Named targets map for BoundVariable resolution (target name -> chosen target) */
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    /** Named values chosen by the player during pipeline execution (e.g., creature type, color). */
    val chosenValues: Map<String, String> = emptyMap(),
    /** Named numeric values stored by pipeline effects (e.g., cards not drawn). */
    val storedNumbers: Map<String, Int> = emptyMap(),
    /** Named string lists stored by pipeline effects (e.g., chosen creature types). */
    val storedStringLists: Map<String, List<String>> = emptyMap(),
    /**
     * Named lists of subtype sets produced by `GatherSubtypesEffect`. Each entry is
     * `List<Set<String>>` — one subtype set per source entity in the order they were
     * gathered. Consumed by `CardPredicate.HasSubtypeInEachStoredGroup`.
     */
    val storedSubtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
) {
    companion object {
        val EMPTY = PipelineState()

        /** Reserved metadata published by ChooseSpell alongside its selected card collection. */
        fun spellFaceKey(collection: String): String = "$collection:spellFace"

        /**
         * Reserved metadata collection published by a SelectFromCollection decision alongside its
         * remainder: the remainder cards that decision displayed to [playerId]. A later move that
         * would pause only so that player can *look* at those cards (a one-card "put back on top")
         * reads it to skip a prompt showing a card they just saw — scry 1 / surveil 1.
         */
        fun shownKey(collection: String, playerId: EntityId): String = "__shown:${playerId.value}:$collection"

        /**
         * Reserved metadata collection published by a library-searching gather
         * (`GatherCardsEffect.search`) alongside [collection]: the cards that search found. A
         * SelectFromCollection over [collection] reads it to mark its decision as a library
         * search ([com.wingedsheep.engine.core.SelectCardsDecision.librarySearch]). Empty when a
         * later non-search gather reused the name.
         */
        fun searchedKey(collection: String): String = "__searched:$collection"

        /**
         * Pipeline collection name under which a batch trigger seeds the entities it captured
         * (the matching permanents in a `PermanentsEnteredEvent` batch). Aliases the SDK-side
         * contract [com.wingedsheep.sdk.scripting.effects.IterationSpace.TRIGGER_CAPTURED_COLLECTION]
         * so card definitions (which only see the SDK) and the engine name the same collection.
         */
        const val TRIGGER_CAPTURED_COLLECTION =
            com.wingedsheep.sdk.scripting.effects.IterationSpace.TRIGGER_CAPTURED_COLLECTION
    }
}
