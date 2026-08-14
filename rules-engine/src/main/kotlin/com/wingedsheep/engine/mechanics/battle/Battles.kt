package com.wingedsheep.engine.mechanics.battle

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ProtectorComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * The battle card type (CR 310) in one place: its defense, its protector, and who may attack it.
 *
 * A battle is a permanent that is *attacked* rather than one that attacks. Three facts define it
 * and every consumer in the engine reads them through this object rather than re-deriving them:
 *
 *  - **Defense is counters.** A battle's defense is its number of defense counters (CR 310.4c); it
 *    enters with its printed defense number of them (CR 310.4b) and damage removes that many
 *    (CR 120.3h). [defenseOf].
 *  - **A protector, not a controller, defends it.** Every battle has a player designated as its
 *    protector (CR 310.8), and for a battle being attacked *that* player — not its controller — is
 *    the defending player for every rule and effect (CR 310.8d). [protectorOf].
 *  - **Its protector can never attack it.** Anyone for whom the protector is a defending player
 *    can, which for a Siege notably includes the battle's own controller (CR 310.8b).
 *    [canBeAttackedBy].
 */
object Battles {

    /**
     * The counter kind a battle's defense is made of. Used by the intrinsic entry ability
     * (CR 310.4b), by damage (CR 120.3h), and by the defeat trigger (CR 310.11b).
     */
    val DEFENSE_COUNTER: CounterTypeFilter = CounterTypeFilter.Named(Counters.DEFENSE)

    /** True if [entityId] is a battle on the battlefield, per projected types (CR 310). */
    fun isBattle(state: GameState, entityId: EntityId): Boolean =
        state.projectedState.isBattle(entityId)

    /**
     * True if [entityId] is a Siege — the only battle type printed so far (CR 310.11), and the one
     * whose protector must be an opponent of its controller (CR 310.11a). Read from projected
     * subtypes so a type-changing effect is respected.
     */
    fun isSiege(state: GameState, entityId: EntityId): Boolean =
        isBattle(state, entityId) && state.projectedState.hasSubtype(entityId, Subtype.SIEGE.value)

    /** A battle's defense: its number of defense counters (CR 310.4c). 0 when it has none. */
    fun defenseOf(state: GameState, entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) ?: 0

    /**
     * The player designated as [entityId]'s protector (CR 310.8), or null if none is designated
     * yet — a gap [com.wingedsheep.engine.mechanics.sba.permanent.BattleProtectorCheck] closes as a
     * state-based action (CR 704.5w).
     */
    fun protectorOf(state: GameState, entityId: EntityId): EntityId? =
        state.getEntity(entityId)?.get<ProtectorComponent>()?.playerId

    /**
     * The players who may legally be [battleId]'s protector, in turn order (CR 310.8a). Determined
     * by the battle's type: a Siege's protector must be an opponent of its controller (CR 310.11a);
     * a battle with no battle types is protected by its own controller (CR 310.8a).
     *
     * Empty means no player qualifies, which puts the battle into its owner's graveyard
     * (CR 704.5w).
     */
    fun eligibleProtectors(state: GameState, battleId: EntityId): List<EntityId> {
        val controller = state.projectedState.getController(battleId) ?: return emptyList()
        return if (isSiege(state, battleId)) {
            state.turnOrder.filter { it != controller }
        } else {
            listOf(controller).filter { it in state.turnOrder }
        }
    }

    /**
     * True if [attackerPlayerId]'s creatures may attack [battleId] (CR 310.8b): a battle can be
     * attacked by any attacking player for whom its protector is a defending player, and never by
     * its protector. The battle's *controller* is irrelevant — which is exactly why a player can
     * attack a Siege they control once an opponent is protecting it.
     */
    fun canBeAttackedBy(
        state: GameState,
        battleId: EntityId,
        attackerPlayerId: EntityId,
        legalDefendingPlayers: Set<EntityId>
    ): Boolean {
        val protector = protectorOf(state, battleId) ?: return false
        if (protector == attackerPlayerId) return false
        return protector in legalDefendingPlayers
    }
}
