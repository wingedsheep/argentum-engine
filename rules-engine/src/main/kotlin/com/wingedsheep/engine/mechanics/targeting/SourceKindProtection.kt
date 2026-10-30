package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.ManaValues
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.WasCastComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Parity
import com.wingedsheep.sdk.scripting.ProtectionScope
import kotlinx.serialization.Serializable

/**
 * A protection/hexproof quality read straight off the source when protection is checked, rather
 * than off its projected colours and types: spells, permanents that were cast this turn,
 * Ring-bearers, activated abilities, triggered abilities, and the parity of its mana value
 * (CR 702.16a — the quality "can be any characteristic value or information").
 */
@Serializable
enum class SourceKind(val phrase: String) {
    SPELL("spells"),
    PERMANENT_CAST_THIS_TURN("permanents that were cast this turn"),
    RING_BEARER("Ring-bearers"),
    ACTIVATED_ABILITY("activated abilities"),
    TRIGGERED_ABILITY("triggered abilities"),
    ODD_MANA_VALUE("each odd mana value"),
    EVEN_MANA_VALUE("each even mana value");

    companion object {
        /** The source kind a [ProtectionScope] names, or null for a characteristic scope. */
        fun of(scope: ProtectionScope): SourceKind? = when (scope) {
            ProtectionScope.Spells -> SPELL
            ProtectionScope.PermanentsCastThisTurn -> PERMANENT_CAST_THIS_TURN
            ProtectionScope.RingBearers -> RING_BEARER
            ProtectionScope.ActivatedAbilities -> ACTIVATED_ABILITY
            ProtectionScope.TriggeredAbilities -> TRIGGERED_ABILITY
            is ProtectionScope.ManaValueParity -> parity(scope.parity)
            else -> null
        }

        fun parity(parity: Parity): SourceKind = when (parity) {
            Parity.ODD -> ODD_MANA_VALUE
            Parity.EVEN -> EVEN_MANA_VALUE
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
        return container.has<WasCastComponent>()
    }

    /**
     * A Ring-bearer (CR 701.54): on the battlefield, carrying the designation, and controlled by
     * the player who chose it — the same reading as `StatePredicate.IsRingBearer`, so a stolen
     * Ring-bearer stops being one for protection too.
     */
    fun isRingBearer(state: GameState, entityId: EntityId): Boolean {
        if (entityId !in state.getBattlefield()) return false
        val bearer = state.getEntity(entityId)?.get<RingBearerComponent>() ?: return false
        return state.projectedState.getController(entityId) == bearer.ownerId
    }

    /**
     * [sourceId]'s mana value (CR 202.3), or null for a source without one (an emblem, a vanished
     * id). A face-down permanent's is 0 (CR 708.2a, 202.3a). A spell's {X} counts as the number chosen for
     * it (CR 202.3e): read off the stack object once it is there, or from [announcedX] while the
     * spell is still being cast and its targets are being checked.
     */
    fun manaValue(state: GameState, sourceId: EntityId, announcedX: Int? = null): Int? {
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return null
        if (sourceId in state.getBattlefield() && state.projectedState.isFaceDown(sourceId)) return 0
        val pendingX = if (announcedX != null && sourceId !in state.stack &&
            card.manaValueOverride == null && card.manaCost.hasX
        ) announcedX * card.manaCost.xCount else 0
        return ManaValues.of(state, sourceId, card) + pendingX
    }

    /** The mana-value parity kind of [sourceId], or null when it has no mana value. */
    private fun manaValueKind(state: GameState, sourceId: EntityId, announcedX: Int? = null): SourceKind? =
        manaValue(state, sourceId, announcedX)?.let { mv -> SourceKind.parity(if (mv % 2 == 0) Parity.EVEN else Parity.ODD) }

    /**
     * A spell's mana-value parity kind — or none while its {X} is still unchosen: legal-target
     * enumeration runs before X is announced, so it stays permissive (as `PredicateEvaluator`
     * does for an unbound X) and the cast's own validation re-checks with the announced value.
     */
    private fun spellManaValueKind(state: GameState, sourceId: EntityId, announcedX: Int?): SourceKind? {
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return null
        val xUnchosen = announcedX == null && sourceId !in state.stack &&
            card.manaValueOverride == null && card.manaCost.hasX
        return if (xUnchosen) null else manaValueKind(state, sourceId, announcedX)
    }

    /** The object kinds — those that describe [sourceId] itself rather than a spell-or-ability. */
    private fun objectKinds(state: GameState, sourceId: EntityId): Set<SourceKind> = buildSet {
        if (isPermanentCastThisTurn(state, sourceId)) add(SourceKind.PERMANENT_CAST_THIS_TURN)
        if (isRingBearer(state, sourceId)) add(SourceKind.RING_BEARER)
        manaValueKind(state, sourceId)?.let(::add)
    }

    /**
     * The kinds a spell or ability from [sourceId] has *as a targeting source*. A spell is known
     * from [targetingSourceType] (at cast time the card is still in hand, not yet on the stack);
     * an unknown type falls back to the source being a spell on the stack. Abilities additionally
     * carry their source's object qualities — "permanent cast this turn", "Ring-bearer" — (CR
     * 702.16b: "abilities from a source with the stated quality"). A spell carries its own mana
     * value's parity, counting [announcedX] while it is still being cast.
     */
    fun targetingKinds(
        state: GameState,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType,
        announcedX: Int? = null
    ): Set<SourceKind> = buildSet {
        when (targetingSourceType) {
            TargetingSourceType.SPELL -> add(SourceKind.SPELL)
            TargetingSourceType.ACTIVATED_ABILITY -> add(SourceKind.ACTIVATED_ABILITY)
            TargetingSourceType.TRIGGERED_ABILITY -> add(SourceKind.TRIGGERED_ABILITY)
            TargetingSourceType.ANY -> if (sourceId != null && isSpell(state, sourceId)) add(SourceKind.SPELL)
        }
        if (sourceId != null) {
            if (targetingSourceType != TargetingSourceType.SPELL) addAll(objectKinds(state, sourceId))
            else spellManaValueKind(state, sourceId, announcedX)?.let(::add)
        }
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
        predicateEvaluator: PredicateEvaluator,
        announcedX: Int? = null
    ): String? {
        if (targetId !in state.getBattlefield()) return null
        val kinds = targetingKinds(state, sourceId, targetingSourceType, announcedX)
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
     * attachment and blocking legs (CR 702.16c–f). Only [SourceKind.SPELL] and the object kinds
     * ([SourceKind.PERMANENT_CAST_THIS_TURN], [SourceKind.RING_BEARER], the mana-value parities)
     * describe objects; ability kinds never match here.
     */
    fun isProtectedFromObject(state: GameState, protectedId: EntityId, sourceId: EntityId?): Boolean {
        if (sourceId == null) return false
        val projected = state.projectedState
        if (projected.hasKeyword(protectedId, protectionKeyword(SourceKind.SPELL)) && isSpell(state, sourceId)) return true
        return objectKinds(state, sourceId).any { projected.hasKeyword(protectedId, protectionKeyword(it)) }
    }
}
