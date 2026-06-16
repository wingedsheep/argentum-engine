package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import kotlinx.serialization.Serializable

/**
 * Represents a "floating" continuous effect that exists independently of permanents.
 *
 * Floating effects are created by spells and abilities that resolve and create
 * temporary modifications (like Giant Growth giving +3/+3 until end of turn).
 *
 * Unlike static abilities (which are tied to permanents), floating effects:
 * - Exist in the game state independently
 * - Have a duration that determines when they expire
 * - Are cleaned up during the cleanup step (for EndOfTurn) or other appropriate times
 *
 * ## Example
 * When Giant Growth resolves targeting a creature:
 * 1. The spell moves to graveyard
 * 2. An ActiveFloatingEffect is created with Duration.EndOfTurn
 * 3. StateProjector reads this effect and applies +3/+3
 * 4. At cleanup, TurnManager removes effects with EndOfTurn duration
 */
@Serializable
data class ActiveFloatingEffect(
    /** Unique identifier for this floating effect */
    val id: EntityId,

    /** The continuous effect data (layer, modification, affected entities) */
    val effect: FloatingEffectData,

    /** How long this effect lasts */
    val duration: Duration,

    /** The source that created this effect (for tracking/display) */
    val sourceId: EntityId?,

    /** The source's name (for UI display) */
    val sourceName: String? = null,

    /** Who controls this effect (for "your creatures" type effects) */
    val controllerId: EntityId,

    /** Timestamp when this effect was created (for ordering) */
    val timestamp: Long
)

/**
 * Data for a floating continuous effect.
 *
 * This is separate from ContinuousEffectData because floating effects
 * store their affected entities directly (resolved at creation time)
 * rather than using a filter that's re-evaluated.
 */
@Serializable
data class FloatingEffectData(
    /** Which layer this effect applies in */
    val layer: Layer,

    /** Sublayer for layer 7 (P/T) effects */
    val sublayer: Sublayer? = null,

    /** What modification this effect makes */
    val modification: SerializableModification,

    /** The specific entities this effect applies to (resolved at creation) */
    val affectedEntities: Set<EntityId>,

    /**
     * Optional dynamic filter for resolving affected entities at projection time.
     * When set, the StateProjector re-evaluates which entities match on every projection
     * instead of using the locked-in [affectedEntities] set. This is required for
     * rule-modifying effects like "creatures can't block this turn" (Rule 611.2c)
     * which apply to all matching objects, including those entering later.
     */
    val dynamicGroupFilter: GroupFilter? = null
)

/**
 * Serializable version of Modification for floating effects.
 *
 * This mirrors the Modification sealed interface but is serializable
 * for storage in GameState.
 */
@Serializable
sealed interface SerializableModification {
    @Serializable
    data class SetPowerToughness(val power: Int, val toughness: Int) : SerializableModification

    @Serializable
    data class SetPower(val power: Int) : SerializableModification

    @Serializable
    data class ModifyPowerToughness(val powerMod: Int, val toughnessMod: Int) : SerializableModification

    @Serializable
    data object SwitchPowerToughness : SerializableModification

    @Serializable
    data class GrantKeyword(val keyword: String) : SerializableModification

    @Serializable
    data class RemoveKeyword(val keyword: String) : SerializableModification

    @Serializable
    data class ChangeColor(val colors: Set<String>) : SerializableModification

    @Serializable
    data class AddColor(val colors: Set<String>) : SerializableModification

    @Serializable
    data class AddType(val type: String) : SerializableModification

    @Serializable
    data class RemoveType(val type: String) : SerializableModification

    @Serializable
    data class ChangeController(val newControllerId: EntityId) : SerializableModification

    /**
     * Combat restriction: this creature must be blocked by all creatures able to block it.
     * Used by Alluring Scent, Taunting Elf, and similar Lure effects.
     */
    @Serializable
    data object MustBeBlockedByAll : SerializableModification

