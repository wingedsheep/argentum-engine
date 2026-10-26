package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.liveStaticAbilities
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

/**
 * The library-top permissions that name *whose* library they open — [PlayFromTopWithAlternativeCost]
 * and [com.wingedsheep.sdk.scripting.LookAtTopOfLibrary], each carrying a `libraryOwner`.
 *
 * Both are read relative to the player who holds them: printed on a permanent that player controls,
 * or granted durationally in [GameState.grantedStaticAbilities] anchored either to such a permanent
 * (Gwenom, Remorseless grants itself the permission) or to the player directly (Xanathar, Guild
 * Kingpin's upkeep trigger grants its controller a turn-long permission over a target opponent's
 * library — one that outlives Xanathar, because it was a resolved effect, not Xanathar's static).
 *
 * The one place the cast enumerator, the cast and land-play handlers and the visibility rules
 * resolve these permissions, so what a player may see, play and is offered never disagree.
 */
object LibraryTopGrants {

    /**
     * The player who holds [grant]: the player it is anchored to, or the controller of the
     * permanent it is anchored to. Null when the anchor is gone.
     */
    fun holderOf(state: GameState, grant: GrantedStaticAbility): EntityId? =
        if (grant.entityId in state.turnOrder) grant.entityId
        else state.getEntity(grant.entityId)?.get<ControllerComponent>()?.playerId

    /**
     * The player whose library [libraryOwner] names, read relative to [holderId]: "your library"
     * ([EffectTarget.Controller]) is the holder's own; a pinned [EffectTarget.SpecificEntity] is that
     * player. Any other reference has no meaning once the granting resolution is over.
     */
    fun libraryOwnerId(libraryOwner: EffectTarget, holderId: EntityId): EntityId? = when (libraryOwner) {
        EffectTarget.Controller -> holderId
        is EffectTarget.SpecificEntity -> libraryOwner.entityId
        else -> null
    }

    /**
     * The [PlayFromTopWithAlternativeCost] permission [playerId] holds over [libraryOwnerId]'s
     * library, printed or granted, or null.
     */
    fun playFromTop(
        state: GameState,
        playerId: EntityId,
        libraryOwnerId: EntityId,
        cardRegistry: CardRegistry,
    ): PlayFromTopWithAlternativeCost? {
        // A printed permission only ever opens its controller's own library.
        if (libraryOwnerId == playerId) {
            for (entityId in state.getBattlefield(playerId)) {
                val cardDef = state.getEntity(entityId)?.faceUpCardDefinition(cardRegistry) ?: continue
                cardDef.liveStaticAbilities(state, entityId)
                    .firstOrNull { it is PlayFromTopWithAlternativeCost && it.libraryOwner == EffectTarget.Controller }
                    ?.let { return it as PlayFromTopWithAlternativeCost }
            }
        }
        for (grant in state.grantedStaticAbilities) {
            val ability = grant.ability as? PlayFromTopWithAlternativeCost ?: continue
            if (holderOf(state, grant) != playerId) continue
            if (libraryOwnerId(ability.libraryOwner, playerId) != libraryOwnerId) continue
            return ability
        }
        return null
    }

    /** The player whose library [cardId] is the top card of, or null. */
    fun libraryTopOwner(state: GameState, cardId: EntityId): EntityId? =
        state.turnOrder.firstOrNull { state.getLibrary(it).firstOrNull() == cardId }
}
