package com.wingedsheep.engine.state.permissions

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Add a permission to the game state's [GameState.mayPlayPermissions] list.
 */
fun GameState.addMayPlayPermission(permission: MayPlayPermission): GameState =
    copy(mayPlayPermissions = mayPlayPermissions + permission)

/**
 * Remove a permission by id. No-op if absent.
 */
fun GameState.removeMayPlayPermission(id: EntityId): GameState =
    copy(mayPlayPermissions = mayPlayPermissions.filterNot { it.id == id })

/**
 * Revoke every [MayPlayPermission.supersededBySameSource] permission granted by [sourceId].
 * Called when that source grants a new such permission (exiles another card), so only its
 * most-recently-exiled card stays playable — models "until you exile another card with this
 * permanent" (Superior Foes of Spider-Man). The superseded cards remain in exile; they simply
 * lose their play permission.
 */
fun GameState.revokeSupersededPermissionsFromSource(sourceId: EntityId): GameState =
    copy(
        mayPlayPermissions = mayPlayPermissions.filterNot {
            it.supersededBySameSource && it.sourceId == sourceId
        }
    )

/**
 * Drop [cardId] from every permission's `cardIds`. Used when a card moves to a zone where
 * the permission is no longer meaningful (e.g., on resolve), to keep the list compact.
 *
 * Multi-card permissions (e.g., Etali / Narset / Mind's Desire grant a single permission
 * spanning all exiled cards) MUST keep authorising the remaining cards after one of them
 * is cast. Removing the whole permission would silently revoke "any number of spells"
 * after the first cast.
 *
 * Permissions whose `cardIds` becomes empty are dropped entirely.
 */
fun GameState.removeMayPlayPermissionsForCard(cardId: EntityId): GameState =
    copy(
        mayPlayPermissions = mayPlayPermissions.mapNotNull { permission ->
            if (cardId !in permission.cardIds) permission
            else {
                val remaining = permission.cardIds - cardId
                if (remaining.isEmpty()) null else permission.copy(cardIds = remaining)
            }
        }
    )

/**
 * Spend one play from every capped ([MayPlayPermission.playsRemaining]) permission [playerId] holds
 * over [cardId]: the card was just cast or played through it. A grant left with plays drops the
 * card that was played; one whose last play this was is revoked for the rest of its group too
 * ("cast a spell from among those cards" — one spell, not one each). Call only at a play site,
 * never when a card merely leaves exile.
 *
 * When an ordinary, unconditional stored grant also covers the card, the play is attributed to
 * that one and the capped grant is left untouched — the player chooses which permission they use,
 * and spending the scarce one would never be their choice. A conditional grant doesn't shield it:
 * its gate isn't re-evaluated here, so it may not have authorized the play.
 */
fun GameState.spendCappedMayPlayFor(cardId: EntityId, playerId: EntityId): GameState {
    fun covers(permission: MayPlayPermission) = permission.controllerId == playerId && cardId in permission.cardIds
    if (mayPlayPermissions.none { it.playsRemaining != null && covers(it) }) return this
    if (mayPlayPermissions.any { it.playsRemaining == null && it.condition == null && covers(it) }) return this
    return copy(
        mayPlayPermissions = mayPlayPermissions.mapNotNull { permission ->
            val plays = permission.playsRemaining
            when {
                plays == null || !covers(permission) -> permission
                plays <= 1 -> null
                else -> permission.copy(playsRemaining = plays - 1, cardIds = permission.cardIds - cardId)
            }
        }
    )
}

/**
 * The granter whose once-per-turn allowance ([MayPlayPermission.oncePerTurn]) playing [cardId]
 * spends, or null when the play spends none. Read against the state *before* the play, while the
 * card is still in exile and its grants can still be found.
 *
 * As with [spendCappedMayPlayFor], an unlimited grant covering the card wins: the player
 * chooses which permission they use, and burning the scarce one would never be their choice.
 * Unlike the stored grants there, every candidate here was already gate-checked by
 * [activeMayPlayFor], so a conditional unlimited grant shields the allowance too. A shield must
 * offer the same mana relaxation, though: when the once-per-turn grant carries an any-color/any-type
 * rider the cast may have leaned on, a rider-less unlimited grant couldn't have paid for it alone.
 */