    /**
     * Combat restriction: at least one creature must block this creature if able.
     * Used by Gaea's Protector and similar "must be blocked if able" effects.
     * Unlike MustBeBlockedByAll, only one blocker is required, not all.
     */
    @Serializable
    data object MustBeBlockedIfAble : SerializableModification

    /**
     * Combat restriction: a specific creature must block a specific attacker if able.
     * Used by Provoke. The affectedEntities contains the blocker; attackerId is the attacker.
     */
    @Serializable
    data class MustBlockSpecificAttacker(val attackerId: EntityId) : SerializableModification

    /**
     * Damage prevention: prevent all combat damage that would be dealt to a player
     * by attacking creatures this turn.
     * Used by Deep Wood and similar effects.
     */
    @Serializable
    data object PreventDamageFromAttackingCreatures : SerializableModification

    /**
     * Damage prevention: prevent all combat damage that would be dealt this turn.
     * Used by Leery Fogbeast and similar effects.
     */
    @Serializable
    data object PreventAllCombatDamage : SerializableModification

    /**
     * Blocking restriction: creature can only be blocked by creatures matching [blockerFilter]
     * (floating, one-shot variant). The dynamic counterpart to the static
     * [com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy] ability — unlike
     * [CantBeBlockedExceptByColor] it maps to a real Layer.ABILITY [Modification.CantBeBlockedExceptBy]
     * so the existing `CantBeBlockedExceptByRule` enforces it through projected state.
     * Used by Resilient Roadrunner's "{3}: can't be blocked except by creatures with haste".
     */
    @Serializable
    data class CantBeBlockedExceptBy(val blockerFilter: GameObjectFilter) : SerializableModification

    /**
     * Blocking restriction: creature can only be blocked by creatures of a specific color.
     * Used by Dread Charge and similar effects.
     */
    @Serializable
    data class CantBeBlockedExceptByColor(val color: String) : SerializableModification

    /**
     * Blocking restriction: creature can't be blocked by creatures of a specific color.
     * Used by Skrelv, Defector Mite and similar effects.
     */
    @Serializable
    data class CantBeBlockedByColor(val color: String) : SerializableModification

    /**
     * Attack/block tax: creature can't attack or block unless its controller pays
     * {manaCostPer} for each creature of the specified type on the battlefield.
     * Used by Whipgrass Entangler and similar effects.
     * Checked directly by CombatManager during attack/block declaration.
     */
    @Serializable
    data class AttackBlockTaxPerCreatureType(
        val creatureType: String,
        val manaCostPer: String
    ) : SerializableModification

    /**
     * Damage reflection: when an attacking creature deals combat damage to the protected player,
     * it deals that much damage to its controller.
     * Used by Harsh Justice.
     */
    @Serializable
    data class ReflectCombatDamage(val protectedPlayerId: String) : SerializableModification

    /**
     * Grant protection from a specific color.
     * Used by Akroma's Blessing and similar effects.
     */
    @Serializable
    data class GrantProtectionFromColor(val color: String) : SerializableModification

    /**
     * Damage prevention shield: prevent the next X damage that would be dealt to target creature/player.
     * Used by Battlefield Medic and similar effects.
     * The shield is consumed as damage is dealt and removed when fully used or at end of turn.
     */
    @Serializable
    data class PreventNextDamage(
        val remainingAmount: Int,
        /** If set, only prevents damage from this specific source (used for CR 615.7 prevention distribution) */
        val onlyFromSource: EntityId? = null
    ) : SerializableModification

    /**
     * Damage prevention: prevent all damage that would be dealt to the affected entities.
     */
    @Serializable
    data object PreventAllDamageTo : SerializableModification

    /**
     * Regeneration shield: the next time the target permanent would be destroyed this turn,
     * instead tap it, remove all damage, and remove it from combat.
     * Used by Boneknitter and similar effects.
     */
    @Serializable
    data object RegenerationShield : SerializableModification

