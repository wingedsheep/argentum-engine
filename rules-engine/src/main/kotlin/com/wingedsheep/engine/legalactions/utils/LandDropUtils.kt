package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayersCantPlayLands
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.state.components.identity.emblemStaticAbilitiesOf
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

/**
 * Utility for calculating additional land drops from static abilities.
 */
object LandDropUtils {

    /**
     * True if any permanent on the battlefield forbids [playerId] from playing lands
     * ([com.wingedsheep.sdk.scripting.PlayersCantPlayLands] — Worms of the Earth).
     *
     * Scans the whole battlefield, not just [playerId]'s: the lock is usually somebody else's
     * enchantment. A [ConditionalStaticAbility] wrapper is unwrapped and evaluated against its own
     * source, the same way the land-drop bonus above is, so an "as long as …" gate is honored
     * instead of silently locking forever.
     *
     * [landCardId] scopes the question to one candidate card, which is what a *filtered* lock
     * needs (City in a Bottle stops only the lands originally printed in ARN). Pass `null` — the
     * default — to ask the blanket question "is this player locked out of land drops entirely?";
     * a filtered lock deliberately answers `false` there, so the unaffected lands in the hand stay
     * playable and only the per-card call below rejects the matching ones.
     */
    fun playerCantPlayLands(
        state: GameState,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
        landCardId: EntityId? = null
    ): Boolean {
        val projected = state.projectedState
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardDef = container.faceUpCardDefinition(cardRegistry) ?: continue
            val sourceController = projected.getController(entityId) ?: continue
            // Class levels and unlocked Room doors carry statics outside `script.staticAbilities`.
            for (ability in RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                val lock = when (ability) {
                    is PlayersCantPlayLands -> ability
                    is ConditionalStaticAbility -> {
                        val inner = ability.ability as? PlayersCantPlayLands ?: continue
                        val context = EffectContext(sourceId = entityId, controllerId = sourceController)
                        if (!conditionEvaluator.evaluate(state, ability.condition, context)) continue
                        inner
                    }
                    else -> continue
                }
                val affected = when (lock.affected) {
                    is Player.Each -> state.activePlayers
                    is Player.You -> listOf(sourceController)
                    is Player.EachOpponent -> state.getOpponents(sourceController)
                    else -> continue
                }
                if (playerId !in affected) continue
                // A filtered lock only bites on a named candidate; the blanket probe skips it.
                if (lock.landFilter != GameObjectFilter.Any) {
                    if (landCardId == null) continue
                    if (!conditionEvaluator.predicates.matches(
                            state, projected, landCardId, lock.landFilter,
                            PredicateContext(controllerId = playerId)
                        )
                    ) continue
                }
                val context = EffectContext(sourceId = entityId, controllerId = sourceController)
                if (lock.condition == null || conditionEvaluator.evaluate(state, lock.condition!!, context)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Cheap guard: does any battlefield permanent carry a *filtered* [PlayersCantPlayLands]
     * (`landFilter != Any`)? Lets enumeration skip the per-card [playerCantPlayLands] scan
     * entirely in the common case where none is in play. Cached once per enumeration pass by
     * [com.wingedsheep.engine.legalactions.EnumerationContext]; the mirror of
     * `CastPermissionUtils.anyPerSpellCastRestrictionPresent`.
     */
    fun anyFilteredLandLockPresent(state: GameState, cardRegistry: CardRegistry): Boolean =
        state.getBattlefield().any { id ->
            val container = state.getEntity(id) ?: return@any false
            val cardDef = container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return@any false
            RoomFaceStatics.activeStaticAbilities(container, cardDef).any { ability ->
                val lock = ability as? PlayersCantPlayLands
                    ?: (ability as? ConditionalStaticAbility)?.ability as? PlayersCantPlayLands
                lock != null && lock.landFilter != GameObjectFilter.Any
            }
        }

    /**
     * Does [playerId] have a land play left this turn — the turn's own drops still
     * [LandDropsComponent.remaining] plus every [GrantAdditionalLandDrop] that applies to them, or
     * any applicable grant of "any number of lands" (Fastbond)? Shared by `PlayLandHandler` and the
     * legal-action enumerator so the two can never disagree.
     */
    fun hasLandPlayLeft(state: GameState, playerId: EntityId): Boolean {
        val remaining = state.getEntity(playerId)?.get<LandDropsComponent>()?.remaining ?: 0
        val bonus = getAdditionalLandDrops(state, playerId) ?: return true
        return remaining + bonus > 0
    }

    /**
     * Count additional land drops granted by [GrantAdditionalLandDrop] static abilities
     * that apply to the given player. Multiple sources are additive; null when any of them grants
     * "any number of lands" (Fastbond) — unlimited, not a large number, so nothing overflows.
     *
     * Read off each permanent's *projected* static abilities, so every route counts: printed
     * (Oracle of Mul Daya; a Class level or unlocked Room door), gained from a resolved effect
     * (Nog, Ambitious Cadet), or granted by a [com.wingedsheep.sdk.scripting.GrantStaticAbility].
     * Projection already honors a [ConditionalStaticAbility] gate (Thranduil's Company only grants
     * the extra drop while you control another Elf) and drops the grant from a permanent that has
     * lost all its abilities. "You" is the holder's projected controller.
     */
    fun getAdditionalLandDrops(state: GameState, playerId: EntityId): Int? {
        // Scans the whole battlefield, not just [playerId]'s permanents: a symmetric grant
        // (`affected = Player.Each` — Rites of Flourishing) usually sits on someone else's side.
        val projected = state.projectedState
        var bonus = 0
        for (entityId in state.getBattlefield()) {
            val abilities = projected.getStaticAbilities(entityId)
            if (abilities.isEmpty()) continue
            val holderController = projected.getController(entityId) ?: continue
            for (grant in abilities) {
                if (grant !is GrantAdditionalLandDrop) continue
                val grantsPlayer = when (grant.affected) {
                    Player.Each -> true
                    Player.EachOpponent -> playerId != holderController &&
                        playerId in state.getOpponents(holderController)
                    else -> playerId == holderController
                }
                if (grantsPlayer) bonus += grant.count ?: return null
            }
        }
        return bonus
    }

    /**
     * Whether [playerId] may play lands from their graveyard through a
     * [MayPlayLandsFromGraveyard] (Crucible of Worlds; no per-turn tracking — the land drop is
     * the limit): one among the projected static abilities of a permanent they control —
     * printed, gated (Glacierwood Siege's Sultai mode, honored by projection), or gained (Nog,
     * Ambitious Cadet) — or an emblem they have (Wrenn and Realmbreaker's −7). Shared by
     * `PlayLandHandler` and the graveyard-play enumerator so the two can never disagree.
     */
    fun hasGraveyardLandPlayPermission(state: GameState, playerId: EntityId): Boolean {
        val projected = state.projectedState
        return state.getBattlefield().any { entityId ->
            projected.getController(entityId) == playerId &&
                projected.getStaticAbilities(entityId).any { it is MayPlayLandsFromGraveyard }
        } || state.emblemStaticAbilitiesOf(playerId).any { (_, ability) -> ability is MayPlayLandsFromGraveyard }
    }
}