fun GameState.oncePerTurnGranterToSpend(
    cardId: EntityId,
    playerId: EntityId,
    conditionEvaluator: ConditionEvaluator,
    cardRegistry: CardRegistry
): EntityId? {
    val active = activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)
    val limited = active.firstOrNull { it.oncePerTurn } ?: return null
    fun relaxesMana(p: MayPlayPermission) = p.withAnyManaType || p.withAnyManaColor || p.spendAsAnyColor != null
    val shielded = active.any { !it.oncePerTurn && (!relaxesMana(limited) || relaxesMana(it)) }
    return if (shielded) null else limited.sourceId
}

/**
 * Find every active permission that authorizes [playerId] to play [cardId], with the gate
 * condition currently open. Multiple permissions can stack (e.g., a conditional grant and
 * an unconditional one); each read site picks how to combine them.
 *
 * Covers both permissions *stored* on the state (granted by a resolving effect) and permissions
 * *derived* from a [com.wingedsheep.sdk.scripting.MayPlayCardsFromExile] static ability on the
 * battlefield (Tinybones, Bauble Burglar) — see [StaticMayPlayGrants] for why the latter can't be
 * stored. Callers therefore need the [cardRegistry] to read the granting permanent's script.
 */
fun GameState.activeMayPlayFor(
    cardId: EntityId,
    playerId: EntityId,
    conditionEvaluator: ConditionEvaluator,
    cardRegistry: CardRegistry
): List<MayPlayPermission> = (
    mayPlayPermissions.filter { permission ->
        permission.controllerId == playerId && cardId in permission.cardIds
    } + StaticMayPlayGrants.forCard(this, cardId, playerId, cardRegistry, predicateEvaluator = conditionEvaluator.predicates)
    ).filter { permission -> permission.gateOpen(this, cardId, conditionEvaluator) }

/**
 * True when playing [cardId] through its may-play permissions puts it onto the battlefield tapped:
 * some active permission authorizes the play, and every one that does carries an
 * [MayPlayPermission.entersTapped] filter the card matches. The rider belongs to the permission a
 * play goes through, so a rider-free permission covering the same card is the one the player plays
 * it with. [landPlay] leaves out the `nonLandOnly` permissions, which never authorize a land play.
 * Read while the card is still in the zone it is played from.
 */
fun GameState.mayPlayEntersTapped(
    cardId: EntityId,
    playerId: EntityId,
    conditionEvaluator: ConditionEvaluator,
    cardRegistry: CardRegistry,
    landPlay: Boolean,
): Boolean {
    val authorizing = activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)
        .filter { !landPlay || !it.nonLandOnly }
    if (authorizing.isEmpty()) return false
    val context = PredicateContext(controllerId = playerId)
    return authorizing.all { permission ->
        val filter = permission.entersTapped ?: return false
        conditionEvaluator.predicates.matches(this, projectedState, cardId, filter, context)
    }
}

/**
 * True when at least one active permission authorizes [playerId] to play [cardId].
 */
fun GameState.hasMayPlayFor(
    cardId: EntityId,
    playerId: EntityId,
    conditionEvaluator: ConditionEvaluator,
    cardRegistry: CardRegistry
): Boolean = activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry).isNotEmpty()

/**
 * Re-evaluate the optional condition gate at the read site. Conditions on permissions
 * (Possibility Technician's "you control a Kavu") must be checked at every query, not
 * just at the moment the permission was created.
 *
 * Supported condition shapes: ambient state (`Exists`, `Compare`, life totals, hand sizes,
 * …) and anything keyed off [EffectContext.controllerId]. Source-referencing conditions
 * (`SourceHas*`, `SourceIs*`) work only when [MayPlayPermission.sourceId] is set; otherwise
 * the source falls back to [cardId] and source-keyed conditions misfire silently. If a card
 * needs a source-keyed gate, set `sourceId` on the permission at grant time.
 */
fun MayPlayPermission.gateOpen(
    state: GameState,
    cardId: EntityId,
    conditionEvaluator: ConditionEvaluator
): Boolean {
    val condition = condition ?: return true
    val context = EffectContext(
        sourceId = sourceId ?: cardId,
        controllerId = controllerId,
    )
    return conditionEvaluator.evaluate(state, condition, context)
}