    /**
     * Remove-damage destruction shield (Pyramids): the next time the target permanent
     * would be destroyed this turn, remove all damage marked on it instead. Unlike a
     * regeneration shield, the permanent is NOT tapped and is NOT removed from combat —
     * the only side-effect of the replacement is clearing marked damage.
     *
     * Consumed once on its first triggered destruction; expires at end of turn.
     */
    @Serializable
    data object RemoveDamageShield : SerializableModification

    /**
     * Marks a permanent as unable to be regenerated this turn.
     * Used by Smother, Cruel Revival, Wrath of God (noRegenerate), etc.
     */
    @Serializable
    data object CantBeRegenerated : SerializableModification

    /**
     * Replace all creature subtypes with the given set of subtypes.
     * Used by Trickery Charm: "Target creature becomes the creature type of your choice until end of turn."
     */
    @Serializable
    data class SetCreatureSubtypes(val subtypes: Set<String>) : SerializableModification

    /**
     * Add a creature subtype in addition to existing types.
     * Used by Amoeboid Changeling, Conspiracy, etc.
     */
    @Serializable
    data class AddSubtype(val subtype: String) : SerializableModification

    /**
     * Set a permanent's basic land subtypes, replacing all existing land subtypes
     * (Rule 305.7). The one-shot counterpart to the [SetEnchantedLandType] static
     * ability. Used by "target land becomes the basic land type of your choice"
     * effects (e.g., Dream Thrush).
     */
    @Serializable
    data class SetBasicLandTypes(val subtypes: Set<String>) : SerializableModification

    /**
     * Replace ALL card types with the given set, leaving supertypes and subtypes intact
     * (Layer 4). One-shot floating counterpart to the [com.wingedsheep.sdk.scripting.TransformPermanent]
     * static ability's `setCardTypes`. Used by "becomes a [type] and loses all other card
     * types" effects on a returned permanent — e.g. Vraska, the Silencer turning a dead
     * creature into a Treasure artifact.
     */
    @Serializable
    data class SetCardTypes(val types: Set<String>) : SerializableModification

    /**
     * Replace ALL subtypes with the given set (Layer 4). One-shot floating counterpart to
     * [com.wingedsheep.sdk.scripting.TransformPermanent]'s `setSubtypes`. Pair with
     * [SetCardTypes] for "becomes a colorless Treasure artifact" (Vraska, the Silencer).
     */
    @Serializable
    data class SetAllSubtypes(val subtypes: Set<String>) : SerializableModification

    /**
     * Add every creature type in addition to existing types, without granting the
     * Changeling keyword. Used by cards like Stalactite Dagger that say "is all
     * creature types" but don't print the Changeling keyword.
     */
    @Serializable
    data object AddAllCreatureTypes : SerializableModification

    /**
     * Attack restriction: creature can't attack this turn.
     * Used by CantAttackGroupEffect and similar effects.
     */
    @Serializable
    data object SetCantAttack : SerializableModification

    /**
     * Blocking restriction: creature can't block this turn.
     * Used by Wave of Indifference and similar effects.
     */
    @Serializable
    data object SetCantBlock : SerializableModification

    /**
     * Suspect status: marks a permanent as suspected.
     * Applied atomically with GrantKeyword(MENACE) and SetCantBlock by SuspectEffectHandler.
     * Stored as a distinct modification so future cards can query the status independently.
     */
    @Serializable
    data object SetSuspected : SerializableModification

    /**
     * Damage prevention: prevent all damage that affected creature(s) would deal this turn.
     * Used by Chain of Silence and similar effects.
     * The affected entities are the creatures whose damage is prevented.
     */
    @Serializable
    data object PreventAllDamageDealtBy : SerializableModification

    /**
     * Damage redirection shield: the next time damage would be dealt to any of the
     * affected entities this turn, redirect that damage to the specified target instead.
     * Used by Glarecaster and similar effects.
     * The shield is consumed after the first redirection and removed.
     */
    @Serializable
    data class RedirectNextDamage(
        val redirectToId: EntityId,
        /** If set, only redirect up to this many damage. Null = redirect all (Glarecaster). */
        val amount: Int? = null
    ) : SerializableModification

