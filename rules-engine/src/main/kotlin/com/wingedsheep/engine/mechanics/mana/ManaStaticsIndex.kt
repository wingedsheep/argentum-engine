package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.MultiplyManaOnSourceTap
import com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * The handful of battlefield statics [ManaSolver] has to consult for *every* candidate mana
 * source, collected in one pass instead of one pass per source.
 *
 * Five of the solver's helpers used to open with `for (id in state.getBattlefield())` and were
 * called once per candidate source, making mana enumeration O(battlefield²) — and enumeration runs
 * at every priority window. Every one of them is hunting for a static that is absent from the vast
 * majority of boards, so the fix is to look for all of them at once, up front: [build] costs one
 * battlefield walk, and each list is empty on a board with no Greenhouse / Fertile Ground /
 * Lavaleaper / Pulse of Llanowar / Shimmerwilds Growth / Virtue of Strength in play, which is
 * nearly all of them.
 *
 * Each bucket reproduces its helper's original collection rules exactly, including where those
 * rules disagree with each other:
 *
 * - [manaAbilityGrantors] skips face-down permanents (CR 708.2) and reads
 *   [RoomFaceStatics.activeStaticAbilities], so an unlocked Room face's grant counts (CR 709.5).
 * - The others read `script.staticAbilities` and do **not** skip face-down permanents.
 *
 * Those differences are pre-existing behaviour, preserved deliberately: this is a hoist, not a
 * rules change.
 */
