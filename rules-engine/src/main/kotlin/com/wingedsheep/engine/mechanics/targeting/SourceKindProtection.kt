package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CastFromExileComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromLibraryComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProtectionScope
import kotlinx.serialization.Serializable

/**
 * A protection/hexproof quality that is a *kind of source* rather than a characteristic:
 * spells, permanents that were cast this turn, activated abilities, triggered abilities
 * (CR 702.16a — the quality "can be any characteristic value or information").
 */
@Serializable
enum class SourceKind(val phrase: String) {
    SPELL("spells"),
    PERMANENT_CAST_THIS_TURN("permanents that were cast this turn"),
    ACTIVATED_ABILITY("activated abilities"),
    TRIGGERED_ABILITY("triggered abilities");

    companion object {
        /** The source kind a [ProtectionScope] names, or null for a characteristic scope. */
        fun of(scope: ProtectionScope): SourceKind? = when (scope) {
            ProtectionScope.Spells -> SPELL
            ProtectionScope.PermanentsCastThisTurn -> PERMANENT_CAST_THIS_TURN
            ProtectionScope.ActivatedAbilities -> ACTIVATED_ABILITY
            ProtectionScope.TriggeredAbilities -> TRIGGERED_ABILITY
            else -> null
        }
    }
}

/**
 * The one reading of [SourceKind] protection and hexproof, shared by every place that enforces
 * protection (targeting, target re-validation on resolution, damage prevention, attachment and
 * blocking) so the legs can't drift apart.
 *
 * The qualities are projected like every other protection scope — `PROTECTION_FROM_SOURCEKIND_<K>`
 * and `HEXPROOF_FROM_SOURCEKIND_<K>` — so losing all abilities drops them with the rest.
 */
object SourceKindProtection {

    fun protectionKeyword(kind: SourceKind): String = "PROTECTION_FROM_SOURCEKIND_${kind.name}"

    fun hexproofKeyword(kind: SourceKind): String = "HEXPROOF_FROM_SOURCEKIND_${kind.name}"

    /** A spell: a card on the stack as a spell (CR 112.1), or a copy of one. */
    fun isSpell(state: GameState, entityId: EntityId): Boolean =
        state.getEntity(entityId)?.has<SpellOnStackComponent>() == true

    /**
     * A permanent that was cast this turn: on the battlefield, entered this turn, and entered by
     * resolving as a cast spell — the cast-origin markers are only written on that path and are
     * stripped when it leaves, so a blinked, reanimated, or token permanent doesn't qualify.
     */
    fun isPermanentCastThisTurn(state: GameState, entityId: EntityId): Boolean {
        if (entityId !in state.getBattlefield()) return false
        val container = state.getEntity(entityId) ?: return false
        if (!container.has<EnteredThisTurnComponent>()) return false
        return container.has<CastFromHandComponent>() || container.has<CastFromGraveyardComponent>() ||
            container.has<CastFromLibraryComponent>() || container.has<CastFromExileComponent>()
    }

    /**
     * The kinds a spell or ability from [sourceId] has *as a targeting source*. A spell is known
     * from [targetingSourceType] (at cast time the card is still in hand, not yet on the stack);
     * an unknown type falls back to the source being a spell on the stack. Abilities additionally
     * carry their source's "permanent cast this turn" quality (CR 702.16b: "abilities from a source
     * with the stated quality").
     */
    fun targetingKinds(
        state: GameState,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType
    ): Set<SourceKind> = buildSet {
        when (targetingSourceType) {
            TargetingSourceType.SPELL -> add(SourceKind.SPELL)
            TargetingSourceType.ACTIVATED_ABILITY -> add(SourceKind.ACTIVATED_ABILITY)
            TargetingSourceType.TRIGGERED_ABILITY -> add(SourceKind.TRIGGERED_ABILITY)
            TargetingSourceType.ANY -> if (sourceId != null && isSpell(state, sourceId)) add(SourceKind.SPELL)
        }
        if (targetingSourceType != TargetingSourceType.SPELL && sourceId != null &&
            isPermanentCastThisTurn(state, sourceId)
        ) add(SourceKind.PERMANENT_CAST_THIS_TURN)
    }

    /**
     * Why [targetId] can't be targeted by a spell or ability from [sourceId] controlled by
     * [casterId], or null. Protection (CR 702.16b) blocks every controller; hexproof from a
     * quality (CR 702.11d) only opponents, and yields to hexproof suppression (CR 702.11e).
     */
    fun targetingError(
        state: GameState,
        targetId: EntityId,
        sourceId: EntityId?,
        casterId: EntityId,
        targetingSourceType: TargetingSourceType,
        predicateEvaluator: PredicateEvaluator
    ): String? {
        if (targetId !in state.getBattlefield()) return null
        val kinds = targetingKinds(state, sourceId, targetingSourceType)
        if (kinds.isEmpty()) return null
        val projected = state.projectedState
        val name = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "target"
        kinds.firstOrNull { projected.hasKeyword(targetId, protectionKeyword(it)) }
            ?.let { return "$name has protection from ${it.phrase}" }
        val hexproof = kinds.firstOrNull { projected.hasKeyword(targetId, hexproofKeyword(it)) } ?: return null
        val controller = projected.getController(targetId)
            ?: state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
        if (controller == casterId) return null
        if (HexproofSuppression.isSuppressedForCaster(state, projected, targetId, casterId, predicateEvaluator = predicateEvaluator)) return null
        return "$name has hexproof from ${hexproof.phrase}"
    }

    /**
     * True when [protectedId] has protection from [sourceId] as an *object* — the damage,
     * attachment and blocking legs (CR 702.16c–f). Only [SourceKind.SPELL] and
     * [SourceKind.PERMANENT_CAST_THIS_TURN] describe objects; ability kinds never match here.
     */
    fun isProtectedFromObject(state: GameState, protectedId: EntityId, sourceId: EntityId?): Boolean {
        if (sourceId == null) return false
        val projected = state.projectedState
        if (projected.hasKeyword(protectedId, protectionKeyword(SourceKind.SPELL)) && isSpell(state, sourceId)) return true
        return projected.hasKeyword(protectedId, protectionKeyword(SourceKind.PERMANENT_CAST_THIS_TURN)) &&
            isPermanentCastThisTurn(state, sourceId)
    }
}