    /**
     * Unified draw replacement shield: the next time the affected player would draw a card
     * this turn, execute the stored [replacementEffect] instead.
     * Used by the entire "Words of" cycle (Words of Worship, Wind, War, Waste, Wilding).
     * The shield is consumed after the first draw replacement and removed.
     *
     * @property replacementEffect The effect to execute instead of the draw
     * @property targets Targets chosen at activation time (e.g., Words of War's damage target)
     * @property namedTargets Named targets chosen at activation time (for BoundVariable resolution)
     * @property sourceId The source permanent that created this shield
     * @property sourceName The source's name (for UI display)
     */
    @Serializable
    data class ReplaceDrawWithEffect(
        val replacementEffect: Effect,
        val targets: List<ChosenTarget> = emptyList(),
        val namedTargets: Map<String, ChosenTarget> = emptyMap(),
        val sourceId: EntityId? = null,
        val sourceName: String? = null,
        /**
         * The {X} chosen when the replacement was set up, captured so the replacement effect can
         * still read `DynamicAmount.XValue` at draw time — the original activation context is gone
         * by then (Aladdin's Lamp: "look at the top X cards"). Null when no X was involved.
         */
        val xValue: Int? = null
    ) : SerializableModification

    /**
     * Damage prevention shield: the next time a creature of the specified type would deal
     * damage to the affected player this turn, prevent that damage.
     * Used by Circle of Solace and similar effects.
     * The shield is consumed after preventing one damage instance and removed.
     */
    @Serializable
    data class PreventNextDamageFromCreatureType(
        val creatureType: String
    ) : SerializableModification

    /**
     * Death replacement: if the affected creature would die this turn, exile it instead.
     * Used by Carbonize and similar effects.
     */
    @Serializable
    data object ExileOnDeath : SerializableModification

    /**
     * Death trigger: when the affected creature dies this turn, exile its controller's graveyard.
     * Used by Burn Away and similar effects.
     */
    @Serializable
    data object ExileControllerGraveyardOnDeath : SerializableModification

    /**
     * Damage prevention: prevent all combat damage that creatures matching the filter would deal this turn.
     * Used by Frontline Strategist and similar effects.
     * The filter is stored so that creature type is checked at the time damage would be dealt.
     */
    @Serializable
    data class PreventCombatDamageFromGroup(
        val filter: GameObjectFilter
    ) : SerializableModification

    /**
     * Damage prevention: prevent all combat damage that would be dealt to and dealt by
     * the affected creature this turn.
     * Used by Deftblade Elite and similar effects.
     */
    @Serializable
    data object PreventCombatDamageToAndBy : SerializableModification

    /**
     * Combat damage redirection: the next time the affected creature would deal combat damage
     * this turn, that damage is dealt to its controller instead.
     * Used by Goblin Psychopath and similar effects.
     * The shield is consumed after the first combat damage event.
     */
    @Serializable
    data object RedirectCombatDamageToController : SerializableModification

    /**
     * Remove all abilities from the affected creature(s).
     * Used by Merfolk Trickster and similar effects.
     */
    @Serializable
    data object RemoveAllAbilities : SerializableModification

    /**
     * Single-instance prevention shield tied to a chosen source: the next time [damageSourceId]
     * would deal damage to the affected player this turn, prevent that damage. The shield is
     * consumed after preventing one damage instance and emits a
     * [com.wingedsheep.engine.core.DamagePreventedEvent] carrying [linkId], which fires the linked
     * "when damage is prevented this way, …" delayed triggered ability on the stack (Deflecting Palm,
     * New Way Forward). The payoff is that ordinary triggered ability — not baked into the shield.
     *
     * @property damageSourceId The chosen source whose damage will be prevented
     * @property linkId The id of the delayed triggered ability that fires when this shield prevents damage
     */
    @Serializable
    data class PreventNextDamageFromChosenSourceShield(
        val damageSourceId: EntityId,
        val linkId: String,
        /**
         * When false, the chosen source's damage is not actually prevented — it is still dealt in
         * full — but the shield is still consumed and its linked reaction still fires with the
         * captured amount (Eye for an Eye). Defaults to true (ordinary deflection).
         */
        val preventDamage: Boolean = true
    ) : SerializableModification