class ManaStaticsIndex private constructor(
    /**
     * Battlefield-scope [GrantActivatedAbility] statics whose granted ability is a mana ability —
     * Clement, the Worrywort; Greenhouse. The per-source work that remains is one filter match
     * against each grantor.
     */
    val manaAbilityGrantors: List<ManaAbilityGrantor>,
    /**
     * Colors forced onto an enchanted land by an attached [OverrideEnchantedLandManaColor]
     * (Shimmerwilds Growth), keyed by the enchanted land. Later attachments in battlefield order
     * overwrite earlier ones, matching the original loop's last-write-wins.
     *
     * This is now the single source for both the solver and the mana-ability *labeller*
     * (`ManaAbilityEnumerator`), which each carried their own copy of the scan. It must stay in
     * sync with `ActivateAbilityHandler.findEnchantedLandManaColorOverride`, which resolves the
     * same override once per activation — if they disagree, the label, the affordability check and
     * the mana actually produced drift apart.
     */
    val landColorOverrideByTarget: Map<EntityId, Color>,
    /** [ReplaceLandManaColor] statics (Pulse of Llanowar) with the controller to evaluate them as. */
    val landColorReplacements: List<LandColorReplacement>,
    /** Attached [AdditionalManaOnTap] statics (Fertile Ground), keyed by the enchanted permanent. */
    val auraBonusManaByTarget: Map<EntityId, List<AuraBonusMana>>,
    /** [AdditionalManaOnSourceTap] statics (Lavaleaper, Badgermole Cub) anywhere on the battlefield. */
    val sourceTapBonuses: List<SourceTapBonus>,
    /** [MultiplyManaOnSourceTap] statics (Virtue of Strength) anywhere on the battlefield. */
    val sourceTapMultipliers: List<SourceTapMultiplier>,
) {
    /** A battlefield-scope grant of a mana ability, with the granter's projected controller. */
    data class ManaAbilityGrantor(
        val granterId: EntityId,
        val grant: GrantActivatedAbility,
        val granterControllerId: EntityId,
    )

    /** A [ReplaceLandManaColor] static, with the projected controller its filter reads as "you". */
    data class LandColorReplacement(
        val sourceId: EntityId,
        val static: ReplaceLandManaColor,
        val sourceControllerId: EntityId,
    )

    /**
     * An [AdditionalManaOnTap] on a permanent attached to a mana source. [chosenColor] is the
     * attachment's chosen color, read at index time so the solver need not re-fetch the container.
     */
    data class AuraBonusMana(
        val auraId: EntityId,
        val static: AdditionalManaOnTap,
        val chosenColor: Color?,
    )

    /** An [AdditionalManaOnSourceTap] static, with the projected controller its filter reads as "you". */
    data class SourceTapBonus(
        val sourceId: EntityId,
        val static: AdditionalManaOnSourceTap,
        val sourceControllerId: EntityId,
    )

    /** A [MultiplyManaOnSourceTap] static, with the projected controller its filter reads as "you". */
    data class SourceTapMultiplier(
        val sourceId: EntityId,
        val static: MultiplyManaOnSourceTap,
        val sourceControllerId: EntityId,
    )

    /** True when no bucket holds anything — the overwhelmingly common case. */
    val isEmpty: Boolean =
        manaAbilityGrantors.isEmpty() &&
            landColorOverrideByTarget.isEmpty() &&
            landColorReplacements.isEmpty() &&
            auraBonusManaByTarget.isEmpty() &&
            sourceTapBonuses.isEmpty() &&
            sourceTapMultipliers.isEmpty()

    companion object {
        val EMPTY =
            ManaStaticsIndex(emptyList(), emptyMap(), emptyList(), emptyMap(), emptyList(), emptyList())

        /**
         * Walk the battlefield once and bucket every mana-relevant static on it.
         *
         * Entities with no card definition, and statics of no interest here, cost one `when`
         * branch each; nothing is retained for an ordinary land or creature.
         */
        fun build(state: GameState, cardRegistry: CardRegistry): ManaStaticsIndex {
            var grantors: MutableList<ManaAbilityGrantor>? = null
            var overrides: MutableMap<EntityId, Color>? = null
            var replacements: MutableList<LandColorReplacement>? = null
            var auraBonuses: MutableMap<EntityId, MutableList<AuraBonusMana>>? = null
            var sourceTapBonuses: MutableList<SourceTapBonus>? = null
            var sourceTapMultipliers: MutableList<SourceTapMultiplier>? = null

            val projected = state.projectedState

            for (permanentId in state.getBattlefield()) {
                val container = state.getEntity(permanentId) ?: continue
                val card = container.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val attachedTo = container.get<AttachedToComponent>()?.targetId
                val faceDown = container.has<FaceDownComponent>()

                // Bucket 1 — battlefield-scope mana-ability grants. Face-down permanents have no
                // abilities (CR 708.2); an unlocked Room face's statics do count (CR 709.5).
                if (!faceDown) {
                    for (ability in RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                        if (ability !is GrantActivatedAbility) continue
                        if (ability.filter.scope !is Scope.Battlefield) continue
                        if (!ability.ability.isManaAbility) continue
                        val granterController = projected.getController(permanentId) ?: continue
                        (grantors ?: mutableListOf<ManaAbilityGrantor>().also { grantors = it })
                            .add(ManaAbilityGrantor(permanentId, ability, granterController))
                    }
                }

                // Buckets 2–5 — read the printed static list, face-down permanents included, as the
                // helpers being replaced did, plus any statics *granted* to this permanent at
                // runtime and recorded in `GameState.grantedStaticAbilities`. That is the same
                // point-of-use read the combat checks do, and it is what lets a durational grant
                // reach the mana path at all: the layer projector does not carry granted statics,
                // so a `{U}: … until end of turn` mana rule (Deep Water) has nowhere else to live.
                val grantedHere = state.grantedStaticAbilities
                    .filter { it.entityId == permanentId }
                    .map { it.ability }
                val staticsHere =
                    if (grantedHere.isEmpty()) cardDef.script.staticAbilities
                    else cardDef.script.staticAbilities + grantedHere
                for (static in staticsHere) {
                    when (static) {
                        is OverrideEnchantedLandManaColor -> {
                            if (attachedTo == null) continue
                            val color = static.color ?: container.chosenColor() ?: continue
                            (overrides ?: mutableMapOf<EntityId, Color>().also { overrides = it })[attachedTo] = color
                        }

                        is ReplaceLandManaColor -> {
                            val controller = projected.getController(permanentId) ?: continue
                            (replacements ?: mutableListOf<LandColorReplacement>().also { replacements = it })
                                .add(LandColorReplacement(permanentId, static, controller))
                        }

                        is AdditionalManaOnTap -> {
                            if (attachedTo == null) continue
                            val bonuses = auraBonuses
                                ?: mutableMapOf<EntityId, MutableList<AuraBonusMana>>().also { auraBonuses = it }
                            bonuses.getOrPut(attachedTo) { mutableListOf() }
                                .add(AuraBonusMana(permanentId, static, container.chosenColor()))
                        }

                        is AdditionalManaOnSourceTap -> {
                            val controller = projected.getController(permanentId) ?: continue
                            (sourceTapBonuses ?: mutableListOf<SourceTapBonus>().also { sourceTapBonuses = it })
                                .add(SourceTapBonus(permanentId, static, controller))
                        }

                        is MultiplyManaOnSourceTap -> {
                            val controller = projected.getController(permanentId) ?: continue
                            (
                                sourceTapMultipliers
                                    ?: mutableListOf<SourceTapMultiplier>().also { sourceTapMultipliers = it }
                                ).add(SourceTapMultiplier(permanentId, static, controller))
                        }

                        else -> {}
                    }
                }
            }

            // Granted statics, not just printed ones. A *spell* can create a mana bonus that no
            // permanent carries — High Tide's "until end of turn, whenever a player taps an Island
            // for mana, that player adds an additional {U}" — by granting the static to its own
            // controller for the turn. Only the battlefield-wide bonus shapes are read here; the
            // attachment-scoped ones (AdditionalManaOnTap) need a host to be attached to.
            for (granted in state.grantedStaticAbilities) {
                when (val static = granted.ability) {
                    is AdditionalManaOnSourceTap -> {
                        // The filter's "you" is the grant holder — a player entity for a
                        // spell-created grant, a permanent's controller otherwise.
                        val controller = projected.getController(granted.entityId) ?: granted.entityId
                        (sourceTapBonuses ?: mutableListOf<SourceTapBonus>().also { sourceTapBonuses = it })
                            .add(SourceTapBonus(granted.entityId, static, controller))
                    }
                    is MultiplyManaOnSourceTap -> {
                        val controller = projected.getController(granted.entityId) ?: granted.entityId
                        (
                            sourceTapMultipliers
                                ?: mutableListOf<SourceTapMultiplier>().also { sourceTapMultipliers = it }
                            ).add(SourceTapMultiplier(granted.entityId, static, controller))
                    }
                    else -> {}
                }
            }

            if (grantors == null && overrides == null && replacements == null &&
                auraBonuses == null && sourceTapBonuses == null && sourceTapMultipliers == null
            ) {
                return EMPTY
            }

            return ManaStaticsIndex(
                manaAbilityGrantors = grantors ?: emptyList(),
                landColorOverrideByTarget = overrides ?: emptyMap(),
                landColorReplacements = replacements ?: emptyList(),
                auraBonusManaByTarget = auraBonuses ?: emptyMap(),
                sourceTapBonuses = sourceTapBonuses ?: emptyList(),
                sourceTapMultipliers = sourceTapMultipliers ?: emptyList(),
            )
        }
    }
}
