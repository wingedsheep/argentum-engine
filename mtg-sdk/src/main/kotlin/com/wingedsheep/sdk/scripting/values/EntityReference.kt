package com.wingedsheep.sdk.scripting.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight sealed interface representing "which entity to read a property from."
 *
 * Intentionally slim — not reusing [com.wingedsheep.sdk.scripting.targets.EffectTarget] which
 * has 15+ variants for effect resolution. EntityReference only needs the small set of
 * "read one entity's numeric property" use cases.
 */
@Serializable
sealed interface EntityReference {
    val description: String

    /** The ability's source permanent. */
    @SerialName("Source")
    @Serializable
    data object Source : EntityReference {
        override val description: String = "it"
    }

    /** A cast-time target by index. */
    @SerialName("Target")
    @Serializable
    data class Target(val index: Int = 0) : EntityReference {
        override val description: String = "target"
    }

    /** A creature sacrificed as cost, by index. */
    @SerialName("Sacrificed")
    @Serializable
    data class Sacrificed(val index: Int = 0) : EntityReference {
        override val description: String = "the sacrificed creature"
    }

    /** The entity that caused the trigger to fire. */
    @SerialName("Triggering")
    @Serializable
    data object Triggering : EntityReference {
        override val description: String = "the triggering creature"
    }

    /**
     * The creature an Aura/Equipment ability's source is attached to. Resolves via the
     * source permanent's attachment relationship — the same creature
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.EnchantedCreature] points at.
     * Used for "enchanted creature deals damage equal to its power" style auras.
     */
    @SerialName("EnchantedCreature")
    @Serializable
    data object EnchantedCreature : EntityReference {
        override val description: String = "enchanted creature"
    }

    /** A permanent tapped as cost, by index. */
    @SerialName("TappedAsCost")
    @Serializable
    data class TappedAsCost(val index: Int = 0) : EntityReference {
        override val description: String = "the tapped creature"
    }

    /** The entity being modified by a continuous effect during state projection. */
    @SerialName("AffectedEntity")
    @Serializable
    data object AffectedEntity : EntityReference {
        override val description: String = "it"
    }

    /** The current entity being iterated in a ForEachInGroupEffect. */
    @SerialName("IterationEntity")
    @Serializable
    data object IterationEntity : EntityReference {
        override val description: String = "it"
    }

    /**
     * An entity recorded into the spell's pipeline `storedCollections` by an
     * additional-cost step (typically
     * [com.wingedsheep.sdk.scripting.AdditionalCost.ChooseEntity]).
     *
     * Reads `EffectContext.pipeline.storedCollections[collectionName][index]`.
     * When paired with `DynamicAmount.EntityProperty(...)`, the evaluator
     * consults `EffectContext.chosenEntitySnapshots` for last-known-info
     * fallback if the entity has left the battlefield since cost-pay time
     * (Rule 112.7a / 608.2h), mirroring the existing
     * [Sacrificed] / [TappedAsCost] LKI paths.
     *
     * @property collectionName The `storeAs` key under which the cost step
     *   recorded the chosen entity.
     * @property index Position within the collection (defaults to the first
     *   chosen entity; useful when a future cost step records multiple).
     */
    @SerialName("FromCostStorage")
    @Serializable
    data class FromCostStorage(
        val collectionName: String,
        val index: Int = 0,
    ) : EntityReference {
        override val description: String = "the chosen card"
    }

    /**
     * The Army chosen by the most recent Amass step in the current resolution pipeline —
     * "the amassed Army," whether or not it received counters (CR 701.47c).
     *
     * Composed with [com.wingedsheep.sdk.scripting.effects.CompositeEffect] of
     * `[AmassEffect, ...]` so a follow-up effect can read the just-amassed Army's
     * power/toughness/etc. via `DynamicAmount.EntityProperty(AmassedArmy, ...)`.
     *
     * Examples:
     * - Foray of Orcs: "Amass Orcs 2, then ~ deals damage to any target equal to the
     *   amassed Army's power."
     * - Surrounded by Orcs: "Amass Orcs 1, then target player mills cards equal to the
     *   amassed Army's power."
     *
     * The engine writes the chosen Army's id into
     * `EffectContext.pipeline.storedCollections[STORAGE_KEY]` after Amass resolves and
     * carries the slot across composite sub-effects (and through the multi-Army choice
     * continuation), so the next sibling effect's evaluator can read it.
     */
    @SerialName("AmassedArmy")
    @Serializable
    data object AmassedArmy : EntityReference {
        override val description: String = "the amassed Army"

        /**
         * Reserved pipeline-storage key — engine writers and SDK readers must agree
         * on this string so neither side has to import the other's module.
         */
        const val STORAGE_KEY: String = "__amassed_army"
    }
}
