package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import kotlinx.serialization.Serializable

/**
 * Face-down status for morph/manifest/disguise/cloak.
 */
@Serializable
data object FaceDownComponent : Component

/**
 * One way a face-down permanent can be turned face up — a cost plus the mechanic whose rule
 * defines the procedure (CR 116.2b special action).
 *
 * A permanent normally has exactly one, but a card that was manifested or cloaked while *also*
 * having morph or disguise has two: CR 701.40c/d and 701.58c/d let its controller choose either
 * the manifest/cloak procedure (pay the card's mana cost) or the morph/disguise one (pay the
 * printed morph/disguise cost).
 */
@Serializable
data class TurnUpProcedure(
    /** Cost paid as the special action is taken. */
    val cost: PayCost,
    /**
     * Which mechanic's turn-up rule this is: [FaceDownMode.MORPH] (CR 702.37e),
     * [FaceDownMode.DISGUISE] (CR 702.168d), [FaceDownMode.MANIFEST] (CR 701.40b) or
     * [FaceDownMode.CLOAK] (CR 701.58b). Never [FaceDownMode.HIDDEN] — that mode has no procedure.
     */
    val mechanic: FaceDownMode,
    /** Effect applied as the permanent is turned face up this way (megamorph's +1/+1 counter). */
    val faceUpEffect: Effect? = null
) {
    /** Player-facing mechanic name, e.g. "Disguise". */
    val label: String get() = mechanic.name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * The turn-up procedures and original card identity for a face-down permanent.
 *
 * Named for morph because that was the first mechanic to need it, but it is mechanic-agnostic:
 * [com.wingedsheep.engine.handlers.effects.FaceDownTurnUp] derives it from the card and the
 * [FaceDownMode] under which the permanent entered, and everything downstream — the legal action,
 * the handler, the client DTO — reads only this component. Absent when the permanent has no way
 * to be turned face up at all.
 */
@Serializable
data class MorphDataComponent(
    /** Non-empty; the first entry is the permanent's primary/default procedure. */
    val procedures: List<TurnUpProcedure>,
    val originalCardDefinitionId: String
) : Component {
    init {
        require(procedures.isNotEmpty()) { "MorphDataComponent needs at least one turn-up procedure" }
    }

    /** Single-procedure convenience constructor — the morph shape this component started as. */
    constructor(
        morphCost: PayCost,
        originalCardDefinitionId: String,
        faceUpEffect: Effect? = null
    ) : this(
        listOf(TurnUpProcedure(morphCost, FaceDownMode.MORPH, faceUpEffect)),
        originalCardDefinitionId
    )

    /** Cost of the primary procedure. */
    val morphCost: PayCost get() = procedures.first().cost

    /** Face-up effect of the primary procedure. */
    val faceUpEffect: Effect? get() = procedures.first().faceUpEffect

    /** True when any procedure comes from a printed morph ability (CR 702.37). */
    val hasMorphProcedure: Boolean get() = procedures.any { it.mechanic == FaceDownMode.MORPH }
}

/**
 * Marks a card as having a morph keyword ability.
 * Present on the entity regardless of zone, so filters can check
 * whether a card in hand/library/graveyard has morph.
 */
@Serializable
data object HasMorphAbilityComponent : Component

/**
 * Which face-down mechanic put this permanent onto the battlefield face down (CR 708.6: which
 * ability or rule made a permanent face down is public information, and paper Magic represents
 * morph, manifest and disguise/cloak with visually distinct helper cards).
 *
 * Two things read it: the face-down art the client shows every viewer, and the ward {2} that
 * disguise and cloak list among the face-down permanent's characteristics
 * ([FaceDownMode.faceDownWard], CR 702.168a / 701.58a). Present only while the permanent is face
 * down — turning it face up ends the characteristic-defining effect and drops the marker.
 */
@Serializable
data class FaceDownModeComponent(val mode: FaceDownMode) : Component