    /**
     * Damage prevention shield tied to a single chosen source: prevent all damage that the
     * source [damageSourceId] would deal to the affected entities this turn (not consumed —
     * it persists for the full turn). Used by Samite Ministration.
     *
     * @property damageSourceId The chosen source whose damage is prevented
     * @property gainLifeFromColors If non-empty, whenever damage from a source of one of these
     *   colors (enum names, e.g. "BLACK", "RED") is prevented by this shield, the affected player
     *   gains that much life.
     */
    @Serializable
    data class PreventAllDamageFromSource(
        val damageSourceId: EntityId,
        val gainLifeFromColors: Set<String> = emptySet()
    ) : SerializableModification

    /**
     * Turn-duration noncombat-damage amplification (CR 616): every source the effect's controller
     * controls deals [bonus] additional noncombat damage to any permanent or player this turn.
     * Combat damage is unaffected; there is no opponent restriction. Read directly during damage
     * resolution by `DamageUtils.applyStaticDamageAmplification` (not a layer modification). The
     * effect's [ActiveFloatingEffect.controllerId] identifies whose sources benefit. Installed by
     * Taii Wakeen, Perfect Shot's "{X}, {T}" ability with [bonus] = the X paid.
     */
    @Serializable
    data class AmplifyNoncombatDamage(val bonus: Int) : SerializableModification
}

/**
 * Convert SerializableModification to Modification for the projector.
 */
