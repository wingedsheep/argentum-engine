package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * The printed static abilities live on the permanent [entityId]: the card's base statics plus, for a
 * Class, those of every level it has reached (CR 716.2a). A scan that reads `script.staticAbilities`
 * directly sees only the base statics, so a level-gated permission (Fortune Teller's Talent's level 2
 * "play cards from the top of your library") silently never applies.
 */
fun CardDefinition.liveStaticAbilities(state: GameState, entityId: EntityId): List<StaticAbility> =
    script.effectiveStaticAbilities(state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel)

/**
 * Every live static ability on the battlefield permanents [playerId] controls, paired with its
 * source: [liveStaticAbilities] per permanent, skipping a face-down permanent and one that has lost
 * all its abilities — neither has the printed statics to grant. For battlefield-wide grant scans
 * that aren't continuous effects (foretell grants, special-action cost modifiers).
 */
fun GameState.controlledLiveStaticAbilities(
    playerId: EntityId,
    cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
): Sequence<Pair<EntityId, StaticAbility>> =
    controlledBattlefield(playerId).asSequence().flatMap { permanentId ->
        val container = getEntity(permanentId)
        if (container == null ||
            container.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>() ||
            projectedState.hasLostAllAbilities(permanentId)
        ) return@flatMap emptySequence()
        val def = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return@flatMap emptySequence()
        def.liveStaticAbilities(this, permanentId).asSequence().map { permanentId to it }
    }
