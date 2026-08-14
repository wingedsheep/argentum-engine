package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.model.EntityId

/**
 * The eligibility rule of a **tap-for-generic** payment — the single rail behind every mechanic
 * shaped as *"you may tap an untapped permanent you control rather than pay {1} generic"*.
 *
 * All such mechanics share the same machinery: the chosen permanents travel in
 * [com.wingedsheep.sdk.scripting.AlternativePaymentChoice.tapForGenericPermanents], each tap
 * removes {1} of generic (never a colored pip), and the total is bounded by the generic mana in
 * the cost. The only thing that varies per mechanic is *which* permanents qualify — that is this
 * enum — plus an optional extra cap supplied by the caller (waterbend's {N}).
 *
 * Adding a further keyword of this shape (e.g. a future artifacts-and-lands variant) is one
 * entry here plus a label at the enumeration layer; it does not need a new payment field, a new
 * handler branch, or new client UI.
 */
enum class TapForGeneric(
    /** Player-facing verb, surfaced on the legal action so the UI names the payment correctly. */
    val label: String
) {
    /**
     * Improvise (CR 702.126a) — untapped **artifacts** you control only.
     */
    IMPROVISE("improvise") {
        override fun matches(projected: ProjectedState, entityId: EntityId): Boolean =
            projected.hasType(entityId, "ARTIFACT")
    },

    /**
     * Waterbend (Avatar: The Last Airbender) — untapped **artifacts or creatures** you control.
     * A superset of [IMPROVISE].
     */
    WATERBEND("waterbend") {
        override fun matches(projected: ProjectedState, entityId: EntityId): Boolean =
            projected.isCreature(entityId) || projected.hasType(entityId, "ARTIFACT")
    };

    /**
     * Whether [entityId] is of an eligible type for this payment. Types are read from
     * [projected] so animated lands and type-changing effects are honored; the caller still
     * checks zone, untapped-ness and control.
     */
    abstract fun matches(projected: ProjectedState, entityId: EntityId): Boolean
}