fun SerializableModification.toModification(): Modification = when (this) {
    is SerializableModification.SetPowerToughness -> Modification.SetPowerToughness(power, toughness)
    is SerializableModification.SetPower -> Modification.SetPower(power)
    is SerializableModification.ModifyPowerToughness -> Modification.ModifyPowerToughness(powerMod, toughnessMod)
    is SerializableModification.SwitchPowerToughness -> Modification.SwitchPowerToughness(EntityId(""))
    is SerializableModification.GrantKeyword -> Modification.GrantKeyword(keyword)
    is SerializableModification.RemoveKeyword -> Modification.RemoveKeyword(keyword)
    is SerializableModification.ChangeColor -> Modification.ChangeColor(colors)
    is SerializableModification.AddColor -> Modification.AddColor(colors)
    is SerializableModification.AddType -> Modification.AddType(type)
    is SerializableModification.RemoveType -> Modification.RemoveType(type)
    is SerializableModification.ChangeController -> Modification.ChangeController(newControllerId)
    // MustBeBlockedByAll / MustBeBlockedIfAble don't map to a layer modification - checked by CombatManager directly
    is SerializableModification.MustBeBlockedByAll -> Modification.NoOp
    is SerializableModification.MustBeBlockedIfAble -> Modification.NoOp
    is SerializableModification.MustBlockSpecificAttacker -> Modification.NoOp
    // PreventDamageFromAttackingCreatures doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.PreventDamageFromAttackingCreatures -> Modification.NoOp
    // CantBeBlockedExceptBy maps to the real Layer.ABILITY modification, so it flows through the
    // projector into `cantBeBlockedExceptByFilters` and is enforced by CantBeBlockedExceptByRule.
    is SerializableModification.CantBeBlockedExceptBy -> Modification.CantBeBlockedExceptBy(blockerFilter)
    // CantBeBlockedExceptByColor doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.CantBeBlockedExceptByColor -> Modification.NoOp
    // CantBeBlockedByColor doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.CantBeBlockedByColor -> Modification.NoOp
    // AttackBlockTaxPerCreatureType doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.AttackBlockTaxPerCreatureType -> Modification.NoOp
    // ReflectCombatDamage doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.ReflectCombatDamage -> Modification.NoOp
    is SerializableModification.GrantProtectionFromColor -> Modification.GrantProtectionFromColor(color)
    // PreventNextDamage doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.PreventNextDamage -> Modification.NoOp
    // PreventAllDamageTo doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.PreventAllDamageTo -> Modification.NoOp
    // RegenerationShield doesn't map to a layer modification - it's checked during destruction
    is SerializableModification.RegenerationShield -> Modification.NoOp
    // RemoveDamageShield doesn't map to a layer modification - it's checked during destruction
    is SerializableModification.RemoveDamageShield -> Modification.NoOp
    // CantBeRegenerated doesn't map to a layer modification - it's checked during destruction
    is SerializableModification.CantBeRegenerated -> Modification.NoOp
    // PreventAllCombatDamage doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.PreventAllCombatDamage -> Modification.NoOp
    is SerializableModification.SetCreatureSubtypes -> Modification.SetCreatureSubtypes(subtypes)
    is SerializableModification.AddSubtype -> Modification.AddSubtype(subtype)
    is SerializableModification.SetCardTypes -> Modification.SetCardTypes(types)
    is SerializableModification.SetAllSubtypes -> Modification.SetAllSubtypes(subtypes)
    is SerializableModification.SetBasicLandTypes -> Modification.SetBasicLandTypes(subtypes)
    is SerializableModification.AddAllCreatureTypes -> Modification.AddAllCreatureTypes
    // SetCantAttack maps to the layer modification for "can't attack" projection
    is SerializableModification.SetCantAttack -> Modification.SetCantAttack
    // SetCantBlock maps to the layer modification for "can't block" projection
    is SerializableModification.SetCantBlock -> Modification.SetCantBlock
    // SetSuspected maps to the layer modification for the "suspected" status projection
    is SerializableModification.SetSuspected -> Modification.SetSuspected
    // PreventAllDamageDealtBy doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.PreventAllDamageDealtBy -> Modification.NoOp
    // RedirectNextDamage doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.RedirectNextDamage -> Modification.NoOp
    // ReplaceDrawWithEffect doesn't map to a layer modification - it's checked during draw execution directly
    is SerializableModification.ReplaceDrawWithEffect -> Modification.NoOp
    // PreventNextDamageFromCreatureType doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.PreventNextDamageFromCreatureType -> Modification.NoOp
    // ExileOnDeath doesn't map to a layer modification - it's checked during SBA creature death
    is SerializableModification.ExileOnDeath -> Modification.NoOp
    // ExileControllerGraveyardOnDeath doesn't map to a layer modification - it's checked during SBA creature death
    is SerializableModification.ExileControllerGraveyardOnDeath -> Modification.NoOp
    // PreventCombatDamageFromGroup doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.PreventCombatDamageFromGroup -> Modification.NoOp
    // PreventCombatDamageToAndBy doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.PreventCombatDamageToAndBy -> Modification.NoOp
    // RedirectCombatDamageToController doesn't map to a layer modification - it's checked by CombatManager directly
    is SerializableModification.RedirectCombatDamageToController -> Modification.NoOp
    // PreventNextDamageFromChosenSourceShield doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.PreventNextDamageFromChosenSourceShield -> Modification.NoOp
    // PreventAllDamageFromSource doesn't map to a layer modification - it's checked during damage resolution directly
    is SerializableModification.PreventAllDamageFromSource -> Modification.NoOp
    // AmplifyNoncombatDamage doesn't map to a layer modification - it's read during damage resolution directly
    is SerializableModification.AmplifyNoncombatDamage -> Modification.NoOp
    is SerializableModification.RemoveAllAbilities -> Modification.RemoveAllAbilities
}
